package com.karensarmiento.collaborationapp

import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.Message


/**
 * This extension enables you to send JSONs in a Smack Message (Packet).
 * An extension element is an XML subdocument with a root element name and namespace.
 */
class JsonMessageExtension(val json: String) : ExtensionElement {
    override fun toXML(): CharSequence {
        // TODO: 1. Do we need to escape the JSON? - StringUtils.escapeForXML(json)
        // 2. How do we use the enclosing namespace?
        return "<$elementName xmlns=\"$namespace\">$json</$elementName>"
    }

    fun toPacket(): Stanza {
        val message = Message()
        message.addExtension(this)
        return message
    }

    // Returns root element name.
    override fun getElementName(): String {
        return Utils.FCM_ELEMENT_NAME
    }

    // Returns the root element name xml namespace.
    override fun getNamespace(): String {
        return Utils.FCM_NAMESPACE
    }
}
