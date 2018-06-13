/**
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
package org.thoughtcrime.securesms;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.thoughtcrime.securesms.crypto.IdentityKeyUtil;
import org.thoughtcrime.securesms.crypto.MasterSecret;
import org.thoughtcrime.securesms.crypto.MasterSecretUtil;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.VersionTracker;

/**
 * Activity for creating a user's local encryption passphrase.
 *
 * @author Moxie Marlinspike
 */

public class PassphraseCreateActivity extends PassphraseActivity {

  private DynamicTheme    dynamicTheme    = new DynamicTheme();
  private DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private LinearLayout createLayout;
  private LinearLayout progressLayout;

  private EditText newPassphrase;
  private EditText repeatPassphrase;
  private Button   okButton;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
    super.onCreate(savedInstanceState);

    setContentView(R.layout.create_passphrase_activity);

    initializeResources();
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
  }

  private void initializeResources() {
    this.createLayout            = (LinearLayout) findViewById(R.id.create_layout   );
    this.progressLayout          = (LinearLayout) findViewById(R.id.progress_layout );

    this.newPassphrase           = (EditText) findViewById(R.id.new_passphrase      );
    this.repeatPassphrase        = (EditText) findViewById(R.id.repeat_passphrase   );
    this.okButton                = (Button  ) findViewById(R.id.ok_button           );

    this.okButton.setOnClickListener(new OkButtonClickListener());
  }

  private void verifyAndSavePassphrases() {
    Editable newText      = this.newPassphrase.getText();
    Editable repeatText   = this.repeatPassphrase.getText();

    String passphrase       = (newText == null ? "" : newText.toString());
    String passphraseRepeat = (repeatText == null ? "" : repeatText.toString());

    if (!passphrase.equals(passphraseRepeat)) {
      this.newPassphrase.setText("");
      this.repeatPassphrase.setText("");
      this.newPassphrase.setError(getString(R.string.PassphraseCreateActivity_passphrases_dont_match_exclamation));
      this.newPassphrase.requestFocus();
    } else if (passphrase.equals("")) {
      this.newPassphrase.setError(getString(R.string.PassphraseCreateActivity_enter_new_passphrase_exclamation));
      this.newPassphrase.requestFocus();
    } else {
      new SecretGenerator(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, passphrase);
    }
  }

  private class OkButtonClickListener implements View.OnClickListener {
    public void onClick(View v) {
      verifyAndSavePassphrases();
    }
  }

  private class SecretGenerator extends AsyncTask<String, Void, MasterSecret> {
    private final Context context;

    public SecretGenerator(Context context) {
      this.context = context;
    }

    @Override
    protected void onPreExecute() {
      createLayout.setVisibility(View.GONE);
      progressLayout.setVisibility(View.VISIBLE);
    }

    @Override
    protected MasterSecret doInBackground(String... params) {
      String passphrase = params[0];

      MasterSecret masterSecret = MasterSecretUtil.generateMasterSecret(context, passphrase);

      if (masterSecret != null) {
        MasterSecretUtil.generateAsymmetricMasterSecret(context, masterSecret);
        IdentityKeyUtil.generateIdentityKeys(context, masterSecret);
        VersionTracker.updateLastSeenVersion(context);

        TextSecurePreferences.setLastExperienceVersionCode(context, Util.getCurrentApkReleaseVersion(context));
        TextSecurePreferences.setReadReceiptsEnabled(context, true);
      }

      return masterSecret;
    }

    @Override
    protected void onPostExecute(MasterSecret masterSecret) {
      if (masterSecret != null) {
        setMasterSecret(masterSecret);
      }
    }
  }

  @Override
  protected void cleanup() {
    this.newPassphrase    = null;
    this.repeatPassphrase = null;

    System.gc();
  }
}
