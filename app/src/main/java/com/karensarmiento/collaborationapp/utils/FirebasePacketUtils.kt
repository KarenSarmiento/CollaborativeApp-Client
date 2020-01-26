package com.karensarmiento.collaborationapp.utils

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

    // Creating a group
    CREATE_GROUP("create_group"),
    GROUP_ID("group_id"),
    MEMBER_EMAILS("member_emails"),
    CREATE_GROUP_RESPONSE("create_group_response"),
    FAILED_EMAILS("failed_emails"),
    ADDED_TO_GROUP("added_to_group"),
    GROUP_NAME("group_name"),

    // Sending messages to device groups.
    FORWARD_MESSAGE("forward_message"),
    FORWARD_TOKEN_ID("forward_token_id"),
    JSON_UPDATE("json_update"),
    ORIGINATOR("originator"),

    // Intents
    VALUE("value")
}