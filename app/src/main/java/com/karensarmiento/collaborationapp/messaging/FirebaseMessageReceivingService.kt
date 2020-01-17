package com.karensarmiento.collaborationapp.messaging

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.content.Intent
import com.karensarmiento.collaborationapp.utils.JsonKeyword as Jk
import com.karensarmiento.collaborationapp.utils.Utils


/**
 * This makes use of the Google FirebaseMessagingService to:
 * 1) Handle incoming messages
 * 2) Handle client registration tokens.
 */
class FirebaseMessageReceivingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FirebaseReceiptService"
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.i(TAG, "Received message!! ${remoteMessage.data}")

        // Handle data payload if one exists.
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)

            when(val downstreamType = remoteMessage.data[Jk.DOWNSTREAM_TYPE.text]) {
                // TODO: Do not accept null here (server should add downstream type.)
                Jk.JSON_UPDATE.text, null -> handleJsonUpdateMessage(remoteMessage)
                Jk.GET_NOTIFICATION_KEY_RESPONSE.text -> handleNotificationKeyResponse(remoteMessage)
                else -> Log.i(TAG, "Downstream type $downstreamType is invalid.")
            }
        }
    }

    private fun handleJsonUpdateMessage(remoteMessage: RemoteMessage) {
        // Ignore messages sent from self.
        // TODO: Could this be a security threat? You set a message as coming from someone else so
        // that they don't see certain changes. Maybe only the server should be allowed to do this.
        // Or maybe you should do this by message id or something.
        if (remoteMessage.data[Jk.EMAIL.text] == Utils.getGoogleEmail()) {
            Log.i(TAG, "Ignoring message sent from self.")
            return
        }

        val jsonUpdate = remoteMessage.data[Jk.JSON_UPDATE.text]

        val updateIntent = Intent()
        updateIntent.action = Jk.JSON_UPDATE.text
        updateIntent.putExtra(Jk.VALUE.text, jsonUpdate)
        sendBroadcast(updateIntent)
    }

    private fun handleNotificationKeyResponse(remoteMessage: RemoteMessage) {
        val notificationKey = remoteMessage.data[Jk.NOTIFICATION_KEY.text] ?: "No notification key!"

        val updateIntent = Intent(this, FirebaseMessageSendingServiceCLASS::class.java)
        updateIntent.action = Jk.GET_NOTIFICATION_KEY_RESPONSE.text
        updateIntent.putExtra(Jk.VALUE.text, notificationKey)
        sendBroadcast(updateIntent)
        Log.i(TAG, "Sent notification key: $notificationKey")
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
}