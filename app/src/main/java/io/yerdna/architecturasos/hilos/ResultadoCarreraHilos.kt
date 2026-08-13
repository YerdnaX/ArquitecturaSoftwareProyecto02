package io.yerdna.architecturasos.hilos

data class ResultadoCarreraHilos(
    val idEjecucion: Int,
    val cantidadHilos: Int,
    val cantidadTrabajo: Int,
    val estadoFinal: EstadoCarreraHilos,
    val tiempoTotalMs: Long,
    val operacionesTotales: Long,
    val finalizados: Int,
    val cancelados: Int,
    val mensaje: String? = null
)
