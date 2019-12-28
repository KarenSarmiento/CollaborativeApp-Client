package com.karensarmiento.collaborationapp.collaboration

import android.accounts.Account
import android.app.Activity
import android.os.AsyncTask
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.karensarmiento.collaborationapp.Utils


object GroupManager {
    private const val TAG = "GroupManager"
    // TODO: Persist group tokens to memory.
    private val ownedGroups: MutableSet<String> = mutableSetOf()

    fun createGroup(activity: Activity, account: Account) {
        FirebaseGroupCreator().execute(activity, account)
    }

    private class FirebaseGroupCreator : AsyncTask<Any, Void, Void>() {
        override fun doInBackground(vararg params: Any?): Void? {
            val activity = params[0] as Activity
            val account = params[1] as Account
            val scope = "audience:server:client_id:${Utils.CLIENT_ID}"
            var idToken: String? = null
            try {
                idToken = GoogleAuthUtil.getToken(activity, account, scope)
            } catch (e: Exception) {
                Log.w(TAG, "Exception while getting idToken: $e")
                // TODO: Make error visible to user.
            }
            ownedGroups.add(idToken!!)
            Log.i(TAG, "Created group with token $idToken")
            return null

        }
    }
}