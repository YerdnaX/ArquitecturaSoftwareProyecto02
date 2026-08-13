package io.yerdna.architecturasos.carreraboletos

const val TAG_CARRERA_BOLETOS = "OSPlayground/Mutex"

enum class FaseCarreraBoletos {
    Inactivo,
    Preparando,
    EjecutandoSinMutex,
    EjecutandoConMutex,
    Exitoso,
    Cancelado,
    Error
}

data class ConfiguracionCarreraBoletos(
    val boletosIniciales: Int = BOLETOS_INICIALES,
    val compradoresTotales: Int = COMPRADORES_INICIALES
) {
    // Ajusta la configuración para que sus valores queden dentro de los rangos permitidos
    fun normalizada(): ConfiguracionCarreraBoletos {
        return copy(
            boletosIniciales = boletosIniciales.coerceIn(MIN_BOLETOS, MAX_BOLETOS),
            compradoresTotales = compradoresTotales.coerceIn(MIN_COMPRADORES, MAX_COMPRADORES)
        )
    }

    companion object {
        const val BOLETOS_INICIALES = 1
        const val COMPRADORES_INICIALES = 8
        const val MIN_BOLETOS = 1
        const val MAX_BOLETOS = 5
        const val MIN_COMPRADORES = 2
        const val MAX_COMPRADORES = 20
    }
}

data class EstadoCarreraBoletos(
    val fase: FaseCarreraBoletos = FaseCarreraBoletos.Inactivo,
    val configuracion: ConfiguracionCarreraBoletos = ConfiguracionCarreraBoletos(),
    val compradores: List<CompradorBoleto> = compradoresIniciales(ConfiguracionCarreraBoletos()),
    val metricas: MetricasCarreraBoletos = MetricasCarreraBoletos(),
    val resultadoSinMutex: ResultadoCarreraBoletos? = null,
    val resultadoConMutex: ResultadoCarreraBoletos? = null,
    val mensajeError: String? = null,
    val ejecucionActiva: Boolean = false
)

sealed class EventoRegistroCarreraBoletos {
    data class ConfiguracionAplicada(
        val boletosIniciales: Int,
        val compradoresTotales: Int
    ) : EventoRegistroCarreraBoletos()

    data class EjecucionIniciada(val idEjecucion: Int) : EventoRegistroCarreraBoletos()
    data class ModoSeleccionado(val modo: ModoCarreraBoletos) : EventoRegistroCarreraBoletos()
    data class CompradorIntentandoEntrar(val idComprador: Int) : EventoRegistroCarreraBoletos()
    data class CompradorEsperandoMutex(val idComprador: Int) : EventoRegistroCarreraBoletos()
    data class MutexAdquirido(val idComprador: Int) : EventoRegistroCarreraBoletos()
    data class VentaRealizada(val idComprador: Int, val boletosRestantes: Int) : EventoRegistroCarreraBoletos()
    data class VentaIncorrectaDetectada(val idComprador: Int, val ventasRegistradas: Int) : EventoRegistroCarreraBoletos()
    data class MutexLiberado(val idComprador: Int) : EventoRegistroCarreraBoletos()
    data class EjecucionFinalizada(val resultado: ResultadoCarreraBoletos) : EventoRegistroCarreraBoletos()
    data object EjecucionCancelada : EventoRegistroCarreraBoletos()
    data object SalidaConfirmada : EventoRegistroCarreraBoletos()
    data class ErrorTecnico(val mensaje: String?) : EventoRegistroCarreraBoletos()
}

// Genera la lista inicial de compradores en estado de espera según la configuración dada
fun compradoresIniciales(configuracion: ConfiguracionCarreraBoletos): List<CompradorBoleto> {
    return (1..configuracion.normalizada().compradoresTotales).map { id ->
        CompradorBoleto(
            id = id,
            nombre = "Comprador $id"
        )
    }
}
