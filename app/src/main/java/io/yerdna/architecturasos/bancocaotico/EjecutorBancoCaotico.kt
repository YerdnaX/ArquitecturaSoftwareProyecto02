package io.yerdna.architecturasos.bancocaotico

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

class EjecutorBancoCaotico(
    private val callbacks: Callbacks
) {
    interface Callbacks {
        fun onCajeroActualizado(idEjecucion: Int, cajero: CajeroBancoCaotico)
        fun onEvento(idEjecucion: Int, evento: EventoBancoCaotico)
        fun onFinalizada(idEjecucion: Int, resultado: ResultadoBancoCaotico)
        fun onCancelada(idEjecucion: Int)
        fun onError(idEjecucion: Int, mensaje: String?, throwable: Throwable?)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val cancelada = AtomicBoolean(false)
    private var executorService: ExecutorService? = null
    private var coordinador: Thread? = null

    fun iniciar(idEjecucion: Int, configuracion: ConfiguracionBancoCaotico) {
        limpiar()
        cancelada.set(false)

        val config = configuracion.normalizada()
        val inicio = SystemClock.elapsedRealtime()
        var saldoCompartido = config.saldoInicial

        val executor = Executors.newFixedThreadPool(config.cantidadCajeros)
        executorService = executor
        val futures = mutableListOf<Future<*>>()

        repeat(config.cantidadCajeros) { indice ->
            val idCajero = indice + 1
            val nombre = "BancoCaotico-$idEjecucion-$idCajero"
            futures.add(
                executor.submit {
                    try {
                        publicar {
                            callbacks.onEvento(idEjecucion, EventoBancoCaotico.CajeroIniciado(idCajero))
                            callbacks.onCajeroActualizado(
                                idEjecucion,
                                CajeroBancoCaotico(
                                    id = idCajero,
                                    nombre = nombre,
                                    estado = EstadoCajeroBancoCaotico.Trabajando
                                )
                            )
                        }

                        var operacionesCompletadas = 0
                        var ultimoLeido: Int? = null
                        var ultimoEscrito: Int? = null

                        while (
                            operacionesCompletadas < config.operacionesPorCajero &&
                            !cancelada.get()
                        ) {
                            val saldoLeido = saldoCompartido
                            Thread.yield()
                            val saldoNuevo = saldoLeido + config.montoPorOperacion
                            saldoCompartido = saldoNuevo

                            operacionesCompletadas++
                            ultimoLeido = saldoLeido
                            ultimoEscrito = saldoNuevo

                            if (operacionesCompletadas % TAMANO_REPORTE == 0) {
                                publicarProgreso(
                                    idEjecucion = idEjecucion,
                                    idCajero = idCajero,
                                    nombre = nombre,
                                    operacionesCompletadas = operacionesCompletadas,
                                    ultimoLeido = ultimoLeido,
                                    ultimoEscrito = ultimoEscrito
                                )
                            }
                        }

                        val estadoFinal = if (cancelada.get()) {
                            EstadoCajeroBancoCaotico.Cancelado
                        } else {
                            EstadoCajeroBancoCaotico.Finalizado
                        }
                        publicar {
                            callbacks.onCajeroActualizado(
                                idEjecucion,
                                CajeroBancoCaotico(
                                    id = idCajero,
                                    nombre = nombre,
                                    estado = estadoFinal,
                                    operacionesCompletadas = operacionesCompletadas,
                                    ultimoSaldoLeido = ultimoLeido,
                                    ultimoSaldoEscrito = ultimoEscrito
                                )
                            )
                            callbacks.onEvento(
                                idEjecucion,
                                if (estadoFinal == EstadoCajeroBancoCaotico.Finalizado) {
                                    EventoBancoCaotico.CajeroFinalizado(idCajero)
                                } else {
                                    EventoBancoCaotico.CajeroCancelado(idCajero)
                                }
                            )
                        }
                    } catch (throwable: Throwable) {
                        cancelada.set(true)
                        publicar {
                            callbacks.onCajeroActualizado(
                                idEjecucion,
                                CajeroBancoCaotico(
                                    id = idCajero,
                                    nombre = nombre,
                                    estado = EstadoCajeroBancoCaotico.Error
                                )
                            )
                            callbacks.onError(idEjecucion, throwable.message, throwable)
                        }
                    }
                }
            )
        }

        coordinador = Thread({
            try {
                futures.forEach { it.get() }
                if (cancelada.get()) {
                    publicar { callbacks.onCancelada(idEjecucion) }
                } else {
                    val esperado = config.saldoInicial +
                        (config.cantidadCajeros * config.operacionesPorCajero * config.montoPorOperacion)
                    val real = saldoCompartido
                    val resultado = ResultadoBancoCaotico(
                        saldoInicial = config.saldoInicial,
                        resultadoEsperado = esperado,
                        resultadoReal = real,
                        diferencia = esperado - real,
                        carreraDetectada = esperado != real,
                        duracionMs = SystemClock.elapsedRealtime() - inicio,
                        cajerosFinalizados = config.cantidadCajeros
                    )
                    publicar { callbacks.onFinalizada(idEjecucion, resultado) }
                }
            } catch (interrupted: InterruptedException) {
                Thread.currentThread().interrupt()
                publicar { callbacks.onCancelada(idEjecucion) }
            } catch (throwable: Throwable) {
                cancelada.set(true)
                publicar { callbacks.onError(idEjecucion, throwable.message, throwable) }
            } finally {
                executor.shutdownNow()
                coordinador = null
            }
        }, "BancoCaotico-Coordinador-$idEjecucion")
        coordinador?.start()
    }

    fun cancelar() {
        cancelada.set(true)
        executorService?.shutdownNow()
        coordinador?.interrupt()
    }

    fun limpiar() {
        cancelar()
        executorService = null
        coordinador = null
    }

    private fun publicarProgreso(
        idEjecucion: Int,
        idCajero: Int,
        nombre: String,
        operacionesCompletadas: Int,
        ultimoLeido: Int?,
        ultimoEscrito: Int?
    ) {
        publicar {
            callbacks.onCajeroActualizado(
                idEjecucion,
                CajeroBancoCaotico(
                    id = idCajero,
                    nombre = nombre,
                    estado = EstadoCajeroBancoCaotico.Trabajando,
                    operacionesCompletadas = operacionesCompletadas,
                    ultimoSaldoLeido = ultimoLeido,
                    ultimoSaldoEscrito = ultimoEscrito
                )
            )
            callbacks.onEvento(
                idEjecucion,
                EventoBancoCaotico.ProgresoCajero(idCajero, operacionesCompletadas)
            )
        }
    }

    private fun publicar(accion: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            accion()
        } else {
            mainHandler.post(accion)
        }
    }

    companion object {
        private const val TAMANO_REPORTE = 1_000
    }
}

sealed class EventoBancoCaotico {
    data class CajeroIniciado(val idCajero: Int) : EventoBancoCaotico()
    data class ProgresoCajero(
        val idCajero: Int,
        val operacionesCompletadas: Int
    ) : EventoBancoCaotico()

    data class CajeroFinalizado(val idCajero: Int) : EventoBancoCaotico()
    data class CajeroCancelado(val idCajero: Int) : EventoBancoCaotico()
}
