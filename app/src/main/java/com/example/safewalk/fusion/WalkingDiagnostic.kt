package com.example.safewalk.fusion

data class WalkingDiagnostic(
    val state: WalkingState,
    val severityDeg: Float,
    val imuDrift: Float,
    val visionOffset: Float,
    val visionConfidence: Float,
    val timestamp: Long
)

fun createAlignedDiagnostic() = WalkingDiagnostic(
    state = WalkingState.ALIGNED,
    severityDeg = 0f,
    imuDrift = 0f,
    visionOffset = 0f,
    visionConfidence = 0f,
    timestamp = System.currentTimeMillis()
)
