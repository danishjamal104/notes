package com.github.danishjamal104.notes.di

import android.content.Context
import com.github.danishjamal104.notes.ui.Notes
import com.github.danishjamal104.notes.util.sharedpreference.UserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideBaseApplication(@ApplicationContext context: Context): Notes {
        return context as Notes
    }

    @Singleton
    @Provides
    fun providesUserPreferences(@ApplicationContext context: Context): UserPreferences {
        return UserPreferences(context)
    }

}