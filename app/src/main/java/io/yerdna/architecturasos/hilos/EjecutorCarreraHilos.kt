package io.yerdna.architecturasos.hilos

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean

const val TAG_CARRERA_HILOS = "OSPlayground/ThreadRace"

// Determina cuántas operaciones debe realizar cada hilo según el nivel de trabajo configurado.
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
        // Notifica que el progreso de un hilo específico cambió.
        fun onHiloActualizado(idEjecucion: Int, progreso: ProgresoHiloCarrera)
        // Notifica un evento ocurrido durante la ejecución de un hilo.
        fun onEvento(idEjecucion: Int, evento: EventoCarreraHilos)
        // Notifica que la ejecución completa de la carrera de hilos finalizó.
        fun onFinalizada(idEjecucion: Int, resumen: ResumenEjecucionCarrera)
        // Notifica que ocurrió un error durante la ejecución de un hilo.
        fun onError(idEjecucion: Int, idHilo: Int, mensaje: String?, throwable: Throwable?)
    }

    // El Handler permite enviar a la interfaz los cambios producidos por los hilos trabajadores.
    private val mainHandler = Handler(Looper.getMainLooper())

    // Todos los hilos consultan esta bandera para saber si deben detener su trabajo.
    private val cancelada = AtomicBoolean(false)

    // Aquí se guardan los Thread creados para poder iniciarlos, esperarlos y cancelarlos.
    private val hilos = mutableListOf<Thread>()
    private var coordinador: Thread? = null

    // Crea y lanza los hilos de trabajo junto con un hilo coordinador que espera a que todos terminen.
    fun iniciar(idEjecucion: Int, configuracion: ConfiguracionCarreraHilos) {
        cancelar()
        cancelada.set(false)
        hilos.clear()

        val config = configuracion.normalizada()
        val inicioTotal = SystemClock.elapsedRealtime()
        val operacionesPorHilo = operacionesPorHiloCarrera(config.cantidadTrabajo)

        // Se crea un objeto Thread por cada participante. La lambda entregada a Thread
        // indica que ejecutarHilo() será el trabajo que realizará cuando se llame a start().
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

        // Crear un Thread no lo pone a trabajar. start() inicia cada hilo de verdad.
        hilos.forEach { it.start() }

        // Este hilo adicional no hace cálculos: coordina la carrera y espera a los trabajadores.
        coordinador = Thread({
            try {
                // join() bloquea solamente al coordinador hasta que todos los hilos terminen.
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

    // Marca la ejecución como cancelada para que los hilos en curso se detengan.
    fun cancelar() {
        cancelada.set(true)
    }

    // Ejecuta el trabajo simulado de un hilo, publicando su progreso periódicamente por bloques.
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
            // El trabajo se divide en bloques para no intentar actualizar la pantalla
            // después de cada una de las millones de operaciones realizadas.
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

    // Ejecuta la acción en el hilo principal, directamente si ya está en él o mediante el handler.
    private fun publicar(accion: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            accion()
        } else {
            mainHandler.post(accion)
        }
    }

    // Realiza un cálculo aritmético simulado usado para generar carga de trabajo en cada operación.
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
