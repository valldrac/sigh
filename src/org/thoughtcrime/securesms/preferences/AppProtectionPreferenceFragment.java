package org.thoughtcrime.securesms.preferences;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.widget.Toast;

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.ApplicationPreferencesActivity;
import org.thoughtcrime.securesms.BlockedContactsActivity;
import org.thoughtcrime.securesms.PassphraseChangeActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.SwitchPreferenceCompat;
import org.thoughtcrime.securesms.crypto.MasterSecretUtil;
import org.thoughtcrime.securesms.dependencies.InjectableType;
import org.thoughtcrime.securesms.jobs.MultiDeviceReadReceiptUpdateJob;
import org.thoughtcrime.securesms.lock.RegistrationLockDialog;
import org.thoughtcrime.securesms.service.KeyCachingService;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import mobi.upod.timedurationpicker.TimeDurationPickerDialog;

public class AppProtectionPreferenceFragment extends CorrectedPreferenceFragment implements InjectableType {

  private static final String PREFERENCE_CATEGORY_BLOCKED = "preference_category_blocked";

  @Inject
  SignalServiceAccountManager accountManager;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    ApplicationContext.getInstance(activity).injectDependencies(this);
  }

  @Override
  public void onCreate(Bundle paramBundle) {
    super.onCreate(paramBundle);

    this.findPreference(TextSecurePreferences.REGISTRATION_LOCK_PREF).setOnPreferenceClickListener(new AccountLockClickListener());
    this.findPreference(TextSecurePreferences.SCREEN_LOCK).setOnPreferenceChangeListener(new ScreenLockListener());
    this.findPreference(TextSecurePreferences.SCREEN_LOCK_TIMEOUT).setOnPreferenceClickListener(new ScreenLockTimeoutListener());

    this.findPreference(TextSecurePreferences.CHANGE_PASSPHRASE_PREF).setOnPreferenceClickListener(new ChangePassphraseClickListener());
    this.findPreference(TextSecurePreferences.READ_RECEIPTS_PREF).setOnPreferenceChangeListener(new ReadReceiptToggleListener());
    this.findPreference(PREFERENCE_CATEGORY_BLOCKED).setOnPreferenceClickListener(new BlockedContactsClickListener());
  }

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.preferences_app_protection);
  }

  @Override
  public void onResume() {
    super.onResume();
    ((ApplicationPreferencesActivity) getActivity()).getSupportActionBar().setTitle(R.string.preferences__privacy);

    initializeScreenLockTimeoutSummary();
  }

  private void initializeScreenLockTimeoutSummary() {
    long timeoutSeconds = TextSecurePreferences.getScreenLockTimeout(getContext());
    long hours          = TimeUnit.SECONDS.toHours(timeoutSeconds);
    long minutes        = TimeUnit.SECONDS.toMinutes(timeoutSeconds) - (TimeUnit.SECONDS.toHours(timeoutSeconds) * 60  );
    long seconds        = TimeUnit.SECONDS.toSeconds(timeoutSeconds) - (TimeUnit.SECONDS.toMinutes(timeoutSeconds) * 60);

    findPreference(TextSecurePreferences.SCREEN_LOCK_TIMEOUT)
        .setSummary(timeoutSeconds <= 0 ? getString(R.string.AppProtectionPreferenceFragment_none) :
                                          String.format("%02d:%02d:%02d", hours, minutes, seconds));
  }

  private class ScreenLockListener implements Preference.OnPreferenceChangeListener {
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      boolean enabled = (Boolean)newValue;
      TextSecurePreferences.setScreenLockEnabled(getContext(), enabled);

      Intent intent = new Intent(getContext(), KeyCachingService.class);
      intent.setAction(KeyCachingService.LOCK_TOGGLED_EVENT);
      getContext().startService(intent);
      return true;
    }
  }

  private class ScreenLockTimeoutListener implements Preference.OnPreferenceClickListener {

    @Override
    public boolean onPreferenceClick(Preference preference) {
      new TimeDurationPickerDialog(getContext(), (view, duration) -> {
        if (duration == 0) {
          TextSecurePreferences.setScreenLockTimeout(getContext(), 0);
        } else {
          long timeoutSeconds = Math.max(TimeUnit.MILLISECONDS.toSeconds(duration), 60);
          TextSecurePreferences.setScreenLockTimeout(getContext(), timeoutSeconds);
        }

        initializeScreenLockTimeoutSummary();
      }, 0).show();

      return true;
    }
  }

  private class AccountLockClickListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(Preference preference) {
      if (((SwitchPreferenceCompat)preference).isChecked()) {
        RegistrationLockDialog.showRegistrationUnlockPrompt(getContext(), (SwitchPreferenceCompat)preference, accountManager);
      } else {
        RegistrationLockDialog.showRegistrationLockPrompt(getContext(), (SwitchPreferenceCompat)preference, accountManager);
      }

      return true;
    }
  }

  private class BlockedContactsClickListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(Preference preference) {
      Intent intent = new Intent(getActivity(), BlockedContactsActivity.class);
      startActivity(intent);
      return true;
    }
  }

  private class ReadReceiptToggleListener implements Preference.OnPreferenceChangeListener {
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      boolean enabled = (boolean)newValue;
      ApplicationContext.getInstance(getContext())
                        .getJobManager()
                        .add(new MultiDeviceReadReceiptUpdateJob(getContext(), enabled));

      return true;
    }
  }

  public static CharSequence getSummary(Context context) {
    final int    privacySummaryResId = R.string.ApplicationPreferencesActivity_privacy_summary;
    final String onRes               = context.getString(R.string.ApplicationPreferencesActivity_on);
    final String offRes              = context.getString(R.string.ApplicationPreferencesActivity_off);

    if (TextSecurePreferences.isRegistrationtLockEnabled(context)) {
      return context.getString(privacySummaryResId, onRes, onRes);
    } else {
      return context.getString(privacySummaryResId, onRes, offRes);
    }
  }

  // Derecated

  private class ChangePassphraseClickListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(Preference preference) {
      if (MasterSecretUtil.isPassphraseInitialized(getActivity())) {
        startActivity(new Intent(getActivity(), PassphraseChangeActivity.class));
      } else {
        Toast.makeText(getActivity(),
                       R.string.ApplicationPreferenceActivity_you_havent_set_a_passphrase_yet,
                       Toast.LENGTH_LONG).show();
      }

      return true;
    }
  }

}
