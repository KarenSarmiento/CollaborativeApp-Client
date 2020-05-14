package com.karensarmiento.collaborationapp.collaboration

import android.os.AsyncTask
import com.karensarmiento.collaborationapp.grouping.GroupManager

class TodoDeleter(val automerge: Automerge, private val groupName: String, private val index: Int)
    : AsyncTask<Void, Void, Void>() {

    val TAG = "TodoDeleter"

    override fun doInBackground(vararg params: Void): Void? {
        GroupManager.lock(groupName)
        return null
    }

    override fun onPostExecute(result: Void?) {
        super.onPostExecute(result)
        automerge.removeCard(groupName, index)
        // Lock is released inside removeCard.
    }
}

class TodoAdder(val automerge: Automerge, private val groupName: String, private val todoText: String)
    : AsyncTask<Void, Void, Void>() {

    val TAG = "TodoAdder"

    override fun doInBackground(vararg params: Void): Void? {
        GroupManager.lock(groupName)
        return null
    }

    override fun onPostExecute(result: Void?) {
        super.onPostExecute(result)
        automerge.addCard(groupName, Card(todoText, false))
        // Lock is released inside addCard.
    }
}

class TodoChecker(val automerge: Automerge, private val groupName: String, private val index: Int, private val completed: Boolean)
    : AsyncTask<Void, Void, Void>() {

    val TAG = "TodoChecker"

    override fun doInBackground(vararg params: Void): Void? {
        GroupManager.lock(groupName)
        return null
    }

    override fun onPostExecute(result: Void?) {
        super.onPostExecute(result)
        automerge.setCardCompleted(groupName, index, completed)
    }
}