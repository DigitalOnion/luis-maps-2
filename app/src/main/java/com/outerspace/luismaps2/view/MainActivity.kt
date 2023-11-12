package com.outerspace.luismaps2.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.outerspace.luismaps2.R
import com.outerspace.luismaps2.model.EmailLoginDatabase
import com.outerspace.luismaps2.ui.theme.LuisMaps2Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

const val EMAIL_LOGIN_DB_NAME = "email-login.db"

class MainActivity : ComponentActivity() {
    private lateinit var mainVM: MainViewModel
    private lateinit var mainView: MainView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainVM = ViewModelProvider(this)[MainViewModel::class.java]
        mainVM.weakActivity = WeakReference(this)

        mainVM.setEmailDb(
            Room.databaseBuilder(
                this.applicationContext, EmailLoginDatabase::class.java, EMAIL_LOGIN_DB_NAME
            ).build()
        )

        mainVM.logInSuccess.observe(this) {
            if (!it) {
                Toast.makeText(this, getString(R.string.login_failed_message), Toast.LENGTH_LONG).show()
            }
        }

        mainVM.currentUser.observe(this) {
            val text = getString(R.string.login_success_message, it)
            Toast.makeText(this, text, Toast.LENGTH_LONG).show()
            CoroutineScope(Dispatchers.IO).launch {
                val intent = Intent(this@MainActivity.baseContext, MapsActivity::class.java)
                startActivity(intent)
            }
        }

        mainVM.message.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }

        mainView = MainView(this)

        setContent {
            LuisMaps2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    mainView.MainLoginScreen()
                }
            }
        }
    }

}

