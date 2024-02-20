package com.udacity.project4.viewModels

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.ViewModel
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.view.MapsActivity


const val LOGGED_IN_USER_KEY = "logged-in-user"
class LoginViewModel
    : ViewModel() {

    companion object {

        val signInListener: (FirebaseAuthUIAuthenticationResult?, ComponentActivity) -> Unit = {
            result: FirebaseAuthUIAuthenticationResult?, activity: ComponentActivity ->
                if (result?.resultCode == ComponentActivity.RESULT_OK) {
                    val user = FirebaseAuth.getInstance().currentUser
                    val userBundle = Bundle()
                    userBundle.putParcelable(LOGGED_IN_USER_KEY, user)
                    val intent = Intent(activity.baseContext, MapsActivity::class.java)
                    activity.startActivity(intent, userBundle)
                } else {
                    Toast.makeText(activity.baseContext,
                        R.string.authentication_failed,
                        Toast.LENGTH_SHORT).show()
            }
        }

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder() // ... options ...
            .setAvailableProviders(
                listOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build(),
                )
            )
            .setTheme(R.style.Theme_LuisMaps2)
            .build()

        fun signOut(activity: ComponentActivity) {
            AuthUI.getInstance()
                .signOut(activity.baseContext)
                .addOnCompleteListener {
                    Toast.makeText(activity.baseContext,
                        R.string.authentication_signed_out,
                        Toast.LENGTH_SHORT).show()
                    activity.finish()
                }
        }
    }

}