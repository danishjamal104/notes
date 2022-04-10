package com.github.danishjamal104.notes.util.encryption

import android.annotation.SuppressLint
import android.security.keystore.KeyProperties
import com.github.danishjamal104.notes.util.encodeToBase64
import com.github.danishjamal104.notes.util.toSHA1
import com.github.danishjamal104.notes.data.backupandrestore.PassKeyProcessor
import java.nio.charset.Charset
import java.security.spec.KeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@SuppressLint("NewApi")
object EncryptionHelper {

    /**
     * Generates printable encryption key which can be used for creating backup and performing
     * restore task. This key can be directly loaded into [PassKeyProcessor] and can see the
     * processed data.
     * @param baseKey the plain text
     * @param ivString the string to be used for generating [IvParameterSpec]
     * @param rotationFactor the factor for generating file based password
     * @return [String] the Base64 encoded string
     */
    fun generateEncryptionKey(baseKey: String? = generatePassword(),
                              ivString: String? = null,
                              rotationFactor: Int? = (1..9).random()): String {
        val iv = ivString?.let {
            generateIv(it)
        } ?: generateIv()
        var password = "$baseKey$rotationFactor-${String(iv.iv, Charset.defaultCharset())}"
        password = password.encodeToBase64().trim().replace("=", "")

        var fl = ""
        val step = 6
        var i = 0
        var j = 6
        while (j < password.length) {
            fl += password.substring(i, j) + "-"
            i = j
            if (j+step < password.length) {
                j += step
            } else {
                fl += password.substring(i, password.length)
                break
            }
        }
        return fl
    }

    /**
     * Generates a [size] char long random password using all english alphabets in case sensitive
     * manner and numbers from 0-9
     * @param size length of the password defaults to 8
     * @return [String] the random password as plain text
     */
    fun generatePassword(size: Int = 8): String {
        val set = ('a'..'z').toMutableList() +
                ('A'..'Z').toMutableList() +
                ('0'..'9').toMutableList()
        var password = ""
        for(i in 1..size) {
            password += set[(0 until set.size-1).random()]
        }
        return password
    }

    /**
     * Generates a 5 char long password by rotating the baseId with the factor of [rotationFactor]
     * This algorithm will result in same password for [baseId] with same [rotationFactor]
     * @param rotationFactor the number fo times rotation has tobe performed
     * @return [String] the string based valid password, can be used for files
     */
    fun generateFilePassword(baseId: String, rotationFactor: Int = (2..9).random()): String {
        var rotation = baseId
        for (i in 1..rotationFactor) {
            rotation = rotation.substring(0,5).toSHA1()
        }
        return rotation.substring(10, 15) + "$rotationFactor"
    }

    /**
     * Generates [SecretKey] instance using custom password and salt
     * @param password the base key used for generating secret
     * @param salt the extra text to impose randomness in generated secret key
     * @return [SecretKey] An instance of the [SecretKey]
     */
    fun getKeyFromPassword(password: String, salt: String): SecretKey {
        val factory: SecretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec: KeySpec = PBEKeySpec(
            password.toCharArray(), salt.toByteArray(), 65536, 256)
        return SecretKeySpec(
            factory.generateSecret(spec)
                .encoded, "AES"
        )
    }

    /**
     * Generates instance of [IvParameterSpec] using plain text
     * When [inputData] is null it generates random 16 char long string using all english
     * alphabets in case sensitive manner
     * @param inputData text to be used for generating iv
     * @return [IvParameterSpec] An instance of [IvParameterSpec]
     */
    fun generateIv(inputData: String? = null): IvParameterSpec {
        if(inputData != null) {
            return IvParameterSpec(inputData.toByteArray())
        }
        val set = ('a'..'z').toMutableList() + ('A'..'Z').toMutableList()
        var data = ""
        for (i in 1..16) {
            data += set[(0..51).random()]
        }
        val iv = data.toByteArray()
        return IvParameterSpec(iv)
    }

    /**
     * Applies given cipher on a plain text
     * @param input text to be encoded
     * @param cipher teh instance of the [Cipher]
     * @return [String] encrypted data as a Base64 encoded text
     */
    fun encryptFromCipher(input: String, cipher: Cipher): String {
        val cipherText: ByteArray = cipher.doFinal(input.toByteArray())
        return Base64.getEncoder().encodeToString(cipherText)
    }

    /**
     * Applies given cipher on a Base64 encoded data
     * @param cipherText encoded text on which cipher is to be applied
     * @param cipher teh instance of the [Cipher]
     * @return [String] decrypted data as a plain text
     */
    fun decryptFromCipher(cipherText: String, cipher: Cipher): String {
        val plainText = cipher.doFinal(
            Base64.getDecoder()
                .decode(cipherText)
        )
        return String(plainText)
    }

    /**
     * Encrypts the string data using [key] (SecretKey) and [iv] (IvParameterSpec)
     * @param input text to be encoded
     * @param key secret key to be used for encryption
     * @param iv iv to be used for encryption
     * @return [String] encoded data as plain text
     */
    fun encrypt(
       input: String, key: SecretKey, iv: IvParameterSpec
    ): String {
        val cipher: Cipher = getCipher()
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        return encryptFromCipher(input, cipher)
    }

    /**
     * Decrypts the string data using [key] (SecretKey) and [iv] (IvParameterSpec)
     * @param cipherText encoded data in [String] format
     * @param key same secret key used for encryption
     * @param iv same iv used for encryption
     * @return [String] decoded data as plain text
     */
    fun decrypt(
        cipherText: String, key: SecretKey, iv: IvParameterSpec
    ): String {
        val cipher = getCipher()
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        return decryptFromCipher(cipherText, cipher)
    }

    /**
     * @return [Cipher] returns the default instance of [Cipher]
     */
    fun getCipher(): Cipher {
        return Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                + KeyProperties.BLOCK_MODE_CBC + "/"
                + KeyProperties.ENCRYPTION_PADDING_PKCS7)
    }

}