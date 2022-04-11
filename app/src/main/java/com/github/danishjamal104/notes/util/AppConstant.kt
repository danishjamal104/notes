package com.github.danishjamal104.notes.util

object AppConstant {
    const val NOTE_ID_KEY = "noteId"

    object Database {
        const val DB_NAME = "NOTES-ROOT-TABLE"
    }

    object Worker {
        const val KEY = "KEY"
        const val FILE_URI = "FILE_URI"
    }

    object Notification {
        const val CHANNEL_ID = "com.github.danishjamal104.notes.util.notification.channel.id"
        const val CHANNEL_NAME = "Note Notification"
        const val CHANNEL_DESCRIPTION = "Default notification channel for all Note notifications"
    }
    object IntentExtra {
        const val ENCRYPTION_KEY = "ENCRYPTION-KEY"
    }
}