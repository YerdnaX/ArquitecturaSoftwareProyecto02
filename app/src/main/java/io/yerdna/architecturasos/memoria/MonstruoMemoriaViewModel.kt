package io.yerdna.architecturasos.memoria

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class MonstruoMemoriaViewModel : ViewModel() {
    var estado by mutableStateOf(EstadoMonstruoMemoria())
        private set

    private val bloques = mutableListOf<BloqueMemoriaReservada>()
    private var proximoIdBloque = 1
    private var idOperacionActiva = 0
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        estado = aplicarMedicion(estado.copy(limiteSeguroMb = calcularLimiteSeguroMb()))
    }

    fun reservar(tamanoMb: Int, onEvento: (EventoRegistroMonstruoMemoria) -> Unit) {
        if (estado.estadoEjecucion == EstadoEjecucionMemoria.Reservando) return

        onEvento(EventoRegistroMonstruoMemoria.IntentoReserva(tamanoMb))

        if (estado.memoriaReservadaMb + tamanoMb > estado.limiteSeguroMb) {
            estado = aplicarMedicion(
                estado.copy(
                    estadoEjecucion = EstadoEjecucionMemoria.Advertencia,
                    mensajeAdvertencia = "Reserva de $tamanoMb MB bloqueada: supera el limite seguro de ${estado.limiteSeguroMb} MB.",
                    resultadoUltimaAccion = "Bloqueado: $tamanoMb MB excede el limite seguro"
                )
            )
            onEvento(EventoRegistroMonstruoMemoria.ReservaBloqueada(tamanoMb, estado.memoriaReservadaMb))
            return
        }

        idOperacionActiva++
        val idOperacion = idOperacionActiva
        estado = recalcularControles(estado.copy(estadoEjecucion = EstadoEjecucionMemoria.Reservando))

        Thread {
            try {
                val bytes = ByteArray(tamanoMb * BYTES_POR_MB)
                var indice = 0
                while (indice < bytes.size) {
                    bytes[indice] = 1
                    indice += PASO_PAGINA_BYTES
                }
                val bloque = BloqueMemoriaReservada(
                    id = proximoIdBloque++,
                    tamanoMb = tamanoMb,
                    bytes = bytes,
                    timestamp = System.currentTimeMillis()
                )
                mainHandler.post {
                    if (idOperacion != idOperacionActiva) return@post
                    bloques.add(bloque)
                    publicarResultadoReserva(bloque, onEvento)
                }
            } catch (error: OutOfMemoryError) {
                mainHandler.post {
                    if (idOperacion != idOperacionActiva) return@post
                    bloques.clear()
                    estado = aplicarMedicion(
                        estado.copy(
                            estadoEjecucion = EstadoEjecucionMemoria.Error,
                            mensajeError = error.message ?: "OutOfMemoryError",
                            mensajeAdvertencia = null,
                            resultadoUltimaAccion = "Error: memoria insuficiente"
                        )
                    )
                    onEvento(EventoRegistroMonstruoMemoria.ErrorTecnico(error.message))
                }
            }
        }.start()
    }

    fun liberarMemoria(onEvento: (EventoRegistroMonstruoMemoria) -> Unit) {
        if (estado.estadoEjecucion == EstadoEjecucionMemoria.Reservando) return

        val bloquesLiberados = bloques.size
        bloques.clear()
        estado = aplicarMedicion(
            estado.copy(
                estadoEjecucion = EstadoEjecucionMemoria.Liberado,
                mensajeAdvertencia = null,
                mensajeError = null,
                resultadoUltimaAccion = "Liberados $bloquesLiberados bloques"
            )
        )
        onEvento(EventoRegistroMonstruoMemoria.LiberacionMemoria(bloquesLiberados))
    }

    fun solicitarGc(onEvento: (EventoRegistroMonstruoMemoria) -> Unit) {
        if (estado.estadoEjecucion == EstadoEjecucionMemoria.Reservando) return

        System.gc()
        estado = aplicarMedicion(
            estado.copy(
                estadoEjecucion = EstadoEjecucionMemoria.RecolectorSolicitado,
                resultadoUltimaAccion = "GC solicitado; Android decide cuando recolectar"
            )
        )
        onEvento(EventoRegistroMonstruoMemoria.SolicitudGc)
    }

    fun reiniciar(onEvento: (EventoRegistroMonstruoMemoria) -> Unit) {
        if (estado.estadoEjecucion == EstadoEjecucionMemoria.Reservando) return

        idOperacionActiva++
        bloques.clear()
        estado = aplicarMedicion(
            EstadoMonstruoMemoria(
                limiteSeguroMb = estado.limiteSeguroMb,
                memoriaDisponibleSistemaMb = estado.memoriaDisponibleSistemaMb
            )
        )
        onEvento(EventoRegistroMonstruoMemoria.ReinicioConfirmado)
    }

    fun actualizarMedicionSistema(memoriaDisponibleSistemaMb: Long?) {
        estado = estado.copy(memoriaDisponibleSistemaMb = memoriaDisponibleSistemaMb)
    }

    fun limpiarRecursos() {
        idOperacionActiva++
        bloques.clear()
    }

    override fun onCleared() {
        limpiarRecursos()
        super.onCleared()
    }

    private fun publicarResultadoReserva(
        bloque: BloqueMemoriaReservada,
        onEvento: (EventoRegistroMonstruoMemoria) -> Unit
    ) {
        val memoriaReservadaMb = bloques.sumOf { it.tamanoMb }
        val porcentaje = if (estado.limiteSeguroMb > 0) {
            (memoriaReservadaMb * 100) / estado.limiteSeguroMb
        } else {
            0
        }
        val esAdvertencia = porcentaje >= UMBRAL_ADVERTENCIA_PORCENTAJE

        estado = aplicarMedicion(
            estado.copy(
                estadoEjecucion = if (esAdvertencia) {
                    EstadoEjecucionMemoria.Advertencia
                } else {
                    EstadoEjecucionMemoria.Exitoso
                },
                mensajeAdvertencia = if (esAdvertencia) {
                    "Presion de memoria: $porcentaje% del limite seguro."
                } else {
                    null
                },
                mensajeError = null,
                resultadoUltimaAccion = "Reservados ${bloque.tamanoMb} MB (bloque #${bloque.id})"
            )
        )

        onEvento(EventoRegistroMonstruoMemoria.ReservaCompletada(bloque.id, bloque.tamanoMb, memoriaReservadaMb))
        if (esAdvertencia) {
            onEvento(EventoRegistroMonstruoMemoria.AdvertenciaPresion(porcentaje))
        }
    }

    private fun aplicarMedicion(base: EstadoMonstruoMemoria): EstadoMonstruoMemoria {
        val runtime = Runtime.getRuntime()
        val memoriaUsadaMb = bytesAMb(runtime.totalMemory() - runtime.freeMemory())
        val memoriaLibreRuntimeMb = bytesAMb(runtime.freeMemory())
        val memoriaMaximaRuntimeMb = bytesAMb(runtime.maxMemory())
        val memoriaReservadaMb = bloques.sumOf { it.tamanoMb }
        val bloquesReservados = bloques.size

        val muestra = MuestraMemoria(
            timestamp = System.currentTimeMillis(),
            memoriaUsadaMb = memoriaUsadaMb,
            memoriaLibreRuntimeMb = memoriaLibreRuntimeMb,
            memoriaMaximaRuntimeMb = memoriaMaximaRuntimeMb,
            memoriaDisponibleSistemaMb = base.memoriaDisponibleSistemaMb,
            bloquesReservados = bloquesReservados
        )

        return recalcularControles(
            base.copy(
                memoriaUsadaMb = memoriaUsadaMb,
                memoriaLibreRuntimeMb = memoriaLibreRuntimeMb,
                memoriaMaximaRuntimeMb = memoriaMaximaRuntimeMb,
                memoriaReservadaMb = memoriaReservadaMb,
                bloquesReservados = bloquesReservados,
                estadoVisual = calcularEstadoVisual(memoriaReservadaMb, base.limiteSeguroMb),
                historico = (base.historico + muestra).takeLast(MAXIMO_HISTORICO)
            )
        )
    }

    private fun recalcularControles(base: EstadoMonstruoMemoria): EstadoMonstruoMemoria {
        val puedeOperar = base.estadoEjecucion != EstadoEjecucionMemoria.Reservando
        return base.copy(
            puedeReservar10Mb = puedeOperar,
            puedeReservar25Mb = puedeOperar,
            puedeReservar50Mb = puedeOperar,
            puedeLiberar = puedeOperar && base.bloquesReservados > 0,
            puedeSolicitarGc = puedeOperar,
            puedeReiniciar = puedeOperar,
            hayTrabajoActivo = base.bloquesReservados > 0 || base.estadoEjecucion == EstadoEjecucionMemoria.Reservando
        )
    }

    private fun calcularLimiteSeguroMb(): Int {
        val maximaMb = bytesAMb(Runtime.getRuntime().maxMemory()).toInt()
        return minOf(LIMITE_SEGURO_MAXIMO_MB, maximaMb / 2)
    }

    private fun calcularEstadoVisual(memoriaReservadaMb: Int, limiteSeguroMb: Int): EstadoVisualMonstruo {
        if (limiteSeguroMb <= 0) return EstadoVisualMonstruo.Saludable
        val porcentaje = (memoriaReservadaMb * 100) / limiteSeguroMb
        return when {
            porcentaje >= 80 -> EstadoVisualMonstruo.PresionMemoria
            porcentaje >= 50 -> EstadoVisualMonstruo.Alto
            porcentaje >= 25 -> EstadoVisualMonstruo.Moderado
            else -> EstadoVisualMonstruo.Saludable
        }
    }

    private fun bytesAMb(bytes: Long): Long = bytes / BYTES_POR_MB

    companion object {
        private const val BYTES_POR_MB = 1024 * 1024
        private const val PASO_PAGINA_BYTES = 4096
        private const val LIMITE_SEGURO_MAXIMO_MB = 200
        private const val UMBRAL_ADVERTENCIA_PORCENTAJE = 80
        private const val MAXIMO_HISTORICO = 30
    }
}
