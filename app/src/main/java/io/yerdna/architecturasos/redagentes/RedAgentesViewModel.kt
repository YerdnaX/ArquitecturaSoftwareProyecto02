package io.yerdna.architecturasos.redagentes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class RedAgentesViewModel : ViewModel() {
    var estado by mutableStateOf(EstadoRedAgentes())
        private set

    var textoMensaje by mutableStateOf("")
        private set

    val mensajeValido: Boolean
        get() = textoMensaje.trim().isNotEmpty() &&
            textoMensaje.length <= MAX_CARACTERES_MENSAJE &&
            !textoMensaje.contains(TERMINADOR_MENSAJE)

    fun actualizarMensaje(texto: String) {
        textoMensaje = texto
            .filter { it != TERMINADOR_MENSAJE }
            .take(MAX_CARACTERES_MENSAJE)
    }

    fun prepararInicioServidor() {
        estado = EstadoRedAgentes(
            estadoServidor = EstadoServidorRedAgentes.Iniciando
        )
    }

    fun actualizarEstadoServidor(nuevoEstado: EstadoServidorRedAgentes) {
        if (estado.estadoServidor == EstadoServidorRedAgentes.Error &&
            nuevoEstado == EstadoServidorRedAgentes.Detenido
        ) {
            return
        }

        estado = estado.copy(
            estadoServidor = nuevoEstado,
            codigoError = if (nuevoEstado == EstadoServidorRedAgentes.Error) {
                estado.codigoError
            } else {
                null
            }
        )
    }

    fun servidorEscuchando(puerto: Int) {
        estado = estado.copy(
            puerto = puerto,
            estadoServidor = EstadoServidorRedAgentes.Escuchando,
            codigoError = null
        )
    }

    fun prepararEnvio(): String? {
        val mensaje = textoMensaje.trim()
        if (mensaje.isEmpty()) {
            marcarError(CodigoErrorRedAgentes.MensajeVacio)
            return null
        }
        if (!mensajeValido || estado.puerto == null) {
            marcarError(CodigoErrorRedAgentes.ServidorNoDisponible)
            return null
        }
        estado = estado.copy(
            estadoEnvio = EstadoEnvioRedAgentes.Conectando,
            resultadoUltimaEjecucion = ResultadoRedAgentes.SinEjecucion,
            horaConexion = System.currentTimeMillis(),
            mensajeEnviado = mensaje,
            mensajeRecibidoServidor = "",
            respuestaEnviadaServidor = "",
            respuestaRecibidaCliente = "",
            bytesEnviados = 0,
            bytesRecibidos = 0,
            tiempoRespuestaMs = null,
            codigoError = null
        )
        return mensaje
    }

    fun actualizarEstadoEnvio(nuevoEstado: EstadoEnvioRedAgentes) {
        estado = estado.copy(
            estadoEnvio = nuevoEstado,
            resultadoUltimaEjecucion = if (nuevoEstado == EstadoEnvioRedAgentes.Cancelado) {
                ResultadoRedAgentes.Cancelado
            } else {
                estado.resultadoUltimaEjecucion
            }
        )
    }

    fun mensajeProcesadoServidor(evento: EventoServidorRedAgentes) {
        estado = estado.copy(
            mensajeRecibidoServidor = evento.mensaje,
            respuestaEnviadaServidor = evento.respuesta,
            bytesRecibidos = evento.bytesRecibidos,
            codigoError = null
        )
    }

    fun envioExitoso(resultado: ResultadoEnvioRedAgentes) {
        estado = estado.copy(
            estadoServidor = EstadoServidorRedAgentes.Escuchando,
            estadoEnvio = EstadoEnvioRedAgentes.Exitoso,
            resultadoUltimaEjecucion = ResultadoRedAgentes.Exitoso,
            respuestaRecibidaCliente = resultado.respuesta,
            bytesEnviados = resultado.bytesEnviados,
            bytesRecibidos = resultado.bytesRecibidos,
            tiempoRespuestaMs = resultado.tiempoRespuestaMs,
            codigoError = null
        )
    }

    fun detenerServidor() {
        estado = estado.copy(
            estadoServidor = EstadoServidorRedAgentes.Deteniendo,
            estadoEnvio = if (estado.hayEnvioActivo) EstadoEnvioRedAgentes.Cancelado else estado.estadoEnvio,
            resultadoUltimaEjecucion = if (estado.hayEnvioActivo) {
                ResultadoRedAgentes.Cancelado
            } else {
                estado.resultadoUltimaEjecucion
            }
        )
    }

    fun marcarError(codigo: CodigoErrorRedAgentes) {
        estado = estado.copy(
            estadoEnvio = if (estado.hayEnvioActivo) EstadoEnvioRedAgentes.Error else estado.estadoEnvio,
            estadoServidor = if (estado.estadoServidor == EstadoServidorRedAgentes.Iniciando) {
                EstadoServidorRedAgentes.Error
            } else {
                estado.estadoServidor
            },
            resultadoUltimaEjecucion = ResultadoRedAgentes.Error,
            codigoError = codigo
        )
    }

    fun hayRecursosActivos(): Boolean {
        return estado.hayServidorActivo || estado.hayEnvioActivo
    }

    fun puedeIniciarServidor(): Boolean {
        return estado.estadoServidor == EstadoServidorRedAgentes.Inactivo ||
            estado.estadoServidor == EstadoServidorRedAgentes.Detenido ||
            estado.estadoServidor == EstadoServidorRedAgentes.Error
    }

    fun puedeEnviar(): Boolean {
        return estado.estadoServidor == EstadoServidorRedAgentes.Escuchando &&
            !estado.hayEnvioActivo &&
            mensajeValido
    }

    fun puedeDetener(): Boolean {
        return estado.estadoServidor == EstadoServidorRedAgentes.Iniciando ||
            estado.estadoServidor == EstadoServidorRedAgentes.Escuchando ||
            estado.estadoServidor == EstadoServidorRedAgentes.ClienteConectado
    }

}
