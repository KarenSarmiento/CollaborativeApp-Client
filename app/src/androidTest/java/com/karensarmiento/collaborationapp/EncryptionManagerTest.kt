package com.karensarmiento.collaborationapp

import com.karensarmiento.collaborationapp.security.EncryptionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class EncryptionManagerTest {
    @Test
    fun conversionBetweenStringAndSecretKeyIsSuccessful() {
        // GIVEN
        val secretKey = EncryptionManager.generateKeyAESGCM()

        // WHEN
        val secretKeyString = EncryptionManager.keyAsString(secretKey)
        val secretKeyRecovered = EncryptionManager.stringToKeyAESGCM(secretKeyString)

        // THEN
        assertEquals(secretKey, secretKeyRecovered)
    }

    @Test
    fun encryptingPlaintextOutputsNonEqualCiphertextAES_GCM() {
        // GIVEN
        val plaintext = "This is a long secret test message. " +
                "This is a long secret test message. This is a long secret test message. " +
                "This is a long secret test message. This is a long secret test message."
        val secretKey = EncryptionManager.generateKeyAESGCM()

        // WHEN
        val ciphertext = EncryptionManager.encryptAESGCM(plaintext, secretKey)

        // THEN
        assertNotEquals(plaintext, ciphertext)
    }

    @Test
    fun decryptingCiphertextRecoversPlaintextAES_GCM() {
        // GIVEN
        val plaintext = "This is a long secret test message. " +
                "This is a long secret test message. This is a long secret test message. " +
                "This is a long secret test message. This is a long secret test message."
        val secretKey = EncryptionManager.generateKeyAESGCM()

        // WHEN
        val ciphertext = EncryptionManager.encryptAESGCM(plaintext, secretKey)
        val recoveredPlaintext = EncryptionManager.decryptAESGCM(ciphertext, secretKey)

        // THEN
        assertEquals(plaintext, recoveredPlaintext)
    }


    @Test
    fun encryptingPlaintextOutputsNonEqualCiphertextRSA() {
        // GIVEN
        val plaintext = "This is a secret test message."
        val publicKey = EncryptionManager.getPublicKeyAsString()

        // WHEN
        val ciphertext = EncryptionManager.maybeEncryptRSA(plaintext, publicKey)

        // THEN
        assertNotEquals(plaintext, ciphertext)
    }

    @Test
    fun decryptingCiphertextRecoversPlaintextRSA() {
        // GIVEN
        val plaintext = "This is a secret test message."
        val privateKey = EncryptionManager.getPrivateKeyAsString()
        val publicKey = EncryptionManager.getPublicKeyAsString()
        val ciphertext = EncryptionManager.maybeEncryptRSA(plaintext, publicKey)

        // WHEN
        val recoveredPlaintext = EncryptionManager.maybeDecryptRSA(ciphertext!!, privateKey)

        // THEN
        assertEquals(plaintext, recoveredPlaintext)
    }
}


