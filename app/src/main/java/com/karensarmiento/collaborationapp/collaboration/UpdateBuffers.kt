package com.karensarmiento.collaborationapp.collaboration

import com.karensarmiento.collaborationapp.utils.AccountUtils

private const val TAG = "UpdateBuffer"

class UpdateBuffer {
    private val pendingUpdates = mutableMapOf<String, PendingUpdate>()

    @Synchronized fun pushUpdate(groupName: String, update: String) {
        val updateId = AccountUtils.getUniqueId()
        pendingUpdates[updateId] = PendingUpdate(groupName, update, updateId)
    }

    @Synchronized fun pushUpdate(pendingUpdate: PendingUpdate) {
        pendingUpdates[pendingUpdate.id] = pendingUpdate
    }

    @Synchronized fun popPendingUpdates(): Set<PendingUpdate> {
        val pendingUpdatesImmutable = pendingUpdates.toMap()
        pendingUpdates.keys.removeAll(pendingUpdatesImmutable.keys)
        return pendingUpdatesImmutable.values.toSet()
    }

    @Synchronized fun hasPendingUpdates(): Boolean {
        return pendingUpdates.isNotEmpty()
    }

    @Synchronized fun getPendingUpdates(): Set<PendingUpdate> {
        return pendingUpdates.values.toSet()
    }
}

data class PendingUpdate(val groupName: String, val update: String, val id: String)

val docInitBuffer = UpdateBuffer()
val peerMergeBuffer = UpdateBuffer()
val peerUpdateBuffer = UpdateBuffer()

class UpdateBufferList {
    private val pendingUpdates = mutableMapOf<String, MutableList<PendingUpdate>>()

    @Synchronized fun pushUpdate(groupName: String, update: String) {
        val updateId = AccountUtils.getUniqueId()
        if (pendingUpdates[groupName] == null) {
            pendingUpdates[groupName] = mutableListOf()
        }
        pendingUpdates[groupName]!!.add(PendingUpdate(groupName, update, updateId))
    }

    @Synchronized fun hasPendingUpdatesForKey(key: String): Boolean {
        if (pendingUpdates[key] == null)
            return false
        return pendingUpdates[key]!!.isEmpty()
    }

    @Synchronized fun popPendingUpdatesForKey(key: String): List<PendingUpdate> {
        if (key in pendingUpdates) {
            return pendingUpdates[key]!!.toList()
        }
        return listOf()
    }

    @Synchronized fun deleteKey(key: String) {
        pendingUpdates.remove(key)
    }
}

/**
 * Used to store updates that were received for each group BEFORE they could be decrypted since
 * the client did not yet own the user key for that group.
 */
val unencryptedUpdateBuffer = UpdateBufferList()