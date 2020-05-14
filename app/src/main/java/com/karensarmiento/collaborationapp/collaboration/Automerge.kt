package com.karensarmiento.collaborationapp.collaboration

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import com.karensarmiento.collaborationapp.MainActivity
import com.karensarmiento.collaborationapp.evaluation.Test
import com.karensarmiento.collaborationapp.grouping.GroupManager
import com.karensarmiento.collaborationapp.utils.*
import com.karensarmiento.collaborationapp.utils.JsonKeyword as Jk
import org.json.JSONArray
import org.json.JSONObject

data class Card(val title: String, val completed: Boolean)

class Automerge(
    /** The webview to run Automerge in. It should not surprisingly restart */
    private val webview: WebView,

    /** Callback when the list of cards changes. Not guaranteed to be on the UI thread */
    private val onCardsChangeCallback: (List<Card>) -> Unit
) {

    companion object {
        private const val TAG = "Automerge.kt"
    }


    private val perfLogger =
        PerfLogger { name, time ->
            val message = "$name: $time ms"
            Log.i("PerfLogger", message) // shows up in LogCat
            Toast.makeText(webview.context, message, Toast.LENGTH_SHORT)
                .show() // shows up in the UI
        }

    init {
        setupWebview()
        loadHtml()
    }

    /**
     * The following functions are done by asynchronously calling the JS sister function.
     *
     * All functions which make changes to state return a JSON summarising the change. This can be
     * accessed through the use of a functional callback.
     */
    @Synchronized fun addCard(groupName: String, card: Card, callback: ((String) -> Unit)? = null) {
        Test.currMeasurement.localMergeFromKotlinStart = System.currentTimeMillis()
        val document = GroupManager.getDocument(groupName)!!
        val docEncoded = AndroidUtils.base64(document)
        val titleEncoded = AndroidUtils.base64(card.title)
        webview.evaluateJavascript(
            "javascript:addCard(\"$docEncoded\", \"$titleEncoded\", ${card.completed});") {
            Test.currMeasurement.localMergeFromKotlinEnd = System.currentTimeMillis()
            handleLocalUpdateOutput(it, groupName, callback)
        }
    }

    @Synchronized fun removeCard(groupName: String, index: Int, callback: ((String) -> Unit)? = null) {
        val document = GroupManager.getDocument(groupName)!!
        val docEncoded = AndroidUtils.base64(document)
        webview.evaluateJavascript("javascript:removeCard(\"$docEncoded\", \"${index}\");") {
            handleLocalUpdateOutput(it, groupName, callback)
        }
    }

    @Synchronized fun setCardCompleted(groupName: String, index: Int, completed: Boolean, callback: ((String) -> Unit)? = null) {
        val document = GroupManager.getDocument(groupName)!!
        val docEncoded = AndroidUtils.base64(document)
        webview.evaluateJavascript(
            "javascript:setCardCompleted(\"$docEncoded\", \"${index}\", ${completed});") {
            handleLocalUpdateOutput(it, groupName, callback)
        }
    }

    @Synchronized fun applyJsonUpdate(update: PendingUpdate, groupName: String, jsonUpdate: String) {
        Test.currMeasurement.peerMergeFromKotlinStart = System.currentTimeMillis()
        val document = GroupManager.getDocument(groupName)!!
        val docEncoded = AndroidUtils.base64(document)
        val updateEncoded = AndroidUtils.base64(jsonUpdate)
        webview.evaluateJavascript("javascript:applyJsonUpdate(\"$docEncoded\", \"$updateEncoded\");") {
            try {
                if (it != "null" && it != null) {
                    Log.i(TAG, "Just finished applying an update and got doc: $it")
                    GroupManager.setDocument(groupName, it.removeSurrounding("\""))
                    Test.currMeasurement.peerMergeFromKotlinEnd = System.currentTimeMillis()
                    // ===================================
//                       if (Build.VERSION.RELEASE == "9") { // SAMSUNG
//                            // Store measurements
//                            storeMeasurements()
//                            if (Test.count < NUM_RUNS) {
//                                resetDocAndUI()
//                                // Reset test result holder
//                                Test.currMeasurement = TimingMeasurement()
//                                // Add new card to initiate next test.
//                                testingAddCard()
//                            }
//                            Test.count++
//                        } else { // HUAWEI
//                            testingAddCard {
//                                resetDocAndUI()
//                            }
//                        }
                    // ===================================
                } else {
                    peerUpdateBuffer.pushUpdate(update)
                }
            } finally {
                GroupManager.unlock(groupName)
                broadcastBufferUpdates(groupName)
            }
        }
    }


    @Synchronized fun createNewDocument(update: PendingUpdate, groupName: String)  {
        webview.evaluateJavascript("javascript:createNewTodoList();") {
            try {
                if (it != "null" && it != null) {
                    GroupManager.setDocument(groupName, it.removeSurrounding("\""))
                    GroupManager.setInitDocument(groupName, it.removeSurrounding("\""))
                    Test.initDoc = it.removeSurrounding("\"")
                    Log.i(TAG, "Applied doc init! Doc is now ${GroupManager.getDocument(it)}")

                } else {
                    docInitBuffer.pushUpdate(update)
                }
            } finally {
                GroupManager.unlock(groupName)
                broadcastBufferUpdates(groupName)
            }
        }
    }

    @Synchronized fun mergeNewDocument(update: PendingUpdate, groupName: String, docToMerge: String) {
        val docEncoded = AndroidUtils.base64(docToMerge)
        webview.evaluateJavascript("javascript:mergeNewDocument(\"$docEncoded\");") {
            try {
                if (it != "null" && it != null) {
                    GroupManager.setInitDocument(groupName, it.removeSurrounding("\""))
                    GroupManager.setDocument(groupName, it.removeSurrounding("\""))
                    Test.initDoc = it.removeSurrounding("\"")
                    Log.i(TAG, "Applied peerMerge! Doc is now ${GroupManager.getDocument(groupName)}")
                } else {
                    peerMergeBuffer.pushUpdate(update)
                }
            } finally {
                GroupManager.unlock(groupName)
                broadcastBufferUpdates(groupName)
            }
        }
    }

    // TODO: unlock here (lock before local updates)
    private fun handleLocalUpdateOutput(output: String, groupName: String, callback: ((String) -> Unit)?) {
        val responseJson = jsonStringToJsonObject(output)
        val changes = getJsonArrayOrNull(responseJson, Jk.CHANGES.text)
        val updatedDoc = getStringOrNull(responseJson, Jk.UPDATED_DOC.text)

        GroupManager.setDocument(groupName, updatedDoc!!)
        GroupManager.addChange(groupName, changes!!.toString())
        callback?.invoke(changes.toString())
    }

    private fun broadcastBufferUpdates(groupName: String) {
        val updateIntent = Intent()
        updateIntent.action = Jk.GROUP_MESSAGE.text
        updateIntent.putExtra(Jk.VALUE.text, groupName)
        MainActivity.appContext?.sendBroadcast(updateIntent)
    }

    /**
     * Setups the WebView object by enabling JS and adding the JS callbacks. Also enables a remote
     * debugging bridge.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebview() {
        webview.settings.javaScriptEnabled = true
        webview.addJavascriptInterface(object {
            @JavascriptInterface
            fun onCardsChange(json: String) {
                val jsonArray = JSONArray(json) // deserialize JSON into array
                val cards = List(jsonArray.length()) { // list construction
                    Card(
                        (jsonArray[it] as JSONObject).getString("title"),
                        (jsonArray[it] as JSONObject).getBoolean("completed")
                    )
                }

                onCardsChangeCallback(cards) // callback into UI
            }

            @JavascriptInterface
            fun startEvent(name: String) = perfLogger.startEvent(name)

            @JavascriptInterface
            fun endEvent(name: String) = perfLogger.endEvent(name)
        }, "ktchannel")

        // Allows remote debugging the JS state by entering "chrome://inspect" in Chrome/Chromium
        WebView.setWebContentsDebuggingEnabled(true)
    }

    private fun loadHtml() {
        perfLogger.startEvent("html_load") // the "end_event" will be called in index.html

        webview.resources.assets.open("index.html")
            .use {
                val htmlString = it.readBytes().toString(Charsets.UTF_8)
                webview.loadDataWithBaseURL(
                    "file:///android_asset/",
                    htmlString,
                    "text/html",
                    "utf-8",
                    ""
                )
            }
    }
}
