package com.udacity.project4.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import com.outerspace.luismaps2.R
import com.udacity.project4.view.ui.theme.LuisMaps2Theme
import com.udacity.project4.viewModels.GEOFENCE_NOTIFICATION_DESCRIPTION_KEY
import com.udacity.project4.viewModels.GEOFENCE_NOTIFICATION_ID_KEY
import com.udacity.project4.viewModels.GEOFENCE_NOTIFICATION_TITLE_KEY

const val NO_TITLE = "Unknown Reminder Title"
const val NO_DESCRIPTION = "Unknown Reminder Description"

class POIDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = intent.extras

        setContent {
            LuisMaps2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (bundle != null)
                        showReminder(bundle!!, Modifier)
                    else
                        showReminderError()
                }
            }
        }
    }
}



@Composable
fun showReminder(bundle: Bundle, modifier: Modifier = Modifier) {
    val reminderId = bundle.getLong(GEOFENCE_NOTIFICATION_ID_KEY, 0)!!
    val reminderTitle = bundle.getString(GEOFENCE_NOTIFICATION_TITLE_KEY, NO_TITLE)
    val reminderDescription = bundle.getString(GEOFENCE_NOTIFICATION_DESCRIPTION_KEY, NO_DESCRIPTION)

    Column {
        Text(text = stringResource(id = R.string.poi_details_activity_title),
            modifier = modifier.offset(y = 4.dp),
            style = MaterialTheme.typography.titleMedium)
        Text(text = stringResource(id = R.string.poi_details_title),
            modifier = modifier.offset(y = 16.dp),
            style = MaterialTheme.typography.labelSmall)
        Text(text = reminderTitle,
            modifier = modifier.offset(y = 8.dp, x = 16.dp),
            style = MaterialTheme.typography.titleLarge)
        Text(text = stringResource(id = R.string.poi_details_description),
            modifier = modifier.offset(y = 12.dp),
            style = MaterialTheme.typography.labelSmall)
        Text(text = reminderDescription,
            modifier = modifier.offset(y = 8.dp, x = 16.dp),
            style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun showReminderError() {

}

@Preview(showBackground = true)
@Composable
fun showReminderPreview() {
    val detailsBundle = bundleOf(
        Pair(GEOFENCE_NOTIFICATION_TITLE_KEY, "Rev Cafe"),
        Pair(GEOFENCE_NOTIFICATION_DESCRIPTION_KEY, "Have a large coffee with cream."),
        Pair(GEOFENCE_NOTIFICATION_ID_KEY, 5)
    )
    LuisMaps2Theme {
        showReminder(detailsBundle)
    }
}