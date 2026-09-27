package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ConnectionState
import com.example.model.DiscoveredServer
import com.example.model.SavedDevice
import com.example.model.TrackpadSettings
import com.example.network.ConnectionRepository
import com.example.network.TrackpadClient
import com.example.network.UdpDiscoveryListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TrackpadViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ConnectionRepository(application)
    private val discoveryListener = UdpDiscoveryListener(application, viewModelScope)
    private val client = TrackpadClient(viewModelScope)

    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    val connectionState: StateFlow<ConnectionState> = client.connectionState
    val discoveredServers: StateFlow<List<DiscoveredServer>> = discoveryListener.discoveredServers

    private val _savedDevice = MutableStateFlow<SavedDevice?>(null)
    val savedDevice: StateFlow<SavedDevice?> = _savedDevice.asStateFlow()

    private val _settings = MutableStateFlow(repository.getSettings())
    val settings: StateFlow<TrackpadSettings> = _settings.asStateFlow()

    private val _isMediaBarVisible = MutableStateFlow(false)
    val isMediaBarVisible: StateFlow<Boolean> = _isMediaBarVisible.asStateFlow()

    private val _isKeyboardVisible = MutableStateFlow(false)
    val isKeyboardVisible: StateFlow<Boolean> = _isKeyboardVisible.asStateFlow()

    init {
        val saved = repository.getSavedDevice()
        _savedDevice.value = saved

        // Start UDP discovery
        discoveryListener.start()

        // Friction-free auto-reconnect if saved device exists
        if (saved != null) {
            connectWithSaved(saved)
        }
    }

    fun autoReconnect() {
        val saved = _savedDevice.value ?: repository.getSavedDevice()
        if (saved != null) {
            connectWithSaved(saved)
        }
    }

    private fun connectWithSaved(saved: SavedDevice) {
        client.connect(
            serverName = saved.name,
            ip = saved.ip,
            port = saved.port,
            pin = saved.pin,
            onSuccess = {
                vibrateClick()
            },
            onPinFailed = {
                // If saved PIN was changed on desktop, prompt user
            }
        )
    }

    fun connectToServer(server: DiscoveredServer, pin: String) {
        val finalPin = pin.ifBlank { server.pinHint }.ifBlank { "0000" }
        client.connect(
            serverName = server.name,
            ip = server.ip,
            port = server.port,
            pin = finalPin,
            onSuccess = {
                val saved = SavedDevice(server.name, server.ip, server.port, finalPin)
                repository.saveDevice(saved)
                _savedDevice.value = saved
                vibrateClick()
            }
        )
    }

    fun connectManual(ip: String, port: Int, pin: String) {
        client.connect(
            serverName = "PC ($ip)",
            ip = ip,
            port = port,
            pin = pin,
            onSuccess = {
                val saved = SavedDevice("PC ($ip)", ip, port, pin)
                repository.saveDevice(saved)
                _savedDevice.value = saved
                vibrateClick()
            }
        )
    }

    fun disconnect() {
        client.disconnect()
    }

    fun forgetSavedDevice() {
        repository.clearSavedDevice()
        _savedDevice.value = null
        disconnect()
    }

    fun toggleMediaBar() {
        _isMediaBarVisible.value = !_isMediaBarVisible.value
    }

    fun toggleKeyboard() {
        _isKeyboardVisible.value = !_isKeyboardVisible.value
    }

    fun updateSettings(newSettings: TrackpadSettings) {
        _settings.value = newSettings
        repository.saveSettings(newSettings)
    }

    // Gesture Handlers
    fun onMove(dx: Float, dy: Float) {
        val s = _settings.value.sensitivity
        // Smooth non-linear cursor acceleration curve
        val speed = kotlin.math.sqrt(dx * dx + dy * dy)
        val accel = if (speed > 10f) 1.25f else 1.0f

        val finalDx = dx * s * accel
        val finalDy = dy * s * accel
        client.sendMove(finalDx, finalDy)
    }

    fun onScroll(dy: Float) {
        val s = _settings.value.scrollSensitivity
        val dir = if (_settings.value.invertScroll) -1f else 1f
        client.sendScroll(dy * s * dir)
    }

    fun onClick(button: String = "l") {
        client.sendClick(button)
        vibrateClick()
    }

    fun onMouseDown(button: String = "l") {
        client.sendMouseDown(button)
        vibrateClick()
    }

    fun onMouseUp(button: String = "l") {
        client.sendMouseUp(button)
    }

    fun sendMedia(action: String) {
        client.sendMedia(action)
        vibrateClick()
    }

    fun sendKey(key: String) {
        client.sendKey(key)
        vibrateClick()
    }

    fun sendText(text: String) {
        client.sendText(text)
    }

    private fun vibrateClick() {
        if (!_settings.value.hapticFeedback) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(18)
            }
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        discoveryListener.stop()
        client.disconnect()
    }
}
