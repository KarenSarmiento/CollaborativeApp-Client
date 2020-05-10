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

    // Group name of current group.
    var currentGroup: String? = null
        get() = field

    fun getCurrentGroupKey(): SecretKey? {
        return groups[currentGroup]?.key
    }

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

    fun registerGroup(
        groupName: String, groupId: String, memberEmails: MutableSet<String>, document: String,
        key: SecretKey? = null) {
        if (groupName in groups) {
            Log.e(TAG, "Cannot register group with name $groupName since group already exists.")
            return
        }
        groups[groupName] = GroupData(groupId, memberEmails, key, document)
        Log.i(TAG, "Registered $groupName to groupId $groupId.")
    }

    fun getDocument(groupName: String): String? {
        if (!groups.containsKey(groupName)) {
            Log.e(TAG, "Cannot get document for group $groupName since this group does not exist.")
            return null
        }
        return groups[groupName]!!.document
    }

    fun setDocument(groupName: String, document: String) {
        if (!groups.containsKey(groupName)) {
            Log.e(TAG, "Cannot set document for group $groupName since this group does not exist.")
            return
        }
        groups[groupName]!!.document = document.removeSurrounding("\"").replace("\\\"", "\"")
    }

    fun getAllRegisteredGroups(): MutableSet<String> {
        return groups.keys
    }

    fun getGroupKey(groupName: String): SecretKey? {
        return groups[groupName]?.key
    }

    fun setGroupKey(groupName: String, aesKey: SecretKey) {
        if (!groups.containsKey(groupName)) {
            Log.e(TAG, "Cannot set key for group $groupName since this group does not exist.")
            return
        }
        groups[groupName]!!.key = aesKey
    }

    fun getMembers(groupName: String): Set<String>? {
        if (!groups.containsKey(groupName)) {
            Log.e(TAG, "Cannot get members for group $groupName since this group does not exist.")
            return null
        }
        return groups[groupName]?.members as Set<String>
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

    fun addToGroup(groupName: String, peerEmail: String) {
        if (!groups.containsKey(groupName)) {
            Log.e(TAG, "Cannot add $peerEmail to group $groupName since this group does not exist.")
            return
        }
        groups[groupName]!!.members.add(peerEmail)
    }

    fun leaveGroup(groupName: String) {
        groups.remove(groupName)
    }

    fun removePeerFromGroup(groupName: String, peerEmail: String) {
        if (!groups.containsKey(groupName)) {
            Log.e(TAG, "Cannot remove $peerEmail from group $groupName since this group does not exist.")
            return
        }
        groups[groupName]!!.members.remove(peerEmail)
    }
}

/**
 *  Contains all data associated with a group.
 *
 *  GroupId is a globally unique identifier for the group.
 */
data class GroupData(var groupId: String, val members: MutableSet<String>, var key: SecretKey?,
                     var document: String)
