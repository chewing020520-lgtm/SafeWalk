package com.example.safewalk.fusion

import com.example.safewalk.vision.AlignmentResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class WalkingState {
    ALIGNED,
    DRIFTING_LEFT,
    DRIFTING_RIGHT,
    UNCERTAIN
}

data class FusedDiagnostic(
    val state: WalkingState,
    val severityDeg: Float,
    val source: String
)

class AlignmentFusionEngine {

    var imuDriftDeg: Float = 0f

    private var misalignedFrameCount = 0
    private val MISALIGN_FRAME_THRESHOLD = 30

    private val _state = MutableStateFlow(
        FusedDiagnostic(WalkingState.ALIGNED, 0f, "INIT")
    )
    val state: StateFlow<FusedDiagnostic> = _state

    fun update(vision: AlignmentResult) {
        val diagnostic = when {
            vision.confidence < 0.4f -> {
                diagnosisFromImu()
            }
            else -> {
                fuseImuAndVision(vision)
            }
        }

        if (diagnostic.state != WalkingState.ALIGNED) {
            misalignedFrameCount++
        } else {
            misalignedFrameCount = 0
        }

        if (misalignedFrameCount >= MISALIGN_FRAME_THRESHOLD
            || diagnostic.state == WalkingState.ALIGNED) {
            _state.value = diagnostic
        }
    }

    private fun fuseImuAndVision(vision: AlignmentResult): FusedDiagnostic {
        val vpOffset = (vision.vanishingPointX - 0.5f) * 2f

        val visionWeight = vision.confidence
        val imuWeight = 1f - visionWeight
        val fusedDrift = vision.offsetAngleDeg * visionWeight + imuDriftDeg * imuWeight

        return buildDiagnostic(fusedDrift, "FUSED")
    }

    private fun diagnosisFromImu() = buildDiagnostic(imuDriftDeg, "IMU")

    private fun buildDiagnostic(driftDeg: Float, source: String): FusedDiagnostic {
        val state = when {
            driftDeg > 12f -> WalkingState.DRIFTING_RIGHT
            driftDeg < -12f -> WalkingState.DRIFTING_LEFT
            else -> WalkingState.ALIGNED
        }
        return FusedDiagnostic(state, kotlin.math.abs(driftDeg), source)
    }
}
