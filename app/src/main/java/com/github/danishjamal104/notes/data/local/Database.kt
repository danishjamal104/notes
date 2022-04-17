package com.github.danishjamal104.notes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.github.danishjamal104.notes.data.entity.cache.LabelCacheEntity
import com.github.danishjamal104.notes.data.entity.cache.NoteCacheEntity
import com.github.danishjamal104.notes.data.entity.cache.NoteLabelJoinEntity
import com.github.danishjamal104.notes.data.entity.cache.UserCacheEntity
import com.github.danishjamal104.notes.data.local.dao.LabelDao
import com.github.danishjamal104.notes.data.local.dao.NoteDao
import com.github.danishjamal104.notes.data.local.dao.NoteLabelJoinDao
import com.github.danishjamal104.notes.data.local.dao.UserDao

@Database(
    entities = [NoteCacheEntity::class, UserCacheEntity::class, LabelCacheEntity::class, NoteLabelJoinEntity::class],
    version = 3
)
abstract class Database : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun noteDao(): NoteDao
    abstract fun labelDao(): LabelDao
    abstract fun noteLabelJoinDao(): NoteLabelJoinDao
}