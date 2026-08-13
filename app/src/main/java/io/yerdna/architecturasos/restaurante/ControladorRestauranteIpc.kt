package io.yerdna.architecturasos.restaurante

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.KEY_EVENTO_MENSAJE
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.KEY_EVENTO_ORIGEN
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.KEY_EVENTO_TIMESTAMP
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.KEY_EVENTO_TIPO
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.KEY_ID_ORDEN
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.KEY_MENSAJES_INTERCAMBIADOS
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.KEY_MENSAJE_ERROR
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.KEY_ORDEN
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.KEY_ORDENES_EN_COLA
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.KEY_PID_DESTINO
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.KEY_PID_ORIGEN
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.KEY_RESPUESTA
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.KEY_TIMESTAMP
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.KEY_TOTAL_PROCESADAS
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.MSG_COCINA_CONECTADA
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.MSG_DESCONECTAR_CLIENTE
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.MSG_ENVIAR_ORDEN
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.MSG_ERROR_IPC
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.MSG_EVENTO_REGISTRO
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.MSG_ORDEN_EN_COLA
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.MSG_ORDEN_PROCESANDO
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.MSG_ORDEN_RECIBIDA
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.MSG_REGISTRAR_CLIENTE
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.MSG_RESPUESTA_ENVIADA
import io.yerdna.architecturasos.util.EventoExperimento
import io.yerdna.architecturasos.util.OrigenEvento
import io.yerdna.architecturasos.util.TipoEvento
import java.lang.ref.WeakReference

