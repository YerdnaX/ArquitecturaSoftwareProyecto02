package io.yerdna.architecturasos.restaurante

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import android.os.RemoteException
import android.util.Log
import io.yerdna.architecturasos.restaurante.ContratoRestauranteIpc.DEMORA_PREPARACION_MS
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
import java.util.ArrayDeque

class ServicioRestauranteIpc : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val messenger = Messenger(ManejadorMensajes(this))
    private val colaOrdenes = ArrayDeque<OrdenRestaurante>()
    private var cliente: Messenger? = null
    private var ordenActual: OrdenRestaurante? = null
    private var totalProcesadas = 0
    private var mensajesIntercambiados = 0

    private val tareaPreparacion = Runnable {
        val orden = ordenActual ?: return@Runnable
        val respuesta = "Orden lista: ${orden.texto}"
        totalProcesadas += 1
        Log.i(TAG_LOGCAT, "Respuesta enviada para orden ${orden.id}: $respuesta")
        enviarMensaje(
            tipo = MSG_RESPUESTA_ENVIADA,
            orden = orden,
            datosExtra = Bundle().apply {
                putString(KEY_RESPUESTA, respuesta)
            }
        )
        ordenActual = null
        procesarSiguienteOrden()
    }

    override fun onBind(intent: Intent?): IBinder {
        return messenger.binder
    }

    override fun onDestroy() {
        limpiarTrabajo("Servicio destruido")
        super.onDestroy()
    }

    private class ManejadorMensajes(
        servicio: ServicioRestauranteIpc
    ) : Handler(Looper.getMainLooper()) {
        private val referenciaServicio = WeakReference(servicio)

        override fun handleMessage(msg: Message) {
            val servicio = referenciaServicio.get() ?: return
            when (msg.what) {
                MSG_REGISTRAR_CLIENTE -> servicio.registrarCliente(msg.replyTo)
                MSG_ENVIAR_ORDEN -> servicio.recibirOrden(msg)
                MSG_DESCONECTAR_CLIENTE -> servicio.desconectarCliente()
                else -> super.handleMessage(msg)
            }
        }
    }

    private fun registrarCliente(messengerCliente: Messenger?) {
        cliente = messengerCliente
        mensajesIntercambiados += 1
        val pidCocina = Process.myPid()
        Log.i(TAG_LOGCAT, "Cocina conectada con PID $pidCocina")
        enviarMensaje(
            tipo = MSG_COCINA_CONECTADA,
            datosExtra = Bundle().apply {
                putInt(KEY_PID_DESTINO, pidCocina)
                putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            }
        )
    }

    private fun recibirOrden(msg: Message) {
        val ordenTexto = msg.data.getString(KEY_ORDEN).orEmpty().trim()
        if (ordenTexto.isBlank()) {
            enviarError("La orden recibida esta vacia")
            return
        }

        val orden = OrdenRestaurante(
            id = msg.data.getInt(KEY_ID_ORDEN),
            texto = ordenTexto,
            pidOrigen = msg.data.getInt(KEY_PID_ORIGEN),
            timestamp = msg.data.getLong(KEY_TIMESTAMP, System.currentTimeMillis())
        )

        Log.i(TAG_LOGCAT, "Orden recibida ${orden.id}: ${orden.texto}")
        if (ordenActual == null) {
            ordenActual = orden
            enviarMensaje(MSG_ORDEN_RECIBIDA, orden)
            procesarOrdenActual()
        } else {
            colaOrdenes.addLast(orden)
            Log.i(TAG_LOGCAT, "Orden en cola ${orden.id}. Pendientes: ${colaOrdenes.size}")
            enviarMensaje(MSG_ORDEN_EN_COLA, orden)
        }
    }

    private fun procesarOrdenActual() {
        val orden = ordenActual ?: return
        Log.i(TAG_LOGCAT, "Orden procesando ${orden.id}")
        enviarMensaje(MSG_ORDEN_PROCESANDO, orden)
        handler.removeCallbacks(tareaPreparacion)
        handler.postDelayed(tareaPreparacion, DEMORA_PREPARACION_MS)
    }

    private fun procesarSiguienteOrden() {
        val siguiente = if (colaOrdenes.isEmpty()) {
            null
        } else {
            colaOrdenes.removeFirst()
        }
        if (siguiente == null) {
            stopSelf()
            return
        }

        ordenActual = siguiente
        enviarMensaje(MSG_ORDEN_RECIBIDA, siguiente)
        procesarOrdenActual()
    }

    private fun desconectarCliente() {
        Log.i(TAG_LOGCAT, "Desconexion solicitada por el mesero")
        limpiarTrabajo("Desconexion: orden actual cancelada y cola vaciada")
        stopSelf()
    }

    private fun limpiarTrabajo(motivo: String) {
        handler.removeCallbacks(tareaPreparacion)
        if (ordenActual != null) {
            Log.i(TAG_LOGCAT, "Orden actual cancelada")
        }
        if (colaOrdenes.isNotEmpty()) {
            Log.i(TAG_LOGCAT, "Cola vaciada: ${colaOrdenes.size} ordenes pendientes")
        }
        ordenActual = null
        colaOrdenes.clear()
        cliente = null
        Log.i(TAG_LOGCAT, motivo)
    }

    private fun enviarError(mensaje: String) {
        Log.e(TAG_LOGCAT, mensaje)
        enviarMensaje(
            tipo = MSG_ERROR_IPC,
            datosExtra = Bundle().apply {
                putString(KEY_MENSAJE_ERROR, mensaje)
                putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            }
        )
    }

    private fun enviarMensaje(
        tipo: Int,
        orden: OrdenRestaurante? = null,
        datosExtra: Bundle = Bundle()
    ) {
        val destino = cliente ?: return
        mensajesIntercambiados += 1
        val datos = Bundle(datosExtra).apply {
            orden?.let {
                putInt(KEY_ID_ORDEN, it.id)
                putString(KEY_ORDEN, it.texto)
                putInt(KEY_PID_ORIGEN, it.pidOrigen)
                putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            }
            putInt(KEY_PID_DESTINO, Process.myPid())
            putInt(KEY_ORDENES_EN_COLA, colaOrdenes.size)
            putInt(KEY_TOTAL_PROCESADAS, totalProcesadas)
            putInt(KEY_MENSAJES_INTERCAMBIADOS, mensajesIntercambiados)
        }

        try {
            destino.send(Message.obtain(null, tipo).apply { data = datos })
        } catch (exception: RemoteException) {
            Log.e(TAG_LOGCAT, "No se pudo enviar mensaje al mesero", exception)
            limpiarTrabajo("Error enviando respuesta al mesero")
            stopSelf()
        }
    }
}
