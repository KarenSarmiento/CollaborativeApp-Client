package com.karensarmiento.collaborationapp


import android.util.Log
import java.io.File
import java.util.*


object Utils {

    private const val TAG = "Utils"
    val APP_USER_ID: String = setAppUserId()

    const val FCM_SERVER = "fcm-xmpp.googleapis.com"
    const val FCM_PROD_PORT = 5235
    const val FCM_TEST_PORT = 5236
    const val FCM_SERVER_AUTH_CONNECTION = "gcm.googleapis.com"

    const val FCM_ELEMENT_NAME = "gcm"
    const val FCM_NAMESPACE = "google:mobile:data"
    const val SENDER_ID = "849641919488"
    const val SERVER_KEY = "AAAAxdKa1AA:APA91bEPghKBhTv8xaQnzP6NFaLiuUJmg4sbI92__5CkoIe8kBAFXYDGH72RX_LKcQ3TixxkHuVELDSHCQt9SWW_wyJVEVmYULaLI6b9nim7CSJkIJKSoKJos4KPmk019jP-GxKY4d_C"

    const val JSON_UPDATE = "JSON_UPDATE"

    private fun setAppUserId(): String {
        val userIdFile = File(MainActivity.appContext?.filesDir, "app-user-id")
        if(userIdFile.exists()) {
            Log.i(TAG, "File")
            return userIdFile.readText()
        }
        Log.i(TAG, "No file")
        val newId = getUniqueId()
        userIdFile.writeText(newId)

        return newId
    }

    fun getUniqueId(): String {
        return UUID.randomUUID().toString()
    }
}
