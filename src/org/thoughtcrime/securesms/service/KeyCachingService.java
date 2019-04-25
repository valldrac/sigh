/*
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.service;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.logging.Log;
import android.widget.RemoteViews;

import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.DatabaseUpgradeActivity;
import org.thoughtcrime.securesms.DummyActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.crypto.InvalidPassphraseException;
import org.thoughtcrime.securesms.crypto.MasterSecret;
import org.thoughtcrime.securesms.crypto.MasterSecretUtil;
import org.thoughtcrime.securesms.notifications.MessageNotifier;
import org.thoughtcrime.securesms.notifications.NotificationChannels;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.ServiceUtil;
import org.thoughtcrime.securesms.util.TextSecurePreferences;

import java.util.concurrent.TimeUnit;

/**
 * Small service that stays running to keep a key cached in memory.
 *
 * @author Moxie Marlinspike
 */

public class KeyCachingService extends Service {

  private static final String TAG = KeyCachingService.class.getSimpleName();

  public static final int SERVICE_RUNNING_ID = 4141;

  public  static final String KEY_PERMISSION           = "org.sigh.app.ACCESS_SECRETS";
  public  static final String NEW_KEY_EVENT            = "org.sigh.app.service.action.NEW_KEY_EVENT";
  public  static final String CLEAR_KEY_EVENT          = "org.sigh.app.service.action.CLEAR_KEY_EVENT";
  public  static final String LOCK_TOGGLED_EVENT       = "org.sigh.app.service.action.LOCK_ENABLED_EVENT";
  private static final String PASSPHRASE_EXPIRED_EVENT = "org.sigh.app.service.action.PASSPHRASE_EXPIRED_EVENT";
  public  static final String CLEAR_KEY_ACTION         = "org.sigh.app.service.action.CLEAR_KEY";
  public  static final String LOCALE_CHANGE_EVENT      = "org.sigh.app.service.action.LOCALE_CHANGE_EVENT";

  private DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private final IBinder binder  = new KeySetBinder();

  private static MasterSecret masterSecret;

  public KeyCachingService() {}

  public static synchronized boolean isLocked() {
    return masterSecret == null;
  }

  public static synchronized MasterSecret getMasterSecret() {
    while (masterSecret == null) {
      try {
        KeyCachingService.class.wait();
      } catch (InterruptedException e) {
        Log.w(TAG, e);
      }
    }
    return masterSecret;
  }

  public static void onAppForegrounded(@NonNull Context context) {
    ServiceUtil.getAlarmManager(context).cancel(buildExpirationPendingIntent(context));
  }

  public static void onAppBackgrounded(@NonNull Context context) {
    startTimeoutIfAppropriate(context);
  }

  @SuppressLint("StaticFieldLeak")
  public void setMasterSecret(final MasterSecret masterSecret) {
    synchronized (KeyCachingService.class) {
      KeyCachingService.masterSecret = masterSecret;
      KeyCachingService.class.notifyAll();

      foregroundService();
      broadcastNewSecret();
      startTimeoutIfAppropriate(this);

      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          if (!DatabaseUpgradeActivity.isUpdate(KeyCachingService.this)) {
            MessageNotifier.updateNotification(KeyCachingService.this);
          }
          return null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent == null) return START_NOT_STICKY;
    Log.d(TAG, "onStartCommand, " + intent.getAction());

    if (intent.getAction() != null) {
      switch (intent.getAction()) {
        case CLEAR_KEY_ACTION:         handleClearKey();        break;
        case PASSPHRASE_EXPIRED_EVENT: handleClearKey();        break;
        case LOCALE_CHANGE_EVENT:      handleLocaleChanged();   break;
        case LOCK_TOGGLED_EVENT:       handleLockToggled();     break;
      }
    }

    return START_NOT_STICKY;
  }

  @Override
  public void onCreate() {
    Log.i(TAG, "onCreate()");
    super.onCreate();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.w(TAG, "KCS Is Being Destroyed!");
    handleClearKey();
  }

