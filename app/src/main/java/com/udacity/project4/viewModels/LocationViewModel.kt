package com.udacity.project4.viewModels

import androidx.activity.ComponentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udacity.project4.domain.WorldLocation
import com.udacity.project4.repositories.RemindersLocalRepository
import com.udacity.project4.repositories.WorldLocationEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

const val LOCATION_DATABASE_NAME = "location"
const val LOCATION_REFRESH_PERIOD = 2L * 1000L        // 2 seconds
const val LONDON_LAT = "london_lat"
const val LONDON_LON = "london_lon"
val LONDON_LOCATION = WorldLocation (0, 51.5072, 0.1276, "London", "Great City of London")

@HiltViewModel
class LocationViewModel
    @Inject constructor(private val remindersRepo: RemindersLocalRepository)
    : ViewModel() {

    val mutablePoiList: MutableLiveData<MutableList<WorldLocation>> = MutableLiveData()
    val mutableCurrentLocation: MutableLiveData<WorldLocation> = MutableLiveData()

    var jumpToLocation = true

    suspend fun getLocations() = remindersRepo.getLocations()

    fun refreshPoiList() {
        viewModelScope.launch(Dispatchers.IO) {
            val resultList = remindersRepo.getLocations().map { WorldLocation(it) }.toMutableList()
            withContext(Dispatchers.Main) {
                mutablePoiList.value = resultList
            }
        }
    }

    suspend fun addOrUpdateLocation(location: WorldLocation) {
        val locationsAt = remindersRepo.getLocationAt(location.id)
        if (locationsAt.isNotEmpty()) {
            remindersRepo.updateLocationAt(location.id, location.title, location.description)
        } else {
            location.id = remindersRepo.insert(WorldLocationEntity(location))
        }
        refreshPoiList()
    }

    suspend fun removeLocation(location: WorldLocation) {
        remindersRepo.deleteLocationAt(location.id)
        refreshPoiList()
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

