package io.yerdna.architecturasos.procesos

object ContratoFabricaRobots {
    const val MSG_REGISTRAR_CLIENTE = 1
    const val MSG_INICIAR_FABRICA = 2
    const val MSG_DETENER_FABRICA = 3
    const val MSG_PID_FABRICA = 4
    const val MSG_ESTADO_FABRICA = 5
    const val MSG_ROBOT_ENSAMBLADO = 6
    const val MSG_ERROR_FABRICA = 7
    const val MSG_EVENTO_REGISTRO = 8

    const val KEY_PID = "pid"
    const val KEY_ESTADO = "estado"
    const val KEY_ROBOTS_CONFIGURADOS = "robots_configurados"
    const val KEY_ROBOTS_ENSAMBLADOS = "robots_ensamblados"
    const val KEY_MENSAJES_INTERCAMBIADOS = "mensajes_intercambiados"
    const val KEY_MENSAJE_ERROR = "mensaje_error"
    const val KEY_EVENTO_TIPO = "evento_tipo"
    const val KEY_EVENTO_MENSAJE = "evento_mensaje"
    const val KEY_EVENTO_TIMESTAMP = "evento_timestamp"
    const val KEY_EVENTO_ORIGEN = "evento_origen"
}
