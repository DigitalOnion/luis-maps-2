package com.udacity.project4.view

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.outerspace.luismaps2.R
import com.udacity.project4.domain.WorldLocation
import com.udacity.project4.theme.LuisMaps2Theme
import com.udacity.project4.viewModels.GeofenceViewModel
import com.udacity.project4.viewModels.LocationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface LocationParamInterface {
    fun navigateBack()
    fun deleteLocation(location: WorldLocation)
    fun editLocation(location: WorldLocation)
    fun toastMessage(message: String)
}

@AndroidEntryPoint
class ReminderListActivity : ComponentActivity() {
    private lateinit var locationVM: LocationViewModel
    private lateinit var geofenceVM: GeofenceViewModel
    private lateinit var locations: List<WorldLocation>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationVM = ViewModelProvider(this)[LocationViewModel::class.java]     // since the Activity is annotated with @AndroidEntryPoint, the view models are injected with the ViewModelProvider0
        geofenceVM = ViewModelProvider(this)[GeofenceViewModel::class.java]

        val params = object: LocationParamInterface {
            override fun deleteLocation(location: WorldLocation) {
                lifecycleScope.launch(Dispatchers.IO) {
                    locationVM.removeLocation(location)
                    geofenceVM.remove(this@ReminderListActivity, location)
                }
            }

            override fun editLocation(location: WorldLocation) {
                lifecycleScope.launch(Dispatchers.IO) {
                    locationVM.addOrUpdateLocation(location)
                }
            }
            override fun navigateBack() {
                this@ReminderListActivity.finish()
            }
            override fun toastMessage(message: String) {
                Toast.makeText(this@ReminderListActivity.applicationContext, message, Toast.LENGTH_SHORT).show()
            }
        }

        setContent {
            ReminderListComposable(params)
        }
    }

    @Composable
    fun ReminderListComposable(locationParams: LocationParamInterface) {
        val forceRecomposeCount: MutableState<Int> = remember { mutableIntStateOf(0) }
        fetchLocations(forceRecomposeCount)
        if (forceRecomposeCount.value > 0) {
            LuisMaps2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (locations.isNotEmpty()) {
                        locationList(locations, locationParams, Modifier)
                    } else {
                        emptyLocationsList(locationParams, Modifier)
                    }
                }
            }
        }
    }

    private fun fetchLocations(forceRecompose: MutableState<Int>) {
        CoroutineScope(Dispatchers.IO).launch {
            locations = locationVM.getLocations().map{ WorldLocation(it) }
            forceRecompose.value += 1
        }
    }
}

@Composable
private fun emptyLocationsList(params: LocationParamInterface, modifier:Modifier = Modifier) {
    IconButton(
        modifier = modifier.fillMaxWidth(),
        onClick = {params.navigateBack()}
    ) {
        Column(
            modifier = modifier.fillMaxSize(0.5F).aspectRatio(1F),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                modifier = modifier.fillMaxWidth(0.5F).aspectRatio(1F),
                painter = painterResource(R.drawable.baseline_playlist_add_24),
                contentDescription = stringResource(R.string.empty_location_list)
            )
            Text(stringResource(R.string.empty_location_list))
        }
    }
}

