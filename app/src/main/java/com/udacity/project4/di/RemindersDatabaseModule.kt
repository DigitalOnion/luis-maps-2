package com.udacity.project4.di

import android.content.Context
import androidx.room.Room
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.repositories.RemindersLocalRepository
import com.udacity.project4.repositories.WorldLocationDatabase
import com.udacity.project4.viewModels.LOCATION_DATABASE_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext

// References:
//      https://developer.android.com/training/dependency-injection/hilt-android
//      https://stackoverflow.com/questions/63146318/how-to-create-and-use-a-room-database-in-kotlin-dagger-hilt

@Module
@InstallIn(ViewModelComponent::class)
object RemindersDatabaseModule {
    @Provides
    fun provideLocationDatabase(@ApplicationContext appContext: Context): WorldLocationDatabase =
        Room.databaseBuilder(
            appContext,
            WorldLocationDatabase::class.java,
            LOCATION_DATABASE_NAME
        ).build()

    @Provides
    fun provideWorldLocationDao(db: WorldLocationDatabase) = db.worldLocationDao()

    @Provides
    fun provideReminderDataSource(db: WorldLocationDatabase): ReminderDataSource {
        val dao = db.worldLocationDao()
        return RemindersLocalRepository(dao)
    }
}
