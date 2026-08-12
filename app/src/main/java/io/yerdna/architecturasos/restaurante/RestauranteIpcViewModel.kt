package io.yerdna.architecturasos.restaurante

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class RestauranteIpcViewModel : ViewModel() {
    var estado by mutableStateOf(EstadoRestauranteIpc())
        private set

    var textoOrden by mutableStateOf("")
        private set

    val ordenValida: Boolean
        get() = textoOrden.trim().isNotEmpty()

    private var siguienteIdOrden = 1

    fun actualizarOrden(texto: String) {
        textoOrden = texto
    }

    fun cargarOrdenRapida(orden: String) {
        textoOrden = orden
    }

    fun prepararConexion() {
        estado = estado.copy(
            estadoConexion = EstadoConexionRestaurante.Conectando,
            mensajeError = null,
            timestampUltimoEvento = System.currentTimeMillis()
        )
    }

    fun cocinaConectada(pidCocina: Int, timestamp: Long, mensajes: Int) {
        estado = estado.copy(
            pidCocina = pidCocina,
            estadoConexion = EstadoConexionRestaurante.Conectado,
            mensajeError = null,
            timestampUltimoEvento = timestamp,
            mensajesIntercambiados = mensajes
        )
    }

    fun prepararEnvioOrden(): OrdenPreparada? {
        val orden = textoOrden.trim()
        if (orden.isEmpty() || estado.estadoConexion != EstadoConexionRestaurante.Conectado) {
            return null
        }

        val idOrden = siguienteIdOrden
        siguienteIdOrden += 1
        val timestamp = System.currentTimeMillis()
        estado = estado.copy(
            estadoOrden = EstadoOrdenRestaurante.PreparandoMensaje,
            resultadoUltimaEjecucion = ResultadoRestauranteIpc.SinEjecucion,
            ordenActual = orden,
            respuestaActual = "",
            idOrdenActual = idOrden,
            timestampUltimoEvento = timestamp,
            mensajeError = null
        )
        return OrdenPreparada(
            id = idOrden,
            texto = orden,
            pidOrigen = estado.pidMesero
        )
    }

    fun marcarOrdenEnviada() {
        estado = estado.copy(
            estadoOrden = EstadoOrdenRestaurante.Enviando,
            timestampUltimoEvento = System.currentTimeMillis(),
            mensajesIntercambiados = estado.mensajesIntercambiados + 1
        )
    }

    fun marcarOrdenEnCola(evento: EventoOrdenRestaurante) {
        estado = estado.copy(
            pidCocina = evento.pidDestino,
            estadoOrden = if (estado.estadoOrden == EstadoOrdenRestaurante.SinOrden) {
                EstadoOrdenRestaurante.EnCola
            } else {
                estado.estadoOrden
            },
            timestampUltimoEvento = evento.timestamp,
            ordenesEnCola = evento.ordenesEnCola,
            totalOrdenesProcesadas = evento.totalProcesadas,
            mensajesIntercambiados = evento.mensajesIntercambiados,
            mensajeError = null
        )
    }

    fun marcarOrdenRecibida(evento: EventoOrdenRestaurante) {
        estado = estado.copy(
            pidCocina = evento.pidDestino,
            estadoOrden = EstadoOrdenRestaurante.Recibido,
            ordenActual = evento.orden,
            idOrdenActual = evento.idOrden,
            timestampUltimoEvento = evento.timestamp,
            ordenesEnCola = evento.ordenesEnCola,
            totalOrdenesProcesadas = evento.totalProcesadas,
            mensajesIntercambiados = evento.mensajesIntercambiados,
            mensajeError = null
        )
    }

    fun marcarOrdenProcesando(evento: EventoOrdenRestaurante) {
        estado = estado.copy(
            pidCocina = evento.pidDestino,
            estadoOrden = EstadoOrdenRestaurante.Procesando,
            ordenActual = evento.orden,
            idOrdenActual = evento.idOrden,
            timestampUltimoEvento = evento.timestamp,
            ordenesEnCola = evento.ordenesEnCola,
            totalOrdenesProcesadas = evento.totalProcesadas,
            mensajesIntercambiados = evento.mensajesIntercambiados,
            mensajeError = null
        )
    }

    fun marcarRespuestaRecibida(evento: EventoOrdenRestaurante) {
        estado = estado.copy(
            pidCocina = evento.pidDestino,
            estadoOrden = EstadoOrdenRestaurante.RespuestaRecibida,
            resultadoUltimaEjecucion = ResultadoRestauranteIpc.Completado,
            ordenActual = evento.orden,
            respuestaActual = evento.respuesta,
            idOrdenActual = evento.idOrden,
            timestampUltimoEvento = evento.timestamp,
            ordenesEnCola = evento.ordenesEnCola,
            totalOrdenesProcesadas = evento.totalProcesadas,
            mensajesIntercambiados = evento.mensajesIntercambiados,
            mensajeError = null
        )
    }

    fun marcarDesconectado(cancelado: Boolean = false) {
        estado = estado.copy(
            pidCocina = null,
            estadoConexion = EstadoConexionRestaurante.Desconectado,
            estadoOrden = EstadoOrdenRestaurante.SinOrden,
            resultadoUltimaEjecucion = if (cancelado) {
                ResultadoRestauranteIpc.Cancelado
            } else {
                estado.resultadoUltimaEjecucion
            },
            ordenActual = "",
            respuestaActual = "",
            idOrdenActual = null,
            timestampUltimoEvento = System.currentTimeMillis(),
            mensajeError = null,
            mensajesIntercambiados = 0,
            ordenesEnCola = 0,
            totalOrdenesProcesadas = 0
        )
    }

    fun limpiarDatos() {
        estado = EstadoRestauranteIpc(pidMesero = estado.pidMesero)
    }

    fun marcarError(mensaje: String, rompeConexion: Boolean, mensajes: Int = estado.mensajesIntercambiados) {
        estado = estado.copy(
            estadoConexion = if (rompeConexion) EstadoConexionRestaurante.Error else estado.estadoConexion,
            estadoOrden = if (rompeConexion) estado.estadoOrden else EstadoOrdenRestaurante.Error,
            resultadoUltimaEjecucion = ResultadoRestauranteIpc.Error,
            mensajeError = mensaje.ifBlank { "Error de comunicacion con la cocina" },
            mensajesIntercambiados = mensajes,
            timestampUltimoEvento = System.currentTimeMillis()
        )
    }

    fun hayConexionActiva(): Boolean {
        return estado.estadoConexion == EstadoConexionRestaurante.Conectando ||
            estado.estadoConexion == EstadoConexionRestaurante.Conectado
    }

    fun hayTrabajoPendiente(): Boolean {
        return estado.estadoOrden == EstadoOrdenRestaurante.PreparandoMensaje ||
            estado.estadoOrden == EstadoOrdenRestaurante.Enviando ||
            estado.estadoOrden == EstadoOrdenRestaurante.EnCola ||
            estado.estadoOrden == EstadoOrdenRestaurante.Recibido ||
            estado.estadoOrden == EstadoOrdenRestaurante.Procesando ||
            estado.estadoOrden == EstadoOrdenRestaurante.RespuestaEnviada ||
            estado.ordenesEnCola > 0
    }

    fun hayDatosPrevios(): Boolean {
        return estado.ordenActual.isNotBlank() ||
            estado.respuestaActual.isNotBlank() ||
            estado.pidCocina != null ||
            estado.mensajesIntercambiados > 0 ||
            estado.totalOrdenesProcesadas > 0 ||
            estado.mensajeError != null
    }
}

data class OrdenPreparada(
    val id: Int,
    val texto: String,
    val pidOrigen: Int
)
