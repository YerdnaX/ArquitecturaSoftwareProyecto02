package io.yerdna.architecturasos.parqueointeligente

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class EjecutorParqueoInteligente(
    private val callbacks: Callbacks
) {
    interface Callbacks {
        // Notifica que el estado de un vehiculo cambio durante la ejecucion
        fun onVehiculoActualizado(idEjecucion: Int, vehiculo: VehiculoParqueoInteligente)
        // Notifica que las metricas globales de la ejecucion se actualizaron
        fun onMetricasActualizadas(idEjecucion: Int, metricas: MetricasParqueoInteligente)
        // Notifica un nuevo evento de registro producido durante la ejecucion
        fun onEvento(idEjecucion: Int, evento: EventoRegistroParqueoInteligente)
        // Notifica que la ejecucion termino con exito e informa el resultado final
        fun onFinalizada(idEjecucion: Int, resultado: ResultadoParqueoInteligente)
        // Notifica que la ejecucion fue cancelada
        fun onCancelada(idEjecucion: Int)
        // Notifica que ocurrio un error tecnico durante la ejecucion
        fun onError(idEjecucion: Int, mensaje: String?, throwable: Throwable?)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val cancelada = AtomicBoolean(false)
    private var executorService: ExecutorService? = null
    private var coordinador: Thread? = null

    // Arranca una nueva ejecucion de la simulacion de parqueo con un vehiculo por hilo compitiendo por permisos del semaforo
    fun iniciar(
        idEjecucion: Int,
        configuracion: ConfiguracionParqueoInteligente
    ) {
        limpiar()
        cancelada.set(false)

        val config = configuracion.normalizada()
        val inicio = SystemClock.elapsedRealtime()
        val semaforo = Semaphore(config.espaciosDisponibles)
        val permisosDisponibles = AtomicInteger(config.espaciosDisponibles)
        val recursosOcupados = AtomicInteger(0)
        val hilosEsperando = AtomicInteger(0)
        val maximoSimultaneos = AtomicInteger(0)
        val vehiculosFinalizados = AtomicInteger(0)
        val vehiculosCancelados = AtomicInteger(0)
        val vehiculosConError = AtomicInteger(0)

        val pool = Executors.newFixedThreadPool(config.vehiculosTotales)
        executorService = pool

        publicarMetricas(
            idEjecucion, config, permisosDisponibles, recursosOcupados, hilosEsperando,
            maximoSimultaneos, vehiculosFinalizados, vehiculosCancelados, vehiculosConError, inicio
        )

        repeat(config.vehiculosTotales) { indice ->
            val idVehiculo = indice + 1
            val nombre = "ParqueoInteligente-$idEjecucion-$idVehiculo"
            pool.submit {
                ejecutarVehiculo(
                    idEjecucion = idEjecucion,
                    idVehiculo = idVehiculo,
                    nombre = nombre,
                    semaforo = semaforo,
                    config = config,
                    permisosDisponibles = permisosDisponibles,
                    recursosOcupados = recursosOcupados,
                    hilosEsperando = hilosEsperando,
                    maximoSimultaneos = maximoSimultaneos,
                    vehiculosFinalizados = vehiculosFinalizados,
                    vehiculosCancelados = vehiculosCancelados,
                    vehiculosConError = vehiculosConError,
                    inicio = inicio
                )
            }
        }

        pool.shutdown()

        coordinador = Thread({
            try {
                pool.awaitTermination(TIEMPO_MAXIMO_ESPERA_MS, TimeUnit.MILLISECONDS)
                if (cancelada.get()) {
                    publicar { callbacks.onCancelada(idEjecucion) }
                } else {
                    publicar {
                        callbacks.onFinalizada(
                            idEjecucion,
                            ResultadoParqueoInteligente(
                                permisosTotales = config.espaciosDisponibles,
                                vehiculosTotales = config.vehiculosTotales,
                                vehiculosFinalizados = vehiculosFinalizados.get(),
                                vehiculosCancelados = vehiculosCancelados.get(),
                                vehiculosConError = vehiculosConError.get(),
                                maximoVehiculosSimultaneos = maximoSimultaneos.get(),
                                duracionTotalMs = SystemClock.elapsedRealtime() - inicio
                            )
                        )
                    }
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                publicar { callbacks.onCancelada(idEjecucion) }
            } finally {
                coordinador = null
            }
        }, "ParqueoInteligente-Coordinador-$idEjecucion")
        coordinador?.start()
    }

    // Marca la ejecucion como cancelada e interrumpe el pool de hilos y el coordinador
    fun cancelar() {
        cancelada.set(true)
        executorService?.shutdownNow()
        coordinador?.interrupt()
    }

    // Cancela la ejecucion en curso y espera a que los recursos se liberen antes de descartarlos
    fun limpiar() {
        cancelar()
        val pool = executorService
        if (pool != null) {
            try {
                pool.awaitTermination(TIEMPO_ESPERA_LIMPIEZA_MS, TimeUnit.MILLISECONDS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        coordinador = null
        executorService = null
    }

    // Simula el ciclo de vida completo de un vehiculo: esperar, adquirir permiso, entrar, estacionarse y salir
    private fun ejecutarVehiculo(
        idEjecucion: Int,
        idVehiculo: Int,
        nombre: String,
        semaforo: Semaphore,
        config: ConfiguracionParqueoInteligente,
        permisosDisponibles: AtomicInteger,
        recursosOcupados: AtomicInteger,
        hilosEsperando: AtomicInteger,
        maximoSimultaneos: AtomicInteger,
        vehiculosFinalizados: AtomicInteger,
        vehiculosCancelados: AtomicInteger,
        vehiculosConError: AtomicInteger,
        inicio: Long
    ) {
        var permisoAdquirido = false
        var completo = false
        val horaInicio = SystemClock.elapsedRealtime()
        var horaEntrada = 0L
        var horaSalida = 0L

        try {
            publicarVehiculo(idEjecucion, idVehiculo, nombre, EstadoVehiculoParqueoInteligente.Esperando, horaInicioMs = horaInicio)
            publicarEvento(idEjecucion, EventoRegistroParqueoInteligente.VehiculoEsperando(idVehiculo))
            hilosEsperando.incrementAndGet()
            publicarMetricas(
                idEjecucion, config, permisosDisponibles, recursosOcupados, hilosEsperando,
                maximoSimultaneos, vehiculosFinalizados, vehiculosCancelados, vehiculosConError, inicio
            )

            semaforo.acquire()
            permisoAdquirido = true
            hilosEsperando.decrementAndGet()

            horaEntrada = SystemClock.elapsedRealtime()
            val disponiblesAlEntrar = permisosDisponibles.decrementAndGet()
            actualizarMaximo(maximoSimultaneos, recursosOcupados.incrementAndGet())

            publicarVehiculo(
                idEjecucion, idVehiculo, nombre, EstadoVehiculoParqueoInteligente.Entrando,
                horaInicioMs = horaInicio, horaEntradaMs = horaEntrada, tiempoEsperaMs = horaEntrada - horaInicio
            )
            publicarEvento(idEjecucion, EventoRegistroParqueoInteligente.PermisoAdquirido(idVehiculo, disponiblesAlEntrar))
            publicarMetricas(
                idEjecucion, config, permisosDisponibles, recursosOcupados, hilosEsperando,
                maximoSimultaneos, vehiculosFinalizados, vehiculosCancelados, vehiculosConError, inicio
            )
            Thread.sleep(DURACION_ENTRANDO_MS)

            publicarVehiculo(
                idEjecucion, idVehiculo, nombre, EstadoVehiculoParqueoInteligente.Estacionado,
                horaInicioMs = horaInicio, horaEntradaMs = horaEntrada, tiempoEsperaMs = horaEntrada - horaInicio
            )
            publicarEvento(idEjecucion, EventoRegistroParqueoInteligente.VehiculoEstacionado(idVehiculo))
            Thread.sleep(DURACION_ESTACIONADO_MS)

            horaSalida = SystemClock.elapsedRealtime()
            publicarVehiculo(
                idEjecucion, idVehiculo, nombre, EstadoVehiculoParqueoInteligente.Saliendo,
                horaInicioMs = horaInicio, horaEntradaMs = horaEntrada, horaSalidaMs = horaSalida,
                tiempoEsperaMs = horaEntrada - horaInicio, tiempoEstacionadoMs = horaSalida - horaEntrada
            )
            publicarEvento(idEjecucion, EventoRegistroParqueoInteligente.VehiculoSaliendo(idVehiculo))
            Thread.sleep(DURACION_SALIENDO_MS)

            completo = true
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            vehiculosCancelados.incrementAndGet()
            publicarVehiculo(idEjecucion, idVehiculo, nombre, EstadoVehiculoParqueoInteligente.Cancelado, horaInicioMs = horaInicio)
        } catch (throwable: Throwable) {
            cancelada.set(true)
            vehiculosConError.incrementAndGet()
            publicarVehiculo(
                idEjecucion, idVehiculo, nombre, EstadoVehiculoParqueoInteligente.Error,
                horaInicioMs = horaInicio, mensaje = throwable.message
            )
            publicar { callbacks.onError(idEjecucion, throwable.message, throwable) }
        } finally {
            if (permisoAdquirido) {
                semaforo.release()
                val disponiblesAlSalir = permisosDisponibles.incrementAndGet()
                recursosOcupados.decrementAndGet()
                publicarEvento(idEjecucion, EventoRegistroParqueoInteligente.PermisoLiberado(idVehiculo, disponiblesAlSalir))
            }
            if (completo) {
                vehiculosFinalizados.incrementAndGet()
                publicarVehiculo(
                    idEjecucion, idVehiculo, nombre, EstadoVehiculoParqueoInteligente.Finalizado,
                    horaInicioMs = horaInicio, horaEntradaMs = horaEntrada, horaSalidaMs = horaSalida,
                    tiempoEsperaMs = horaEntrada - horaInicio, tiempoEstacionadoMs = horaSalida - horaEntrada
                )
                publicarEvento(idEjecucion, EventoRegistroParqueoInteligente.VehiculoFinalizado(idVehiculo))
            }
            publicarMetricas(
                idEjecucion, config, permisosDisponibles, recursosOcupados, hilosEsperando,
                maximoSimultaneos, vehiculosFinalizados, vehiculosCancelados, vehiculosConError, inicio
            )
        }
    }

    // Actualiza el maximo de vehiculos simultaneos registrado si el valor actual lo supera
    private fun actualizarMaximo(maximoSimultaneos: AtomicInteger, valorActual: Int) {
        maximoSimultaneos.updateAndGet { maxOf(it, valorActual) }
    }

    // Construye el estado actualizado de un vehiculo y lo publica en el hilo principal via callback
    private fun publicarVehiculo(
        idEjecucion: Int,
        idVehiculo: Int,
        nombre: String,
        estado: EstadoVehiculoParqueoInteligente,
        horaInicioMs: Long? = null,
        horaEntradaMs: Long? = null,
        horaSalidaMs: Long? = null,
        tiempoEsperaMs: Long? = null,
        tiempoEstacionadoMs: Long? = null,
        mensaje: String? = null
    ) {
        publicar {
            callbacks.onVehiculoActualizado(
                idEjecucion,
                VehiculoParqueoInteligente(
                    id = idVehiculo,
                    nombre = nombre,
                    estado = estado,
                    horaInicioMs = horaInicioMs,
                    horaEntradaMs = horaEntradaMs,
                    horaSalidaMs = horaSalidaMs,
                    tiempoEsperaMs = tiempoEsperaMs,
                    tiempoEstacionadoMs = tiempoEstacionadoMs,
                    mensaje = mensaje
                )
            )
        }
    }

    // Publica un evento de registro en el hilo principal via callback
    private fun publicarEvento(idEjecucion: Int, evento: EventoRegistroParqueoInteligente) {
        publicar { callbacks.onEvento(idEjecucion, evento) }
    }

    // Construye una foto de las metricas actuales y la publica en el hilo principal via callback
    private fun publicarMetricas(
        idEjecucion: Int,
        config: ConfiguracionParqueoInteligente,
        permisosDisponibles: AtomicInteger,
        recursosOcupados: AtomicInteger,
        hilosEsperando: AtomicInteger,
        maximoSimultaneos: AtomicInteger,
        vehiculosFinalizados: AtomicInteger,
        vehiculosCancelados: AtomicInteger,
        vehiculosConError: AtomicInteger,
        inicio: Long
    ) {
        publicar {
            callbacks.onMetricasActualizadas(
                idEjecucion,
                MetricasParqueoInteligente(
                    permisosTotales = config.espaciosDisponibles,
                    permisosDisponibles = permisosDisponibles.get(),
                    recursosOcupados = recursosOcupados.get(),
                    hilosEsperando = hilosEsperando.get(),
                    vehiculosFinalizados = vehiculosFinalizados.get(),
                    vehiculosCancelados = vehiculosCancelados.get(),
                    vehiculosConError = vehiculosConError.get(),
                    maximoVehiculosSimultaneos = maximoSimultaneos.get(),
                    tiempoTotalMs = SystemClock.elapsedRealtime() - inicio
                )
            )
        }
    }

    // Ejecuta la accion en el hilo principal, directamente si ya esta en el o encolandola en el handler
    private fun publicar(accion: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            accion()
        } else {
            mainHandler.post(accion)
        }
    }

    companion object {
        private const val DURACION_ENTRANDO_MS = 300L
        private const val DURACION_ESTACIONADO_MS = 1200L
        private const val DURACION_SALIENDO_MS = 300L
        private const val TIEMPO_MAXIMO_ESPERA_MS = 30_000L
        private const val TIEMPO_ESPERA_LIMPIEZA_MS = 500L
    }
}
