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
import android.util.Log
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
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.MSG_ORDEN_EN_COLA
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.MSG_ORDEN_PROCESANDO
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.MSG_ORDEN_RECIBIDA
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.MSG_REGISTRAR_CLIENTE
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.MSG_RESPUESTA_ENVIADA
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.TAG_LOGCAT
import java.lang.ref.WeakReference

class ControladorRestauranteIpc(
    private val applicationContext: Context,
    private val callbacks: Callbacks
) {
    interface Callbacks {
        fun onCocinaConectada(pidCocina: Int, timestamp: Long, mensajes: Int)
        fun onOrdenEnCola(evento: EventoOrdenRestaurante)
        fun onOrdenRecibida(evento: EventoOrdenRestaurante)
        fun onOrdenProcesando(evento: EventoOrdenRestaurante)
        fun onRespuestaEnviada(evento: EventoOrdenRestaurante)
        fun onServicioDesconectado()
        fun onError(mensaje: String, rompeConexion: Boolean, mensajes: Int)
    }

    private val intentServicio = Intent(applicationContext, ServicioRestauranteIpc::class.java)
    private val handlerRespuesta = ManejadorRespuesta(this)
    private val messengerCliente = Messenger(handlerRespuesta)
    private var messengerServicio: Messenger? = null
    private var enlazado = false
    private var liberado = true

    private val conexion = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            enlazado = true
            liberado = false
            messengerServicio = Messenger(service)
            Log.i(TAG_LOGCAT, "Servicio Restaurante IPC enlazado")
            enviarRegistroCliente()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            enlazado = false
            messengerServicio = null
            callbacks.onServicioDesconectado()
        }
    }

    fun conectar() {
        if (enlazado || messengerServicio != null) return
        liberado = false
        try {
            Log.i(TAG_LOGCAT, "Conexion solicitada")
            applicationContext.startService(intentServicio)
            applicationContext.bindService(intentServicio, conexion, Context.BIND_AUTO_CREATE)
        } catch (exception: RuntimeException) {
            Log.e(TAG_LOGCAT, "No se pudo conectar con la cocina", exception)
            liberar(detenerServicio = true)
            callbacks.onError("No se pudo conectar con la cocina", true, 0)
        }
    }

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
            Log.i(TAG_LOGCAT, "Orden enviada $idOrden: $orden")
        } catch (exception: RemoteException) {
            Log.e(TAG_LOGCAT, "No se pudo enviar orden a la cocina", exception)
            liberar(detenerServicio = true)
            callbacks.onError("No se pudo enviar la orden a la cocina", true, 0)
        }
    }

    fun desconectar() {
        Log.i(TAG_LOGCAT, "Desconexion solicitada")
        enviarDesconexionSiDisponible()
        liberar(detenerServicio = true)
    }

    fun reiniciar(onReconectar: () -> Unit) {
        Log.i(TAG_LOGCAT, "Reinicio solicitado")
        desconectar()
        onReconectar()
    }

    fun liberar() {
        liberar(detenerServicio = true)
    }

    private fun enviarRegistroCliente() {
        enviarMensaje(MSG_REGISTRAR_CLIENTE) { replyTo = messengerCliente }
    }

    private fun enviarDesconexionSiDisponible() {
        if (messengerServicio == null) return
        enviarMensaje(MSG_DESCONECTAR_CLIENTE) { replyTo = messengerCliente }
    }

    private fun enviarMensaje(tipo: Int, configurar: Message.() -> Unit = {}) {
        val destino = messengerServicio ?: return
        try {
            destino.send(Message.obtain(null, tipo).apply(configurar))
        } catch (exception: RemoteException) {
            Log.e(TAG_LOGCAT, "No se pudo enviar mensaje al servicio", exception)
            callbacks.onError("No se pudo comunicar con la cocina", true, 0)
        }
    }

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

    private class ManejadorRespuesta(
        controlador: ControladorRestauranteIpc
    ) : Handler(Looper.getMainLooper()) {
        private val referenciaControlador = WeakReference(controlador)

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
                else -> super.handleMessage(msg)
            }
        }

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
