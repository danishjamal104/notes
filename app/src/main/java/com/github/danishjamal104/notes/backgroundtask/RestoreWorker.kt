package com.github.danishjamal104.notes.backgroundtask

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.WorkerParameters
import com.github.danishjamal104.notes.data.backupandrestore.BackupHelper
import com.github.danishjamal104.notes.data.backupandrestore.PassKeyProcessor
import com.github.danishjamal104.notes.data.model.Note
import com.github.danishjamal104.notes.util.AppConstant
import com.github.danishjamal104.notes.util.FileUtil
import com.github.danishjamal104.notes.util.ServiceResult
import com.github.danishjamal104.notes.util.encryption.EncryptionHelper
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import java.io.File


@Suppress("DEPRECATION")
class RestoreWorker(ctx: Context, params: WorkerParameters) : BaseCoroutineNoteWorker(ctx, params) {

    lateinit var key: String
    private lateinit var filePath: String
    private lateinit var fileName: String
    private lateinit var fileUri: Uri
    private lateinit var passKeyProcessor: PassKeyProcessor

    private val restorationPath = File(
        applicationContext.externalCacheDir!!.absolutePath
                + "/restore"
    ).absolutePath

    override suspend fun doWork(): Result {
        progressNotificationTitle = "Restoring Backup"
        updateProgress()
        key = inputData.getString(AppConstant.Worker.KEY) ?: return Result.failure()
        val fileUriString =
            inputData.getString(AppConstant.Worker.FILE_URI) ?: return Result.failure()
        fileUri = Uri.parse(fileUriString)
        filePath = getFilePath(fileUri) ?: return Result.failure()
        fileName = getFileName(fileUri) ?: return Result.failure()
        passKeyProcessor = PassKeyProcessor.load(key)
        updateProgress(5)
        restore()
        return Result.success()
    }

    private suspend fun restore() {
        when (val unzipResult = unzipBackup()) {
            is ServiceResult.Error -> {
                makeDefaultTextNotification("Restore Failed", unzipResult.reason)
                return
            }
            is ServiceResult.Success -> Unit
        }
        updateProgress(4)
        when (val res = databaseCleanUp()) {
            is ServiceResult.Error -> {
                makeDefaultTextNotification(
                    "Restore Failed",
                    "Database maybe tampered or corrupted"
                )
                Log.i("SECUREDINFO", res.reason)
                return
            }
            is ServiceResult.Success -> Unit
        }
        updateProgress(1)
        val readDataResult = readData()
        when (readDataResult) {
            is ServiceResult.Error -> {
                makeDefaultTextNotification(
                    "Restore Failed",
                    readDataResult.reason
                )
                return
            }
            is ServiceResult.Success -> Unit
        }
        updateProgress(45)
        when (val insertResult = insertData(readDataResult.data)) {
            is ServiceResult.Error -> {
                makeDefaultTextNotification(
                    "Restore Failed",
                    "Data maybe tampered or corrupted"
                )
            }
            is ServiceResult.Success -> {
                makeDefaultTextNotification(
                    "Restore success",
                    "Successfully restored ${insertResult.data} notes"
                )
            }
        }
        updateProgress(45)
    }

    private fun unzipBackup(): ServiceResult<Unit> {
        val filePassword = EncryptionHelper.generateFilePassword(
            userPreferences.getUserId(),
            passKeyProcessor.rotationFactor
        )
        return try {
            val zipFile = ZipFile(filePath)
            zipFile.setPassword(filePassword.toCharArray())
            zipFile.extractAll(restorationPath)
            ServiceResult.Success(Unit)
        } catch (e: ZipException) {
            e.printStackTrace()
            Log.i("SECUREDINFO", "Error info " + e.localizedMessage)
            ServiceResult.Error("Invalid encryption key")
        }
    }

    private fun readData(): ServiceResult<List<Note>> {
        return try {
            val f = File(restorationPath, "$fileName.txt")
            val dt = f.readText()
            f.delete()
            val notes = BackupHelper.restoreBackupData(dt, key)
            ServiceResult.Success(notes)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.i("SECUREDINFO", "Error info " + e.localizedMessage)
            ServiceResult.Error("Invalid encryption key")
        }
    }

    private suspend fun insertData(notes: List<Note>): ServiceResult<Int> {
        return when (notesRepository.insertNotes(notes)) {
            is ServiceResult.Error -> {
                Log.i("SECUREDINFO", "Restore failed")
                ServiceResult.Error("Data maybe tampered or corrupted")
            }
            is ServiceResult.Success -> {
                Log.i("SECUREDINFO", "Restore success")
                ServiceResult.Success(notes.size)
            }
        }
    }

    private suspend fun databaseCleanUp(): ServiceResult<Unit> {
        return notesRepository.deleteAllNotes()
    }

    private fun getFilePath(uri: Uri): String? {
        val path = FileUtil.getPathFromLocalUri(applicationContext, uri)
        if (path != null) {
            return path
        }
        val ins = applicationContext.contentResolver.openInputStream(uri)
        val file = File(applicationContext.externalCacheDir!!.absolutePath + "/test")
        file.writeBytes(ins!!.readBytes())
        return file.absolutePath
    }

    private fun getFileName(uri: Uri): String? {
        val a = uri.lastPathSegment
        return a?.split("/")?.get(a.split("/").size - 1)?.split(".")?.get(0)
    }

}