  /**
   * Workaround for Android bug:
   * https://code.google.com/p/android/issues/detail?id=53313
   */
  @Override
  public void onTaskRemoved(Intent rootIntent) {
    Intent intent = new Intent(this, DummyActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
  }

  @SuppressLint("StaticFieldLeak")
  private void handleClearKey() {
    Log.w(TAG, "handleClearKey()");
    synchronized (KeyCachingService.class) {
      if (KeyCachingService.masterSecret == null)
        return;

      Intent intent = new Intent(CLEAR_KEY_EVENT);
      intent.setPackage(getApplicationContext().getPackageName());

      sendBroadcast(intent, KEY_PERMISSION);

      Log.i(TAG, "Scheduling Memory Wipe...");

      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          MessageNotifier.clearNotifications(KeyCachingService.this, true);

          try {
            Thread.sleep(2000);
          } catch (InterruptedException ignored) {}

          KeyCachingService.masterSecret.destroy();
          stopForeground(true);

          Intent intent = new Intent(KeyCachingService.this, MemoryWipeService.class);
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(intent);
          else
            startService(intent);

          return null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }

  private void handleLockToggled() {
  }

  private void handleLocaleChanged() {
    dynamicLanguage.updateServiceLocale(this);
    foregroundService();
  }

  private static void startTimeoutIfAppropriate(@NonNull Context context) {
    boolean appVisible       = ApplicationContext.getInstance(context).isAppVisible();
    boolean secretSet        = KeyCachingService.masterSecret != null;

    long    screenTimeout    = TextSecurePreferences.getScreenLockTimeout(context);
    boolean screenLockActive = screenTimeout >= 60 && TextSecurePreferences.isScreenLockEnabled(context);

    if (!appVisible && secretSet && screenLockActive) {
      long screenLockTimeoutSeconds = TextSecurePreferences.getScreenLockTimeout(context);

      long timeoutMillis = TimeUnit.SECONDS.toMillis(screenLockTimeoutSeconds);

      Log.i(TAG, "Starting timeout: " + timeoutMillis);

      AlarmManager  alarmManager     = ServiceUtil.getAlarmManager(context);
      PendingIntent expirationIntent = buildExpirationPendingIntent(context);

      alarmManager.cancel(expirationIntent);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + timeoutMillis, expirationIntent);
      else
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + timeoutMillis, expirationIntent);
    }
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  private void foregroundServiceModern() {
    Log.i(TAG, "foregrounding KCS");
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationChannels.LOCKED_STATUS);

    builder.setContentTitle(getString(R.string.KeyCachingService_passphrase_cached));
    builder.setContentText(getString(R.string.KeyCachingService_signal_passphrase_cached));
    builder.setSmallIcon(R.drawable.icon_cached);
    builder.setWhen(0);
    builder.setPriority(Notification.PRIORITY_MIN);

    builder.addAction(R.drawable.ic_menu_lock_dark, getString(R.string.KeyCachingService_lock), buildLockIntent());
    builder.setContentIntent(buildLaunchIntent());

    stopForeground(true);
    startForeground(SERVICE_RUNNING_ID, builder.build());
  }

  private void foregroundServiceICS() {
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationChannels.LOCKED_STATUS);
    RemoteViews remoteViews            = new RemoteViews(getPackageName(), R.layout.key_caching_notification);

    remoteViews.setOnClickPendingIntent(R.id.lock_cache_icon, buildLockIntent());

    builder.setSmallIcon(R.drawable.icon_cached);
    builder.setContent(remoteViews);
    builder.setContentIntent(buildLaunchIntent());

    stopForeground(true);
    startForeground(SERVICE_RUNNING_ID, builder.build());
  }

  private void foregroundServiceLegacy() {
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationChannels.LOCKED_STATUS);
    builder.setSmallIcon(R.drawable.icon_cached);
    builder.setWhen(System.currentTimeMillis());

    builder.setContentTitle(getString(R.string.KeyCachingService_passphrase_cached));
    builder.setContentText(getString(R.string.KeyCachingService_signal_passphrase_cached));
    builder.setContentIntent(buildLaunchIntent());

    stopForeground(true);
    startForeground(SERVICE_RUNNING_ID, builder.build());
  }

  private void foregroundService() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      foregroundServiceModern();
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      foregroundServiceICS();
    } else {
      foregroundServiceLegacy();
    }
  }

  private void broadcastNewSecret() {
    Log.i(TAG, "Broadcasting new secret...");

    Intent intent = new Intent(NEW_KEY_EVENT);
    intent.setPackage(getApplicationContext().getPackageName());

    sendBroadcast(intent, KEY_PERMISSION);
  }

  private PendingIntent buildLockIntent() {
    Intent intent = new Intent(this, KeyCachingService.class);
    intent.setAction(PASSPHRASE_EXPIRED_EVENT);
    return PendingIntent.getService(getApplicationContext(), 0, intent, 0);
  }

  private PendingIntent buildLaunchIntent() {
    Intent intent              = new Intent(this, ConversationListActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    return PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
  }

  private static PendingIntent buildExpirationPendingIntent(@NonNull Context context) {
    Intent expirationIntent = new Intent(PASSPHRASE_EXPIRED_EVENT, null, context, KeyCachingService.class);
    return PendingIntent.getService(context, 0, expirationIntent, 0);
  }

  @Override
  public IBinder onBind(Intent arg0) {
    return binder;
  }

  public class KeySetBinder extends Binder {
    public KeyCachingService getService() {
      return KeyCachingService.this;
    }
  }
}
