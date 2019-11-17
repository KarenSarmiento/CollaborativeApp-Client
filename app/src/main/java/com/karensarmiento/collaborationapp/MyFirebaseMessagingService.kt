package com.karensarmiento.collaborationapp

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload (occurs whether app is in foreground or
        // background.
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
        }

        // Check if message contains a notification payload. Can only occur if app is in the
        // background.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }
    }

    /**
     * Called when FCM does not deliver a message. This may be because there are too many messages
     * or if the device hasn't connected to FCM for a month. When the app instance receives this
     * callback, we must perform a full sync with the app server.
     */
    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Log.d(TAG, "Message Deleted")
    }

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        // TODO: Notify fellow clients of updated registration ID.
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}