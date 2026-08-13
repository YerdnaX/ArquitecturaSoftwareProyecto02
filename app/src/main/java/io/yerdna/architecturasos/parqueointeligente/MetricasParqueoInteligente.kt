package io.yerdna.architecturasos.parqueointeligente

data class MetricasParqueoInteligente(
    val permisosTotales: Int = ConfiguracionParqueoInteligente.ESPACIOS_INICIALES,
    val permisosDisponibles: Int = permisosTotales,
    val recursosOcupados: Int = 0,
    val hilosEsperando: Int = 0,
    val vehiculosFinalizados: Int = 0,
    val vehiculosCancelados: Int = 0,
    val vehiculosConError: Int = 0,
    val maximoVehiculosSimultaneos: Int = 0,
    val tiempoTotalMs: Long = 0L
)
