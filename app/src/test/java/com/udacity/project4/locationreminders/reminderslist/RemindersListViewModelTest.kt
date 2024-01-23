package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.domain.WorldLocation
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.repositories.WorldLocationEntity
import com.udacity.project4.viewModels.LocationViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    //TODO: provide testing to the RemindersListViewModel and its live data objects

    private fun getTepoztlanLocation() = WorldLocation(
    0, 18.9848, 99.0931,
    "Tepoztlán, Morelos. Mexico",
    "Tepoztlán is a small, touristy and very cultural town in the middle of Mexico"
    )

    private val guanajuatoLocation = WorldLocation(
        0, 21.0190, -101.2574,            "Guanajuato, Guanajuato. Mexico",
        "Guanajuato is the capital of the Guanajuato State, with a powerful economy built around a picturesque, culture rich downtown."
    )

    @get: Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var locationVM: LocationViewModel
    private val dataSource = FakeDataSource()

    @Before
    fun initialization() {
        locationVM = LocationViewModel(dataSource)
    }

    private fun WorldLocation.toWorldLocationEntity(): WorldLocationEntity =
        WorldLocationEntity(id, lat, lon, title, description, isMapsPoi)

    private fun WorldLocationEntity.toWorldLocation(): WorldLocation =
        WorldLocation(locationId, lat, lon, title, description, isMapsPoi)

    @Test
    fun locationViewModel_getLocations_test() {
        var list: List<WorldLocationEntity>
        runBlocking {
            list = locationVM.getLocations()
        }

        assert(list.size == dataSource.size)
        for (location in list) {
            dataSource.contains(location)
        }
    }

    @Test
    fun locationViewModel_addOrUpdateLocation_Test() {
        val newTitle = "Tepoz"
        val newDescription  = "Beautiful Town"
        val newLat = 19.0
        val newLon = -99.00

        val tepoztlanLocation = getTepoztlanLocation()

        runBlocking {
            locationVM.addOrUpdateLocation(tepoztlanLocation) // adds location, it is not in the database
        }

        assert(dataSource.contains(tepoztlanLocation.toWorldLocationEntity()))
        assert(!dataSource.contains(guanajuatoLocation.toWorldLocationEntity()))

        tepoztlanLocation.title = newTitle
        tepoztlanLocation.description = newDescription
        tepoztlanLocation.lat = newLat
        tepoztlanLocation.lon = newLon

        runBlocking {
            locationVM.addOrUpdateLocation(tepoztlanLocation) // updates location, it is in the database
        }

        var recoveredLocation: WorldLocationEntity? = null
        runBlocking {
            recoveredLocation = locationVM.getLocations().find {it.locationId == tepoztlanLocation.id}
        }
        // update can change Title or Description
        assert(recoveredLocation?.title == newTitle)
        assert(recoveredLocation?.description == newDescription)

        // update should not change Lat and Lon, The location is specific of its coordinates
        assert(recoveredLocation?.lat != newLat)
        assert(recoveredLocation?.lon != newLon)
    }

    @Test
    fun locationViewModel_removeLocation_Test() {
        val list: MutableList<WorldLocationEntity>
        runBlocking {
            list = locationVM.getLocations().toMutableList()
        }
        while(list.isNotEmpty()) {
            runTest {
                val location = list.first()
                locationVM.removeLocation(location.toWorldLocation())
                list.remove(location)
                assert(locationVM.getLocations().size == list.size)
            }
        }
    }

    /**
     * check https://knowledge.udacity.com/questions/965879
     */

    @Test
    fun countDownLatch_Test() {
        val live = MutableLiveData<Int>()
        val observedSignal = CountDownLatch(1)

        suspend fun doSomething() {
            delay(500)
            live.value = 1
        }

        runTest {
            live.observeForever {
                runBlocking {
                    println("We are in the observer")
                    observedSignal.countDown()
                }
            }
            doSomething()
            observedSignal.await()
        }
        println("outside the runTest coroutine")
    }

    // test for the mutableAddedPoi MutableLiveData
    @Test
    fun locationViewModel_mutableAddedPoi_Test() {
        val tepoztlanLocation = getTepoztlanLocation()

        // prepares observer
        locationVM.mutableAddedPoi.observeForever {
            // verifies the observed value is equal (equality) to the added value
            // WordLocation equality only compares lat and lon
            assert(it == tepoztlanLocation)

            // verifies that each and all fields are stored and retrieved (title and description)
            assert(it.title == tepoztlanLocation.title)
            assert(it.description == tepoztlanLocation.description)
            // addOrUpdateLocation relies on a database table which is simulated by a map
            // in the FakeDataSource. The latest added entry should be the one with the max id
            runTest {
                val retrievedLocations = locationVM.getLocations()
                val idx = retrievedLocations.maxOfOrNull { it.locationId } ?: 0
                val retrievedLocation = retrievedLocations.first { it.locationId == idx }.toWorldLocation()

                // verifies that each and all fields are present in the database
                assert(retrievedLocation == tepoztlanLocation)
                assert(retrievedLocation.title == tepoztlanLocation.title)
                assert(retrievedLocation.description == tepoztlanLocation.description)
                assert(retrievedLocation.lat == tepoztlanLocation.lat)
                assert(retrievedLocation.lon == tepoztlanLocation.lon)
            }
        }

        // adds a location to the View Model. This will trigger the observer
        // assigning a the value to the observedLocation
        runTest {
            locationVM.addOrUpdateLocation(tepoztlanLocation)

            // addOrUpdateLocation relies on a database table which is simulated by a map
            // in the FakeDataSource. The latest added entry should be the one with the max id
            val retrievedLocations = locationVM.getLocations()
            val idx = retrievedLocations.maxOfOrNull { it.locationId } ?: 0
            val retrievedLocation = retrievedLocations.first { it.locationId == idx }.toWorldLocation()

            // verifies that each and all fields are present in the database
            assert(retrievedLocation == tepoztlanLocation)
            assert(retrievedLocation.title == tepoztlanLocation.title)
            assert(retrievedLocation.description == tepoztlanLocation.description)
            assert(retrievedLocation.lat == tepoztlanLocation.lat)
            assert(retrievedLocation.lon == tepoztlanLocation.lon)
        }
    }

    @Test
    fun locationViewModel_mutableCurrentLocation_Test() {
        val lat = Random.nextDouble()
        val lon = Random.nextDouble()
        val wlb = WorldLocation(lat, lon)
        locationVM.mutableCurrentLocation.observeForever {
            assert(it.lat == lat)
            assert(it.lon == lon)
        }
        locationVM.mutableCurrentLocation.value = wlb
    }

    // addOrUpdateLocation calls refreshPoiList which in turn triggers the mutablePoiList
    // livedata observer.
    @Test
    fun locationViewModel_mutablePoiList_Test() {
        val tepoztlanLocation = getTepoztlanLocation()

        locationVM.mutablePoiList.observeForever {
            runTest {
                val retrievedLocations = locationVM.getLocations()
                val idx = retrievedLocations.maxOfOrNull { it.locationId } ?: 0
                val retrievedLocation = retrievedLocations.first { it.locationId == idx }.toWorldLocation()

                assert( retrievedLocation == tepoztlanLocation)
            }
        }

        runTest {
            locationVM.addOrUpdateLocation(tepoztlanLocation)
        }
    }

    @Test
    fun locationViewModel_mutableDeletePoi_Test() {
        var location: WorldLocation? = null
        var listSize = 0

        locationVM.mutablePoiList.observeForever {
            assert(it.size == listSize - 1)
            for (l in it) {
                assert(l != location)
            }
        }

        locationVM.mutableDeletedPoi.observeForever {
            assert(it == location)
        }

        runTest {
            val list = locationVM.getLocations()
            listSize = list.size
            val idx = Random.nextInt(list.size - 1)
            location = list[idx].toWorldLocation()
            locationVM.removeLocation(location!!)
        }
    }
}