package io.yerdna.architecturasos.bancocaotico

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class BancoCaoticoViewModel : ViewModel() {
    var estado by mutableStateOf(EstadoBancoCaotico())
        private set

    private var proximoIdEjecucion = 1
    private var idEjecucionActiva: Int? = null
    private var ejecutor: EjecutorBancoCaotico? = null
    private var registrarEvento: ((EventoRegistroBancoCaotico) -> Unit)? = null

    fun actualizarCantidadCajeros(valor: Int) {
        if (estado.ejecucionActiva) return
        val config = estado.configuracion.copy(cantidadCajeros = valor).normalizada()
        estado = estado.copy(
            configuracion = config,
            cajeros = cajerosIniciales(config)
        )
    }

    fun actualizarOperacionesPorCajero(valor: Int) {
        if (estado.ejecucionActiva) return
        estado = estado.copy(
            configuracion = estado.configuracion.copy(operacionesPorCajero = valor).normalizada()
        )
    }

    fun iniciar(onEvento: (EventoRegistroBancoCaotico) -> Unit) {
        iniciarConConfiguracion(estado.configuracion.normalizada(), onEvento)
    }

    fun cancelar() {
        if (!estado.ejecucionActiva) return
        ejecutor?.cancelar()
    }

    fun reiniciar() {
        if (estado.ejecucionActiva) return
        val config = estado.configuracion.normalizada()
        estado = EstadoBancoCaotico(
            configuracion = config,
            cajeros = cajerosIniciales(config)
        )
    }

    fun limpiarRecursos() {
        ejecutor?.limpiar()
        ejecutor = null
        idEjecucionActiva = null
        registrarEvento = null
    }

    fun puedeIniciar(): Boolean {
        return estado.fase == FaseBancoCaotico.Inactivo ||
            estado.fase == FaseBancoCaotico.Exitoso ||
            estado.fase == FaseBancoCaotico.Cancelado ||
            estado.fase == FaseBancoCaotico.Error
    }

    fun puedeCancelar(): Boolean {
        return estado.fase == FaseBancoCaotico.Ejecutando
    }

    fun puedeReiniciar(): Boolean {
        return estado.fase != FaseBancoCaotico.Ejecutando
    }

    fun hayEjecucionActiva(): Boolean {
        return estado.ejecucionActiva
    }

    private fun iniciarConConfiguracion(
        configuracion: ConfiguracionBancoCaotico,
        onEvento: (EventoRegistroBancoCaotico) -> Unit
    ) {
        if (!puedeIniciar()) return

        val config = configuracion.normalizada()
        val idEjecucion = proximoIdEjecucion++
        idEjecucionActiva = idEjecucion
        registrarEvento = onEvento

        ejecutor?.limpiar()
        estado = EstadoBancoCaotico(
            fase = FaseBancoCaotico.Ejecutando,
            configuracion = config,
            cajeros = cajerosIniciales(config),
            resultadoUltimaEjecucion = null,
            mensajeError = null,
            ejecucionActiva = true
        )

        emitir(EventoRegistroBancoCaotico.ConfiguracionAplicada(config.cantidadCajeros, config.operacionesPorCajero))
        emitir(EventoRegistroBancoCaotico.EjecucionIniciada(idEjecucion))

        ejecutor = EjecutorBancoCaotico(
            callbacks = object : EjecutorBancoCaotico.Callbacks {
                override fun onCajeroActualizado(idEjecucion: Int, cajero: CajeroBancoCaotico) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    estado = estado.copy(
                        cajeros = estado.cajeros.map {
                            if (it.id == cajero.id) cajero else it
                        }
                    )
                }

                override fun onEvento(idEjecucion: Int, evento: EventoBancoCaotico) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    when (evento) {
                        is EventoBancoCaotico.CajeroIniciado -> emitir(
                            EventoRegistroBancoCaotico.CajeroIniciado(evento.idCajero)
                        )

                        is EventoBancoCaotico.ProgresoCajero -> emitir(
                            EventoRegistroBancoCaotico.ProgresoCajero(
                                evento.idCajero,
                                evento.operacionesCompletadas
                            )
                        )

                        is EventoBancoCaotico.CajeroFinalizado -> emitir(
                            EventoRegistroBancoCaotico.CajeroFinalizado(evento.idCajero)
                        )

                        is EventoBancoCaotico.CajeroCancelado -> emitir(
                            EventoRegistroBancoCaotico.CajeroCancelado(evento.idCajero)
                        )
                    }
                }

                override fun onFinalizada(idEjecucion: Int, resultado: ResultadoBancoCaotico) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    estado = estado.copy(
                        fase = FaseBancoCaotico.Exitoso,
                        resultadoUltimaEjecucion = resultado,
                        mensajeError = null,
                        ejecucionActiva = false,
                        cajeros = estado.cajeros.map {
                            if (it.estado == EstadoCajeroBancoCaotico.Trabajando) {
                                it.copy(estado = EstadoCajeroBancoCaotico.Finalizado)
                            } else {
                                it
                            }
                        }
                    )
                    cerrarEjecucion()
                    emitir(EventoRegistroBancoCaotico.EjecucionFinalizada(resultado))
                }

                override fun onCancelada(idEjecucion: Int) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    estado = estado.copy(
                        fase = FaseBancoCaotico.Cancelado,
                        ejecucionActiva = false,
                        cajeros = estado.cajeros.map {
                            if (it.estado == EstadoCajeroBancoCaotico.Finalizado) {
                                it
                            } else {
                                it.copy(estado = EstadoCajeroBancoCaotico.Cancelado)
                            }
                        }
                    )
                    cerrarEjecucion()
                    emitir(EventoRegistroBancoCaotico.EjecucionCancelada)
                }

                override fun onError(idEjecucion: Int, mensaje: String?, throwable: Throwable?) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    ejecutor?.cancelar()
                    estado = estado.copy(
                        fase = FaseBancoCaotico.Error,
                        mensajeError = mensaje,
                        ejecucionActiva = false,
                        cajeros = estado.cajeros.map {
                            if (it.estado == EstadoCajeroBancoCaotico.Finalizado) {
                                it
                            } else {
                                it.copy(estado = EstadoCajeroBancoCaotico.Error)
                            }
                        }
                    )
                    cerrarEjecucion()
                    emitir(EventoRegistroBancoCaotico.ErrorTecnico(mensaje))
                }
            }
        )
        ejecutor?.iniciar(idEjecucion, config)
    }

    private fun cerrarEjecucion() {
        ejecutor = null
        idEjecucionActiva = null
    }

    private fun emitir(evento: EventoRegistroBancoCaotico) {
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
