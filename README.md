# Sigh Android

A fork of [Signal Android](https://github.com/signalapp/Signal-Android) that encrypts app data and keys with a passphrase.

Some time ago, Signal supported **user passphrase for securing data on disk independently** of [Android FDE](https://source.android.com/security/encryption/full-disk). But this option was deliberately removed. Now we bring it back again with some additional security features.

## Features

* Screen is locked with **user passphrase**.
* **Database**, identity keys and persistent jobs are **encrypted** with the master secret.
* **Master secret** is encrypted and authenticated with user passphrase.
* Application is **auto-locked** after 15 minutes of inactivity. Because clearing secrets from memory in Android is hard it **kills the whole process**. It is expected that RAM-hungry apps will overwrite freed memory.
* Local encryption is upgraded to **AES-256** and **SHA-256**.
* **Privacy settings** are enabled by default.

## FAQ

### Q: How does Signal protect data at rest?

Signal encrypts data on the device but it stores the encryption key in the [KeyStore](https://developer.android.com/training/articles/keystore).

Signal screen lock is just a screen lock not connected with any kind of encryption.

Identity key pair is stored in plain text in [Preferences](https://developer.android.com/training/data-storage/shared-preferences).

### Q: Can I install it along with Signal?

Yes, you can install and run both apps on the same device. The app identifier was changed to `Sigh` so they will not share storage or configuration.

## Bugs

Database migrations for older versions were removed.

Master secret and database encryption key are never rotated. Changing your password will NOT rotate them.

## Disclaimer

License and legal notices in the original [README](README-ORIG.md).

For educational and informational purposes only.
