package com.udacity.project4.viewModels

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.outerspace.luismaps2.R
import com.udacity.project4.domain.LoginScreens
import com.udacity.project4.repositories.EmailLogin
import com.udacity.project4.repositories.EmailLoginLocalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject

@HiltViewModel
class LoginViewModel
    @Inject constructor(private val emailRepo: EmailLoginLocalRepository)
    : ViewModel()
{
    private var showProgressBar: MutableLiveData<Boolean> = MutableLiveData()
    var logInSuccess: MutableLiveData<Boolean> = MutableLiveData()
    var message: MutableLiveData<Int> = MutableLiveData()
    var currentUser: MutableLiveData<String> = MutableLiveData()

    var mutableLoginScreen: MutableLiveData<LoginScreens> = MutableLiveData(LoginScreens.CHOOSE_LOGIN)

    private var signInClient: SignInClient? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var signInLauncher: ActivityResultLauncher<IntentSenderRequest>

    lateinit var weakLifecycleOwner: WeakReference<LifecycleOwner>
        private set

    var weakActivity: WeakReference<ComponentActivity> = WeakReference(null)
        set(weakActivity) {
            field = weakActivity

            weakLifecycleOwner = WeakReference(weakActivity.get())

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

    fun emailSignIn(loginName: String, password: String, stub: String) {
        viewModelScope.launch {
            val d = viewModelScope.async(Dispatchers.IO) {
                emailRepo.passwordMatch(loginName, password)
            }
            val b = d.await()
            logInSuccess.value = b
            if (b) currentUser.value = "User"
        }
    }

    fun emailSignOn(loginName: String, password: String, stub: String) {
        viewModelScope.launch {
            val d = viewModelScope.async(Dispatchers.IO) {
                if (emailRepo.countEmailByName(loginName) == 0) {
                    emailRepo.insert(EmailLogin(emailName = loginName, password = password))
                    R.string.sign_on_success
                } else {
                    R.string.user_exists
                }
            }
            message.value = d.await() // logged In Message
        }
    }

    fun emailUpdatePassword(loginName: String, password: String, newPassword: String) {
        viewModelScope.launch {
            val d = viewModelScope.async(Dispatchers.IO) {
                if (emailRepo.countEmailByName(loginName) > 0 &&
                    emailRepo.passwordMatch(loginName, password)) {
                    emailRepo.updatePassword(loginName, password, newPassword)
                    R.string.password_update_success
                } else {
                    R.string.password_update_failed
                }
            }
            message.value = d.await()
        }
    }
}