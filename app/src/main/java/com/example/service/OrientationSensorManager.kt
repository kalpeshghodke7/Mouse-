package com.example.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.model.ControllerSettings
import com.example.model.SteeringDirection
import com.example.model.SteeringPacket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sin

class OrientationSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private var rotationVectorSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var gyroscopeSensor: Sensor? = null

    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val accelerometerValues = FloatArray(3)

    private val _steeringState = MutableStateFlow(
        SteeringPacket(
            angle = 0f,
            direction = SteeringDirection.CENTER,
            normalizedX = 0.5f,
            yRatio = 0.5f,
            isActive = true
        )
    )
    val steeringState: StateFlow<SteeringPacket> = _steeringState.asStateFlow()

    private val _sensorAvailable = MutableStateFlow(true)
    val sensorAvailable: StateFlow<Boolean> = _sensorAvailable.asStateFlow()

    var settings: ControllerSettings = ControllerSettings()

    // Calibration baseline offset in degrees
    private var zeroOffsetDegrees: Float = 0f
    private var smoothedAngle: Float = 0f
    private var currentRawAngle: Float = 0f

    private var lastAngleTime = System.currentTimeMillis()
    private var lastAngleValue = 0f
    private var currentYawRate = 0f

    init {
        rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationVectorSensor == null) {
            rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        }
        accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscopeSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        _sensorAvailable.value = (rotationVectorSensor != null || accelerometerSensor != null)
    }

    fun startListening() {
        if (sensorManager == null) return

        rotationVectorSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        accelerometerSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscopeSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    fun calibrateZero() {
        zeroOffsetDegrees = currentRawAngle
        smoothedAngle = 0f
        updatePacket(0f)
    }

    fun resetCalibration() {
        zeroOffsetDegrees = 0f
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        var rawAngle = 0f
        var calculated = false

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR ||
            event.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR
        ) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            // Remap coordinates for Landscape orientation:
            // Device X -> Screen -Y, Device Y -> Screen X
            SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_Y,
                SensorManager.AXIS_MINUS_X,
                remappedMatrix
            )

            // remappedMatrix[6] is earth UP along screen horizontal X
            // remappedMatrix[7] is earth UP along screen vertical Y
            val ux = remappedMatrix[6].toDouble()
            val uy = remappedMatrix[7].toDouble()

            // Negation ensures correct steering direction:
            // Tilting LEFT (counter-clockwise) produces a NEGATIVE angle (-1° to -180°) -> Direction.LEFT
            // Tilting RIGHT (clockwise) produces a POSITIVE angle (+1° to +180°) -> Direction.RIGHT
            rawAngle = -Math.toDegrees(kotlin.math.atan2(ux, uy)).toFloat()
            calculated = true
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerValues, 0, 3)
            if (rotationVectorSensor == null) {
                val ax = accelerometerValues[0].toDouble()
                val ay = accelerometerValues[1].toDouble()
                rawAngle = -Math.toDegrees(kotlin.math.atan2(ay, ax)).toFloat()
                calculated = true
            }
        } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            // Gyroscope yaw/roll rate around screen normal
            currentYawRate = Math.toDegrees(abs(event.values[2]).toDouble()).toFloat()
        }

        if (calculated) {
            currentRawAngle = rawAngle

            // Calculate calibrated angle with modular wrapping around [-180, 180]
            var angle = normalizeAngle(rawAngle - zeroOffsetDegrees)

            // Invert if user toggled invert
            if (settings.invertAxis) {
                angle = -angle
            }

            // Exponential moving average filter with shortest angle difference
            val alpha = settings.smoothingFactor.coerceIn(0f, 0.85f)
            val diff = shortestAngleDifference(angle, smoothedAngle)
            smoothedAngle = normalizeAngle(smoothedAngle + (1f - alpha) * diff)

            // Clamp strictly within the configured max angle range (e.g. ±180° or ±90°)
            val maxRange = settings.maxAngleRange
            val clampedAngle = smoothedAngle.coerceIn(-maxRange, maxRange)

            // Calculate yaw rate if gyro wasn't registered
            val now = System.currentTimeMillis()
            val dt = (now - lastAngleTime).coerceAtLeast(1) / 1000f
            if (gyroscopeSensor == null) {
                currentYawRate = abs(clampedAngle - lastAngleValue) / dt
            }
            lastAngleTime = now
            lastAngleValue = clampedAngle

            updatePacket(clampedAngle)
        }
    }

    private fun updatePacket(angle: Float) {
        val maxRange = settings.maxAngleRange

        // Full edge-to-edge linear mapping without dead-zone truncation:
        // -maxRange -> normX = 0.0 (Far left)
        //         0 -> normX = 0.5 (Exact center)
        // +maxRange -> normX = 1.0 (Far right)
        val normX = ((angle - (-maxRange)) / (2f * maxRange)).coerceIn(0f, 1f)

        val direction = SteeringDirection.fromAngle(angle, settings.deadzoneDegrees)

        // Calculate simulated lateral G based on steering deflection
        val lateralG = abs(sin(Math.toRadians(angle.toDouble()))).toFloat()

        _steeringState.value = SteeringPacket(
            angle = angle,
            direction = direction,
            normalizedX = normX,
            yRatio = settings.lockedYRatio,
            isActive = settings.isSignalActive,
            lateralG = lateralG,
            yawRate = currentYawRate
        )
    }

    private fun normalizeAngle(angle: Float): Float {
        var a = angle
        while (a > 180f) a -= 360f
        while (a < -180f) a += 360f
        return a
    }

    private fun shortestAngleDifference(target: Float, current: Float): Float {
        var diff = target - current
        while (diff > 180f) diff -= 360f
        while (diff < -180f) diff += 360f
        return diff
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
