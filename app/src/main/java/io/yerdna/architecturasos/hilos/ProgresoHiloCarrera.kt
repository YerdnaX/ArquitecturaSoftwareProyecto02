package io.yerdna.architecturasos.hilos

data class ProgresoHiloCarrera(
    val idHilo: Int,
    val nombreHilo: String,
    val estado: EstadoHiloCarrera = EstadoHiloCarrera.Esperando,
    val progreso: Float = 0f,
    val operacionesCompletadas: Long = 0L,
    val operacionesTotales: Long = 0L,
    val tiempoMs: Long = 0L,
    val mensaje: String? = null
)
