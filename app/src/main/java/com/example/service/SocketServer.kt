package com.example.service

import com.example.model.SteeringPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

data class SocketServerStatus(
    val isRunning: Boolean = false,
    val port: Int = 8888,
    val connectedClients: Int = 0,
    val packetsSentTotal: Long = 0L,
    val currentRateHz: Float = 0f,
    val lastError: String? = null
)

class SocketServer(
    private val scope: CoroutineScope
) {
    private var serverSocket: ServerSocket? = null
    private val activeClients = CopyOnWriteArrayList<ClientSession>()

    private val _status = MutableStateFlow(SocketServerStatus())
    val status: StateFlow<SocketServerStatus> = _status.asStateFlow()

    private var acceptJob: Job? = null
    private var statsJob: Job? = null
    private var streamJob: Job? = null

    private var totalPacketsCounter = 0L
    private var intervalPacketsCounter = 0L

    private class ClientSession(
        val socket: Socket,
        val writer: BufferedWriter
    )

    fun start(port: Int = 8888, packetProvider: () -> SteeringPacket) {
        stop()

        _status.value = _status.value.copy(
            isRunning = true,
            port = port,
            lastError = null,
            connectedClients = 0
        )

        acceptJob = scope.launch(Dispatchers.IO) {
            try {
                val server = ServerSocket(port).also { serverSocket = it }
                while (isActive && !server.isClosed) {
                    try {
                        val clientSocket = server.accept()
                        clientSocket.tcpNoDelay = true // minimize latency
                        clientSocket.sendBufferSize = 1024

                        val writer = BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream()))
                        val session = ClientSession(clientSocket, writer)
                        activeClients.add(session)

                        updateClientCount()
                    } catch (e: IOException) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                _status.value = _status.value.copy(
                    isRunning = false,
                    lastError = "Port $port error: ${e.localizedMessage}"
                )
            }
        }

        // Streaming loop targeting ~60 Hz (every ~16ms)
        streamJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val startTime = System.currentTimeMillis()

                if (activeClients.isNotEmpty()) {
                    val packet = packetProvider()
                    val payload = packet.toJson()

                    val deadSessions = mutableListOf<ClientSession>()
                    for (session in activeClients) {
                        try {
                            session.writer.write(payload)
                            session.writer.flush()
                            totalPacketsCounter++
                            intervalPacketsCounter++
                        } catch (e: IOException) {
                            deadSessions.add(session)
                        }
                    }

                    if (deadSessions.isNotEmpty()) {
                        for (dead in deadSessions) {
                            try {
                                dead.socket.close()
                            } catch (_: Exception) {}
                            activeClients.remove(dead)
                        }
                        updateClientCount()
                    }
                }

                val elapsed = System.currentTimeMillis() - startTime
                val sleepTime = (16L - elapsed).coerceAtLeast(1L)
                delay(sleepTime)
            }
        }

        // Stats monitor every 1 second
        statsJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000L)
                val hz = intervalPacketsCounter.toFloat()
                intervalPacketsCounter = 0L

                _status.value = _status.value.copy(
                    packetsSentTotal = totalPacketsCounter,
                    currentRateHz = hz
                )
            }
        }
    }

    private fun updateClientCount() {
        _status.value = _status.value.copy(
            connectedClients = activeClients.size
        )
    }

    fun stop() {
        acceptJob?.cancel()
        streamJob?.cancel()
        statsJob?.cancel()

        for (client in activeClients) {
            try {
                client.socket.close()
            } catch (_: Exception) {}
        }
        activeClients.clear()

        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null

        _status.value = _status.value.copy(
            isRunning = false,
            connectedClients = 0,
            currentRateHz = 0f
        )
    }
}
