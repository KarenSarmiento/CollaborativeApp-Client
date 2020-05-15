package com.karensarmiento.collaborationapp

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Environment
import android.util.Log
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
import android.webkit.WebView
import android.webkit.WebViewClient
import com.karensarmiento.collaborationapp.evaluation.Test
import java.io.File


class MainActivity : AppCompatActivity() {

    private lateinit var automerge: Automerge
    private lateinit var jsonUpdateListener: BroadcastReceiver
    private lateinit var currentGroup: String

    private lateinit var samsungListener: BroadcastReceiver
    private lateinit var huaweiListener: BroadcastReceiver

    private val evalFile = File("${Environment.getExternalStorageDirectory()}/eval_measurements.txt")
    private val MAX_RUNS = 50

    private val longText = "Two exquisite objection delighted deficient yet its contained. Cordial because are account evident its subject but eat. Can properly followed learning prepared you doubtful yet him. Over many our good lady feet ask that. Expenses own moderate day fat trifling stronger sir domestic feelings. Itself at be answer always exeter up do. Though or my plenty uneasy do. Friendship so considered remarkably be to sentiments. Offered mention greater fifteen one promise because nor. Why denoting speaking fat"
    private val cardText = longText

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
                DocInitHandler(automerge, docInit).execute()
            }
        }
    }

    private fun handleBufferedPeerMerges() {
        while (peerMergeBuffer.hasPendingUpdates()) {
            for (peerMerge in peerMergeBuffer.popPendingUpdates()) {
                PeerMergeHandler(automerge, peerMerge).execute()
            }
        }
    }

    private fun handleBufferedPeerUpdates() {
        if (peerUpdateBuffer.hasPendingUpdates()) {
            for (peerUpdate in peerUpdateBuffer.popPendingUpdates()) {
                PeerUpdateHandler(automerge, peerUpdate).execute()
            }
        }
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

        store_init_doc.setOnClickListener {
            Test.initDoc = GroupManager.getDocument(currentGroup)!!.removeSurrounding("\"")
            Log.i(TAG, "Set init doc to: ${Test.initDoc}")
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

        samsungListener = AndroidUtils.createSimpleBroadcastReceiver(this, "SAMSUNG") {
            storeMeasurements()
            if (Test.count > MAX_RUNS) {
                Log.i(TAG, "Test over!")
            } else {
                // Reset document
                automerge.resetDoc(Test.initDoc!!) {
                    GroupManager.setDocument(GroupManager.currentGroup!!, Test.initDoc!!.removeSurrounding("\""))
                    runOnUiThread { updateCards(emptyList()) }
                    Thread.sleep(500)
                    // Start new test.
                    addCardTest(cardText)
                }
                Test.count++
            }
        }

        huaweiListener = AndroidUtils.createSimpleBroadcastReceiver(this, "HUAWEI") {
            Test.initDoc?.let {initDoc ->
                addCardTest(cardText)
                automerge.resetDoc(initDoc) {
                    GroupManager.setDocument(GroupManager.currentGroup!!, initDoc.removeSurrounding("\""))
                    runOnUiThread { updateCards(emptyList()) }
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


    private fun addCardTest(todoText: String) {
        Test.currMeasurement.start = System.currentTimeMillis()
        TodoAdder(automerge, currentGroup, todoText).execute()
        todo_entry_box.text_entry.setText("")
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
        unregisterReceiver(samsungListener)
        unregisterReceiver(huaweiListener)
    }
}
