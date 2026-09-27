package com.example.network

import android.os.Build
import com.example.model.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Locale

class TrackpadClient(
    private val scope: CoroutineScope
) {
    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val sendChannel = Channel<String>(capacity = 100)
    private var sendJob: Job? = null
    private var receiveJob: Job? = null
    private var connectionJob: Job? = null

    fun connect(
        serverName: String,
        ip: String,
        port: Int,
        pin: String,
        onSuccess: (() -> Unit)? = null,
        onPinFailed: (() -> Unit)? = null
    ) {
        disconnect()

        _connectionState.value = ConnectionState.Connecting(serverName, ip)

        connectionJob = scope.launch(Dispatchers.IO) {
            try {
                val s = Socket()
                s.tcpNoDelay = true
                s.sendBufferSize = 2048
                s.receiveBufferSize = 2048
                s.connect(InetSocketAddress(ip, port), 4000)
                socket = s

                val w = BufferedWriter(OutputStreamWriter(s.getOutputStream(), Charsets.UTF_8))
                val r = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
                writer = w
                reader = r

                // Send Auth Handshake
                val authPayload = JSONObject().apply {
                    put("type", "auth")
                    put("pin", pin)
                    put("client_name", "${Build.MANUFACTURER} ${Build.MODEL}")
                }.toString() + "\n"

                w.write(authPayload)
                w.flush()

                // Read Auth Response
                s.soTimeout = 4000
                val responseLine = r.readLine()
                s.soTimeout = 0

                if (responseLine == null) {
                    throw IllegalStateException("Connection closed during authentication")
                }

                val responseJson = JSONObject(responseLine)
                if (responseJson.optString("type") == "auth_ok") {
                    val verifiedName = responseJson.optString("server_name", serverName)
                    _connectionState.value = ConnectionState.Connected(verifiedName, ip, port)

                    withContext(Dispatchers.Main) {
                        onSuccess?.invoke()
                    }

                    startMessageLoops()
                } else {
                    val reason = responseJson.optString("reason", "Invalid 4-digit PIN")
                    _connectionState.value = ConnectionState.Error(reason)
                    withContext(Dispatchers.Main) {
                        onPinFailed?.invoke()
                    }
                    disconnect()
                }
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Error(e.localizedMessage ?: "Failed to connect to $ip:$port")
            }
        }
    }

    private fun startMessageLoops() {
        // Send loop
        sendJob = scope.launch(Dispatchers.IO) {
            val w = writer ?: return@launch
            while (isActive) {
                val msg = sendChannel.receive()
                try {
                    w.write(msg)
                    w.flush()
                } catch (e: Exception) {
                    if (isActive) {
                        _connectionState.value = ConnectionState.Error("Disconnected: ${e.message}")
                    }
                    break
                }
            }
        }

        // Receive / Keepalive monitor loop
        receiveJob = scope.launch(Dispatchers.IO) {
            val r = reader ?: return@launch
            try {
                while (isActive) {
                    val line = r.readLine() ?: break
                    // Handle server-to-client events if needed
                }
            } catch (e: Exception) {
                if (isActive) {
                    _connectionState.value = ConnectionState.Error("Connection closed")
                }
            }
        }
    }

    fun sendMove(dx: Float, dy: Float) {
        val payload = String.format(Locale.US, "{\"t\":\"m\",\"dx\":%.2f,\"dy\":%.2f}\n", dx, dy)
        sendChannel.trySend(payload)
    }

    fun sendClick(button: String = "l") {
        sendChannel.trySend("{\"t\":\"c\",\"b\":\"$button\"}\n")
    }

    fun sendMouseDown(button: String = "l") {
        sendChannel.trySend("{\"t\":\"d\",\"b\":\"$button\"}\n")
    }

    fun sendMouseUp(button: String = "l") {
        sendChannel.trySend("{\"t\":\"u\",\"b\":\"$button\"}\n")
    }

    fun sendScroll(dy: Float) {
        val payload = String.format(Locale.US, "{\"t\":\"s\",\"dy\":%.2f}\n", dy)
        sendChannel.trySend(payload)
    }

    fun sendKey(key: String) {
        sendChannel.trySend("{\"t\":\"k\",\"k\":\"$key\"}\n")
    }

    fun sendText(text: String) {
        val escaped = JSONObject.quote(text)
        sendChannel.trySend("{\"t\":\"text\",\"text\":$escaped}\n")
    }

    fun sendMedia(action: String) {
        sendChannel.trySend("{\"t\":\"media\",\"a\":\"$action\"}\n")
    }

    fun disconnect() {
        connectionJob?.cancel()
        sendJob?.cancel()
        receiveJob?.cancel()

        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        writer = null
        reader = null

        if (_connectionState.value !is ConnectionState.Error) {
            _connectionState.value = ConnectionState.Disconnected
        }
    }
}
