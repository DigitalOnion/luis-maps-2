//package com.udacity.project4.di
//
//import android.content.Context
//import androidx.room.Room
//import com.udacity.project4.locationreminders.data.ReminderDataSource
//import com.udacity.project4.repositories.RemindersLocalRepository
//import com.udacity.project4.repositories.WorldLocationDatabase
//import dagger.Module
//import dagger.Provides
//import dagger.hilt.android.qualifiers.ApplicationContext
//import dagger.hilt.components.SingletonComponent
//import dagger.hilt.testing.TestInstallIn
//
//@Module
//@TestInstallIn(
//    components = [SingletonComponent::class],
//    replaces = [RemindersDatabaseModule::class]
//)
//object FakeRemindersDatabaseModule {
//    @Provides
//    fun provideLocationDatabase(@ApplicationContext appContext: Context): WorldLocationDatabase =
//        Room.inMemoryDatabaseBuilder(
//            appContext,
//            WorldLocationDatabase::class.java,
//        ).build()
//
//    @Provides
//    fun provideWorldLocationDao(db: WorldLocationDatabase) = db.worldLocationDao()
//
//    @Provides
//    fun provideReminderDataSource(db: WorldLocationDatabase): ReminderDataSource {
//        val dao = db.worldLocationDao()
//        return RemindersLocalRepository(dao)
//    }
//}
