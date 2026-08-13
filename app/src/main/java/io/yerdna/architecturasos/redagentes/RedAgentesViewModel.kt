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

    // Actualiza el texto del mensaje quitando el terminador y recortandolo a la longitud maxima permitida
    fun actualizarMensaje(texto: String) {
        textoMensaje = texto
            .filter { it != TERMINADOR_MENSAJE }
            .take(MAX_CARACTERES_MENSAJE)
    }

    // Reinicia el estado dejando al servidor marcado como iniciando
    fun prepararInicioServidor() {
        estado = EstadoRedAgentes(
            estadoServidor = EstadoServidorRedAgentes.Iniciando
        )
    }

    // Cambia el estado del servidor, ignorando transiciones de error a detenido y limpiando el error si corresponde
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

    // Marca el servidor como escuchando en el puerto indicado y limpia cualquier error previo
    fun servidorEscuchando(puerto: Int) {
        estado = estado.copy(
            puerto = puerto,
            estadoServidor = EstadoServidorRedAgentes.Escuchando,
            codigoError = null
        )
    }

    // Valida el mensaje pendiente y, si es valido, deja el estado listo para iniciar el envio
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

    // Cambia el estado del envio y marca el resultado como cancelado si corresponde
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

    // Guarda en el estado el mensaje recibido y la respuesta que el servidor genero
    fun mensajeProcesadoServidor(evento: EventoServidorRedAgentes) {
        estado = estado.copy(
            mensajeRecibidoServidor = evento.mensaje,
            respuestaEnviadaServidor = evento.respuesta,
            bytesRecibidos = evento.bytesRecibidos,
            codigoError = null
        )
    }

    // Registra en el estado el resultado exitoso de un envio y vuelve el servidor a escuchando
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

    // Marca el servidor como deteniendose y cancela el envio si habia uno en curso
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

    // Registra un error en el estado, marcando el envio y/o el servidor segun corresponda
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

    // Indica si hay un servidor o un envio activos en este momento
    fun hayRecursosActivos(): Boolean {
        return estado.hayServidorActivo || estado.hayEnvioActivo
    }

    // Indica si el servidor esta en condiciones de iniciarse
    fun puedeIniciarServidor(): Boolean {
        return estado.estadoServidor == EstadoServidorRedAgentes.Inactivo ||
            estado.estadoServidor == EstadoServidorRedAgentes.Detenido ||
            estado.estadoServidor == EstadoServidorRedAgentes.Error
    }

    // Indica si es posible enviar un mensaje en el estado actual
    fun puedeEnviar(): Boolean {
        return estado.estadoServidor == EstadoServidorRedAgentes.Escuchando &&
            !estado.hayEnvioActivo &&
            mensajeValido
    }

    // Indica si el servidor puede detenerse en el estado actual
    fun puedeDetener(): Boolean {
        return estado.estadoServidor == EstadoServidorRedAgentes.Iniciando ||
            estado.estadoServidor == EstadoServidorRedAgentes.Escuchando ||
            estado.estadoServidor == EstadoServidorRedAgentes.ClienteConectado
    }

}
