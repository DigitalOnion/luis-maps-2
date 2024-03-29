package com.udacity.project4.viewModels

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.lang.ref.WeakReference

fun checkPermissions(context: Context) =
    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

class PermissionsViewModel: ViewModel() {
    val mutablePermissionMap: MutableLiveData<Map<String, Boolean>> = MutableLiveData()
    val mutableAllPermissionsGranted: MutableLiveData<Boolean> = MutableLiveData(false)

    private lateinit var activityResultLauncher: ActivityResultLauncher<Array<String>>

    var weakActivity: WeakReference<ComponentActivity> = WeakReference(null)
        set(wActivity) {
            field = wActivity
            val activity = wActivity.get() ?: return
            val reqPermissions = ActivityResultContracts.RequestMultiplePermissions()
            activityResultLauncher = activity.registerForActivityResult(reqPermissions) {
                if (it.isNotEmpty()) mutablePermissionMap.value = it
            }
        }

    fun requestLocationPermissions() {
        activityResultLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ))
    }

    fun requestBackgroundPermissions() {
        activityResultLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        ))
    }
}