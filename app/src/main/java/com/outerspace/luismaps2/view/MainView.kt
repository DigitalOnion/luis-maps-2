package com.outerspace.luismaps2.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.outerspace.luismaps2.R
import com.outerspace.luismaps2.viewModels.MainViewModel

// NOTE: I passed all the composable functions to this class for it to
// contain the "view" (MVVM). Before it was in the MainActivity and was
// becoming a mess of a file.

class MainView(owner: ViewModelStoreOwner) {

    private var mainVM: MainViewModel = ViewModelProvider(owner)[MainViewModel::class.java]

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun mainLoginScreen(modifier: Modifier = Modifier) {
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
        var showMenu by remember { mutableStateOf(false) }

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
                    actions = {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(imageVector = Icons.Default.Menu, stringResource(id = R.string.login_menu))
                        }
                        DropdownMenu(expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.google_sign_in_menu)) },
                                onClick = { showMenu = false
                                    mainVM.mutableLoginButtons.value = true
                                    mainVM.googleSignIn()
                                })
                            DropdownMenuItem(text = { Text(stringResource(R.string.email_sign_in_menu)) },
                                onClick = { showMenu = false
                                    mainVM.mutableLoginButtons.value = false
                                    mainVM.mutableEmailSignIn.value = true
                                })
                            DropdownMenuItem(text = { Text(stringResource(R.string.email_sign_on_menu)) },
                                onClick = { showMenu = false
                                    mainVM.mutableLoginButtons.value = false
                                    mainVM.mutableEmailSignOn.value = true
                                })
                            DropdownMenuItem(text = { Text(stringResource(R.string.email_update_menu)) },
                                onClick = { showMenu = false
                                    mainVM.mutableLoginButtons.value = false
                                    mainVM.mutableUpdatePassword.value = true
                                })
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { innerPadding ->
            loginScreen(modifier, innerPadding)
        }
    }

    @Composable
    private fun loginScreen(modifier: Modifier = Modifier,
                            innerPadding: PaddingValues = PaddingValues(0.dp)
    ) {
        var showLoginButtons by remember { mutableStateOf(true) }

        val owner = mainVM.weakActivity.get() ?: return

        mainVM.mutableLoginButtons.observe(owner) {
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
                title3D(modifier)
            }
            if (showLoginButtons) {
                mainLoginButtons(modifier)
            } else {
                mainEmailLogin(modifier)
            }
        }
    }

    @Composable
    private fun mainLoginButtons(modifier: Modifier = Modifier) {
        Column {
            Text(
                stringResource(R.string.login_header),
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
                        content = { Text(stringResource(R.string.login_google_button_face)) },
                        onClick = {
                            mainVM.googleSignIn()
                        })
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        content = { Text(stringResource(R.string.login_email_password_button_face)) },
                        onClick = {
                            mainVM.mutableLoginButtons.value = false
                        })
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun mainEmailLogin(modifier: Modifier = Modifier) {
        var loginEmail by remember { mutableStateOf("") }
        var loginPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }

        val owner = mainVM.weakActivity.get() ?: return

        var updatePasswordUI by remember { mutableStateOf(false) }  // execution order is important
        var signOnUI by remember { mutableStateOf(false) }
        var signInUI by remember { mutableStateOf(true) }

        mainVM.mutableUpdatePassword.observe(owner) { signInUI = false; signOnUI = false; updatePasswordUI = true }
        mainVM.mutableEmailSignOn.observe(owner) { signInUI = false; signOnUI = true; updatePasswordUI = false }
        mainVM.mutableEmailSignIn.observe(owner) { signInUI = true; signOnUI = false; updatePasswordUI = false }

        Column {
            OutlinedTextField(value = loginEmail,
                onValueChange = { loginEmail = it },
                label = { Text(text = stringResource(id = R.string.login_email)) },
                placeholder = { Text(text = stringResource(id = R.string.login_email_instructions)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                ),
                modifier = modifier.fillMaxWidth()
            )

            OutlinedTextField(value = loginPassword,
                onValueChange = { loginPassword = it},
                label = { Text(text = stringResource(id = R.string.password_email)) },
                placeholder = { Text(text = stringResource(id = R.string.password_email_instructions)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                ),
                modifier = modifier.fillMaxWidth()
            )

            if (updatePasswordUI) {
                OutlinedTextField(value = newPassword,
                    onValueChange = { newPassword = it},
                    label = { Text(text = stringResource(id = R.string.new_password)) },
                    placeholder = { Text(text = stringResource(id = R.string.new_password_instructions)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                    ),
                    modifier = modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                Button(
                    modifier = modifier.weight(3F),
                    content = { Text(stringResource(R.string.cancel_button_face)) },
                    onClick = { mainVM.mutableLoginButtons.value = true },
                )
                Spacer(modifier = modifier.weight(1F))
                Column(modifier = modifier.weight(6F)) {
                    if (signInUI) {
                        Button(
                            modifier = modifier.fillMaxWidth(),
                            content = { Text(stringResource(R.string.login_button_face)) },
                            onClick = {
                                mainVM.emailSignIn(loginEmail, loginPassword) }
                        )
                    }
                    if (signOnUI) {
                        Button(
                            modifier = modifier.fillMaxWidth(),
                            content = { Text(stringResource(R.string.signon_button_face)) },
                            onClick = { mainVM.emailSignOn(loginEmail, loginPassword) }
                        )
                    }
                    if (updatePasswordUI) {
                        Button(
                            modifier = modifier.fillMaxWidth(),
                            content = { Text(stringResource(R.string.update_button_face)) },
                            onClick = {
                                mainVM.emailUpdatePassword(
                                    loginEmail,
                                    loginPassword,
                                    newPassword
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun title3D(modifier: Modifier = Modifier) {
        Text(
            stringResource(R.string.app_title),
            modifier = modifier.padding(vertical = 50.dp),
            style = TextStyle(
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

//    @Preview(showBackground = true)
//    @Composable
//    fun GreetingPreview() {
//        LuisMaps2Theme {
//            MainLoginScreen()
//        }
//    }

}