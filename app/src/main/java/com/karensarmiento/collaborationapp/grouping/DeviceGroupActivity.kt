package com.karensarmiento.collaborationapp.grouping

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.android.synthetic.main.activity_device_group.*
import com.karensarmiento.collaborationapp.collaboration.MainActivity
import com.karensarmiento.collaborationapp.R
import com.karensarmiento.collaborationapp.grouping.GroupManager.maybeSetCurrentGroup
import com.karensarmiento.collaborationapp.utils.AndroidUtils
import com.karensarmiento.collaborationapp.utils.JsonKeyword as Jk
import com.karensarmiento.collaborationapp.messaging.FirebaseMessageSendingService as Firebase


class DeviceGroupActivity : AppCompatActivity() {

    private lateinit var addedToGroupListener: BroadcastReceiver
    private lateinit var removedFromGroupListener: BroadcastReceiver

    companion object {
        private const val TAG = "DeviceGroupActivity"
        fun getLaunchIntent(from: Context) = Intent(from, DeviceGroupActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_group)
        setUpTitleBar()

        registerGroupMembershipListeners()
        refreshListOfTodoLists()
    }

    private fun setUpTitleBar() {
        val headerText = "My Tasks"
        actionBar?.title = headerText
        supportActionBar?.title = headerText
    }

    private fun registerGroupMembershipListeners() {
        addedToGroupListener = AndroidUtils.createSimpleBroadcastReceiver(
            this, Jk.ADDED_TO_GROUP.text) { insertTodoListSelector(it) }

        removedFromGroupListener = AndroidUtils.createSimpleBroadcastReceiver(
            this, Jk.REMOVED_FROM_GROUP.text) { refreshListOfTodoLists() }
    }

    private fun refreshListOfTodoLists() {
        layout_todo_lists.removeAllViewsInLayout()
        insertNewTodoListOption()
        for (groupName in GroupManager.getAllRegisteredGroups()) {
            insertTodoListSelector(groupName)
        }
    }

    private fun insertTodoListSelector(groupName: String) {
        // Create view.
        val view = layoutInflater.inflate(R.layout.todo_list_selector, layout_todo_lists, false)
        val text = view.findViewById<TextView>(R.id.text)
        text.text = groupName

        // Set as current group and go to main activity.v
        view.setOnClickListener {
            Log.i(TAG, "Switching to group $groupName")
            val result = maybeSetCurrentGroup(groupName)
            if (result) {
                GroupManager.currentGroup = groupName
                // TODO: Restore state for this group.
                startActivity(MainActivity.getLaunchIntent(this))
            } else {
                AndroidUtils.hideKeyboard(this)
                Snackbar.make(
                    findViewById(R.id.layout_todo_lists),
                    R.string.error_join_unregistered_group,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
        layout_todo_lists.addView(view)
    }

    private fun insertNewTodoListOption() {
        val view = layoutInflater.inflate(R.layout.new_todo_list, layout_todo_lists, false)
        layout_todo_lists.addView(view, 0)

        val addButton = view.findViewById<ImageButton>(R.id.add_button)
        addButton.setOnClickListener {
            // TODO: Validate that the group name is unique.
            val groupName =  view.findViewById<TextInputEditText>(R.id.text_field_new_group).text.toString()
            Firebase.sendCreateGroupRequest(groupName, setOf())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(addedToGroupListener)
        unregisterReceiver(removedFromGroupListener)
    }
}
