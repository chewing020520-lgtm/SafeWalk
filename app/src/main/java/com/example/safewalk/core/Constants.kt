package com.example.safewalk.core

object Constants {

    const val ALIGNMENT_THRESHOLD_DEG = 12f
    const val MIN_VISION_CONFIDENCE = 0.4f
    const val CAMERA_FPS = 30

    const val SENSOR_UPDATE_INTERVAL_US = 50000
    const val KALMAN_PROCESS_NOISE = 0.1f
    const val KALMAN_MEASUREMENT_NOISE = 0.3f

    const val MISALIGN_FRAME_THRESHOLD = 30
    const val DRIFT_THRESHOLD_DEG = 12f

    const val FEEDBACK_COOLDOWN_MS = 3000L
    const val VIBRATION_SHORT = 100L
    const val VIBRATION_LONG = 300L

    const val TMAP_API_KEY = "YOUR_TMAP_API_KEY_HERE"
    const val ROUTE_DEVIATION_THRESHOLD_M = 5f
    const val CROSSWALK_DETECTION_RANGE_M = 20f
    const val DESTINATION_ARRIVAL_THRESHOLD_M = 15f

    const val TRAFFIC_LIGHT_MODEL_PATH = "traffic_light_yolov8n.tflite"
    const val DETECTION_CONFIDENCE_THRESHOLD = 0.7f
    const val DETECTION_TIMEOUT_MS = 5000L

    val REQUIRED_PERMISSIONS = arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.VIBRATE
    )
}
