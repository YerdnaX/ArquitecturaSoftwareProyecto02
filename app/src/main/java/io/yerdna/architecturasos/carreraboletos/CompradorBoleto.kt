package io.yerdna.architecturasos.carreraboletos

data class CompradorBoleto(
    val id: Int,
    val nombre: String,
    val estado: EstadoCompradorBoleto = EstadoCompradorBoleto.Esperando,
    val intentos: Int = 0,
    val boletosComprados: Int = 0,
    val mensaje: String? = null
)

enum class EstadoCompradorBoleto {
    Esperando,
    IntentandoEntrar,
    EsperandoMutex,
    EnSeccionCritica,
    Compro,
    SinBoleto,
    Finalizado,
    Cancelado,
    Error
}
