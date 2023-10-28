package com.outerspace.luismaps2.view

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.room.Room
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.outerspace.luismaps2.MapsApplication
import com.outerspace.luismaps2.R
import com.outerspace.luismaps2.model.EmailLogin
import com.outerspace.luismaps2.model.EmailLoginDatabase
import java.lang.ref.WeakReference

class MainViewModel: ViewModel() {
    private var showProgressBar: MutableLiveData<Boolean> = MutableLiveData()
    var logInFailed: MutableLiveData<Boolean> = MutableLiveData()
    var currentUser: MutableLiveData<FirebaseUser> = MutableLiveData()
    var mutableLoginButtons: MutableLiveData<Boolean> = MutableLiveData(true)

    private var signInClient: SignInClient? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var signInLauncher: ActivityResultLauncher<IntentSenderRequest>

    var weakActivity: WeakReference<ComponentActivity> = WeakReference(null)
        set(weakActivity) {
            field = weakActivity
            weakActivity.get()?.applicationContext?.let { signInClient = Identity.getSignInClient(it) }
            weakActivity.get()?.let {
                signInLauncher = it.registerForActivityResult(
                    StartIntentSenderForResult(),
                    signInLauncherListener
                )
            }
            auth = Firebase.auth
        }

    /**
     * Google Sign In
     */

    fun googleSignIn() {
        if (!(::signInLauncher.isInitialized) || signInClient == null) return

        val signInRequest = GetSignInIntentRequest.builder()
            .setServerClientId(weakActivity.get()?.getString(R.string.default_web_client_id)!!)
            .build()

        signInClient!!.getSignInIntent(signInRequest)
            .addOnSuccessListener { pendingIntent ->
                val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent)
                    .build()
                signInLauncher.launch(intentSenderRequest)
            }
            .addOnFailureListener {
                logInFailed.value = true
            }
    }

    private val signInLauncherListener = { result: ActivityResult ->
        try {
            // Authenticate
            val credential = signInClient?.getSignInCredentialFromIntent(result.data)
            val idToken = credential?.googleIdToken
            if (idToken != null) {
                firebaseAuthWithGoogle(idToken)
            } else {
                logInFailed.value = true
            }
        } catch (e: ApiException) {
            logInFailed.value = true
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showProgressBar.value = true
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val activity = weakActivity.get()
        if (activity != null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener(activity) { task ->
                    if (task.isSuccessful) {
                        currentUser.value = auth.currentUser
                    } else {
                        logInFailed.value = true
                    }
                    showProgressBar.value = false
                }
        else showProgressBar.value = false
    }

    /**
     * Email / Password sign in
     */

    fun emailSignIn(loginName: String, password: String) {
        val context = weakActivity.get()?.applicationContext  //  MapsApplication.instance.applicationContext
        val db = Room.inMemoryDatabaseBuilder(
            context!!,
            EmailLoginDatabase::class.java, /* "email-login.db" */
        ).build()

        val dao = db.emailLoginDao()
        dao.insert(EmailLogin(emailName = "Luis@luis.com", password = "LuisIsTheBest"))

    }
}