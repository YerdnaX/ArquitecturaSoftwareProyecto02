package io.yerdna.architecturasos.parqueointeligente

const val TAG_PARQUEO_INTELIGENTE = "OSPlayground/Semaphore"

enum class FaseParqueoInteligente {
    Inactivo,
    Ejecutando,
    Exitoso,
    Cancelado,
    Error
}

data class ConfiguracionParqueoInteligente(
    val espaciosDisponibles: Int = ESPACIOS_INICIALES,
    val vehiculosTotales: Int = VEHICULOS_INICIALES
) {
    // Ajusta espacios y vehiculos a los rangos minimo y maximo permitidos
    fun normalizada(): ConfiguracionParqueoInteligente {
        return copy(
            espaciosDisponibles = espaciosDisponibles.coerceIn(MIN_ESPACIOS, MAX_ESPACIOS),
            vehiculosTotales = vehiculosTotales.coerceIn(MIN_VEHICULOS, MAX_VEHICULOS)
        )
    }

    companion object {
        const val ESPACIOS_INICIALES = 3
        const val VEHICULOS_INICIALES = 8
        const val MIN_ESPACIOS = 1
        const val MAX_ESPACIOS = 6
        const val MIN_VEHICULOS = 1
        const val MAX_VEHICULOS = 12
    }
}

data class EstadoParqueoInteligente(
    val fase: FaseParqueoInteligente = FaseParqueoInteligente.Inactivo,
    val configuracion: ConfiguracionParqueoInteligente = ConfiguracionParqueoInteligente(),
    val vehiculos: List<VehiculoParqueoInteligente> = vehiculosIniciales(ConfiguracionParqueoInteligente()),
    val metricas: MetricasParqueoInteligente = MetricasParqueoInteligente(),
    val resultadoUltimaEjecucion: ResultadoParqueoInteligente? = null,
    val mensajeError: String? = null,
    val ejecucionActiva: Boolean = false
)

sealed class EventoRegistroParqueoInteligente {
    data class ConfiguracionAplicada(
        val espaciosDisponibles: Int,
        val vehiculosTotales: Int
    ) : EventoRegistroParqueoInteligente()

    data class EjecucionIniciada(val idEjecucion: Int) : EventoRegistroParqueoInteligente()
    data class VehiculoEsperando(val idVehiculo: Int) : EventoRegistroParqueoInteligente()
    data class PermisoAdquirido(val idVehiculo: Int, val permisosDisponibles: Int) : EventoRegistroParqueoInteligente()
    data class VehiculoEstacionado(val idVehiculo: Int) : EventoRegistroParqueoInteligente()
    data class VehiculoSaliendo(val idVehiculo: Int) : EventoRegistroParqueoInteligente()
    data class PermisoLiberado(val idVehiculo: Int, val permisosDisponibles: Int) : EventoRegistroParqueoInteligente()
    data class VehiculoFinalizado(val idVehiculo: Int) : EventoRegistroParqueoInteligente()
    data class EjecucionFinalizada(val resultado: ResultadoParqueoInteligente) : EventoRegistroParqueoInteligente()
    data object EjecucionCancelada : EventoRegistroParqueoInteligente()
    data object SalidaConfirmada : EventoRegistroParqueoInteligente()
    data class ErrorTecnico(val mensaje: String?) : EventoRegistroParqueoInteligente()
}

// Crea la lista inicial de vehiculos en espera segun la configuracion normalizada
fun vehiculosIniciales(configuracion: ConfiguracionParqueoInteligente): List<VehiculoParqueoInteligente> {
    return (1..configuracion.normalizada().vehiculosTotales).map { id ->
        VehiculoParqueoInteligente(
            id = id,
            nombre = "Vehiculo $id"
        )
    }
}
