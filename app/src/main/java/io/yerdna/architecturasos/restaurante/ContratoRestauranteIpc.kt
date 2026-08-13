package io.yerdna.architecturasos.restaurante

object ContratoRestauranteIpc {
    const val MSG_REGISTRAR_CLIENTE = 1
    const val MSG_ENVIAR_ORDEN = 2
    const val MSG_DESCONECTAR_CLIENTE = 3
    const val MSG_COCINA_CONECTADA = 4
    const val MSG_ORDEN_EN_COLA = 5
    const val MSG_ORDEN_RECIBIDA = 6
    const val MSG_ORDEN_PROCESANDO = 7
    const val MSG_RESPUESTA_ENVIADA = 8
    const val MSG_ERROR_IPC = 9
    const val MSG_EVENTO_REGISTRO = 10

    const val KEY_ID_ORDEN = "id_orden"
    const val KEY_ORDEN = "orden"
    const val KEY_RESPUESTA = "respuesta"
    const val KEY_PID_ORIGEN = "pid_origen"
    const val KEY_PID_DESTINO = "pid_destino"
    const val KEY_TIMESTAMP = "timestamp"
    const val KEY_MENSAJE_ERROR = "mensaje_error"
    const val KEY_ORDENES_EN_COLA = "ordenes_en_cola"
    const val KEY_TOTAL_PROCESADAS = "total_procesadas"
    const val KEY_MENSAJES_INTERCAMBIADOS = "mensajes_intercambiados"
    const val KEY_EVENTO_TIPO = "evento_tipo"
    const val KEY_EVENTO_MENSAJE = "evento_mensaje"
    const val KEY_EVENTO_TIMESTAMP = "evento_timestamp"
    const val KEY_EVENTO_ORIGEN = "evento_origen"

    const val TAG_LOGCAT = "OSPlayground/BinderIPC"
    const val DEMORA_PREPARACION_MS = 1_500L
}
