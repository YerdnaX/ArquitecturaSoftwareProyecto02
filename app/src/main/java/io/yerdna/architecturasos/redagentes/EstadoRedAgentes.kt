package io.yerdna.architecturasos.redagentes

enum class EstadoServidorRedAgentes {
    Inactivo,
    Iniciando,
    Escuchando,
    ClienteConectado,
    Deteniendo,
    Detenido,
    Error
}

enum class EstadoEnvioRedAgentes {
    SinEnvio,
    Conectando,
    Enviando,
    EsperandoRespuesta,
    Exitoso,
    Error,
    Cancelado
}

enum class ResultadoRedAgentes {
    SinEjecucion,
    Exitoso,
    Cancelado,
    Error
}

enum class CodigoErrorRedAgentes {
    MensajeVacio,
    ServidorNoDisponible,
    Timeout,
    MensajeDemasiadoGrande,
    ErrorLectura,
    ErrorEscritura,
    ErrorCierre,
    ErrorDesconocido
}

data class EstadoRedAgentes(
    val host: String = HOST_RED_AGENTES,
    val puerto: Int? = null,
    val estadoServidor: EstadoServidorRedAgentes = EstadoServidorRedAgentes.Inactivo,
    val estadoEnvio: EstadoEnvioRedAgentes = EstadoEnvioRedAgentes.SinEnvio,
    val resultadoUltimaEjecucion: ResultadoRedAgentes = ResultadoRedAgentes.SinEjecucion,
    val horaConexion: Long? = null,
    val mensajeEnviado: String = "",
    val mensajeRecibidoServidor: String = "",
    val respuestaEnviadaServidor: String = "",
    val respuestaRecibidaCliente: String = "",
    val bytesEnviados: Int = 0,
    val bytesRecibidos: Int = 0,
    val tiempoRespuestaMs: Long? = null,
    val codigoError: CodigoErrorRedAgentes? = null
) {
    val hayServidorActivo: Boolean
        get() = estadoServidor == EstadoServidorRedAgentes.Iniciando ||
            estadoServidor == EstadoServidorRedAgentes.Escuchando ||
            estadoServidor == EstadoServidorRedAgentes.ClienteConectado ||
            estadoServidor == EstadoServidorRedAgentes.Deteniendo

    val hayEnvioActivo: Boolean
        get() = estadoEnvio == EstadoEnvioRedAgentes.Conectando ||
            estadoEnvio == EstadoEnvioRedAgentes.Enviando ||
            estadoEnvio == EstadoEnvioRedAgentes.EsperandoRespuesta
}

data class EventoServidorRedAgentes(
    val mensaje: String,
    val respuesta: String,
    val bytesRecibidos: Int,
    val bytesEnviados: Int,
    val timestamp: Long
)

data class ResultadoEnvioRedAgentes(
    val respuesta: String,
    val bytesEnviados: Int,
    val bytesRecibidos: Int,
    val tiempoRespuestaMs: Long,
    val timestamp: Long
)

const val HOST_RED_AGENTES = "127.0.0.1"

// Terminador y limites mantienen el protocolo simple y evitan lecturas indefinidas durante la demo.
const val TERMINADOR_MENSAJE = '\u001E'
const val MAX_CARACTERES_MENSAJE = 240
const val MAX_BYTES_MENSAJE = 1024
const val TIMEOUT_SOCKET_MS = 3000
const val TAG_RED_AGENTES = "OSPlayground/SocketLab"
