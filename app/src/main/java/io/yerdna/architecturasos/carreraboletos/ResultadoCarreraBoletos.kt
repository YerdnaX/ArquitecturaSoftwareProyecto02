package io.yerdna.architecturasos.carreraboletos

data class ResultadoCarreraBoletos(
    val modo: ModoCarreraBoletos,
    val boletosIniciales: Int,
    val ventasRegistradas: Int,
    val boletosRestantes: Int,
    val ventasIncorrectas: Int,
    val huboErrorDeConcurrencia: Boolean,
    val tiempoTotalMs: Long
)
