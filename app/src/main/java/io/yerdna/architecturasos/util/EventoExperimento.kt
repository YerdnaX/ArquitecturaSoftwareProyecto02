package io.yerdna.architecturasos.util

data class EventoExperimento(
    val timestamp: Long,
    val tipo: TipoEvento,
    val mensaje: String,
    val origen: OrigenEvento
)
