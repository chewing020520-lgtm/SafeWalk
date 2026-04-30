package com.example.safewalk.vision

package com.safewalk.vision

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.*

data class AlignmentResult(
    val offsetAngleDeg: Float,    // 화면 Y축 대비 도로선 각도 편차
    val vanishingPointX: Float,   // 소실점 X 위치 (0.0=왼쪽, 1.0=오른쪽)
    val vanishingPointY: Float,   // 소실점 Y 위치
    val confidence: Float,        // 검출 신뢰도 (0.0~1.0)
    val isAligned: Boolean        // 정렬 여부
)

class RoadGeometryAnalyzer(
    private val alignmentThresholdDeg: Float = 12f,  // 하이퍼파라미터
    private val minConfidence: Float = 0.4f
) {

    // ROI: 화면 하단 40% 영역만 분석 (연산 절약)
    private fun extractRoi(frame: Mat): Mat {
        val roiStart = (frame.rows() * 0.6).toInt()
        val roi = Rect(0, roiStart, frame.cols(), frame.rows() - roiStart)
        return Mat(frame, roi)
    }

    fun analyze(rgbaFrame: Mat): AlignmentResult {
        val gray = Mat()
        val edges = Mat()
        val lines = Mat()

        try {
            // 1. 전처리
            Imgproc.cvtColor(rgbaFrame, gray, Imgproc.COLOR_RGBA2GRAY)
            val roi = extractRoi(gray)

            // 가우시안 블러로 노이즈 제거
            Imgproc.GaussianBlur(roi, roi, Size(5.0, 5.0), 0.0)

            // Canny 엣지 검출
            Imgproc.Canny(roi, edges, 50.0, 150.0)

            // 2. 확률적 허프 변환
            // threshold=80: 최소 투표 수
            // minLineLength=60: 최소 선 길이 (px)
            // maxLineGap=20: 선 사이 최대 간격
            Imgproc.HoughLinesP(
                edges, lines,
                rho = 1.0,
                theta = Math.PI / 180,
                threshold = 80,
                minLineLength = 60.0,
                maxLineGap = 20.0
            )

            if (lines.rows() == 0) {
                return AlignmentResult(0f, 0.5f, 0.5f, 0f, true)
            }

            // 3. 수직에 가까운 선만 필터링 (±30° 이내)
            val verticalLines = filterVerticalLines(lines)
            if (verticalLines.isEmpty()) {
                return AlignmentResult(0f, 0.5f, 0.5f, 0f, true)
            }

            // 4. 평균 기울기 각도 계산
            val avgAngle = computeAverageAngle(verticalLines)

            // 화면 Y축(90°) 대비 편차
            val offsetAngle = avgAngle - 90f

            // 5. 소실점 추정
            val (vpX, vpY) = estimateVanishingPoint(verticalLines, rgbaFrame)

            // 신뢰도: 검출된 선 수에 비례
            val confidence = min(1f, verticalLines.size / 10f)

            val isAligned = abs(offsetAngle) < alignmentThresholdDeg
                    && confidence >= minConfidence

            return AlignmentResult(
                offsetAngleDeg = offsetAngle,
                vanishingPointX = vpX / rgbaFrame.cols(),
                vanishingPointY = vpY / rgbaFrame.rows(),
                confidence = confidence,
                isAligned = isAligned
            )
        } finally {
            gray.release(); edges.release(); lines.release()
        }
    }

    private fun filterVerticalLines(lines: Mat): List<FloatArray> {
        val result = mutableListOf<FloatArray>()
        for (i in 0 until lines.rows()) {
            val line = lines.get(i, 0)
            val dx = line[2] - line[0]
            val dy = line[3] - line[1]
            val angleDeg = Math.toDegrees(atan2(dy, dx)).toFloat()
            // 수직선: 60°~120° 범위
            if (abs(angleDeg) in 60f..120f) {
                result.add(floatArrayOf(
                    line[0].toFloat(), line[1].toFloat(),
                    line[2].toFloat(), line[3].toFloat(),
                    angleDeg
                ))
            }
        }
        return result
    }

    private fun computeAverageAngle(lines: List<FloatArray>): Float {
        // 가중 평균: 선 길이로 가중
        var weightedSum = 0.0
        var totalWeight = 0.0
        lines.forEach { line ->
            val len = sqrt(
                (line[2]-line[0]).pow(2) + (line[3]-line[1]).pow(2)
            ).toDouble()
            weightedSum += line[4] * len
            totalWeight += len
        }
        return if (totalWeight > 0) (weightedSum / totalWeight).toFloat() else 90f
    }

    /**
     * 소실점: 선들의 교점 클러스터 중심
     * 간략화: 연장선들의 X 교점 평균을 소실점 X로 사용
     */
    private fun estimateVanishingPoint(
        lines: List<FloatArray>,
        frame: Mat
    ): Pair<Float, Float> {
        val intersectXs = mutableListOf<Float>()

        for (i in lines.indices) {
            for (j in i+1 until lines.size) {
                val ix = lineIntersectX(lines[i], lines[j])
                if (ix != null && ix in 0f..frame.cols().toFloat()) {
                    intersectXs.add(ix)
                }
            }
        }

        val vpX = if (intersectXs.isNotEmpty()) {
            intersectXs.average().toFloat()
        } else {
            frame.cols() / 2f
        }

        // Y는 화면 상단 20% 근처로 가정 (원근법상 소실점 위치)
        val vpY = frame.rows() * 0.15f
        return Pair(vpX, vpY)
    }

    private fun lineIntersectX(l1: FloatArray, l2: FloatArray): Float? {
        val (x1, y1, x2, y2) = l1
        val (x3, y3, x4, y4) = l2
        val denom = (x1-x2)*(y3-y4) - (y1-y2)*(x3-x4)
        if (abs(denom) < 1e-6) return null
        val t = ((x1-x3)*(y3-y4) - (y1-y3)*(x3-x4)) / denom
        return x1 + t*(x2-x1)
    }
}