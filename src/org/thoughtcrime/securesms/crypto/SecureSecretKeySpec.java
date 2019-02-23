package org.thoughtcrime.securesms.crypto;

import java.lang.reflect.Field;
import java.util.Arrays;

import javax.crypto.spec.SecretKeySpec;

/**
 * Replacement for SecretKeySpec that implements Destroyable interface
 * and securely clears secret key from memory.
 */
public class SecureSecretKeySpec extends SecretKeySpec {
    private boolean zeroed;

    public SecureSecretKeySpec(byte[] key, String algorithm) {
        super(key, algorithm);
    }

    public SecureSecretKeySpec(byte[] key, int offset, int len, String algorithm) {
        super(key, offset, len, algorithm);
    }

    @Override
    public void destroy() {
        try {
            super.destroy();
        } catch (Exception | Error e) {
            clearSecretKeyArray();
        }
    }

    @Override
    public boolean isDestroyed() {
        try {
            return super.isDestroyed();
        } catch (Exception | Error e) {
            return zeroed;
        }
    }

    /**
     * Use reflection to clear the underlying private key from memory.
     */
    private void clearSecretKeyArray() {
        try {
            Field f = SecretKeySpec.class.getDeclaredField("key");
            f.setAccessible(true);
            byte[] key = (byte[]) f.get(this);
            Arrays.fill(key, (byte) 0x00);
            zeroed = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
