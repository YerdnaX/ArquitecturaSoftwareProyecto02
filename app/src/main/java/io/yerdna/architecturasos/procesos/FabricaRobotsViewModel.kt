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

    // Actualiza el texto de cantidad de robots ingresado, validando que no haya una ejecución en curso.
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

    // Reinicia el estado para comenzar una nueva ejecución con la cantidad de robots indicada.
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

    // Registra el PID del proceso secundario recibido junto con el conteo de mensajes.
    fun actualizarPidFabrica(pid: Int, mensajes: Int) {
        estado = estado.copy(
            pidSecundario = pid,
            mensajesIntercambiados = mensajes
        )
    }

    // Actualiza el estado de ejecución de la fábrica con el nuevo valor recibido.
    fun actualizarEstadoFabrica(nuevoEstado: EstadoEjecucionFabrica, mensajes: Int) {
        estado = estado.copy(
            estado = nuevoEstado,
            mensajesIntercambiados = mensajes
        )
    }

    // Registra que se ensambló un nuevo robot y actualiza el contador correspondiente.
    fun robotEnsamblado(cantidad: Int, mensajes: Int) {
        estado = estado.copy(
            estado = EstadoEjecucionFabrica.Ejecucion,
            robotsEnsamblados = cantidad,
            mensajesIntercambiados = mensajes
        )
    }

    // Marca el estado como en proceso de detención.
    fun iniciarDetencion() {
        estado = estado.copy(estado = EstadoEjecucionFabrica.Deteniendo)
    }

    // Marca la ejecución como cancelada y limpia los datos del proceso secundario.
    fun marcarCancelado() {
        estado = estado.copy(
            estado = EstadoEjecucionFabrica.Inactivo,
            resultadoUltimaEjecucion = ResultadoUltimaEjecucion.Cancelado,
            pidSecundario = null,
            horaFin = System.currentTimeMillis()
        )
    }

    // Marca la ejecución como completada exitosamente.
    fun marcarCompletado(mensajes: Int) {
        estado = estado.copy(
            estado = EstadoEjecucionFabrica.Inactivo,
            resultadoUltimaEjecucion = ResultadoUltimaEjecucion.Completado,
            pidSecundario = null,
            mensajesIntercambiados = mensajes,
            horaFin = System.currentTimeMillis()
        )
    }

    // Marca la ejecución como fallida con el mensaje de error indicado.
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
