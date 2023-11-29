package com.outerspace.luismaps2.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.LogPrinter
import android.view.Gravity
import android.widget.Toast
import android.widget.Toast.makeText
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.CameraUpdateFactory.newCameraPosition
import com.google.android.gms.maps.UiSettings
import com.google.android.gms.maps.model.BitmapDescriptorFactory

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.outerspace.luismaps2.R
import com.outerspace.luismaps2.location.GeofenceViewModel
import com.outerspace.luismaps2.location.LOCATION_DATABASE_NAME
import com.outerspace.luismaps2.location.LOG_TAG
import com.outerspace.luismaps2.ui.theme.LuisMaps2Theme
import com.outerspace.luismaps2.location.LONDON_LOCATION
import com.outerspace.luismaps2.location.LocationDatabase
import com.outerspace.luismaps2.location.LocationViewModel
import com.outerspace.luismaps2.location.WorldLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference

class MapsActivity : ComponentActivity() /* , OnMapReadyCallback*/ {
    private lateinit var locationVM: LocationViewModel
    private lateinit var geofenceVM: GeofenceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationVM = ViewModelProvider(this)[LocationViewModel::class.java]
        locationVM.weakActivity = WeakReference(this)
        locationVM.locationDb =
            Room.databaseBuilder(
                this.applicationContext, LocationDatabase::class.java, LOCATION_DATABASE_NAME
            ).build()

        geofenceVM = ViewModelProvider(this)[GeofenceViewModel::class.java]
        geofenceVM.weakActivity = WeakReference(this)

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
        locationVM.startCurrentLocationFlow(this)
    }

    override fun onResume() {
        super.onResume()
        locationVM.mutableDeletedPoi.value = LONDON_LOCATION // value is irrelevant, it triggers recomposition
    }

    // NOTE: I use these MapParamsInterface and DialogParamsInterface to hoist various
    // objects and functions at once.
    interface MapParamsInterface {
        var forceRecomposeCount: MutableState<Int>
        var currentLocation: MutableState<WorldLocation>
        var currentZoom: MutableState<Float>
        var cameraPositionState:CameraPositionState
        fun onMapClick(clickedLocation: WorldLocation)
        fun getValidPoiList(): List<WorldLocation>
        fun editLocation(location: WorldLocation)
        fun launchSnackBar()
        fun toast(msg: String)
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
                geofenceVM.add(poi)
            }
            override fun showListener(show: Boolean) {
                showFormDialog = show
            }
        }

        val scope = rememberCoroutineScope()
        val snackBarHostState = remember { SnackbarHostState() }

        val snackBarVisuals = object: SnackbarVisuals {
            override val message = stringResource(R.string.test_delete_all_poi)
            override val actionLabel = stringResource(R.string.yes_button_face)
            override val withDismissAction = true
            override val duration = SnackbarDuration.Short
        }

        val mapParams = object: MapParamsInterface {
            override var forceRecomposeCount: MutableState<Int> = remember { mutableIntStateOf(0) }  // forces to recompose.
            override var currentLocation: MutableState<WorldLocation> = remember { mutableStateOf(LONDON_LOCATION)}
            override var currentZoom: MutableState<Float> = remember { mutableFloatStateOf(15F) }
            override var cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(LONDON_LOCATION.getLatLng(), 15F)
            }
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
            override fun launchSnackBar() {
                scope.launch {
                    when(snackBarHostState.showSnackbar(snackBarVisuals)) {
                        SnackbarResult.ActionPerformed -> {
                            locationVM.deleteAllLocations()
                            forceRecomposeCount.value += 1
                            makeText(this@MapsActivity, this@MapsActivity.getText(R.string.all_locations_were_deleted), Toast.LENGTH_SHORT).show()
                        }
                        SnackbarResult.Dismissed -> {
                            makeText(this@MapsActivity, this@MapsActivity.getText(R.string.no_locations_deleted), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            override fun toast(msg: String) {
                runBlocking(Dispatchers.Main) {
                    makeText(this@MapsActivity.applicationContext, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }

        locationVM.mutableAddedPoi.observe(this) {
            dialogParams.poi.value = it
            mapParams.forceRecomposeCount.value += 1
        }

        locationVM.mutableDeletedPoi.observe(this) {
            mapParams.forceRecomposeCount.value += 1
        }

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
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
                            DropdownMenuItem(text = { Text(stringResource(R.string.delete_all_poi))},
                                onClick = {
                                    showMenu = false
                                    mapParams.launchSnackBar()
                                }
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->

            locationVM.mutableCurrentLocation.observe(this) {
                mapParams.currentLocation.value = it
                mapParams.forceRecomposeCount.value += 1
            }

            Box(modifier = modifier.padding(innerPadding)) {
                mapsScreen(modifier, mapParams)
                if (showFormDialog)
                    inputFormDialog(modifier, dialogParams)
            }
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
        snapshotFlow { mapParams.cameraPositionState.position }.let {
            lifecycleScope.launch(Dispatchers.IO) {
                it.collect {
                    val zoomValue: Float = mapParams.currentZoom.value
                    if (zoomValue != it.zoom) {
                        mapParams.toast(msg = "zoom: ${it.zoom}")
                        mapParams.currentZoom.value = it.zoom
                    }
                }
            }
        }

        if (mapParams.forceRecomposeCount.value > 0) {
            val uiSettings = remember { MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = true) }
            val properties = remember { MapProperties(isMyLocationEnabled = true) }

            GoogleMap(
                modifier = modifier.fillMaxSize(),
                uiSettings = uiSettings,
                properties = properties,
                cameraPositionState = mapParams.cameraPositionState,
                onMapClick = { mapParams.onMapClick(WorldLocation(it)) },
                onMyLocationButtonClick = {
                    val update = newCameraPosition(CameraPosition.Builder()
                        .target(mapParams.currentLocation.value.getLatLng())
                        .zoom(mapParams.currentZoom.value)
                        .build())
                    mapParams.cameraPositionState.move(update)
                    true
                }
            ) {
                Marker(
                    state = MarkerState(position = mapParams.currentLocation.value.getLatLng()),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                    onClick = {
                        mapParams.editLocation(mapParams.currentLocation.value)
                        false
                    },
                )
                for (location in mapParams.getValidPoiList()) {
                    Marker(
                        state = MarkerState(position = location.getLatLng()),
                        title = location.title,
                        snippet = location.description,
                        tag = location,
                        onClick = {
                            mapParams.editLocation(it.tag as WorldLocation)
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
