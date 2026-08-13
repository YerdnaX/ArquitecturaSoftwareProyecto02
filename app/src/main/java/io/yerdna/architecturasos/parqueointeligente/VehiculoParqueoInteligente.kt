package io.yerdna.architecturasos.parqueointeligente

data class VehiculoParqueoInteligente(
    val id: Int,
    val nombre: String,
    val estado: EstadoVehiculoParqueoInteligente = EstadoVehiculoParqueoInteligente.Esperando,
    val horaInicioMs: Long? = null,
    val horaEntradaMs: Long? = null,
    val horaSalidaMs: Long? = null,
    val tiempoEsperaMs: Long? = null,
    val tiempoEstacionadoMs: Long? = null,
    val mensaje: String? = null
)

enum class EstadoVehiculoParqueoInteligente {
    Esperando,
    Entrando,
    Estacionado,
    Saliendo,
    Finalizado,
    Cancelado,
    Error
}
