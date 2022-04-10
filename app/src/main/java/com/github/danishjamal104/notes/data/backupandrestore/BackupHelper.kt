package com.github.danishjamal104.notes.data.backupandrestore

import com.github.danishjamal104.notes.data.model.Note
import com.github.danishjamal104.notes.util.encryption.EncryptionHelper
import com.github.danishjamal104.notes.util.decrypt
import com.github.danishjamal104.notes.util.encrypt
import com.google.gson.Gson
import com.google.gson.JsonObject

object BackupHelper {

    private val gson = Gson()

    /**
     * Converts the [notes] into text representational form and encrypts the entire data using
     * [secretKey]. The data returned can directly be stored in files and can later be retrieved
     * only using the same [secretKey]
     * @param notes list of notes
     * @param secretKey the secret key to be used for encrypting the data
     * @return [String] encrypted string data to be stored as a backup
     */
    fun createBackupData(notes: List<Note>, secretKey: String): String {
        val originalPassKey = PassKeyProcessor.load(secretKey)
        val time = System.currentTimeMillis()

        val valueEncodingString = EncryptionHelper.generateEncryptionKey(
            "${time + notes.size}", originalPassKey.ivString, originalPassKey.rotationFactor
        )

        val jsonData = JsonObject()
        val notesJson = gson.toJson(notes)

        jsonData.addProperty("size", notes.size)
        jsonData.addProperty("timestamp", time)
        jsonData.addProperty("value", notesJson.encrypt(valueEncodingString))

        val stringData: String = jsonData.toString()
        return stringData.encrypt(secretKey)
    }

    /**
     * Restores the backup data by decrypting it using the [secretKey]
     * @param data encrypted data
     * @param secretKey the same key used for encrypting this data
     * @return [List] list of notes
     */
    fun restoreBackupData(data: String, secretKey: String): List<Note> {
        val originalPassKey = PassKeyProcessor.load(secretKey)
        val jsonString = data.decrypt(secretKey)
        val jsonData = gson.fromJson(jsonString, JsonObject::class.java)

        val time = jsonData.get("timestamp").asLong
        val size = jsonData.get("size").asInt

        val notesDecodingString = EncryptionHelper.generateEncryptionKey(
            "${time+size}", originalPassKey.ivString, originalPassKey.rotationFactor
        )
        val notesJson = jsonData.get("value").asString.decrypt(notesDecodingString)
        val notes = gson.fromJson(notesJson, Array<Note>::class.java)
        return notes.asList()
    }
}