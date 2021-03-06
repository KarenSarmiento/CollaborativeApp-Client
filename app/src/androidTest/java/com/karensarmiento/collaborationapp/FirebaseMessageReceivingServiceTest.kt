package com.karensarmiento.collaborationapp
//
//import android.content.Intent
//import com.google.firebase.messaging.RemoteMessage
//import com.karensarmiento.collaborationapp.messaging.FirebaseMessageReceivingService
//import com.karensarmiento.collaborationapp.utils.JsonKeyword as Jk
//import org.junit.Test
//import org.junit.runner.RunWith
//import org.mockito.ArgumentMatchers.any
//import org.mockito.Mockito.*
//import org.mockito.junit.MockitoJUnitRunner
//import org.mockito.ArgumentMatcher
//
//
//@RunWith(MockitoJUnitRunner::class)
//class FirebaseMessageReceivingServiceTest {
//
//    @Test
//    fun whenReceiveJsonUpdateThenBroadcastIntent() {
//        // GIVEN
//        val fmrsMock = spy(FirebaseMessageReceivingService::class.java)
//        doNothing().`when`(fmrsMock).sendBroadcast(any())
//
//        val jsonUpdate = "{\"test\":\"json\"}"
//        val email = "test@gmail.com"
//        val messageMock = mock(RemoteMessage::class.java)
//        `when`(messageMock.data).thenReturn(mapOf(
//            Jk.DOWNSTREAM_TYPE.text to Jk.GROUP_MESSAGE.text,
//            Jk.GROUP_MESSAGE.text to jsonUpdate,
//            Jk.EMAIL.text to email
//        ))
//
//        // WHEN
//        fmrsMock.onMessageReceived(messageMock)
//
//        // THEN
//        val expectedIntent = Intent()
//        expectedIntent.action = Jk.GROUP_MESSAGE.text
//        expectedIntent.putExtra(Jk.VALUE.text, jsonUpdate)
//        verify(fmrsMock /*as ContextWrapper*/).sendBroadcast(argThat(IntentMatcher(expectedIntent)))
//    }
//}
//
//class IntentMatcher(private val actualIntent: Intent) : ArgumentMatcher<Intent> {
//    override fun matches(mockIntent: Intent): Boolean {
//        return mockIntent.filterEquals(actualIntent)
//    }
//}
//
//// TODO: Create test for other downstream message types that can currently be processed.