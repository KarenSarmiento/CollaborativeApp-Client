package com.karensarmiento.collaborationapp

import com.karensarmiento.collaborationapp.security.KeyManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class KeyManagerTest {

    @Test
    fun encryptingPlaintextOutputsNonEqualCiphertextAES_GCM() {
        // GIVEN
        val plaintext = "This is a long secret test message. " +
                "This is a long secret test message. This is a long secret test message. " +
                "This is a long secret test message. This is a long secret test message."
        val secretKey = KeyManager.generateKeyAESGCM()

        // WHEN
        val ciphertext = KeyManager.encryptAESGCM(plaintext, secretKey)

        // THEN
        assertNotEquals(plaintext, ciphertext)
    }

    @Test
    fun decryptingCiphertextRecoversPlaintextAES_GCM() {
        // GIVEN
        val plaintext = "This is a long secret test message. " +
                "This is a long secret test message. This is a long secret test message. " +
                "This is a long secret test message. This is a long secret test message."
        val secretKey = KeyManager.generateKeyAESGCM()

        // WHEN
        val ciphertext = KeyManager.encryptAESGCM(plaintext, secretKey)
        val recoveredPlaintext = KeyManager.decryptAESGCM(ciphertext, secretKey)

        // THEN
        assertEquals(plaintext, recoveredPlaintext)
    }


    @Test
    fun encryptingPlaintextOutputsNonEqualCiphertextRSA() {
        // GIVEN
        val plaintext = "This is a secret test message."
        val publicKey = KeyManager.getPublicKeyAsString()

        // WHEN
        val ciphertext = KeyManager.maybeEncryptRSA(plaintext, publicKey)

        // THEN
        assertNotEquals(plaintext, ciphertext)
    }

    @Test
    fun decryptingCiphertextRecoversPlaintextRSA() {
        // GIVEN
        val plaintext = "This is a secret test message."
        val privateKey = KeyManager.getPrivateKeyAsString()
        val publicKey = KeyManager.getPublicKeyAsString()
        val ciphertext = KeyManager.maybeEncryptRSA(plaintext, publicKey)

        // WHEN
        val recoveredPlaintext = KeyManager.maybeDecryptRSA(ciphertext!!, privateKey)

        // THEN
        assertEquals(plaintext, recoveredPlaintext)
    }
}


