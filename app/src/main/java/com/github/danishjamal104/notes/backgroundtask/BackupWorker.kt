package com.github.danishjamal104.notes.backgroundtask

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.github.danishjamal104.notes.R
import com.github.danishjamal104.notes.data.backupandrestore.BackupHelper
import com.github.danishjamal104.notes.data.backupandrestore.PassKeyProcessor
import com.github.danishjamal104.notes.data.local.CacheDataSourceImpl
import com.github.danishjamal104.notes.data.local.Database
import com.github.danishjamal104.notes.data.mapper.NoteMapper
import com.github.danishjamal104.notes.data.mapper.UserMapper
import com.github.danishjamal104.notes.data.model.Note
import com.github.danishjamal104.notes.data.repository.note.NotesRepositoryImpl
import com.github.danishjamal104.notes.ui.main.MainActivity
import com.github.danishjamal104.notes.util.*
import com.github.danishjamal104.notes.util.encryption.EncryptionHelper
import com.github.danishjamal104.notes.util.sharedpreference.UserPreferences
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

    private lateinit var keyProcessor: PassKeyProcessor
    private lateinit var fileLocation: String

    override suspend fun doWork(): Result {
        val id = System.currentTimeMillis().toInt()
        setForeground(makeStatusNotification(id))
        val key = inputData.getString(AppConstant.Worker.KEY) ?: return Result.failure()
        keyProcessor = PassKeyProcessor.load(key)
        val notes = getAllNotes()
        val data = BackupHelper.createBackupData(notes, key)
        storeBackupToFile("$id", data)
        makeSuccessNotification()
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
                Environment.DIRECTORY_DOCUMENTS), "/Notes/Backup/$filename.zip")
            ZipFile(zip, pwd.toCharArray()).addFile(file, zipParameters)
            fileLocation = zip.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Shows success notification. To be used to inform user that backup is successfully completed.
     * The notification message contains the file location.
     */
    private fun makeSuccessNotification() {
        val context = applicationContext
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or
                    PendingIntent.FLAG_UPDATE_CURRENT
        )
        val message = "Backup file can be found at $fileLocation"
        val builder = NotificationCompat.Builder(context, AppConstant.Notification.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Backup Complete")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setVibrate(LongArray(0))

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    /**
     * Creates the default notification showing the backup progress
     * @param id Notification id
     */
    private fun makeStatusNotification(id: Int): ForegroundInfo {
        val context = applicationContext
        val title = "Creating Backup"
        val message = ""
        // Make a channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            createChannel()
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or
                    PendingIntent.FLAG_UPDATE_CURRENT
        )
        val cancelIntent =
            WorkManager.getInstance(applicationContext).createCancelPendingIntent(getId())
        // Create the notification
        val builder = NotificationCompat.Builder(context, AppConstant.Notification.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, "Cancel Upload", cancelIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setVibrate(LongArray(0))

        return ForegroundInfo(id, builder.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createChannel() {
        val name = AppConstant.Notification.CHANNEL_NAME
        val description = AppConstant.Notification.CHANNEL_DESCRIPTION
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(AppConstant.Notification.CHANNEL_ID, name, importance)
        channel.description = description

        // Add the channel
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        notificationManager?.createNotificationChannel(channel)
    }

}