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
        // Notifica que el estado de un comprador cambió durante la ejecución
        fun onCompradorActualizado(idEjecucion: Int, comprador: CompradorBoleto)
        // Notifica que las métricas de la simulación se actualizaron
        fun onMetricasActualizadas(idEjecucion: Int, metricas: MetricasCarreraBoletos)
        // Notifica que ocurrió un evento técnico durante la ejecución
        fun onEvento(idEjecucion: Int, evento: EventoTecnicoCarreraBoletos)
        // Notifica que la ejecución terminó exitosamente con su resultado
        fun onFinalizada(idEjecucion: Int, resultado: ResultadoCarreraBoletos)
        // Notifica que la ejecución fue cancelada
        fun onCancelada(idEjecucion: Int)
        // Notifica que ocurrió un error durante la ejecución
        fun onError(idEjecucion: Int, mensaje: String?, throwable: Throwable?)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val cancelada = AtomicBoolean(false)
    private val hilos = mutableListOf<Thread>()
    private var coordinador: Thread? = null

    // Lanza un hilo por comprador que intenta comprar un boleto en el modo indicado y coordina su finalización
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
        // Todos los compradores comparten la misma cantidad de boletos.
        val recurso = RecursoBoletos(config.boletosIniciales)

        // Este lock funciona como mutex: permite que solo un comprador a la vez
        // ejecute la sección crítica cuando se selecciona el modo ConMutex.
        val lock = ReentrantLock()
        val ventasRegistradas = AtomicInteger(0)
        val ventasIncorrectasReportadas = AtomicInteger(0)

        publicarMetricas(idEjecucion, modo, config, recurso, ventasRegistradas.get(), inicio)

        // Se crea un Thread por comprador. Todos reciben el mismo recurso y el mismo lock.
        repeat(config.compradoresTotales) { indice ->
            val idComprador = indice + 1
            val nombre = "CarreraBoletos-$idEjecucion-$idComprador"
            // Esta lambda es el trabajo del hilo. Al comenzar, decide si compra
            // usando la versión protegida o la versión que provoca la carrera.
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

        // start() pone en ejecución concurrente a todos los compradores.
        hilos.forEach { it.start() }

        // El coordinador espera a todos los compradores antes de crear el resultado final.
        coordinador = Thread({
            try {
                // join() espera a cada hilo sin detener el hilo principal de la interfaz.
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

    // Marca la ejecución como cancelada e interrumpe todos los hilos en curso
    fun cancelar() {
        cancelada.set(true)
        synchronized(hilos) {
            hilos.forEach { it.interrupt() }
        }
        coordinador?.interrupt()
    }

    // Cancela la ejecución y espera a que los hilos terminen antes de liberar los recursos
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

    // Simula la compra de un boleto sin sincronización, permitiendo que se produzca una condición de carrera
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

        // Sin mutex, varios compradores pueden leer la misma cantidad disponible.
        val boletosLeidos = recurso.boletosDisponibles

        // La pausa permite que otros hilos lean antes de que este comprador escriba,
        // aumentando la posibilidad de vender el mismo boleto más de una vez.
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

    // Simula la compra de un boleto protegiendo el acceso al recurso compartido con un mutex
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

        // withLock adquiere el mutex antes de entrar y lo libera automáticamente al salir.
        // Mientras un comprador está aquí, los demás deben esperar.
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

    // Publica que el comprador está intentando entrar a comprar un boleto
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

    // Publica que el comprador entró a la sección crítica junto con las métricas actuales
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

    // Publica que la venta se realizó con éxito junto con las métricas actualizadas
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

    // Publica que el comprador se quedó sin boleto disponible
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

    // Publica el estado actual de las métricas de la simulación
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

    // Construye un snapshot de las métricas actuales a partir del estado del recurso compartido
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

    // Construye el resultado final de la ejecución, detectando si hubo error de concurrencia
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

    // Ejecuta la acción en el hilo principal, despachándola si es necesario
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
