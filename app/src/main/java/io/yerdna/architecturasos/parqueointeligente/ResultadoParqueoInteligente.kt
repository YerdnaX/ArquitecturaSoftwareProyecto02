package io.yerdna.architecturasos.parqueointeligente

data class ResultadoParqueoInteligente(
    val permisosTotales: Int,
    val vehiculosTotales: Int,
    val vehiculosFinalizados: Int,
    val vehiculosCancelados: Int,
    val vehiculosConError: Int,
    val maximoVehiculosSimultaneos: Int,
    val duracionTotalMs: Long
)
