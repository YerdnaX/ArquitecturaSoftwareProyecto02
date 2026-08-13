package io.yerdna.architecturasos.hilos

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean

const val TAG_CARRERA_HILOS = "OSPlayground/ThreadRace"

internal fun operacionesPorHiloCarrera(nivelTrabajo: Int): Long {
    return when (nivelTrabajo.coerceIn(1, 5)) {
        1 -> 500_000L
        2 -> 1_000_000L
        3 -> 2_000_000L
        4 -> 3_500_000L
        else -> 5_000_000L
    }
}

class EjecutorCarreraHilos(
    private val callbacks: Callbacks
) {
    interface Callbacks {
        fun onHiloActualizado(idEjecucion: Int, progreso: ProgresoHiloCarrera)
        fun onEvento(idEjecucion: Int, evento: EventoCarreraHilos)
        fun onFinalizada(idEjecucion: Int, resumen: ResumenEjecucionCarrera)
        fun onError(idEjecucion: Int, idHilo: Int, mensaje: String?, throwable: Throwable?)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val cancelada = AtomicBoolean(false)
    private val hilos = mutableListOf<Thread>()
    private var coordinador: Thread? = null

    fun iniciar(idEjecucion: Int, configuracion: ConfiguracionCarreraHilos) {
        cancelar()
        cancelada.set(false)
        hilos.clear()

        val config = configuracion.normalizada()
        val inicioTotal = SystemClock.elapsedRealtime()
        val operacionesPorHilo = operacionesPorHiloCarrera(config.cantidadTrabajo)

        repeat(config.cantidadHilos) { indice ->
            val idHilo = indice + 1
            val nombreHilo = "ThreadRace-$idEjecucion-$idHilo"
            val hilo = Thread({
                ejecutarHilo(
                    idEjecucion = idEjecucion,
                    idHilo = idHilo,
                    nombreHilo = nombreHilo,
                    operacionesTotales = operacionesPorHilo
                )
            }, nombreHilo)
            hilos.add(hilo)
        }

        hilos.forEach { it.start() }

        coordinador = Thread({
            try {
                hilos.forEach { it.join() }
                val tiempoTotalMs = SystemClock.elapsedRealtime() - inicioTotal
                val estadoFinal = if (cancelada.get()) {
                    EstadoCarreraHilos.Cancelada
                } else {
                    EstadoCarreraHilos.Exitosa
                }
                publicar {
                    callbacks.onFinalizada(
                        idEjecucion,
                        ResumenEjecucionCarrera(
                            estadoFinal = estadoFinal,
                            tiempoTotalMs = tiempoTotalMs
                        )
                    )
                }
            } catch (exception: InterruptedException) {
                Thread.currentThread().interrupt()
                publicar {
                    callbacks.onFinalizada(
                        idEjecucion,
                        ResumenEjecucionCarrera(
                            estadoFinal = EstadoCarreraHilos.Cancelada,
                            tiempoTotalMs = SystemClock.elapsedRealtime() - inicioTotal,
                            mensaje = exception.message
                        )
                    )
                }
            } finally {
                hilos.clear()
                coordinador = null
            }
        }, "ThreadRace-Coordinador-$idEjecucion")
        coordinador?.start()
    }

    fun cancelar() {
        cancelada.set(true)
    }

    private fun ejecutarHilo(
        idEjecucion: Int,
        idHilo: Int,
        nombreHilo: String,
        operacionesTotales: Long
    ) {
        val inicio = SystemClock.elapsedRealtime()
        var checksum = 17L
        var completadas = 0L

        publicar {
            callbacks.onEvento(idEjecucion, EventoCarreraHilos.HiloIniciado(idHilo))
            callbacks.onHiloActualizado(
                idEjecucion,
                ProgresoHiloCarrera(
                    idHilo = idHilo,
                    nombreHilo = nombreHilo,
                    estado = EstadoHiloCarrera.Ejecutando,
                    operacionesTotales = operacionesTotales
                )
            )
        }

        try {
            while (completadas < operacionesTotales && !cancelada.get()) {
                val limiteBloque = (completadas + TAMANO_BLOQUE).coerceAtMost(operacionesTotales)
                while (completadas < limiteBloque) {
                    checksum = procesarNumero(checksum, completadas + idHilo)
                    completadas++
                }

                val tiempoMs = SystemClock.elapsedRealtime() - inicio
                publicar {
                    callbacks.onHiloActualizado(
                        idEjecucion,
                        ProgresoHiloCarrera(
                            idHilo = idHilo,
                            nombreHilo = nombreHilo,
                            estado = EstadoHiloCarrera.Ejecutando,
                            progreso = (completadas.toFloat() / operacionesTotales).coerceIn(0f, 1f),
                            operacionesCompletadas = completadas,
                            operacionesTotales = operacionesTotales,
                            tiempoMs = tiempoMs
                        )
                    )
                }
            }

            val estadoFinal = if (cancelada.get()) EstadoHiloCarrera.Cancelado else EstadoHiloCarrera.Finalizado
            val tiempoMs = SystemClock.elapsedRealtime() - inicio
            publicar {
                callbacks.onHiloActualizado(
                    idEjecucion,
                    ProgresoHiloCarrera(
                        idHilo = idHilo,
                        nombreHilo = nombreHilo,
                        estado = estadoFinal,
                        progreso = if (estadoFinal == EstadoHiloCarrera.Finalizado) 1f else {
                            (completadas.toFloat() / operacionesTotales).coerceIn(0f, 1f)
                        },
                        operacionesCompletadas = completadas,
                        operacionesTotales = operacionesTotales,
                        tiempoMs = tiempoMs,
                        mensaje = checksum.toString()
                    )
                )
                callbacks.onEvento(
                    idEjecucion,
                    if (estadoFinal == EstadoHiloCarrera.Finalizado) {
                        EventoCarreraHilos.HiloFinalizado(idHilo)
                    } else {
                        EventoCarreraHilos.HiloCancelado(idHilo)
                    }
                )
            }
        } catch (exception: Throwable) {
            publicar {
                callbacks.onHiloActualizado(
                    idEjecucion,
                    ProgresoHiloCarrera(
                        idHilo = idHilo,
                        nombreHilo = nombreHilo,
                        estado = EstadoHiloCarrera.Error,
                        progreso = (completadas.toFloat() / operacionesTotales).coerceIn(0f, 1f),
                        operacionesCompletadas = completadas,
                        operacionesTotales = operacionesTotales,
                        tiempoMs = SystemClock.elapsedRealtime() - inicio,
                        mensaje = exception.message
                    )
                )
                callbacks.onError(idEjecucion, idHilo, exception.message, exception)
            }
            cancelada.set(true)
        }
    }

    private fun publicar(accion: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            accion()
        } else {
            mainHandler.post(accion)
        }
    }

    private fun procesarNumero(checksumActual: Long, numero: Long): Long {
        var valor = checksumActual xor numero
        valor = (valor * 31L + numero * 17L) % 1_000_000_007L
        if (valor % 3L == 0L) {
            valor += numero % 97L
        } else {
            valor -= numero % 53L
        }
        return valor xor (valor shl 7)
    }

    companion object {
        private const val TAMANO_BLOQUE = 25_000L
    }
}

sealed class EventoCarreraHilos {
    data class HiloIniciado(val idHilo: Int) : EventoCarreraHilos()
    data class HiloFinalizado(val idHilo: Int) : EventoCarreraHilos()
    data class HiloCancelado(val idHilo: Int) : EventoCarreraHilos()
}

data class ResumenEjecucionCarrera(
    val estadoFinal: EstadoCarreraHilos,
    val tiempoTotalMs: Long,
    val mensaje: String? = null
)
