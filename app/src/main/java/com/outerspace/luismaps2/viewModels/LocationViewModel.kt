package com.outerspace.luismaps2.viewModels

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.outerspace.luismaps2.domain.WorldLocation
import com.outerspace.luismaps2.repositories.LocationDatabase
import com.outerspace.luismaps2.repositories.WorldLocationEntity
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference

const val LOCATION_DATABASE_NAME = "location"
const val LOCATION_REFRESH_PERIOD = 2L * 1000L        // 2 seconds
const val LONDON_LAT = "london_lat"
const val LONDON_LON = "london_lon"
val LONDON_LOCATION = WorldLocation (51.5072, 0.1276, "London", "Great City of London")

class LocationViewModel: ViewModel() {
    val mutablePoiList: MutableLiveData<MutableList<WorldLocation>> = MutableLiveData()
    val mutableCurrentLocation: MutableLiveData<WorldLocation> = MutableLiveData()
    val mutableAddedPoi: MutableLiveData<WorldLocation> = MutableLiveData()
    val mutableDeletedPoi: MutableLiveData<WorldLocation> = MutableLiveData()

    var jumpToLocation = true

    var locationDb: LocationDatabase? = null
        set(db) {
            field = db
            viewModelScope.launch { populatePoiList(locationDb) }
        }

    private fun populatePoiList(db: LocationDatabase?): Deferred<List<WorldLocation>> {
        if (db != null) {
            val d = viewModelScope.async(Dispatchers.IO) {
                db.worldLocationDao().getLocations().map { WorldLocation(it) }
            }
            return d
        }
        return viewModelScope.async { listOf() }
    }

    fun refreshPoiList() {
        var l: List<WorldLocation>
        runBlocking(Dispatchers.IO) {
            l = populatePoiList(locationDb).await()
        }
        mutablePoiList.value = mutableListOf()
        mutablePoiList.value?.addAll(l)
    }

    fun addOrUpdateLocation(location: WorldLocation) {
        runBlocking(Dispatchers.IO) {
            val dao = locationDb!!.worldLocationDao()
            val locationsAt = dao.getLocationAt(location.id)
            if (locationsAt.isNotEmpty()) {
                dao.updateLocationAt(location.id, location.title, location.description)
            } else {
                location.id = dao.insert(WorldLocationEntity(location))
            }
            viewModelScope.launch(Dispatchers.Main) {
                refreshPoiList()
                mutableAddedPoi.value = location
            }
        }
    }

    fun removeLocation(location: WorldLocation) {
        runBlocking(Dispatchers.IO) {
            val dao = locationDb!!.worldLocationDao()
            dao.deleteLocationAt(location.id)
            viewModelScope.launch(Dispatchers.Main) {
                refreshPoiList()
                mutableDeletedPoi.value = location
            }
        }
    }

    fun deleteAllLocations(activity:ComponentActivity, geofenceVM: GeofenceViewModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val allLocations = locationDb!!.worldLocationDao().getLocations()
            allLocations.forEach {
                geofenceVM.remove(activity, WorldLocation(it))
            }
            locationDb!!.worldLocationDao().deleteAll()
            mutablePoiList.value?.clear()
        }
        viewModelScope.launch(Dispatchers.Main) {
            refreshPoiList()
        }
    }
}

