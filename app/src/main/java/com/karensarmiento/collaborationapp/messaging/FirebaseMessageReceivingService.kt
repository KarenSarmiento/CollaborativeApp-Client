package com.karensarmiento.collaborationapp.messaging

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.content.Intent
import com.karensarmiento.collaborationapp.grouping.GroupManager
import com.karensarmiento.collaborationapp.utils.JsonKeyword as Jk
import com.karensarmiento.collaborationapp.utils.Utils
import org.json.JSONArray


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
        Log.i(TAG, "Received message!! ${remoteMessage.messageId}")

        // Handle data payload if one exists.
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)

            when(val downstreamType = remoteMessage.data[Jk.DOWNSTREAM_TYPE.text]) {
                null -> Log.w(TAG, "No downstream type was specified in received message.")
                Jk.JSON_UPDATE.text-> handleJsonUpdateMessage(remoteMessage)
                Jk.ADDED_TO_GROUP.text -> handleAddedToGroupMessage(remoteMessage)
                else -> handleResponseMessage(downstreamType, remoteMessage)
            }
        }
    }

    /**
     *  Send intent containing Json update to trigger automerge update.
     *
     *  @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    private fun handleJsonUpdateMessage(remoteMessage: RemoteMessage) {
        Utils.onCurrentFirebaseToken { currToken ->
            var processUpdate = true
            if (remoteMessage.data[Jk.ORIGINATOR.text] == currToken) {
                Log.i(TAG, "Ignoring message sent from self.")
                processUpdate = false
            }
            val jsonUpdate = remoteMessage.data[Jk.JSON_UPDATE.text]
            if (jsonUpdate == null) {
                Log.w(TAG, "Received json_update message with no jsonUpdate - will ignore it.")
                processUpdate = false
            }
            if (processUpdate) {
                // TODO: Store these changes in a file in case the user is offline.
                broadcastIntent(Jk.JSON_UPDATE.text, jsonUpdate!!)
            }
        }
    }

    /**
     *  Upon being added to a group, register the group for the user.
     *
     *  @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    private fun handleAddedToGroupMessage(remoteMessage: RemoteMessage) {
        val groupName = remoteMessage.data[Jk.GROUP_NAME.text]
        val groupId = remoteMessage.data[Jk.GROUP_ID.text]
        Log.i(TAG, "Received added_to_group message for groupName $groupName and " +
                "groupId $groupId.")
        registerValidatedGroupAndBroadcastOnSuccess(groupName, groupId)
    }

    /**
     *  Resolve corresponding request ID from waitingRequests maps and handle.
     *
     *  @param downstreamType The type of downstream message.
     *  @param responseMessage Object representing the message received from Firebase Cloud Messaging.
     */
    private fun handleResponseMessage(downstreamType: String, responseMessage: RemoteMessage) {
        val requestId = responseMessage.data[Jk.REQUEST_ID.text]
        if (requestId == null) {
            Log.w(TAG, "No request id was found in the packet. It will be ignored.")
            return
        }
        if (!MessageBuffer.tryResolveRequest(requestId))
            return

        when(downstreamType) {
            Jk.GET_NOTIFICATION_KEY_RESPONSE.text -> handleNotificationKeyResponse(responseMessage)
            Jk.CREATE_GROUP_RESPONSE.text -> handleCreateGroupResponse(responseMessage)
            else -> Log.w(TAG, "Downstream type $downstreamType not yet supported.")
        }
    }

    /**
     *  Apply the callback to the received notification key or report an error.
     *
     *  @param responseMessage Object representing the message received from Firebase Cloud Messaging.
     *  @param callback The callback to apply to the received notification key.
     */
    private fun handleNotificationKeyResponse(responseMessage: RemoteMessage) {
        val notificationKey = responseMessage.data[Jk.NOTIFICATION_KEY.text]
        if (notificationKey == null) {
            Log.w(TAG, "No notification key was found in a notification key response message.")
        }
    }

    /**
     *  Apply the callback to the received notification key or report an error.
     *
     *  @param responseMessage Object representing the message received from Firebase Cloud Messaging.
     */
    private fun handleCreateGroupResponse(responseMessage: RemoteMessage) {
        val success = responseMessage.data[Jk.SUCCESS.text]
        if (success != null && success.toBoolean()) {
            // Register group.
            registerValidatedGroupAndBroadcastOnSuccess(
                responseMessage.data[Jk.GROUP_NAME.text], responseMessage.data[Jk.GROUP_ID.text])

            // Notify of any failures
            val failedEmails = JSONArray(responseMessage.data[Jk.FAILED_EMAILS.text])
            if (failedEmails.length() == 0)
                Log.i(TAG, "Successfully created group!")
            else
                Log.i(TAG, "Successfully created group but failed to add $failedEmails.")
        } else {
            Log.i(TAG, "Failed to create new group.")
        }
    }

    private fun registerValidatedGroupAndBroadcastOnSuccess(groupName: String?, groupId: String?) {
        if (groupId == null) {
            Log.w(TAG, "Attempted to register group but no group id was specified. Will ignore request.")
            return
        }
        var usedGroupName = groupName
        if (usedGroupName == null) {
            Log.w(TAG, "Attempted to register group but no group name was specified. Will use group id.")
            usedGroupName = groupId
        }
        // TODO: Apply check to see if group name exists, in which case add (1) to the end.
        GroupManager.registerGroup(usedGroupName, groupId)
        broadcastIntent(Jk.ADDED_TO_GROUP.text, usedGroupName)
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
        // TODO: Notify server of updated registration ID.
        Log.d(TAG, "Refreshed token: $token")
    }


    private fun broadcastIntent(action: String, value: String) {
        val updateIntent = Intent()
        updateIntent.action = action
        updateIntent.putExtra(Jk.VALUE.text, value)
        sendBroadcast(updateIntent)
    }

    override fun onMessageSent(s: String) {
        super.onMessageSent(s)
        Log.i(TAG, "*** Officially sent upstream message: $s")
    }
}