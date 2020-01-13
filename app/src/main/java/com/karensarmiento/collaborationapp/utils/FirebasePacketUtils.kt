package com.karensarmiento.collaborationapp.utils

enum class JsonKeyword(val text: String) {
    // General fields.
    UPSTREAM_TYPE("upstream_type"),

    // Registering public keys.
    REGISTER_PUBLIC_KEY("register_public_key"), PUBLIC_KEY("public_key"), EMAIL("email"),

    // Sending messages to device groups.
    FORWARD_MESSAGE("forward_message"),
    FORWARD_TOKEN_ID("forward_token_id"),
    JSON_UPDATE("json_update")
}