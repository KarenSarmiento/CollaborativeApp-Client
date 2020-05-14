package com.karensarmiento.collaborationapp.collaboration

import android.os.AsyncTask
import android.util.Log
import com.karensarmiento.collaborationapp.grouping.GroupManager
import com.karensarmiento.collaborationapp.utils.JsonKeyword


class DocInitHandler(val automerge: Automerge, private val docInit: PendingUpdate) : AsyncTask<Void, Void, Void>() {

    val TAG = "DocInitHandler"

    override fun doInBackground(vararg params: Void): Void? {
        Log.i(TAG, "OBTAINING LOCK FOR DOC INIT HANDLING.")
        GroupManager.lock(docInit.groupName)
        return null
    }

    override fun onPostExecute(result: Void?) {
        super.onPostExecute(result)
        val document = GroupManager.getDocument(docInit.groupName)
        if (document == JsonKeyword.WAITING_FOR_SELF.text) {
            automerge.createNewDocument(docInit, docInit.groupName)
            // Lock is released inside createNewDocument.
        } else {
            Log.w(TAG, "Got a pending doc init for group ${docInit.groupName} but " +
                    "this group's document was in state: $document.")
            docInitBuffer.pushUpdate(docInit)
            GroupManager.unlock(docInit.groupName)
        }
    }

}

class PeerMergeHandler(val automerge: Automerge, private val peerMerge: PendingUpdate) : AsyncTask<Void, Void, Void>() {

    val TAG = "PeerMergeHandler"

    override fun doInBackground(vararg params: Void): Void? {
        Log.i(TAG, "OBTAINING LOCK FOR PEER MERGE HANDLING.")
        GroupManager.lock(peerMerge.groupName)
        return null
    }

    override fun onPostExecute(result: Void?) {
        super.onPostExecute(result)
        val document = GroupManager.getDocument(peerMerge.groupName)
        if (document == JsonKeyword.WAITING_FOR_PEER.text) {
            automerge.mergeNewDocument(peerMerge, peerMerge.groupName, peerMerge.update)
            // Lock is released inside mergeNewDocument.
        } else {
            Log.w(TAG, "Got a pending peer merge for group ${peerMerge.groupName} but " +
                    "this group's document was in state: $document.")
            peerMergeBuffer.pushUpdate(peerMerge)
            GroupManager.unlock(peerMerge.groupName)
        }
    }

}

class PeerUpdateHandler(val automerge: Automerge, private val peerUpdate: PendingUpdate) : AsyncTask<Void, Void, Void>() {

    val TAG = "PeerUpdateHandler"

    override fun doInBackground(vararg params: Void): Void? {
        Log.i(TAG, "OBTAINING LOCK FOR PEER UPDATE HANDLING.")
        GroupManager.lock(peerUpdate.groupName)
        return null
    }

    override fun onPostExecute(result: Void?) {
        val document = GroupManager.getDocument(peerUpdate.groupName)
        if (document != JsonKeyword.WAITING_FOR_PEER.text && document != JsonKeyword.WAITING_FOR_SELF.text) {
            automerge.applyJsonUpdate(peerUpdate, peerUpdate.groupName, peerUpdate.update)
            // Lock is released inside applyJsonUpdate.
        } else {
            Log.i(TAG, "Could not apply peer update for group " +
                    "${peerUpdate.groupName} since their document is in state $document")
            GroupManager.unlock(peerUpdate.groupName)
            peerUpdateBuffer.pushUpdate(peerUpdate)
        }
    }

}