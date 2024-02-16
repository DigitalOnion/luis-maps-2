package com.udacity.project4.di

import android.content.Context
import androidx.room.Room
import com.udacity.project4.domain.WorldLocation
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.repositories.RemindersLocalRepository
import com.udacity.project4.repositories.WorldLocationDao
import com.udacity.project4.repositories.WorldLocationDatabase
import com.udacity.project4.repositories.WorldLocationEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.runBlocking

@Module
@TestInstallIn(
    components = [ViewModelComponent::class],
    replaces = [RemindersDatabaseModule::class]
)
object FakeRemindersDatabaseModule {
    @Provides
    fun provideLocationDatabase(@ApplicationContext appContext: Context): WorldLocationDatabase =
        Room.inMemoryDatabaseBuilder(
            appContext,
            WorldLocationDatabase::class.java,
        ).build()

    @Provides
    fun provideWorldLocationDao(db: WorldLocationDatabase): WorldLocationDao {
        val dao = db.worldLocationDao()
        runBlocking {
            for (location in initialList()) {
                dao.insert(location)
            }
        }
        return dao
    }

    @Provides
    fun provideReminderDataSource(db: WorldLocationDatabase): ReminderDataSource {
        val dao = db.worldLocationDao()
        return RemindersLocalRepository(dao)
    }
}

private fun initialList(): List<WorldLocationEntity> {
    val xalapaDescription = "Xalapa is a cultural city. Capital of the Veracruz State, Xalapa has theaters, museums, cafes, Universities. It holds all the Government's offices, Courts, and the Palace of Government"
    val miamiDescription = "Miami is a US coastal touristic city, famous for art, movies, music, sports and, of course, night life."
    return listOf(
        WorldLocationEntity( WorldLocation(0, 19.4326, -99.1332, "Mexico", "Mexico City. Mexico")),
        WorldLocationEntity( WorldLocation(0, 20.9674, -89.5926, "Merida", "Merida, Yucatan. Mexico")),
        WorldLocationEntity( WorldLocation(0, 33.7488, -84.3877, "Atlanta", "Atlanta, Georgia, US")),
        WorldLocationEntity( WorldLocation(0, 19.5438, -96.9102, "Xalapa", xalapaDescription)),
        WorldLocationEntity( WorldLocation(0, 25.7617, -80.1918, "Miami", miamiDescription)),
    )
}
