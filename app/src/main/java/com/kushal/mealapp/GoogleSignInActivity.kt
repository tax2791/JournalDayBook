// GoogleSignInActivity.kt
@file:Suppress("DEPRECATION")

package com.kushal.mealapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class GoogleSignInActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GoogleSignInUI()
        }
    }

    @Composable
    fun GoogleSignInUI() {
        val context = LocalContext.current
        val firebaseAuth = remember { FirebaseAuth.getInstance() }
        var userState by remember { mutableStateOf(GoogleSignIn.getLastSignedInAccount(context)) }
        rememberCoroutineScope()

        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .build()

        val googleSignInClient = GoogleSignIn.getClient(context, googleSignInOptions)

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(Exception::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account.idToken!!, firebaseAuth) {
                        userState = GoogleSignIn.getLastSignedInAccount(context)
                        Log.d("SignIn", "Firebase sign-in completed for ${userState?.displayName}")
                    }
                }
            } catch (e: Exception) {
                Log.e("SignIn", "Google sign-in failed", e)
                Toast.makeText(context, "Sign-in failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }

        MaterialTheme {
            Surface {
                GoogleSignInScreenContent(
                    signIn = {
                        val signInIntent = googleSignInClient.signInIntent
                        launcher.launch(signInIntent)
                    },
                    signOut = {
                        firebaseAuth.signOut()
                        googleSignInClient.signOut().addOnCompleteListener {
                            userState = null
                        }
                    }
                )
            }
        }
    }

    private fun firebaseAuthWithGoogle(
        idToken: String,
        firebaseAuth: FirebaseAuth,
        onSuccess: () -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) onSuccess()
                else Log.w("Firebase", "signInWithCredential:failure", task.exception)
            }
    }
}
