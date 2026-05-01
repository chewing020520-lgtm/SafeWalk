package com.example.safewalk.core

import android.app.Application
import org.opencv.android.OpenCVLoader

class SafeWalkApp : Application() {
    override fun onCreate() {
        super.onCreate()

        if (!OpenCVLoader.initDebug()) {
            android.util.Log.e("SafeWalk", "OpenCV initialization failed")
        } else {
            android.util.Log.d("SafeWalk", "OpenCV loaded successfully")
        }
    }
}
