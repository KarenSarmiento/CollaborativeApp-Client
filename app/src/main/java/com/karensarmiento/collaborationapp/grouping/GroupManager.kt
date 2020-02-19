package com.karensarmiento.collaborationapp.grouping

import android.util.Log
import javax.crypto.SecretKey


object GroupManager {
    private const val TAG = "GroupManager"

    // TODO: Persist group ids to memory.
    // TODO: Eliminate group name and incorporate it into the todo list json.

    /**
     *  Maps groupName (human-readable, locally unique group identifier) to group data.
     */
    private val groups: MutableMap<String, GroupData> = mutableMapOf()

    var currentGroup: String? = null
        get() = field

    fun maybeSetCurrentGroup(groupName: String): Boolean {
        if (groups.containsKey(groupName)) {
            currentGroup = groupName
            return true
        }
        Log.e(TAG, "Attempted to set current group as an unregistered group.")
        return false
    }

    fun groupId(groupName: String): String? {
        return groups[groupName]?.groupId
    }

    fun registerGroup(groupName: String, groupId: String, memberEmails: MutableSet<String>,
                      key: SecretKey? = null, serverKey: SecretKey? = null) {
        if (groupName in groups) {
            Log.e(TAG, "Cannot register group with name $groupName since group already exists.")
            return
        }
        groups[groupName] = GroupData(groupId, memberEmails, key, serverKey)
        Log.i(TAG, "Registered $groupName to groupId $groupId.")
    }

    fun getAllRegisteredGroups(): MutableSet<String> {
        return groups.keys
    }

    fun setGroupKey(groupName: String, aesKey: SecretKey) {
        if (!groups.containsKey(groupName)) {
            Log.e(TAG, "Cannot set key for group $groupName since this group does not exist.")
            return
        }
        groups[groupName]!!.key = aesKey
    }

    fun setServerGroupKey(groupName: String, serverKey: SecretKey?) {
        if (!groups.containsKey(groupName)) {
            Log.e(TAG, "Cannot set key for group $groupName since this group does not exist.")
            return
        }
        groups[groupName]!!.serverKey = serverKey
    }

    fun getMembers(groupName: String): Set<String>? {
        if (!groups.containsKey(groupName)) {
            Log.e(TAG, "Cannot get members for group $groupName since this group does not exist.")
            return null
        }
        return groups[groupName]?.members as Set<String>
    }

    fun getGroupKey(groupName: String): SecretKey? {
        if (!groups.containsKey(groupName)) {
            Log.e(TAG, "Cannot get key for group $groupName since this group does not exist.")
            return null
        }
        return groups[groupName]?.key
    }

    // TODO: Once eliminated group name, remove this method. Avoid linear search!!
    fun groupName(groupId: String): String? {
        for ((groupName, groupData) in groups) {
            if (groupData.groupId == groupId)
                return groupName
        }
        Log.i(TAG, "No group name was found for group id $groupId")
        return null
    }
}

/**
 *  Contains all data associated with a group.
 *
 *  groupId: the app identifier for the group.
 *  members: the emails of all users belonging to the group.
 *  key: the key shared among all peers in the group.
 *  serverKey: the key shared among all peers in the group AND the server (used to encrypt message
 *             headers appended by server).
 */
data class GroupData(var groupId: String, val members: MutableSet<String>, var key: SecretKey?,
                     var serverKey: SecretKey?)