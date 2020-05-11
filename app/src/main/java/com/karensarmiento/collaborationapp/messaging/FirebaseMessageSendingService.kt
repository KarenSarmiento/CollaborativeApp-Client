package com.karensarmiento.collaborationapp.messaging

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.karensarmiento.collaborationapp.evaluation.Test
import com.karensarmiento.collaborationapp.security.AddressBook
import com.karensarmiento.collaborationapp.utils.AccountUtils
import com.karensarmiento.collaborationapp.grouping.GroupManager
import com.karensarmiento.collaborationapp.security.EncryptionManager
import com.karensarmiento.collaborationapp.utils.JsonKeyword as Jk
import org.json.JSONArray
import javax.json.Json

/**
 *  This handles the sending of upstream messages to the app server.
 */
object FirebaseMessageSendingService {
    private const val TAG = "FirebaseSendingService"
    private const val TTL = 200

    fun sendDocumentToPeer(peerEmail: String, groupId: String, document: String) {
        // Build message containing symmetric key.
        val peerMessage = Json.createObjectBuilder()
            .add(Jk.PEER_TYPE.text, Jk.DOCUMENT_INIT.text)
            .add(Jk.GROUP_ID.text, groupId)
            .add(Jk.DOCUMENT.text, document)
            .build().toString()

        // Send encrypted message.
        val messageId = AccountUtils.getUniqueId()
        sendEncryptedPeerMessage(peerMessage, peerEmail, messageId)

        Log.i(TAG, "Sent forward document to peer request to server: $messageId")
    }

    fun sendSymmetricKeyToPeer(peerEmail: String, groupId: String, key: String) {
        // Build message containing symmetric key.
        val peerMessage = Json.createObjectBuilder()
            .add(Jk.PEER_TYPE.text, Jk.SYMMETRIC_KEY_UPDATE.text)
            .add(Jk.GROUP_ID.text, groupId)
            .add(Jk.SYMMETRIC_KEY.text, key)
            .build().toString()

        // Create request.
        val messageId = AccountUtils.getUniqueId()
        sendEncryptedPeerMessage(peerMessage, peerEmail, messageId)
    }

    fun sendRegisterPublicKeyRequest(publicKey: String) {
        // Create request.
        val request = Json.createObjectBuilder()
            .add(Jk.UPSTREAM_TYPE.text, Jk.REGISTER_PUBLIC_KEY.text)
            .add(Jk.EMAIL.text, AccountUtils.getGoogleEmail())
            .add(Jk.PUBLIC_KEY.text, publicKey)
            .build().toString()

        // Send request to server.
        val messageId = AccountUtils.getUniqueId()
        sendEncryptedServerRequest(request, messageId, signature = false)
        Log.i(TAG, "Sent register public key request to server: $messageId")
    }

    fun sendJsonUpdateToCurrentDeviceGroup(jsonUpdate: String) {
        val currentGroup = GroupManager.currentGroup
        if (currentGroup == null) {
            Log.e(TAG, "Could not send message to current group since it is null.")
            return
        }
        sendJsonUpdateToDeviceGroup(currentGroup, jsonUpdate)
    }

    private fun sendJsonUpdateToDeviceGroup(groupName: String, jsonUpdate: String) {
        Test.currMeasurement.encryptStart = System.currentTimeMillis()
        // Encrypt json update and create digital signature.
        val groupKey = GroupManager.getCurrentGroupKey() ?: return
        val encryptedUpdate = EncryptionManager.encryptAESGCM(jsonUpdate, groupKey)
        val signature = EncryptionManager.createDigitalSignature(encryptedUpdate)

        // Create request.
        val groupToken = GroupManager.groupId(groupName) ?: return
        val request = Json.createObjectBuilder()
            .add(Jk.UPSTREAM_TYPE.text, Jk.FORWARD_TO_GROUP.text)
            .add(Jk.GROUP_ID.text, groupToken)
            .add(Jk.GROUP_MESSAGE.text, Json.createObjectBuilder()
                .add(Jk.ENC_MESSAGE.text, encryptedUpdate)
                .add(Jk.EMAIL.text, AccountUtils.getGoogleEmail())
                .add(Jk.SIGNATURE.text, signature))
            .build().toString()

        // Send request to server.
        val messageId = AccountUtils.getUniqueId()
        sendEncryptedServerRequest(request, messageId)
        Log.i(TAG, "Sent message to device group: $messageId")
    }

    fun sendCreateGroupRequest(groupName: String, peerEmails: Set<String>) {
        // Create request.
        val groupId = AccountUtils.getUniqueId()
        val memberEmails = JSONArray()
        for (peerEmail in peerEmails) {
            memberEmails.put(peerEmail)
        }
        val request = Json.createObjectBuilder()
            .add(Jk.UPSTREAM_TYPE.text, Jk.CREATE_GROUP.text)
            .add(Jk.GROUP_NAME.text, groupName)
            .add(Jk.GROUP_ID.text, groupId)
            .add(Jk.MEMBER_EMAILS.text, memberEmails.toString())
            .build().toString()

        // Register this request as one that is awaiting a response.
        val messageId = AccountUtils.getUniqueId()
        MessageBuffer.registerWaitingRequest(messageId)

        // Send request to server.
        sendEncryptedServerRequest(request, messageId)
        Log.i(TAG, "Sent create group request to server: $messageId")
    }

