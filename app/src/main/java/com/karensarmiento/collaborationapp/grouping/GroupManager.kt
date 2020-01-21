package com.karensarmiento.collaborationapp.grouping

import android.util.Log


object GroupManager {
    private const val TAG = "GroupManager"

    // TODO: Persist group ids to memory.
    // Maps group names to group ids.
    private val ownedGroups: MutableMap<String, String> = mutableMapOf()
    var currentGroup: String? = null
        get() = field

    fun maybeSetCurrentGroup(groupName: String): Boolean {
        if (ownedGroups.containsKey(groupName)) {
            currentGroup = groupName
            return true
        }
        Log.e(TAG, "Attempted to set current group as an unregistered group.")
        return false
    }

    fun groupId(groupName: String): String? {
        return ownedGroups[groupName]
    }

    fun registerGroup(groupName: String, groupId: String) {
        Log.i(TAG, "Registered $groupName to groupId $groupId.")
        ownedGroups[groupName] = groupId
    }

    fun getAllRegisteredGroups(): MutableSet<String> {
        return ownedGroups.keys
    }
}