package com.karensarmiento.collaborationapp.collaboration

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.util.Log
import android.widget.ImageButton
import com.karensarmiento.collaborationapp.grouping.GroupManager
import kotlinx.android.synthetic.main.todo_entry_box.view.*
import com.karensarmiento.collaborationapp.utils.JsonKeyword as Jk
import android.view.Menu
import android.view.MenuItem
import com.karensarmiento.collaborationapp.grouping.GroupSettingsActivity
import com.karensarmiento.collaborationapp.utils.AndroidUtils
import android.webkit.WebView
import android.webkit.WebViewClient
import com.karensarmiento.collaborationapp.R


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
        // Apply buffered updates once webview has successfully loaded.
        webview.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                automerge.updateUI(GroupManager.currentGroup!!)
                applyBufferedUpdates()
                setUpButtonListeners()
            }
        }
        registerJsonUpdateListener()
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


    @Synchronized private fun applyBufferedUpdates() {
        handleBufferedDocInits()
        handleBufferedPeerMerges()
        handleBufferedPeerUpdates()
    }

    private fun handleBufferedDocInits() {
        while (docInitBuffer.hasPendingUpdates()) {
            for (docInit in docInitBuffer.popPendingUpdates()) {
                Log.i(TAG, "Applying Doc init: $docInit")
                DocInitHandler(automerge, docInit).execute()
            }
        }
        Log.i(TAG, "Doc init: ${docInitBuffer.getPendingUpdates()}")
    }

    private fun handleBufferedPeerMerges() {
        while (peerMergeBuffer.hasPendingUpdates()) {
            for (peerMerge in peerMergeBuffer.popPendingUpdates()) {
                Log.i(TAG, "Applying peer merge: $peerMerge")
                PeerMergeHandler(automerge, peerMerge).execute()
            }
        }
        Log.i(TAG, "Peer merges: ${peerMergeBuffer.getPendingUpdates()}")
    }

    private fun handleBufferedPeerUpdates() {
        if (peerUpdateBuffer.hasPendingUpdates()) {
            for (peerUpdate in peerUpdateBuffer.popPendingUpdates()) {
                Log.i(TAG, "Applying peer update: $peerUpdate")
                PeerUpdateHandler(automerge, peerUpdate).execute()
            }
        }
        Log.i(TAG, "Peer update: ${peerUpdateBuffer.getPendingUpdates()}")
    }

    private fun setUpAutomerge(callback: ((Unit) -> Unit)? = null) {
        // The callback will by default be called by a BG thread; therefore we need to dispatch it
        // to the UI thread.
        automerge = Automerge(webview) {
            runOnUiThread { updateCards(it) }
        }
        callback?.invoke(Unit)
    }

    private fun setUpButtonListeners() {
        todo_entry_box.button_add_todo.setOnClickListener {
            val todoText = todo_entry_box.text_entry.text.toString()
            TodoAdder(automerge, currentGroup, todoText).execute()
            todo_entry_box.text_entry.setText("")
        }
    }

    fun onCheckboxClicked(view: View) {
        if (view is CheckBox) {
            val index = layout_todos.indexOfChild(view.parent.parent.parent as ViewGroup)
            TodoChecker(automerge, currentGroup, index, view.isChecked).execute()
        }
    }

    private fun registerJsonUpdateListener() {
        jsonUpdateListener = AndroidUtils.createSimpleBroadcastReceiver(
            this, Jk.GROUP_MESSAGE.text) {
            applyBufferedUpdates()
        }
    }


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
                TodoDeleter(automerge, currentGroup, index).execute()
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
