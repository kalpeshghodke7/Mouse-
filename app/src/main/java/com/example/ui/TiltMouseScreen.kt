package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SteerProfile
import com.example.model.SteeringDirection
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.abs

// Cockpit Dark Aesthetic Palette
private val CockpitBlack = Color(0xFF080B10)
private val CockpitCardBg = Color(0xFF0D1219)
private val CockpitInnerCard = Color(0xFF0A0F15)
private val CockpitBorder = Color(0xFF192433)
private val CockpitBorderCyan = Color(0xFF1E3A52)

@Composable
fun TiltMouseScreen(
    viewModel: TiltMouseViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showGuideDialog by remember { mutableStateOf(false) }
    var showScriptDialog by remember { mutableStateOf(false) }
    var showYAdjustDialog by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("COCKPIT HUD") }

    val infinitePulse = rememberInfiniteTransition(label = "cockpit_pulse")
    val pulseAlpha by infinitePulse.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitBlack)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("tiltmouse_cockpit_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP HEADER TELEMETRY BAR
            HeaderTelemetryBar(
                serverRunning = uiState.serverStatus.isRunning,
                clientConnected = uiState.serverStatus.connectedClients > 0,
                port = uiState.settings.port,
                tickHz = if (uiState.serverStatus.currentRateHz > 0) uiState.serverStatus.currentRateHz else 60.0f,
                sessionTimer = uiState.sessionTimeFormatted,
                isSignalActive = uiState.settings.isSignalActive,
                pulseAlpha = pulseAlpha,
                onToggleSignal = { viewModel.toggleSignalActive() }
            )

            Spacer(modifier = Modifier.height(3.dp))

            // 2. ENGINE REV-COUNTER / TILT DEFLECTION LED BAR
            RevCounterShiftLightBar(
                angle = uiState.packet.angle,
                maxAngle = uiState.settings.maxAngleRange
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 3. MAIN COCKPIT BODY (LEFT WING - CENTER HUD - RIGHT WING)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT WING (PADDLE LEFT, ZERO CALIBRATE, SIGNAL ACTIVE / PAUSE)
                LeftCockpitWing(
                    gear = uiState.gear,
                    isCalibrated = uiState.isCalibrated,
                    isSignalActive = uiState.settings.isSignalActive,
                    isInverted = uiState.settings.invertAxis,
                    pulseAlpha = pulseAlpha,
                    isEbrake = uiState.isEbrakeActive,
                    onShiftDown = { viewModel.shiftGear(-1) },
                    onCalibrateZero = { viewModel.calibrateZero() },
                    onToggleSignal = { viewModel.toggleSignalActive() },
                    onToggleInvert = { viewModel.toggleInvert() },
                    onToggleEbrake = { viewModel.toggleEbrake() },
                    modifier = Modifier
                        .weight(1.05f)
                        .fillMaxHeight()
                )

                Spacer(modifier = Modifier.width(8.dp))

                // CENTER HUD (DIAL HUD & DIGITAL ANGLE)
                CenterCockpitHud(
                    angle = uiState.packet.angle,
                    direction = uiState.packet.direction,
                    lateralG = uiState.packet.lateralG,
                    yawRate = uiState.packet.yawRate,
                    maxRange = uiState.settings.maxAngleRange,
                    isSignalActive = uiState.settings.isSignalActive,
                    pulseAlpha = pulseAlpha,
                    onToggleSignal = { viewModel.toggleSignalActive() },
                    modifier = Modifier
                        .weight(1.9f)
                        .fillMaxHeight()
                )

                Spacer(modifier = Modifier.width(8.dp))

                // RIGHT WING (PADDLE RIGHT, STEER PROFILE, FLASH LIGHTS, PACKET RATE)
                RightCockpitWing(
                    gear = uiState.gear,
                    steerProfile = uiState.settings.steerProfile,
                    packetsTotal = uiState.serverStatus.packetsSentTotal,
                    isFlashing = uiState.isLightsFlashing,
                    onShiftUp = { viewModel.shiftGear(1) },
                    onCycleProfile = { viewModel.cycleSteerProfile() },
                    onFlashLights = { viewModel.flashLights() },
                    modifier = Modifier
                        .weight(1.05f)
                        .fillMaxHeight()
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 4. DESKTOP CURSOR MAPPING & Y-DRAG INTERACTION BAR
            DesktopCursorMappingBar(
                normalizedX = uiState.packet.normalizedX,
                yRatio = uiState.settings.lockedYRatio,
                isSignalActive = uiState.settings.isSignalActive,
                onToggleSignal = { viewModel.toggleSignalActive() },
                onYRatioChanged = { viewModel.setLockedYRatio(it) },
                onOpenYAdjust = { showYAdjustDialog = true }
            )

            Spacer(modifier = Modifier.height(3.dp))

            // 5. BOTTOM NAVIGATION DOCK
            BottomCockpitDock(
                activeTab = activeTab,
                onSelectTab = { tab ->
                    activeTab = tab
                    if (tab == "ADB & BRIDGE") showGuideDialog = true
                    if (tab == "INPUT MAPPER") showScriptDialog = true
                    if (tab == "FFB TUNING") showYAdjustDialog = true
                }
            )
        }
    }

    // USB GUIDE DIALOG
    if (showGuideDialog) {
        UsbGuideDialog(
            port = uiState.settings.port,
            onDismiss = {
                showGuideDialog = false
                activeTab = "COCKPIT HUD"
            }
        )
    }

    // PYTHON SCRIPT DIALOG
    if (showScriptDialog) {
        PythonScriptDialog(
            port = uiState.settings.port,
            onDismiss = {
                showScriptDialog = false
                activeTab = "COCKPIT HUD"
            }
        )
    }

    // Y-AXIS ADJUST DIALOG
    if (showYAdjustDialog) {
        YAxisAdjustDialog(
            currentYRatio = uiState.settings.lockedYRatio,
            onSetRatio = { viewModel.setLockedYRatio(it) },
            onDismiss = {
                showYAdjustDialog = false
                activeTab = "COCKPIT HUD"
            }
        )
    }
}

