package org.thoughtcrime.securesms.crypto;


import android.support.annotation.NonNull;

import org.thoughtcrime.securesms.service.KeyCachingService;
import org.thoughtcrime.securesms.util.Hex;
import org.thoughtcrime.securesms.util.Util;

import java.io.IOException;

public class DatabaseSecret {

  private final byte[] key;

  public DatabaseSecret(@NonNull byte[] key) {
    this.key = key;
  }

  public DatabaseSecret(@NonNull String encoded) throws IOException {
    this.key = Hex.fromStringCondensed(encoded);
  }

  public String asString() {
    return Hex.toStringCondensed(asBytes());
  }

  public byte[] asBytes() {
    MasterSecret masterSecret = KeyCachingService.getMasterSecret();
    return Util.combine(key, masterSecret.getEncryptionKey().getEncoded());
  }
}
