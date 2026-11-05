package com.example.data

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.model.User
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {

    private val auth: FirebaseAuth = Firebase.auth
    private val credentialManager: CredentialManager = CredentialManager.create(context)
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _currentUser = MutableStateFlow<User?>(
        auth.currentUser?.let { User.fromFirebaseUser(it) }
    )
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { fbAuth ->
        val fbUser = fbAuth.currentUser
        if (fbUser != null) {
            _currentUser.value = User.fromFirebaseUser(fbUser)
        } else if (_currentUser.value?.isGuest != true) {
            _currentUser.value = null
        }
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    fun getWebClientId(): String? {
        return try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.e("Auth", "Default web client id not found in resources", e)
            null
        }
    }

    /**
     * Interactive Google Sign-In using Credential Manager and Firebase Auth.
     * Supplies exactly one credential option per request (GetSignInWithGoogleOption)
     * for responsiveness on API 36.
     */
    fun signInWithGoogle(
        activity: Activity,
        onSuccess: (User) -> Unit,
        onError: (String) -> Unit,
        onCancelled: () -> Unit
    ) {
        val clientId = getWebClientId()
        if (clientId.isNullOrBlank()) {
            onError("Google Sign-In configuration missing (default_web_client_id)")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential

                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val fbUser = authResult.user

                    if (fbUser != null) {
                        val user = User.fromFirebaseUser(fbUser)
                        _currentUser.value = user
                        onSuccess(user)
                    } else {
                        onError("Firebase user is null after sign in")
                    }
                } else {
                    onError("Unexpected credential returned by Google")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                onCancelled()
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign-In failed", e)
                onError(e.localizedMessage ?: "Sign in failed")
            }
        }
    }

    /**
     * Silent Auto-Sign-In for existing authorized Google accounts on app launch.
     */
    fun attemptSilentSignIn(
        onSuccess: (User) -> Unit,
        onUnauthenticated: () -> Unit
    ) {
        val fbUser = auth.currentUser
        if (fbUser != null) {
            val user = User.fromFirebaseUser(fbUser)
            _currentUser.value = user
            onSuccess(user)
            return
        }

        val clientId = getWebClientId()
        if (clientId.isNullOrBlank()) {
            onUnauthenticated()
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val user = authResult.user?.let { User.fromFirebaseUser(it) }
                    if (user != null) {
                        _currentUser.value = user
                        onSuccess(user)
                    } else {
                        onUnauthenticated()
                    }
                } else {
                    onUnauthenticated()
                }
            } catch (e: Exception) {
                onUnauthenticated()
            }
        }
    }

    fun quickGuestLogin(name: String = "Guest Listener"): User {
        val guest = User.createGuest(name)
        _currentUser.value = guest
        return guest
    }

    fun signOut(onComplete: () -> Unit) {
        auth.signOut()
        _currentUser.value = null
        scope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("Auth", "Failed to clear credential state", e)
            } finally {
                onComplete()
            }
        }
    }
}
