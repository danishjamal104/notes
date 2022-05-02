package com.github.danishjamal104.notes.di

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration {

    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Since we didn't alter the table, there's nothing else to do here.
        }
    }
}