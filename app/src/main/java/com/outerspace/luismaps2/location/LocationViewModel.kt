package com.outerspace.luismaps2.location

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
import com.google.android.gms.location.LastLocationRequest
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference

const val LOCATION_DATABASE_NAME = "location"
const val LOCATION_REFRESH_PERIOD = 2L * 1000L        // 2 seconds
val LONDON_LOCATION = WorldLocation (51.5072, 0.1276, "London", "Great City of London")

class LocationViewModel: ViewModel() {
    val poiSet: MutableSet<WorldLocation> = mutableSetOf()

    val mutableCurrentLocation: MutableLiveData<WorldLocation> = MutableLiveData()
    val mutableAddedPoi: MutableLiveData<WorldLocation> = MutableLiveData()
    val mutableDeletedPoi: MutableLiveData<WorldLocation> = MutableLiveData()
    val mutableHasPermissions: MutableLiveData<Boolean> = MutableLiveData()

    private lateinit var locationFlow: Flow<WorldLocation>

    var weakActivity: WeakReference<ComponentActivity> = WeakReference(null)
        @SuppressLint("MissingPermission")
        set(weakActivityArg) {
            field = weakActivityArg
            val activity: ComponentActivity = weakActivityArg.get() ?: return

            // the createRequest and other API is deprecated. I found the new api in: https://tomas-repcik.medium.com/locationrequest-create-got-deprecated-how-to-fix-it-e4f814138764
            locationFlow = flow {
                val onSuccess = { it: Location? ->
                     if (it != null) {
                        val wl = WorldLocation(it.latitude, it.longitude)
                        wl.current = true
                        mutableCurrentLocation.value =wl
                    }
                }

                val callback = object: LocationCallback() {
                    override fun onLocationAvailability(availability: LocationAvailability) {}
                    override fun onLocationResult(lResult: LocationResult) {
                        super.onLocationResult(lResult)
                        val l = lResult.lastLocation
                        if (l != null) {
                            val wl = WorldLocation(l.latitude, l.longitude)
                            wl.current = true
                            mutableCurrentLocation.value = wl
                        }

                    }
                }

                fun createCurrentRequest(): CurrentLocationRequest = CurrentLocationRequest
                    .Builder()
                    .setDurationMillis(LOCATION_REFRESH_PERIOD)
                    .setGranularity(Granularity.GRANULARITY_FINE)
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .build()

                fun createLocationRequest(): LocationRequest = LocationRequest
                    .Builder(LOCATION_REFRESH_PERIOD)
                    .setGranularity(Granularity.GRANULARITY_FINE)
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .build()

                val context: Context = weakActivity.get()?.applicationContext ?: throw Error("LocationViewModel.WeakActivity is not valid")
                if (checkPermissions(context)) {
                    val locationClient = LocationServices.getFusedLocationProviderClient(context)
                    locationClient.getCurrentLocation(createCurrentRequest(), null)
                        .addOnSuccessListener(onSuccess)
                    locationClient.requestLocationUpdates(createLocationRequest(), callback, null)
                }
            }
        }

    fun startCurrentLocationFlow(owner: LifecycleOwner) {
        if (checkPermissions(owner as Context)) {
            owner.lifecycleScope.launch {
                locationFlow.collect {
                    it.current = true
                    mutableCurrentLocation.value = it
                }
            }
        } else {
            mutableHasPermissions.value = false
        }
    }

    var locationDb: LocationDatabase? = null
        set(db) {
            field = db
            viewModelScope.launch {
                if (db != null) {
                    val d = viewModelScope.async(Dispatchers.IO) {
                        db.worldLocationDao().getLocations().map {WorldLocation(it)}
                    }
                    val locations = d.await()
                    poiSet.addAll(locations)
                }
            }
        }

    fun addLocation(location: WorldLocation) {
        poiSet.add(location)                    // user's POI are stored
        runBlocking(Dispatchers.IO) {
            val dao = locationDb!!.worldLocationDao()
            dao.insert(WorldLocationEntity(location))
            viewModelScope.launch(Dispatchers.Main) {
                mutableAddedPoi.value = location
            }
        }
    }

    fun removeLocation(location: WorldLocation) {
        poiSet.remove(location)
        runBlocking(Dispatchers.IO) {
            val dao = locationDb!!.worldLocationDao()
            dao.deleteLocationAt(location.lat, location.lon)
            viewModelScope.launch(Dispatchers.Main) {
                mutableDeletedPoi.value = location
            }
        }
    }

    fun deleteAllLocations() {
        viewModelScope.launch(Dispatchers.IO) {
            locationDb!!.worldLocationDao().deleteAll()
        }
    }
}

