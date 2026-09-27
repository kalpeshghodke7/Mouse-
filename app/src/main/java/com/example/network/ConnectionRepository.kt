package com.example.network

import android.content.Context
import android.content.SharedPreferences
import com.example.model.SavedDevice
import com.example.model.TrackpadSettings

class ConnectionRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("airmouse_prefs", Context.MODE_PRIVATE)

    fun getSavedDevice(): SavedDevice? {
        val ip = prefs.getString("saved_ip", null) ?: return null
        val port = prefs.getInt("saved_port", 8888)
        val name = prefs.getString("saved_name", "My Computer") ?: "My Computer"
        val pin = prefs.getString("saved_pin", "") ?: ""
        return SavedDevice(name, ip, port, pin)
    }

    fun saveDevice(device: SavedDevice) {
        prefs.edit()
            .putString("saved_name", device.name)
            .putString("saved_ip", device.ip)
            .putInt("saved_port", device.port)
            .putString("saved_pin", device.pin)
            .apply()
    }

    fun clearSavedDevice() {
        prefs.edit()
            .remove("saved_name")
            .remove("saved_ip")
            .remove("saved_port")
            .remove("saved_pin")
            .apply()
    }

    fun getSettings(): TrackpadSettings {
        return TrackpadSettings(
            sensitivity = prefs.getFloat("pref_sensitivity", 1.25f),
            scrollSensitivity = prefs.getFloat("pref_scroll", 1.0f),
            invertScroll = prefs.getBoolean("pref_invert_scroll", false),
            hapticFeedback = prefs.getBoolean("pref_haptics", true),
            showPhysicalButtons = prefs.getBoolean("pref_buttons", true)
        )
    }

    fun saveSettings(settings: TrackpadSettings) {
        prefs.edit()
            .putFloat("pref_sensitivity", settings.sensitivity)
            .putFloat("pref_scroll", settings.scrollSensitivity)
            .putBoolean("pref_invert_scroll", settings.invertScroll)
            .putBoolean("pref_haptics", settings.hapticFeedback)
            .putBoolean("pref_buttons", settings.showPhysicalButtons)
            .apply()
    }
}
