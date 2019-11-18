package com.karensarmiento.collaborationapp.collaboration

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
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
     */
    fun addCard(card: Card, callback: ((String) -> Unit)? = null) =
        webview.evaluateJavascript(
            "javascript:addCard(\"${card.title}\", \"${card.completed}\");") {
            callback?.invoke(it)
        }

    fun removeCard(callback: ((String) -> Unit)? = null) =
        webview.evaluateJavascript("javascript:removeCard();") {
            callback?.invoke(it)
        }

    fun setCardCompleted(index: Int, completed: Boolean, callback: ((String) -> Unit)? = null) =
        webview.evaluateJavascript(
            "javascript:setCardCompleted(\"${index}\", \"${completed}\");") {
            callback?.invoke(it)
        }

    fun getDocumentState(callback: (String) -> Unit) {
        webview.evaluateJavascript("javascript:getDocumentState();") {
            callback?.invoke(it)
        }
    }

    // TODO: Protect against javascript injection.
    fun setDocumentState(docState: String) =
        webview.evaluateJavascript("javascript:setDocumentState(\"${docState}\");") {}

    fun clearState() =
        webview.evaluateJavascript("javascript:clearState();") {}


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
