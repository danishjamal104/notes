package com.github.danishjamal104.notes.data.backupandrestore

import com.github.danishjamal104.notes.util.encryption.EncryptionHelper
import com.github.danishjamal104.notes.util.encryption.KeyHelper
import com.github.danishjamal104.notes.util.decodeFromBase64
import javax.crypto.SecretKey

class PassKeyProcessor {

    var baseKey = ""
    var rotationFactor = -1
    var ivString = ""

    fun getSecret(): SecretKey {
        return KeyHelper.getSecretKey(baseKey)
    }

    override fun toString(): String {
        return "PassKeyProcessor(baseKey='$baseKey', rotationFactor=$rotationFactor, ivString='$ivString')"
    }


    companion object {

        fun load(key: String, storeInKeystore: Boolean = false): PassKeyProcessor {
            val b64DecodedKey = key.replace("-","").decodeFromBase64()
            val parts = b64DecodedKey.split("-")
            val baseKey = parts[0].substring(0, parts[0].length-1)
            val rotationFactor = parts[0][parts[0].length-1].toString()
            val ivString = parts[1]
            if (storeInKeystore) {
                KeyHelper.storeKey(
                    baseKey,
                    EncryptionHelper.getKeyFromPassword(baseKey, baseKey.reversed())
                )
            }
            return PassKeyProcessor().apply {
                this.baseKey = baseKey
                this.rotationFactor = rotationFactor.toInt()
                this.ivString = ivString
            }
        }

    }
}