package com.karensarmiento.collaborationapp

import android.content.Intent
import com.google.firebase.messaging.RemoteMessage
import com.karensarmiento.collaborationapp.messaging.FirebaseMessageReceivingService
import com.karensarmiento.collaborationapp.utils.JsonKeyword as Jk
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.ArgumentMatcher


@RunWith(MockitoJUnitRunner::class)
class UnitTestSample {

    // TODO: Can probably do as normal unit test by mocking intent.
    @Test
    fun whenReceiveJsonUpdateThenBroadcastIntent() {
        // GIVEN
        val fmrsMock = spy(FirebaseMessageReceivingService::class.java)
        doNothing().`when`(fmrsMock).sendBroadcast(any())

        val jsonUpdate = "{\"test\":\"json\"}"
        val email = "test@gmail.com"
        val messageMock = mock(RemoteMessage::class.java)
        `when`(messageMock.data).thenReturn(mapOf(
            Jk.DOWNSTREAM_TYPE.text to Jk.JSON_UPDATE.text,
            Jk.JSON_UPDATE.text to jsonUpdate,
            Jk.EMAIL.text to email
        ))

        // WHEN
        fmrsMock.onMessageReceived(messageMock)

        // THEN
        val expectedIntent = Intent()
        expectedIntent.action = Jk.JSON_UPDATE.text
        expectedIntent.putExtra(Jk.VALUE.text, jsonUpdate)
        verify(fmrsMock).sendBroadcast(argThat(IntentMatcher(expectedIntent)))
    }
}

// TODO: See if can use as anonymous class.
class IntentMatcher(private val actualIntent: Intent) : ArgumentMatcher<Intent> {
    override fun matches(mockIntent: Intent): Boolean {
        return mockIntent.filterEquals(actualIntent)
    }
}

// TODO: Create test for other downstream message types that can currently be processed.