package com.github.danishjamal104.notes.backgroundtask

import android.content.Context
import android.os.Environment
import androidx.work.WorkerParameters
import com.github.danishjamal104.notes.data.backupandrestore.BackupHelper
import com.github.danishjamal104.notes.data.backupandrestore.PassKeyProcessor
import com.github.danishjamal104.notes.data.model.Note
import com.github.danishjamal104.notes.util.*
import com.github.danishjamal104.notes.util.encryption.EncryptionHelper
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File
import java.io.FileWriter
import java.io.IOException

@Suppress("DEPRECATION")
class BackupWorker(ctx: Context, params: WorkerParameters) : BaseCoroutineNoteWorker(ctx, params) {

    private lateinit var keyProcessor: PassKeyProcessor
    private lateinit var fileLocation: String

    override suspend fun doWork(): Result {
        val id = System.currentTimeMillis().toInt()
        setForeground(makeStatusNotification("Creating Backup", "", id))
        val key = inputData.getString(AppConstant.Worker.KEY) ?: return Result.failure()
        keyProcessor = PassKeyProcessor.load(key)
        val notes = getAllNotes()
        if(notes.isEmpty()) {
            return Result.failure()
        }
        val data = BackupHelper.createBackupData(notes, key)
        storeBackupToFile("$id", data)
        return Result.success()
    }

    /**
     * Fetches all the notes form DB.
     * @return [List] list of [Note]
     */
    private suspend fun getAllNotes(): List<Note> {
        return when (val result = notesRepository.getNotes()) {
            is ServiceResult.Error -> throw Exception("Failed to fetch notes")
            is ServiceResult.Success -> result.data
        }
    }

    /**
     * Creates the txt file with the data. Once the file is created the file is protected using
     * .zip.
     * @see [createZip]
     * @param sFileName name of the file without extension
     * @param sBody data to be added in file
     */
    private fun storeBackupToFile(sFileName: String, sBody: String) {
        try {
            val root = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), "/Notes/Backup")
            if (!root.exists()) {
                root.mkdirs()
            }
            val file = File(root, "$sFileName.txt")
            val writer = FileWriter(file)
            writer.append(sBody)
            writer.flush()
            writer.close()
            createZip(sFileName, file)
            file.delete()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Creates the password protected zip folder for storing backup. The default location of
     * backup zip is storage/emulated/0/Documents/Notes/Backup
     * @param filename name of the file without extension
     * @param file the actual backup file containing the encrypted data
     */
    private fun createZip(filename: String, file: File) {
        try {
            val zipParameters = ZipParameters()
            zipParameters.isEncryptFiles = true
            zipParameters.encryptionMethod = EncryptionMethod.AES
            zipParameters.aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256

            val pwd = EncryptionHelper.generateFilePassword(userPreferences.getUserId(), keyProcessor.rotationFactor)
            val zip = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), getBackupLocation(filename, "zip"))
            ZipFile(zip, pwd.toCharArray()).addFile(file, zipParameters)
            fileLocation = zip.absolutePath
            makeDefaultTextNotification("Backup Complete",
                "Backup file can be found at $fileLocation"
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}