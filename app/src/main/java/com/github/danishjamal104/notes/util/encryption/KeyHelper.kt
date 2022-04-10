package com.github.danishjamal104.notes.util.encryption

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProtection
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object KeyHelper {

    /**
     * Generates the secret key using [KeyGenerator]
     * @param keyGenParameterSpec spec to be used for generating key
     * @return [SecretKey] the [KeyStore] backed secret key
     */
    fun generateSecretKey(keyGenParameterSpec: KeyGenParameterSpec): SecretKey? {
        val keygen = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        ).apply { init(keyGenParameterSpec) }
        return keygen.generateKey()
    }

    /**
     * Fetches the secret key from the [KeyStore] based on [keyName]
     * @param keyName name of the key
     */
    fun getSecretKey(keyName: String): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        // Before the keystore can be accessed, it must be loaded.
        keyStore.load(null)
        return keyStore.getKey(keyName, null) as SecretKey
    }

    /**
     * Adds the encryption and decryption secret key in [KeyStore]
     * @param keyName name of the key
     * @param secretKey instance of [SecretKey]
     */
    fun storeKey(keyName: String, secretKey: SecretKey) {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        keyStore.setEntry(
            keyName,
            KeyStore.SecretKeyEntry(secretKey),
            KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setRandomizedEncryptionRequired(false)
                .build()
        )
    }

    /**
     * Builds the default parameter spec for key generation
     * @param keyName name of the key
     * @return [KeyGenParameterSpec] parameter spec for key generation
     */
    fun getDefaultKeyGenParameterSpec(keyName: String): KeyGenParameterSpec {
        return KeyGenParameterSpec.Builder(
            keyName,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(128)
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(true)
            .setRandomizedEncryptionRequired(false)
            .build()
    }

}