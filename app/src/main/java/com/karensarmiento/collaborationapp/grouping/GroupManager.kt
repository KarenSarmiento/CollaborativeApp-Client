package com.karensarmiento.collaborationapp.grouping

import android.app.Activity
import android.os.AsyncTask
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.karensarmiento.collaborationapp.Utils


object GroupManager {
    private const val TAG = "GroupManager"
    // TODO: Persist group tokens to memory.
    private val ownedGroups: MutableSet<String> = mutableSetOf()

    fun createGroup(activity: Activity, callback: ((Unit) -> Unit)? = null) {
        FirebaseGroupCreator(callback).execute(activity)
    }

    fun joinGroup(activity: Activity, callback: ((Unit) -> Unit)? = null) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private class FirebaseGroupCreator(val callback: ((Unit) -> Unit)? = null) : AsyncTask<Any, Void, Void>() {
        override fun doInBackground(vararg params: Any?): Void? {
            val activity = params[0] as Activity
            val account = Utils.getGoogleAccount(activity)
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

        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
            callback?.invoke(Unit)
        }
    }
}