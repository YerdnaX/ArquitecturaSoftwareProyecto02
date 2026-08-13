package io.yerdna.architecturasos.parqueointeligente

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class ParqueoInteligenteViewModel : ViewModel() {
    var estado by mutableStateOf(EstadoParqueoInteligente())
        private set

    private var proximoIdEjecucion = 1
    private var idEjecucionActiva: Int? = null
    private var ejecutor: EjecutorParqueoInteligente? = null
    private var registrarEvento: ((EventoRegistroParqueoInteligente) -> Unit)? = null

    fun actualizarEspaciosDisponibles(valor: Int) {
        if (estado.ejecucionActiva) return
        val config = estado.configuracion.copy(espaciosDisponibles = valor).normalizada()
        estado = estado.copy(
            configuracion = config,
            vehiculos = vehiculosIniciales(config),
            metricas = MetricasParqueoInteligente(
                permisosTotales = config.espaciosDisponibles,
                permisosDisponibles = config.espaciosDisponibles
            )
        )
    }

    fun actualizarVehiculosTotales(valor: Int) {
        if (estado.ejecucionActiva) return
        val config = estado.configuracion.copy(vehiculosTotales = valor).normalizada()
        estado = estado.copy(
            configuracion = config,
            vehiculos = vehiculosIniciales(config),
            metricas = MetricasParqueoInteligente(
                permisosTotales = config.espaciosDisponibles,
                permisosDisponibles = config.espaciosDisponibles
            )
        )
    }

    fun iniciar(onEvento: (EventoRegistroParqueoInteligente) -> Unit) {
        if (!puedeIniciar()) return

        val config = estado.configuracion.normalizada()
        val idEjecucion = proximoIdEjecucion++
        idEjecucionActiva = idEjecucion
        registrarEvento = onEvento
        ejecutor?.limpiar()

        estado = estado.copy(
            fase = FaseParqueoInteligente.Ejecutando,
            configuracion = config,
            vehiculos = vehiculosIniciales(config),
            metricas = MetricasParqueoInteligente(
                permisosTotales = config.espaciosDisponibles,
                permisosDisponibles = config.espaciosDisponibles
            ),
            mensajeError = null,
            ejecucionActiva = true
        )

        emitir(EventoRegistroParqueoInteligente.ConfiguracionAplicada(config.espaciosDisponibles, config.vehiculosTotales))
        emitir(EventoRegistroParqueoInteligente.EjecucionIniciada(idEjecucion))

        ejecutor = EjecutorParqueoInteligente(
            callbacks = object : EjecutorParqueoInteligente.Callbacks {
                override fun onVehiculoActualizado(idEjecucion: Int, vehiculo: VehiculoParqueoInteligente) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    estado = estado.copy(
                        vehiculos = estado.vehiculos.map {
                            if (it.id == vehiculo.id) vehiculo else it
                        }
                    )
                }

                override fun onMetricasActualizadas(idEjecucion: Int, metricas: MetricasParqueoInteligente) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    estado = estado.copy(metricas = metricas)
                }

                override fun onEvento(idEjecucion: Int, evento: EventoRegistroParqueoInteligente) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    emitir(evento)
                }

                override fun onFinalizada(idEjecucion: Int, resultado: ResultadoParqueoInteligente) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    estado = estado.copy(
                        fase = FaseParqueoInteligente.Exitoso,
                        vehiculos = estado.vehiculos.map {
                            if (it.estado == EstadoVehiculoParqueoInteligente.Finalizado) it else it.copy(
                                estado = EstadoVehiculoParqueoInteligente.Finalizado
                            )
                        },
                        metricas = estado.metricas.copy(
                            tiempoTotalMs = resultado.duracionTotalMs
                        ),
                        resultadoUltimaEjecucion = resultado,
                        mensajeError = null,
                        ejecucionActiva = false
                    )
                    cerrarEjecucion()
                    emitir(EventoRegistroParqueoInteligente.EjecucionFinalizada(resultado))
                }

                override fun onCancelada(idEjecucion: Int) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    estado = estado.copy(
                        fase = FaseParqueoInteligente.Cancelado,
                        vehiculos = estado.vehiculos.map {
                            if (it.estado == EstadoVehiculoParqueoInteligente.Finalizado) it else it.copy(
                                estado = EstadoVehiculoParqueoInteligente.Cancelado
                            )
                        },
                        ejecucionActiva = false
                    )
                    cerrarEjecucion()
                    emitir(EventoRegistroParqueoInteligente.EjecucionCancelada)
                }

                override fun onError(idEjecucion: Int, mensaje: String?, throwable: Throwable?) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    ejecutor?.cancelar()
                    estado = estado.copy(
                        fase = FaseParqueoInteligente.Error,
                        vehiculos = estado.vehiculos.map {
                            if (it.estado == EstadoVehiculoParqueoInteligente.Finalizado) it else it.copy(
                                estado = EstadoVehiculoParqueoInteligente.Error
                            )
                        },
                        mensajeError = mensaje,
                        ejecucionActiva = false
                    )
                    cerrarEjecucion()
                    emitir(EventoRegistroParqueoInteligente.ErrorTecnico(mensaje))
                }
            }
        )
        ejecutor?.iniciar(idEjecucion, config)
    }

    fun cancelar() {
        if (!estado.ejecucionActiva) return
        ejecutor?.cancelar()
    }

    fun reiniciar() {
        if (estado.ejecucionActiva) return
        val config = estado.configuracion.normalizada()
        estado = EstadoParqueoInteligente(
            configuracion = config,
            vehiculos = vehiculosIniciales(config),
            metricas = MetricasParqueoInteligente(
                permisosTotales = config.espaciosDisponibles,
                permisosDisponibles = config.espaciosDisponibles
            )
        )
    }

    fun limpiarRecursos() {
        ejecutor?.limpiar()
        ejecutor = null
        idEjecucionActiva = null
        registrarEvento = null
    }

    fun puedeIniciar(): Boolean {
        return estado.fase == FaseParqueoInteligente.Inactivo ||
            estado.fase == FaseParqueoInteligente.Exitoso ||
            estado.fase == FaseParqueoInteligente.Cancelado ||
            estado.fase == FaseParqueoInteligente.Error
    }

    fun puedeCancelar(): Boolean {
        return estado.ejecucionActiva
    }

    fun puedeReiniciar(): Boolean {
        return !estado.ejecucionActiva
    }

    fun hayEjecucionActiva(): Boolean {
        return estado.ejecucionActiva
    }

    private fun cerrarEjecucion() {
        ejecutor = null
        idEjecucionActiva = null
    }

    private fun emitir(evento: EventoRegistroParqueoInteligente) {
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
