package com.example.safewalk.utils

import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

fun Mat.drawLine(x1: Int, y1: Int, x2: Int, y2: Int, color: Scalar, thickness: Int = 2) {
    Imgproc.line(this, Point(x1.toDouble(), y1.toDouble()), Point(x2.toDouble(), y2.toDouble()), color, thickness)
}

fun Mat.drawCircle(x: Int, y: Int, radius: Int, color: Scalar, thickness: Int = -1) {
    Imgproc.circle(this, Point(x.toDouble(), y.toDouble()), radius, color, thickness)
}

fun Mat.toGrayscale(): Mat {
    val gray = Mat()
    Imgproc.cvtColor(this, gray, Imgproc.COLOR_RGBA2GRAY)
    return gray
}

fun Mat.clone(): Mat {
    val copy = Mat()
    this.copyTo(copy)
    return copy
}
