package io.yerdna.architecturasos.hilos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class CarreraHilosViewModel : ViewModel() {
    var estado by mutableStateOf(EstadoCarreraHilos.Inactiva)
        private set

    var configuracion by mutableStateOf(ConfiguracionCarreraHilos())
        private set

    var progresos by mutableStateOf(progresosIniciales(configuracion))
        private set

    var tiempoTotalMs by mutableStateOf(0L)
        private set

    var resultadoUltimaEjecucion by mutableStateOf<ResultadoCarreraHilos?>(null)
        private set

    var historial by mutableStateOf(emptyList<ResultadoCarreraHilos>())
        private set

    private var proximoIdEjecucion = 1
    private var idEjecucionActiva: Int? = null
    private var ejecutor: EjecutorCarreraHilos? = null
    private var registrarEvento: ((EventoRegistroCarreraHilos) -> Unit)? = null

    fun actualizarCantidadHilos(cantidad: Int) {
        if (estado == EstadoCarreraHilos.Ejecutando) return
        configuracion = configuracion.copy(cantidadHilos = cantidad).normalizada()
        progresos = progresosIniciales(configuracion)
    }

    fun actualizarCantidadTrabajo(cantidad: Int) {
        if (estado == EstadoCarreraHilos.Ejecutando) return
        configuracion = configuracion.copy(cantidadTrabajo = cantidad).normalizada()
    }

    fun iniciar(onEvento: (EventoRegistroCarreraHilos) -> Unit) {
        if (!puedeIniciar()) return

        val idEjecucion = proximoIdEjecucion++
        val config = configuracion.normalizada()
        idEjecucionActiva = idEjecucion
        registrarEvento = onEvento
        estado = EstadoCarreraHilos.Ejecutando
        tiempoTotalMs = 0L
        resultadoUltimaEjecucion = null
        progresos = progresosIniciales(config)

        emitir(EventoRegistroCarreraHilos.EjecucionIniciada(idEjecucion))
        emitir(EventoRegistroCarreraHilos.CantidadHilos(config.cantidadHilos))
        emitir(EventoRegistroCarreraHilos.CantidadTrabajo(config.cantidadTrabajo))

        ejecutor = EjecutorCarreraHilos(
            callbacks = object : EjecutorCarreraHilos.Callbacks {
                override fun onHiloActualizado(idEjecucion: Int, progreso: ProgresoHiloCarrera) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    progresos = progresos.map {
                        if (it.idHilo == progreso.idHilo) progreso else it
                    }
                }

                override fun onEvento(idEjecucion: Int, evento: EventoCarreraHilos) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    when (evento) {
                        is EventoCarreraHilos.HiloIniciado -> emitir(
                            EventoRegistroCarreraHilos.HiloIniciado(evento.idHilo)
                        )

                        is EventoCarreraHilos.HiloFinalizado -> emitir(
                            EventoRegistroCarreraHilos.HiloFinalizado(evento.idHilo)
                        )

                        is EventoCarreraHilos.HiloCancelado -> emitir(
                            EventoRegistroCarreraHilos.HiloCancelado(evento.idHilo)
                        )
                    }
                }

                override fun onFinalizada(idEjecucion: Int, resumen: ResumenEjecucionCarrera) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    val resumenFinal = if (estado == EstadoCarreraHilos.Error) {
                        resumen.copy(estadoFinal = EstadoCarreraHilos.Error)
                    } else {
                        resumen
                    }
                    cerrarEjecucion(idEjecucion, resumenFinal)
                }

                override fun onError(idEjecucion: Int, idHilo: Int, mensaje: String?, throwable: Throwable?) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    ejecutor?.cancelar()
                    estado = EstadoCarreraHilos.Error
                    emitir(EventoRegistroCarreraHilos.ErrorTecnico(idHilo, mensaje))
                }
            }
        )
        ejecutor?.iniciar(idEjecucion, config)
    }

    fun cancelar() {
        if (estado != EstadoCarreraHilos.Ejecutando) return
        ejecutor?.cancelar()
    }

    fun reiniciar() {
        if (estado == EstadoCarreraHilos.Ejecutando) return
        estado = EstadoCarreraHilos.Inactiva
        tiempoTotalMs = 0L
        resultadoUltimaEjecucion = null
        progresos = progresosIniciales(configuracion)
    }

    fun limpiarRecursos() {
        ejecutor?.cancelar()
        ejecutor = null
        idEjecucionActiva = null
        registrarEvento = null
    }

    fun puedeIniciar(): Boolean {
        return estado == EstadoCarreraHilos.Inactiva ||
            estado == EstadoCarreraHilos.Exitosa ||
            estado == EstadoCarreraHilos.Cancelada ||
            estado == EstadoCarreraHilos.Error
    }

    fun puedeCancelar(): Boolean {
        return estado == EstadoCarreraHilos.Ejecutando
    }

    fun puedeReiniciar(): Boolean {
        return estado != EstadoCarreraHilos.Ejecutando
    }

    fun hayEjecucionActiva(): Boolean {
        return estado == EstadoCarreraHilos.Ejecutando
    }

    fun mejorTiempoMismaCantidad(): Long? {
        return historial
            .filter {
                it.estadoFinal == EstadoCarreraHilos.Exitosa &&
                    it.cantidadHilos == configuracion.cantidadHilos
            }
            .minOfOrNull { it.tiempoTotalMs }
    }

    private fun cerrarEjecucion(idEjecucion: Int, resumen: ResumenEjecucionCarrera) {
        val finalizados = progresos.count { it.estado == EstadoHiloCarrera.Finalizado }
        val cancelados = progresos.count { it.estado == EstadoHiloCarrera.Cancelado }
        val operacionesTotales = progresos.sumOf { it.operacionesTotales }
        val resultado = ResultadoCarreraHilos(
            idEjecucion = idEjecucion,
            cantidadHilos = configuracion.cantidadHilos,
            cantidadTrabajo = configuracion.cantidadTrabajo,
            estadoFinal = resumen.estadoFinal,
            tiempoTotalMs = resumen.tiempoTotalMs,
            operacionesTotales = operacionesTotales,
            finalizados = finalizados,
            cancelados = cancelados,
            mensaje = resumen.mensaje
        )

        estado = resumen.estadoFinal
        tiempoTotalMs = resumen.tiempoTotalMs
        resultadoUltimaEjecucion = resultado
        historial = historial + resultado
        idEjecucionActiva = null
        ejecutor = null

        if (resumen.estadoFinal == EstadoCarreraHilos.Exitosa) {
            emitir(EventoRegistroCarreraHilos.EjecucionFinalizada(resumen.tiempoTotalMs))
        } else {
            emitir(EventoRegistroCarreraHilos.EjecucionCancelada)
        }
    }

    private fun emitir(evento: EventoRegistroCarreraHilos) {
        registrarEvento?.invoke(evento)
    }

    private fun esEjecucionActiva(idEjecucion: Int): Boolean {
        return idEjecucionActiva == idEjecucion
    }

    override fun onCleared() {
        limpiarRecursos()
        super.onCleared()
    }
}

