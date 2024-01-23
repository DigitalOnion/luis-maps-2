package com.udacity.project4.view

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
import androidx.lifecycle.LifecycleOwner
import com.outerspace.luismaps2.R
import com.udacity.project4.domain.LoginScreens
import com.udacity.project4.viewModels.LoginViewModel

// NOTE: I passed all the composable functions to this class for it to
// contain the "view" (MVVM). Before it was in the MainActivity and was
// becoming a mess of a file.

class LoginComposeView(private val loginVM: LoginViewModel) {
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
                                    loginVM.mutableLoginScreen.value = LoginScreens.CHOOSE_LOGIN
                                    loginVM.googleSignIn()
                                })
                            DropdownMenuItem(text = { Text(stringResource(R.string.email_sign_in_menu)) },
                                onClick = { showMenu = false
                                    loginVM.mutableLoginScreen.value = LoginScreens.EMAIL_LOGIN
                                })
                            DropdownMenuItem(text = { Text(stringResource(R.string.email_sign_on_menu)) },
                                onClick = { showMenu = false
                                    loginVM.mutableLoginScreen.value = LoginScreens.EMAIL_SIGN_ON
                                })
                            DropdownMenuItem(text = { Text(stringResource(R.string.email_update_menu)) },
                                onClick = { showMenu = false
                                    loginVM.mutableLoginScreen.value = LoginScreens.PASSWORD_UPDATE
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
        var showLoginScreen by remember { mutableStateOf(LoginScreens.CHOOSE_LOGIN) }

        val owner: LifecycleOwner = loginVM.weakLifecycleOwner.get() ?: return

        loginVM.mutableLoginScreen.observe(owner) {
            showLoginScreen = it
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
            when (showLoginScreen) {
                LoginScreens.CHOOSE_LOGIN -> mainLoginButtons(modifier)
                LoginScreens.EMAIL_LOGIN -> mainEmailLogin(LoginScreens.EMAIL_LOGIN, loginVM::emailSignIn,  modifier)
                LoginScreens.EMAIL_SIGN_ON -> mainEmailLogin(LoginScreens.EMAIL_SIGN_ON, loginVM::emailSignOn, modifier)
                LoginScreens.PASSWORD_UPDATE -> mainEmailLogin(LoginScreens.PASSWORD_UPDATE, loginVM::emailUpdatePassword, modifier)
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
                            loginVM.googleSignIn()
                        })
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        content = { Text(stringResource(R.string.login_email_password_button_face)) },
                        onClick = {
                            loginVM.mutableLoginScreen.value = LoginScreens.EMAIL_LOGIN
                        })
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun mainEmailLogin(
        screen: LoginScreens,
        onClick: (loginEmail: String, loginPassword: String, newPassword: String) -> Unit,
        modifier: Modifier = Modifier) {

        val whiteColors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
        )

        var loginEmail by remember { mutableStateOf("") }
        var loginPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }

        Column {
            OutlinedTextField(value = loginEmail,
                onValueChange = { loginEmail = it },
                label = { Text(text = stringResource(id = screen.loginLabel)) },
                placeholder = { Text(text = stringResource(id = R.string.login_email_instructions)) },
                colors = whiteColors,
                modifier = modifier.fillMaxWidth()
            )

            OutlinedTextField(value = loginPassword,
                onValueChange = { loginPassword = it},
                label = { Text(text = stringResource(screen.passwordLabel)) },
                placeholder = { Text(text = stringResource(id = R.string.password_email_instructions)) },
                colors = whiteColors,
                modifier = modifier.fillMaxWidth()
            )

            if (screen == LoginScreens.PASSWORD_UPDATE) {
                OutlinedTextField(value = newPassword,
                    onValueChange = { newPassword = it},
                    label = { Text(text = stringResource(id = R.string.new_password)) },
                    placeholder = { Text(text = stringResource(id = R.string.new_password_instructions)) },
                    colors = whiteColors,
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
                    onClick = { loginVM.mutableLoginScreen.value = LoginScreens.CHOOSE_LOGIN },
                )
                Spacer(modifier = modifier.weight(1F))
                Button(
                    modifier = modifier.fillMaxWidth(),
                    content = { Text(stringResource(screen.buttonLabel)) },
                    onClick = { onClick(loginEmail, loginPassword, newPassword) }
                )
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