package com.karensarmiento.collaborationapp.utils

import android.util.Log
import java.io.StringReader
import java.lang.ClassCastException
import javax.json.Json
import javax.json.JsonArray
import javax.json.JsonObject

private const val TAG = "FirebasePacketUtils"

enum class JsonKeyword(val text: String) {
    // General fields.
    UPSTREAM_TYPE("upstream_type"),
    DOWNSTREAM_TYPE("downstream_type"),
    SUCCESS("success"),
    REQUEST_ID("request_id"),
    ENC_MESSAGE("enc_message"),
    ENC_KEY("enc_key"),

    // Registering public keys.
    REGISTER_PUBLIC_KEY("register_public_key"),
    PUBLIC_KEY("public_key"),
    EMAIL("email"),

    // Requesting a notification key.
    GET_NOTIFICATION_KEY("get_notification_key"),
    NOTIFICATION_KEY("notification_key"),
    GET_NOTIFICATION_KEY_RESPONSE("get_notification_key_response"),

    // Creating a group.
    CREATE_GROUP("create_group"),
    GROUP_ID("group_id"),
    MEMBER_EMAILS("member_emails"),
    CREATE_GROUP_RESPONSE("create_group_response"),
    FAILED_EMAILS("failed_emails"),
    ADDED_TO_GROUP("added_to_group"),
    GROUP_NAME("group_name"),
    MEMBERS("members"),

    // Managing group.
    ADD_PEER_TO_GROUP("add_peer_to_group"),
    ADD_PEER_TO_GROUP_RESPONSE("add_peer_to_group_response"),
    ADDED_PEER_TO_GROUP("added_peer_to_group"),
    PEER_TOKEN("peer_token"),
    PEER_PUBLIC_KEY("peer_public_key"),

    // Peer messaging.
    FORWARD_TO_PEER("forward_to_peer"),
    PEER_TYPE("peer_type"),
    PEER_EMAIL("peer_email"),
    PEER_MESSAGE("peer_message"),
    SYMMETRIC_KEY_UPDATE("symmetric_key_update"),
    SYMMETRIC_KEY("symmetric_key"),
    // TODO: Add timestamp for when group key can change.

    // Sending messages to device groups.
    FORWARD_TO_GROUP("forward_to_group"),
    GROUP_MESSAGE("group_message"),
    ORIGINATOR("originator"),

    // Intents
    VALUE("value")
}

fun jsonStringToJsonObject(jsonString: String): JsonObject =
    Json.createReader(StringReader(jsonString)).readObject()

fun getStringOrNullFromMap(data: Map<String, String>, field: String): String? {
    val value = data[field]
    if (value == null) {
        Log.w(TAG, "Data map does not have field $field.")
    }
    return value
}

fun getStringOrNull(jsonObject: JsonObject, fieldName: String): String? {
    val field = jsonObject.getString(fieldName, null)
    if (field == null)
        Log.w(TAG, "Missing field \"$fieldName\" in packet $jsonObject")
    return field
}

fun getBooleanOrNull(jsonObject: JsonObject, fieldName: String): Boolean? {
    var field: Boolean? = null
    try {
        field = jsonObject.getBoolean(fieldName)
    } catch(e: NullPointerException ) {
        Log.w(TAG, "Missing field \"$fieldName\" in packet $jsonObject")
    } catch(e: ClassCastException) {
        Log.w(TAG, "Field \"$fieldName\" was not a boolean in packet $jsonObject")
    }
    return field
}

fun getJsonArrayOrNull(jsonObject: JsonObject, fieldName: String): JsonArray? {
    val field = jsonObject.getJsonArray(fieldName)
    if (field == null)
        Log.w(TAG, "Missing field \"$fieldName\" in packet $jsonObject")
    return field
}