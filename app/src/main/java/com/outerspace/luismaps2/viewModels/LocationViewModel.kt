package com.outerspace.luismaps2.viewModels

import androidx.activity.ComponentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.outerspace.luismaps2.domain.WorldLocation
import com.outerspace.luismaps2.repositories.RemindersLocalRepository
import com.outerspace.luismaps2.repositories.WorldLocationEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

const val LOCATION_DATABASE_NAME = "location"
const val LOCATION_REFRESH_PERIOD = 2L * 1000L        // 2 seconds
const val LONDON_LAT = "london_lat"
const val LONDON_LON = "london_lon"
val LONDON_LOCATION = WorldLocation (51.5072, 0.1276, "London", "Great City of London")

@HiltViewModel
class LocationViewModel
    @Inject constructor(private val remindersRepo: RemindersLocalRepository)
    : ViewModel() {

    val mutablePoiList: MutableLiveData<MutableList<WorldLocation>> = MutableLiveData()
    val mutableCurrentLocation: MutableLiveData<WorldLocation> = MutableLiveData()
    val mutableAddedPoi: MutableLiveData<WorldLocation> = MutableLiveData()
    val mutableDeletedPoi: MutableLiveData<WorldLocation> = MutableLiveData()

    var jumpToLocation = true

    fun getLocations() = remindersRepo.getLocations()

    fun refreshPoiList() {
        var resultList: MutableList<WorldLocation>
        runBlocking(Dispatchers.IO) {
            resultList = remindersRepo.getLocations().map { WorldLocation(it) }.toMutableList()
        }
        mutablePoiList.value = resultList
    }

    fun addOrUpdateLocation(location: WorldLocation) {
        runBlocking(Dispatchers.IO) {
            val locationsAt = remindersRepo.getLocationAt(location.id)
            if (locationsAt.isNotEmpty()) {
                remindersRepo.updateLocationAt(location.id, location.title, location.description)
            } else {
                location.id = remindersRepo.insert(WorldLocationEntity(location))
            }
            viewModelScope.launch(Dispatchers.Main) {
                refreshPoiList()
                mutableAddedPoi.value = location
            }
        }
    }

    fun removeLocation(location: WorldLocation) {
        runBlocking(Dispatchers.IO) {
            remindersRepo.deleteLocationAt(location.id)
            viewModelScope.launch(Dispatchers.Main) {
                refreshPoiList()
                mutableDeletedPoi.value = location
            }
        }
    }

    fun deleteAllLocations(activity:ComponentActivity, geofenceVM: GeofenceViewModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val allLocations = remindersRepo.getLocations()
            allLocations.forEach {
                geofenceVM.remove(activity, WorldLocation(it))
            }
            remindersRepo.deleteAll()
            mutablePoiList.value?.clear()
        }
        viewModelScope.launch(Dispatchers.Main) {
            refreshPoiList()
        }
    }
}

