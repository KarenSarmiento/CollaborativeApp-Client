package com.karensarmiento.collaborationapp.grouping

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.karensarmiento.collaborationapp.collaboration.MainActivity
import com.karensarmiento.collaborationapp.R
import com.karensarmiento.collaborationapp.utils.AndroidUtils
import com.karensarmiento.collaborationapp.utils.JsonKeyword as Jk
import com.karensarmiento.collaborationapp.utils.AccountUtils
import com.karensarmiento.collaborationapp.messaging.FirebaseMessageSendingService as Firebase
import kotlinx.android.synthetic.main.activity_group_settings.*
import kotlinx.android.synthetic.main.add_friends_entry_box.view.*


class GroupSettingsActivity : AppCompatActivity() {

    private lateinit var peerAddedToGroupListener: BroadcastReceiver
    private lateinit var peerRemovedFromGroupListener: BroadcastReceiver

    companion object {
        private const val TAG = "GroupSettingsActivity"
        private lateinit var currentGroup: String
        fun getLaunchIntent(from: Context) = Intent(from, GroupSettingsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentGroup = GroupManager.currentGroup ?: "NULL"

        setContentView(R.layout.activity_group_settings)
        setUpTitleBar()
        refreshListOfCurrentFriends()

        supportAddingToGroup()
        supportLeavingGroup()
        backToMainActivityOnDone()
        registerGroupMembershipListeners()
    }

    private fun setUpTitleBar() {
        val headerText = "Share ${GroupManager.currentGroup} with Friends"
        actionBar?.title = headerText
        supportActionBar?.title = headerText
    }

    private fun registerGroupMembershipListeners() {
        peerAddedToGroupListener = AndroidUtils.createSimpleBroadcastReceiver(
            this, Jk.ADD_PEER_TO_GROUP.text) {
            if (it == GroupManager.currentGroup)
                refreshListOfCurrentFriends()
        }

        peerRemovedFromGroupListener = AndroidUtils.createSimpleBroadcastReceiver(
            this, Jk.REMOVED_PEER_FROM_GROUP.text) {
            if (it == GroupManager.currentGroup)
                refreshListOfCurrentFriends()
        }
    }

    private fun refreshListOfCurrentFriends() {
        layout_friends_list.removeAllViewsInLayout()
        val groupMembers = GroupManager.getMembers(currentGroup) ?: setOf()
        if (groupMembers.isEmpty()) {
            val textView = TextView(this)
            textView.text = getString(R.string.text_no_friends)
            layout_friends_list.addView(textView)
        } else {
            for (friend in groupMembers) {
                // TODO: Display remove button next to each member.
                val textView = TextView(this)
                textView.text = friend
                layout_friends_list.addView(textView)
            }
        }
    }

    private fun supportAddingToGroup() {
        friends_entry_box.button_add_friend.setOnClickListener {
            // TODO: Validate email
            val peerEmail = friends_entry_box.text_entry_friend.text.toString().removeSurrounding(" ")
            val groupId = GroupManager.groupId(currentGroup)
            if (groupId == null) {
                Log.e(TAG, "Could not add peer to group because there was no group id for " +
                        "group $currentGroup.")
                return@setOnClickListener
            }
            Firebase.sendAddPeerToGroupRequest(currentGroup, groupId, peerEmail)
        }
    }

    private fun supportLeavingGroup() {
        button_leave.setOnClickListener {
            val groupId = GroupManager.groupId(currentGroup)
            if (groupId == null) {
                Log.e(TAG, "Could not leave group because there was no group id for " +
                        "group $currentGroup.")
                return@setOnClickListener
            }
            Firebase.sendRemovePeerFromGroupRequest(currentGroup, groupId, AccountUtils.getGoogleEmail())
            startActivity(DeviceGroupActivity.getLaunchIntent(this))
        }
    }

    private fun backToMainActivityOnDone() {
        button_back.setOnClickListener {
            startActivity(MainActivity.getLaunchIntent(this))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(peerAddedToGroupListener)
        unregisterReceiver(peerRemovedFromGroupListener)
    }
}
