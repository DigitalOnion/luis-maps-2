package com.outerspace.luismaps2.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.outerspace.luismaps2.R
import com.outerspace.luismaps2.ui.theme.LuisMaps2Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.JdkConstants.HorizontalAlignment
import java.io.StringReader
import java.lang.ref.WeakReference

const val WAIT_TO_OPEN_MAP_ACTIVITY = 1000L

class MainActivity : ComponentActivity() {

    private lateinit var mainVM: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainVM = ViewModelProvider(this)[MainViewModel::class.java]
        mainVM.weakActivity = WeakReference(this)

        mainVM.logInFailed.observe(this) {
            Toast.makeText(this, R.string.login_failed_message, Toast.LENGTH_LONG).show()
        }

        mainVM.currentUser.observe(this) {
            val text = getString(R.string.login_success_message, it.displayName)
            Toast.makeText(this, text, Toast.LENGTH_LONG).show()
            CoroutineScope(Dispatchers.IO).launch {
                delay(WAIT_TO_OPEN_MAP_ACTIVITY)
                val intent = Intent(this@MainActivity.baseContext, MapsActivity::class.java)
                startActivity(intent)
            }
        }

        setContent {
            LuisMaps2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainLoginScreen()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainLoginScreen(modifier: Modifier = Modifier) {
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),

            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text(
                            stringResource(id = R.string.app_title),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },

                    scrollBehavior = scrollBehavior,
                )
            },
        ) { innerPadding ->
            LoginScreen(modifier, innerPadding)
        }
    }
    
    @Composable
    private fun LoginScreen(modifier: Modifier = Modifier,
                            innerPadding: PaddingValues = PaddingValues(0.dp)
                            ) {

        var showLoginButtons by remember { mutableStateOf(true) }
        mainVM.mutableLoginButtons.observe(this) {
            showLoginButtons = it
        }

        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .paint(
                painterResource(id = R.drawable.map_picture_2),
                contentScale = ContentScale.FillWidth
            )
        )
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)) {
            Column(Modifier.weight(1F)) {
                Title3D(modifier)
            }
            if (showLoginButtons) {
                MainLoginButtons(modifier)
            } else {
                MainEmailLogin(modifier)
            }
        }
    }

    @Composable
    private fun MainLoginButtons(modifier: Modifier = Modifier) {
        Column {
            Text(stringResource(R.string.login_header),
                modifier = modifier.padding(bottom = 16.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
            Row {
                Spacer(modifier = Modifier.fillMaxWidth(0.3F))
                Column (
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        content = {Text(stringResource(R.string.login_google_button_face))},
                        onClick = {
                            mainVM.googleSignIn()
                        })
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        content = {Text(stringResource(R.string.login_email_password_button_face))},
                        onClick = {
                            mainVM.mutableLoginButtons.value = false
                        })
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainEmailLogin(modifier: Modifier = Modifier) {
        var login_email by remember { mutableStateOf("") }
        var login_password by remember { mutableStateOf("") }

        Column {
            OutlinedTextField(value = login_email,
                onValueChange = { login_email = it },
                label = { Text(text = stringResource(id = R.string.login_email)) },
                placeholder = { Text(text = stringResource(id = R.string.login_email_instructions)) },
                colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = Color.White),
                modifier = modifier.fillMaxWidth()
            )

            OutlinedTextField(value = login_password,
                onValueChange = { login_password = it},
                label = { Text(text = stringResource(id = R.string.password_email))},
                placeholder = { Text(text = stringResource(id = R.string.password_email_instructions)) },
                colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = Color.White),
                modifier = modifier.fillMaxWidth()
            )

            Row(
                modifier = modifier.fillMaxWidth().padding(top =16.dp),
            ) {
                Button(
                    modifier = modifier.weight(2F),
                    content = {Text(stringResource(R.string.cancel_button_face))},
                    onClick = { mainVM.mutableLoginButtons.value = true },
                )
                Spacer(modifier = modifier.weight(1F))
                Button(
                    modifier = modifier.weight(2F),
                    content = {Text(stringResource(R.string.login_button_face))},
                    onClick = { mainVM.emailSignIn(login_email, login_password) }
                )
            }
        }
    }

    @Composable
    private fun Title3D(modifier: Modifier = Modifier) {
        Text(stringResource(R.string.app_title),
            modifier = Modifier.padding(vertical = 50.dp),
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 50.sp,
                color = Color(0xAAB88032),
                shadow = Shadow(
                    color = Color(0xCC000000),
                    offset = Offset(5.0F, 10.0F),
                    blurRadius = 5f
                )
            )

        )
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        LuisMaps2Theme {
            MainLoginScreen()
        }
    }
}

