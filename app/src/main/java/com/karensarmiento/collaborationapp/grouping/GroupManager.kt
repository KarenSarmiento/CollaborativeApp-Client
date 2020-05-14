package com.karensarmiento.collaborationapp.grouping

import android.util.Log
import java.util.concurrent.Semaphore
import javax.crypto.SecretKey


/**
 * Methods are synchronised to ensure that the containsKey check is still valid until later in the
 * function.
 */
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

    @Synchronized fun getCurrentGroupKey(): SecretKey? {
        return groups[currentGroup]?.key
    }

    @Synchronized fun maybeSetCurrentGroup(groupName: String): Boolean {
        if (groups.containsKey(groupName)) {
            currentGroup = groupName
            return true
        }
        Log.e(TAG, "Attempted to set current group as an unregistered group.")
        return false
    }

    @Synchronized fun groupId(groupName: String): String? {
        return groups[groupName]?.groupId
    }

    @Synchronized fun registerGroup(
        groupName: String, groupId: String, memberEmails: MutableSet<String>, document: String,
        initDocument: String, key: SecretKey? = null) {
        if (groupName in groups) {
            Log.e(TAG, "Cannot register group with name $groupName since group already exists.")
            return
        }
        groups[groupName] = GroupData(groupId, memberEmails, key, document, initDocument,
            mutableSetOf(), Semaphore(1)
        )
        Log.i(TAG, "Registered $groupName to groupId $groupId.")
    }

    @Synchronized fun getDocument(groupName: String): String? {
        if (!groups.containsKey(groupName)) {
            Log.e(TAG, "Cannot get document for group $groupName since this group does not exist.")
            return null
        }
        return groups[groupName]!!.document
    }

    @Synchronized fun setDocument(groupName: String, document: String) {
        if (!groups.containsKey(groupName)) {
            Log.e(TAG, "Cannot set document for group $groupName since this group does not exist.")
            return
        }
        groups[groupName]!!.document = document.removeSurrounding("\"").replace("\\\"", "\"")
    }

    @Synchronized fun getInitDocument(groupName: String): String? {
        if (!groups.containsKey(groupName)) {
            Log.e(TAG, "Cannot get init document for group $groupName since this group does not exist.")
            return null
        }
        return groups[groupName]!!.initDocument
    }

    @Synchronized fun setInitDocument(groupName: String, document: String) {
        if (!groups.containsKey(groupName)) {
            Log.e(TAG, "Cannot set init document for group $groupName since this group does not exist.")
            return
        }
        groups[groupName]!!.initDocument = document.removeSurrounding("\"").replace("\\\"", "\"")
    }

    @Synchronized fun getAllRegisteredGroups(): MutableSet<String> {
        return groups.keys
    }

    @Synchronized fun getGroupKey(groupName: String): SecretKey? {
        return groups[groupName]?.key
    }

    @Synchronized fun setGroupKey(groupName: String, aesKey: SecretKey) {
        if (!groups.containsKey(groupName)) {
            Log.e(TAG, "Cannot set key for group $groupName since this group does not exist.")
            return
        }
        groups[groupName]!!.key = aesKey
    }

    @Synchronized fun getMembers(groupName: String): Set<String>? {
        if (!groups.containsKey(groupName)) {
            Log.e(TAG, "Cannot get members for group $groupName since this group does not exist.")
            return null
        }
        return groups[groupName]?.members as Set<String>
    }

    // TODO: Once eliminated group name, remove this method. Avoid linear search!!
    @Synchronized fun groupName(groupId: String): String? {
        for ((groupName, groupData) in groups) {
            if (groupData.groupId == groupId)
                return groupName
        }
        Log.i(TAG, "No group name was found for group id $groupId")
        return null
    }

    @Synchronized fun addToGroup(groupName: String, peerEmail: String) {
        if (!groups.containsKey(groupName)) {
            Log.e(TAG, "Cannot add $peerEmail to group $groupName since this group does not exist.")
            return
        }
        groups[groupName]!!.members.add(peerEmail)
    }

    @Synchronized fun leaveGroup(groupName: String) {
        groups.remove(groupName)
    }

    @Synchronized fun removePeerFromGroup(groupName: String, peerEmail: String) {
        if (!groups.containsKey(groupName)) {
            Log.e(TAG, "Cannot remove $peerEmail from group $groupName since this group does not exist.")
            return
        }
        groups[groupName]!!.members.remove(peerEmail)
    }

    @Synchronized fun isMember(groupName: String, email: String): Boolean {
        if (!groups.containsKey(groupName)) {
            Log.e(TAG, "Group with name $groupName does not exist.")
            return false
        }
        val member = email in groups[groupName]!!.members
        if (!member) {
            Log.i(TAG, "$email is not a member of group $groupName.")
        }
        return member
    }

    @Synchronized fun addChange(groupName: String, change: String) {
        if (!groups.containsKey(groupName)) {
            Log.e(TAG, "Group with name $groupName does not exist.")
            return
        }
        groups[groupName]!!.changes.add(change)
    }

    @Synchronized fun getChanges(groupName: String): MutableSet<String>? {
        if (!groups.containsKey(groupName)) {
            Log.e(TAG, "Group with name $groupName does not exist.")
            return null
        }
        return groups[groupName]!!.changes
    }

    fun lock(groupName: String) {
        Log.i(TAG, "LOCK 1")
        if (!groups.containsKey(groupName)) {
            Log.e(TAG, "Group with name $groupName does not exist.")
            return
        }
        groups[groupName]!!.lock.acquire()
        Log.i(TAG, "LOCK 2: ${groups[groupName]!!.lock.availablePermits()}")
    }

    fun unlock(groupName: String) {
        Log.i(TAG, "UNLOCK 1")
        if (!groups.containsKey(groupName)) {
            Log.e(TAG, "Group with name $groupName does not exist.")
            return
        }
        groups[groupName]!!.lock.release()
        Log.i(TAG, "UNLOCK 2: ${groups[groupName]!!.lock.availablePermits()}")
    }
}

/**
 *  Contains all data associated with a group.
 *
 *  - GroupId is a globally unique identifier for the group.
 *  - initDocument contains the first document string that has been stored for this group.
 *  - changes are a set of all the changes that have been applied since initDocument was made.
 *  - document is the current document string.
 *  - a lock that must be acquired in order to read or write to the document.
 */
data class GroupData(var groupId: String, val members: MutableSet<String>, var key: SecretKey?,
                     var document: String, var initDocument: String, val changes: MutableSet<String>,
                     val lock: Semaphore)
