package com.karensarmiento.collaborationapp.authentication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.karensarmiento.collaborationapp.R
import com.karensarmiento.collaborationapp.grouping.DeviceGroupActivity
import com.karensarmiento.collaborationapp.messaging.FirebaseMessageSendingService as Firebase
import com.karensarmiento.collaborationapp.utils.Utils
import kotlinx.android.synthetic.main.activity_sign_in.*

class SignInActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SignInActivity"
        private const val RC_SIGN_IN: Int = 1
    }

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        firebaseAuth = FirebaseAuth.getInstance()
        configureGoogleSignIn()
        setUpButtonListeners()
    }

    private fun configureGoogleSignIn() {
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
    }

    private fun setUpButtonListeners() {
        button_sign_in_google.setOnClickListener {
            // Prompt user to select Google account and send intent
            val signInIntent: Intent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        button_next_page.setOnClickListener {
            startActivity(DeviceGroupActivity.getLaunchIntent(this))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // TODO : Check connected to internet
        when(requestCode) {
            RC_SIGN_IN -> {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                // Using authenticated Google account, authenticate with Firebase.
                if (account == null) {
                    Toast.makeText(this, "Google sign in failed.", Toast.LENGTH_LONG)
                        .show()
                }
                else {
                    Utils.setGoogleSignInAccount(account)
                    firebaseAuthWithGoogle(account)
                }
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(this, "Firebase sign in successful.", Toast.LENGTH_LONG)
                    .show()
                onUserLoggedIn()
            }
            else {
                Toast.makeText(this, "Firebase sign in failed.", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun onUserLoggedIn() {
        // TODO: Create public key on installation, and send this over instead.
        Firebase.sendRegisterPublicKeyRequest("test-public-key")
        button_next_page.isEnabled = true
    }


}
