package com.github.danishjamal104.notes.util.encryption

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.danishjamal104.notes.data.backupandrestore.PassKeyProcessor
import javax.crypto.Cipher

object BiometricEncryption {

    fun encrypt(
        fragment: Fragment,
        textToEncrypt: String,
        key: String,
        result: (success: Boolean, data: String) -> Unit
    ) {
        return performBiometricAuth(Cipher.ENCRYPT_MODE, fragment, textToEncrypt, key, result)
    }

    fun decrypt(
        fragment: Fragment,
        textToEncrypt: String,
        key: String,
        result: (success: Boolean, data: String) -> Unit
    ) {
        return performBiometricAuth(Cipher.DECRYPT_MODE, fragment, textToEncrypt, key, result)
    }

    private fun performBiometricAuth(
        mode: Int, fragment: Fragment,
        textToEncrypt: String,
        key: String,
        result: (success: Boolean, data: String) -> Unit
    ) {
        if (mode != Cipher.ENCRYPT_MODE && mode != Cipher.DECRYPT_MODE) {
            throw Exception("Invalid mode for performing biometric auth")
        }
        val title = "Encryption"
        val subtitle = "Encrypting the data requires the biometric authentication"
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        val executor = ContextCompat.getMainExecutor(fragment.requireContext())
        val biometricPrompt = BiometricPrompt(fragment, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    result.invoke(false, errString.toString())
                }

                override fun onAuthenticationSucceeded(
                    res: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(res)
                    val cipher = res.cryptoObject?.cipher
                    if (cipher == null) {
                        result.invoke(false, "Invalid key")
                    } else {
                        val encData = when (mode) {
                            Cipher.ENCRYPT_MODE -> EncryptionHelper.encryptFromCipher(
                                textToEncrypt,
                                cipher
                            )
                            Cipher.DECRYPT_MODE -> EncryptionHelper.decryptFromCipher(
                                textToEncrypt,
                                cipher
                            )
                            else -> EncryptionHelper.encryptFromCipher(textToEncrypt, cipher)
                        }
                        result.invoke(true, encData)
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    result.invoke(false, "Authentication failed")
                }
            })
        val keyProcessor = PassKeyProcessor.load(key, true)
        val secret = keyProcessor.getSecret()
        val iv = EncryptionHelper.generateIv(keyProcessor.ivString)
        val cipher = EncryptionHelper.getCipher()
            .apply { init(mode, secret, iv) }
        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

}