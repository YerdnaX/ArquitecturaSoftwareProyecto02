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

    // Actualiza la cantidad de cajeros configurada y reinicia la lista de cajeros si no hay ejecución activa
    fun actualizarCantidadCajeros(valor: Int) {
        if (estado.ejecucionActiva) return
        val config = estado.configuracion.copy(cantidadCajeros = valor).normalizada()
        estado = estado.copy(
            configuracion = config,
            cajeros = cajerosIniciales(config)
        )
    }

    // Actualiza la cantidad de operaciones por cajero si no hay una ejecución activa
    fun actualizarOperacionesPorCajero(valor: Int) {
        if (estado.ejecucionActiva) return
        estado = estado.copy(
            configuracion = estado.configuracion.copy(operacionesPorCajero = valor).normalizada()
        )
    }

    // Inicia una nueva ejecución usando la configuración actual normalizada
    fun iniciar(onEvento: (EventoRegistroBancoCaotico) -> Unit) {
        iniciarConConfiguracion(estado.configuracion.normalizada(), onEvento)
    }

    // Solicita la cancelación de la ejecución activa
    fun cancelar() {
        if (!estado.ejecucionActiva) return
        ejecutor?.cancelar()
    }

    // Reinicia el estado a sus valores iniciales si no hay una ejecución activa
    fun reiniciar() {
        if (estado.ejecucionActiva) return
        val config = estado.configuracion.normalizada()
        estado = EstadoBancoCaotico(
            configuracion = config,
            cajeros = cajerosIniciales(config)
        )
    }

    // Libera el ejecutor y los datos asociados a la ejecución activa
    fun limpiarRecursos() {
        ejecutor?.limpiar()
        ejecutor = null
        idEjecucionActiva = null
        registrarEvento = null
    }

    // Indica si el estado actual permite iniciar una nueva ejecución
    fun puedeIniciar(): Boolean {
        return estado.fase == FaseBancoCaotico.Inactivo ||
            estado.fase == FaseBancoCaotico.Exitoso ||
            estado.fase == FaseBancoCaotico.Cancelado ||
            estado.fase == FaseBancoCaotico.Error
    }

    // Indica si hay una ejecución en curso que pueda cancelarse
    fun puedeCancelar(): Boolean {
        return estado.fase == FaseBancoCaotico.Ejecutando
    }

    // Indica si el estado actual permite reiniciar
    fun puedeReiniciar(): Boolean {
        return estado.fase != FaseBancoCaotico.Ejecutando
    }

    // Indica si actualmente hay una ejecución en curso
    fun hayEjecucionActiva(): Boolean {
        return estado.ejecucionActiva
    }

    // Prepara el estado y arranca el ejecutor con la configuración indicada, registrando sus callbacks
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
                // Actualiza el cajero correspondiente dentro del estado cuando el ejecutor reporta un cambio
                override fun onCajeroActualizado(idEjecucion: Int, cajero: CajeroBancoCaotico) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    estado = estado.copy(
                        cajeros = estado.cajeros.map {
                            if (it.id == cajero.id) cajero else it
                        }
                    )
                }

                // Traduce los eventos técnicos del ejecutor en eventos de registro para la UI
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

                // Marca la ejecución como exitosa y guarda el resultado final
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

                // Marca la ejecución como cancelada y ajusta el estado de los cajeros
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

                // Marca la ejecución como fallida, cancela el ejecutor y guarda el mensaje de error
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

    // Limpia las referencias asociadas a la ejecución activa
    private fun cerrarEjecucion() {
        ejecutor = null
        idEjecucionActiva = null
    }

    // Envía un evento de registro al callback suscrito, si existe
    private fun emitir(evento: EventoRegistroBancoCaotico) {
        registrarEvento?.invoke(evento)
    }

    // Verifica si el id de ejecución dado corresponde a la ejecución actualmente activa
    private fun esEjecucionActiva(idEjecucion: Int): Boolean {
        return idEjecucionActiva == idEjecucion
    }

    // Libera los recursos del ejecutor cuando el ViewModel se destruye
    override fun onCleared() {
        limpiarRecursos()
        super.onCleared()
    }
}
