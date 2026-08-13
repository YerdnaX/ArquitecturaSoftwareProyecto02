package io.yerdna.architecturasos.carreraboletos

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class EjecutorCarreraBoletos(
    private val callbacks: Callbacks
) {
    interface Callbacks {
        fun onCompradorActualizado(idEjecucion: Int, comprador: CompradorBoleto)
        fun onMetricasActualizadas(idEjecucion: Int, metricas: MetricasCarreraBoletos)
        fun onEvento(idEjecucion: Int, evento: EventoTecnicoCarreraBoletos)
        fun onFinalizada(idEjecucion: Int, resultado: ResultadoCarreraBoletos)
        fun onCancelada(idEjecucion: Int)
        fun onError(idEjecucion: Int, mensaje: String?, throwable: Throwable?)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val cancelada = AtomicBoolean(false)
    private val hilos = mutableListOf<Thread>()
    private var coordinador: Thread? = null

    fun iniciar(
        idEjecucion: Int,
        configuracion: ConfiguracionCarreraBoletos,
        modo: ModoCarreraBoletos
    ) {
        limpiar()
        cancelada.set(false)
        hilos.clear()

        val config = configuracion.normalizada()
        val inicio = SystemClock.elapsedRealtime()
        val recurso = RecursoBoletos(config.boletosIniciales)
        val lock = ReentrantLock()
        val ventasRegistradas = AtomicInteger(0)
        val ventasIncorrectasReportadas = AtomicInteger(0)

        publicarMetricas(idEjecucion, modo, config, recurso, ventasRegistradas.get(), inicio)

        repeat(config.compradoresTotales) { indice ->
            val idComprador = indice + 1
            val nombre = "CarreraBoletos-$idEjecucion-$idComprador"
            val hilo = Thread({
                try {
                    if (modo == ModoCarreraBoletos.ConMutex) {
                        comprarConMutex(
                            idEjecucion = idEjecucion,
                            idComprador = idComprador,
                            nombre = nombre,
                            config = config,
                            recurso = recurso,
                            lock = lock,
                            ventasRegistradas = ventasRegistradas,
                            ventasIncorrectasReportadas = ventasIncorrectasReportadas,
                            inicio = inicio
                        )
                    } else {
                        comprarSinMutex(
                            idEjecucion = idEjecucion,
                            idComprador = idComprador,
                            nombre = nombre,
                            config = config,
                            recurso = recurso,
                            ventasRegistradas = ventasRegistradas,
                            ventasIncorrectasReportadas = ventasIncorrectasReportadas,
                            inicio = inicio
                        )
                    }
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    publicar {
                        callbacks.onCompradorActualizado(
                            idEjecucion,
                            CompradorBoleto(
                                id = idComprador,
                                nombre = nombre,
                                estado = EstadoCompradorBoleto.Cancelado,
                                intentos = 1
                            )
                        )
                    }
                } catch (throwable: Throwable) {
                    cancelada.set(true)
                    publicar {
                        callbacks.onCompradorActualizado(
                            idEjecucion,
                            CompradorBoleto(
                                id = idComprador,
                                nombre = nombre,
                                estado = EstadoCompradorBoleto.Error,
                                intentos = 1,
                                mensaje = throwable.message
                            )
                        )
                        callbacks.onError(idEjecucion, throwable.message, throwable)
                    }
                }
            }, nombre)
            hilos.add(hilo)
        }

        hilos.forEach { it.start() }

        coordinador = Thread({
            try {
                hilos.forEach { it.join() }
                if (cancelada.get()) {
                    publicar { callbacks.onCancelada(idEjecucion) }
                } else {
                    publicar {
                        callbacks.onFinalizada(
                            idEjecucion,
                            crearResultado(
                                modo = modo,
                                config = config,
                                recurso = recurso,
                                ventasRegistradas = ventasRegistradas.get(),
                                inicio = inicio
                            )
                        )
                    }
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                publicar { callbacks.onCancelada(idEjecucion) }
            } catch (throwable: Throwable) {
                cancelada.set(true)
                publicar { callbacks.onError(idEjecucion, throwable.message, throwable) }
            } finally {
                synchronized(hilos) {
                    hilos.clear()
                }
                coordinador = null
            }
        }, "CarreraBoletos-Coordinador-$idEjecucion")
        coordinador?.start()
    }

    fun cancelar() {
        cancelada.set(true)
        synchronized(hilos) {
            hilos.forEach { it.interrupt() }
        }
        coordinador?.interrupt()
    }

    fun limpiar() {
        cancelar()
        val copiaHilos = synchronized(hilos) { hilos.toList() }
        copiaHilos.forEach { hilo ->
            try {
                hilo.join(TIEMPO_ESPERA_LIMPIEZA_MS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        coordinador = null
        synchronized(hilos) {
            hilos.clear()
        }
    }

    private fun comprarSinMutex(
        idEjecucion: Int,
        idComprador: Int,
        nombre: String,
        config: ConfiguracionCarreraBoletos,
        recurso: RecursoBoletos,
        ventasRegistradas: AtomicInteger,
        ventasIncorrectasReportadas: AtomicInteger,
        inicio: Long
    ) {
        if (cancelada.get()) return

        publicarIntento(idEjecucion, idComprador, nombre)
        publicarSeccionCritica(idEjecucion, idComprador, nombre, config, ModoCarreraBoletos.SinMutex, recurso, ventasRegistradas.get(), inicio)

        val boletosLeidos = recurso.boletosDisponibles
        Thread.sleep(PAUSA_CARRERA_MS)
        if (cancelada.get()) return

        if (boletosLeidos > 0) {
            recurso.boletosDisponibles = boletosLeidos - 1
            val ventas = ventasRegistradas.incrementAndGet()
            val incorrectas = maxOf(0, ventas - config.boletosIniciales)
            if (incorrectas > ventasIncorrectasReportadas.get()) {
                ventasIncorrectasReportadas.set(incorrectas)
                publicar { callbacks.onEvento(idEjecucion, EventoTecnicoCarreraBoletos.VentaIncorrectaDetectada(idComprador, ventas)) }
            }
            publicarVenta(idEjecucion, idComprador, nombre, config, ModoCarreraBoletos.SinMutex, recurso, ventas, inicio)
        } else {
            publicarSinBoleto(idEjecucion, idComprador, nombre, config, ModoCarreraBoletos.SinMutex, recurso, ventasRegistradas.get(), inicio)
        }
    }

    private fun comprarConMutex(
        idEjecucion: Int,
        idComprador: Int,
        nombre: String,
        config: ConfiguracionCarreraBoletos,
        recurso: RecursoBoletos,
        lock: ReentrantLock,
        ventasRegistradas: AtomicInteger,
        ventasIncorrectasReportadas: AtomicInteger,
        inicio: Long
    ) {
        if (cancelada.get()) return

        publicarIntento(idEjecucion, idComprador, nombre)
        publicar {
            callbacks.onEvento(idEjecucion, EventoTecnicoCarreraBoletos.CompradorEsperandoMutex(idComprador))
            callbacks.onCompradorActualizado(
                idEjecucion,
                CompradorBoleto(
                    id = idComprador,
                    nombre = nombre,
                    estado = EstadoCompradorBoleto.EsperandoMutex,
                    intentos = 1
                )
            )
        }
        publicarMetricas(idEjecucion, ModoCarreraBoletos.ConMutex, config, recurso, ventasRegistradas.get(), inicio)

        lock.withLock {
            if (cancelada.get()) return

            publicar { callbacks.onEvento(idEjecucion, EventoTecnicoCarreraBoletos.MutexAdquirido(idComprador)) }
            publicarSeccionCritica(idEjecucion, idComprador, nombre, config, ModoCarreraBoletos.ConMutex, recurso, ventasRegistradas.get(), inicio)

            if (recurso.boletosDisponibles > 0) {
                recurso.boletosDisponibles -= 1
                val ventas = ventasRegistradas.incrementAndGet()
                val incorrectas = maxOf(0, ventas - config.boletosIniciales)
                ventasIncorrectasReportadas.set(incorrectas)
                publicarVenta(idEjecucion, idComprador, nombre, config, ModoCarreraBoletos.ConMutex, recurso, ventas, inicio)
            } else {
                publicarSinBoleto(idEjecucion, idComprador, nombre, config, ModoCarreraBoletos.ConMutex, recurso, ventasRegistradas.get(), inicio)
            }

            publicar { callbacks.onEvento(idEjecucion, EventoTecnicoCarreraBoletos.MutexLiberado(idComprador)) }
        }
    }

    private fun publicarIntento(idEjecucion: Int, idComprador: Int, nombre: String) {
        publicar {
            callbacks.onEvento(idEjecucion, EventoTecnicoCarreraBoletos.CompradorIntentandoEntrar(idComprador))
            callbacks.onCompradorActualizado(
                idEjecucion,
                CompradorBoleto(
                    id = idComprador,
                    nombre = nombre,
                    estado = EstadoCompradorBoleto.IntentandoEntrar,
                    intentos = 1
                )
            )
        }
    }

    private fun publicarSeccionCritica(
        idEjecucion: Int,
        idComprador: Int,
        nombre: String,
        config: ConfiguracionCarreraBoletos,
        modo: ModoCarreraBoletos,
        recurso: RecursoBoletos,
        ventasRegistradas: Int,
        inicio: Long
    ) {
        publicar {
            callbacks.onCompradorActualizado(
                idEjecucion,
                CompradorBoleto(
                    id = idComprador,
                    nombre = nombre,
                    estado = EstadoCompradorBoleto.EnSeccionCritica,
                    intentos = 1
                )
            )
            callbacks.onMetricasActualizadas(
                idEjecucion,
                crearMetricas(modo, config, recurso, ventasRegistradas, idComprador.toString(), inicio)
            )
        }
    }

    private fun publicarVenta(
        idEjecucion: Int,
        idComprador: Int,
        nombre: String,
        config: ConfiguracionCarreraBoletos,
        modo: ModoCarreraBoletos,
        recurso: RecursoBoletos,
        ventasRegistradas: Int,
        inicio: Long
    ) {
        publicar {
            callbacks.onEvento(idEjecucion, EventoTecnicoCarreraBoletos.VentaRealizada(idComprador, recurso.boletosDisponibles))
            callbacks.onCompradorActualizado(
                idEjecucion,
                CompradorBoleto(
                    id = idComprador,
                    nombre = nombre,
                    estado = EstadoCompradorBoleto.Compro,
                    intentos = 1,
                    boletosComprados = 1
                )
            )
            callbacks.onMetricasActualizadas(
                idEjecucion,
                crearMetricas(modo, config, recurso, ventasRegistradas, null, inicio)
            )
        }
    }

    private fun publicarSinBoleto(
        idEjecucion: Int,
        idComprador: Int,
        nombre: String,
        config: ConfiguracionCarreraBoletos,
        modo: ModoCarreraBoletos,
        recurso: RecursoBoletos,
        ventasRegistradas: Int,
        inicio: Long
    ) {
        publicar {
            callbacks.onCompradorActualizado(
                idEjecucion,
                CompradorBoleto(
                    id = idComprador,
                    nombre = nombre,
                    estado = EstadoCompradorBoleto.SinBoleto,
                    intentos = 1
                )
            )
            callbacks.onMetricasActualizadas(
                idEjecucion,
                crearMetricas(modo, config, recurso, ventasRegistradas, null, inicio)
            )
        }
    }

    private fun publicarMetricas(
        idEjecucion: Int,
        modo: ModoCarreraBoletos,
        config: ConfiguracionCarreraBoletos,
        recurso: RecursoBoletos,
        ventasRegistradas: Int,
        inicio: Long
    ) {
        publicar {
            callbacks.onMetricasActualizadas(
                idEjecucion,
                crearMetricas(modo, config, recurso, ventasRegistradas, null, inicio)
            )
        }
    }

    private fun crearMetricas(
        modo: ModoCarreraBoletos,
        config: ConfiguracionCarreraBoletos,
        recurso: RecursoBoletos,
        ventasRegistradas: Int,
        compradorEnSeccionCritica: String?,
        inicio: Long
    ): MetricasCarreraBoletos {
        val ventasIncorrectas = maxOf(0, ventasRegistradas - config.boletosIniciales)
        return MetricasCarreraBoletos(
            modo = modo,
            boletosIniciales = config.boletosIniciales,
            boletosRestantes = recurso.boletosDisponibles,
            compradoresTotales = config.compradoresTotales,
            ventasCorrectas = ventasRegistradas.coerceAtMost(config.boletosIniciales),
            ventasIncorrectas = ventasIncorrectas,
            compradorEnSeccionCritica = compradorEnSeccionCritica,
            tiempoTotalMs = SystemClock.elapsedRealtime() - inicio
        )
    }

    private fun crearResultado(
        modo: ModoCarreraBoletos,
        config: ConfiguracionCarreraBoletos,
        recurso: RecursoBoletos,
        ventasRegistradas: Int,
        inicio: Long
    ): ResultadoCarreraBoletos {
        val ventasIncorrectas = maxOf(0, ventasRegistradas - config.boletosIniciales)
        return ResultadoCarreraBoletos(
            modo = modo,
            boletosIniciales = config.boletosIniciales,
            ventasRegistradas = ventasRegistradas,
            boletosRestantes = recurso.boletosDisponibles,
            ventasIncorrectas = ventasIncorrectas,
            huboErrorDeConcurrencia = ventasIncorrectas > 0 || recurso.boletosDisponibles < 0,
            tiempoTotalMs = SystemClock.elapsedRealtime() - inicio
        )
    }

    private fun publicar(accion: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            accion()
        } else {
            mainHandler.post(accion)
        }
    }

    private class RecursoBoletos(
        @Volatile var boletosDisponibles: Int
    )

    companion object {
        private const val PAUSA_CARRERA_MS = 20L
        private const val TIEMPO_ESPERA_LIMPIEZA_MS = 100L
    }
}

sealed class EventoTecnicoCarreraBoletos {
    data class CompradorIntentandoEntrar(val idComprador: Int) : EventoTecnicoCarreraBoletos()
    data class CompradorEsperandoMutex(val idComprador: Int) : EventoTecnicoCarreraBoletos()
    data class MutexAdquirido(val idComprador: Int) : EventoTecnicoCarreraBoletos()
    data class VentaRealizada(val idComprador: Int, val boletosRestantes: Int) : EventoTecnicoCarreraBoletos()
    data class VentaIncorrectaDetectada(val idComprador: Int, val ventasRegistradas: Int) : EventoTecnicoCarreraBoletos()
    data class MutexLiberado(val idComprador: Int) : EventoTecnicoCarreraBoletos()
}
