package com.github.danishjamal104.notes.backgroundtask

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.danishjamal104.notes.data.local.CacheDataSourceImpl
import com.github.danishjamal104.notes.data.local.Database
import com.github.danishjamal104.notes.data.mapper.NoteMapper
import com.github.danishjamal104.notes.data.mapper.UserMapper
import com.github.danishjamal104.notes.data.model.Note
import com.github.danishjamal104.notes.data.repository.note.NotesRepositoryImpl
import com.github.danishjamal104.notes.util.AppConstant
import com.github.danishjamal104.notes.util.ServiceResult
import com.github.danishjamal104.notes.util.encodeToBase64
import com.github.danishjamal104.notes.util.sharedpreference.UserPreferences
import com.google.gson.Gson
import com.google.gson.JsonObject
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File
import java.io.FileWriter
import java.io.IOException


@Suppress("DEPRECATION")
class BackupWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    private val _db =
        Room.databaseBuilder(applicationContext, Database::class.java, AppConstant.Database.DB_NAME)
            .fallbackToDestructiveMigration()
            .build()
    private val db get() = _db

    private val userDao get() = db.userDao()
    private val noteDao get() = db.noteDao()

    private val _userPreferences = UserPreferences(applicationContext)
    private val userPreferences get() = _userPreferences

    private val _cacheDataSource =  CacheDataSourceImpl(UserMapper(),
        NoteMapper(), userDao, noteDao)
    private val cacheDataSource get() = _cacheDataSource

    private val _noteRepository = NotesRepositoryImpl(cacheDataSource, userPreferences)
    private val notesRepository = _noteRepository

    private val _gson = Gson()
    private val gson = _gson

    override suspend fun doWork(): Result {
        log("Started work")
        val jsonData = JsonObject()
        val notes = getAllNotes()
        val notesJson = gson.toJson(notes)

        val time = System.currentTimeMillis()
        jsonData.addProperty("size", notes.size)
        jsonData.addProperty("timestamp", time)
        jsonData.addProperty("value", notesJson)

        val stringData: String = jsonData.toString()
        log(stringData)
        val data = stringData.encodeToBase64()
        log(data)
        storeBackupToFile("$time", data)
        return Result.success()
    }

    private suspend fun getAllNotes(): List<Note> {
        return when (val result = notesRepository.getNotes()) {
            is ServiceResult.Error -> throw Exception("Failed to fetch notes")
            is ServiceResult.Success -> result.data
        }
    }

    private fun storeBackupToFile(sFileName: String, sBody: String) {
        try {
            val root = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "")
            if (!root.exists()) {
                root.mkdirs()
            }
            val file = File(root, "$sFileName.txt")
            val writer = FileWriter(file)
            writer.append(sBody)
            writer.flush()
            writer.close()
            log("File at: " + file.absolutePath)
            createZip(sFileName, file)
            file.delete()
        } catch (e: IOException) {
            e.printStackTrace()
            log(e.localizedMessage as String)
        }
    }

    private fun createZip(filename: String, file: File) {
        try {
            val zipParameters = ZipParameters()
            zipParameters.isEncryptFiles = true
            zipParameters.encryptionMethod = EncryptionMethod.AES
            zipParameters.aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256

            val zip = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "$filename.zip")
            ZipFile(zip, "password".toCharArray()).addFile(file, zipParameters)
            log("File at: " + zip.absolutePath)
        } catch (e: IOException) {
            e.printStackTrace()
            log(e.localizedMessage as String)
        }
    }

    private fun log(text: String) {
        Log.i("BackupWorker", text)
    }

}