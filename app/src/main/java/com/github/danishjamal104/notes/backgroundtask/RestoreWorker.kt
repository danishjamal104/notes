package com.github.danishjamal104.notes.backgroundtask

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.WorkerParameters
import com.github.danishjamal104.notes.data.backupandrestore.BackupHelper
import com.github.danishjamal104.notes.data.backupandrestore.PassKeyProcessor
import com.github.danishjamal104.notes.util.AppConstant
import com.github.danishjamal104.notes.util.FileUtil
import com.github.danishjamal104.notes.util.ServiceResult
import com.github.danishjamal104.notes.util.encryption.EncryptionHelper
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import java.io.File
import java.lang.Exception


@Suppress("DEPRECATION")
class RestoreWorker(ctx: Context, params: WorkerParameters) : BaseCoroutineNoteWorker(ctx, params) {

    lateinit var key: String
    private lateinit var filePath: String
    private lateinit var fileName: String
    private lateinit var fileUri: Uri
    private lateinit var passKeyProcessor: PassKeyProcessor

    private val restorationPath = File(applicationContext.externalCacheDir!!.absolutePath
            + "/restore").absolutePath

    override suspend fun doWork(): Result {
        val id = System.currentTimeMillis().toInt()
        setForeground(makeStatusNotification("Restoring Backup", "", id))
        key = inputData.getString(AppConstant.Worker.KEY) ?: return Result.failure()
        val fileUriString = inputData.getString(AppConstant.Worker.FILE_URI) ?: return Result.failure()
        fileUri = Uri.parse(fileUriString)
        filePath = getFilePath(fileUri) ?: return Result.failure()
        fileName = getFileName(fileUri) ?: return Result.failure()
        passKeyProcessor = PassKeyProcessor.load(key)
        restore()
        return Result.success()
    }

    private suspend fun restore() {
        val filePassword = EncryptionHelper.generateFilePassword(
            userPreferences.getUserId(),
        passKeyProcessor.rotationFactor)
        try {
            val zipFile = ZipFile(filePath)
            zipFile.setPassword(filePassword.toCharArray())
            zipFile.extractAll(restorationPath)
            makeDefaultTextNotification("Restore complete", restorationPath)
            readData()
        } catch (e: ZipException) {
            e.printStackTrace()
            Log.i("SECUREDINFO", "Error info " + e.localizedMessage)
        }
    }

    private suspend fun readData() {
        try {
            val f = File(restorationPath, "$fileName.txt")
            val dt = f.readText()
            val notes = BackupHelper.restoreBackupData(dt, key)
            when(notesRepository.insertNotes(notes)) {
                is ServiceResult.Error -> Log.i("SECUREDINFO", "Restore failed")
                is ServiceResult.Success -> Log.i("SECUREDINFO", "Restore success")
            }
            Log.i("SECUREDINFO", dt)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.i("SECUREDINFO", "Error info " + e.localizedMessage)
        }
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
        return a?.split("/")?.get(a.split("/").size-1)?.split(".")?.get(0)
    }

}