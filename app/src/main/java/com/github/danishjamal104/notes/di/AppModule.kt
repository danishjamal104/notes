package com.github.danishjamal104.notes.di

import android.Manifest
import android.content.Context
import androidx.work.WorkManager
import com.github.danishjamal104.notes.ui.Notes
import com.github.danishjamal104.notes.ui.fragment.home.adapter.NotesAdapter
import com.github.danishjamal104.notes.util.AppConstant
import com.github.danishjamal104.notes.util.SystemManager
import com.github.danishjamal104.notes.util.sharedpreference.EncryptionPreferences
import com.github.danishjamal104.notes.util.sharedpreference.UserPreferences
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
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

    @Singleton
    @Provides
    fun providesEncryptionPreferences(@ApplicationContext context: Context,
    userPreferences: UserPreferences): EncryptionPreferences {
        return EncryptionPreferences(context, userPreferences)
    }

    @Singleton
    @Provides
    fun providesGoogleSigningOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
    }

    @Singleton
    @Provides
    fun providesGoogleSignInClient(@ApplicationContext context: Context,
                                   gso: GoogleSignInOptions): GoogleSignInClient {
        return GoogleSignIn.getClient(context, gso)
    }

    @Singleton
    @Provides
    fun provideGoogleSignInAccount(@ApplicationContext context: Context): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    @Singleton
    @Provides
    fun provideNoteAdapter(@ApplicationContext context: Context): NotesAdapter {
        return NotesAdapter(context)
    }

    @Singleton
    @Provides
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Singleton
    @Provides
    @Named(AppConstant.PERMISSION_ARRAY)
    fun providePermissionArray(): List<String> {
        return listOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    @Singleton
    @Provides
    fun provideSystemManager(
        @ApplicationContext ctx: Context,
        @Named(AppConstant.PERMISSION_ARRAY) permission: List<String>
    ): SystemManager {
        return SystemManager(ctx, permission)
    }

}