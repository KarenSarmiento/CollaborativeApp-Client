package com.karensarmiento.collaborationapp.grouping

import android.app.Activity
import android.os.AsyncTask
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.karensarmiento.collaborationapp.utils.Utils
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


object GroupManager {
    private const val TAG = "GroupManager"
    // TODO: Persist group tokens to memory.
    // Maps group names to group tokens (notification key).
    private val ownedGroups: MutableMap<String, String> = mutableMapOf()

    fun createGroup(activity: Activity, groupName: String, callback: ((Unit) -> Unit)? = null) {
        FirebaseGroupCreator(groupName, callback).execute(activity)
    }


    // TODO: Validate.
    fun addToGroup(userEmail: String, userToken: String, groupName: String) {
        Log.i(TAG, "Adding $userToken to group $groupName.")
        if (!ownedGroups.containsKey(groupName)) {
            Log.w(TAG, "Adding to group failed: You do not own a group with name $groupName.")
            return
        }
        FirebaseGroupCollaboratorAdder(userEmail, userToken, groupName).execute()
    }

    fun groupToken(groupName: String): String? {
        return ownedGroups[groupName]
    }

    private class FirebaseGroupCreator(
        val groupName: String, val callback: ((Unit) -> Unit)? = null)
        : AsyncTask<Any, Void, Void>() {

        override fun doInBackground(vararg params: Any?): Void? {
            val activity = params[0] as Activity
            val account = Utils.getGoogleAccount(activity)
            val scope = "audience:server:client_id:${Utils.CLIENT_ID}"
            try {
                val groupToken = GoogleAuthUtil.getToken(activity, account, scope)
                ownedGroups[groupName] = groupToken
                Log.i(TAG, "Created group with token $groupToken")
            } catch (e: Exception) {
                Log.w(TAG, "Exception while getting idToken: $e")
                // TODO: Make error visible to user.
            }
            return null
        }

        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
            callback?.invoke(Unit)
        }
    }

    private class FirebaseGroupCollaboratorAdder(
        val userEmail: String, val userToken: String, val groupName: String)
        : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg params: String?): String {
            ownedGroups[groupName].let {groupToken ->
                // Configure HTTP Connection
                val url = URL("https://fcm.googleapis.com/fcm/googlenotification")
                val con = url.openConnection() as HttpURLConnection
                con.doOutput = true

                // HTTP request header
                con.setRequestProperty("project_id", Utils.SENDER_ID)
                con.setRequestProperty("Content-Type", "application/json")
                con.setRequestProperty("Accept", "application/json")
                con.requestMethod = "POST"
                con.connect()

                // HTTP request
                val data = JSONObject()
                data.put("operation", "add")
                data.put("notification_key_name", userEmail)
                data.put("registration_ids", JSONArray(arrayListOf(userToken)))
                data.put("id_token", groupToken)

                val os = con.outputStream
                os.write(data.toString().toByteArray(charset("UTF-8")))
                os.close()

                // Read the response into a string
                val `is` = con.inputStream
                val responseString = Scanner(`is`, "UTF-8")
                    .useDelimiter("\\A").next()
                `is`.close()

                // Parse the JSON string and return the notification key
                val response = JSONObject(responseString)
                val newGroupToken = response.getString("notification_key")
                ownedGroups[groupName] = newGroupToken
                Log.i(TAG, "Group token changed from $groupToken to $newGroupToken.")
                Log.i(TAG, "$userToken added to $groupName successfully.")
                return newGroupToken
            }
        }
    }
}