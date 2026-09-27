package com.example.model

data class DiscoveredServer(
    val name: String,
    val ip: String,
    val port: Int = 8888,
    val pinHint: String = "",
    val lastSeenMs: Long = System.currentTimeMillis()
) {
    val addressKey: String get() = "$ip:$port"
}

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Searching : ConnectionState()
    data class Connecting(val serverName: String, val ip: String) : ConnectionState()
    data class PinRequired(val serverName: String, val ip: String, val port: Int) : ConnectionState()
    data class Connected(val serverName: String, val ip: String, val port: Int) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class TrackpadSettings(
    val sensitivity: Float = 1.25f,
    val scrollSensitivity: Float = 1.0f,
    val invertScroll: Boolean = false,
    val hapticFeedback: Boolean = true,
    val showPhysicalButtons: Boolean = true
)

data class SavedDevice(
    val name: String,
    val ip: String,
    val port: Int,
    val pin: String
)