private fun progresosIniciales(configuracion: ConfiguracionCarreraHilos): List<ProgresoHiloCarrera> {
    val operaciones = operacionesPorHiloCarrera(configuracion.cantidadTrabajo)
    return (1..configuracion.cantidadHilos).map { id ->
        ProgresoHiloCarrera(
            idHilo = id,
            nombreHilo = "Hilo $id",
            operacionesTotales = operaciones
        )
    }
}

sealed class EventoRegistroCarreraHilos {
    data class EjecucionIniciada(val idEjecucion: Int) : EventoRegistroCarreraHilos()
    data class CantidadHilos(val cantidad: Int) : EventoRegistroCarreraHilos()
    data class CantidadTrabajo(val cantidad: Int) : EventoRegistroCarreraHilos()
    data class HiloIniciado(val idHilo: Int) : EventoRegistroCarreraHilos()
    data class HiloFinalizado(val idHilo: Int) : EventoRegistroCarreraHilos()
    data class HiloCancelado(val idHilo: Int) : EventoRegistroCarreraHilos()
    data class EjecucionFinalizada(val tiempoMs: Long) : EventoRegistroCarreraHilos()
    data object EjecucionCancelada : EventoRegistroCarreraHilos()
    data class ErrorTecnico(val idHilo: Int, val mensaje: String?) : EventoRegistroCarreraHilos()
}
