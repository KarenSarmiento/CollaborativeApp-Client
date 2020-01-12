package com.karensarmiento.collaborationapp.grouping

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import com.karensarmiento.collaborationapp.MainActivity
import com.karensarmiento.collaborationapp.utils.Utils
import kotlinx.android.synthetic.main.activity_device_group.*
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.karensarmiento.collaborationapp.R


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
        // TODO: Display groups as radio buttons or state that there are no groups on the screen.
    }

    private fun setUpButtonListeners() {
        button_create_group.setOnClickListener {
            // TODO: This would allow you to create lots of groups that have no members (and end up
            // sharing same group token). Provide an error or force use to enter the just created
            // group.
            // TODO: Validate that the group name is unique.
            val groupName = group_name_text_field.text.toString()
            GroupManager.createGroup(this, groupName) {
                addNewRadioButton(groupName)
            }
            getSelectedRadioButtonText()
        }

        button_join_group.setOnClickListener {
            val groupName = getSelectedRadioButtonText()
            if (groupName == null) {
                showSnackBarError(R.string.join_group_with_none_selected)
            } else {
                Utils.onCurrentFirebaseToken {
                    val account = Utils.getGoogleAccount()
                    if (account == null)
                        showSnackBarError(R.string.sign_in_to_account_request)
                    else
                        GroupManager.addToGroup(account.name, it, groupName)
                }
                startActivity(MainActivity.getLaunchIntent(this))
            }
        }
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

    private fun showSnackBarError(stringId: Int) {
        Utils.hideKeyboard(this)
        Snackbar.make(findViewById(R.id.radio_group_list), stringId, Snackbar.LENGTH_SHORT).show()
    }
}
