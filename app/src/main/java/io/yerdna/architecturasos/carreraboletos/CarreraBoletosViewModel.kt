package io.yerdna.architecturasos.carreraboletos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class CarreraBoletosViewModel : ViewModel() {
    var estado by mutableStateOf(EstadoCarreraBoletos())
        private set

    private var proximoIdEjecucion = 1
    private var idEjecucionActiva: Int? = null
    private var ejecutor: EjecutorCarreraBoletos? = null
    private var registrarEvento: ((EventoRegistroCarreraBoletos) -> Unit)? = null

    fun actualizarBoletosIniciales(valor: Int) {
        if (estado.ejecucionActiva) return
        val config = estado.configuracion.copy(boletosIniciales = valor).normalizada()
        estado = estado.copy(
            configuracion = config,
            compradores = compradoresIniciales(config),
            metricas = MetricasCarreraBoletos(
                boletosIniciales = config.boletosIniciales,
                boletosRestantes = config.boletosIniciales,
                compradoresTotales = config.compradoresTotales
            )
        )
    }

    fun actualizarCompradoresTotales(valor: Int) {
        if (estado.ejecucionActiva) return
        val config = estado.configuracion.copy(compradoresTotales = valor).normalizada()
        estado = estado.copy(
            configuracion = config,
            compradores = compradoresIniciales(config),
            metricas = MetricasCarreraBoletos(
                boletosIniciales = config.boletosIniciales,
                boletosRestantes = config.boletosIniciales,
                compradoresTotales = config.compradoresTotales
            )
        )
    }

    fun iniciarSinMutex(onEvento: (EventoRegistroCarreraBoletos) -> Unit) {
        iniciar(ModoCarreraBoletos.SinMutex, onEvento)
    }

    fun iniciarConMutex(onEvento: (EventoRegistroCarreraBoletos) -> Unit) {
        iniciar(ModoCarreraBoletos.ConMutex, onEvento)
    }

    fun cancelar() {
        if (!estado.ejecucionActiva) return
        ejecutor?.cancelar()
    }

    fun reiniciar() {
        if (estado.ejecucionActiva) return
        val config = estado.configuracion.normalizada()
        estado = EstadoCarreraBoletos(
            configuracion = config,
            compradores = compradoresIniciales(config),
            metricas = MetricasCarreraBoletos(
                boletosIniciales = config.boletosIniciales,
                boletosRestantes = config.boletosIniciales,
                compradoresTotales = config.compradoresTotales
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
        return estado.fase == FaseCarreraBoletos.Inactivo ||
            estado.fase == FaseCarreraBoletos.Exitoso ||
            estado.fase == FaseCarreraBoletos.Cancelado ||
            estado.fase == FaseCarreraBoletos.Error
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

    private fun iniciar(
        modo: ModoCarreraBoletos,
        onEvento: (EventoRegistroCarreraBoletos) -> Unit
    ) {
        if (!puedeIniciar()) return

        val config = estado.configuracion.normalizada()
        val idEjecucion = proximoIdEjecucion++
        idEjecucionActiva = idEjecucion
        registrarEvento = onEvento
        ejecutor?.limpiar()

        estado = estado.copy(
            fase = FaseCarreraBoletos.Preparando,
            configuracion = config,
            compradores = compradoresIniciales(config),
            metricas = MetricasCarreraBoletos(
                modo = modo,
                boletosIniciales = config.boletosIniciales,
                boletosRestantes = config.boletosIniciales,
                compradoresTotales = config.compradoresTotales
            ),
            mensajeError = null,
            ejecucionActiva = true
        )

        emitir(EventoRegistroCarreraBoletos.ConfiguracionAplicada(config.boletosIniciales, config.compradoresTotales))
        emitir(EventoRegistroCarreraBoletos.EjecucionIniciada(idEjecucion))
        emitir(EventoRegistroCarreraBoletos.ModoSeleccionado(modo))

        estado = estado.copy(
            fase = if (modo == ModoCarreraBoletos.SinMutex) {
                FaseCarreraBoletos.EjecutandoSinMutex
            } else {
                FaseCarreraBoletos.EjecutandoConMutex
            }
        )

        ejecutor = EjecutorCarreraBoletos(
            callbacks = object : EjecutorCarreraBoletos.Callbacks {
                override fun onCompradorActualizado(idEjecucion: Int, comprador: CompradorBoleto) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    estado = estado.copy(
                        compradores = estado.compradores.map {
                            if (it.id == comprador.id) comprador else it
                        }
                    )
                    actualizarCompradoresEsperando()
                }

                override fun onMetricasActualizadas(idEjecucion: Int, metricas: MetricasCarreraBoletos) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    estado = estado.copy(
                        metricas = metricas.copy(
                            compradoresEsperando = estado.compradores.count {
                                it.estado == EstadoCompradorBoleto.EsperandoMutex
                            }
                        )
                    )
                }

                override fun onEvento(idEjecucion: Int, evento: EventoTecnicoCarreraBoletos) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    when (evento) {
                        is EventoTecnicoCarreraBoletos.CompradorIntentandoEntrar -> emitir(
                            EventoRegistroCarreraBoletos.CompradorIntentandoEntrar(evento.idComprador)
                        )

                        is EventoTecnicoCarreraBoletos.CompradorEsperandoMutex -> emitir(
                            EventoRegistroCarreraBoletos.CompradorEsperandoMutex(evento.idComprador)
                        )

                        is EventoTecnicoCarreraBoletos.MutexAdquirido -> emitir(
                            EventoRegistroCarreraBoletos.MutexAdquirido(evento.idComprador)
                        )

                        is EventoTecnicoCarreraBoletos.VentaRealizada -> emitir(
                            EventoRegistroCarreraBoletos.VentaRealizada(
                                evento.idComprador,
                                evento.boletosRestantes
                            )
                        )

                        is EventoTecnicoCarreraBoletos.VentaIncorrectaDetectada -> emitir(
                            EventoRegistroCarreraBoletos.VentaIncorrectaDetectada(
                                evento.idComprador,
                                evento.ventasRegistradas
                            )
                        )

                        is EventoTecnicoCarreraBoletos.MutexLiberado -> emitir(
                            EventoRegistroCarreraBoletos.MutexLiberado(evento.idComprador)
                        )
                    }
                }

                override fun onFinalizada(idEjecucion: Int, resultado: ResultadoCarreraBoletos) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    val compradoresFinales = estado.compradores.map {
                        if (
                            it.estado == EstadoCompradorBoleto.Compro ||
                            it.estado == EstadoCompradorBoleto.SinBoleto
                        ) {
                            it
                        } else {
                            it.copy(estado = EstadoCompradorBoleto.Finalizado)
                        }
                    }
                    estado = estado.copy(
                        fase = FaseCarreraBoletos.Exitoso,
                        compradores = compradoresFinales,
                        metricas = estado.metricas.copy(
                            compradorEnSeccionCritica = null,
                            compradoresEsperando = 0,
                            tiempoTotalMs = resultado.tiempoTotalMs
                        ),
                        resultadoSinMutex = if (resultado.modo == ModoCarreraBoletos.SinMutex) {
                            resultado
                        } else {
                            estado.resultadoSinMutex
                        },
                        resultadoConMutex = if (resultado.modo == ModoCarreraBoletos.ConMutex) {
                            resultado
                        } else {
                            estado.resultadoConMutex
                        },
                        mensajeError = null,
                        ejecucionActiva = false
                    )
                    cerrarEjecucion()
                    emitir(EventoRegistroCarreraBoletos.EjecucionFinalizada(resultado))
                }

                override fun onCancelada(idEjecucion: Int) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    estado = estado.copy(
                        fase = FaseCarreraBoletos.Cancelado,
                        compradores = estado.compradores.map {
                            if (
                                it.estado == EstadoCompradorBoleto.Finalizado ||
                                it.estado == EstadoCompradorBoleto.Compro ||
                                it.estado == EstadoCompradorBoleto.SinBoleto
                            ) {
                                it
                            } else {
                                it.copy(estado = EstadoCompradorBoleto.Cancelado)
                            }
                        },
                        metricas = estado.metricas.copy(
                            compradorEnSeccionCritica = null,
                            compradoresEsperando = 0
                        ),
                        ejecucionActiva = false
                    )
                    cerrarEjecucion()
                    emitir(EventoRegistroCarreraBoletos.EjecucionCancelada)
                }

                override fun onError(idEjecucion: Int, mensaje: String?, throwable: Throwable?) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    ejecutor?.cancelar()
                    estado = estado.copy(
                        fase = FaseCarreraBoletos.Error,
                        compradores = estado.compradores.map {
                            if (it.estado == EstadoCompradorBoleto.Finalizado) it else it.copy(
                                estado = EstadoCompradorBoleto.Error
                            )
                        },
                        metricas = estado.metricas.copy(
                            compradorEnSeccionCritica = null,
                            compradoresEsperando = 0
                        ),
                        mensajeError = mensaje,
                        ejecucionActiva = false
                    )
                    cerrarEjecucion()
                    emitir(EventoRegistroCarreraBoletos.ErrorTecnico(mensaje))
                }
            }
        )
        ejecutor?.iniciar(idEjecucion, config, modo)
    }

    private fun actualizarCompradoresEsperando() {
        estado = estado.copy(
            metricas = estado.metricas.copy(
                compradoresEsperando = estado.compradores.count {
                    it.estado == EstadoCompradorBoleto.EsperandoMutex
                }
            )
        )
    }

    private fun cerrarEjecucion() {
        ejecutor = null
        idEjecucionActiva = null
    }

    private fun emitir(evento: EventoRegistroCarreraBoletos) {
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
