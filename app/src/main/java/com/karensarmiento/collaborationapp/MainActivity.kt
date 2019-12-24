package com.karensarmiento.collaborationapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import com.karensarmiento.collaborationapp.collaboration.Automerge
import com.karensarmiento.collaborationapp.collaboration.Card
import com.karensarmiento.collaborationapp.messaging.FirebaseMessageSendingService
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        internal var appContext: Context?  = null
        private var automerge: Automerge? = null
        private const val localHistoryFileName = "automerge-state.txt"
        private var localHistory: File? = null
        private const val topic = "myTestTopic"
    }

    /**
     * The activityReceiver object receives messages from FirebaseMessagingReceivingService.
     *
     * Whenever a message is received, we update the local state.
     */
    private val activityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val updateJson = intent.getStringExtra(Utils.JSON_UPDATE)
            updateJson?.let {
                automerge?.applyJsonUpdate(it)
                appendJsonToLocalHistory(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = applicationContext

        setContentView(R.layout.activity_main)
        setUpAutomerge()
        setUpButtonListeners()
        setUpLocalFileState()
        registerBroadcastReceiver()
        subscribeToFcmTopic(topic)
    }

    private fun setUpAutomerge(){
        // The callback will by default be called by a BG thread; therefore we need to dispatch it
        // to the UI thread.
        automerge = Automerge(webview) {
            runOnUiThread { updateCards(it) }
        }
    }

    private fun setUpButtonListeners() {
        button_add_card.setOnClickListener {
            automerge?.addCard(Card(text_field.text.toString(), false)) {
                appendJsonToLocalHistory(it)
                sendJsonUpdateToTopic(it)
            }
            text_field.setText("")
        }

        button_remove_card.setOnClickListener {
            automerge?.removeCard {
                appendJsonToLocalHistory(it)
                sendJsonUpdateToTopic(it)
            }
        }

        button_recover_state.setOnClickListener {
            recoverLocalStateFromFile()
        }

        button_send_message_server.setOnClickListener {
            sendSampleUpstreamMessage()
        }
    }

    private fun sendSampleUpstreamMessage() {
//        val message = "THIS IS A SAMPLE MESSAGE :)"
//        FirebaseMessaging.getInstance().send(
//            RemoteMessage.Builder("${Utils.SENDER_ID}@${Utils.FCM_SERVER_AUTH_CONNECTION}")
//                .setMessageId(Utils.getUniqueId())
//                .addData("message", message)
//                .build()
//        )
        Log.d(TAG, "Sending message to server using RemoteMessage!")
        val to = "\\topics\\$topic" // the notification key
        FirebaseMessaging.getInstance().send(
            RemoteMessage.Builder(to)
                .setMessageId(Utils.getUniqueId())
                .addData("hello", "world")
                .build()
        )
        Log.d(TAG, "Done sending message to server using RemoteMessage!")
    }

    fun onCheckboxClicked(view: View) {
        if (view is CheckBox) {
            val index = layout_cards.indexOfChild(((view.getParent() as ViewGroup).parent as ViewGroup))
            automerge?.setCardCompleted(index, view.isChecked) {
                appendJsonToLocalHistory(it)
                sendJsonUpdateToTopic(it)
            }
        }
    }


    private fun setUpLocalFileState() {
        localHistory = File(this.applicationContext.filesDir, localHistoryFileName)
        localHistory?.writeText("")
    }

    private fun registerBroadcastReceiver() {
        activityReceiver.let {
            val intentFilter = IntentFilter("ACTION_ACTIVITY")
            registerReceiver(activityReceiver, intentFilter)
        }
    }

    private fun subscribeToFcmTopic(topic: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener {
                var msg = "You have subscribed to $topic!"
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            }
    }

    private fun recoverLocalStateFromFile() {
        val updates = localHistory?.readLines()
        updates?.let {
            for (jsonUpdate in it) {
                automerge?.applyJsonUpdate(jsonUpdate)
                Log.i(TAG, "Updated state with: $jsonUpdate")
            }
        }
    }

    // TODO: Recreating cards is inefficient. Use RecyclerView with a proper adapter instead.
    private fun updateCards(cards: List<Card>) {
        layout_cards.removeAllViewsInLayout()
        cards.forEach { card ->
            val view = layoutInflater.inflate(R.layout.card, layout_cards, false)
            val item = view.findViewById<CheckBox>(R.id.checkbox)
            if (item is CheckBox) {
                item.text = card.title
                item.isChecked = card.completed
            }
            layout_cards.addView(view)
        }
    }

    private fun appendJsonToLocalHistory(json : String) {
        localHistory?.appendText("$json\n")
    }

    private fun sendJsonUpdateToTopic(jsonUpdate: String) {
        FirebaseMessageSendingService.sendMessageToTopic(topic, jsonUpdate)
    }
}
