package com.karensarmiento.collaborationapp.security

import android.util.Log

object AddressBook {

    private val TAG = "AddressBook"

    /**
     *  This contains all the details of the people that we share a group with.
     *
     *  It maps the user email to that user's other details
     */
    private val contacts: MutableMap<String, UserContact> = mutableMapOf()

    fun addContact(email: String, token: String, publicKey: String) {
        if (email in contacts) {
            Log.w(TAG, "Contact with email $email already existed. Replacing contact info.")
        }

        contacts[email] = UserContact(token, publicKey)
        Log.i(TAG, "Registered contact $email.")
    }

    fun getContactKey(email: String): String? {
        if (!contacts.containsKey(email)) {
            Log.e(TAG,"Cannot get key for user $email since they are not in the address book.")
            return null
        }
        return contacts[email]!!.publicKey
    }

    fun removeContact(email: String) {
        contacts.remove(email)
        Log.i(TAG, "Removed contact $email.")
    }

    fun updateContactToken(email: String, token: String) {
        if (!contacts.containsKey(email)) {
            Log.e(TAG,"Cannot update entry for user $email since they are not in the address book.")
            return
        }
        val publicKey = contacts[email]!!.publicKey
        contacts[email] =
            UserContact(token, publicKey)
        Log.i(TAG, "Updated token for contact $email.")
    }
}

// TODO: Remove token?
data class UserContact(val token: String, val publicKey: String)