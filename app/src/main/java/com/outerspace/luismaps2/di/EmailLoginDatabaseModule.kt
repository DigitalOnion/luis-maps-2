package com.outerspace.luismaps2.di

import android.content.Context
import androidx.room.Room
import com.outerspace.luismaps2.repositories.EmailLoginDatabase
import com.outerspace.luismaps2.view.EMAIL_LOGIN_DB_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext

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