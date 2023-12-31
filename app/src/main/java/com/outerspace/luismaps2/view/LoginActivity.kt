package com.outerspace.luismaps2.view

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.outerspace.luismaps2.R
import com.outerspace.luismaps2.domain.LOG_TAG
import com.outerspace.luismaps2.viewModels.LocationViewModel
import com.outerspace.luismaps2.viewModels.PermissionsViewModel
import com.outerspace.luismaps2.repositories.EmailLoginDatabase
import com.outerspace.luismaps2.ui.theme.LuisMaps2Theme
import com.outerspace.luismaps2.viewModels.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference

const val EMAIL_LOGIN_DB_NAME = "email-login.db"

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {
    private lateinit var loginVM: LoginViewModel
    private lateinit var locationVM: LocationViewModel
    private lateinit var permissionsVM: PermissionsViewModel
    private lateinit var mainView: LoginComposeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loginVM = ViewModelProvider(this)[LoginViewModel::class.java]
        loginVM.weakActivity = WeakReference(this)

        locationVM = ViewModelProvider(this)[LocationViewModel::class.java]

        permissionsVM = ViewModelProvider(this)[PermissionsViewModel::class.java]
        permissionsVM.weakActivity = WeakReference(this)

        permissionsVM.mutablePermissionGranted.observe(this) { permissionMap: Map<String, Boolean> ->
            Log.d(LOG_TAG, permissionMap.toString())
            if (permissionMap[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                permissionMap[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
                permissionsVM.requestBackgroundPermissions()
            } else if (permissionMap[Manifest.permission.ACCESS_BACKGROUND_LOCATION] == true) {
                // this action gets triggered at mainVM.logInSuccess when
                // ACCESS_FINE, ACCESS_COARSE and ACCESS_BACKGROUND permissions
                // are granted proceed to MapsActivity
                val intent = Intent(this@LoginActivity.baseContext, MapsActivity::class.java)
                startActivity(intent)
            }
        }

        loginVM.logInSuccess.observe(this) {
            if (it) {
                permissionsVM.requestLocationPermissions()
            } else {
                Toast.makeText(this, getString(R.string.login_failed_message), Toast.LENGTH_LONG).show()
            }
        }

        loginVM.currentUser.observe(this) {
            val text = getString(R.string.login_success_message, it)
            Toast.makeText(this, text, Toast.LENGTH_LONG).show()
        }

        loginVM.message.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }

        mainView = LoginComposeView(loginVM)

        setContent {
            LuisMaps2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    mainView.mainLoginScreen()
                }
            }
        }
    }

}

