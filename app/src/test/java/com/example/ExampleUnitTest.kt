package com.example

import com.example.model.SteeringDirection
import com.example.model.SteeringPacket
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testSteeringDirectionMapping() {
        assertEquals(SteeringDirection.LEFT, SteeringDirection.fromAngle(-15.0f))
        assertEquals(SteeringDirection.RIGHT, SteeringDirection.fromAngle(15.0f))
        assertEquals(SteeringDirection.CENTER, SteeringDirection.fromAngle(0.1f, deadzone = 1.5f))
    }

    @Test
    fun testSteeringPacketJsonPayload() {
        val packet = SteeringPacket(
            angle = -45.5f,
            direction = SteeringDirection.LEFT,
            normalizedX = 0.3736f,
            yRatio = 0.65f,
            isActive = true,
            timestamp = 1700000000000L
        )
        val json = packet.toJson()
        assertTrue(json.contains("\"angle\": -45.50"))
        assertTrue(json.contains("\"y_ratio\": 0.6500"))
        assertTrue(json.contains("\"is_active\": true"))
        assertTrue(json.contains("\"direction\": \"LEFT\""))
        assertTrue(json.endsWith("\n"))
    }

    @Test
    fun testFullRotationRangeEdgeReach() {
        val maxRange = 180f

        // 1. Far Left Edge Reach: -180 deg -> normX = 0.0 -> X = 0
        val leftAngle = -180f
        val normLeft = ((leftAngle - (-maxRange)) / (2f * maxRange)).coerceIn(0f, 1f)
        assertEquals(0.0f, normLeft, 0.0001f)
        val xLeft = (normLeft * 1919).toInt()
        assertEquals(0, xLeft)

        // 2. Center Neutral Reach: 0 deg -> normX = 0.5 -> X = 959 (center of 1920)
        val centerAngle = 0f
        val normCenter = ((centerAngle - (-maxRange)) / (2f * maxRange)).coerceIn(0f, 1f)
        assertEquals(0.5f, normCenter, 0.0001f)
        val xCenter = (normCenter * 1919).toInt()
        assertEquals(959, xCenter)

        // 3. Far Right Edge Reach: +180 deg -> normX = 1.0 -> X = 1919 (far right)
        val rightAngle = 180f
        val normRight = ((rightAngle - (-maxRange)) / (2f * maxRange)).coerceIn(0f, 1f)
        assertEquals(1.0f, normRight, 0.0001f)
        val xRight = (normRight * 1919).toInt()
        assertEquals(1919, xRight)
    }

    @Test
    fun testCustomizableYRatioMapping() {
        val screenHeight = 1080
        val yRatioCenter = 0.5f
        val yPixelCenter = (yRatioCenter * (screenHeight - 1)).toInt()
        assertEquals(539, yPixelCenter) // 540 middle pixel

        val yRatioTop = 0.0f
        val yPixelTop = (yRatioTop * (screenHeight - 1)).toInt()
        assertEquals(0, yPixelTop)

        val yRatioBottom = 1.0f
        val yPixelBottom = (yRatioBottom * (screenHeight - 1)).toInt()
        assertEquals(1079, yPixelBottom)
    }
}