@Composable
fun locationList(locationList: List<WorldLocation>, params: LocationParamInterface, modifier: Modifier = Modifier) {
    val cdLocalList = stringResource(R.string.content_description_location_list)
    LazyColumn(
        modifier = modifier.fillMaxSize().semantics(mergeDescendants = false) { contentDescription = cdLocalList},
        contentPadding = PaddingValues(16.dp),
    ) {
        items(locationList) {
            holder(it, params, modifier)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun holder(location: WorldLocation, params: LocationParamInterface, modifier: Modifier = Modifier, initialShowIcons: Boolean = false, initialEditMode: Boolean = false) {
    val showIcons = remember { mutableStateOf(initialShowIcons) }
    val editMode = remember { mutableStateOf(initialEditMode) }

    var reminderTitle by remember { mutableStateOf(location.title) }
    var reminderDescription by remember { mutableStateOf(location.description) }
    Card (
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .padding(bottom = 8.dp)
            .fillMaxWidth(),
        onClick = { showIcons.value = !showIcons.value}
    )
    {
        Row (
            modifier = modifier
                .padding(12.dp)
        ) {
            Column(
                modifier.weight(1F)
            ) {
                if (editMode.value) {
                    OutlinedTextField(value = reminderTitle,
                        onValueChange = { reminderTitle = it },
                        enabled = !location.isMapsPoi,
                        label = { Text(text = stringResource(id = R.string.reminder_title_label)) },
                        placeholder = { Text(text = stringResource(id = R.string.reminder_title_placeholder)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(value = reminderDescription,
                        onValueChange = { reminderDescription = it },
                        label = { Text(text = stringResource(id = R.string.reminder_description_label)) },
                        placeholder = { Text(text = stringResource(id = R.string.reminder_description_placeholder)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(text = location.title,
                        style = MaterialTheme.typography.labelLarge)
                    Text(text = location.description,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(text = "${location.lat}, ${location.lon}; ${if (location.isMapsPoi) "Map's POI" else "User's POI"}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            if (showIcons.value) {
                if (!editMode.value) {
                    IconButton(
                        onClick = {
                            editMode.value = !editMode.value
                        }
                    ) {
                        Icon(painter = painterResource(R.drawable.baseline_edit_24),
                            contentDescription = stringResource(R.string.content_description_edit)
                        )
                    }
                    IconButton(
                        onClick = {
                            params.deleteLocation(location)
                        }
                    ) {
                        Icon(painter = painterResource(R.drawable.baseline_delete_24),
                            contentDescription = stringResource(R.string.content_description_delete)
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            location.title = reminderTitle
                            location.description = reminderDescription
                            params.editLocation(location)
                            editMode.value = false
                        }
                    ) {
                        Icon(painter = painterResource(R.drawable.baseline_check_24),
                            contentDescription = stringResource(R.string.content_description_check),
                            tint = Green
                        )
                    }
                    IconButton(
                        onClick = {
                            editMode.value = false
                            showIcons.value = false
                        }
                    ) {
                        Icon(painter = painterResource(R.drawable.baseline_cancel_24),
                            contentDescription = stringResource(R.string.content_description_cancel),
                            tint = Red
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun holderPreview() {
    val xalapaLocation = WorldLocation(0, 19.5438, 96.9102, "Xalapa", "Xalapa is a cultural city. It has theaters and Universities. It is the capital of Veracruz")  // Xalapa, Veracruz. Mexico
    val params = object: LocationParamInterface {
        override fun deleteLocation(location: WorldLocation) {}
        override fun editLocation(location: WorldLocation) {}
        override fun navigateBack() {}
        override fun toastMessage(message: String) {}
    }
    LuisMaps2Theme {
        holder(xalapaLocation, params, Modifier, initialShowIcons = true, initialEditMode = true)
    }
}

@Preview(showBackground = true)
@Composable
fun reminderListPreview() {
    val locations = listOf(
        WorldLocation(0, 19.4326, 99.1332, "Mexico City", "Extremely large city"), // Mexico City. Mexico
        WorldLocation(0, 20.9674, 89.5926, "Merida, Yucatán", "Extremely hot city"), // Merida, Yucatan. Mexico
        WorldLocation(0, 33.7488, 84.3877, "Atlanta, GA", "My city"), // Atlanta, Georgia, US
        WorldLocation(0, 19.5438, 96.9102, "Xalapa", "Xalapa is a cultural city. It has theaters and Universities. It is the capital of Veracruz"),  // Xalapa, Veracruz. Mexico
        WorldLocation(0, 25.7617, 80.1918, "Miami", "Miami is a coastal touristic city, famous for art, like movies and music, and night life."), // Miami, FL, US
    )

    val params = object: LocationParamInterface {
        override fun deleteLocation(location: WorldLocation) {}
        override fun editLocation(location: WorldLocation) {}
        override fun navigateBack() {}
        override fun toastMessage(message: String) {}
    }

    LuisMaps2Theme {
        locationList(locations, params)
    }
}
