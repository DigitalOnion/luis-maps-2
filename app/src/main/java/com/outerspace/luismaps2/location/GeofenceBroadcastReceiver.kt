package com.outerspace.luismaps2.location

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
        val bundle = intent.extras as Bundle

        if (bundle == null || geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes
                .getStatusCodeString(geofencingEvent.errorCode)
            Log.e(LOG_TAG, errorMessage)
            return
        }
        val transition = geofencingEvent.geofenceTransition
        val geofenceMessage = getTransitionMessage(context, transition)
        Toast.makeText(context, geofenceMessage, Toast.LENGTH_SHORT).show()

        when(transition) {
            Geofence.GEOFENCE_TRANSITION_DWELL -> {
//                val triggeringGeofences = geofencingEvent.triggeringGeofences
                createNotificationChannel(context)
                sendNotification(context, bundle)
            }
            Geofence.GEOFENCE_TRANSITION_ENTER ->
                Log.d(LOG_TAG, getString(context, R.string.entering_geofence))
            Geofence.GEOFENCE_TRANSITION_EXIT ->
                Log.d(LOG_TAG, getString(context, R.string.exiting_geofence))
            else ->
                Log.d(LOG_TAG, getString(context, R.string.geofence_transition_invalid_type))
        }
    }
//
//    private fun getGeofenceTransitionDetails(
//        context: Context,
//        transition: Int,
//        geofences: List<Geofence>?): String {
//        val geo = geofences!!
//            .map {geofence -> "(${geofence.latitude}, ${geofence.longitude}) "}
//            .reduce { acc, geofence -> acc + geofence }
//
//        return "${getTransitionMessage(context, transition)}: $geo"
//    }

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

    private fun sendNotification(context: Context, bundle: Bundle) {
        val errorMessage = context.getString(R.string.geofence_error)
        var notificationTitle: String = getString(context, R.string.app_title)
        var notificationText: String = errorMessage
        if (bundle.containsKey(GEOFENCE_NOTIFICATION_KEY)) {
            val info = bundle.getString(GEOFENCE_NOTIFICATION_KEY)?.split("|")
            if (info != null && info.size == 2) {
                notificationTitle = "${notificationTitle} - ${info[0]}"
                notificationText = "${info[1]}"
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
        notificationManager?.notify(GEOFENCE_NOTIFICATION_ID, builder.build())
    }
}