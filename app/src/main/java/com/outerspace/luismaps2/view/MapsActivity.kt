package com.outerspace.luismaps2.view

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room

import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.outerspace.luismaps2.R
import com.outerspace.luismaps2.location.LOCATION_DATABASE_NAME
import com.outerspace.luismaps2.ui.theme.LuisMaps2Theme
import com.outerspace.luismaps2.location.LONDON_LOCATION
import com.outerspace.luismaps2.location.LocationDatabase
import com.outerspace.luismaps2.location.LocationViewModel
import com.outerspace.luismaps2.location.WorldLocation
import java.lang.ref.WeakReference

class MapsActivity : ComponentActivity() /* , OnMapReadyCallback*/ {
    private lateinit var locationVM: LocationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationVM = ViewModelProvider(this)[LocationViewModel::class.java]
        locationVM.weakActivity = WeakReference(this)
        locationVM.locationDb =
            Room.databaseBuilder(
                this.applicationContext, LocationDatabase::class.java, LOCATION_DATABASE_NAME
            ).build()

        locationVM.mutablePermissionGranted.observe(this) {
            if (it) locationVM.requestCurrentWorldLocation(this.baseContext)
        }

        setContent {
            LuisMaps2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    mapsScreenScaffold()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        locationVM.requestCurrentWorldLocation(this)
    }

    // NOTE: I use these MapParamsInterface and DialogParamsInterface to hoist various
    // objects and functions at once.
    interface MapParamsInterface {
        var forceRecomposeCount: MutableState<Int>
        var currentLocation: WorldLocation
        fun onMapClick(clickedLocation: WorldLocation)
        fun getValidPoiList(): List<WorldLocation>
        fun editLocation(location: WorldLocation)
    }

    interface DialogParamsInterface {
        var poi: MutableState<WorldLocation>
        fun onClickCreatePoi(poi: WorldLocation)
        fun showListener(show: Boolean)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun mapsScreenScaffold(modifier: Modifier = Modifier) {
        fun policy(): SnapshotMutationPolicy<WorldLocation> =
            object : SnapshotMutationPolicy<WorldLocation> {
                override fun equivalent(a: WorldLocation, b: WorldLocation): Boolean = a == b
            }

        var showMenu by remember { mutableStateOf(false) }
        var showFormDialog by remember { mutableStateOf(false) }

        val dialogParams = object: DialogParamsInterface {
            override var poi: MutableState<WorldLocation> = remember { mutableStateOf(LONDON_LOCATION, policy()) }
            override fun onClickCreatePoi(poi: WorldLocation) {
                locationVM.addLocation(poi)
            }
            override fun showListener(show: Boolean) {
                showFormDialog = show
            }
        }

        val mapParams = object: MapParamsInterface {
            override var forceRecomposeCount: MutableState<Int> = remember { mutableIntStateOf(0) }  // forces to recompose.
            override var currentLocation: WorldLocation = LONDON_LOCATION
            override fun onMapClick(clickedLocation: WorldLocation) {
                dialogParams.poi.value = clickedLocation
                showFormDialog = true
            }
            override fun getValidPoiList(): List<WorldLocation> {
                return locationVM.poiSet.filter {it.valid}
            }
            override fun editLocation(location: WorldLocation) {
                dialogParams.poi.value = location
                showFormDialog = true
            }
        }

        locationVM.mutableCurrentLocation.observe(this) {
            mapParams.currentLocation = it
            mapParams.forceRecomposeCount.value += 1
        }

        locationVM.mutableAddedPoi.observe(this) {
            dialogParams.poi.value = it
            mapParams.forceRecomposeCount.value += 1
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            stringResource(id = R.string.app_title),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(imageVector = Icons.Default.Menu, stringResource(id = R.string.login_menu))
                        }
                        DropdownMenu(expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.reminder_list_menu)) },
                                onClick = {
                                    showMenu = false
                                    val intent = Intent(this@MapsActivity.baseContext, LocationsActivity::class.java)
                                    startActivity(intent)
                                })
                        }
                    },
                )
            },
        ) { innerPadding ->
            innerPadding.toString()
            mapsScreen(modifier, mapParams)
            if (showFormDialog)
                inputFormDialog(modifier, dialogParams)
        }
    }

    @Composable
    private fun inputFormDialog(
        modifier: Modifier = Modifier,
        params: DialogParamsInterface
    ) {
        var reminderTitle by remember { mutableStateOf("") }
        var reminderDescription by remember { mutableStateOf("") }

        Dialog (onDismissRequest = { params.poi.value.valid = false }) {
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider // Reference: https://stackoverflow.com/questions/70390697/position-alert-dialog-in-android-compose
            dialogWindowProvider.window.setGravity(Gravity.BOTTOM)
            Card (
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentWidth()
                        .wrapContentHeight()
                        .padding(16.dp),
                ) {
                    OutlinedTextField(value = reminderTitle,
                        onValueChange = { reminderTitle = it },
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
                    Row(
                        modifier = modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Button(onClick = {
                            params.poi.value.valid = false
                            params.showListener(false)
                        }) {
                            Text(text = stringResource(R.string.cancel_button_face))
                        }
                        Button(onClick = {
                            val newPoi = params.poi.value.copy()
                            newPoi.title = reminderTitle
                            newPoi.description = reminderDescription
                            newPoi.valid = true
                            params.onClickCreatePoi(newPoi)
                            params.showListener(false)
                        }) {
                            Text(text = stringResource(R.string.save_button_face))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun mapsScreen(modifier: Modifier = Modifier, mapParams: MapParamsInterface) {
        if (mapParams.forceRecomposeCount.value > 0) {
            val cameraPositionState: CameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(mapParams.currentLocation.getLatLng(), 15f)
            }

            GoogleMap(
                modifier = modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { mapParams.onMapClick(WorldLocation(it)) }
            ) {
                for (location in mapParams.getValidPoiList()) {
                    Marker(
                        state = MarkerState(position = location.getLatLng()),
                        title = location.title,
                        snippet = location.description,
                        tag = location,
                        onClick = {
                            if ((it.tag as WorldLocation).title.isEmpty()) {
                                mapParams.editLocation(it.tag as WorldLocation)
                            } else {
                                it.showInfoWindow()
                            }
                            false
                        },
                    )
                }
            }
        }

    }

    @Preview
    @Composable
    private fun bottomInputFormPreview() {
        val dialogParams = object: DialogParamsInterface {
            override var poi: MutableState<WorldLocation> = remember { mutableStateOf(LONDON_LOCATION) }
            override fun showListener(show: Boolean) {}
            override fun onClickCreatePoi(poi: WorldLocation) {}
        }
        inputFormDialog(params = dialogParams)
    }
}
