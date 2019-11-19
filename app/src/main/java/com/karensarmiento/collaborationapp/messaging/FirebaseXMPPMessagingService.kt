package com.karensarmiento.collaborationapp.messaging

import android.os.AsyncTask
import android.util.Log
import org.jivesoftware.smack.ConnectionConfiguration
import org.jivesoftware.smack.ReconnectionManager
import org.jivesoftware.smack.SASLAuthentication
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.json.JSONObject
import java.security.SecureRandom
import javax.net.ssl.SSLContext

object FirebaseXMPPMessagingService {
    private const val TAG = "FirebaseXMPPMessaging"
    private var xmppConn: XMPPTCPConnection? = null
    private val buffer = ArrayList<String>()

    init {
        InitialiseFirebaseXMPPConnection().execute()
    }

    // TODO: Send JSON instead of sample message.
    fun sendMessage() {
        SendMessageToFirebase().execute()
    }

    private class InitialiseFirebaseXMPPConnection : AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void?): Void? {
            // Allow connection to be resumed if it is ever lost.
            XMPPTCPConnection.setUseStreamManagementResumptiodDefault(true)
            XMPPTCPConnection.setUseStreamManagementDefault(true)

            // SSL (TLS) is a cryptographic protocol. This object contains configurations for this.
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, null, SecureRandom())

            // Specify connection configurations
            Log.i(TAG, "Connecting to the FCM XMPP Server...")
            val config = XMPPTCPConnectionConfiguration.builder()
                .setServiceName(Utils.FCM_SERVER)
                .setHost(Utils.FCM_SERVER)
                .setPort(Utils.FCM_TEST_PORT)
                .setSendPresence(false)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.ifpossible)
                .setCompressionEnabled(true)
                .setSocketFactory(sslContext.socketFactory)
                .setCustomSSLContext(sslContext)
                .build()

            // Connect
            xmppConn = XMPPTCPConnection(config)
            xmppConn?.connect()

            // Allow reconnection to be automatic.
            ReconnectionManager.getInstanceFor(xmppConn).enableAutomaticReconnection()

            // Disable Roster (contact list). This will be managed directly with Firebase.
            Roster.getInstanceFor(xmppConn).isRosterLoadedAtLogin = false

            // FCM requires a SASL PLAIN authentication mechanism.
            SASLAuthentication.unBlacklistSASLMechanism("PLAIN")
            SASLAuthentication.blacklistSASLMechanism("DIGEST-MD5")

            // Login to Firebase server
            // Login to Firebase server
            val username = "${Utils.SENDER_ID}@${Utils.FCM_SERVER_AUTH_CONNECTION}"
            xmppConn?.login(username, Utils.SERVER_KEY)
            Log.i(TAG, "User logged in!")

            return null
        }
    }

    private class SendMessageToFirebase : AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void?): Void? {
            sendSampleMessageToSelf()
            return null
        }

        private fun sendSampleMessageToSelf() {
            val payload = HashMap<String, String>()
            payload["message"] = "This is a sample message :)"
            val messageId = Utils.getUniqueMessageId()

            Utils.onCurrentToken { token ->
                token?.let { sendMessage(it, messageId, payload) }
            }
        }

        private fun sendMessage(to: String, messageId: String, payload: Map<String, String>) {
            val jsonRequest = createJsonMessage(to, messageId, payload)
            val request: Stanza = JsonMessageExtension(jsonRequest).toPacket()
            xmppConn?.sendStanza(request)
        }

        private fun createJsonMessage(
            to: String,
            messageId: String,
            payload: Map<String, String>
        ): String {
            // TODO: Make HashMap type stricter (key=String, value=Any)
            val message = HashMap<Any?, Any?>()
            message["to"] = to
            message["message_id"] = messageId
            message["data"] = payload

            return JSONObject(message).toString()
        }
    }

}