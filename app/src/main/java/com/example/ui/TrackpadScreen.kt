package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionState
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

private val DarkTrackpadBg = Color(0xFF090D13)
private val GlassSurfaceBg = Color(0xFF101722)
private val TopBarBg = Color(0xFF0D131C)
private val ButtonSurface = Color(0xFF141E2B)

@Composable
fun TrackpadScreen(
    viewModel: TrackpadViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isMediaVisible by viewModel.isMediaBarVisible.collectAsState()
    val isKeyboardVisible by viewModel.isKeyboardVisible.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var keyboardInputText by remember { mutableStateOf("") }

    // Server name from connection state
    val serverName = when (val s = connectionState) {
        is ConnectionState.Connected -> s.serverName
        is ConnectionState.Connecting -> s.serverName
        else -> "PC Connected"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkTrackpadBg)
            .testTag("trackpad_main_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. DISCREET TOP BAR
            DiscreetTopBar(
                serverName = serverName,
                isMediaOpen = isMediaVisible,
                isKeyboardOpen = isKeyboardVisible,
                onToggleMedia = { viewModel.toggleMediaBar() },
                onToggleKeyboard = { viewModel.toggleKeyboard() },
                onOpenSettings = { showSettingsDialog = true },
                onDisconnect = { viewModel.disconnect() }
            )

            // 2. COLLAPSIBLE MEDIA CONTROLS ROW
            AnimatedVisibility(visible = isMediaVisible) {
                MediaControlsBar(
                    onMediaAction = { viewModel.sendMedia(it) }
                )
            }

            // 3. COLLAPSIBLE VIRTUAL KEYBOARD TYPING BAR
            AnimatedVisibility(visible = isKeyboardVisible) {
                KeyboardInputBar(
                    text = keyboardInputText,
                    onTextChanged = {
                        keyboardInputText = it
                    },
                    onSend = {
                        if (keyboardInputText.isNotEmpty()) {
                            viewModel.sendText(keyboardInputText)
                            keyboardInputText = ""
                        }
                    },
                    onKeyAction = { key ->
                        viewModel.sendKey(key)
                    }
                )
            }

            // 4. MAIN GESTURE TRACKPAD SURFACE (FULL-SCREEN MINIMALIST)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassSurfaceBg)
                    .border(1.dp, Color(0xFF1E2B3C), RoundedCornerShape(16.dp))
                    .testTag("trackpad_touch_surface")
                    .pointerInput(Unit) {
                        // Multi-touch & Gesture detection
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val count = event.changes.size

                                if (count == 1) {
                                    val change = event.changes[0]
                                    if (change.pressed && change.previousPressed) {
                                        val delta = change.position - change.previousPosition
                                        if (delta != Offset.Zero) {
                                            viewModel.onMove(delta.x, delta.y)
                                            change.consume()
                                        }
                                    }
                                } else if (count >= 2) {
                                    // 2-finger scroll
                                    val c1 = event.changes[0]
                                    val c2 = event.changes[1]
                                    if (c1.pressed && c2.pressed) {
                                        val dy1 = c1.position.y - c1.previousPosition.y
                                        val dy2 = c2.position.y - c2.previousPosition.y
                                        val avgDy = (dy1 + dy2) / 2f
                                        if (kotlin.math.abs(avgDy) > 0.5f) {
                                            viewModel.onScroll(avgDy)
                                            c1.consume()
                                            c2.consume()
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                viewModel.onClick("l") // 1-finger tap = Left Click
                            },
                            onDoubleTap = {
                                viewModel.onClick("d") // Double tap = Double Click
                            },
                            onLongPress = {
                                viewModel.onMouseDown("l") // Long press = Drag start
                            }
                        )
                    }
            ) {
                // Subtle centered guidance watermark
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16202E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(0xFF2C3E55),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "TOUCHPAD SURFACE",
                        color = Color(0xFF2C3E55),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "1-Finger: Move / Tap: Left Click\n2-Fingers: Scroll / Tap: Right Click",
                        color = Color(0xFF253345),
                        fontSize = 10.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }
            }

            // 5. ERGONOMIC BOTTOM MOUSE BUTTONS (OPTIONAL)
            if (settings.showPhysicalButtons) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // LEFT CLICK
                    Surface(
                        color = ButtonSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                            .clickable { viewModel.onClick("l") }
                            .testTag("left_click_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "LEFT CLICK",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // RIGHT CLICK
                    Surface(
                        color = ButtonSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { viewModel.onClick("r") }
                            .testTag("right_click_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "RIGHT CLICK",
                                color = NeonCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }

    // SETTINGS DIALOG
    if (showSettingsDialog) {
        TrackpadSettingsDialog(
            currentSettings = settings,
            onSave = { updated ->
                viewModel.updateSettings(updated)
                showSettingsDialog = false
            },
            onForgetDevice = {
                showSettingsDialog = false
                viewModel.forgetSavedDevice()
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

// ---------------------------------------------------------------------------
// 1. DISCREET TOP BAR
// ---------------------------------------------------------------------------
@Composable
private fun DiscreetTopBar(
    serverName: String,
    isMediaOpen: Boolean,
    isKeyboardOpen: Boolean,
    onToggleMedia: () -> Unit,
    onToggleKeyboard: () -> Unit,
    onOpenSettings: () -> Unit,
    onDisconnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(TopBarBg)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Connection status pill
        Surface(
            color = Color(0xFF131D28),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(NeonGreen))
                Text(
                    text = serverName,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Action Icons: Media, Keyboard, Settings, Disconnect
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onToggleMedia, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Media Controls",
                    tint = if (isMediaOpen) NeonCyan else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onToggleKeyboard, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Virtual Keyboard",
                    tint = if (isKeyboardOpen) NeonCyan else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onOpenSettings, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onDisconnect, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Disconnect",
                    tint = NeonRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 2. MEDIA CONTROLS ROW
// ---------------------------------------------------------------------------
@Composable
private fun MediaControlsBar(
    onMediaAction: (String) -> Unit
) {
    Surface(
        color = Color(0xFF0F1622),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onMediaAction("prev") }, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.FastRewind, contentDescription = "Previous", tint = Color.White)
            }
            IconButton(onClick = { onMediaAction("play_pause") }, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = NeonCyan, modifier = Modifier.size(26.dp))
            }
            IconButton(onClick = { onMediaAction("next") }, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.FastForward, contentDescription = "Next", tint = Color.White)
            }
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(DarkBorder))
            IconButton(onClick = { onMediaAction("mute") }, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.VolumeMute, contentDescription = "Mute", tint = NeonAmber)
            }
            IconButton(onClick = { onMediaAction("vol_down") }, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.VolumeDown, contentDescription = "Vol -", tint = Color.White)
            }
            IconButton(onClick = { onMediaAction("vol_up") }, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.VolumeUp, contentDescription = "Vol +", tint = Color.White)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 3. VIRTUAL KEYBOARD TYPING BAR
// ---------------------------------------------------------------------------
@Composable
private fun KeyboardInputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onKeyAction: (String) -> Unit
) {
    Surface(
        color = Color(0xFF0F1622),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder)
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    placeholder = { Text("Type here to send to PC...", fontSize = 12.sp, color = TextMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = onSend,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Send", fontWeight = FontWeight.Bold)
                }
            }

            // Quick Special Keys (Enter, Backspace, Space, Esc)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SpecialKeyButton("Enter", modifier = Modifier.weight(1f)) { onKeyAction("enter") }
                SpecialKeyButton("⌫ Backspace", modifier = Modifier.weight(1f)) { onKeyAction("backspace") }
                SpecialKeyButton("Space", modifier = Modifier.weight(1f)) { onKeyAction("space") }
                SpecialKeyButton("Esc", modifier = Modifier.weight(0.7f)) { onKeyAction("esc") }
            }
        }
    }
}

