package com.karensarmiento.collaborationapp.grouping

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_device_group.*
import com.karensarmiento.collaborationapp.MainActivity
import com.karensarmiento.collaborationapp.R
import com.karensarmiento.collaborationapp.grouping.GroupManager.maybeSetCurrentGroup
import com.karensarmiento.collaborationapp.utils.JsonKeyword as Jk
import com.karensarmiento.collaborationapp.utils.Utils
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
        setUpRadioButtons()
        registerAddedToGroupListener()
    }

    private fun setUpRadioButtons() {
        val allGroups = GroupManager.getAllRegisteredGroups()
        for (group in allGroups) {
            addNewRadioButton(group)
        }
    }

    private fun setUpButtonListeners() {
        button_create_group.setOnClickListener {
            // TODO: Validate that the group name is unique.
            val groupName = group_name_text_field.text.toString()
            val peerEmail = text_field_peer_email.text.toString()
            Firebase.sendCreateGroupRequest(groupName, peerEmail)
            // TODO: Buffer all group packets until we have received a successful group creation response.
//            startActivity(MainActivity.getLaunchIntent(this))
        }

        radio_group_list.setOnCheckedChangeListener { _, _ ->
            button_existing_group.isEnabled = true
        }

        button_existing_group.setOnClickListener {
            val groupName = getSelectedRadioButtonText()
            if (groupName == null) {
                Utils.hideKeyboard(this)
                Snackbar.make(
                    findViewById(R.id.radio_group_list),
                    R.string.error_join_group_with_none_selected,
                    Snackbar.LENGTH_SHORT
                ).show()
            } else {
                // TODO: Load data corresponding with existing group and update ui for messages
                // related only to this group.
                val result = maybeSetCurrentGroup(groupName)
                if (result) {
                    GroupManager.currentGroup = groupName
                    startActivity(MainActivity.getLaunchIntent(this))
                } else {
                    Utils.hideKeyboard(this)
                    Snackbar.make(
                        findViewById(R.id.radio_group_list),
                        R.string.error_join_unregistered_group,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun registerAddedToGroupListener() {
        val addedToGroupListener = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val groupName = intent.getStringExtra(Jk.VALUE.text)
                groupName?.let {
                    addNewRadioButton(groupName)
                }
            }
        }
        val intentFilter = IntentFilter(Jk.ADDED_TO_GROUP.text)
        registerReceiver(addedToGroupListener, intentFilter)
    }

    private fun addNewRadioButton(buttonText: String) {
        val radioButton = RadioButton(this)
        radioButton.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        radioButton.text = buttonText
        radio_group_list.addView(radioButton)
    }

    private fun getSelectedRadioButtonText(): String? {
        if (radio_group_list.checkedRadioButtonId != -1) {
            return (findViewById<View>(radio_group_list.checkedRadioButtonId) as RadioButton)
                .text.toString()
        }
        return null
    }
    override fun onDestroy() {
        super.onDestroy()
        // TODO: deregister broadcast receiver
    }
}
