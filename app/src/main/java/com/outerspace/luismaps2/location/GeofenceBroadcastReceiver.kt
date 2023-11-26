package com.outerspace.luismaps2.location

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getString
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.outerspace.luismaps2.R

const val LOG_TAG = "LUIS"
const val CHANNEL_ID = "Luis_Maps_2_notifications_channel_id"
const val GEOFENCE_NOTIFICATION_ID = 872 // random number

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes
                .getStatusCodeString(geofencingEvent.errorCode)
            Log.e(LOG_TAG, errorMessage)
            return
        }
        val transition = geofencingEvent.geofenceTransition
        val geofenceMessage = getTransitionMessage(context, transition)
        Toast.makeText(context, geofenceMessage, Toast.LENGTH_SHORT).show()
        if (transition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            val geofenceTransitionDetails = getGeofenceTransitionDetails(
                context,
                transition,
                triggeringGeofences
            )
            createNotificationChannel(context)
            sendNotification(context, geofenceTransitionDetails)
        } else {
            Log.e(LOG_TAG, getString(context ?: return,
                    R.string.geofence_transition_invalid_type))
        }
    }

    private fun getGeofenceTransitionDetails(
        context: Context,
        transition: Int,
        geofences: List<Geofence>?): String {
        val geo = geofences!!
            .map {geofence -> "(${geofence.latitude}, ${geofence.longitude}) "}
            .reduce { acc, geofence -> acc + geofence }

        return "${getTransitionMessage(context, transition)}: $geo"
    }

    private fun getTransitionMessage(context:Context, transition: Int): String {
        return when(transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> context.getString(R.string.entering_geofence)
            Geofence.GEOFENCE_TRANSITION_EXIT -> context.getString(R.string.exiting_geofence)
            Geofence.GEOFENCE_TRANSITION_DWELL -> context.getString(R.string.dwelling_geofence)
            else -> context.getString(R.string.other_geofence_transition)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(context, R.string.channel_name)
            val descriptionText = getString(context, R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system.
            val notificationManager: NotificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(context: Context, details: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(getString(context, R.string.app_title))
            .setContentText(details)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
        notificationManager?.notify(GEOFENCE_NOTIFICATION_ID, builder.build())
    }
}