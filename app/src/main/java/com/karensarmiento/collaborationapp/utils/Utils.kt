package com.karensarmiento.collaborationapp.utils


import android.accounts.Account
import android.app.Activity
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import java.util.*


object Utils {

    private const val TAG = "Utils"

    const val FCM_SERVER = "fcm-xmpp.googleapis.com"
    const val FCM_PROD_PORT = 5235
    const val FCM_TEST_PORT = 5236
    const val FCM_SERVER_AUTH_CONNECTION = "fcm.googleapis.com"

    const val FCM_ELEMENT_NAME = "gcm"
    const val FCM_NAMESPACE = "google:mobile:data"
    const val SENDER_ID = "849641919488"
    const val CLIENT_ID = "849641919488-n1r2bp1114666f92lnh4jo85s68ntu3h.apps.googleusercontent.com"
    const val SERVER_KEY = "AAAAxdKa1AA:APA91bEPghKBhTv8xaQnzP6NFaLiuUJmg4sbI92__5CkoIe8kBAFXYD" +
            "GH72RX_LKcQ3TixxkHuVELDSHCQt9SWW_wyJVEVmYULaLI6b9nim7CSJkIJKSoKJos4KPmk019jP-GxKY4d_C"

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

    fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = activity.currentFocus
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}