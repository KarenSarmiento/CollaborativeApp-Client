package com.karensarmiento.collaborationapp.collaboration

import android.util.Log
import com.karensarmiento.collaborationapp.utils.AccountUtils

private const val TAG = "UpdateBuffer"

// TODO: Change to generic type.
// TODO: Impose mutex on pendingUpdates vs lock on individual items ? Or in webview?
// Lock on individual items
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

val peerUpdates = UpdateBuffer()
val peerMerges = UpdateBuffer()
val docInits = UpdateBuffer()