package com.karensarmiento.collaborationapp.grouping

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.karensarmiento.collaborationapp.utils.Utils
import kotlinx.android.synthetic.main.activity_device_group.*
import com.google.android.material.snackbar.Snackbar
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
            GroupManager.createGroup(this, groupName) {
                addSelfToGroupOrShowError(groupName)
                val peerEmail = text_field_peer_email.text.toString()
                val peerToken = Firebase.sendGetNotificationKeyRequest(peerEmail)

                // if successful then
                 startActivity(MainActivity.getLaunchIntent(this))
            }
        }
    }

    private fun addSelfToGroupOrShowError(groupName: String) {
        Utils.onCurrentFirebaseToken {
            addUserToGroupOrShowError(it, groupName)
        }
    }

    private fun addUserToGroupOrShowError(groupName: String, userToken: String) {
        val account = Utils.getGoogleAccount()
        if (account == null)
            showSnackBarError(R.string.sign_in_to_account_request)
        else
            GroupManager.addToGroup(account.name, userToken, groupName)
    }


    private fun showSnackBarError(stringId: Int) {
        Utils.hideKeyboard(this)
        Snackbar.make(findViewById(R.id.button_create_group), stringId, Snackbar.LENGTH_SHORT).show()
    }
}
