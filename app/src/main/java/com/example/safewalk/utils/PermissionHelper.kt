package com.example.safewalk.utils

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.safewalk.core.Constants

class PermissionHelper(private val activity: Activity) {

    companion object {
        private const val REQUEST_CODE = 1001
    }

    fun hasRequiredPermissions(): Boolean {
        return Constants.REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(
                activity,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestPermissions() {
        ActivityCompat.requestPermissions(
            activity,
            Constants.REQUIRED_PERMISSIONS,
            REQUEST_CODE
        )
    }
}
