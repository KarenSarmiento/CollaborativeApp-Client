package com.karensarmiento.collaborationapp.messaging

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.content.Intent
import com.karensarmiento.collaborationapp.collaboration.docInits
import com.karensarmiento.collaborationapp.collaboration.peerMerges
import com.karensarmiento.collaborationapp.collaboration.peerUpdates
import com.karensarmiento.collaborationapp.evaluation.Test
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
        Test.currMeasurement.receiveMessage = System.currentTimeMillis()
        // Handle data payload if one exists.
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Received message!")
            Test.currMeasurement.decryptStart = System.currentTimeMillis()
            // Authenticate Message as having come from the genuine server.
            val encryptedMessage = getStringOrNullFromMap(remoteMessage.data, Jk.ENC_MESSAGE.text) ?: return
            val signature = getStringOrNullFromMap(remoteMessage.data, Jk.SIGNATURE.text) ?: return
            if (!EncryptionManager.authenticateSignature(signature, encryptedMessage, EncryptionManager.FB_SERVER_PUBLIC_KEY)) return

            // Decrypt Message
            val encryptedKey = getStringOrNullFromMap(remoteMessage.data, Jk.ENC_KEY.text) ?: return
            val decryptedMessage = getDecryptedMessage(encryptedKey, encryptedMessage)
            if (decryptedMessage == null) {
                Log.e(TAG, "Could not decrypt message with id ${remoteMessage.messageId}." +
                        " Will ignore it.")
                return
            }

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
        // Authenticate the message
        Log.i(TAG, "handling message: $message")
        val peerMessage = getJsonObjectOrNull(message, Jk.PEER_MESSAGE.text) ?: return
        val encryptedMessage = getStringOrNull(peerMessage, Jk.ENC_MESSAGE.text) ?: return
        val senderEmail = getStringOrNull(peerMessage, Jk.EMAIL.text) ?: return
        val signature = getStringOrNull(peerMessage, Jk.SIGNATURE.text) ?: return
        val senderPublicKey = AddressBook.getContactKey(senderEmail) ?: return
        if (!EncryptionManager.authenticateSignature(signature, encryptedMessage, senderPublicKey)) return

        // Decrypt message using private key.
        val encryptedKey = getStringOrNull(peerMessage, Jk.ENC_KEY.text) ?: return
        val decryptedPeerMessage = getDecryptedMessage(encryptedKey, encryptedMessage)
        if (decryptedPeerMessage == null) {
            Log.e(TAG, "Could not decrypt peer message. Will ignore it.")
            return
        }

        // Handle the message
        val peerType = getStringOrNull(decryptedPeerMessage, Jk.PEER_TYPE.text) ?: return
        Log.i(TAG, "Got peer message of type $peerType: $decryptedPeerMessage")
        when(peerType) {
            Jk.SYMMETRIC_KEY_UPDATE.text -> handleSymmetricKeyUpdate(decryptedPeerMessage)
            Jk.DOCUMENT_INIT.text -> handleDocumentInitMessage(decryptedPeerMessage)
            else ->  Log.w(TAG, "Peer type $peerType not yet supported.")
        }
    }

    /**
     *  Handle updates to group symmetric keys.
     */
    private fun handleDocumentInitMessage(message: JsonObject) {
        val groupId = getStringOrNull(message, Jk.GROUP_ID.text) ?: return
        val document = getStringOrNull(message, Jk.DOCUMENT.text) ?: return

        val groupName = GroupManager.groupName(groupId) ?: return
        peerMerges.pushUpdate(groupName, document)
        broadcastIntent(Jk.GROUP_MESSAGE.text, groupName)
        Log.i(TAG , "Handling document init message for group $groupName to buffer.")
    }

    /**
     *  Handle updates to group symmetric keys.
     */
    private fun handleSymmetricKeyUpdate(message: JsonObject) {
        val groupId = getStringOrNull(message, Jk.GROUP_ID.text) ?: return
        val key = getStringOrNull(message, Jk.SYMMETRIC_KEY.text) ?: return

        val groupName = GroupManager.groupName(groupId) ?: return
        GroupManager.setGroupKey(groupName, EncryptionManager.stringToKeyAESGCM(key))
        Log.i(TAG , "Handling update group key message for group $groupName.")
    }

    private fun getDecryptedMessage(encryptedKey: String, encryptedMessage: String): JsonObject? {
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
        Log.i(TAG, "Handling Json Update Message.")

        // Authenticate message.
        val groupId = getStringOrNull(message, Jk.GROUP_ID.text) ?: return
        val groupName = GroupManager.groupName(groupId)
        if (groupName == null) {
            Log.e(TAG, "Could not decrypt message since we do not own a group under the id" +
                    groupId)
            return
        }
        val groupMessage = getJsonObjectOrNull(message, Jk.GROUP_MESSAGE.text) ?: return
        val encryptedMessage = getStringOrNull(groupMessage, Jk.ENC_MESSAGE.text) ?: return
        val email = getStringOrNull(groupMessage, Jk.EMAIL.text) ?: return
        val signature = getStringOrNull(groupMessage, Jk.SIGNATURE.text) ?: return
        if (!GroupManager.isMember(groupName, email)) return
        val senderPublicKey = AddressBook.getContactKey(email) ?: return
        if (!EncryptionManager.authenticateSignature(signature, encryptedMessage, senderPublicKey)) return

        // Decrypt the message
        val groupKey = GroupManager.getGroupKey(groupName)
        if (groupKey == null) {
            Log.e(TAG, "Could not decrypt message since we do not own a group key for" +
                    "group $groupId. Will drop packet.")
            return
        }
        val decryptedUpdate = EncryptionManager.decryptAESGCM(encryptedMessage, groupKey)
        Test.currMeasurement.decryptEnd = System.currentTimeMillis()
        // Apply the update.
        peerUpdates.pushUpdate(groupName, decryptedUpdate)
        broadcastIntent(Jk.GROUP_MESSAGE.text, groupName)
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
        registerValidatedGroupAndBroadcastOnSuccess(groupName, groupId, members, Jk.WAITING_FOR_PEER.text)
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

            // Add peer to contacts and group.
            AddressBook.addContact(peerEmail, peerToken, peerPublicKey)
            GroupManager.addToGroup(groupName, peerEmail)

            // Send peer group key.
            val groupKey = GroupManager.getGroupKey(groupName) ?: return
            FirebaseMessageSendingService.sendSymmetricKeyToPeer(
                peerEmail, groupId, EncryptionManager.keyAsString(groupKey))

            // Send peer the up to date doc.
            val document = GroupManager.getDocument(groupName) ?: return
            FirebaseMessageSendingService.sendDocumentToPeer(peerEmail, groupId, document)
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
        Log.i(TAG, "Handling create group response.")
        val success = getBooleanOrNull(response, Jk.SUCCESS.text) ?: return
        if (success) {
            // Register group.
            val groupName = getStringOrNull(response, Jk.GROUP_NAME.text)
            val groupId = getStringOrNull(response, Jk.GROUP_ID.text)
            val members = getJsonArrayOrNull(response, Jk.MEMBERS.text)
            val registeredGroupName = registerValidatedGroupAndBroadcastOnSuccess(
                groupName, groupId, members, Jk.WAITING_FOR_SELF.text) ?: return

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

            docInits.pushUpdate(groupName!!, groupName)
            // ASSERT no peers area added in create group, so we do not need to send them the
            // new document state here, only when we add them.
            // TODO: Remove members fields??
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

    private fun registerValidatedGroupAndBroadcastOnSuccess(groupName: String?, groupId: String?, members: JsonArray?, document: String): String? {
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

        GroupManager.registerGroup(usedGroupName, groupId, memberEmails, document)
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
        sendBroadcast(updateIntent)
    }

//    override fun onMessageSent(s: String) {
//        super.onMessageSent(s)
//        Log.i(TAG, "*** Officially sent upstream message: $s")
//    }
}