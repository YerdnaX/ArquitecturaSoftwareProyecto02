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

    // Actualiza la cantidad inicial de boletos y reinicia compradores/métricas si no hay ejecución activa
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

    // Actualiza la cantidad total de compradores y reinicia compradores/métricas si no hay ejecución activa
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

    // Inicia la simulación en modo sin mutex, donde puede producirse una condición de carrera
    fun iniciarSinMutex(onEvento: (EventoRegistroCarreraBoletos) -> Unit) {
        iniciar(ModoCarreraBoletos.SinMutex, onEvento)
    }

    // Inicia la simulación en modo con mutex, donde el acceso al recurso está sincronizado
    fun iniciarConMutex(onEvento: (EventoRegistroCarreraBoletos) -> Unit) {
        iniciar(ModoCarreraBoletos.ConMutex, onEvento)
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

    // Libera el ejecutor y los datos asociados a la ejecución activa
    fun limpiarRecursos() {
        ejecutor?.limpiar()
        ejecutor = null
        idEjecucionActiva = null
        registrarEvento = null
    }

    // Indica si el estado actual permite iniciar una nueva ejecución
    fun puedeIniciar(): Boolean {
        return estado.fase == FaseCarreraBoletos.Inactivo ||
            estado.fase == FaseCarreraBoletos.Exitoso ||
            estado.fase == FaseCarreraBoletos.Cancelado ||
            estado.fase == FaseCarreraBoletos.Error
    }

    // Indica si hay una ejecución en curso que pueda cancelarse
    fun puedeCancelar(): Boolean {
        return estado.ejecucionActiva
    }

    // Indica si el estado actual permite reiniciar
    fun puedeReiniciar(): Boolean {
        return !estado.ejecucionActiva
    }

    // Indica si actualmente hay una ejecución en curso
    fun hayEjecucionActiva(): Boolean {
        return estado.ejecucionActiva
    }

    // Prepara el estado y arranca el ejecutor en el modo indicado, registrando sus callbacks
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
                // Actualiza el comprador correspondiente dentro del estado y recalcula cuántos están esperando
                override fun onCompradorActualizado(idEjecucion: Int, comprador: CompradorBoleto) {
                    if (!esEjecucionActiva(idEjecucion)) return
                    estado = estado.copy(
                        compradores = estado.compradores.map {
                            if (it.id == comprador.id) comprador else it
                        }
                    )
                    actualizarCompradoresEsperando()
                }

                // Actualiza las métricas del estado con la cantidad de compradores esperando el mutex
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

                // Traduce los eventos técnicos del ejecutor en eventos de registro para la UI
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

                // Marca la ejecución como exitosa y guarda el resultado según el modo utilizado
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

                // Marca la ejecución como cancelada y ajusta el estado de los compradores
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

                // Marca la ejecución como fallida, cancela el ejecutor y guarda el mensaje de error
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

    // Recalcula en las métricas cuántos compradores están esperando el mutex
    private fun actualizarCompradoresEsperando() {
        estado = estado.copy(
            metricas = estado.metricas.copy(
                compradoresEsperando = estado.compradores.count {
                    it.estado == EstadoCompradorBoleto.EsperandoMutex
                }
            )
        )
    }

    // Limpia las referencias asociadas a la ejecución activa
    private fun cerrarEjecucion() {
        ejecutor = null
        idEjecucionActiva = null
    }

    // Envía un evento de registro al callback suscrito, si existe
    private fun emitir(evento: EventoRegistroCarreraBoletos) {
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
