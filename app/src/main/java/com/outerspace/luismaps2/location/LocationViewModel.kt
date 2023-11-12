package com.outerspace.luismaps2.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference

const val LOCATION_DATABASE_NAME = "location"
val LONDON_LOCATION = WorldLocation (51.5072, 0.1276, "London", "Great City of London")

class LocationViewModel: ViewModel() {
    private var weakPermissionLauncher: WeakReference<ActivityResultLauncher<Array<String>>> = WeakReference(null)
    private var weakLocationClient: WeakReference<FusedLocationProviderClient> = WeakReference(null)

    private lateinit var mutablePermissionsMap: MutableLiveData<Map<String, Boolean>>

    val poiSet: MutableSet<WorldLocation> = mutableSetOf()

    val mutablePermissionGranted: MutableLiveData<Boolean> = MutableLiveData(false)
    val mutableCurrentLocation: MutableLiveData<WorldLocation> = MutableLiveData()
    val mutableAddedPoi: MutableLiveData<WorldLocation> = MutableLiveData()
    val mutableDeletedPoi: MutableLiveData<WorldLocation> = MutableLiveData()

    var weakActivity: WeakReference<ComponentActivity> = WeakReference(null)
        set(weakActivityArg) {
            field = weakActivityArg
            val activity: ComponentActivity = weakActivityArg.get() ?: return

            weakPermissionLauncher = WeakReference(activity.registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) {
                mutablePermissionsMap = MutableLiveData(it)
                mutablePermissionGranted.value = true
            })

            val locationClient = LocationServices.getFusedLocationProviderClient(activity)
            weakLocationClient = WeakReference(locationClient)
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

    private lateinit var previousPoi: WorldLocation

    @SuppressLint("MissingPermission")
    fun requestCurrentWorldLocation(context: Context) {
        if (isLocationPermissionGranted(context)) {
            val client = weakLocationClient.get()
            val lastLocation = client?.lastLocation
            lastLocation?.addOnSuccessListener {
                if (it != null) {
                    val worldLocation = WorldLocation(it.latitude, it.longitude)
                    if (this::previousPoi.isInitialized) {
                        poiSet.removeIf { poi -> poi.lat == previousPoi.lat && poi.lon == previousPoi.lon }
                    }
                    previousPoi = worldLocation
                    poiSet.add(worldLocation)    // current location is shown but does not get stored
                    mutableCurrentLocation.value = worldLocation
                }
            }
        } else {
            requestLocationPermissions()
        }
    }

    private fun isLocationPermissionGranted(context: Context): Boolean {
        val checkCoarse = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        val checkFine = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        return checkCoarse == PackageManager.PERMISSION_GRANTED && checkFine == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        val launcher = weakPermissionLauncher.get()
        launcher?.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
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
}