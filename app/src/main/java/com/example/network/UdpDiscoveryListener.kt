package com.example.network

import android.content.Context
import android.net.wifi.WifiManager
import com.example.model.DiscoveredServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

class UdpDiscoveryListener(
    context: Context,
    private val scope: CoroutineScope
) {
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null

    private val _discoveredServers = MutableStateFlow<List<DiscoveredServer>>(emptyList())
    val discoveredServers: StateFlow<List<DiscoveredServer>> = _discoveredServers.asStateFlow()

    private var listenJob: Job? = null
    private var cleanupJob: Job? = null
    private var socket: DatagramSocket? = null

    companion object {
        const val DISCOVERY_PORT = 53530
    }

    fun start() {
        stop()

        try {
            multicastLock = wifiManager?.createMulticastLock("airmouse_discovery")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (_: Exception) {}

        listenJob = scope.launch(Dispatchers.IO) {
            try {
                val s = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(DISCOVERY_PORT))
                    broadcast = true
                    soTimeout = 3000
                }
                socket = s

                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)

                while (isActive && !s.isClosed) {
                    try {
                        s.receive(packet)
                        val text = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
                        val senderIp = packet.address?.hostAddress ?: continue

                        parseBeacon(text, senderIp)
                    } catch (_: java.net.SocketTimeoutException) {
                        // Timeout allows checking isActive
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (_: Exception) {}
        }

        // Cleanup stale servers every 3 seconds
        cleanupJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(3000L)
                val now = System.currentTimeMillis()
                val current = _discoveredServers.value
                val fresh = current.filter { now - it.lastSeenMs < 6000L }
                if (fresh.size != current.size) {
                    _discoveredServers.value = fresh
                }
            }
        }
    }

    private fun parseBeacon(jsonStr: String, fallbackIp: String) {
        try {
            val json = JSONObject(jsonStr)
            if (json.optString("type") == "airmouse_beacon") {
                val name = json.optString("name", "Desktop PC")
                val reportedIp = json.optString("ip", fallbackIp)
                val ip = if (reportedIp.isNotBlank() && reportedIp != "127.0.0.1") reportedIp else fallbackIp
                val port = json.optInt("port", 8888)
                val pinHint = json.optString("pin", "")

                val discovered = DiscoveredServer(
                    name = name,
                    ip = ip,
                    port = port,
                    pinHint = pinHint,
                    lastSeenMs = System.currentTimeMillis()
                )

                val list = _discoveredServers.value.toMutableList()
                val existingIndex = list.indexOfFirst { it.addressKey == discovered.addressKey }
                if (existingIndex >= 0) {
                    list[existingIndex] = discovered
                } else {
                    list.add(discovered)
                }
                _discoveredServers.value = list
            }
        } catch (_: Exception) {}
    }

    fun stop() {
        listenJob?.cancel()
        cleanupJob?.cancel()
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null

        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {}
        multicastLock = null
    }
}
