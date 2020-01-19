package com.karensarmiento.collaborationapp.grouping

import android.util.Log


object GroupManager {
    private const val TAG = "GroupManager"

    // TODO: Persist group ids to memory.
    // Maps group names to group ids.
    private val ownedGroups: MutableMap<String, String> = mutableMapOf()

    fun groupId(groupName: String): String? {
        return ownedGroups[groupName]
    }

    fun registerGroup(groupName: String, groupId: String) {
        Log.i(TAG, "Registered $groupName to groupId $groupId.")
        ownedGroups[groupName] = groupId
    }
}