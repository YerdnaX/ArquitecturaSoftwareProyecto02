package io.yerdna.architecturasos.memoria

const val TAG_MONSTRUO_MEMORIA = "OSPlayground/Memory"

enum class EstadoEjecucionMemoria {
    Inactivo,
    Reservando,
    Exitoso,
    Advertencia,
    Error,
    Liberado,
    RecolectorSolicitado
}

enum class EstadoVisualMonstruo {
    Saludable,
    Moderado,
    Alto,
    PresionMemoria
}

class BloqueMemoriaReservada(
    val id: Int,
    val tamanoMb: Int,
    val bytes: ByteArray,
    val timestamp: Long
)

data class MuestraMemoria(
    val timestamp: Long,
    val memoriaUsadaMb: Long,
    val memoriaLibreRuntimeMb: Long,
    val memoriaMaximaRuntimeMb: Long,
    val memoriaDisponibleSistemaMb: Long?,
    val bloquesReservados: Int
)

data class EstadoMonstruoMemoria(
    val estadoEjecucion: EstadoEjecucionMemoria = EstadoEjecucionMemoria.Inactivo,
    val estadoVisual: EstadoVisualMonstruo = EstadoVisualMonstruo.Saludable,
    val memoriaUsadaMb: Long = 0,
    val memoriaLibreRuntimeMb: Long = 0,
    val memoriaMaximaRuntimeMb: Long = 0,
    val memoriaDisponibleSistemaMb: Long? = null,
    val memoriaReservadaMb: Int = 0,
    val bloquesReservados: Int = 0,
    val limiteSeguroMb: Int = 0,
    val historico: List<MuestraMemoria> = emptyList(),
    val resultadoUltimaAccion: String? = null,
    val mensajeAdvertencia: String? = null,
    val mensajeError: String? = null,
    val puedeReservar10Mb: Boolean = true,
    val puedeReservar25Mb: Boolean = true,
    val puedeReservar50Mb: Boolean = true,
    val puedeLiberar: Boolean = false,
    val puedeSolicitarGc: Boolean = true,
    val puedeReiniciar: Boolean = true,
    val hayTrabajoActivo: Boolean = false
)

sealed class EventoRegistroMonstruoMemoria {
    data object AperturaModulo : EventoRegistroMonstruoMemoria()
    data class IntentoReserva(val tamanoMb: Int) : EventoRegistroMonstruoMemoria()
    data class ReservaCompletada(
        val idBloque: Int,
        val tamanoMb: Int,
        val memoriaReservadaMb: Int
    ) : EventoRegistroMonstruoMemoria()

    data class ReservaBloqueada(val tamanoMb: Int, val memoriaReservadaMb: Int) : EventoRegistroMonstruoMemoria()
    data class AdvertenciaPresion(val porcentajeUsado: Int) : EventoRegistroMonstruoMemoria()
    data class LiberacionMemoria(val bloquesLiberados: Int) : EventoRegistroMonstruoMemoria()
    data object SolicitudGc : EventoRegistroMonstruoMemoria()
    data object ReinicioConfirmado : EventoRegistroMonstruoMemoria()
    data object SalidaConfirmada : EventoRegistroMonstruoMemoria()
    data class ErrorTecnico(val mensaje: String?) : EventoRegistroMonstruoMemoria()
}
