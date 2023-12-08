package com.outerspace.luismaps2.location

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

const val GEOFENCE_RADIUS_IN_METERS = 100F
const val GEOFENCE_EXPIRATION_IN_MILLISECONDS = 365L * 24L * 60L * 60L * 1000L
const val GEOFENCE_LOITERING_DELAY = 30 * 1000
const val GEOFENCE_NOTIFICATION_KEY = "geofence_notification_key"

class GeofenceViewModel: ViewModel() {

    fun add(activity:ComponentActivity, location: WorldLocation) {
        if (location.id == 0) {
            throw Error("ERROR: adding a geofence location with no ID")
        }
        val geofence = Geofence.Builder()
            .setRequestId(location.id.toString())
            .setCircularRegion(
                location.lat,
                location.lon,
                GEOFENCE_RADIUS_IN_METERS
            )
            .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT or Geofence.GEOFENCE_TRANSITION_DWELL)
            .setLoiteringDelay(GEOFENCE_LOITERING_DELAY)
            .build()

        val geofenceRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL or GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_EXIT)
            .addGeofence(geofence)
            .build()

        val geofencePendingIntent: PendingIntent by lazy {
            val bundle = Bundle()
            bundle.putString(GEOFENCE_NOTIFICATION_KEY, "${location.title}|${location.description}")
            val intent = Intent(activity, GeofenceBroadcastReceiver::class.java)
            intent.putExtras(bundle)
            PendingIntent.getBroadcast(activity, 0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        }

        val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(activity)
        val context = activity.applicationContext ?: return
        val fineLocationGranted = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val accessBackgroundGranted = context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fineLocationGranted && accessBackgroundGranted) {
            geofencingClient.addGeofences(geofenceRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    Log.d(LOG_TAG, "registerAddedGeofences ok")
                }
                addOnFailureListener {
                    Log.d(LOG_TAG, "registerAddedGeofences failed")
                }
            }
        } else {
            val msg = "missing required permissions: ${if (fineLocationGranted) "Fine Location," else ""} ${if (fineLocationGranted) "Access Background Location," else ""}"
            throw Error(msg)
        }
    }

    fun remove(activity: ComponentActivity, location: WorldLocation) {
        val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(activity)
        geofencingClient.removeGeofences(listOf(location.id.toString()))
    }
}