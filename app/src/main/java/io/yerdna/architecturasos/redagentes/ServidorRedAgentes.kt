package io.yerdna.architecturasos.redagentes

import java.io.ByteArrayOutputStream
import java.io.InterruptedIOException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

class ServidorRedAgentes(
    private val callbacks: Callbacks
) {
    interface Callbacks {
        fun onIniciando()
        fun onEscuchando(puerto: Int)
        fun onClienteConectado()
        fun onMensajeProcesado(evento: EventoServidorRedAgentes)
        fun onDetenido()
        fun onError(codigo: CodigoErrorRedAgentes, throwable: Throwable? = null)
    }

    @Volatile
    private var activo = false
    private var serverSocket: ServerSocket? = null
    private var socketCliente: Socket? = null

    fun ejecutar() {
        activo = true
        callbacks.onIniciando()

        try {
            ServerSocket(0).use { servidor ->
                serverSocket = servidor
                callbacks.onEscuchando(servidor.localPort)

                while (activo) {
                    val cliente = try {
                        servidor.accept()
                    } catch (exception: SocketException) {
                        if (activo) callbacks.onError(CodigoErrorRedAgentes.ErrorLectura, exception)
                        break
                    }

                    socketCliente = cliente
                    callbacks.onClienteConectado()
                    atenderCliente(cliente)
                    socketCliente = null
                    if (activo) callbacks.onEscuchando(servidor.localPort)
                }
            }
        } catch (exception: SocketException) {
            if (activo) callbacks.onError(CodigoErrorRedAgentes.ServidorNoDisponible, exception)
        } catch (exception: Exception) {
            if (activo) callbacks.onError(CodigoErrorRedAgentes.ErrorDesconocido, exception)
        } finally {
            activo = false
            serverSocket = null
            socketCliente = null
            callbacks.onDetenido()
        }
    }

    fun detener() {
        activo = false
        cerrarSocketCliente()
        cerrarServidor()
    }

    private fun atenderCliente(cliente: Socket) {
        cliente.use { socket ->
            socket.soTimeout = TIMEOUT_SOCKET_MS
            try {
                val lectura = leerMensaje(socket)
                val respuesta = "Central recibio: ${lectura.mensaje}"
                val bytesRespuesta = aBytesConTerminador(respuesta)
                socket.getOutputStream().write(bytesRespuesta)
                socket.getOutputStream().flush()
                callbacks.onMensajeProcesado(
                    EventoServidorRedAgentes(
                        mensaje = lectura.mensaje,
                        respuesta = respuesta,
                        bytesRecibidos = lectura.bytesLeidos,
                        bytesEnviados = bytesRespuesta.size,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (exception: InterruptedIOException) {
                callbacks.onError(CodigoErrorRedAgentes.Timeout, exception)
            } catch (exception: MensajeGrandeException) {
                callbacks.onError(CodigoErrorRedAgentes.MensajeDemasiadoGrande, exception)
            } catch (exception: Exception) {
                callbacks.onError(CodigoErrorRedAgentes.ErrorLectura, exception)
            }
        }
    }

    private fun leerMensaje(socket: Socket): LecturaMensaje {
        val buffer = ByteArrayOutputStream()
        val input = socket.getInputStream()

        while (buffer.size() < MAX_BYTES_MENSAJE) {
            val byteLeido = input.read()
            if (byteLeido == -1) break
            buffer.write(byteLeido)
            if (byteLeido.toChar() == TERMINADOR_MENSAJE) {
                val bytes = buffer.toByteArray()
                val mensaje = bytes
                    .dropLast(1)
                    .toByteArray()
                    .toString(Charsets.UTF_8)
                return LecturaMensaje(mensaje = mensaje, bytesLeidos = bytes.size)
            }
        }

        if (buffer.size() >= MAX_BYTES_MENSAJE) {
            throw MensajeGrandeException()
        }
        throw IllegalStateException("Mensaje incompleto")
    }

    private fun cerrarSocketCliente() {
        try {
            socketCliente?.close()
        } catch (_: Exception) {
            callbacks.onError(CodigoErrorRedAgentes.ErrorCierre)
        }
    }

    private fun cerrarServidor() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {
            callbacks.onError(CodigoErrorRedAgentes.ErrorCierre)
        }
    }

    private fun aBytesConTerminador(mensaje: String): ByteArray {
        return "$mensaje$TERMINADOR_MENSAJE".toByteArray(Charsets.UTF_8)
    }

    private data class LecturaMensaje(
        val mensaje: String,
        val bytesLeidos: Int
    )

    private class MensajeGrandeException : Exception()
}
