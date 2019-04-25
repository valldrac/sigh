package org.thoughtcrime.securesms.service;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentCallbacks2;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.logging.Log;
import org.thoughtcrime.securesms.notifications.NotificationChannels;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class MemoryWipeService extends IntentService implements ComponentCallbacks2 {

    private static final String TAG = MemoryWipeService.class.getSimpleName();

    private static final int NOTIFICATION_ID = 4343;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static final int PRESSURE_THRESHOLD = TRIM_MEMORY_RUNNING_MODERATE;

    private static final int AVAIL_MEM_PERCENT_THRESHOLD = 75;

    static {
        System.loadLibrary("native-utils");
    }

    private static native long allocPages(int order);
    private static native void freePages(long p);
    private static native void wipePage(long p, int index);
    private static native long getPageSize();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int maxPressureLevel;

    public MemoryWipeService() {
        super("MemoryWipeService");
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (!KeyCachingService.isLocked())
            killProcess();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int last = getLastMemoryPressure();

        Log.d(TAG, "Pressure: target=" + PRESSURE_THRESHOLD + " last=" + last);

        final Future<?> eaten = executor.submit(new MemoryEater());

        try {
            Log.i(TAG, "Total Wiped: " + eaten.get() + " bytes");
        } catch (InterruptedException | ExecutionException ignored) {}

        Log.d(TAG, "Pressure: max=" + maxPressureLevel);
    }

    @Override
    public void onTrimMemory(int level) {
        Log.d(TAG, "onTrimMemory(" + level +")");
        super.onTrimMemory(level);
        reportPressure(level);
    }

    @Override
    public void onLowMemory() {
        Log.d(TAG, "onLowMemory()");
        super.onLowMemory();
        reportPressure(ComponentCallbacks2.TRIM_MEMORY_COMPLETE);
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        super.onCreate();
        showForegroundNotification();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        hideForegroundNotification();
    }

    private void killProcess() {
        System.exit(0);
    }

    private void reportPressure(int level) {
        if (level > maxPressureLevel)
            maxPressureLevel = level;

        if (level < PRESSURE_THRESHOLD)
            return;

        executor.shutdownNow();
    }

    private void showForegroundNotification() {
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0,
                        new Intent(this, ConversationListActivity.class), 0);
        Notification notification =
                new NotificationCompat.Builder(this, NotificationChannels.LOCKED_STATUS)
                        .setContentTitle(this.getString(R.string.MemoryWipeService_signal_is_clearing_secrets))
                        .setSmallIcon(R.drawable.icon_cached)
                        .setContentIntent(pendingIntent)
                        .build();
        startForeground(NOTIFICATION_ID, notification);
    }

    private void hideForegroundNotification() {
        stopForeground(true);
    }

    private int getLastMemoryPressure() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
            return maxPressureLevel;

        try {
            ActivityManager.RunningAppProcessInfo processInfo =
                    new ActivityManager.RunningAppProcessInfo();
            ActivityManager.getMyMemoryState(processInfo);

            return processInfo.lastTrimLevel;
        } catch (Exception e) {
            // Defensively catch all exceptions, just in case.
        }

        return maxPressureLevel;
    }

    private class MemoryEater implements Callable<Long> {
        private static final int PAGE_ORDER = 250;  // 1000KB

        final long pageSize = getPageSize();

        final ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        final ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

        @Override
        public Long call() {
            List<Long> chunks = new ArrayList<>();

            long total = 0;

            try {
                // Android notifies about memory pressure through onTrimMemory() callback
                // but this is not reliable. Poll ActivityManager periodically to prevent a
                // malloc bomb if memory pressure signals are lost or delayed.
                while (!isLowMemory()) {
                    if (Thread.interrupted())
                        return total;

                    long pages = allocPages(PAGE_ORDER);

                    // If returned a NULL pointer, the memory is exhausted.
                    if (pages == 0)
                        break;

                    chunks.add(pages);

                    for (int i = 0; i < PAGE_ORDER; i++) {
                        if (Thread.interrupted())
                            return total;

                        wipePage(pages, i);

                        total += pageSize;

                        Thread.yield();
                    }
                }
            } catch (OutOfMemoryError ignored) {
            } finally {
                for (Long p : chunks) {
                    freePages(p);
                }
            }

            return total;
        }

        private boolean isLowMemory() {
            activityManager.getMemoryInfo(memoryInfo);

            // Unfortunately, getMemoryInfo() does not provide real-time values,
            // availMem is too optimistic.
            long avail = (long) ((float)memoryInfo.availMem * AVAIL_MEM_PERCENT_THRESHOLD / 100L);

            return memoryInfo.threshold > avail;
        }
    }
}
