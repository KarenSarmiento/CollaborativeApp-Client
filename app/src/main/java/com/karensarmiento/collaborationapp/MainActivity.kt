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
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.widget.ImageButton
import com.karensarmiento.collaborationapp.grouping.GroupManager
import kotlinx.android.synthetic.main.todo_entry_box.view.*
import com.karensarmiento.collaborationapp.utils.JsonKeyword as Jk
import android.view.Menu
import android.view.MenuItem
import com.karensarmiento.collaborationapp.grouping.GroupSettingsActivity
import com.karensarmiento.collaborationapp.utils.AndroidUtils


class MainActivity : AppCompatActivity() {

    private lateinit var automerge: Automerge
    private lateinit var jsonUpdateListener: BroadcastReceiver
    private lateinit var currentGroup: String

    companion object {
        private const val TAG = "MainActivity"
        internal var appContext: Context?  = null

        fun getLaunchIntent(from: Context) = Intent(from, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = applicationContext
        currentGroup = GroupManager.currentGroup!!

        setContentView(R.layout.activity_main)
        setUpTitleBar()

        setUpAutomerge()
        setUpButtonListeners()

        button_create_doc.setOnClickListener {
            createDocumentIfNotYetCreated()
        }

        registerJsonUpdateListener()
        createDocumentIfNotYetCreated()
        // TODO: Get current group and restore all to-dos - obtain from getting history in automerge.
    }

    // Adds share icon.
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_items, menu)
        return true
    }

    // Set up listener to menu item click.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.share_page -> {
                startActivity(GroupSettingsActivity.getLaunchIntent(this))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setUpTitleBar() {
        val headerText = currentGroup
        actionBar?.title = headerText
        supportActionBar?.title = headerText
    }

    private fun createDocumentIfNotYetCreated() {
        val document = GroupManager.getDocument(currentGroup)
        if (document == null || document == "null") {
            automerge.createNewDocument(currentGroup) {}
        }
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
            automerge.addCard(currentGroup, Card(todoText, false)) {
//                FirebaseSending.sendJsonUpdateToCurrentDeviceGroup(it)
            }
            todo_entry_box.text_entry.setText("")
        }
    }

    fun onCheckboxClicked(view: View) {
        if (view is CheckBox) {
            val index = layout_todos.indexOfChild(view.parent.parent.parent as ViewGroup)
            automerge.setCardCompleted(currentGroup, index, view.isChecked) {
//                FirebaseSending.sendJsonUpdateToCurrentDeviceGroup(it)
            }
        }
    }

    private fun registerJsonUpdateListener() {
        jsonUpdateListener = AndroidUtils.createSimpleBroadcastReceiver(
            this, Jk.GROUP_MESSAGE.text) {
            Log.i(TAG, "RECEIVED JSON: $it")
            automerge.applyJsonUpdate(currentGroup, it)
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
                automerge.removeCard(currentGroup, index) {
//                FirebaseSending.sendJsonUpdateToCurrentDeviceGroup(it)
                }
            }

            // Display cards in UI.
            layout_todos.addView(view)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(jsonUpdateListener)
    }
}