// ---------------------------------------------------------------------------
// 1. TOP HEADER TELEMETRY BAR
// ---------------------------------------------------------------------------
@Composable
private fun HeaderTelemetryBar(
    serverRunning: Boolean,
    clientConnected: Boolean,
    port: Int,
    tickHz: Float,
    sessionTimer: String,
    isSignalActive: Boolean,
    pulseAlpha: Float,
    onToggleSignal: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(CockpitCardBg, RoundedCornerShape(6.dp))
            .border(1.dp, CockpitBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ADB Bridge Status + Interactive Bridge Toggle Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (clientConnected) NeonGreen.copy(alpha = pulseAlpha) else NeonCyan.copy(alpha = pulseAlpha))
            )
            Text(
                text = if (clientConnected) "ADB BRIDGE ACTIVE" else "ADB LISTENING",
                color = if (clientConnected) NeonGreen else NeonCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "127.0.0.1:$port",
                color = TextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )

            // Header Signal Pause/Active quick pill
            Surface(
                color = if (isSignalActive) CockpitInnerCard else Color(0xFF261805),
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSignalActive) NeonGreen.copy(alpha = 0.6f) else NeonAmber
                ),
                modifier = Modifier
                    .clickable { onToggleSignal() }
                    .testTag("header_signal_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isSignalActive) NeonGreen.copy(alpha = pulseAlpha) else NeonAmber)
                    )
                    Text(
                        text = if (isSignalActive) "LIVE" else "PAUSED",
                        color = if (isSignalActive) NeonGreen else NeonAmber,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Metrics: Port, Tick, Latency
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("PORT: $port", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(
                text = String.format(Locale.US, "TICK: %.1f Hz", tickHz),
                color = NeonGreen,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text("LATENCY: 0.4 ms", color = NeonCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }

        // Center Logging Stopwatch
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(NeonCyan))
            Text("TELEMETRY LOGGING ACTIVE", color = NeonCyan, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            Text(
                text = sessionTimer,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }

        // Device, Profile, Battery pills
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TelemetryPill(text = "DEV: PIXEL_GYRO_V1", icon = Icons.Default.Tune)
            TelemetryPill(text = "GT3_COMP", icon = Icons.Default.DirectionsCar)
            TelemetryPill(text = "98%", color = NeonGreen)
        }
    }
}

