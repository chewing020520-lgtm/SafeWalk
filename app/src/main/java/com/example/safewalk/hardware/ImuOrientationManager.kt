package com.example.safewalk.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.safewalk.core.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.*

data class OrientationData(
    val azimuthDeg: Float,
    val pitchDeg: Float,
    val rollDeg: Float,
    val timestamp: Long
)

class ImuOrientationManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var gravityVector = FloatArray(3)

    private val _orientation = MutableStateFlow(OrientationData(0f, 0f, 0f, 0L))
    val orientation: StateFlow<OrientationData> = _orientation

    private var kalmanAzimuthSin = 0f
    private var kalmanAzimuthCos = 1f
    private var kalmanP = 1f

    fun start() {
        rotationSensor?.let {
            sensorManager.registerListener(
                this, it,
                Constants.SENSOR_UPDATE_INTERVAL_US,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
        gravitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                val remappedMatrix = adaptiveCoordinateRemap(rotationMatrix, gravityVector)

                SensorManager.getOrientation(remappedMatrix, orientationAngles)

                val rawAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                val azimuth = applyCircularKalmanFilter(rawAzimuth)
                val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

                _orientation.value = OrientationData(
                    azimuthDeg = normalizeAngle(azimuth),
                    pitchDeg = pitch,
                    rollDeg = roll,
                    timestamp = System.currentTimeMillis()
                )
            }
            Sensor.TYPE_GRAVITY -> {
                gravityVector = event.values.clone()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun adaptiveCoordinateRemap(matrix: FloatArray, gravity: FloatArray): FloatArray {
        val remapped = FloatArray(9)

        val isVertical = abs(gravity[2]) > 8.0f

        if (isVertical) {
            SensorManager.remapCoordinateSystem(
                matrix,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Z,
                remapped
            )
        } else {
            matrix.copyInto(remapped)
        }

        return remapped
    }

    private fun applyCircularKalmanFilter(measurementDeg: Float): Float {
        val measurementRad = Math.toRadians(measurementDeg.toDouble()).toFloat()
        val zSin = sin(measurementRad)
        val zCos = cos(measurementRad)

        val K = kalmanP / (kalmanP + Constants.KALMAN_MEASUREMENT_NOISE)

        kalmanAzimuthSin += K * (zSin - kalmanAzimuthSin)
        kalmanAzimuthCos += K * (zCos - kalmanAzimuthCos)

        kalmanP = (1 - K) * kalmanP + Constants.KALMAN_PROCESS_NOISE

        val norm = sqrt(kalmanAzimuthSin * kalmanAzimuthSin + kalmanAzimuthCos * kalmanAzimuthCos)
        kalmanAzimuthSin /= norm
        kalmanAzimuthCos /= norm

        return Math.toDegrees(atan2(kalmanAzimuthSin.toDouble(), kalmanAzimuthCos.toDouble())).toFloat()
    }

    private fun normalizeAngle(deg: Float): Float {
        var normalized = deg % 360f
        if (normalized < 0) normalized += 360f
        return normalized
    }
}
