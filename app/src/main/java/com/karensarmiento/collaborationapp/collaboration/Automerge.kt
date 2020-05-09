package com.karensarmiento.collaborationapp.collaboration

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import com.karensarmiento.collaborationapp.evaluation.Test
import com.karensarmiento.collaborationapp.grouping.GroupManager
import com.karensarmiento.collaborationapp.utils.*
import com.karensarmiento.collaborationapp.utils.JsonKeyword as Jk
import org.json.JSONArray
import org.json.JSONObject

internal data class Card(val title: String, val completed: Boolean)

internal class Automerge(
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
     */ // TODO: Ensure that a document is not edited concurrently. CREATE LOCK ON WEBVIEW?
    fun addCard(groupName: String, card: Card, callback: ((String) -> Unit)? = null) {
        val document = GroupManager.getDocument(groupName)
        // TODO: Protect against javascript injection.
        webview.evaluateJavascript(
            "javascript:addCard(encodeURIComponent(\"$document\"), \"${card.title}\", ${card.completed});") {
            handleUpdateOutput(it, groupName, callback)
        }
    }


    fun removeCard(groupName: String, index: Int, callback: ((String) -> Unit)? = null) {
        val document = GroupManager.getDocument(groupName)
        webview.evaluateJavascript("javascript:removeCard(encodeURIComponent(\"$document\"), \"${index}\");") {
            handleUpdateOutput(it, groupName, callback)
        }
    }


    fun setCardCompleted(groupName: String, index: Int, completed: Boolean, callback: ((String) -> Unit)? = null) {
        val document = GroupManager.getDocument(groupName)
        Test.currMeasurement.localMergeFromKotlinStart = System.currentTimeMillis()
        webview.evaluateJavascript(
            "javascript:setCardCompleted(encodeURIComponent(\"$document\"), \"${index}\", ${completed});") {
            Test.currMeasurement.localMergeFromKotlinEnd = System.currentTimeMillis()
            handleUpdateOutput(it, groupName, callback)
        }
    }

    fun applyJsonUpdate(groupName: String, jsonUpdate: String, callback: ((String) -> Unit)? = null) {
        val document = GroupManager.getDocument(groupName)
        Test.currMeasurement.peerMergeFromKotlinStart = System.currentTimeMillis()
        webview.evaluateJavascript("javascript:applyJsonUpdate(encodeURIComponent(\"$document\"), encodeURIComponent(\"$jsonUpdate\"));") {
            if (it != "null" && it != null) {
                GroupManager.setDocument(groupName, it.removeSurrounding("\""))
                Test.currMeasurement.peerMergeFromKotlinEnd = System.currentTimeMillis()
                callback?.invoke(it)
            }
        }
    }


    fun createNewDocument(groupName: String, callback: ((String) -> Unit)? = null)  {
        webview.evaluateJavascript("javascript:createNewTodoList();") {
            Log.i(TAG, "BLOOP: CreateNewDocumentCalled!")
            if (it != "null" && it != null) {
                GroupManager.setDocument(groupName, it.removeSurrounding("\""))
                callback?.invoke(it)
            }
        }
    }

    fun mergeNewDocument(groupName: String, docToMerge: String, callback: ((String) -> Unit)? = null) {
        Log.i(TAG, "BLOOP: mergeNewDocument called")
        webview.evaluateJavascript("javascript:mergeNewDocument(encodeURIComponent(\"$docToMerge\"));") {
            if (it != "null" && it != null) {
                GroupManager.setDocument(groupName, it.removeSurrounding("\""))
                callback?.invoke(it)
                Log.i(TAG, "BLOOP: Merging new document to get ${it.removeSurrounding("\"")}")
            }
        }
    }

    private fun handleUpdateOutput(output: String, groupName: String, callback: ((String) -> Unit)?) {
        val responseJson = jsonStringToJsonObject(output)
        val changes = getJsonArrayOrNull(responseJson, Jk.CHANGES.text)
        val updatedDoc = getStringOrNull(responseJson, Jk.UPDATED_DOC.text)

        GroupManager.setDocument(groupName, escapePunctuation(updatedDoc!!))
        print(escapePunctuation(updatedDoc!!))
        callback?.invoke(escapePunctuation(changes!!.toString()))
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
