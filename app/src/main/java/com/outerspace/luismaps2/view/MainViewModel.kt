package com.outerspace.luismaps2.view

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.outerspace.luismaps2.R
import com.outerspace.luismaps2.model.EmailLogin
import com.outerspace.luismaps2.model.EmailLoginDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class MainViewModel: ViewModel() {
    private var showProgressBar: MutableLiveData<Boolean> = MutableLiveData()
    var logInSuccess: MutableLiveData<Boolean> = MutableLiveData()
    var message: MutableLiveData<Int> = MutableLiveData()
    var currentUser: MutableLiveData<String> = MutableLiveData()

    var mutableLoginButtons: MutableLiveData<Boolean> = MutableLiveData(true)
    var mutableEmailSignIn: MutableLiveData<Boolean> = MutableLiveData(true)
    var mutableEmailSignOn: MutableLiveData<Boolean> = MutableLiveData(false)
    var mutableUpdatePassword: MutableLiveData<Boolean> = MutableLiveData(false)

    private var signInClient: SignInClient? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var signInLauncher: ActivityResultLauncher<IntentSenderRequest>

    var weakActivity: WeakReference<ComponentActivity> = WeakReference(null)
        set(weakActivity) {
            field = weakActivity
            weakActivity.get()?.applicationContext?.let {
                signInClient = Identity.getSignInClient(it)
            }
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
                logInSuccess.value = false
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
                logInSuccess.value = false
            }
        } catch (e: ApiException) {
            logInSuccess.value = false
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
                        currentUser.value = auth.currentUser?.displayName
                        logInSuccess.value = true
                    } else {
                        logInSuccess.value = false
                    }
                    showProgressBar.value = false
                }
        else showProgressBar.value = false
    }

    /**
     * Email / Password sign in
     */

    private fun getEmailDb(): EmailLoginDatabase {
        val context =
            weakActivity.get()?.applicationContext
//      return Room.inMemoryDatabaseBuilder(context!!, EmailLoginDatabase::class.java).build()
        return Room.databaseBuilder(
            context!!, EmailLoginDatabase::class.java, "email-login.db"
        ).build()
    }

    fun emailSignIn(loginName: String, password: String) {
        val d = viewModelScope.async(Dispatchers.IO) {          // this runs in Dispatchers IO
            val dao = getEmailDb().emailLoginDao()
            dao.passwordMatch(loginName, password)
        }
        viewModelScope.launch {
            val b = d.await()                                   // to set value of the mutableLifeData, this runs in the main thread
            logInSuccess.value = b
            if (b) currentUser.value = "User"
        }
    }

    fun emailSignOn(loginName: String, password: String) {
        val d = viewModelScope.async(Dispatchers.IO) {
            val dao = getEmailDb().emailLoginDao()
            if (dao.countEmailByName(loginName) == 0) {
                dao.insert(EmailLogin(emailName = loginName, password = password))
                R.string.sign_on_success
            } else {
                R.string.user_exists
            }
        }
        viewModelScope.launch {
            val n = d.await()
            message.value = n
        }
    }

    fun emailUpdatePassword(loginName: String, password: String, newPassword: String) {
        val d = viewModelScope.async(Dispatchers.IO) {
            val dao = getEmailDb().emailLoginDao()
            if (dao.countEmailByName(loginName) > 0 && dao.passwordMatch(loginName, password)) {
                dao.updatePassword(loginName, password, newPassword)
                R.string.password_update_success
            } else {
                R.string.password_update_failed
            }
        }
        viewModelScope.launch {
            val n = d.await()
            message.value = n
        }
    }
}