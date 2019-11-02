package com.lambdapioneer.webviewexperiments

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject

internal data class Card(val title: String)

internal class Automerge(
    /** The webview to run AutoMerge in. It should not surprisingly restart */
    private val webview: WebView,

    /** Callback when the list of cards changes. Not guaranteed to be on the UI thread */
    private val onCardsChangeCallback: (List<Card>) -> Unit
) {

    private val perfLogger = PerfLogger { name, time ->
        val message = "$name: $time ms"
        Log.i("PerfLogger", message) // shows up in LogCat
        Toast.makeText(webview.context, message, Toast.LENGTH_SHORT).show() // shows up in the UI
    }

    init {
        setupWebview()
        loadHtml()
    }

    // adding a card will be done by (async) executing a function call in JS
    fun addCard(card: Card) =
        webview.evaluateJavascript("javascript:addCard(\"${card.title}\");") {}

    // removing a card will be done by (async) executing a function call in JS
    fun removeCard() =
        webview.evaluateJavascript("javascript:removeCard()") {}

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
                    Card((jsonArray[it] as JSONObject).getString("title"))
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