@Composable
private fun TelemetryPill(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, color: Color = NeonCyan) {
    Surface(
        color = CockpitInnerCard,
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(10.dp))
            }
            Text(text, color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

// ---------------------------------------------------------------------------
// 2. ENGINE REV-COUNTER / TILT DEFLECTION LED BAR
// ---------------------------------------------------------------------------
@Composable
private fun RevCounterShiftLightBar(angle: Float, maxAngle: Float) {
    val deflectionRatio = (abs(angle) / maxAngle).coerceIn(0f, 1f)
    val activeLeds = (deflectionRatio * 16).toInt()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .background(CockpitCardBg, RoundedCornerShape(5.dp))
            .border(1.dp, CockpitBorder, RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(NeonCyan))
            Text(
                text = "ENGINE TELEMETRY: GT3-V8 TWIN-TURBO",
                color = TextSecondary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // 16-Segment Shift LED Bar
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val colors = listOf(
                NeonCyan, NeonCyan, NeonCyan, NeonCyan,
                NeonGreen, NeonGreen, NeonGreen, NeonGreen,
                NeonAmber, NeonAmber, NeonAmber,
                NeonRed, NeonRed, NeonRed,
                Color(0xFFFF007F), Color(0xFFFF007F)
            )

            for (i in 0 until 16) {
                val isActive = i <= activeLeds
                val segmentColor = if (isActive) colors[i] else colors[i].copy(alpha = 0.15f)
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(segmentColor)
                )
            }
        }

        val rpm = 1200 + (deflectionRatio * 7200).toInt()
        Text(
            text = String.format(Locale.US, "%,d RPM", rpm),
            color = if (deflectionRatio > 0.85f) NeonRed else NeonGreen,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

// ---------------------------------------------------------------------------
// 3. LEFT COCKPIT WING
// ---------------------------------------------------------------------------
@Composable
private fun LeftCockpitWing(
    gear: String,
    isCalibrated: Boolean,
    isSignalActive: Boolean,
    isInverted: Boolean,
    pulseAlpha: Float,
    isEbrake: Boolean,
    onShiftDown: () -> Unit,
    onCalibrateZero: () -> Unit,
    onToggleSignal: () -> Unit,
    onToggleInvert: () -> Unit,
    onToggleEbrake: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 1. Paddle Left: SHIFT -
        Card(
            colors = CardDefaults.cardColors(containerColor = CockpitCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onShiftDown() }
                .testTag("paddle_shift_down")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("PADDLE LEFT", color = TextMuted, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                    Text("SHIFT -", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                }
                Surface(
                    color = CockpitInnerCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("-1", color = NeonRed, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }

        // 2. GYRO ORIGIN & ZERO CALIBRATE CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = CockpitCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(11.dp))
                        Text(
                            text = if (isCalibrated) "0.0° LOCK" else "RAW BASE",
                            color = NeonCyan,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Surface(
                        color = CockpitInnerCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isInverted) NeonAmber else CockpitBorder),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.clickable { onToggleInvert() }
                    ) {
                        Text(
                            text = if (isInverted) "AXIS: INVERTED" else "AXIS: NORMAL",
                            color = if (isInverted) NeonAmber else NeonCyan,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                // ZERO CALIBRATE Button
                Button(
                    onClick = onCalibrateZero,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CockpitInnerCard,
                        contentColor = NeonCyan
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(5.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .testTag("zero_calibrate_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Zero Calibrate", modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("❖ ZERO CALIBRATE", fontWeight = FontWeight.Black, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // 3. PROMINENT DEDICATED BRIDGE PAUSE / LIVE SIGNAL ROCKER SWITCH
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isSignalActive) CockpitCardBg else Color(0xFF261805)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isSignalActive) NeonGreen.copy(alpha = 0.8f) else NeonAmber
            ),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleSignal() }
                .testTag("bridge_pause_button")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (isSignalActive) NeonGreen.copy(alpha = pulseAlpha) else NeonAmber)
                    )
                    Column {
                        Text(
                            text = if (isSignalActive) "BRIDGE: ACTIVE" else "BRIDGE: PAUSED",
                            color = if (isSignalActive) NeonGreen else NeonAmber,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isSignalActive) "SIGNAL ON • TAP TO PAUSE" else "CURSOR FROZEN • TAP TO RESUME",
                            color = if (isSignalActive) TextMuted else NeonAmber.copy(alpha = 0.9f),
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Surface(
                    color = if (isSignalActive) NeonGreen.copy(alpha = 0.2f) else NeonAmber.copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSignalActive) NeonGreen.copy(alpha = 0.7f) else NeonAmber
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (isSignalActive) "PAUSE" else "RESUME",
                        color = if (isSignalActive) NeonGreen else NeonAmber,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 4. CENTER COCKPIT HUD
// ---------------------------------------------------------------------------
@Composable
private fun CenterCockpitHud(
    angle: Float,
    direction: SteeringDirection,
    lateralG: Float,
    yawRate: Float,
    maxRange: Float,
    isSignalActive: Boolean,
    pulseAlpha: Float,
    onToggleSignal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CockpitCardBg),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSignalActive) CockpitBorderCyan else NeonAmber.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Interactive Pill: Master Bridge Signal Status & Toggle
            Surface(
                color = if (isSignalActive) CockpitInnerCard else Color(0xFF261805),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSignalActive) NeonGreen.copy(alpha = 0.6f) else NeonAmber
                ),
                modifier = Modifier
                    .clickable { onToggleSignal() }
                    .testTag("center_bridge_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isSignalActive) NeonGreen.copy(alpha = pulseAlpha) else NeonAmber)
                    )
                    Text(
                        text = if (isSignalActive) "BRIDGE TRANSMIT: ACTIVE [TAP TO PAUSE]" else "BRIDGE SIGNAL: PAUSED [TAP TO RESUME]",
                        color = if (isSignalActive) NeonGreen else NeonAmber,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Radial Steering Gauge with In-Gauge Paused Overlay
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                RadialSteeringGauge(
                    angle = angle,
                    direction = direction,
                    lateralG = lateralG,
                    yawRate = yawRate,
                    maxRange = maxRange,
                    modifier = Modifier.fillMaxHeight()
                )

                if (!isSignalActive) {
                    Surface(
                        color = Color(0xE6100808),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 18.dp)
                            .clickable { onToggleSignal() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(NeonAmber))
                            Text(
                                "CURSOR FROZEN (SIGNAL MUTED)",
                                color = NeonAmber,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 5. RIGHT COCKPIT WING
// ---------------------------------------------------------------------------
@Composable
private fun RightCockpitWing(
    gear: String,
    steerProfile: SteerProfile,
    packetsTotal: Long,
    isFlashing: Boolean,
    onShiftUp: () -> Unit,
    onCycleProfile: () -> Unit,
    onFlashLights: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Paddle Right: SHIFT +
        Card(
            colors = CardDefaults.cardColors(containerColor = CockpitCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onShiftUp() }
                .testTag("paddle_shift_up")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = CockpitInnerCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("+1", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("PADDLE RIGHT", color = TextMuted, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                    Text("SHIFT +", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // STEER PROFILE CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = CockpitCardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(11.dp))
                        Text("STEER PROFILE", color = TextPrimary, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Surface(
                        color = CockpitInnerCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = if (steerProfile == SteerProfile.PRO_RACING_90) "90° DRIFT" else "180° PRO",
                            color = NeonCyan,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                // Profile Selector Box
                Surface(
                    color = CockpitInnerCard,
                    shape = RoundedCornerShape(5.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCycleProfile() }
                        .testTag("cycle_profile_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(steerProfile.title, color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(steerProfile.responseDesc, color = TextMuted, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                        }
                        Icon(Icons.Default.Refresh, contentDescription = "Cycle Profile", tint = TextSecondary, modifier = Modifier.size(13.dp))
                    }
                }
            }
        }

        // FLASH PASS LIGHTS CARD
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isFlashing) NeonAmber.copy(alpha = 0.25f) else CockpitCardBg
            ),
            shape = RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isFlashing) NeonAmber else CockpitBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onFlashLights() }
                .testTag("flash_lights_button")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(14.dp))
                    Column {
                        Text("PASS LIGHTS", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("HEADLIGHT FLASH", color = TextMuted, fontSize = 6.5.sp, fontFamily = FontFamily.Monospace)
                    }
                }
                Surface(
                    color = CockpitInnerCard,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder)
                ) {
                    Text(
                        text = "FLASH",
                        color = NeonAmber,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 6. DESKTOP CURSOR MAPPING & Y-DRAG INTERACTIVE BAR
// ---------------------------------------------------------------------------
@Composable
private fun DesktopCursorMappingBar(
    normalizedX: Float,
    yRatio: Float,
    isSignalActive: Boolean = true,
    onToggleSignal: () -> Unit = {},
    onYRatioChanged: (Float) -> Unit,
    onOpenYAdjust: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CockpitCardBg, RoundedCornerShape(6.dp))
            .border(1.dp, CockpitBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag("desktop_cursor_mapping_bar")
    ) {
        // Labels & Live Pixel Readout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isSignalActive) NeonCyan else NeonAmber)
                )
                Text(
                    text = "Desktop Cursor Mapping:",
                    color = TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                if (!isSignalActive) {
                    Surface(
                        color = Color(0xFF3D2005),
                        shape = RoundedCornerShape(3.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber),
                        modifier = Modifier.clickable { onToggleSignal() }
                    ) {
                        Text(
                            text = "[FROZEN / PAUSED]",
                            color = NeonAmber,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                } else {
                    Text(
                        text = "[DRAG Y-HEIGHT / TAP TO ADJUST]",
                        color = TextMuted,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Calculation assuming standard 1920x1080 display
            val xPx = (normalizedX * 1920).toInt().coerceIn(0, 1920)
            val yPx = (yRatio * 1080).toInt().coerceIn(0, 1080)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable { onOpenYAdjust() }
            ) {
                Text("X:", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text(
                    text = "${xPx}px",
                    color = NeonCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text("/ 1920px", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text(
                    text = "(Locked Y: $yPx)",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Interactive Dual-Axis Track
        // Horizontal represents X position (animated with phone tilt).
        // Vertical touch/drag allows user to set Locked Y position!
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0xFF0F1722))
                .border(1.dp, Color(0xFF1D2F44), RoundedCornerShape(7.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val deltaRatio = dragAmount.y / 80f
                        onYRatioChanged((yRatio + deltaRatio).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { onOpenYAdjust() }
                }
        ) {
            val trackWidth = maxWidth
            val safeNormX = normalizedX.coerceIn(0f, 1f)

            // Cyan filled progress bar for X
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(safeNormX.coerceAtLeast(0.01f))
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(ElectricBlue, NeonCyan)
                        )
                    )
            )

            // Center Reference Mark (X = 50% = 960px)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.5f))
            )

            // Thumb indicator at current X
            val maxThumbTravel = (trackWidth - 12.dp).coerceAtLeast(0.dp)
            val thumbStartPadding = maxThumbTravel * safeNormX

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = thumbStartPadding)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 7. BOTTOM NAVIGATION DOCK
// ---------------------------------------------------------------------------
@Composable
private fun BottomCockpitDock(
    activeTab: String,
    onSelectTab: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tab buttons
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CockpitTabButton("COCKPIT HUD", Icons.Default.Speed, activeTab == "COCKPIT HUD") { onSelectTab("COCKPIT HUD") }
            CockpitTabButton("FFB TUNING", Icons.Default.Tune, activeTab == "FFB TUNING") { onSelectTab("FFB TUNING") }
            CockpitTabButton("ADB & BRIDGE", Icons.Default.Usb, activeTab == "ADB & BRIDGE") { onSelectTab("ADB & BRIDGE") }
            CockpitTabButton("INPUT MAPPER", Icons.Default.Code, activeTab == "INPUT MAPPER") { onSelectTab("INPUT MAPPER") }
        }

        // Status badges
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Y-TILT COMPENSATION:", color = TextSecondary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Text("ACTIVE", color = NeonCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("FILTER:", color = TextSecondary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Text("KALMAN 60HZ", color = NeonGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Surface(
                color = CockpitInnerCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder),
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(Icons.Default.Fullscreen, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(10.dp))
                    Text("FULLSCREEN", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun CockpitTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) NeonCyan.copy(alpha = 0.15f) else CockpitCardBg,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) NeonCyan else CockpitBorder
        ),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .clickable { onClick() }
            .testTag("tab_${title.lowercase().replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) NeonCyan else TextSecondary,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = title,
                color = if (isSelected) NeonCyan else TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ---------------------------------------------------------------------------
// DIALOG: Y-AXIS ADJUSTMENT SLIDER
// ---------------------------------------------------------------------------
@Composable
private fun YAxisAdjustDialog(
    currentYRatio: Float,
    onSetRatio: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var ratio by remember { mutableStateOf(currentYRatio) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CockpitCardBg,
        title = {
            Text(
                text = "Customizable Y-Axis Position Control",
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Set the fixed vertical height of the cursor on your laptop monitor:",
                    color = TextPrimary,
                    fontSize = 12.sp
                )

                val yPx = (ratio * 1080).toInt()
                val yPct = (ratio * 100).toInt()

                Text(
                    text = "Locked Y: $yPx px / 1080px ($yPct%)",
                    color = NeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Slider(
                    value = ratio,
                    onValueChange = {
                        ratio = it
                        onSetRatio(it)
                    },
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = CockpitBorder
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { ratio = 0.1f; onSetRatio(0.1f) }) { Text("Top (10%)", fontSize = 10.sp, color = TextSecondary) }
                    TextButton(onClick = { ratio = 0.5f; onSetRatio(0.5f) }) { Text("Center (50%)", fontSize = 10.sp, color = NeonGreen) }
                    TextButton(onClick = { ratio = 0.9f; onSetRatio(0.9f) }) { Text("Bottom (90%)", fontSize = 10.sp, color = TextSecondary) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    )
}

// ---------------------------------------------------------------------------
// DIALOG: USB GUIDE
// ---------------------------------------------------------------------------
@Composable
private fun UsbGuideDialog(
    port: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val adbReverseCommand = "adb reverse tcp:$port tcp:$port"
    val adbForwardCommand = "adb forward tcp:$port tcp:$port"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CockpitCardBg,
        title = {
            Text(
                text = "ADB Connection & Telemetry Bridge",
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Run these commands on your laptop to establish the high-speed USB bridge:",
                    color = TextPrimary,
                    fontSize = 12.sp
                )

                AdbStepCard(
                    title = "Option 1: ADB Reverse (Recommended)",
                    command = adbReverseCommand,
                    onCopy = { copyToClipboard(context, adbReverseCommand, "Command copied!") }
                )

                AdbStepCard(
                    title = "Option 2: ADB Forward",
                    command = adbForwardCommand,
                    onCopy = { copyToClipboard(context, adbForwardCommand, "Command copied!") }
                )

                AdbStepCard(
                    title = "Start Python Gateway Script",
                    command = "python mouse_gateway.py",
                    onCopy = { copyToClipboard(context, "python mouse_gateway.py", "Command copied!") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun AdbStepCard(title: String, command: String, onCopy: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CockpitInnerCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Surface(
                color = Color.Black,
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(command, color = NeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                    IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// DIALOG: PYTHON SCRIPT
// ---------------------------------------------------------------------------
@Composable
private fun PythonScriptDialog(
    port: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val pythonCode = getGatewayPythonScript(port)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CockpitCardBg,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("mouse_gateway.py", color = NeonCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Button(
                    onClick = { copyToClipboard(context, pythonCode, "Python code copied!") },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Requires: pip install pyautogui\nRun: python mouse_gateway.py",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = pythonCode,
                        color = TextPrimary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = TextSecondary) }
        }
    )
}

private fun copyToClipboard(context: Context, text: String, toastMessage: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("TiltMouse", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
}

fun getGatewayPythonScript(port: Int = 8888): String {
    return """
#!/usr/bin/env python3
""${'"'}
=============================================================================
TiltMouse Cockpit Gateway - Laptop Host Controller
=============================================================================
Dependencies:
    pip install pyautogui

Run:
    python mouse_gateway.py
""${'"'}

import json
import socket
import subprocess
import sys
import time

try:
    import pyautogui
except ImportError:
    print("[Error] Required library 'pyautogui' is not installed.")
    print("Please run: pip install pyautogui")
    sys.exit(1)

pyautogui.PAUSE = 0
pyautogui.FAILSAFE = False

HOST = "127.0.0.1"
PORT = $port


def setup_adb(port):
    try:
        subprocess.run(["adb", "reverse", f"tcp:{port}", f"tcp:{port}"], check=False)
        subprocess.run(["adb", "forward", f"tcp:{port}", f"tcp:{port}"], check=False)
        print(f"[ADB] Bridge established for port {port}")
    except Exception as e:
        print(f"[ADB Notice] {e}")


def main():
    setup_adb(PORT)
    screen_width, screen_height = pyautogui.size()

    print("=" * 65)
    print("     TILTMOUSE TELEMETRY COCKPIT - MOUSE GATEWAY")
    print("=" * 65)
    print(f"Screen Resolution : {screen_width} x {screen_height}")
    print(f"Target Socket     : {HOST}:{PORT}")
    print("Press Ctrl+C at any time to exit.")
    print("=" * 65)

    while True:
        try:
            print(f"\n[Connecting] Linking to phone on port {PORT}...")
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(5.0)
            sock.connect((HOST, PORT))
            sock.settimeout(None)
            print(">>> [CONNECTED] Telemetry live! Tilt phone to steer.\n")

            buffer = ""
            while True:
                data = sock.recv(2048).decode("utf-8", errors="ignore")
                if not data:
                    print("\n[Disconnected] Phone closed connection.")
                    break

                buffer += data
                while "\n" in buffer:
                    line, buffer = buffer.split("\n", 1)
                    line = line.strip()
                    if not line:
                        continue

                    try:
                        packet = json.loads(line)
                        angle = float(packet.get("angle", 0.0))
                        is_active = bool(packet.get("is_active", True))
                        y_ratio = float(packet.get("y_ratio", 0.5))
                        direction = str(packet.get("direction", "CENTER"))

                        # 1. Full rotation range linear mapping:
                        # -180 deg -> X = 0
                        #    0 deg -> X = screen_width / 2
                        # +180 deg -> X = screen_width
                        clamped_angle = max(-180.0, min(180.0, angle))
                        normalized_x = (clamped_angle - (-180.0)) / (180.0 - (-180.0))
                        cursor_x = int(normalized_x * (screen_width - 1))
                        cursor_x = max(0, min(screen_width - 1, cursor_x))

                        # 2. Customizable Y-axis position:
                        cursor_y = int(y_ratio * (screen_height - 1))
                        cursor_y = max(0, min(screen_height - 1, cursor_y))

                        # 3. Live signal toggle: skip mouse movement if paused
                        if is_active:
                            pyautogui.moveTo(cursor_x, cursor_y)
                            status_str = f"ACTIVE [X: {cursor_x:4d}px | Y: {cursor_y:4d}px]"
                        else:
                            status_str = "PAUSED [SIGNAL OFF - MOUSE FREED]"

                        print(f"\rTilt: {angle:+6.1f}° [{direction:6s}] | {status_str}", end="", flush=True)

                    except json.JSONDecodeError:
                        continue

        except (socket.error, ConnectionRefusedError):
            print(f"\r[Waiting] Connecting to {HOST}:{PORT}... (Retrying in 2s)", end="")
            time.sleep(2.0)
        except KeyboardInterrupt:
            print("\n\nTiltMouse Gateway stopped by user.")
            break
        finally:
            try:
                sock.close()
            except Exception:
                pass


if __name__ == "__main__":
    main()
""".trimIndent()
}
