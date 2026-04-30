package com.example.safewalk.fusion

package com.safewalk.fusion

import com.safewalk.vision.AlignmentResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class WalkingState {
    ALIGNED,          // 정상 보행
    DRIFTING_LEFT,    // 왼쪽 쏠림
    DRIFTING_RIGHT,   // 오른쪽 쏠림
    UNCERTAIN         // 신뢰도 부족 (신호 없음)
}

data class FusedDiagnostic(
    val state: WalkingState,
    val severityDeg: Float,   // 이탈 각도
    val source: String        // "IMU", "VISION", "FUSED"
)

class AlignmentFusionEngine {

    // IMU 기반 누적 편차 (외부에서 업데이트)
    var imuDriftDeg: Float = 0f

    // 연속 이탈 감지용 타이머 (1초 기준)
    private var misalignedFrameCount = 0
    private val MISALIGN_FRAME_THRESHOLD = 30 // ~1초 @ 30fps

    private val _state = MutableStateFlow(
        FusedDiagnostic(WalkingState.ALIGNED, 0f, "INIT")
    )
    val state: StateFlow<FusedDiagnostic> = _state

    fun update(vision: AlignmentResult) {
        val diagnostic = when {
            // Vision 신뢰도가 낮으면 IMU만 사용
            vision.confidence < 0.4f -> {
                diagnosisFromImu()
            }
            // IMU와 Vision 모두 신뢰 가능 → 융합
            else -> {
                fuseImuAndVision(vision)
            }
        }

        // 1초 이상 지속될 때만 이탈로 확정
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
        // 소실점 X 위치: 0.5 = 중앙
        val vpOffset = (vision.vanishingPointX - 0.5f) * 2f  // -1~+1

        // Vision 기반 각도 추정 + IMU 가중 합산
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