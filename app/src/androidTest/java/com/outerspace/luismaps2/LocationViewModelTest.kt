package com.outerspace.luismaps2

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import androidx.room.Room.inMemoryDatabaseBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.outerspace.luismaps2.location.LocationDatabase
import com.outerspace.luismaps2.location.LocationViewModel
import com.outerspace.luismaps2.location.WorldLocation
import com.outerspace.luismaps2.location.WorldLocationEntity
import com.outerspace.luismaps2.view.MapsActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.ref.WeakReference
import kotlin.random.Random


@RunWith(AndroidJUnit4::class)
class LocationViewModelTest {
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val activityRule = ActivityScenarioRule(MapsActivity::class.java)

    private lateinit var locations: List<WorldLocation>
    private lateinit var db: LocationDatabase
    private lateinit var locationVM: LocationViewModel


    @Before
    fun initialization() {
        locations = listOf<WorldLocation>(
            WorldLocation(19.4326, 99.1332),  // Mexico City. Mexico
            WorldLocation(20.9674, 89.5926),  // Merida, Yucatan. Mexico
            WorldLocation(33.7488, 84.3877),  // Atlanta, Georgia, US
            WorldLocation(19.5438, 96.9102, "Xalapa", "Xalapa is a cultural city. It has theaters and Universities. It is the capital of Veracruz"),  // Xalapa, Veracruz. Mexico
            WorldLocation(25.7617, 80.1918, "Miami", "Miami is a coastal touristic city, famous for art, like movies and music, and night life."), // Miami, FL, US
        )

        db = inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LocationDatabase::class.java
        ).build()

        locationVM = LocationViewModel()
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

        assert(locations[3] == WorldLocation(19.5438, 96.9102))         // Equality of bare WorldLocation vs WorldLocation with title and description

        // Equality over secondary constructors: LatLng and WorldLocationEntity
        assert(locations[2] == WorldLocation(LatLng(33.7488, 84.3877)))
        assert(locations[2] == WorldLocation(WorldLocationEntity(0, 33.7488, 84.3877, "Any-City", "Whatever it might look like.")))
    }

    @Test
    fun locationViewModel_addLocation_Test() {
        locationVM.weakActivity = WeakReference(null)  // not required. It is needed for Google Login
        locationVM.locationDb = db

        val tepoztlanLocation = WorldLocation(18.9848, 99.0931, "Tepoztlán, Morelos. Mexico", "Tepoztlán is a small, touristy and very cultural town in the middle of Mexico")
        var observedLocation = WorldLocation(0.0, 0.0)
        val observer: Observer<WorldLocation> = Observer { observedLocation = it  }

        try {
            // prepares observer
            locationVM.mutableAddedPoi.observeForever(observer)

            // adds a location to the View Model. This will trigger the observer
            // assigning a the value to the observedLocation
            locationVM.addLocation(tepoztlanLocation)

            // verifies the observed value is equal (equality) to the added value
            // WordLocation equality only compares lat and lon
            assert(observedLocation == tepoztlanLocation)

            // verifies that each and all fields are stored and retrieved (title and description)
            assert(observedLocation.title == tepoztlanLocation.title)
            assert(observedLocation.description == tepoztlanLocation.description)
            assert(observedLocation.lat == tepoztlanLocation.lat)
            assert(observedLocation.lon == tepoztlanLocation.lon)

            // verifies the added value is also in the database
            val retrievedLocations = db.worldLocationDao().getLocations()
            assert(retrievedLocations.size == 1)
            val worldLocation = WorldLocation(retrievedLocations[0])

            // verifies that each and all fields are present in the database
            assert(worldLocation == tepoztlanLocation)
            assert(observedLocation.title == tepoztlanLocation.title)
            assert(observedLocation.description == tepoztlanLocation.description)
            assert(observedLocation.lat == tepoztlanLocation.lat)
            assert(observedLocation.lon == tepoztlanLocation.lon)
        } finally {
            locationVM.mutableAddedPoi.removeObserver(observer)
            db.close()
        }
    }

        @Test
    fun locationViewModel_multiple_addLocation_Test() {
        locationVM.weakActivity = WeakReference(null)  // not required. It is needed for Google Login
        locationVM.locationDb = db

        try {
            // add various locations through the View Model
            locations.forEach { location -> locationVM.addLocation(location) }

            // retrieve locations from database
            val retrievedLocations = db.worldLocationDao().getLocations()
            assert(retrievedLocations.size == locations.size)

            // verify each added location by reading it back from the database.
            locations.forEach { location ->
                assert(retrievedLocations.find {
                    WorldLocation(it) == location} != null
                ) }

        } finally {
            db.close()
        }
    }

    @Test
    fun locationViewModel_removeLocation_Test() {
        locationVM.weakActivity = WeakReference(null)  // not required. It is needed for Google Login
        locationVM.locationDb = db

        var observedLocation = WorldLocation(0.0, 0.0)
        val observer: Observer<WorldLocation> = Observer {
            observedLocation = it
        }

        locationVM.mutableDeletedPoi.observeForever(observer)

        try {
            // add various locations through the View Model
            locations.forEach { location -> locationVM.addLocation(location) }

            // removes one location, any location.
            val idx = Random.nextInt(0, locations.size)
            locationVM.removeLocation(locations[idx])

            // poiSet is now missing an element
            assert(locationVM.poiSet.size == locations.size - 1)
            var nPresent = 0
            var nMissing = 0

            // counts how many are missing
            locations.forEach {
                nMissing += if (!locationVM.poiSet.contains(it)) 1 else 0
                nPresent += if (locationVM.poiSet.contains(it)) 1 else 0
            }
            assert(nPresent == locations.size - nMissing)
            assert(nMissing + nPresent == locations.size)

            val retrievedLocations = db.worldLocationDao().getLocations().map { WorldLocation(it) }
            assert(retrievedLocations.size == locations.size - 1)

            // verify that each reminding locations are present in the poiSet.
            retrievedLocations.forEach { location ->
                assert(locationVM.poiSet.contains( location ))
            }

        } finally {
            locationVM.mutableDeletedPoi.removeObserver(observer)
            db.close()
        }
    }
}

// NOTE: the following would be an instrumented test with AndroidX Test.
//
//@Test
//fun locationViewModel_removeLocation_Test() {
//    activityRule.scenario
//        .moveToState(Lifecycle.State.CREATED)
//        .onActivity { activity ->
//            locationVM.weakActivity = WeakReference(activity)
//            locationVM.locationDb = db
//
//            // add locations through the View Model
//            locations.forEach {location -> locationVM.addLocation(location) }
//
//            // delete one location
//            locationVM.removeLocation(locations[2])
//
//            assert(locationVM.poiSet.size == locations.size - 1)
//            assert(!locationVM.poiSet.contains(locations[2]))
//        }
//}





