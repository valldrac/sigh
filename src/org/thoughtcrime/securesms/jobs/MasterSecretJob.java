package org.thoughtcrime.securesms.jobs;

import android.content.Context;
import android.support.annotation.NonNull;

import org.thoughtcrime.securesms.crypto.MasterSecret;
import org.thoughtcrime.securesms.jobmanager.JobParameters;
import org.thoughtcrime.securesms.service.KeyCachingService;

import androidx.work.WorkerParameters;

public abstract class MasterSecretJob extends ContextJob {

  public MasterSecretJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
    super(context, workerParameters);
  }

  public MasterSecretJob(@NonNull Context context, @NonNull JobParameters parameters) {
    super(context, parameters);
  }

  @Override
  public void onRun() throws Exception {
    MasterSecret masterSecret = getMasterSecret();
    onRun(masterSecret);
  }

  @Override
  public boolean onShouldRetry(Exception exception) {
    if (exception instanceof RequirementNotMetException) return true;
    return onShouldRetryThrowable(exception);
  }

  public abstract void onRun(MasterSecret masterSecret) throws Exception;
  public abstract boolean onShouldRetryThrowable(Exception exception);

  private MasterSecret getMasterSecret() throws RequirementNotMetException {
    if (KeyCachingService.isLocked()) throw new RequirementNotMetException();
    return KeyCachingService.getMasterSecret();
  }

  protected static class RequirementNotMetException extends Exception {}

}