    fun sendAddPeerToGroupRequest(groupName: String, groupId: String, peerEmail: String) {
        // Create request.
        val request = Json.createObjectBuilder()
            .add(Jk.UPSTREAM_TYPE.text, Jk.ADD_PEER_TO_GROUP.text)
            .add(Jk.GROUP_NAME.text, groupName)
            .add(Jk.GROUP_ID.text, groupId)
            .add(Jk.PEER_EMAIL.text, peerEmail)
            .build().toString()

        // Register this request as one that is awaiting a response.
        val messageId = AccountUtils.getUniqueId()
        MessageBuffer.registerWaitingRequest(messageId)

        // Send request to server.
        sendEncryptedServerRequest(request, messageId)
        Log.i(TAG, "Sent add peer to group request to server: $messageId")
    }

    fun sendRemovePeerFromGroupRequest(groupName: String, groupId: String, peerEmail: String?) {
        val request = Json.createObjectBuilder()
            .add(Jk.UPSTREAM_TYPE.text, Jk.REMOVE_PEER_FROM_GROUP.text)
            .add(Jk.GROUP_NAME.text, groupName)
            .add(Jk.GROUP_ID.text, groupId)
            .add(Jk.PEER_EMAIL.text, peerEmail)
            .build().toString()

        // Register this request as one that is awaiting a response.
        val messageId = AccountUtils.getUniqueId()
        MessageBuffer.registerWaitingRequest(messageId)

        // Send request to server.
        sendEncryptedServerRequest(request, messageId)
        Log.i(TAG, "Sent remove peer from group request to server: $messageId")
    }

    private fun sendEncryptedPeerMessage(request: String, peerEmail: String, messageId: String) {
        // Encrypt request.
        val peerKey = AddressBook.getContactKey(peerEmail) ?: return
        val aesKey = EncryptionManager.generateKeyAESGCM()
        val encryptedRequest = EncryptionManager.encryptAESGCM(request, aesKey)
        val aesKeyString = EncryptionManager.keyAsString(aesKey)
        val encryptedKey = EncryptionManager.maybeEncryptRSA(aesKeyString, peerKey)
        if (encryptedKey == null) {
            Log.e(TAG, "Could not encrypt request. Will not send message.")
            return
        }

        // Create request.
        val signature = EncryptionManager.createDigitalSignature(encryptedRequest)
        val forwardToPeerRequest = Json.createObjectBuilder()
            .add(Jk.UPSTREAM_TYPE.text, Jk.FORWARD_TO_PEER.text)
            .add(Jk.PEER_EMAIL.text, peerEmail)
            .add(Jk.PEER_MESSAGE.text, Json.createObjectBuilder()
                .add(Jk.ENC_MESSAGE.text, encryptedRequest)
                .add(Jk.ENC_KEY.text, encryptedKey)
                .add(Jk.EMAIL.text, AccountUtils.getGoogleEmail())
                .add(Jk.SIGNATURE.text, signature))
            .build().toString()

        // Send request to server.
        sendEncryptedServerRequest(forwardToPeerRequest, messageId)
    }

    private fun sendEncryptedServerRequest(request: String, messageId: String, signature: Boolean = true) {
        // Encrypt request.
        val aesKey = EncryptionManager.generateKeyAESGCM()
        val encryptedRequest = EncryptionManager.encryptAESGCM(request, aesKey)
        val aesKeyString = EncryptionManager.keyAsString(aesKey)
        val encryptedKey = EncryptionManager.maybeEncryptRSA(aesKeyString, EncryptionManager.FB_SERVER_PUBLIC_KEY)
        if (encryptedKey == null) {
            Log.e(TAG, "Could not encrypt request. Will not send message.")
            return
        }

        // Send encrypted request with or without digital signature.
        if (signature) {
            Test.currMeasurement.encryptEnd = System.currentTimeMillis()

            val signature = EncryptionManager.createDigitalSignature(encryptedRequest)
            FirebaseMessaging.getInstance().send(
                RemoteMessage.Builder("${AccountUtils.SENDER_ID}@fcm.googleapis.com")
                    .setMessageId(messageId)
                    .setTtl(TTL)
                    .addData(Jk.ENC_MESSAGE.text, encryptedRequest)
                    .addData(Jk.ENC_KEY.text, encryptedKey)
                    .addData(Jk.EMAIL.text, AccountUtils.getGoogleEmail())
                    .addData(Jk.SIGNATURE.text, signature)
                    .build())
        } else {
            FirebaseMessaging.getInstance().send(
                RemoteMessage.Builder("${AccountUtils.SENDER_ID}@fcm.googleapis.com")
                    .setMessageId(messageId)
                    .setTtl(TTL)
                    .addData(Jk.ENC_MESSAGE.text, encryptedRequest)
                    .addData(Jk.ENC_KEY.text, encryptedKey)
                    .addData(Jk.EMAIL.text, AccountUtils.getGoogleEmail())
                    .build())
        }
        Test.currMeasurement.sendMessage = System.currentTimeMillis()
    }
}