package com.karensarmiento.collaborationapp.messaging

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.karensarmiento.collaborationapp.security.AddressBook
import com.karensarmiento.collaborationapp.utils.Utils
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
    private const val TTL = 2000

    fun sendSymmetricKeyToPeer(peerEmail: String, groupId: String, key: String) {
        // Build message containing symmetric key.
        val peerMessage = Json.createObjectBuilder()
            .add(Jk.PEER_TYPE.text, Jk.SYMMETRIC_KEY_UPDATE.text)
            .add(Jk.GROUP_ID.text, groupId)
            .add(Jk.SYMMETRIC_KEY.text, key)
            .build().toString()

        // Encrypt peer message with peer's key.
        // TODO: Need AES? Will message always be small enough without AES?
        val peerKey = AddressBook.getContactKey(peerEmail) ?: return
        val encryptedPeerMessage = EncryptionManager.maybeEncryptRSA(peerMessage, peerKey) ?: return

        // Create request.
        val request = Json.createObjectBuilder()
            .add(Jk.UPSTREAM_TYPE.text, Jk.FORWARD_TO_PEER.text)
            .add(Jk.PEER_EMAIL.text, peerEmail)
            .add(Jk.PEER_MESSAGE.text, encryptedPeerMessage)
            .build().toString()

        // Send request to server.
        val messageId = Utils.getUniqueId()
        sendEncryptedServerRequest(request, messageId)
        Log.i(TAG, "Sent register public key request to server: $messageId")
    }

    fun sendRegisterPublicKeyRequest(publicKey: String) {
        // Create request.
        val request = Json.createObjectBuilder()
            .add(Jk.UPSTREAM_TYPE.text, Jk.REGISTER_PUBLIC_KEY.text)
            .add(Jk.EMAIL.text, Utils.getGoogleEmail())
            .add(Jk.PUBLIC_KEY.text, publicKey)
            .build().toString()

        // Send request to server.
        val messageId = Utils.getUniqueId()
        sendEncryptedServerRequest(request, messageId)
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
        // Encrypt JSON update.
        val groupKey = GroupManager.getCurrentGroupKey() ?: return
        val encryptedUpdate = EncryptionManager.encryptAESGCM(jsonUpdate, groupKey)

        // Create request.
        val groupToken = GroupManager.groupId(groupName) ?: return
        val request = Json.createObjectBuilder()
            .add(Jk.UPSTREAM_TYPE.text, Jk.FORWARD_TO_GROUP.text)
            .add(Jk.GROUP_ID.text, groupToken)
            .add(Jk.GROUP_MESSAGE.text, encryptedUpdate)
            .build().toString()

        // Send request to server.
        val messageId = Utils.getUniqueId()
        sendEncryptedServerRequest(request, messageId)
        Log.i(TAG, "Sent message to device group: $messageId")
    }

    fun sendCreateGroupRequest(groupName: String, peerEmails: Set<String>) {
        // Create request.
        val groupId = Utils.getUniqueId()
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
        val messageId = Utils.getUniqueId()
        MessageBuffer.registerWaitingRequest(messageId)

        // Send request to server.
        sendEncryptedServerRequest(request, messageId)
        Log.i(TAG, "Sent create group request to server: $messageId")
    }

    private fun sendEncryptedServerRequest(request: String, messageId: String) {
        // Encrypt request.
        val aesKey = EncryptionManager.generateKeyAESGCM()
        var encryptedRequest = EncryptionManager.encryptAESGCM(request, aesKey)
        var aesKeyString = EncryptionManager.keyAsString(aesKey)
        val encryptedKey = EncryptionManager.maybeEncryptRSA(aesKeyString, EncryptionManager.FB_SERVER_PUBLIC_KEY)
        if (encryptedKey == null) {
            Log.e(TAG, "Could not encrypt request. Will not send message.")
            return
        }

        // Send encrypted request.
        FirebaseMessaging.getInstance().send(
            RemoteMessage.Builder("${Utils.SENDER_ID}@fcm.googleapis.com")
                .setMessageId(messageId)
                .setTtl(TTL)
                .addData(Jk.ENC_MESSAGE.text, encryptedRequest)
                .addData(Jk.ENC_KEY.text, encryptedKey)
                //TODO: Change identifier to from email something non public e.g. random gen string associated with email.
                .addData(Jk.EMAIL.text, Utils.getGoogleEmail())
                .build())
    }
}