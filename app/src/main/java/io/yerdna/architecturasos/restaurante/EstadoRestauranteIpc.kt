package io.yerdna.architecturasos.restaurante

import android.os.Process

enum class EstadoConexionRestaurante {
    Desconectado,
    Conectando,
    Conectado,
    Error
}

enum class EstadoOrdenRestaurante {
    SinOrden,
    PreparandoMensaje,
    Enviando,
    EnCola,
    Recibido,
    Procesando,
    RespuestaEnviada,
    RespuestaRecibida,
    Error
}

enum class ResultadoRestauranteIpc {
    SinEjecucion,
    Completado,
    Cancelado,
    Error
}

data class EstadoRestauranteIpc(
    val pidMesero: Int = Process.myPid(),
    val pidCocina: Int? = null,
    val estadoConexion: EstadoConexionRestaurante = EstadoConexionRestaurante.Desconectado,
    val estadoOrden: EstadoOrdenRestaurante = EstadoOrdenRestaurante.SinOrden,
    val resultadoUltimaEjecucion: ResultadoRestauranteIpc = ResultadoRestauranteIpc.SinEjecucion,
    val ordenActual: String = "",
    val respuestaActual: String = "",
    val idOrdenActual: Int? = null,
    val timestampUltimoEvento: Long? = null,
    val mensajeError: String? = null,
    val mensajesIntercambiados: Int = 0,
    val ordenesEnCola: Int = 0,
    val totalOrdenesProcesadas: Int = 0
)

data class OrdenRestaurante(
    val id: Int,
    val texto: String,
    val pidOrigen: Int,
    val timestamp: Long
)
