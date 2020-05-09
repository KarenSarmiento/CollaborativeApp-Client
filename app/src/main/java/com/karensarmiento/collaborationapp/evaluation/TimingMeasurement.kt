package com.karensarmiento.collaborationapp.evaluation

object Test {
    var count = 0
    var currMeasurement = TimingMeasurement()
}

class TimingMeasurement(
    var start : Long? = null,
    var localMergeFromKotlinStart : Long? = null,
    var localMergeFromJsStart : Long? = null,
    var localMergeFromJsEnd : Long? = null,
    var localMergeFromKotlinEnd : Long? = null,
    var encryptStart : Long? = null,
    var encryptEnd : Long? = null,
    var sendMessage : Long? = null,
    var receiveMessage : Long? = null,
    var decryptStart : Long? = null,
    var decryptEnd : Long? = null,
    var peerMergeFromKotlinStart : Long? = null,
    var peerMergeFromJsStart : Long? = null,
    var peerMergeFromJsEnd : Long? = null,
    var peerMergeFromKotlinEnd : Long? = null
)