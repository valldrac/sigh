package org.thoughtcrime.securesms.jobs.requirements;

import android.content.Context;

import org.thoughtcrime.securesms.jobqueue.dependencies.ContextDependent;
import org.thoughtcrime.securesms.jobqueue.requirements.NetworkRequirement;
import org.thoughtcrime.securesms.jobqueue.requirements.Requirement;

public class NetworkOrServiceRequirement implements Requirement, ContextDependent {

  private transient Context context;

  public NetworkOrServiceRequirement(Context context) {
    this.context = context;
  }

  @Override
  public void setContext(Context context) {
    this.context = context;
  }

  @Override
  public boolean isPresent() {
    NetworkRequirement networkRequirement = new NetworkRequirement(context);
    ServiceRequirement serviceRequirement = new ServiceRequirement(context);

    return networkRequirement.isPresent() || serviceRequirement.isPresent();
  }
}
