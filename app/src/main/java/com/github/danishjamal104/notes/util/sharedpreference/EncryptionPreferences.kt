package com.github.danishjamal104.notes.util.sharedpreference

import android.content.Context
import android.util.Log
import com.github.danishjamal104.notes.util.encryption.EncryptionHelper
import dagger.hilt.android.qualifiers.ApplicationContext

class EncryptionPreferences
constructor(@ApplicationContext context: Context,
            private val userPreferences: UserPreferences): PreferenceManager(context) {

    private val _enc = getEncryptionKey()
    val key = _enc

    private fun fetchKey(key: String) = get(Key.Custom(key), "")

    private fun putKey(keyName: String, keyValue: String) = put(Key.Custom(keyName), keyValue)

    private fun getEncryptionKey(): String {
        val userId = userPreferences.getUserId()
        var encryptionKey = fetchKey(userId)
        return if(encryptionKey == "") {
            Log.i("homew", "Generating and inserting new")
            putKey(userId, EncryptionHelper.generateEncryptionKey())
            getEncryptionKey()
        } else {
            encryptionKey
        }
    }


}