class ControladorRestauranteIpc(
    private val applicationContext: Context,
    private val callbacks: Callbacks
) {
    interface Callbacks {
        // Notifica que la cocina se conecto, con su pid y cuantos mensajes se han intercambiado
        fun onCocinaConectada(pidCocina: Int, timestamp: Long, mensajes: Int)
        // Notifica que una orden quedo en cola de espera
        fun onOrdenEnCola(evento: EventoOrdenRestaurante)
        // Notifica que la cocina recibio una orden
        fun onOrdenRecibida(evento: EventoOrdenRestaurante)
        // Notifica que la cocina esta procesando una orden
        fun onOrdenProcesando(evento: EventoOrdenRestaurante)
        // Notifica que la respuesta de una orden fue enviada de vuelta
        fun onRespuestaEnviada(evento: EventoOrdenRestaurante)
        // Notifica que el servicio de la cocina se desconecto
        fun onServicioDesconectado()
        // Notifica un error, indicando si rompe la conexion y cuantos mensajes se llevan intercambiados
        fun onError(mensaje: String, rompeConexion: Boolean, mensajes: Int)
        // Notifica un nuevo evento de registro para mostrar en el log
        fun onEventoRegistro(evento: EventoExperimento)
    }

    private val intentServicio = Intent(applicationContext, ServicioRestauranteIpc::class.java)
    private val handlerRespuesta = ManejadorRespuesta(this)
    private val messengerCliente = Messenger(handlerRespuesta)
    private var messengerServicio: Messenger? = null
    private var enlazado = false
    private var liberado = true

    private val conexion = object : ServiceConnection {
        // Guarda el messenger del servicio al enlazarse y envia el registro del cliente
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            enlazado = true
            liberado = false
            messengerServicio = Messenger(service)
            registrarEvento(TipoEvento.Informacion, "Servicio Restaurante IPC enlazado")
            enviarRegistroCliente()
        }

        // Limpia la referencia al servicio y notifica la desconexion
        override fun onServiceDisconnected(name: ComponentName?) {
            enlazado = false
            messengerServicio = null
            callbacks.onServicioDesconectado()
        }
    }

    // Inicia y enlaza el servicio de la cocina si aun no hay una conexion activa
    fun conectar() {
        if (enlazado || messengerServicio != null) return
        liberado = false
        try {
            applicationContext.startService(intentServicio)
            applicationContext.bindService(intentServicio, conexion, Context.BIND_AUTO_CREATE)
        } catch (exception: RuntimeException) {
            registrarEvento(TipoEvento.Error, "No se pudo conectar con la cocina")
            liberar(detenerServicio = true)
            callbacks.onError("No se pudo conectar con la cocina", true, 0)
        }
    }

    // Empaqueta y envia una orden al servicio de la cocina a traves del messenger
    fun enviarOrden(orden: String, idOrden: Int, pidOrigen: Int) {
        val destino = messengerServicio
        if (destino == null) {
            callbacks.onError("No hay conexion disponible con la cocina", true, 0)
            return
        }

        val datos = Bundle().apply {
            putInt(KEY_ID_ORDEN, idOrden)
            putString(KEY_ORDEN, orden)
            putInt(KEY_PID_ORIGEN, pidOrigen)
            putLong(KEY_TIMESTAMP, System.currentTimeMillis())
        }
        try {
            destino.send(
                Message.obtain(null, MSG_ENVIAR_ORDEN).apply {
                    data = datos
                    replyTo = messengerCliente
                }
            )
        } catch (exception: RemoteException) {
            registrarEvento(TipoEvento.Error, "No se pudo enviar orden a la cocina")
            liberar(detenerServicio = true)
            callbacks.onError("No se pudo enviar la orden a la cocina", true, 0)
        }
    }

    // Avisa al servicio que el cliente se desconecta y libera la conexion, deteniendo el servicio
    fun desconectar() {
        enviarDesconexionSiDisponible()
        liberar(detenerServicio = true)
    }

    // Desconecta la conexion actual y ejecuta la accion de reconexion indicada
    fun reiniciar(onReconectar: () -> Unit) {
        desconectar()
        onReconectar()
    }

    // Libera la conexion actual deteniendo tambien el servicio
    fun liberar() {
        liberar(detenerServicio = true)
    }

    // Envia el mensaje de registro del cliente hacia el servicio
    private fun enviarRegistroCliente() {
        enviarMensaje(MSG_REGISTRAR_CLIENTE) { replyTo = messengerCliente }
    }

    // Envia el mensaje de desconexion del cliente si aun hay un servicio disponible
    private fun enviarDesconexionSiDisponible() {
        if (messengerServicio == null) return
        enviarMensaje(MSG_DESCONECTAR_CLIENTE) { replyTo = messengerCliente }
    }

    // Construye y envia un mensaje del tipo indicado al servicio, aplicando configuracion adicional
    private fun enviarMensaje(tipo: Int, configurar: Message.() -> Unit = {}) {
        val destino = messengerServicio ?: return
        try {
            destino.send(Message.obtain(null, tipo).apply(configurar))
        } catch (exception: RemoteException) {
            registrarEvento(TipoEvento.Error, "No se pudo enviar mensaje al servicio")
            callbacks.onError("No se pudo comunicar con la cocina", true, 0)
        }
    }

    // Desenlaza el servicio, limpia el estado interno y opcionalmente lo detiene
    private fun liberar(detenerServicio: Boolean) {
        if (liberado) return
        liberado = true

        if (enlazado) {
            try {
                applicationContext.unbindService(conexion)
            } catch (_: IllegalArgumentException) {
                // La limpieza puede llamarse desde salida, reinicio o errores.
            }
        }

        enlazado = false
        messengerServicio = null
        handlerRespuesta.removeCallbacksAndMessages(null)

        if (detenerServicio) {
            applicationContext.stopService(intentServicio)
        }
    }

    // Construye un evento de registro con el tipo y mensaje dados y lo reporta a los callbacks
    private fun registrarEvento(tipo: TipoEvento, mensaje: String) {
        callbacks.onEventoRegistro(
            EventoExperimento(
                timestamp = System.currentTimeMillis(),
                tipo = tipo,
                mensaje = mensaje,
                origen = OrigenEvento.ProcesoPrincipal
            )
        )
    }

    private class ManejadorRespuesta(
        controlador: ControladorRestauranteIpc
    ) : Handler(Looper.getMainLooper()) {
        private val referenciaControlador = WeakReference(controlador)

        // Interpreta el mensaje IPC recibido del servicio y despacha el callback correspondiente
        override fun handleMessage(msg: Message) {
            val controlador = referenciaControlador.get() ?: return
            val mensajes = msg.data.getInt(KEY_MENSAJES_INTERCAMBIADOS, 0)
            when (msg.what) {
                MSG_COCINA_CONECTADA -> controlador.callbacks.onCocinaConectada(
                    pidCocina = msg.data.getInt(KEY_PID_DESTINO),
                    timestamp = msg.data.getLong(KEY_TIMESTAMP, System.currentTimeMillis()),
                    mensajes = mensajes
                )
                MSG_ORDEN_EN_COLA -> controlador.callbacks.onOrdenEnCola(msg.aEventoOrden())
                MSG_ORDEN_RECIBIDA -> controlador.callbacks.onOrdenRecibida(msg.aEventoOrden())
                MSG_ORDEN_PROCESANDO -> controlador.callbacks.onOrdenProcesando(msg.aEventoOrden())
                MSG_RESPUESTA_ENVIADA -> controlador.callbacks.onRespuestaEnviada(msg.aEventoOrden())
                MSG_ERROR_IPC -> controlador.callbacks.onError(
                    msg.data.getString(KEY_MENSAJE_ERROR).orEmpty(),
                    false,
                    mensajes
                )
                MSG_EVENTO_REGISTRO -> controlador.callbacks.onEventoRegistro(msg.aEventoRegistro())
                else -> super.handleMessage(msg)
            }
        }

        // Extrae del mensaje IPC los datos de una orden y los convierte en un evento de dominio
        private fun Message.aEventoOrden(): EventoOrdenRestaurante {
            return EventoOrdenRestaurante(
                idOrden = data.getInt(KEY_ID_ORDEN),
                orden = data.getString(KEY_ORDEN).orEmpty(),
                respuesta = data.getString(KEY_RESPUESTA).orEmpty(),
                pidOrigen = data.getInt(KEY_PID_ORIGEN),
                pidDestino = data.getInt(KEY_PID_DESTINO),
                timestamp = data.getLong(KEY_TIMESTAMP, System.currentTimeMillis()),
                ordenesEnCola = data.getInt(KEY_ORDENES_EN_COLA),
                totalProcesadas = data.getInt(KEY_TOTAL_PROCESADAS),
                mensajesIntercambiados = data.getInt(KEY_MENSAJES_INTERCAMBIADOS)
            )
        }

        // Extrae del mensaje IPC los datos de un evento de registro y los convierte en un EventoExperimento
        private fun Message.aEventoRegistro(): EventoExperimento {
            val tipo = runCatching {
                TipoEvento.valueOf(data.getString(KEY_EVENTO_TIPO).orEmpty())
            }.getOrDefault(TipoEvento.Informacion)

            return EventoExperimento(
                timestamp = data.getLong(KEY_EVENTO_TIMESTAMP, System.currentTimeMillis()),
                tipo = tipo,
                mensaje = data.getString(KEY_EVENTO_MENSAJE).orEmpty(),
                origen = obtenerOrigen(data.getString(KEY_EVENTO_ORIGEN))
            )
        }

        // Convierte el texto recibido en el origen del evento, usando Servicio como valor por defecto
        private fun obtenerOrigen(valor: String?): OrigenEvento {
            return runCatching {
                OrigenEvento.valueOf(valor.orEmpty())
            }.getOrDefault(OrigenEvento.Servicio)
        }
    }
}

data class EventoOrdenRestaurante(
    val idOrden: Int,
    val orden: String,
    val respuesta: String,
    val pidOrigen: Int,
    val pidDestino: Int,
    val timestamp: Long,
    val ordenesEnCola: Int,
    val totalProcesadas: Int,
    val mensajesIntercambiados: Int
)
