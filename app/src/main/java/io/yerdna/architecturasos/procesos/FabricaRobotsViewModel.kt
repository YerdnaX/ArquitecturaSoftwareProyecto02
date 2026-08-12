package io.yerdna.architecturasos.procesos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class FabricaRobotsViewModel : ViewModel() {
    var estado by mutableStateOf(EstadoFabricaRobots())
        private set

    var textoCantidadRobots by mutableStateOf(ROBOTS_POR_DEFECTO.toString())
        private set

    val cantidadRobotsValida: Int?
        get() = textoCantidadRobots.toIntOrNull()?.takeIf { it >= ROBOTS_MINIMOS }

    fun actualizarCantidad(texto: String) {
        if (estado.estado == EstadoEjecucionFabrica.Iniciando ||
            estado.estado == EstadoEjecucionFabrica.Ejecucion ||
            estado.estado == EstadoEjecucionFabrica.Deteniendo
        ) {
            return
        }

        textoCantidadRobots = texto.filter { it.isDigit() }
        cantidadRobotsValida?.let { cantidad ->
            estado = estado.copy(robotsConfigurados = cantidad)
        }
    }

    fun prepararInicio(cantidad: Int) {
        estado = estado.copy(
            pidSecundario = null,
            estado = EstadoEjecucionFabrica.Iniciando,
            resultadoUltimaEjecucion = ResultadoUltimaEjecucion.SinEjecucion,
            robotsConfigurados = cantidad,
            robotsEnsamblados = 0,
            mensajesIntercambiados = 0,
            horaInicio = System.currentTimeMillis(),
            horaFin = null,
            mensajeError = null
        )
    }

    fun actualizarPidFabrica(pid: Int, mensajes: Int) {
        estado = estado.copy(
            pidSecundario = pid,
            mensajesIntercambiados = mensajes
        )
    }

    fun actualizarEstadoFabrica(nuevoEstado: EstadoEjecucionFabrica, mensajes: Int) {
        estado = estado.copy(
            estado = nuevoEstado,
            mensajesIntercambiados = mensajes
        )
    }

    fun robotEnsamblado(cantidad: Int, mensajes: Int) {
        estado = estado.copy(
            estado = EstadoEjecucionFabrica.Ejecucion,
            robotsEnsamblados = cantidad,
            mensajesIntercambiados = mensajes
        )
    }

    fun iniciarDetencion() {
        estado = estado.copy(estado = EstadoEjecucionFabrica.Deteniendo)
    }

    fun marcarCancelado() {
        estado = estado.copy(
            estado = EstadoEjecucionFabrica.Inactivo,
            resultadoUltimaEjecucion = ResultadoUltimaEjecucion.Cancelado,
            pidSecundario = null,
            horaFin = System.currentTimeMillis()
        )
    }

    fun marcarCompletado(mensajes: Int) {
        estado = estado.copy(
            estado = EstadoEjecucionFabrica.Inactivo,
            resultadoUltimaEjecucion = ResultadoUltimaEjecucion.Completado,
            pidSecundario = null,
            mensajesIntercambiados = mensajes,
            horaFin = System.currentTimeMillis()
        )
    }

    fun marcarError(mensaje: String, mensajes: Int = estado.mensajesIntercambiados) {
        estado = estado.copy(
            estado = EstadoEjecucionFabrica.Error,
            resultadoUltimaEjecucion = ResultadoUltimaEjecucion.Error,
            pidSecundario = null,
            mensajesIntercambiados = mensajes,
            horaFin = System.currentTimeMillis(),
            mensajeError = mensaje
        )
    }
}
