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
    const val FCM_SERVER_AUTH_CONNECTION = "fcm.googleapis.com"

    const val FCM_ELEMENT_NAME = "gcm"
    const val FCM_NAMESPACE = "google:mobile:data"
    const val SENDER_ID = "849641919488"
    const val CLIENT_ID = "849641919488-n1r2bp1114666f92lnh4jo85s68ntu3h.apps.googleusercontent.com"
    const val SERVER_KEY = "AAAAxdKa1AA:APA91bEPghKBhTv8xaQnzP6NFaLiuUJmg4sbI92__5CkoIe8kBAFXYD" +
            "GH72RX_LKcQ3TixxkHuVELDSHCQt9SWW_wyJVEVmYULaLI6b9nim7CSJkIJKSoKJos4KPmk019jP-GxKY4d_C"

    const val JSON_UPDATE = "JSON_UPDATE"

    // TODO: This is currently a way for the user to remember a user id which is used
    // in order to identify where messages are sent from. We can remove this once we have
    // the server in place which ensure that you do not receive your own messages.
    // We can also make use of registration ids instead of this id?
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
