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
import com.karensarmiento.collaborationapp.messaging.Utils
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter


class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private var automerge: Automerge? = null
        private const val localHistoryFileName = "automerge-state.txt"
        private var localHistory: File? = null

    }

    /**
     * The activityReceiver object receives messages from FirebaseMessagingReceivingService.
     *
     * Whenever a message is received, we update the local state.
     */
    private val activityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val updateJson = intent.getStringExtra(Utils.JSON_UPDATE)
            Log.i(TAG, "Bundle: $updateJson")
            automerge?.applyJsonUpdate(updateJson)
            appendJsonToLocalHistory(updateJson)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setUpAutomerge()
        setUpButtonListeners()
        setUpLocalFileState()
        registerBroadcastReceiver()
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
                sendJsonUpdateToSelf(it)
            }
            text_field.setText("")
        }

        button_remove_card.setOnClickListener {
            automerge?.removeCard {
                appendJsonToLocalHistory(it)
                sendJsonUpdateToSelf(it)
            }
        }

        button_recover_state.setOnClickListener {
            val updates = localHistory?.readLines()
            updates?.let {
                for (jsonUpdate in it) {
                    automerge?.applyJsonUpdate(jsonUpdate)
                    Log.i(TAG, "Updated state with: $jsonUpdate")
                }
            }
        }
    }

    fun onCheckboxClicked(view: View) {
        if (view is CheckBox) {
            val index = layout_cards.indexOfChild(((view.getParent() as ViewGroup).parent as ViewGroup))
            automerge?.setCardCompleted(index, view.isChecked) {
                appendJsonToLocalHistory(it)
                sendJsonUpdateToSelf(it)
            }
        }
    }

    private fun setUpLocalFileState() {
        localHistory = File(this.applicationContext.filesDir, localHistoryFileName)
        localHistory?.writeText("")
    }

    private fun registerBroadcastReceiver() {
        activityReceiver?.let {
            val intentFilter = IntentFilter("ACTION_ACTIVITY")
            registerReceiver(activityReceiver, intentFilter)
        }
    }

    /**
     * Will be called whenever the Automerge JS integration deems that the number of cards changed.
     *
     * TODO: This is currently quite inefficient as the entire layout gets re-created.
     * Better would be e.g. using a recyclerview with a proper adapter
     */
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

    // TODO: Send to peers in collaboration group, not self.
    private fun sendJsonUpdateToSelf(jsonUpdate: String) {
        Utils.onCurrentToken {
            it?.let {
                FirebaseMessageSendingService.sendMessage(it, jsonUpdate)
            }
        }
    }
}
