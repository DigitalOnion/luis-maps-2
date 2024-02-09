package com.udacity.project4.locationreminders.data.local

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.domain.WorldLocation
import com.udacity.project4.repositories.RemindersLocalRepository
import com.udacity.project4.repositories.WorldLocationDao
import com.udacity.project4.repositories.WorldLocationDatabase
import com.udacity.project4.repositories.WorldLocationEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@MediumTest

/**
 * RemindersLocalRepositoryTest:
 *      it is basically the same tests than the RemindersDaoTest, but using the
 *      Repository created at the TestDataSource class. This creates an in memory
 *      Room database.
 */

class RemindersLocalRepositoryTest {
    @get: Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val appContext = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var db: WorldLocationDatabase
    private lateinit var dao: WorldLocationDao
    private lateinit var repo: RemindersLocalRepository

    private fun WorldLocation.toWorldLocationEntity(): WorldLocationEntity =
        WorldLocationEntity(id, lat, lon, title, description, isMapsPoi)

    private fun WorldLocationEntity.toWorldLocation(): WorldLocation =
        WorldLocation(locationId, lat, lon, title, description, isMapsPoi)


    @Before
    fun initialization() {
        db = Room.inMemoryDatabaseBuilder(
            context = appContext,
            WorldLocationDatabase::class.java,
        ).build()
        dao = db.worldLocationDao()
        repo = RemindersLocalRepository(dao)
    }

    @After
    fun termination() {
        db.close()
    }

    @Test
    fun remindersLocalRepository_insert_getLocationAt_Test() {
        val map: MutableMap<Long, WorldLocation> = mutableMapOf()
        for (location in initialList()) {
            runTest {
                val pk = repo.insert(location.toWorldLocationEntity())
                map[pk] = location
            }
        }
        for (pk in map.keys) {
            runTest {
                val location = repo.getLocationAt(pk).first().toWorldLocation()
                assert (location == map[pk])
            }
        }
    }

    @Test
    fun remindersLocalRepository_getLocations_Test() {
        val map: MutableMap<Long, WorldLocation> = mutableMapOf()
        var locationTable: List<WorldLocationEntity> = listOf()
        for (location in initialList()) {
            runTest {
                val pk = repo.insert(location.toWorldLocationEntity())
                map[pk] = location
            }
        }
        runTest {
            locationTable = repo.getLocations()
        }
        for (location in locationTable) {
            val targetLocation = map[location.locationId]?.toWorldLocationEntity()
            if (targetLocation != null) {
                assert(location.lat == targetLocation.lat)
                assert(location.lon == targetLocation.lon)
            }
        }
    }

    @Test
    fun remindersLocalRepository_deleteLocationAt_Test() {
        val map: MutableMap<Long, WorldLocation> = mutableMapOf()
        for (location in initialList()) {
            runTest {
                val pk = repo.insert(location.toWorldLocationEntity())
                map[pk] = location
            }
        }
        for (pk in map.keys) {
            runTest {
                repo.deleteLocationAt(pk)
                val locations = repo.getLocations()
                for (location in locations) {
                    assert(location.lat != map[pk]!!.lat && location.lon != map[pk]!!.lon)
                }
            }
        }
    }

    @Test
    fun remindersLocalRepository_updateLocationAt_Test() {
        val map: MutableMap<Long, WorldLocation> = mutableMapOf()
        for (location in initialList()) {
            runTest {
                val pk = repo.insert(location.toWorldLocationEntity())
                map[pk] = location
            }
        }
        for (pk in map.keys) {
            runTest {
                val title = "title-$pk"
                val description = "description-$pk"
                repo.updateLocationAt(pk, title, description)
            }
        }

        runTest {
            for (location in repo.getLocations())
                assert(location.title == "title-${location.locationId}" && location.description == "description-${location.locationId}")
        }
    }

    @Test
    fun remindersLocalRepository_deleteAll_Test() {
        for (location in initialList()) {
            runTest {
                val pk = repo.insert(location.toWorldLocationEntity())
            }
        }

        runTest {
            val listAll = repo.getLocations()
            repo.deleteAll()
            val listNone = repo.getLocations()
            assert(listAll.isNotEmpty())
            assert(listNone.isEmpty())
        }
    }

    // --- initialList utility function -------------
    private fun initialList(): List<WorldLocation> = listOf(
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
}