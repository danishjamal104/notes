package com.github.danishjamal104.notes.di

import android.content.Context
import androidx.room.Room
import com.github.danishjamal104.notes.data.local.CacheDataSource
import com.github.danishjamal104.notes.data.local.CacheDataSourceImpl
import com.github.danishjamal104.notes.data.local.Database
import com.github.danishjamal104.notes.data.local.dao.LabelDao
import com.github.danishjamal104.notes.data.local.dao.NoteDao
import com.github.danishjamal104.notes.data.local.dao.NoteLabelJoinDao
import com.github.danishjamal104.notes.data.local.dao.UserDao
import com.github.danishjamal104.notes.di.Migration.MIGRATION_1_2
import com.github.danishjamal104.notes.util.AppConstant
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object CacheModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): Database {
        return Room.databaseBuilder(context, Database::class.java, AppConstant.Database.DB_NAME)
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Singleton
    @Provides
    fun provideUserDao(database: Database): UserDao {
        return database.userDao()
    }

    @Singleton
    @Provides
    fun provideNoteDao(database: Database): NoteDao {
        return database.noteDao()
    }

    @Singleton
    @Provides
    fun provideLabelDao(database: Database): LabelDao {
        return database.labelDao()
    }

    @Singleton
    @Provides
    fun provideNoteLabelJoinDao(database: Database): NoteLabelJoinDao {
        return database.noteLabelJoinDao()
    }

    @Singleton
    @Provides
    fun provideCacheDataSource(
        userDao: UserDao,
        noteDao: NoteDao,
        labelDao: LabelDao,
        noteLabelJoinDao: NoteLabelJoinDao
    ): CacheDataSource {
        return CacheDataSourceImpl(userDao, noteDao, labelDao, noteLabelJoinDao)
    }
}