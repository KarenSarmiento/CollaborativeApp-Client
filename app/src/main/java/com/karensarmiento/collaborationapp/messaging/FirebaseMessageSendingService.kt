package com.karensarmiento.collaborationapp.messaging

import android.os.AsyncTask
import android.util.Log
import com.karensarmiento.collaborationapp.Utils
import com.karensarmiento.collaborationapp.grouping.GroupManager
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

/**
 * This makes use of Smack in order to open an XMPP connection with the Firebase server and send
 * messages.
 */
object FirebaseMessageSendingService {
    private const val TAG = "FirebaseSendingService"
    private var xmppConn: XMPPTCPConnection? = null
//    private val buffer = ArrayList<String>()

    init {
        FirebaseConnectionInitialiser().execute()
    }

    fun sendMessageToTopic(topic: String, payload: String) {
        // TODO: Use messageIds to ensure messages are sent when implementing buffering.
        val messageId = Utils.getUniqueId()
        FirebaseMessageSender().execute("/topics/$topic", messageId, payload)
    }

    fun sendMessageToDeviceGroup(groupName: String, payload: String) {
        val messageId = Utils.getUniqueId()
        val groupToken = GroupManager.groupToken(groupName)
        FirebaseMessageSender().execute(groupToken, messageId, payload)
    }

    private class FirebaseConnectionInitialiser : AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void?): Void? {
            // Allow connection to be resumed if it is ever lost.
            XMPPTCPConnection.setUseStreamManagementResumptiodDefault(true)
            XMPPTCPConnection.setUseStreamManagementDefault(true)

            // SSL (TLS) is a cryptographic protocol. This object contains configurations for this.
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, null, SecureRandom())

            // Specify connection configurations.
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
            xmppConn?.let {
                it.connect()

                // Allow reconnection to be automatic.
                ReconnectionManager.getInstanceFor(it).enableAutomaticReconnection()

                // Disable Roster (contact list). This will be managed directly with Firebase.
                Roster.getInstanceFor(it).isRosterLoadedAtLogin = false

                // FCM requires a SASL PLAIN authentication mechanism.
                SASLAuthentication.unBlacklistSASLMechanism("PLAIN")
                SASLAuthentication.blacklistSASLMechanism("DIGEST-MD5")

                // Login to Firebase server.
                val username = "${Utils.SENDER_ID}@${Utils.FCM_SERVER_AUTH_CONNECTION}"
                it.login(username, Utils.SERVER_KEY)
                Log.i(TAG, "Connected to the FCM XMPP Server!")
            }

            return null
        }
    }

    private class FirebaseMessageSender : AsyncTask<String, Void, Void>() {
        override fun doInBackground(vararg params: String): Void? {
            val toToken = params[0]
            val messageId = params[1]
            val payload = params[2]

            val xmppMessage = createXMPPMessage(toToken, messageId, payload)
            xmppConn?.let {
                it.sendStanza(xmppMessage)
                Log.i(TAG, "Sent XMPP Message.")
            }
            return null
        }

        private fun createXMPPMessage(to: String, messageId: String, payload: String): Stanza {
            val payloadMap = HashMap<String, String>()
            payloadMap["message"] = payload
            payloadMap["fromUserId"] = Utils.APP_USER_ID

            val message = HashMap<Any?, Any?>()
            message["to"] = to
            message["message_id"] = messageId
            message["data"] = payloadMap

            val jsonRequest = JSONObject(message).toString()
            return FcmMessageExtension(jsonRequest).toPacket()
        }
    }
}