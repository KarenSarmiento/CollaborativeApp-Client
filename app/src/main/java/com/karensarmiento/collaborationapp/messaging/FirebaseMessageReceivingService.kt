package com.karensarmiento.collaborationapp.messaging

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.content.Intent
import com.karensarmiento.collaborationapp.security.AddressBook
import com.karensarmiento.collaborationapp.grouping.GroupManager
import com.karensarmiento.collaborationapp.security.EncryptionManager
import com.karensarmiento.collaborationapp.utils.*
import javax.crypto.SecretKey
import javax.json.JsonArray
import com.karensarmiento.collaborationapp.utils.JsonKeyword as Jk
import javax.json.JsonObject


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
        // Handle data payload if one exists.
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Received message with data payload: " + remoteMessage.data)
            val decryptedMessage = getDecryptedMessage(remoteMessage.data)
            if (decryptedMessage == null) {
                Log.e(TAG, "Could not decrypt message with id ${remoteMessage.messageId}." +
                        " Will ignore it.")
                return
            }
            Log.i(TAG, "**Decrypted packet and got: $decryptedMessage")

            when(val downstreamType = getStringOrNull(decryptedMessage, Jk.DOWNSTREAM_TYPE.text)) {
                null -> Log.w(TAG, "No downstream type was specified in received message.")
                Jk.ADDED_TO_GROUP.text -> handleAddedToGroupMessage(decryptedMessage)
                Jk.ADDED_PEER_TO_GROUP.text -> handleAddedPeerToGroupMessage(decryptedMessage)
                Jk.REMOVED_PEER_FROM_GROUP.text -> handleRemovedPeerFromGroup(decryptedMessage)
                Jk.FORWARD_TO_PEER.text -> handleForwardToPeerMessage(decryptedMessage)
                Jk.FORWARD_TO_GROUP.text-> handleJsonUpdateMessage(decryptedMessage)
                else -> handleResponseMessage(downstreamType, decryptedMessage)
            }
        }
    }

    private fun handleRemovedPeerFromGroup(message: JsonObject) {
        Log.i(TAG, "Handling removed peer message.")
        val groupName = getStringOrNull(message, Jk.GROUP_NAME.text) ?: return
//            val groupId = getStringOrNull(response, Jk.GROUP_ID.text) ?: return
        val peerEmail = getStringOrNull(message, Jk.PEER_EMAIL.text) ?: return

        if (peerEmail == AccountUtils.getGoogleEmail()) {
            Log.i(TAG, "Leaving group $groupName.")
            GroupManager.leaveGroup(groupName)
            broadcastIntent(Jk.REMOVED_FROM_GROUP.text, groupName)
        } else {
            // Remove peer
            Log.i(TAG, "Removing peer $peerEmail from group $groupName.")
            GroupManager.removePeerFromGroup(groupName, peerEmail)
            broadcastIntent(Jk.REMOVED_PEER_FROM_GROUP.text, groupName)
        }
    }

    private fun handleAddedPeerToGroupMessage(message: JsonObject) {
        val groupName = getStringOrNull(message, Jk.GROUP_NAME.text) ?: return
//        val groupId = getStringOrNull(message, Jk.GROUP_ID.text) ?: return
        val peerEmail = getStringOrNull(message, Jk.PEER_EMAIL.text) ?: return
        val peerToken = getStringOrNull(message, Jk.PEER_TOKEN.text) ?: return
        val peerPublicKey = getStringOrNull(message, Jk.PEER_PUBLIC_KEY.text) ?: return

        AddressBook.addContact(peerEmail, peerToken, peerPublicKey)
        GroupManager.addToGroup(groupName, peerEmail)
        broadcastIntent(Jk.ADD_PEER_TO_GROUP.text, groupName)
    }

    /**
     *  Handles updates sent from peers.
     */
    private fun handleForwardToPeerMessage(message: JsonObject) {
        // Decrypt message using private key.
        val peerMessage = getStringOrNull(message, Jk.PEER_MESSAGE.text) ?: return
        val decryptedMessage = EncryptionManager.decryptWithOwnPrivateKey(peerMessage)
        if (decryptedMessage == null) {
            Log.i(TAG, "Could not decrypt message with own key. Will ignore it.")
            return
        }
        val decryptedJson = jsonStringToJsonObject(decryptedMessage)

        // Handle the message
        when(val peerType = getStringOrNull(decryptedJson, Jk.PEER_TYPE.text)) {
            null -> Log.w(TAG, "No peer type was specified in received message.")
            Jk.SYMMETRIC_KEY_UPDATE.text -> handleSymmetricKeyUpdate(decryptedJson)
            else ->  Log.w(TAG, "Peer type $peerType not yet supported.")
        }
    }

    /**
     *  Handle updates to group symmetric keys.
     */
    private fun handleSymmetricKeyUpdate(message: JsonObject) {
        val groupId = getStringOrNull(message, Jk.GROUP_ID.text) ?: return
        val key = getStringOrNull(message, Jk.SYMMETRIC_KEY.text) ?: return

        val groupName = GroupManager.groupName(groupId) ?: return
        GroupManager.setGroupKey(groupName, EncryptionManager.stringToKeyAESGCM(key))
        Log.i(TAG , "Updated group key for group $groupName.")
    }

    private fun getDecryptedMessage(data: Map<String, String>): JsonObject? {
        // Get encrypted AES key and data.
        val encryptedKey = getStringOrNullFromMap(data, Jk.ENC_KEY.text) ?: return null
        val encryptedMessage = getStringOrNullFromMap(data, Jk.ENC_MESSAGE.text) ?: return null

        // Decrypt AES key and obtain SecretKey object.
        val privateKey = EncryptionManager.getPrivateKeyAsString()
        val decryptedKey = EncryptionManager.maybeDecryptRSA(encryptedKey, privateKey) ?: return null
        val secretKey = EncryptionManager.stringToKeyAESGCM(decryptedKey)

        // Decrypt Message and return as JSON object.
        val decryptedMessage = EncryptionManager.decryptAESGCM(encryptedMessage, secretKey)
        return jsonStringToJsonObject(decryptedMessage)
    }

    /**
     *  Send intent containing Json update to trigger automerge update.
     *
     *  @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    private fun handleJsonUpdateMessage(message: JsonObject) {
        val groupMessage = getStringOrNull(message, Jk.GROUP_MESSAGE.text) ?: return
        val groupId = getStringOrNull(message, Jk.GROUP_ID.text) ?: return

        // Decrypt the message
        val groupName = GroupManager.groupName(groupId)
        if (groupName == null) {
            Log.e(TAG, "Could not decrypt message since we do not own a group under the id" +
                    groupId)
            return
        }
        val groupKey = GroupManager.getGroupKey(groupName)
        if (groupKey == null) {
            Log.e(TAG, "Could not decrypt message since we do not own a group key for" +
                    "group $groupId. Will drop packet.")
            return
        }
        val decryptedUpdate = EncryptionManager.decryptAESGCM(groupMessage, groupKey)

        // Apply the update.
        broadcastIntent(Jk.GROUP_MESSAGE.text, decryptedUpdate)
        Log.i(TAG, "Sent JSON update intent.")
    }

    /**
     *  Upon being added to a group, register the group for the user.
     *
     *  @param message JsonObject representing the message received from Firebase Cloud Messaging.
     */
    private fun handleAddedToGroupMessage(message: JsonObject) {
        val groupName = getStringOrNull(message, Jk.GROUP_NAME.text)
        val groupId = getStringOrNull(message, Jk.GROUP_ID.text)
        val members = getJsonArrayOrNull(message, Jk.MEMBERS.text)
        Log.i(TAG, "Received added_to_group message for groupName $groupName and " +
                "groupId $groupId.")
        registerValidatedGroupAndBroadcastOnSuccess(groupName, groupId, members)
    }

    /**
     *  Resolve corresponding request ID from waitingRequests maps and handle.
     *
     *  @param downstreamType The type of downstream message.
     *  @param response JsonObject representing the message received from Firebase Cloud Messaging.
     */
    private fun handleResponseMessage(downstreamType: String, response: JsonObject) {
        val requestId = getStringOrNull(response, Jk.REQUEST_ID.text) ?: return
        if (!MessageBuffer.tryResolveRequest(requestId))
            return

        when(downstreamType) {
            Jk.GET_NOTIFICATION_KEY_RESPONSE.text -> handleNotificationKeyResponse(response)
            Jk.CREATE_GROUP_RESPONSE.text -> handleCreateGroupResponse(response)
            Jk.ADD_PEER_TO_GROUP_RESPONSE.text -> handleAddPeerToGroupResponse(response)
            Jk.REMOVE_PEER_FROM_GROUP_RESPONSE.text -> handleRemovePeerFromGroupResponse(response)
            else -> Log.w(TAG, "Downstream type $downstreamType not yet supported.")
        }
    }

    private fun handleAddPeerToGroupResponse(response: JsonObject) {
        val success = getBooleanOrNull(response, Jk.SUCCESS.text) ?: return
        if (success) {
            Log.i(TAG, "Handling successful added to peer response.")
            val groupName = getStringOrNull(response, Jk.GROUP_NAME.text) ?: return
            val groupId = getStringOrNull(response, Jk.GROUP_ID.text) ?: return
            val peerEmail = getStringOrNull(response, Jk.PEER_EMAIL.text) ?: return
            val peerToken = getStringOrNull(response, Jk.PEER_TOKEN.text) ?: return
            val peerPublicKey = getStringOrNull(response, Jk.PEER_PUBLIC_KEY.text) ?: return

            AddressBook.addContact(peerEmail, peerToken, peerPublicKey)
            GroupManager.addToGroup(groupName, peerEmail)
            val groupKey = GroupManager.getGroupKey(groupName) ?: return
            FirebaseMessageSendingService.sendSymmetricKeyToPeer(
                peerEmail, groupId, EncryptionManager.keyAsString(groupKey))
            broadcastIntent(Jk.ADD_PEER_TO_GROUP.text, groupName)
        } else {
            Log.i(TAG, "Received unsuccessful add peer to group response.")
        }
    }

    private fun handleRemovePeerFromGroupResponse(response: JsonObject) {
        Log.i(TAG, "Received remove peer from group response.")
        val success = getBooleanOrNull(response, Jk.SUCCESS.text) ?: return
        if (success)
            handleRemovedPeerFromGroup(response)
        else
            Log.i(TAG, "Received unsuccessful remove peer from group response.")
    }

    /**
     *  Apply the callback to the received notification key or report an error.
     *
     *  @param response JsonObject representing the message received from Firebase Cloud Messaging.
     */
    private fun handleNotificationKeyResponse(response: JsonObject) {
        val notificationKey = getStringOrNull(response, Jk.NOTIFICATION_KEY.text) ?: return
    }

    /**
     *  Apply the callback to the received notification key or report an error.
     *
     *  @param response JsonObject representing the message received from Firebase Cloud Messaging.
     */
    private fun handleCreateGroupResponse(response: JsonObject) {
        val success = getBooleanOrNull(response, Jk.SUCCESS.text) ?: return
        if (success) {
            // Register group.
            val groupName = getStringOrNull(response, Jk.GROUP_NAME.text)
            val groupId = getStringOrNull(response, Jk.GROUP_ID.text)
            val members = getJsonArrayOrNull(response, Jk.MEMBERS.text)
            val registeredGroupName = registerValidatedGroupAndBroadcastOnSuccess(
                groupName, groupId, members) ?: return

            // Log any failures. (Notify user?)
            val failedEmails = getJsonArrayOrNull(response, Jk.FAILED_EMAILS.text) ?: return
            if (failedEmails.size == 0)
                Log.i(TAG, "Successfully created group!")
            else
                Log.i(TAG, "Successfully created group but failed to add $failedEmails.")

            // Create symmetric group key and send to peers.
            val aesKey = EncryptionManager.generateKeyAESGCM()
            GroupManager.setGroupKey(registeredGroupName, aesKey)
            sendAesKeyToPeers(registeredGroupName, aesKey)

        } else {
            Log.i(TAG, "Failed to create new group.")
        }
    }

    private fun sendAesKeyToPeers(groupName: String, key: SecretKey) {
        val groupToken = GroupManager.groupId(groupName) ?: return
        val peers = GroupManager.getMembers(groupName) ?: return
        for (peer in peers) {
            FirebaseMessageSendingService.sendSymmetricKeyToPeer(
                peer, groupToken, EncryptionManager.keyAsString(key))
        }
    }

    private fun registerValidatedGroupAndBroadcastOnSuccess(groupName: String?, groupId: String?, members: JsonArray?): String? {
        if (groupId == null) {
            Log.w(TAG, "Attempted to register group but no group id was specified. Will ignore request.")
            return null
        }

        if (members == null) {
            Log.w(TAG, "Attempted to register group but no members were specified. Will ignore request.")
            return null
        }

        var usedGroupName = groupName
        if (usedGroupName == null) {
            Log.w(TAG, "Attempted to register group but no group name was specified. Will use group id.")
            usedGroupName = groupId
        }

        val memberEmails = mutableSetOf<String>()
        for (member in members) {
            val memberObject = member as JsonObject
            val email = getStringOrNull(memberObject, Jk.EMAIL.text)
            val token = getStringOrNull(memberObject, Jk.NOTIFICATION_KEY.text)
            val publicKey = getStringOrNull(memberObject, Jk.PUBLIC_KEY.text)

            if (email == null || token == null || publicKey == null) {
                Log.e(TAG, "Cannot add user with email $email, token $token and public key " +
                        "$publicKey since one of these values are null. Will skip.")
                continue
            }
            if (email != AccountUtils.getGoogleEmail()) {
                memberEmails.add(email)
                AddressBook.addContact(email, token, publicKey)
            }
        }

        // TODO: Apply check to see if group name exists, in which case add (1) to the end.
        GroupManager.registerGroup(usedGroupName, groupId, memberEmails)
        broadcastIntent(Jk.ADDED_TO_GROUP.text, usedGroupName)

        return usedGroupName
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
        Log.i(TAG, "Calling sendBroadcast on intent: $updateIntent")
        sendBroadcast(updateIntent)
    }

    override fun onMessageSent(s: String) {
        super.onMessageSent(s)
        Log.i(TAG, "*** Officially sent upstream message: $s")
    }
}