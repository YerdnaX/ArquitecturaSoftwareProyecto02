package io.yerdna.architecturasos.diagnostico

import androidx.annotation.StringRes

enum class CategoriaDiagnostico {
    PROCESOS_CPU,
    MEMORIA,
    LOGS,
    IPC,
    SOCKETS,
    SERVICIOS,
    TRACING,
    REPORTE_SISTEMA,
    ENERGIA
}

data class ComandoDiagnostico(
    val comando: String,
    @param:StringRes val descripcionResId: Int,
    @param:StringRes val cuandoEjecutarloResId: Int,
    @param:StringRes val comoInterpretarloResId: Int
)

data class RelacionExperimentoDiagnostico(
    @param:StringRes val nombreExperimentoResId: Int,
    @param:StringRes val mecanismoObservableResId: Int
)

data class HerramientaDiagnostico(
    val id: String,
    @param:StringRes val nombreResId: Int,
    val categoria: CategoriaDiagnostico,
    @param:StringRes val descripcionResId: Int,
    @param:StringRes val cuandoUsarlaResId: Int,
    @param:StringRes val queValidaResId: Int,
    @param:StringRes val limitacionResId: Int,
    val comandos: List<ComandoDiagnostico> = emptyList(),
    val experimentosRelacionados: List<RelacionExperimentoDiagnostico> = emptyList()
)
