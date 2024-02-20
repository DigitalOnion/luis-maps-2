package com.udacity.project4.di

import android.content.Context
import androidx.room.Room
import com.udacity.project4.repositories.EmailLoginDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext

const val EMAIL_LOGIN_DB_NAME = "email-login-db"

@Module
@InstallIn(ViewModelComponent::class)
object EmailLoginDatabaseModule {
    @Provides
    fun provideEmailLoginDatabase(@ApplicationContext appContext: Context): EmailLoginDatabase =
        Room.databaseBuilder(
            appContext,
            EmailLoginDatabase::class.java,
            EMAIL_LOGIN_DB_NAME
        ).build()

    @Provides
    fun provideEmailLoginDao(db: EmailLoginDatabase) = db.emailLoginDao()
}