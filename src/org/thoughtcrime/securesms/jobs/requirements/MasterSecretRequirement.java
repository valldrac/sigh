package org.thoughtcrime.securesms.jobs.requirements;

import android.content.Context;

import org.thoughtcrime.securesms.service.KeyCachingService;
import org.thoughtcrime.securesms.jobqueue.dependencies.ContextDependent;
import org.thoughtcrime.securesms.jobqueue.requirements.Requirement;

public class MasterSecretRequirement implements Requirement, ContextDependent {

  private transient Context context;

  public MasterSecretRequirement(Context context) {
    this.context = context;
  }

  @Override
  public boolean isPresent() {
    return !KeyCachingService.isLocked();
  }

  @Override
  public void setContext(Context context) {
    this.context = context;
  }
}
