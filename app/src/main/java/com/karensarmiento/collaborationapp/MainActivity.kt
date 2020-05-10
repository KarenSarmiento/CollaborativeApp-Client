package com.karensarmiento.collaborationapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Build
import android.os.Environment
import android.widget.ImageButton
import com.karensarmiento.collaborationapp.grouping.GroupManager
import kotlinx.android.synthetic.main.todo_entry_box.view.*
import com.karensarmiento.collaborationapp.utils.JsonKeyword as Jk
import android.view.Menu
import android.view.MenuItem
import com.karensarmiento.collaborationapp.collaboration.*
import com.karensarmiento.collaborationapp.collaboration.Automerge
import com.karensarmiento.collaborationapp.collaboration.Card
import com.karensarmiento.collaborationapp.grouping.GroupSettingsActivity
import com.karensarmiento.collaborationapp.utils.AndroidUtils
import com.karensarmiento.collaborationapp.messaging.FirebaseMessageSendingService as FirebaseSending
import android.webkit.WebView
import android.webkit.WebViewClient
import com.karensarmiento.collaborationapp.evaluation.Test
import com.karensarmiento.collaborationapp.evaluation.TimingMeasurement
import java.io.File


class MainActivity : AppCompatActivity() {

    private lateinit var automerge: Automerge
    private lateinit var jsonUpdateListener: BroadcastReceiver
    private lateinit var currentGroup: String
    private val evalFile = File("${Environment.getExternalStorageDirectory()}/eval_measurements.txt")
    private val NUM_RUNS = 10

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
                applyBufferedUpdates()
                setUpButtonListeners()
            }
        }
        registerJsonUpdateListener()
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


    private fun applyBufferedUpdates() {
        // TODO: We now remove the pending update from buffer once we are sure that the update has occurred.
        // We need to makes sure therefore that we do not attempt to apply the update more than once by
        // associating each update with a mutex
        // What if we remove it straight away. Then add it back on if failed.
        while (docInits.hasPendingUpdates()) {
            for (docInit in docInits.popPendingUpdates()) {
                val document = GroupManager.getDocument(docInit.groupName)
                if (document == Jk.WAITING_FOR_SELF.text) {
                    automerge.createNewDocument(docInit, docInit.groupName) {
                        Test.initDoc = it.removeSurrounding("\"")
                    }
                } else {
                    Log.w(TAG, "Got a pending doc init for group ${docInit.groupName} but " +
                            "this group's document was in state: $document. Will drop.")
                }
            }
        }
        while (peerMerges.hasPendingUpdates()) {
            for (peerMerge in peerMerges.popPendingUpdates()) {
                val document = GroupManager.getDocument(peerMerge.groupName)
                if (document == Jk.WAITING_FOR_PEER.text) {
                    automerge.mergeNewDocument(peerMerge, peerMerge.groupName, peerMerge.update) {
                        Test.initDoc = it.removeSurrounding("\"")
                    }
                } else {
                    Log.w(TAG, "Got a pending peer merge for group ${peerMerge.groupName} but " +
                            "this group's document was in state: $document. Will drop.")
                }
            }
        }
        if (peerUpdates.hasPendingUpdates()) {
            for (pendingUpdate in peerUpdates.popPendingUpdates()) {
                val document = GroupManager.getDocument(pendingUpdate.groupName)
                if (document != Jk.WAITING_FOR_PEER.text && document != Jk.WAITING_FOR_SELF.text) {
                    automerge.applyJsonUpdate(pendingUpdate, pendingUpdate.groupName, pendingUpdate.update) {

                        if (Build.VERSION.RELEASE == "9") { // SAMSUNG
                            // Store measurements
                            storeMeasurements()
                            if (Test.count < NUM_RUNS) {
                                resetDocAndUI()
                                // Reset test result holder
                                Test.currMeasurement = TimingMeasurement()
                                // Add new card to initiate next test.
                                testingAddCard()
                            }
                            Test.count++
                        } else { // HUAWEI
                            testingAddCard {
                                resetDocAndUI()
                            }
                        }
                    }
                } else {
                    Log.i(TAG, "Could not apply peer update for group " +
                            "${pendingUpdate.groupName} since their document is in state $document")
                    peerUpdates.pushUpdate(pendingUpdate)
                }
            }
        }
    }

    private fun storeMeasurements() {
        val data = "${Test.currMeasurement.start}," +
                "${Test.currMeasurement.localMergeFromKotlinStart}," +
                "${Test.currMeasurement.localMergeFromKotlinEnd}," +
                "${Test.currMeasurement.encryptStart}," +
                "${Test.currMeasurement.encryptEnd}," +
                "${Test.currMeasurement.sendMessage}," +
                "${Test.currMeasurement.receiveMessage}," +
                "${Test.currMeasurement.decryptStart}," +
                "${Test.currMeasurement.decryptEnd}," +
                "${Test.currMeasurement.peerMergeFromKotlinStart}," +
                "${Test.currMeasurement.peerMergeFromKotlinEnd}\n"
        Log.i(TAG, "*TEST ${Test.count}: $data")
        evalFile.appendText(data)
    }


    private fun testingAddCard(todoText: String = "", callback: ((Unit) -> Unit)? = null) {
        Test.currMeasurement.start = System.currentTimeMillis()
        automerge.addCard(currentGroup, Card(todoText, false)) {
            FirebaseSending.sendJsonUpdateToCurrentDeviceGroup(it)
            callback?.invoke(Unit)
        }
        todo_entry_box.text_entry.setText("")
    }

    private fun resetDocAndUI() {
        // Set document as empty.
        GroupManager.setDocument(GroupManager.currentGroup!!, Test.initDoc!!)

        // Update UI
        runOnUiThread { updateCards(emptyList()) }
    }

    //==========================================================================================

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
            automerge.addCard(currentGroup, Card(todoText, false)) {
                FirebaseSending.sendJsonUpdateToCurrentDeviceGroup(it)
            }
            todo_entry_box.text_entry.setText("")
        }
    }

    fun onCheckboxClicked(view: View) {
        Test.currMeasurement.start = System.currentTimeMillis()
        if (view is CheckBox) {
            val index = layout_todos.indexOfChild(view.parent.parent.parent as ViewGroup)
            automerge.setCardCompleted(currentGroup, index, view.isChecked) {
                FirebaseSending.sendJsonUpdateToCurrentDeviceGroup(it)
            }
        }
    }

    private fun registerJsonUpdateListener() {
        jsonUpdateListener = AndroidUtils.createSimpleBroadcastReceiver(
            this, Jk.GROUP_MESSAGE.text) {
            applyBufferedUpdates()
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
                    FirebaseSending.sendJsonUpdateToCurrentDeviceGroup(it)
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
