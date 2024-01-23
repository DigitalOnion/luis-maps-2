package com.udacity.project4.locationreminders.data

import android.content.Context
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.udacity.project4.domain.WorldLocation
import com.udacity.project4.repositories.ReminderDTO
import com.udacity.project4.repositories.WorldLocationDao
import com.udacity.project4.repositories.WorldLocationEntity
import com.udacity.project4.locationreminders.data.Result
import com.udacity.project4.repositories.RemindersLocalRepository
import com.udacity.project4.repositories.WorldLocationDatabase
import com.udacity.project4.viewModels.LocationViewModel

//Use FakeDataSource that acts as a test double to the LocalDataSource

/**
 * FakeDataSource implements RemindersDataSource interface.
 * The RemindersLocalRepository also implements the RemindersDataSource interface.
 *
 * The real datasource is the RemindersLocalRepository which takes a WorldLocationDao
 * the RemindersListViewModel takes one of those, though we can give it any RemindersDataSource
 *
 * The RemindersLocalRepositoryTest test uses an instance of the RemindersListViewModel
 * initialized with a FakeDataSource casted to a RemindersLocalRepository.
 */

class FakeDataSource : ReminderDataSource {
    private val map = mutableMapOf<Long, WorldLocationEntity>()

    fun contains(worldLocationEntity: WorldLocationEntity) = map.values.contains(worldLocationEntity)
    var size: Int = 0
        private set
        get() = map.size

    var values: MutableCollection<WorldLocationEntity> = mutableListOf()
        private set
        get() = map.values

    fun WorldLocation.toWorldLocationEntity(): WorldLocationEntity {
        return WorldLocationEntity(id, lat, lon, title, description, isMapsPoi)
    }

    init {
        var idx = 1L
        initialList().forEach {
            it.id = idx
            map[idx++] = it.toWorldLocationEntity()
        }
    }

    private val dao = object: WorldLocationDao {
        override suspend fun insert(location: WorldLocationEntity): Long {
            val idx = map.keys.max() + 1
            location.locationId = idx
            map[idx] = location
            return idx
        }

        override suspend fun getLocations(): List<WorldLocationEntity> = map.values.toList()

        override suspend fun getLocationAt(poiId: Long): List<WorldLocationEntity> =
            if (map.containsKey(poiId)) listOf(map[poiId]!!) else listOf()

        override suspend fun deleteLocationAt(poiId: Long) {
            map.remove(poiId)
        }

        override suspend fun updateLocationAt(poiId: Long, title: String, description: String) {
            val location = map[poiId] ?: return
            val newLocation = WorldLocationEntity(poiId, location.lat, location.lon, title, description, location.isMapsPoi)
            map[poiId] = newLocation
        }

        override suspend fun deleteAll() = map.clear()
    }

    fun initialList(): List<WorldLocation> = listOf(
            WorldLocation(0, 19.4326, -99.1332, "Mexico", "Mexico City. Mexico"),
            WorldLocation(0, 20.9674, -89.5926, "Merida", "Merida, Yucatan. Mexico"),
            WorldLocation(0, 33.7488, -84.3877, "Atlanta", "Atlanta, Georgia, US"),
            WorldLocation(0, 19.5438, -96.9102,
                "Xalapa", "Xalapa is a cultural city. Capital of the Veracruz State, Xalapa has theaters, museums, cafes, Universities. It holds all the Government's offices, Courts, and the Palace of Government"
            ),  // Xalapa, Veracruz. Mexico
            WorldLocation(0, 25.7617, -80.1918,
                "Miami", "Miami is a US coastal touristic city, famous for art, movies, music, sports and, of course, night life."
            ), // Miami, FL, US
        )

    override suspend fun insert(location: WorldLocationEntity): Long = dao.insert(location)

    override suspend fun getLocations(): List<WorldLocationEntity> = dao.getLocations()

    override suspend fun getLocationAt(poiId: Long): List<WorldLocationEntity> = dao.getLocationAt(poiId)

    override suspend fun deleteLocationAt(poiId: Long) = dao.deleteLocationAt(poiId)

    override suspend fun updateLocationAt(poiId: Long, title: String, description: String) =
        dao.updateLocationAt(poiId, title, description)

    override suspend fun deleteAll() = dao.deleteAll()

    override suspend fun getReminders(): Result<List<ReminderDTO>> =
        try {
            Result.Success(dao.getLocations())
        } catch (e: Exception) {
            Result.Error(e.message)
        }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        dao.insert(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> =
        try {
            Result.Success(dao.getLocationAt(id.toLong()).first())
        } catch (e: Exception) {
            Result.Error(e.message)
        }

    override suspend fun deleteAllReminders() = dao.deleteAll()
}

