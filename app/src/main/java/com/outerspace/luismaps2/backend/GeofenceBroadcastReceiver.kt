package com.outerspace.luismaps2.backend

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getString
import androidx.core.os.bundleOf
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.outerspace.luismaps2.R
import com.outerspace.luismaps2.domain.LOG_TAG
import com.outerspace.luismaps2.view.POIDetailsActivity
import com.outerspace.luismaps2.viewModels.GEOFENCE_NOTIFICATION_DESCRIPTION_KEY
import com.outerspace.luismaps2.viewModels.GEOFENCE_NOTIFICATION_ID_KEY
import com.outerspace.luismaps2.viewModels.GEOFENCE_NOTIFICATION_TITLE_KEY


const val CHANNEL_ID = "Luis_Maps_2_notifications_channel_id"
const val GEOFENCE_NOTIFICATION_ID = 872 // random number
const val REQUEST_CODE = 0

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        val bundle = intent.extras as Bundle

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes
                .getStatusCodeString(geofencingEvent.errorCode)
            Log.e(LOG_TAG, errorMessage)
            return
        }
        val transition = geofencingEvent.geofenceTransition
        val geofenceMessage = getTransitionMessage(context, transition)
        val spannableMessage = Html.fromHtml(context.getString(R.string.geofence_toast, geofenceMessage), Html.FROM_HTML_MODE_LEGACY)

        val toast = Toast.makeText(context, spannableMessage, Toast.LENGTH_SHORT)

        toast.show()

        when(transition) {
            Geofence.GEOFENCE_TRANSITION_DWELL -> {
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
        val notificationTitle = bundle.getString(
            GEOFENCE_NOTIFICATION_TITLE_KEY, errorMessage)
        val notificationText = bundle.getString(
            GEOFENCE_NOTIFICATION_DESCRIPTION_KEY, errorMessage
        )
        val locationId = bundle.getLong(GEOFENCE_NOTIFICATION_ID_KEY, 0)

        val detailsBundle = bundleOf(
            Pair(GEOFENCE_NOTIFICATION_TITLE_KEY, notificationTitle),
            Pair(GEOFENCE_NOTIFICATION_DESCRIPTION_KEY, notificationText),
            Pair(GEOFENCE_NOTIFICATION_ID_KEY, locationId)
        )
        val intent = Intent(context, POIDetailsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtras(detailsBundle)
        }

        val pendingIntent = PendingIntent.getActivity(context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)

        val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
        notificationManager?.notify(GEOFENCE_NOTIFICATION_ID, builder.build())
    }
}