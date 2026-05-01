package com.example.safewalk.vision

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.*

data class AlignmentResult(
    val offsetAngleDeg: Float,
    val vanishingPointX: Float,
    val vanishingPointY: Float,
    val confidence: Float,
    val isAligned: Boolean
)

class RoadGeometryAnalyzer(
    private val alignmentThresholdDeg: Float = 12f,
    private val minConfidence: Float = 0.4f
) {

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
            Imgproc.cvtColor(rgbaFrame, gray, Imgproc.COLOR_RGBA2GRAY)
            val roi = extractRoi(gray)

            Imgproc.GaussianBlur(roi, roi, Size(5.0, 5.0), 0.0)

            Imgproc.Canny(roi, edges, 50.0, 150.0)

            Imgproc.HoughLinesP(
                edges,
                lines,
                1.0,
                Math.PI / 180,
                80,
                60.0,
                20.0
            )

            if (lines.rows() == 0) {
                return AlignmentResult(0f, 0.5f, 0.5f, 0f, true)
            }

            val verticalLines = filterVerticalLines(lines)
            if (verticalLines.isEmpty()) {
                return AlignmentResult(0f, 0.5f, 0.5f, 0f, true)
            }

            val avgAngle = computeAverageAngle(verticalLines)

            val offsetAngle = avgAngle - 90f

            val (vpX, vpY) = estimateVanishingPoint(verticalLines, rgbaFrame)

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
            gray.release()
            edges.release()
            lines.release()
        }
    }

    private fun filterVerticalLines(lines: Mat): List<FloatArray> {
        val result = mutableListOf<FloatArray>()
        for (i in 0 until lines.rows()) {
            val line = lines.get(i, 0)
            val dx = line[2] - line[0]
            val dy = line[3] - line[1]
            val angleDeg = Math.toDegrees(atan2(dy, dx)).toFloat()
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
