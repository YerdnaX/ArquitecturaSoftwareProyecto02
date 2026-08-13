package io.yerdna.architecturasos.redagentes

import java.io.ByteArrayOutputStream
import java.io.InterruptedIOException
import java.net.Socket

class ClienteRedAgentes {
    fun enviarMensaje(host: String, puerto: Int, mensaje: String): ResultadoEnvioRedAgentes {
        val inicio = System.currentTimeMillis()

        try {
            Socket(host, puerto).use { socket ->
                socket.soTimeout = TIMEOUT_SOCKET_MS
                val payload = "$mensaje$TERMINADOR_MENSAJE".toByteArray(Charsets.UTF_8)
                socket.getOutputStream().write(payload)
                socket.getOutputStream().flush()

                val respuesta = leerRespuesta(socket)
                return ResultadoEnvioRedAgentes(
                    respuesta = respuesta.mensaje,
                    bytesEnviados = payload.size,
                    bytesRecibidos = respuesta.bytesLeidos,
                    tiempoRespuestaMs = System.currentTimeMillis() - inicio,
                    timestamp = System.currentTimeMillis()
                )
            }
        } catch (exception: InterruptedIOException) {
            throw ErrorRedAgentesException(CodigoErrorRedAgentes.Timeout, exception)
        } catch (exception: ErrorRedAgentesException) {
            throw exception
        } catch (exception: Exception) {
            throw ErrorRedAgentesException(CodigoErrorRedAgentes.ServidorNoDisponible, exception)
        }
    }

    private fun leerRespuesta(socket: Socket): LecturaMensaje {
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
            throw ErrorRedAgentesException(CodigoErrorRedAgentes.MensajeDemasiadoGrande)
        }
        throw ErrorRedAgentesException(CodigoErrorRedAgentes.ErrorLectura)
    }

    private data class LecturaMensaje(
        val mensaje: String,
        val bytesLeidos: Int
    )
}

class ErrorRedAgentesException(
    val codigo: CodigoErrorRedAgentes,
    cause: Throwable? = null
) : Exception(cause)
