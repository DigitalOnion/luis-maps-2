package com.outerspace.luismaps2

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.outerspace.luismaps2.repositories.WorldLocationDatabase
import com.outerspace.luismaps2.viewModels.LocationViewModel
import com.outerspace.luismaps2.domain.WorldLocation
import com.outerspace.luismaps2.repositories.RemindersLocalRepository
import com.outerspace.luismaps2.repositories.WorldLocationDao
import com.outerspace.luismaps2.repositories.WorldLocationEntity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import kotlin.random.Random


@RunWith(AndroidJUnit4::class)
class LocationViewModelTest {
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var locations: List<WorldLocation>
    private lateinit var locationVM: LocationViewModel
    private lateinit var db: WorldLocationDatabase
    private lateinit var dao: WorldLocationDao

    @Before
    fun initialization() {
        locations = listOf<WorldLocation>(
            WorldLocation(19.4326, 99.1332),  // Mexico City. Mexico
            WorldLocation(20.9674, 89.5926),  // Merida, Yucatan. Mexico
            WorldLocation(33.7488, 84.3877),  // Atlanta, Georgia, US
            WorldLocation(
                19.5438,
                96.9102,
                "Xalapa",
                "Xalapa is a cultural city. Capital of the Veracruz State, Xalapa has theaters, museums, cafes, Universities. It holds all the Government's offices, Courts, and the Palace of Government"
            ),  // Xalapa, Veracruz. Mexico
            WorldLocation(
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
    fun compareWorldLocationsTest() {
        // WorldLocation equality test. It compares the coordinates
        assert(locations[0] != WorldLocation(20.9674, 89.5926))         // Equality
        assert(locations[1] == WorldLocation(20.9674, 89.5926))         // Inequality

        assert(
            locations[3] == WorldLocation(
                19.5438,
                96.9102
            )
        )         // Equality of bare WorldLocation vs WorldLocation with title and description

        // Equality over secondary constructors: LatLng and WorldLocationEntity
        assert(locations[2] == WorldLocation(LatLng(33.7488, 84.3877)))
        assert(
            locations[2] == WorldLocation(
                WorldLocationEntity(
                    0,
                    33.7488, 84.3877,
                    "Any-City",
                    "Whatever it might look like.",
                    false
                )
            )
        )
    }

    @Test
    fun locationViewModel_addLocation_Test() {
        val tepoztlanLocation = WorldLocation(
            18.9848, 99.0931,
            "Tepoztlán, Morelos. Mexico",
            "Tepoztlán is a small, touristy and very cultural town in the middle of Mexico"
        )
        var observedLocation = WorldLocation(0.0, 0.0)

        try {
            val latch = CountDownLatch(1)       // latch to wait for the observer to finish

            // prepares observer
            locationVM.mutableAddedPoi.observeForever {
                observedLocation = it

                // verifies the observed value is equal (equality) to the added value
                // WordLocation equality only compares lat and lon
                assert(observedLocation == tepoztlanLocation)

                // verifies that each and all fields are stored and retrieved (title and description)
                assert(observedLocation.title == tepoztlanLocation.title)
                assert(observedLocation.description == tepoztlanLocation.description)
                assert(observedLocation.lat == tepoztlanLocation.lat)
                assert(observedLocation.lon == tepoztlanLocation.lon)
                latch.countDown()
            }

            // adds a location to the View Model. This will trigger the observer
            // assigning a the value to the observedLocation
            locationVM.addOrUpdateLocation(tepoztlanLocation)

            latch.await()

            // verifies the added value is also in the database
            val retrievedLocations = dao.getLocations()
            assert(retrievedLocations.size == 1)
            val retrievedLocation = WorldLocation(retrievedLocations[0])

            // verifies that each and all fields are present in the database
            assert(retrievedLocation == tepoztlanLocation)
            assert(retrievedLocation.title == tepoztlanLocation.title)
            assert(retrievedLocation.description == tepoztlanLocation.description)
            assert(retrievedLocation.lat == tepoztlanLocation.lat)
            assert(retrievedLocation.lon == tepoztlanLocation.lon)
        } catch (e: Error) {
            println(e.message)
            assert(false)
        }
    }

    @Test
    fun locationViewModel_multiple_addLocation_Test() {
        try {
            // add various locations through the View Model
            locations.forEach { location -> locationVM.addOrUpdateLocation(location) }

            // retrieve locations from database
            val retrievedLocations = db.worldLocationDao().getLocations()
            assert(retrievedLocations.size == locations.size)

            // verify each added location by reading it back from the database.
            locations.forEach { location ->
                assert(retrievedLocations.find {
                    WorldLocation(it) == location
                } != null
                )
            }

        } finally {
            db.close()
        }
    }
}

