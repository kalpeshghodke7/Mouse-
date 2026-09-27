package com.example.model

import java.util.Locale

enum class SteeringDirection(val label: String) {
    LEFT("LEFT"),
    CENTER("CENTER / NEUTRAL"),
    RIGHT("RIGHT");

    companion object {
        fun fromAngle(angle: Float, deadzone: Float = 1.5f): SteeringDirection {
            return when {
                angle < -deadzone -> LEFT
                angle > deadzone -> RIGHT
                else -> CENTER
            }
        }
    }
}

data class SteeringPacket(
    val angle: Float,
    val direction: SteeringDirection,
    val normalizedX: Float, // 0.0 (far left) to 1.0 (far right)
    val yRatio: Float = 0.5f, // 0.0 (top) to 1.0 (bottom), default 0.5 (center)
    val isActive: Boolean = true, // signal toggle (active vs paused)
    val lateralG: Float = 0.0f,
    val yawRate: Float = 0.0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): String {
        return String.format(
            Locale.US,
            "{\"angle\": %.2f, \"y_ratio\": %.4f, \"is_active\": %b, \"direction\": \"%s\", \"norm_x\": %.4f, \"ts\": %d}\n",
            angle,
            yRatio,
            isActive,
            direction.name,
            normalizedX,
            timestamp
        )
    }
}

enum class SteerProfile(val title: String, val maxRange: Float, val responseDesc: String) {
    SIM_LINEAR_GT("SIM LINEAR GT", 180f, "Response: 1:1 Smooth Curve"),
    PRO_RACING_90("PRO DRIFT 90°", 90f, "Response: Ultra-Fast Precision"),
    FULL_WHEEL_360("SIM FULL 360°", 180f, "Response: Full Lock-to-Lock");
}

data class ControllerSettings(
    val port: Int = 8888,
    val invertAxis: Boolean = false,
    val deadzoneDegrees: Float = 1.5f,
    val maxAngleRange: Float = 180f,
    val smoothingFactor: Float = 0.20f,
    val lockedYRatio: Float = 0.5f,
    val isSignalActive: Boolean = true,
    val steerProfile: SteerProfile = SteerProfile.SIM_LINEAR_GT
)
