package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ControllerSettings
import com.example.model.SteerProfile
import com.example.model.SteeringDirection
import com.example.model.SteeringPacket
import com.example.service.OrientationSensorManager
import com.example.service.SocketServer
import com.example.service.SocketServerStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

data class CockpitAuxState(
    val gear: String = "4",
    val sessionTimer: String = "00:00:00.00",
    val isCalibrated: Boolean = false,
    val isLightsFlashing: Boolean = false,
    val isEbrakeActive: Boolean = false
)

data class CockpitUiState(
    val packet: SteeringPacket = SteeringPacket(0f, SteeringDirection.CENTER, 0.5f),
    val serverStatus: SocketServerStatus = SocketServerStatus(),
    val settings: ControllerSettings = ControllerSettings(),
    val sensorAvailable: Boolean = true,
    val isCalibrated: Boolean = false,
    val gear: String = "4",
    val sessionTimeFormatted: String = "00:00:00.00",
    val isLightsFlashing: Boolean = false,
    val isEbrakeActive: Boolean = false
)

class TiltMouseViewModel(application: Application) : AndroidViewModel(application) {

    private val sensorManager = OrientationSensorManager(application)
    private val socketServer = SocketServer(viewModelScope)

    private val _settings = MutableStateFlow(ControllerSettings())
    val settings: StateFlow<ControllerSettings> = _settings.asStateFlow()

    private val _auxState = MutableStateFlow(CockpitAuxState())

    val uiState: StateFlow<CockpitUiState> = combine(
        sensorManager.steeringState,
        socketServer.status,
        _settings,
        _auxState
    ) { packet, serverStatus, currentSettings, aux ->
        CockpitUiState(
            packet = packet,
            serverStatus = serverStatus,
            settings = currentSettings,
            sensorAvailable = sensorManager.sensorAvailable.value,
            isCalibrated = aux.isCalibrated,
            gear = aux.gear,
            sessionTimeFormatted = aux.sessionTimer,
            isLightsFlashing = aux.isLightsFlashing,
            isEbrakeActive = aux.isEbrakeActive
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CockpitUiState()
    )

    private val startTimeMs = System.currentTimeMillis()

    init {
        sensorManager.startListening()
        startServer(_settings.value.port)

        // Session timer coroutine for telemetry display
        viewModelScope.launch {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTimeMs
                val hours = (elapsed / 3600000) % 24
                val minutes = (elapsed / 60000) % 60
                val seconds = (elapsed / 1000) % 60
                val centis = (elapsed % 1000) / 10
                val formatted = String.format(Locale.US, "%02d:%02d:%02d.%02d", hours, minutes, seconds, centis)
                _auxState.value = _auxState.value.copy(sessionTimer = formatted)
                delay(40L)
            }
        }
    }

    private fun startServer(port: Int) {
        socketServer.start(port) {
            sensorManager.steeringState.value
        }
    }

    fun calibrateZero() {
        sensorManager.calibrateZero()
        _auxState.value = _auxState.value.copy(isCalibrated = true)
    }

    fun resetCalibration() {
        sensorManager.resetCalibration()
        _auxState.value = _auxState.value.copy(isCalibrated = false)
    }

    fun toggleSignalActive() {
        val updated = _settings.value.copy(isSignalActive = !_settings.value.isSignalActive)
        _settings.value = updated
        sensorManager.settings = updated
    }

    fun setLockedYRatio(ratio: Float) {
        val clamped = ratio.coerceIn(0f, 1f)
        val updated = _settings.value.copy(lockedYRatio = clamped)
        _settings.value = updated
        sensorManager.settings = updated
    }

    fun cycleSteerProfile() {
        val profiles = SteerProfile.entries
        val nextIdx = (profiles.indexOf(_settings.value.steerProfile) + 1) % profiles.size
        val nextProfile = profiles[nextIdx]
        val updated = _settings.value.copy(
            steerProfile = nextProfile,
            maxAngleRange = nextProfile.maxRange
        )
        _settings.value = updated
        sensorManager.settings = updated
    }

    fun shiftGear(direction: Int) {
        val gears = listOf("R", "N", "1", "2", "3", "4", "5", "6", "7")
        val currentIdx = gears.indexOf(_auxState.value.gear).coerceAtLeast(0)
        val newIdx = (currentIdx + direction).coerceIn(0, gears.size - 1)
        _auxState.value = _auxState.value.copy(gear = gears[newIdx])
    }

    fun flashLights() {
        viewModelScope.launch {
            _auxState.value = _auxState.value.copy(isLightsFlashing = true)
            delay(350L)
            _auxState.value = _auxState.value.copy(isLightsFlashing = false)
        }
    }

    fun toggleEbrake() {
        _auxState.value = _auxState.value.copy(isEbrakeActive = !_auxState.value.isEbrakeActive)
    }

    fun toggleInvert() {
        val updated = _settings.value.copy(invertAxis = !_settings.value.invertAxis)
        _settings.value = updated
        sensorManager.settings = updated
    }

    fun updatePort(newPort: Int) {
        if (newPort in 1024..65535 && newPort != _settings.value.port) {
            val updated = _settings.value.copy(port = newPort)
            _settings.value = updated
            sensorManager.settings = updated
            startServer(newPort)
        }
    }

    fun restartServer() {
        startServer(_settings.value.port)
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.stopListening()
        socketServer.stop()
    }
}
