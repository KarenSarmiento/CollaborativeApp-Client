package com.karensarmiento.collaborationapp.messaging

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
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

    // TODO: Add comments containing JSON example for each of these methods.
    fun sendRegisterPublicKeyRequest(publicKey: String) {
        // Crate request.
        val request = Json.createObjectBuilder()
            .add(Jk.UPSTREAM_TYPE.text, Jk.REGISTER_PUBLIC_KEY.text)
            .add(Jk.EMAIL.text, Utils.getGoogleEmail())
            .add(Jk.PUBLIC_KEY.text, publicKey).build().toString()

        // Send request to server.
        val messageId = Utils.getUniqueId()
        sendEncryptedServerRequest(request, messageId)
        Log.i(TAG, "Sent register public key request to server: $messageId")
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
                .build())
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
            val messageId = Utils.getUniqueId()
            val groupToken = GroupManager.groupId(groupName)

            FirebaseMessaging.getInstance().send(
                RemoteMessage.Builder("${Utils.SENDER_ID}@fcm.googleapis.com")
                    .setMessageId(messageId)
                    .setTtl(TTL)
                    .addData(Jk.UPSTREAM_TYPE.text, Jk.FORWARD_MESSAGE.text)
                    .addData(Jk.FORWARD_TOKEN_ID.text, groupToken)
                    .addData(Jk.JSON_UPDATE.text, jsonUpdate)
                    .build())
            Log.i(TAG, "Sent message to device group: $messageId")
        }

        fun sendCreateGroupRequest(groupName: String, peerEmail: String) {
            val messageId = Utils.getUniqueId()
            val groupId = Utils.getUniqueId()

            // Register this request as one that is awaiting a response.
            MessageBuffer.registerWaitingRequest(messageId)

            // Send request
            val memberEmails = JSONArray()
            memberEmails.put(peerEmail)
            FirebaseMessaging.getInstance().send(
                RemoteMessage.Builder("${Utils.SENDER_ID}@fcm.googleapis.com")
                    .setMessageId(messageId)
                    .setTtl(TTL)
                    .addData(Jk.UPSTREAM_TYPE.text, Jk.CREATE_GROUP.text)
                    .addData(Jk.GROUP_NAME.text, groupName)
                    .addData(Jk.GROUP_ID.text, groupId)
                    .addData(Jk.MEMBER_EMAILS.text, memberEmails.toString())
                    .build()
            )
            Log.i(TAG, "Sent create group request to server: $messageId")
        }
    }