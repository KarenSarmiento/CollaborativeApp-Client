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
    fun encryptingPlaintextOutputsNonEqualCiphertext() {
        // GIVEN
        val plaintext = "This is a secret test message."
        val publicKey = KeyManager.publicKeyAsString()

        // WHEN
        val ciphertext = KeyManager.maybeEncryptRSA(plaintext, publicKey)

        // THEN
        assertNotEquals(plaintext, ciphertext)
    }

    @Test
    fun decryptingCiphertextRecoversPlaintext() {
        // GIVEN
        val plaintext = "This is a secret test message."
        val privateKey = KeyManager.privateKeyAsString()
        val publicKey = KeyManager.publicKeyAsString()
        val ciphertext = KeyManager.maybeEncryptRSA(plaintext, publicKey)

        // WHEN
        val recoveredPlaintext = KeyManager.maybeDecryptRSA(ciphertext!!, privateKey)

        // THEN
        assertEquals(plaintext, recoveredPlaintext)
    }
}


