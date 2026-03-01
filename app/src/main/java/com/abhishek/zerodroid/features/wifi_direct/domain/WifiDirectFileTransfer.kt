package com.abhishek.zerodroid.features.wifi_direct.domain

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

data class TransferProgress(
    val state: TransferState,
    val fileName: String = "",
    val bytesTransferred: Long = 0,
    val totalBytes: Long = 0,
    val speedBytesPerSec: Long = 0,
    val error: String? = null
) {
    val progressPercent: Float
        get() = if (totalBytes > 0) (bytesTransferred.toFloat() / totalBytes) else 0f
}

enum class TransferState {
    Idle,
    WaitingForConnection,
    Connecting,
    Transferring,
    Completed,
    Failed
}

data class TransferHistoryEntry(
    val fileName: String,
    val fileSize: Long,
    val isSent: Boolean,
    val timestamp: Long,
    val success: Boolean
)

class WifiDirectFileTransfer(private val context: Context) {
    companion object {
        const val PORT = 8988
        const val BUFFER_SIZE = 8192
        private const val SOCKET_TIMEOUT = 15_000
    }

    private val _progress = MutableStateFlow(TransferProgress(TransferState.Idle))
    val progress: StateFlow<TransferProgress> = _progress.asStateFlow()

    private val _history = MutableStateFlow<List<TransferHistoryEntry>>(emptyList())
    val history: StateFlow<List<TransferHistoryEntry>> = _history.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null

    suspend fun startReceiving() = withContext(Dispatchers.IO) {
        try {
            _progress.value = TransferProgress(TransferState.WaitingForConnection)

            serverSocket = ServerSocket(PORT).apply {
                reuseAddress = true
                soTimeout = 0 // Block indefinitely until a connection arrives
            }

            val socket = serverSocket!!.accept()
            clientSocket = socket

            _progress.value = TransferProgress(TransferState.Connecting)

            val input = DataInputStream(socket.getInputStream())

            // Read header: [4 bytes name length][name bytes][8 bytes file size]
            val nameLength = input.readInt()
            val nameBytes = ByteArray(nameLength)
            input.readFully(nameBytes)
            val fileName = String(nameBytes, Charsets.UTF_8)
            val fileSize = input.readLong()

            _progress.value = TransferProgress(
                state = TransferState.Transferring,
                fileName = fileName,
                totalBytes = fileSize
            )

            // Save to Downloads directory
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: throw IllegalStateException("Downloads directory unavailable")
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val outputFile = generateUniqueFile(downloadsDir, fileName)
            val outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(BUFFER_SIZE)
            var bytesReceived: Long = 0
            var lastSpeedUpdate = System.currentTimeMillis()
            var bytesAtLastUpdate: Long = 0

            while (bytesReceived < fileSize) {
                val remaining = fileSize - bytesReceived
                val toRead = minOf(BUFFER_SIZE.toLong(), remaining).toInt()
                val read = input.read(buffer, 0, toRead)
                if (read == -1) break
                outputStream.write(buffer, 0, read)
                bytesReceived += read

                val now = System.currentTimeMillis()
                val elapsed = now - lastSpeedUpdate
                val speed = if (elapsed > 500) {
                    val delta = bytesReceived - bytesAtLastUpdate
                    val bps = (delta * 1000) / elapsed
                    bytesAtLastUpdate = bytesReceived
                    lastSpeedUpdate = now
                    bps
                } else {
                    _progress.value.speedBytesPerSec
                }

                _progress.value = TransferProgress(
                    state = TransferState.Transferring,
                    fileName = fileName,
                    bytesTransferred = bytesReceived,
                    totalBytes = fileSize,
                    speedBytesPerSec = speed
                )
            }

            outputStream.flush()
            outputStream.close()
            input.close()
            socket.close()

            val success = bytesReceived == fileSize
            _progress.value = if (success) {
                TransferProgress(
                    state = TransferState.Completed,
                    fileName = fileName,
                    bytesTransferred = bytesReceived,
                    totalBytes = fileSize
                )
            } else {
                TransferProgress(
                    state = TransferState.Failed,
                    fileName = fileName,
                    bytesTransferred = bytesReceived,
                    totalBytes = fileSize,
                    error = "Transfer incomplete: received $bytesReceived of $fileSize bytes"
                )
            }

            addHistoryEntry(
                TransferHistoryEntry(
                    fileName = fileName,
                    fileSize = fileSize,
                    isSent = false,
                    timestamp = System.currentTimeMillis(),
                    success = success
                )
            )
        } catch (e: Exception) {
            if (_progress.value.state != TransferState.Idle) {
                _progress.value = TransferProgress(
                    state = TransferState.Failed,
                    fileName = _progress.value.fileName,
                    error = e.message ?: "Receive failed"
                )
            }
        } finally {
            closeServerSocket()
        }
    }

