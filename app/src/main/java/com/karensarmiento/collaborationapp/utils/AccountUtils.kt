package com.karensarmiento.collaborationapp.utils


import android.accounts.Account
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import java.util.*


object AccountUtils {

    private const val TAG = "AccountUtils"

    const val FCM_ELEMENT_NAME = "gcm"
    const val FCM_NAMESPACE = "google:mobile:data"
    const val SENDER_ID = "849641919488"

    private var googleSignInAccount: GoogleSignInAccount? = null

    fun getUniqueId(): String {
        return UUID.randomUUID().toString()
    }

    fun setGoogleSignInAccount(account: GoogleSignInAccount) {
        googleSignInAccount = account
    }

    fun getGoogleEmail(): String? {
        return googleSignInAccount?.email
    }

    fun getGoogleAccount(): Account? {
        return googleSignInAccount?.account
    }

    fun onCurrentFirebaseToken(callback: ((String) -> Unit)) {
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "getting Instance ID token failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new Instance ID token and apply callback
                val token = task.result?.token
                token?.let {
                    callback(it)
                }
            })
    }
}
