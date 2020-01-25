package com.karensarmiento.collaborationapp.security

import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.nio.ByteBuffer


object KeyManager {

    private const val TAG = "KeyManager"

    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val AES_KEY_SIZE = 256
    private const val AES_IV_LENGTH = 12
    private const val AES_AUTH_TAG_LENGTH = 128

    private const val RSA_TRANSFORMATION = "RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING"
    private const val RSA_KEY_SIZE = 2048
    private lateinit var personalKeys: KeyPair

    init {
        val genKeys = generateKeyPairRSA()
        if (genKeys == null) {
            Log.e(TAG, "Public and private keys could not be generated and are null.")
        } else {
            personalKeys = genKeys
        }
    }

    /**
     *  AES encryption
     */
    fun generateKeyAESGCM(): SecretKey {
        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES)
        keyGen.init(AES_KEY_SIZE)
        return keyGen.generateKey()
    }

    fun encryptAESGCM(plaintext: String, key: SecretKey): String {
        // Create an initialisation vector.
        val secureRandom = SecureRandom()
        val iv = ByteArray(AES_IV_LENGTH)
        secureRandom.nextBytes(iv)

        // Initialise cipher and encrypt.
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val paramSpec = GCMParameterSpec(AES_AUTH_TAG_LENGTH, iv) //128 bit auth tag length
        cipher.init(Cipher.ENCRYPT_MODE, key, paramSpec)
        val ciphertext = cipher.doFinal(plaintext.toByteArray(charset("UTF-8")))

        // Concatenate initialisation vector with ciphertext.
        val byteBuffer = ByteBuffer.allocate(iv.size + ciphertext.size)
        byteBuffer.put(iv)
        byteBuffer.put(ciphertext)
        val cipherMessage = byteBuffer.array()

        // Encode to string.
        return String(Base64.encode(cipherMessage, Base64.DEFAULT))
    }

    fun decryptAESGCM(ciphertext: String, key: SecretKey): String {
        // Decode from string.
        val encryptedBytes = Base64.decode(ciphertext, Base64.DEFAULT)

        // Deconstruct the message.
        val byteBuffer = ByteBuffer.wrap(encryptedBytes)
        val iv = ByteArray(AES_IV_LENGTH)
        byteBuffer.get(iv)
        val cipherText = ByteArray(byteBuffer.remaining())
        byteBuffer.get(cipherText)

        // Initialise cipher and decrypt.
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(AES_AUTH_TAG_LENGTH, iv))
        val plainText = cipher.doFinal(cipherText)

        // Encode to string.
        return String(plainText)
    }

    /**
     *  RSA encryption
     */

    fun getPublicKeyAsString(): String {
        return keyAsString(personalKeys.public)
    }

    fun getPrivateKeyAsString(): String {
        return keyAsString(personalKeys.private)
    }

    fun maybeEncryptRSA(plaintext: String, publicKey: String): String? {
        var encryptedString: String? = null
        try {
            val key = stringToPublicKeyRSA(publicKey)
            encryptedString = encryptFromKeyRSA(plaintext, key)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return encryptedString?.replace("(\\r|\\n)".toRegex(), "")
    }

    fun maybeDecryptRSA(ciphertext: String, privateKey: String): String? {
        var decryptedString: String? = null
        try {
            val key = stringToPrivateKeyRSA(privateKey)
            decryptedString = decryptFromKeyRSA(ciphertext, key)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return decryptedString
    }

    private fun generateKeyPairRSA(): KeyPair? {
        var kp: KeyPair? = null
        try {
            val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA)
            kpg.initialize(RSA_KEY_SIZE)
            kp = kpg.generateKeyPair()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return kp
    }

    private fun stringToPublicKeyRSA(publicKey: String): PublicKey {
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

    private fun stringToPrivateKeyRSA(privateKey: String): PrivateKey {
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

    /**
     *  Useful functions.
     */

    fun keyAsString(key: Key): String {
        return Base64.encodeToString(key.encoded, Base64.NO_WRAP)
    }
}