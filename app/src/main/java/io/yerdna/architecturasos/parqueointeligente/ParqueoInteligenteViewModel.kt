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

    // Cambia la cantidad de espacios disponibles y reinicia vehiculos y metricas si no hay ejecucion activa
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

    // Cambia la cantidad total de vehiculos y reinicia vehiculos y metricas si no hay ejecucion activa
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

    // Prepara el estado inicial y lanza el ejecutor para arrancar una nueva simulacion de parqueo
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
                // Reemplaza en el estado el vehiculo cuyo id coincide con el que llego actualizado
                override fun onVehiculoActualizado(idEjecucion: Int, vehiculo: VehiculoParqueoInteligente) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    estado = estado.copy(
                        vehiculos = estado.vehiculos.map {
                            if (it.id == vehiculo.id) vehiculo else it
                        }
                    )
                }

                // Reemplaza las metricas del estado con las mas recientes reportadas por el ejecutor
                override fun onMetricasActualizadas(idEjecucion: Int, metricas: MetricasParqueoInteligente) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    estado = estado.copy(metricas = metricas)
                }

                // Reenvia al callback externo un evento de registro producido por el ejecutor
                override fun onEvento(idEjecucion: Int, evento: EventoRegistroParqueoInteligente) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    emitir(evento)
                }

                // Marca la ejecucion como exitosa, finaliza los vehiculos pendientes y guarda el resultado
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

                // Marca la ejecucion como cancelada y actualiza los vehiculos que quedaron pendientes
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

                // Cancela la ejecucion tras un error tecnico y guarda el mensaje para mostrarlo
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

    // Cancela la ejecucion en curso si hay una activa
    fun cancelar() {
        if (!estado.ejecucionActiva) return
        ejecutor?.cancelar()
    }

    // Restaura el estado inicial con la configuracion normalizada vigente
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

    // Libera el ejecutor y limpia las referencias de la ejecucion activa
    fun limpiarRecursos() {
        ejecutor?.limpiar()
        ejecutor = null
        idEjecucionActiva = null
        registrarEvento = null
    }

    // Indica si el estado actual permite iniciar una nueva ejecucion
    fun puedeIniciar(): Boolean {
        return estado.fase == FaseParqueoInteligente.Inactivo ||
            estado.fase == FaseParqueoInteligente.Exitoso ||
            estado.fase == FaseParqueoInteligente.Cancelado ||
            estado.fase == FaseParqueoInteligente.Error
    }

    // Indica si el estado actual permite cancelar la ejecucion
    fun puedeCancelar(): Boolean {
        return estado.ejecucionActiva
    }

    // Indica si el estado actual permite reiniciar el modulo
    fun puedeReiniciar(): Boolean {
        return !estado.ejecucionActiva
    }

    // Indica si hay una ejecucion en curso en este momento
    fun hayEjecucionActiva(): Boolean {
        return estado.ejecucionActiva
    }

    // Descarta las referencias al ejecutor y a la ejecucion activa una vez que termino
    private fun cerrarEjecucion() {
        ejecutor = null
        idEjecucionActiva = null
    }

    // Envia un evento de registro al callback externo actualmente suscrito
    private fun emitir(evento: EventoRegistroParqueoInteligente) {
        registrarEvento?.invoke(evento)
    }

    // Verifica que el id de ejecucion recibido corresponda a la ejecucion activa actual
    private fun esEjecucionActiva(idEjecucion: Int): Boolean {
        return idEjecucionActiva == idEjecucion
    }

    // Libera los recursos del ejecutor cuando el ViewModel se destruye
    override fun onCleared() {
        limpiarRecursos()
        super.onCleared()
    }
}
