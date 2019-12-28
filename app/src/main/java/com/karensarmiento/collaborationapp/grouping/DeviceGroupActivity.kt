package com.karensarmiento.collaborationapp.grouping

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.karensarmiento.collaborationapp.MainActivity
import com.karensarmiento.collaborationapp.R
import kotlinx.android.synthetic.main.activity_device_group.*

class DeviceGroupActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DeviceGroupActivity"
        fun getLaunchIntent(from: Context) = Intent(from, DeviceGroupActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_group)

        setUpButtonListeners()
    }

    private fun setUpButtonListeners() {
        button_create_group.setOnClickListener {
            GroupManager.createGroup(this) {
                startActivity(MainActivity.getLaunchIntent(this))
            }
        }

        button_join_group.setOnClickListener {
            GroupManager.joinGroup(this) {
                startActivity(MainActivity.getLaunchIntent(this))
            }
        }
    }
}
