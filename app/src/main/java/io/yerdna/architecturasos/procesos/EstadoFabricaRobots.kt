package io.yerdna.architecturasos.procesos

import android.os.Process

enum class EstadoEjecucionFabrica {
    Inactivo,
    Iniciando,
    Ejecucion,
    Deteniendo,
    Error
}

enum class ResultadoUltimaEjecucion {
    SinEjecucion,
    Completado,
    Cancelado,
    Error
}

data class EstadoFabricaRobots(
    val pidPrincipal: Int = Process.myPid(),
    val pidSecundario: Int? = null,
    val estado: EstadoEjecucionFabrica = EstadoEjecucionFabrica.Inactivo,
    val resultadoUltimaEjecucion: ResultadoUltimaEjecucion = ResultadoUltimaEjecucion.SinEjecucion,
    val robotsConfigurados: Int = ROBOTS_POR_DEFECTO,
    val robotsEnsamblados: Int = 0,
    val mensajesIntercambiados: Int = 0,
    val horaInicio: Long? = null,
    val horaFin: Long? = null,
    val mensajeError: String? = null
)

const val ROBOTS_POR_DEFECTO = 60
const val ROBOTS_MINIMOS = 1
const val UMBRAL_ADVERTENCIA_ROBOTS = 300
