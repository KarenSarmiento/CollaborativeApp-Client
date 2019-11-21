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
import org.json.JSONObject
import java.io.File


class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val localHistoryFileName = "automerge-state.txt"
        private var localHistory: File? = null
        private var automerge: Automerge? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setUpAutomerge()
        setUpButtonListeners()
        localHistory = File(this.applicationContext.filesDir, localHistoryFileName)
        localHistory?.writeText("")
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
            }
            text_field.setText("")
        }

        button_remove_card.setOnClickListener {
            automerge?.removeCard {
                appendJsonToLocalHistory(it)
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

        button_send_sample_message.setOnClickListener {
            FirebaseMessageSendingService.sendMessage()
        }
    }

    fun onCheckboxClicked(view: View) {
        if (view is CheckBox) {
            val index = layout_cards.indexOfChild(((view.getParent() as ViewGroup).parent as ViewGroup))
            automerge?.setCardCompleted(index, view.isChecked) {
                appendJsonToLocalHistory(it)
            }
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
}
