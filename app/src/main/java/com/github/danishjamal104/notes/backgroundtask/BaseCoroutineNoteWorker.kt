package com.github.danishjamal104.notes.backgroundtask

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.github.danishjamal104.notes.R
import com.github.danishjamal104.notes.data.local.CacheDataSource
import com.github.danishjamal104.notes.data.local.CacheDataSourceImpl
import com.github.danishjamal104.notes.data.local.Database
import com.github.danishjamal104.notes.data.mapper.LabelMapper
import com.github.danishjamal104.notes.data.mapper.NoteMapper
import com.github.danishjamal104.notes.data.mapper.UserMapper
import com.github.danishjamal104.notes.data.repository.note.NotesRepository
import com.github.danishjamal104.notes.data.repository.note.NotesRepositoryImpl
import com.github.danishjamal104.notes.ui.main.MainActivity
import com.github.danishjamal104.notes.util.AppConstant
import com.github.danishjamal104.notes.util.sharedpreference.EncryptionPreferences
import com.github.danishjamal104.notes.util.sharedpreference.UserPreferences

abstract class BaseCoroutineNoteWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    private val _db =
        Room.databaseBuilder(applicationContext, Database::class.java, AppConstant.Database.DB_NAME)
            .fallbackToDestructiveMigration()
            .build()
    private val db get() = _db

    private val userDao get() = db.userDao()
    private val noteDao get() = db.noteDao()
    private val labelDao get() = db.labelDao()
    private val noteLabelJoinDao get() = db.noteLabelJoinDao()

    private val _userPreferences = UserPreferences(applicationContext)
    protected val userPreferences get() = _userPreferences

    private val _encryptionPreferences = EncryptionPreferences(applicationContext, userPreferences)
    private val encryptionPreferences = _encryptionPreferences

    private val _cacheDataSource =  CacheDataSourceImpl(
        userDao, noteDao, labelDao, noteLabelJoinDao)
    private val cacheDataSource: CacheDataSource = _cacheDataSource

    private val _noteRepository = NotesRepositoryImpl(cacheDataSource, userPreferences, encryptionPreferences)
    protected val notesRepository: NotesRepository = _noteRepository

    var progressNotificationTitle = "BackupRestore Task"
    private val maxProgress = 100
    private var progress = 0

    private val notifyId = System.currentTimeMillis().toInt()

    /**
     * Updates the notification progress
     */
    suspend fun updateProgress(updateBy: Int = 0) {
        progress = if (progress+updateBy > 100) 100 else progress + updateBy
        setForeground(makeStatusNotification(progressNotificationTitle,  notifyId))
    }

    /**
     * Shows default notification with [title] and [message]
     * @param title title of the notification
     * @param message message to be displayed in notification
     */
    protected fun makeDefaultTextNotification(title: String, message: String) {
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
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or
                    PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(context, AppConstant.Notification.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
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
     * @param title title of the notification
     * @param message message to be displayed in notification
     * @param id Notification id
     */
    private fun makeStatusNotification(title: String, id: Int): ForegroundInfo {
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
            .setContentText("Completed $progress%")
            .setContentIntent(pendingIntent)
            .setProgress(maxProgress, progress, false)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, "Cancel", cancelIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

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

    protected fun getBackupLocation(filename: String, extension: String): String {
        return "/Notes/Backup/$filename.$extension"
    }

}