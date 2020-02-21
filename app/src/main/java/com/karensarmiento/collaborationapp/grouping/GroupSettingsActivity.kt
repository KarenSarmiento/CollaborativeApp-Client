package com.karensarmiento.collaborationapp.grouping

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.karensarmiento.collaborationapp.R


class GroupSettingsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "GroupSettingsActivity"
        fun getLaunchIntent(from: Context) = Intent(from, GroupSettingsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_settings)
        setUpTitleBar()

        Log.i(TAG, "CREATED GROUP SETTINGS PAGE")
    }

    private fun setUpTitleBar() {
        val headerText = "Share To-do List"
        actionBar?.title = headerText
        supportActionBar?.title = headerText
    }

}
