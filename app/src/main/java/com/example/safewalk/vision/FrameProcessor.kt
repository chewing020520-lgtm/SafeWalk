package com.example.safewalk.vision

import androidx.camera.core.ImageProxy
import org.opencv.core.CvType
import org.opencv.core.Mat

class FrameProcessor(
    private val onFrameProcessed: (AlignmentResult) -> Unit
) {

    private val analyzer = RoadGeometryAnalyzer()

    fun process(imageProxy: ImageProxy) {
        try {
            val rgbaMat = imageProxyToMat(imageProxy)
            val result = analyzer.analyze(rgbaMat)
            onFrameProcessed(result)
            rgbaMat.release()
        } finally {
            imageProxy.close()
        }
    }

    private fun imageProxyToMat(imageProxy: ImageProxy): Mat {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val mat = Mat(imageProxy.height, imageProxy.width, CvType.CV_8UC4)
        mat.put(0, 0, bytes)

        return mat
    }
}
