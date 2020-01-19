package com.karensarmiento.collaborationapp.grouping

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_device_group.*
import com.karensarmiento.collaborationapp.MainActivity
import com.karensarmiento.collaborationapp.R
import com.karensarmiento.collaborationapp.messaging.FirebaseMessageSendingService as Firebase


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
            // TODO: Validate that the group name is unique.
            val groupName = group_name_text_field.text.toString()
            val peerEmail = text_field_peer_email.text.toString()
            Firebase.sendCreateGroupRequest(groupName, peerEmail)
            // TODO: Buffer all group packets until we have received a successful group creation response.
            startActivity(MainActivity.getLaunchIntent(this))
        }
    }
}
