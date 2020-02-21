package com.karensarmiento.collaborationapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import com.karensarmiento.collaborationapp.collaboration.Automerge
import com.karensarmiento.collaborationapp.collaboration.Card
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.widget.ImageButton
import com.karensarmiento.collaborationapp.grouping.GroupManager
import kotlinx.android.synthetic.main.todo_entry_box.view.*
import com.karensarmiento.collaborationapp.messaging.FirebaseMessageSendingService as FirebaseSending
import com.karensarmiento.collaborationapp.utils.JsonKeyword as Jk


class MainActivity : AppCompatActivity() {

    private var localHistory: File? = null
    private var automerge: Automerge? = null
    private lateinit var jsonUpdateListener: BroadcastReceiver

    companion object {
        private const val TAG = "MainActivity"
        internal var appContext: Context?  = null
        private const val localHistoryFileName = "automerge-state.txt"

        fun getLaunchIntent(from: Context) = Intent(from, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = applicationContext
        setContentView(R.layout.activity_main)
        setUpTitleBar()

        setUpAutomerge()
        setUpButtonListeners()
        setUpLocalFileState()
        registerJsonUpdateListener()
    }

    private fun setUpTitleBar() {
        val headerText = GroupManager.currentGroup
        actionBar?.title = headerText
        supportActionBar?.title = headerText
    }

    private fun setUpAutomerge(){
        // The callback will by default be called by a BG thread; therefore we need to dispatch it
        // to the UI thread.
        automerge = Automerge(webview) {
            runOnUiThread { updateCards(it) }
        }
    }

    private fun setUpButtonListeners() {
        todo_entry_box.button_add_todo.setOnClickListener {
            val todoText = todo_entry_box.text_entry.text.toString()
            automerge?.addCard(Card(todoText, false)) {
                appendJsonToLocalHistory(it)
                FirebaseSending.sendJsonUpdateToCurrentDeviceGroup(it)
            }
            todo_entry_box.text_entry.setText("")
        }
    }

    fun onCheckboxClicked(view: View) {
        if (view is CheckBox) {
            val index = layout_todos.indexOfChild(view.parent.parent.parent as ViewGroup)
            automerge?.setCardCompleted(index, view.isChecked) {
                appendJsonToLocalHistory(it)
                FirebaseSending.sendJsonUpdateToCurrentDeviceGroup(it)
            }
        }
    }


    private fun setUpLocalFileState() {
        localHistory = File(this.applicationContext.filesDir, localHistoryFileName)
        localHistory?.writeText("")
    }

    private fun registerJsonUpdateListener() {
        jsonUpdateListener = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val updateJson = intent.getStringExtra(Jk.VALUE.text)
                Log.i(TAG, "RECEIVED JSON: $updateJson")
                updateJson?.let {
                    automerge?.applyJsonUpdate(it)
                    appendJsonToLocalHistory(it)
                }
            }
        }
        val intentFilter = IntentFilter(Jk.GROUP_MESSAGE.text)
        registerReceiver(jsonUpdateListener, intentFilter)
    }

    // TODO: Fix me. I do not always work.
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
        layout_todos.removeAllViewsInLayout()
        cards.forEach { card ->
            val view = layoutInflater.inflate(R.layout.card, layout_todos, false)

            // Set text and checkbox.
            val item = view.findViewById<CheckBox>(R.id.checkbox)
            if (item is CheckBox) {
                item.text = card.title
                item.isChecked = card.completed
            }

            // Set delete button listener.
            val deleteButton = view.findViewById<ImageButton>(R.id.button_delete)
            deleteButton.setOnClickListener {
                val index = layout_todos.indexOfChild(view as ViewGroup)
                automerge?.removeCard(index) {
                appendJsonToLocalHistory(it)
                FirebaseSending.sendJsonUpdateToCurrentDeviceGroup(it)
                }
            }

            // Display card in UI.
            layout_todos.addView(view)
        }
    }

    private fun appendJsonToLocalHistory(json : String) {
        localHistory?.appendText("$json\n")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(jsonUpdateListener)
    }
}
