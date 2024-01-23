package com.udacity.project4.locationreminders.data.local

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.domain.WorldLocation
import com.udacity.project4.repositories.RemindersLocalRepository
import com.udacity.project4.repositories.WorldLocationDao
import com.udacity.project4.repositories.WorldLocationDatabase
import com.udacity.project4.repositories.WorldLocationEntity
import com.udacity.project4.viewModels.LocationViewModel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

//Unit test the DAO

@RunWith(AndroidJUnit4::class)
@SmallTest
class RemindersDaoTest {
    @get: Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var locations: List<WorldLocation>
    private lateinit var locationVM: LocationViewModel
    private lateinit var db: WorldLocationDatabase
    private lateinit var dao: WorldLocationDao

    private fun WorldLocation.toWorldLocationEntity(): WorldLocationEntity =
        WorldLocationEntity(id, lat, lon, title, description, isMapsPoi)

    private fun WorldLocationEntity.toWorldLocation(): WorldLocation =
        WorldLocation(locationId, lat, lon, title, description, isMapsPoi)

    @Before
    fun initialization() {
        locations = listOf(
            WorldLocation(19.4326, 99.1332),  // Mexico City. Mexico
            WorldLocation(20.9674, 89.5926),  // Merida, Yucatan. Mexico
            WorldLocation(33.7488, 84.3877),  // Atlanta, Georgia, US
            WorldLocation( 0,
                19.5438,
                96.9102,
                "Xalapa",
                "Xalapa is a cultural city. Capital of the Veracruz State, Xalapa has theaters, museums, cafes, Universities. It holds all the Government's offices, Courts, and the Palace of Government"
            ),  // Xalapa, Veracruz. Mexico
            WorldLocation( 0,
                25.7617,
                80.1918,
                "Miami",
                "Miami is a US coastal touristic city, famous for art, movies, music, sports and, of course, night life."
            ), // Miami, FL, US
        )

        val appContext = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            appContext,
            WorldLocationDatabase::class.java
        ).build()
        dao = db.worldLocationDao()

        locationVM = LocationViewModel(RemindersLocalRepository(dao))
    }

    @After
    fun termination() {
        db.close()
    }

    @Test
    fun worldLocationDao_insert_getLocationAt_Test() {
        val map: MutableMap<Long, WorldLocation> = mutableMapOf()
        for (location in locations) {
            runTest {
                val pk = dao.insert(location.toWorldLocationEntity())
                map[pk] = location
            }
        }
        for (pk in map.keys) {
            runTest {
                val location = dao.getLocationAt(pk).first().toWorldLocation()
                assert (location == map[pk])
            }
        }
    }

    @Test
    fun worldLocationDao_getLocations_Test() {
        val map: MutableMap<Long, WorldLocation> = mutableMapOf()
        var locationTable: List<WorldLocationEntity> = listOf()
        for (location in locations) {
            runTest {
                val pk = dao.insert(location.toWorldLocationEntity())
                map[pk] = location
            }
        }
        runTest {
            locationTable = dao.getLocations()
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
    fun worldLocationDao_deleteLocationAt_Test() {
        val map: MutableMap<Long, WorldLocation> = mutableMapOf()
        for (location in locations) {
            runTest {
                val pk = dao.insert(location.toWorldLocationEntity())
                map[pk] = location
            }
        }
        for (pk in map.keys) {
            runTest {
                dao.deleteLocationAt(pk)
                val locations = dao.getLocations()
                for (location in locations) {
                    assert(location.lat != map[pk]!!.lat && location.lon != map[pk]!!.lon)
                }
            }
        }
    }

    @Test
    fun worldLocationDao_updateLocationAt_Test() {
        val map: MutableMap<Long, WorldLocation> = mutableMapOf()
        for (location in locations) {
            runTest {
                val pk = dao.insert(location.toWorldLocationEntity())
                map[pk] = location
            }
        }
        for (pk in map.keys) {
            runTest {
                val title = "title-$pk"
                val description = "description-$pk"
                dao.updateLocationAt(pk, title, description)
            }
        }

        runTest {
            for (location in dao.getLocations())
                assert(location.title == "title-${location.locationId}" && location.description == "description-${location.locationId}")
        }
    }

    @Test
    fun worldLocationDao_deleteAll_Test() {
        for (location in locations) {
            runTest {
                val pk = dao.insert(location.toWorldLocationEntity())
            }
        }

        runTest {
            val listAll = dao.getLocations()
            dao.deleteAll()
            val listNone = dao.getLocations()
            assert(listAll.isNotEmpty())
            assert(listNone.isEmpty())
        }
    }
}