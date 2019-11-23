package com.karensarmiento.collaborationapp.messaging


import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import java.util.*


object Utils {

    private const val TAG = "Utils"

    const val FCM_SERVER = "fcm-xmpp.googleapis.com"
    const val FCM_PROD_PORT = 5235
    const val FCM_TEST_PORT = 5236
    const val FCM_SERVER_AUTH_CONNECTION = "gcm.googleapis.com"

    const val FCM_ELEMENT_NAME = "gcm"
    const val FCM_NAMESPACE = "google:mobile:data"
    const val SENDER_ID = "849641919488"
    const val SERVER_KEY = "AAAAxdKa1AA:APA91bEPghKBhTv8xaQnzP6NFaLiuUJmg4sbI92__5CkoIe8kBAFXYDGH72RX_LKcQ3TixxkHuVELDSHCQt9SWW_wyJVEVmYULaLI6b9nim7CSJkIJKSoKJos4KPmk019jP-GxKY4d_C"

    const val JSON_UPDATE = "JSON_UPDATE"

    fun getUniqueMessageId(): String {
        return UUID.randomUUID().toString()
    }

    fun onCurrentToken(callback : (String?) -> Unit) {
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "getInstanceId failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new Instance ID token
                val token = task.result?.token

                // Apply callback
                callback(token)
            })
    }

}
