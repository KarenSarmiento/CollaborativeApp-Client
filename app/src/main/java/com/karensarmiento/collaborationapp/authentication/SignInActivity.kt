package com.karensarmiento.collaborationapp.authentication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.karensarmiento.collaborationapp.R
import com.karensarmiento.collaborationapp.grouping.DeviceGroupActivity
import com.karensarmiento.collaborationapp.security.EncryptionManager
import com.karensarmiento.collaborationapp.messaging.FirebaseMessageSendingService as Firebase
import com.karensarmiento.collaborationapp.utils.AccountUtils
import kotlinx.android.synthetic.main.activity_sign_in.*
import java.io.File
import java.io.IOException


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

    override fun onStart() {
        super.onStart()
//        if (Build.VERSION.RELEASE != "9") {
//            return
//        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1
            )
        } else {
            createEvalFile()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> createEvalFile()
        }
    }

    private fun createEvalFile() {
        val path = "${Environment.getExternalStorageDirectory()}/eval_measurements.txt"
        val file = File(path)
        if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (ioe: IOException) {
                ioe.printStackTrace()
            }
        }
        file.writeText("")
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
        // TODO : Check connected to internet (this throws error if not connected)
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
                    AccountUtils.setGoogleSignInAccount(account)
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
                Log.e(TAG, "Failed to sign in to firebase with exception: ${it.exception}")
            }
        }
    }

    private fun onUserLoggedIn() {
        // TODO: Check if user is registered with server or not. If not, then gen and send public
        // key.
        val publicKey = EncryptionManager.getPublicKeyAsString()
        Firebase.sendRegisterPublicKeyRequest(publicKey)

        button_next_page.isEnabled = true
    }
}
