package com.github.danishjamal104.notes.backgroundtask

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkerParameters
import com.github.danishjamal104.notes.R
import com.github.danishjamal104.notes.data.backupandrestore.BackupHelper
import com.github.danishjamal104.notes.data.backupandrestore.PassKeyProcessor
import com.github.danishjamal104.notes.data.model.Note
import com.github.danishjamal104.notes.ui.main.MainActivity
import com.github.danishjamal104.notes.util.AppConstant
import com.github.danishjamal104.notes.util.ServiceResult
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

    lateinit var key: String
    private lateinit var keyProcessor: PassKeyProcessor

    override suspend fun doWork(): Result {
        progressNotificationTitle = "Creating Backup"
        updateProgress()
        key = inputData.getString(AppConstant.Worker.KEY) ?: return Result.failure()
        keyProcessor = PassKeyProcessor.load(key)
        backup()
        return Result.success()
    }

    private suspend fun backup() {
        val notes = getAllNotes()
        if (notes.isEmpty()) {
            makeDefaultTextNotification("Backup completed", "No notes to be backed up")
            return
        }
        updateProgress(30)
        val fileName = "${System.currentTimeMillis()}"
        val encryptedData = BackupHelper.createBackupData(notes, key)
        updateProgress(30)
        val writeToFileResult = storeBackupDataToFile(fileName, encryptedData)
        when (writeToFileResult) {
            is ServiceResult.Error -> {
                makeDefaultTextNotification("Backup Failed", writeToFileResult.reason)
                return
            }
            is ServiceResult.Success -> Unit
        }
        updateProgress(20)
        when (val zipResult = createZip(fileName, writeToFileResult.data)) {
            is ServiceResult.Error -> {
                makeDefaultTextNotification("Backup Failed", zipResult.reason)
                return
            }
            is ServiceResult.Success -> {
                makeNotificationWithEncryptionKeyInfo(
                    "Backup Complete",
                    "Backup file can be found at ${zipResult.data}"
                )
            }
        }
        writeToFileResult.data.delete()
        updateProgress(20)
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
     * Creates the txt file with the data. Returns the instance of the [File]
     * @param sFileName name of the file without extension
     * @param sBody data to be added in file
     */
    private fun storeBackupDataToFile(sFileName: String, sBody: String): ServiceResult<File> {
        return try {
            val root = File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS
                ), "/Notes/Backup"
            )
            if (!root.exists()) {
                root.mkdirs()
            }
            val file = File(root, "$sFileName.txt")
            val writer = FileWriter(file)
            writer.append(sBody)
            writer.flush()
            writer.close()
            ServiceResult.Success(file)
        } catch (e: IOException) {
            e.printStackTrace()
            Log.i("SECUREDINFO", "Error info " + e.localizedMessage)
            ServiceResult.Error("Unable to write data to file")
        }
    }

    /**
     * Creates the password protected zip folder for storing backup. The default location of
     * backup zip is storage/emulated/0/Documents/Notes/Backup. Returns the file location for
     * successful zip creating
     * @param filename name of the file without extension
     * @param file the actual backup file containing the encrypted data
     * @return [ServiceResult]
     */
    private fun createZip(filename: String, file: File): ServiceResult<String> {
        return try {
            val zipParameters = ZipParameters()
            zipParameters.isEncryptFiles = true
            zipParameters.encryptionMethod = EncryptionMethod.AES
            zipParameters.aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256

            val pwd = EncryptionHelper.generateFilePassword(
                userPreferences.getUserId(),
                keyProcessor.rotationFactor
            )
            val zip = File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS
                ), getBackupLocation(filename, "zip")
            )
            ZipFile(zip, pwd.toCharArray()).addFile(file, zipParameters)
            ServiceResult.Success(zip.absolutePath)
        } catch (e: IOException) {
            e.printStackTrace()
            Log.i("SECUREDINFO", "Error info " + e.localizedMessage)
            ServiceResult.Error("Unable to secure zip")
        }
    }


    @SuppressLint("UnspecifiedImmutableFlag")
    private fun makeNotificationWithEncryptionKeyInfo(title: String, message: String) {
        val context = applicationContext
        // Make a channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            createChannel()
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        intent.putExtra(AppConstant.IntentExtra.ENCRYPTION_KEY, key)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(context, AppConstant.Notification.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_baseline_lock_open_24, "Copy EncryptionKey", pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setVibrate(LongArray(0))

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

}