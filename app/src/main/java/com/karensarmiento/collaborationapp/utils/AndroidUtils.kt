package com.karensarmiento.collaborationapp.utils

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Base64
import android.view.View
import android.view.inputmethod.InputMethodManager

object AndroidUtils {
    fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = activity.currentFocus
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun createSimpleBroadcastReceiver(context: Context, filter: String, callback: (String) -> Unit): BroadcastReceiver {
        val listener = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val value = intent.getStringExtra(JsonKeyword.VALUE.text)
                value?.let {
                    callback(value)
                }
            }
        }
        val intentFilter = IntentFilter(filter)
        context.registerReceiver(listener, intentFilter)
        return listener
    }

    fun base64(string: String): String {
        return String(Base64.encode(string.toByteArray(), Base64.DEFAULT)).replace("\n", "")
    }
}