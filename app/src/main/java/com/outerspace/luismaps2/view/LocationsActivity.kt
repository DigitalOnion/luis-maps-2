package com.outerspace.luismaps2.view

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.outerspace.luismaps2.R
import com.outerspace.luismaps2.location.LOCATION_DATABASE_NAME
import com.outerspace.luismaps2.location.LocationDatabase
import com.outerspace.luismaps2.location.LocationViewModel
import com.outerspace.luismaps2.location.WorldLocation
import com.outerspace.luismaps2.view.ui.theme.LuisMaps2Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private interface LocationParamInterface {
    fun toastMessage(message: String)
}

class LocationsActivity : ComponentActivity() {
    private lateinit var locationVM: LocationViewModel
    private lateinit var db: LocationDatabase
    private lateinit var locations: List<WorldLocation>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CoroutineScope(Dispatchers.IO).launch {
            db = Room.databaseBuilder(
                this@LocationsActivity.applicationContext,
                LocationDatabase::class.java,
                LOCATION_DATABASE_NAME
            ).build()
            locationVM = ViewModelProvider(this@LocationsActivity)[LocationViewModel::class.java]
            locationVM.locationDb = db
        }

        val locationParams = object: LocationParamInterface {
            override fun toastMessage(message: String) {
                Toast.makeText(this@LocationsActivity.applicationContext, message, Toast.LENGTH_SHORT).show()
            }
        }

        setContent {
            val forceRecomposeCount: MutableState<Int> = remember { mutableIntStateOf(0) }

            fetchLocations(forceRecomposeCount)

            if (forceRecomposeCount.value > 0) {
                LuisMaps2Theme {
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        locationList(locations, locationParams, Modifier)
                    }
                }
            }
        }
    }

    private fun fetchLocations(forceRecompose: MutableState<Int>) {
        CoroutineScope(Dispatchers.IO).launch {
            locations = db.worldLocationDao().getLocations().map{ WorldLocation(it) }
            forceRecompose.value += 1
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun locationList(locationList: List<WorldLocation>, params: LocationParamInterface, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        items(locationList) {
            holder(it, params, modifier)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun holder(location: WorldLocation, params: LocationParamInterface, modifier: Modifier = Modifier) {
    val showIcons = remember { mutableStateOf(false) }
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
                Text(text = location.title,
                    style = MaterialTheme.typography.labelLarge)
                Text(text = location.description,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (showIcons.value) {
                IconButton(
                    onClick = {
                        params.toastMessage("Kill it!")
                    }
                ) {
                    Icon(painter = painterResource(R.drawable.baseline_delete_24),
                        contentDescription = stringResource(R.string.content_description_delete)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun holderPreview() {
    val xalapaLocation = WorldLocation(19.5438, 96.9102, "Xalapa", "Xalapa is a cultural city. It has theaters and Universities. It is the capital of Veracruz")  // Xalapa, Veracruz. Mexico
    val params = object: LocationParamInterface {
        override fun toastMessage(message: String) {}
    }
    LuisMaps2Theme {
        holder(xalapaLocation, params)
    }
}

@Preview(showBackground = true)
@Composable
fun greetingPreview() {
    val locations = listOf(
        WorldLocation(19.4326, 99.1332, "Mexico City", "Extremely large city"), // Mexico City. Mexico
        WorldLocation(20.9674, 89.5926, "Merida, Yucatán", "Extremely hot city"), // Merida, Yucatan. Mexico
        WorldLocation(33.7488, 84.3877, "Atlanta, GA", "My city"), // Atlanta, Georgia, US
        WorldLocation(19.5438, 96.9102, "Xalapa", "Xalapa is a cultural city. It has theaters and Universities. It is the capital of Veracruz"),  // Xalapa, Veracruz. Mexico
        WorldLocation(25.7617, 80.1918, "Miami", "Miami is a coastal touristic city, famous for art, like movies and music, and night life."), // Miami, FL, US
    )

    val params = object: LocationParamInterface {
        override fun toastMessage(message: String) {}
    }

    LuisMaps2Theme {
        locationList(locations, params)
    }
}