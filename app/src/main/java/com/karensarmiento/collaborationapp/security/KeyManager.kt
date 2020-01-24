package com.karensarmiento.collaborationapp.security

import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher


object KeyManager {

    private const val TAG = "KeyManager"
    private const val RSA_TRANSFORMATION = "RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING"
    private lateinit var personalKeys: KeyPair

    init {
        val genKeys = generateKeyPair()
        if (genKeys == null) {
            Log.e(TAG, "Public and private keys could not be generated and are null.")
        } else {
            personalKeys = genKeys
        }
    }

    fun publicKeyAsString(): String {
        return Base64.encodeToString(personalKeys.public.encoded, Base64.NO_WRAP)
    }

    fun privateKeyAsString(): String {
        return Base64.encodeToString(personalKeys.private.encoded, Base64.NO_WRAP)
    }

    fun maybeEncryptRSA(plaintext: String, publicKey: String): String? {
        var encryptedString: String? = null
        try {
            val key = stringToRSAPublicKey(publicKey)
            encryptedString = encryptFromKeyRSA(plaintext, key)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return encryptedString?.replace("(\\r|\\n)".toRegex(), "")
    }

    fun maybeDecryptRSA(ciphertext: String, privateKey: String): String? {
        var decryptedString: String? = null
        try {
            val key = stringToRSAPrivateKey(privateKey)
            decryptedString = decryptFromKeyRSA(ciphertext, key)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return decryptedString
    }

    private fun generateKeyPair(): KeyPair? {
        var kp: KeyPair? = null
        try {
            val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA)
            kpg.initialize(2048)
            kp = kpg.generateKeyPair()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return kp
    }

    private fun stringToRSAPublicKey(publicKey: String): PublicKey {
        val keyFac = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_RSA)
        // Convert the public key string into X509EncodedKeySpec format.
        val keySpec = X509EncodedKeySpec(
            Base64.decode(
                publicKey.toByteArray(),
                Base64.NO_WRAP // removes surrounding spaces.
            ) // key as bit string
        )
        // Create Android PublicKey object from X509EncodedKeySpec object.
        return keyFac.generatePublic(keySpec)
    }

    private fun stringToRSAPrivateKey(privateKey: String): PrivateKey {
        val keyFac = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_RSA)
        // Convert the public key string into PKCS8EncodedKeySpec format.
        val keySpec = PKCS8EncodedKeySpec(
            Base64.decode(
                privateKey,
                Base64.NO_WRAP
            )
        )
        // Create Android PrivateKey object from PKCS8EncodedKeySpec object.
        return keyFac.generatePrivate(keySpec)
    }

    private fun encryptFromKeyRSA(plaintext: String, publicKey: PublicKey): String {
        // Get an RSA cipher object and print the provider.
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)

        // Encrypt the plaintext and return as string.
        val encryptedBytes = cipher.doFinal(plaintext.toByteArray(charset("UTF-8")))
        return String(Base64.encode(encryptedBytes, Base64.DEFAULT))
    }


    private fun decryptFromKeyRSA(ciphertext: String, privateKey: PrivateKey): String {
        // Get an RSA cipher object and print the provider
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, privateKey)

        // Decrypt the plaintext and return as string.
        val encryptedBytes = Base64.decode(ciphertext, Base64.DEFAULT)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes)
    }
}