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
        // Notifica que el estado de un cajero cambió durante la ejecución
        fun onCajeroActualizado(idEjecucion: Int, cajero: CajeroBancoCaotico)
        // Notifica que ocurrió un evento técnico durante la ejecución
        fun onEvento(idEjecucion: Int, evento: EventoBancoCaotico)
        // Notifica que la ejecución terminó exitosamente con su resultado
        fun onFinalizada(idEjecucion: Int, resultado: ResultadoBancoCaotico)
        // Notifica que la ejecución fue cancelada
        fun onCancelada(idEjecucion: Int)
        // Notifica que ocurrió un error durante la ejecución
        fun onError(idEjecucion: Int, mensaje: String?, throwable: Throwable?)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val cancelada = AtomicBoolean(false)
    private var executorService: ExecutorService? = null
    private var coordinador: Thread? = null

    // Lanza los hilos cajero que compiten por leer y escribir el saldo compartido sin sincronización
    fun iniciar(idEjecucion: Int, configuracion: ConfiguracionBancoCaotico) {
        limpiar()
        cancelada.set(false)

        val config = configuracion.normalizada()
        val inicio = SystemClock.elapsedRealtime()
        // Existe un solo saldo para todos los cajeros. Al no estar protegido, varios
        // hilos pueden leerlo y escribirlo al mismo tiempo.
        var saldoCompartido = config.saldoInicial

        // El ExecutorService crea y administra un grupo fijo de hilos. En este ejemplo
        // el grupo tiene la misma cantidad de hilos que cajeros participantes.
        val executor = Executors.newFixedThreadPool(config.cantidadCajeros)
        executorService = executor

        // Cada Future representa una tarea de cajero y luego permite esperar su finalización.
        val futures = mutableListOf<Future<*>>()

        // Se prepara y envía una tarea independiente por cada cajero.
        repeat(config.cantidadCajeros) { indice ->
            val idCajero = indice + 1
            val nombre = "BancoCaotico-$idEjecucion-$idCajero"
            futures.add(
                // submit() entrega el trabajo al grupo. No hace falta llamar a start(),
                // porque el ExecutorService asigna un hilo y comienza la tarea.
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
                            // Sección crítica sin protección: leer, calcular y escribir son
                            // pasos separados. Otro cajero puede entrar entre cualquiera de ellos.
                            val saldoLeido = saldoCompartido

                            // yield() facilita que otro hilo se ejecute después de la lectura,
                            // haciendo más visible la condición de carrera durante la demostración.
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

        // El coordinador espera las tareas y calcula el resultado cuando todas terminan.
        coordinador = Thread({
            try {
                // Future.get() cumple aquí una función parecida a join(): espera una tarea.
                futures.forEach { it.get() }
                if (cancelada.get()) {
                    publicar { callbacks.onCancelada(idEjecucion) }
                } else {
                    // El valor esperado supone que ninguna actualización se perdió.
                    // Si el saldo real es distinto, se comprobó la condición de carrera.
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

    // Marca la ejecución como cancelada e interrumpe los hilos en curso
    fun cancelar() {
        cancelada.set(true)
        executorService?.shutdownNow()
        coordinador?.interrupt()
    }

    // Cancela la ejecución y libera los recursos del ejecutor y del coordinador
    fun limpiar() {
        cancelar()
        executorService = null
        coordinador = null
    }

    // Publica en el hilo principal el progreso parcial de un cajero
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

    // Ejecuta la acción en el hilo principal, despachándola si es necesario
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
