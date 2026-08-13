package io.yerdna.architecturasos.bancocaotico

const val TAG_BANCO_CAOTICO = "OSPlayground/ChaosBank"

enum class FaseBancoCaotico {
    Inactivo,
    Ejecutando,
    Exitoso,
    Cancelado,
    Error
}

enum class EstadoCajeroBancoCaotico {
    Esperando,
    Trabajando,
    Finalizado,
    Cancelado,
    Error
}

data class ConfiguracionBancoCaotico(
    val saldoInicial: Int = 0,
    val cantidadCajeros: Int = 8,
    val operacionesPorCajero: Int = 8,
    val montoPorOperacion: Int = 1
) {
    // Ajusta la configuración para que sus valores queden dentro de los rangos permitidos
    fun normalizada(): ConfiguracionBancoCaotico {
        return copy(
            cantidadCajeros = cantidadCajeros.coerceIn(MIN_CAJEROS, MAX_CAJEROS),
            operacionesPorCajero = operacionesPorCajero.coerceIn(MIN_OPERACIONES, MAX_OPERACIONES),
            montoPorOperacion = 1
        )
    }

    companion object {
        const val MIN_CAJEROS = 2
        const val MAX_CAJEROS = 8
        const val MIN_OPERACIONES = 8
        const val MAX_OPERACIONES = 200_000
    }
}

data class CajeroBancoCaotico(
    val id: Int,
    val nombre: String,
    val estado: EstadoCajeroBancoCaotico = EstadoCajeroBancoCaotico.Esperando,
    val operacionesCompletadas: Int = 0,
    val ultimoSaldoLeido: Int? = null,
    val ultimoSaldoEscrito: Int? = null
)

data class ResultadoBancoCaotico(
    val saldoInicial: Int,
    val resultadoEsperado: Int,
    val resultadoReal: Int,
    val diferencia: Int,
    val carreraDetectada: Boolean,
    val duracionMs: Long,
    val cajerosFinalizados: Int
)

data class EstadoBancoCaotico(
    val fase: FaseBancoCaotico = FaseBancoCaotico.Inactivo,
    val configuracion: ConfiguracionBancoCaotico = ConfiguracionBancoCaotico(),
    val cajeros: List<CajeroBancoCaotico> = cajerosIniciales(ConfiguracionBancoCaotico()),
    val resultadoUltimaEjecucion: ResultadoBancoCaotico? = null,
    val mensajeError: String? = null,
    val ejecucionActiva: Boolean = false
)

sealed class EventoRegistroBancoCaotico {
    data class ConfiguracionAplicada(
        val cantidadCajeros: Int,
        val operacionesPorCajero: Int
    ) : EventoRegistroBancoCaotico()

    data class EjecucionIniciada(val idEjecucion: Int) : EventoRegistroBancoCaotico()
    data class CajeroIniciado(val idCajero: Int) : EventoRegistroBancoCaotico()
    data class ProgresoCajero(
        val idCajero: Int,
        val operacionesCompletadas: Int
    ) : EventoRegistroBancoCaotico()

    data class CajeroFinalizado(val idCajero: Int) : EventoRegistroBancoCaotico()
    data class CajeroCancelado(val idCajero: Int) : EventoRegistroBancoCaotico()
    data class EjecucionFinalizada(val resultado: ResultadoBancoCaotico) : EventoRegistroBancoCaotico()
    data object EjecucionCancelada : EventoRegistroBancoCaotico()
    data class ErrorTecnico(val mensaje: String?) : EventoRegistroBancoCaotico()
}

// Genera la lista inicial de cajeros en estado de espera según la configuración dada
fun cajerosIniciales(configuracion: ConfiguracionBancoCaotico): List<CajeroBancoCaotico> {
    return (1..configuracion.normalizada().cantidadCajeros).map { id ->
        CajeroBancoCaotico(
            id = id,
            nombre = "BancoCaotico-$id"
        )
    }
}
