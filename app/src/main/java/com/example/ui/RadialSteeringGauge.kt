package com.example.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SteeringDirection
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadialSteeringGauge(
    angle: Float,
    direction: SteeringDirection,
    lateralG: Float,
    yawRate: Float,
    maxRange: Float = 180f,
    modifier: Modifier = Modifier
) {
    val animatedAngle by animateFloatAsState(
        targetValue = angle,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "hud_needle"
    )

    Box(
        modifier = modifier
            .aspectRatio(1.15f)
            .testTag("radial_steering_gauge"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) * 0.88f

            drawTelemetryDial(
                center = center,
                radius = radius,
                currentAngle = animatedAngle,
                maxRange = maxRange
            )

            drawSweepArc(
                center = center,
                radius = radius,
                currentAngle = animatedAngle
            )

            drawIndicatorNeedle(
                center = center,
                radius = radius,
                angle = animatedAngle
            )
        }

        // Center Digital Display Readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 10.dp)
        ) {
            // "• STEERING ANGLE"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(NeonCyan)
                )
                Text(
                    text = "STEERING ANGLE",
                    color = NeonCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Big Bold Number: e.g. "0.0°" or "-45.2°"
            val formattedAngle = String.format(Locale.US, "%.1f°", angle)
            Text(
                text = formattedAngle,
                color = Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Subtext: • CENTER / NEUTRAL or LEFT / RIGHT
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val dotColor = when (direction) {
                    SteeringDirection.CENTER -> NeonGreen
                    SteeringDirection.LEFT, SteeringDirection.RIGHT -> NeonCyan
                }
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Text(
                    text = when (direction) {
                        SteeringDirection.CENTER -> "CENTER / NEUTRAL"
                        SteeringDirection.LEFT -> "LEFT TILT"
                        SteeringDirection.RIGHT -> "RIGHT TILT"
                    },
                    color = when (direction) {
                        SteeringDirection.CENTER -> NeonGreen
                        else -> NeonCyan
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Live Telemetry: LAT: 0.00 G | YAW: 0°/s
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "LAT: %.2f G", lateralG),
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "|",
                    color = DarkBorder,
                    fontSize = 9.sp
                )
                Text(
                    text = String.format(Locale.US, "YAW: %.0f°/s", yawRate),
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun DrawScope.drawTelemetryDial(
    center: Offset,
    radius: Float,
    currentAngle: Float,
    maxRange: Float
) {
    val trackWidth = 2.dp.toPx()

    // Outer faint guide ring
    drawCircle(
        color = Color(0xFF131D2A),
        radius = radius,
        center = center,
        style = Stroke(width = trackWidth)
    )

    // Inner subtle ring
    drawCircle(
        color = Color(0xFF0F1823),
        radius = radius * 0.78f,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )

    val labelPaint = Paint().apply {
        color = TextPrimary.toArgb()
        textSize = radius * 0.075f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        isAntiAlias = true
    }

    val redMarkerPaint = Paint().apply {
        color = NeonRed.toArgb()
        textSize = radius * 0.075f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        isAntiAlias = true
    }

    // Draw degree tick marks
    // Top is 0° (-90° in standard polar canvas coords)
    for (deg in -180..180 step 10) {
        val canvasAngle = -90f + deg
        val rad = Math.toRadians(canvasAngle.toDouble())

        val isZero = (deg == 0)
        val isCardinals = (deg == -180 || deg == 180)
        val isMajor = (deg % 45 == 0)

        val tickLen = when {
            isZero -> radius * 0.12f
            isCardinals -> radius * 0.10f
            isMajor -> radius * 0.07f
            else -> radius * 0.035f
        }

        val tickColor = when {
            isZero -> NeonRed
            isCardinals -> NeonCyan
            isMajor -> ElectricBlue
            else -> Color(0xFF1D2F44)
        }

        val tickStroke = when {
            isZero -> 3.dp.toPx()
            isMajor -> 2.dp.toPx()
            else -> 1.dp.toPx()
        }

        val innerR = radius - tickLen
        val startX = (center.x + innerR * cos(rad)).toFloat()
        val startY = (center.y + innerR * sin(rad)).toFloat()
        val endX = (center.x + radius * cos(rad)).toFloat()
        val endY = (center.y + radius * sin(rad)).toFloat()

        drawLine(
            color = tickColor,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = tickStroke,
            cap = StrokeCap.Round
        )
    }

    // Draw Cardinal Text Labels: Top, Left, Right
    // 0° CTR at TOP CENTER
    val zeroTextY = center.y - radius - 8.dp.toPx()
    drawContext.canvas.nativeCanvas.drawText("0° CTR", center.x, zeroTextY, redMarkerPaint)

    // -180° on LEFT
    val leftTextX = center.x - radius - 18.dp.toPx()
    val leftTextY = center.y + (labelPaint.textSize * 0.35f)
    drawContext.canvas.nativeCanvas.drawText("-180°", leftTextX, leftTextY, labelPaint)

    // +180° on RIGHT
    val rightTextX = center.x + radius + 18.dp.toPx()
    val rightTextY = center.y + (labelPaint.textSize * 0.35f)
    drawContext.canvas.nativeCanvas.drawText("+180°", rightTextX, rightTextY, labelPaint)
}

private fun DrawScope.drawSweepArc(
    center: Offset,
    radius: Float,
    currentAngle: Float
) {
    if (abs(currentAngle) < 0.2f) return

    val arcWidth = radius * 0.065f
    val arcRect = Size(radius * 2f, radius * 2f)
    val topLeft = Offset(center.x - radius, center.y - radius)

    // Sweep from top (-90° in canvas coords)
    val startAngle = -90f
    val sweepAngle = currentAngle.coerceIn(-180f, 180f)

    val gradient = Brush.sweepGradient(
        0.5f to ElectricBlue,
        0.75f to NeonCyan,
        1.0f to NeonCyan,
        center = center
    )

    drawArc(
        brush = gradient,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = topLeft,
        size = arcRect,
        style = Stroke(width = arcWidth, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawIndicatorNeedle(
    center: Offset,
    radius: Float,
    angle: Float
) {
    // 0° points STRAIGHT UP AT TOP CENTER (12 o'clock) right at '0° CTR'.
    // The needle geometry is drawn pointing vertically UP towards center.y - needleLength.
    // By rotating by 'angle' around center, the needle is at TOP CENTER at 0°,
    // and turns left (counter-clockwise) on negative angles and right (clockwise) on positive angles.
    val needleAngle = angle.coerceIn(-180f, 180f)

    rotate(degrees = needleAngle, pivot = center) {
        val needleLength = radius * 1.05f
        val needleBase = radius * 0.045f

        // Bright Crimson Marker Needle (#FF3B30) pointing at TOP CENTER
        val needlePath = Path().apply {
            moveTo(center.x, center.y - needleLength)
            lineTo(center.x - needleBase, center.y - radius * 0.85f)
            lineTo(center.x + needleBase, center.y - radius * 0.85f)
            close()
        }

        drawPath(
            path = needlePath,
            color = NeonRed
        )

        // Center spine glow line
        drawLine(
            color = Color.White,
            start = Offset(center.x, center.y - needleLength),
            end = Offset(center.x, center.y - radius * 0.90f),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
