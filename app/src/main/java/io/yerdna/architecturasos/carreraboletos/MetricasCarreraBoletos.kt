package io.yerdna.architecturasos.carreraboletos

enum class ModoCarreraBoletos {
    SinMutex,
    ConMutex
}

data class MetricasCarreraBoletos(
    val modo: ModoCarreraBoletos? = null,
    val boletosIniciales: Int = ConfiguracionCarreraBoletos.BOLETOS_INICIALES,
    val boletosRestantes: Int = ConfiguracionCarreraBoletos.BOLETOS_INICIALES,
    val compradoresTotales: Int = ConfiguracionCarreraBoletos.COMPRADORES_INICIALES,
    val ventasCorrectas: Int = 0,
    val ventasIncorrectas: Int = 0,
    val compradoresEsperando: Int = 0,
    val compradorEnSeccionCritica: String? = null,
    val tiempoTotalMs: Long = 0L
)