@Composable
private fun SpecialKeyButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        color = Color(0xFF182230),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = modifier
            .height(34.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

// ---------------------------------------------------------------------------
// 4. SETTINGS DIALOG
// ---------------------------------------------------------------------------
@Composable
private fun TrackpadSettingsDialog(
    currentSettings: com.example.model.TrackpadSettings,
    onSave: (com.example.model.TrackpadSettings) -> Unit,
    onForgetDevice: () -> Unit,
    onDismiss: () -> Unit
) {
    var sensitivity by remember { mutableStateOf(currentSettings.sensitivity) }
    var scrollSens by remember { mutableStateOf(currentSettings.scrollSensitivity) }
    var invertScroll by remember { mutableStateOf(currentSettings.invertScroll) }
    var haptics by remember { mutableStateOf(currentSettings.hapticFeedback) }
    var showButtons by remember { mutableStateOf(currentSettings.showPhysicalButtons) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121824),
        title = {
            Text("Trackpad Settings", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Pointer Sensitivity
                Column {
                    Text(
                        text = String.format(Locale.US, "Pointer Sensitivity: %.1fx", sensitivity),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = sensitivity,
                        onValueChange = { sensitivity = it },
                        valueRange = 0.5f..2.5f,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )
                }

                // Scroll Sensitivity
                Column {
                    Text(
                        text = String.format(Locale.US, "Scroll Speed: %.1fx", scrollSens),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = scrollSens,
                        onValueChange = { scrollSens = it },
                        valueRange = 0.5f..2.5f,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )
                }

                // Invert Scroll
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Natural Scrolling", color = Color.White, fontSize = 12.sp)
                    Switch(
                        checked = invertScroll,
                        onCheckedChange = { invertScroll = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = ElectricBlue)
                    )
                }

                // Physical Buttons Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Left/Right Buttons", color = Color.White, fontSize = 12.sp)
                    Switch(
                        checked = showButtons,
                        onCheckedChange = { showButtons = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = ElectricBlue)
                    )
                }

                // Haptic Feedback
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Haptic Feedback on Click", color = Color.White, fontSize = 12.sp)
                    Switch(
                        checked = haptics,
                        onCheckedChange = { haptics = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = ElectricBlue)
                    )
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DarkBorder))

                // Forget Saved Device Button
                OutlinedButton(
                    onClick = onForgetDevice,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Forget Paired Computer", fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        currentSettings.copy(
                            sensitivity = sensitivity,
                            scrollSensitivity = scrollSens,
                            invertScroll = invertScroll,
                            hapticFeedback = haptics,
                            showPhysicalButtons = showButtons
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
