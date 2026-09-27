package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionState
import com.example.model.DiscoveredServer
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private val DarkBg = Color(0xFF0A0E14)
private val CardBg = Color(0xFF121824)
private val InnerCardBg = Color(0xFF182232)

@Composable
fun PairingScreen(
    viewModel: TrackpadViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val discoveredServers by viewModel.discoveredServers.collectAsState()
    val savedDevice by viewModel.savedDevice.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedServerForPin by remember { mutableStateOf<DiscoveredServer?>(null) }

    // Manual connection fields
    var manualIp by remember { mutableStateOf("") }
    var manualPort by remember { mutableStateOf("8888") }
    var manualPin by remember { mutableStateOf("") }

    val infinitePulse = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infinitePulse.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radar_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
            .testTag("pairing_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = ElectricBlue.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Laptop,
                            contentDescription = "AirMouse",
                            tint = NeonCyan,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "AIRMOUSE",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Wireless Trackpad & Remote",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Wi-Fi Status Badge
                Surface(
                    color = InnerCardBg,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NeonGreen.copy(alpha = pulseAlpha))
                        )
                        Text(
                            text = "Wi-Fi Ready",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Auto-Reconnect Card (if saved device exists and currently disconnected)
            if (savedDevice != null && connectionState is ConnectionState.Disconnected) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LAST PAIRED COMPUTER",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = savedDevice?.name ?: "PC",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${savedDevice?.ip}:${savedDevice?.port}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = { viewModel.autoReconnect() },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Reconnect", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Connection Progress / Error Banner
            when (val state = connectionState) {
                is ConnectionState.Connecting -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                            Text(
                                text = "Connecting to ${state.serverName}...",
                                color = NeonCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                is ConnectionState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF221115)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = NeonRed, modifier = Modifier.size(20.dp))
                            Text(
                                text = state.message,
                                color = NeonRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                else -> {}
            }

            // Navigation Tabs: Auto-Discovery vs Manual IP
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkBg,
                contentColor = NeonCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NeonCyan
                    )
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Auto-Discovery", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Direct IP", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // TAB 1: AUTO DISCOVERY LIST
            if (selectedTab == 0) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AVAILABLE COMPUTERS (${discoveredServers.size})",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                            Text("Scanning subnet...", color = TextMuted, fontSize = 10.sp)
                        }
                    }

                    if (discoveredServers.isEmpty()) {
                        // Empty Discovery State
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(InnerCardBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                                }
                                Text(
                                    text = "Searching for PC on local Wi-Fi...",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "1. Launch desktop_server.py on your laptop.\n2. Ensure both phone & PC are on the same Wi-Fi.\n3. Your PC will appear here automatically!",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    } else {
                        // Discovered Servers List
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(discoveredServers, key = { it.addressKey }) { server ->
                                ServerCard(
                                    server = server,
                                    onPairClicked = {
                                        selectedServerForPin = server
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // TAB 2: MANUAL DIRECT IP ENTRY
            if (selectedTab == 1) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Direct Network Connection",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Use this fallback if your router blocks UDP broadcasts:",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )

                        OutlinedTextField(
                            value = manualIp,
                            onValueChange = { manualIp = it },
                            label = { Text("PC IP Address (e.g. 192.168.1.50)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = manualPort,
                                onValueChange = { manualPort = it },
                                label = { Text("Port (8888)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = manualPin,
                                onValueChange = { manualPin = it },
                                label = { Text("4-Digit PIN") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Button(
                            onClick = {
                                val port = manualPort.toIntOrNull() ?: 8888
                                viewModel.connectManual(manualIp.trim(), port, manualPin.trim())
                            },
                            enabled = manualIp.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Connect Directly", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // 4-DIGIT PIN ENTRY DIALOG
    selectedServerForPin?.let { server ->
        PinEntryDialog(
            server = server,
            onConfirmPin = { pin ->
                viewModel.connectToServer(server, pin)
                selectedServerForPin = null
            },
            onDismiss = { selectedServerForPin = null }
        )
    }
}

@Composable
private fun ServerCard(
    server: DiscoveredServer,
    onPairClicked: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPairClicked() }
            .testTag("server_card_${server.ip}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(InnerCardBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Computer, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                }

                Column {
                    Text(
                        text = server.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${server.ip}:${server.port}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Button(
                onClick = onPairClicked,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Pair", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PinEntryDialog(
    server: DiscoveredServer,
    onConfirmPin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pinText by remember { mutableStateOf(server.pinHint) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Pin, contentDescription = null, tint = NeonCyan)
                Text(
                    text = "Pair with ${server.name}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter the 4-digit PIN displayed on your PC screen:",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = pinText,
                    onValueChange = { if (it.length <= 6) pinText = it },
                    singleLine = true,
                    placeholder = { Text("e.g. 4821") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmPin(pinText) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
            ) {
                Text("Connect", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
