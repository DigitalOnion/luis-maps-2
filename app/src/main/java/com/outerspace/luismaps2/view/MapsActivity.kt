package com.outerspace.luismaps2.view

import android.content.Context
import android.content.Intent
import android.os.Bundle

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
import com.google.android.gms.maps.CameraUpdateFactory.newCameraPosition
import com.google.android.gms.maps.model.BitmapDescriptorFactory

import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.outerspace.luismaps2.R
import com.outerspace.luismaps2.viewModels.GeofenceViewModel
import com.outerspace.luismaps2.viewModels.LOCATION_DATABASE_NAME
import com.outerspace.luismaps2.viewModels.LONDON_LAT
import com.outerspace.luismaps2.ui.theme.LuisMaps2Theme
import com.outerspace.luismaps2.viewModels.LONDON_LOCATION
import com.outerspace.luismaps2.viewModels.LONDON_LON
import com.outerspace.luismaps2.repositories.LocationDatabase
import com.outerspace.luismaps2.viewModels.LocationViewModel
import com.outerspace.luismaps2.domain.WorldLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class MapsActivity : ComponentActivity() /* , OnMapReadyCallback*/ {
    private lateinit var locationVM: LocationViewModel
    private lateinit var geofenceVM: GeofenceViewModel
    private lateinit var initialLocation: WorldLocation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = this.getPreferences(Context.MODE_PRIVATE)
        val initialLatitude = sharedPreferences.getString(LONDON_LAT, LONDON_LOCATION.lat.toString())!!.toDouble()
        val initialLongitude = sharedPreferences.getString(LONDON_LON, LONDON_LOCATION.lon.toString())!!.toDouble()
        initialLocation = WorldLocation(initialLatitude, initialLongitude)

        locationVM = ViewModelProvider(this)[LocationViewModel::class.java]
        locationVM.weakActivity = WeakReference(this)
        locationVM.locationDb =
            Room.databaseBuilder(
                this.applicationContext, LocationDatabase::class.java, LOCATION_DATABASE_NAME
            ).build()

        geofenceVM = ViewModelProvider(this)[GeofenceViewModel::class.java]

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
        locationVM.refreshPoiList()
    }

    // NOTE: I use these MapParamsInterface and DialogParamsInterface to hoist various
    // objects and functions at once.
    interface MapParamsInterface {
        var currentLocation: MutableState<WorldLocation>
        var currentZoom: MutableState<Float>
        var cameraPositionState:CameraPositionState
        fun refreshPoiList()
        fun getPoiList(): MutableList<WorldLocation>
        fun onMapClick(clickedLocation: WorldLocation)
        fun editLocation(location: WorldLocation)
        fun launchSnackBar()
    }

    interface DialogParamsInterface {
        var poi: MutableState<WorldLocation>
        fun onClickCreatePoi(poi: WorldLocation)
        fun showInputPoiDialog(show: Boolean)
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
        var poiListChanged by remember { mutableIntStateOf(1) }

        val dialogParams = object: DialogParamsInterface {
            override var poi: MutableState<WorldLocation> = remember { mutableStateOf(LONDON_LOCATION, policy()) }
            override fun onClickCreatePoi(poi: WorldLocation) {
                locationVM.addOrUpdateLocation(poi)
                geofenceVM.add(this@MapsActivity, poi)
            }
            override fun showInputPoiDialog(show: Boolean) {
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
            override var currentLocation: MutableState<WorldLocation> = remember { mutableStateOf(initialLocation)}
            override var currentZoom: MutableState<Float> = remember { mutableFloatStateOf(15F) }
            override var cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(initialLocation.getLatLng(), 15F)
            }
            override fun refreshPoiList() {
                locationVM.refreshPoiList()
                poiListChanged++
            }
            override fun getPoiList(): MutableList<WorldLocation> {
                return locationVM.mutablePoiList.value ?: mutableListOf()
            }
            override fun onMapClick(clickedLocation: WorldLocation) {
                dialogParams.poi.value = clickedLocation
                showFormDialog = true
            }
            override fun editLocation(location: WorldLocation) {
                dialogParams.poi.value.apply {
                    lat = location.lat
                    lon = location.lon
                    title = location.title
                    description = location.description
                }
                showFormDialog = true
            }
            override fun launchSnackBar() {
                scope.launch {
                    when(snackBarHostState.showSnackbar(snackBarVisuals)) {
                        SnackbarResult.ActionPerformed -> {
                            locationVM.deleteAllLocations(this@MapsActivity, geofenceVM)
                            makeText(this@MapsActivity, this@MapsActivity.getText(R.string.all_locations_were_deleted), Toast.LENGTH_SHORT).show()
                        }
                        SnackbarResult.Dismissed -> {
                            makeText(this@MapsActivity, this@MapsActivity.getText(R.string.no_locations_deleted), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        locationVM.mutableDeletedPoi.observe(this) {
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
                if (locationVM.jumpToLocation) {
                    val update = newCameraPosition(CameraPosition.Builder()
                        .target(mapParams.currentLocation.value.getLatLng())
                        .zoom(mapParams.currentZoom.value)
                        .build())
                    mapParams.cameraPositionState.move(update)

                    val sharedPreferences = this.getPreferences(Context.MODE_PRIVATE)
                    sharedPreferences.edit()
                        .putString(LONDON_LAT, it.lat.toString())
                        .putString(LONDON_LON, it.lon.toString())
                        .apply()

                    locationVM.jumpToLocation = false
                }
            }

            Box(modifier = modifier.padding(innerPadding)) {
                if (poiListChanged > 0)
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
        var reminderTitle by remember { mutableStateOf(params.poi.value.title) }
        var reminderDescription by remember { mutableStateOf(params.poi.value.description) }

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
                            params.showInputPoiDialog(false)
                        }) {
                            Text(text = stringResource(R.string.cancel_button_face))
                        }
                        Button(onClick = {
                            params.poi.value.apply {
                                title = reminderTitle
                                description = reminderDescription
                                valid = true
                            }
                            params.onClickCreatePoi(params.poi.value)
                            params.showInputPoiDialog(false)
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
                        mapParams.currentZoom.value = it.zoom
                    }
                }
            }
        }

        val uiSettings = remember { MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = true) }
        val properties = remember { MapProperties(isMyLocationEnabled = true) }
        mapParams.refreshPoiList()

        GoogleMap(
            modifier = modifier.fillMaxSize(),
            uiSettings = uiSettings,
            properties = properties,
            cameraPositionState = mapParams.cameraPositionState,
            onMapClick = {
                val wl = WorldLocation(it)
                mapParams.onMapClick(wl)
            },
            onMyLocationButtonClick = {
                val update = newCameraPosition(CameraPosition.Builder()
                    .target(mapParams.currentLocation.value.getLatLng())
                    .zoom(mapParams.currentZoom.value)
                    .build())
                mapParams.cameraPositionState.move(update)

                val sharedPreferences = this.getPreferences(Context.MODE_PRIVATE)
                sharedPreferences.edit()
                    .putString(LONDON_LAT, mapParams.currentLocation.value.lat.toString())
                    .putString(LONDON_LON, mapParams.currentLocation.value.lon.toString())
                    .apply()

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
            for (location in mapParams.getPoiList()) {
                Marker(
                    state = MarkerState(position = location.getLatLng()),
                    title = location.title,
                    snippet = location.description,
                    tag = location,
                    onClick = {
                        val list = mapParams.getPoiList()
                        val clickedPoi: WorldLocation = it.tag as WorldLocation
                        val poi = list.find {poiElement -> poiElement == clickedPoi }
                        if (poi != null) {
                            it.title = poi.title
                            it.snippet = poi.description
                        }
                        it.showInfoWindow()
                        lifecycleScope.launch {
                            delay(5000)
                            it.hideInfoWindow()
                        }
                        true
                    },
                )
            }
        }
    }

    @Preview
    @Composable
    private fun bottomInputFormPreview() {
        val dialogParams = object: DialogParamsInterface {
            override var poi: MutableState<WorldLocation> = remember { mutableStateOf(LONDON_LOCATION) }
            override fun showInputPoiDialog(show: Boolean) {}
            override fun onClickCreatePoi(poi: WorldLocation) {}
        }
        inputFormDialog(params = dialogParams)
    }
}