    suspend fun sendFile(hostAddress: String, uri: Uri) = withContext(Dispatchers.IO) {
        try {
            _progress.value = TransferProgress(TransferState.Connecting)

            val fileName = getFileNameFromUri(uri) ?: "unknown_file"
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Cannot open file URI")
            val fileSize = getFileSizeFromUri(uri)

            val socket = Socket()
            clientSocket = socket
            socket.connect(InetSocketAddress(hostAddress, PORT), SOCKET_TIMEOUT)

            val output = DataOutputStream(socket.getOutputStream())

            // Write header: [4 bytes name length][name bytes][8 bytes file size]
            val nameBytes = fileName.toByteArray(Charsets.UTF_8)
            output.writeInt(nameBytes.size)
            output.write(nameBytes)
            output.writeLong(fileSize)

            _progress.value = TransferProgress(
                state = TransferState.Transferring,
                fileName = fileName,
                totalBytes = fileSize
            )

            val buffer = ByteArray(BUFFER_SIZE)
            var bytesSent: Long = 0
            var lastSpeedUpdate = System.currentTimeMillis()
            var bytesAtLastUpdate: Long = 0

            while (true) {
                val read = inputStream.read(buffer)
                if (read == -1) break
                output.write(buffer, 0, read)
                bytesSent += read

                val now = System.currentTimeMillis()
                val elapsed = now - lastSpeedUpdate
                val speed = if (elapsed > 500) {
                    val delta = bytesSent - bytesAtLastUpdate
                    val bps = (delta * 1000) / elapsed
                    bytesAtLastUpdate = bytesSent
                    lastSpeedUpdate = now
                    bps
                } else {
                    _progress.value.speedBytesPerSec
                }

                _progress.value = TransferProgress(
                    state = TransferState.Transferring,
                    fileName = fileName,
                    bytesTransferred = bytesSent,
                    totalBytes = fileSize,
                    speedBytesPerSec = speed
                )
            }

            output.flush()
            output.close()
            inputStream.close()
            socket.close()

            _progress.value = TransferProgress(
                state = TransferState.Completed,
                fileName = fileName,
                bytesTransferred = bytesSent,
                totalBytes = fileSize
            )

            addHistoryEntry(
                TransferHistoryEntry(
                    fileName = fileName,
                    fileSize = fileSize,
                    isSent = true,
                    timestamp = System.currentTimeMillis(),
                    success = true
                )
            )
        } catch (e: Exception) {
            val currentFileName = _progress.value.fileName
            _progress.value = TransferProgress(
                state = TransferState.Failed,
                fileName = currentFileName,
                error = e.message ?: "Send failed"
            )
            addHistoryEntry(
                TransferHistoryEntry(
                    fileName = currentFileName,
                    fileSize = 0,
                    isSent = true,
                    timestamp = System.currentTimeMillis(),
                    success = false
                )
            )
        } finally {
            clientSocket = null
        }
    }

    fun cancel() {
        try {
            clientSocket?.close()
        } catch (_: Exception) {}
        closeServerSocket()
        clientSocket = null
        _progress.value = TransferProgress(TransferState.Idle)
    }

    private fun closeServerSocket() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
    }

    private fun addHistoryEntry(entry: TransferHistoryEntry) {
        _history.value = listOf(entry) + _history.value
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun getFileSizeFromUri(uri: Uri): Long {
        var size: Long = 0
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex >= 0 && cursor.moveToFirst()) {
                size = cursor.getLong(sizeIndex)
            }
        }
        return size
    }

    private fun generateUniqueFile(directory: File, fileName: String): File {
        var file = File(directory, fileName)
        if (!file.exists()) return file

        val dotIndex = fileName.lastIndexOf('.')
        val baseName = if (dotIndex > 0) fileName.substring(0, dotIndex) else fileName
        val extension = if (dotIndex > 0) fileName.substring(dotIndex) else ""

        var counter = 1
        while (file.exists()) {
            file = File(directory, "${baseName}_($counter)$extension")
            counter++
        }
        return file
    }
}
