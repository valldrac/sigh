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
package org.thoughtcrime.securesms.crypto;

import android.os.Parcel;
import android.os.Parcelable;

import javax.security.auth.Destroyable;

import java.util.Arrays;

/**
 * When a user first initializes TextSecure, a few secrets
 * are generated.  These are:
 *
 * 1) A 256bit symmetric encryption key.
 * 2) A 256bit symmetric MAC key.
 * 3) An ECC keypair.
 *
 * The first two, along with the identity ECC keypair, are
 * then encrypted on disk using PBE.
 *
 * This class represents 1 and 2.
 *
 * @author Moxie Marlinspike
 */

public class MasterSecret implements Parcelable, Destroyable {

  private final SecureSecretKeySpec encryptionKey;
  private final SecureSecretKeySpec macKey;

  public static final Parcelable.Creator<MasterSecret> CREATOR = new Parcelable.Creator<MasterSecret>() {
    @Override
    public MasterSecret createFromParcel(Parcel in) {
      return new MasterSecret(in);
    }

    @Override
    public MasterSecret[] newArray(int size) {
      return new MasterSecret[size];
    }
  };

  public MasterSecret(SecureSecretKeySpec encryptionKey, SecureSecretKeySpec macKey) {
    this.encryptionKey = encryptionKey;
    this.macKey        = macKey;
  }

  private MasterSecret(Parcel in) {
    byte[] encryptionKeyBytes = new byte[in.readInt()];
    in.readByteArray(encryptionKeyBytes);

    byte[] macKeyBytes = new byte[in.readInt()];
    in.readByteArray(macKeyBytes);

    this.encryptionKey = new SecureSecretKeySpec(encryptionKeyBytes, "AES");
    this.macKey        = new SecureSecretKeySpec(macKeyBytes, "HmacSHA256");

    // SecretKeySpec does an internal copy in its constructor.
    Arrays.fill(encryptionKeyBytes, (byte) 0x00);
    Arrays.fill(macKeyBytes, (byte)0x00);
  }


  public SecureSecretKeySpec getEncryptionKey() {
    return this.encryptionKey;
  }

  public SecureSecretKeySpec getMacKey() {
    return this.macKey;
  }

  @Override
  public void writeToParcel(Parcel out, int flags) {
    out.writeInt(encryptionKey.getEncoded().length);
    out.writeByteArray(encryptionKey.getEncoded());
    out.writeInt(macKey.getEncoded().length);
    out.writeByteArray(macKey.getEncoded());
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public MasterSecret parcelClone() {
    Parcel thisParcel = Parcel.obtain();
    Parcel thatParcel = Parcel.obtain();
    byte[] bytes      = null;

    thisParcel.writeValue(this);
    bytes = thisParcel.marshall();

    thatParcel.unmarshall(bytes, 0, bytes.length);
    thatParcel.setDataPosition(0);

    MasterSecret that = (MasterSecret)thatParcel.readValue(MasterSecret.class.getClassLoader());

    thisParcel.recycle();
    thatParcel.recycle();

    return that;
  }

  @Override
  public void destroy() {
    if (encryptionKey != null) encryptionKey.destroy();
    if (macKey        != null) macKey.destroy();
  }

  @Override
  public boolean isDestroyed() {
    return (encryptionKey == null || encryptionKey.isDestroyed())
            && (macKey == null || macKey.isDestroyed());
  }
}
