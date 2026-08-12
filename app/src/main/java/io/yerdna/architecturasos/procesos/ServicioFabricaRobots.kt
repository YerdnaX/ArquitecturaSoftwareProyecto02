package io.yerdna.architecturasos.procesos

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
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.KEY_ESTADO
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.KEY_MENSAJES_INTERCAMBIADOS
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.KEY_MENSAJE_ERROR
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.KEY_PID
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.KEY_ROBOTS_CONFIGURADOS
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.KEY_ROBOTS_ENSAMBLADOS
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.MSG_DETENER_FABRICA
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.MSG_ERROR_FABRICA
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.MSG_ESTADO_FABRICA
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.MSG_INICIAR_FABRICA
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.MSG_PID_FABRICA
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.MSG_REGISTRAR_CLIENTE
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.MSG_ROBOT_ENSAMBLADO
import java.lang.ref.WeakReference

class ServicioFabricaRobots : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val messenger = Messenger(ManejadorMensajes(this))
    private var cliente: Messenger? = null
    private var robotsConfigurados = ROBOTS_POR_DEFECTO
    private var robotsEnsamblados = 0
    private var mensajesIntercambiados = 0

    private val tareaEnsamblado = object : Runnable {
        override fun run() {
            if (robotsEnsamblados >= robotsConfigurados) {
                finalizarCompletado()
                return
            }

            robotsEnsamblados += 1
            Log.i(TAG, "Robot ensamblado #$robotsEnsamblados")
            enviarMensaje(
                tipo = MSG_ROBOT_ENSAMBLADO,
                datos = Bundle().apply {
                    putInt(KEY_ROBOTS_CONFIGURADOS, robotsConfigurados)
                    putInt(KEY_ROBOTS_ENSAMBLADOS, robotsEnsamblados)
                }
            )

            if (robotsEnsamblados >= robotsConfigurados) {
                finalizarCompletado()
            } else {
                handler.postDelayed(this, INTERVALO_ROBOT_MS)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return messenger.binder
    }

    override fun onDestroy() {
        detenerTarea()
        Log.i(TAG, "Servicio destruido, recursos liberados")
        super.onDestroy()
    }

    private class ManejadorMensajes(
        servicio: ServicioFabricaRobots
    ) : Handler(Looper.getMainLooper()) {
        private val referenciaServicio = WeakReference(servicio)

        override fun handleMessage(msg: Message) {
            val servicio = referenciaServicio.get() ?: return
            when (msg.what) {
                MSG_REGISTRAR_CLIENTE -> servicio.registrarCliente(msg.replyTo)
                MSG_INICIAR_FABRICA -> servicio.iniciarFabrica(
                    msg.data.getInt(KEY_ROBOTS_CONFIGURADOS, ROBOTS_POR_DEFECTO)
                )
                MSG_DETENER_FABRICA -> servicio.detenerPorUsuario()
                else -> super.handleMessage(msg)
            }
        }
    }

    private fun registrarCliente(messengerCliente: Messenger?) {
        cliente = messengerCliente
        mensajesIntercambiados += 1
        val pid = Process.myPid()
        Log.i(TAG, "PID secundario detectado: $pid")
        enviarMensaje(
            tipo = MSG_PID_FABRICA,
            datos = Bundle().apply {
                putInt(KEY_PID, pid)
            }
        )
    }

    private fun iniciarFabrica(cantidadRobots: Int) {
        robotsConfigurados = cantidadRobots.coerceAtLeast(ROBOTS_MINIMOS)
        robotsEnsamblados = 0
        mensajesIntercambiados = 1

        Log.i(TAG, "Proceso ejecutandose con objetivo de $robotsConfigurados robots")
        enviarEstado(EstadoEjecucionFabrica.Ejecucion)
        handler.removeCallbacks(tareaEnsamblado)
        handler.postDelayed(tareaEnsamblado, INTERVALO_ROBOT_MS)
    }

    private fun detenerPorUsuario() {
        mensajesIntercambiados += 1
        Log.i(TAG, "Detencion solicitada")
        detenerTarea()
        enviarEstado(EstadoEjecucionFabrica.Inactivo)
        stopSelf()
    }

    private fun finalizarCompletado() {
        Log.i(TAG, "Finalizacion: $robotsEnsamblados robots ensamblados")
        detenerTarea()
        enviarEstado(EstadoEjecucionFabrica.Inactivo)
        stopSelf()
    }

    private fun detenerTarea() {
        handler.removeCallbacks(tareaEnsamblado)
    }

    private fun enviarEstado(estado: EstadoEjecucionFabrica) {
        enviarMensaje(
            tipo = MSG_ESTADO_FABRICA,
            datos = Bundle().apply {
                putString(KEY_ESTADO, estado.name)
                putInt(KEY_ROBOTS_CONFIGURADOS, robotsConfigurados)
                putInt(KEY_ROBOTS_ENSAMBLADOS, robotsEnsamblados)
            }
        )
    }

    private fun enviarError(mensaje: String) {
        Log.e(TAG, mensaje)
        enviarMensaje(
            tipo = MSG_ERROR_FABRICA,
            datos = Bundle().apply {
                putString(KEY_MENSAJE_ERROR, mensaje)
            }
        )
    }

    private fun enviarMensaje(tipo: Int, datos: Bundle = Bundle()) {
        val destino = cliente ?: return
        mensajesIntercambiados += 1
        datos.putInt(KEY_MENSAJES_INTERCAMBIADOS, mensajesIntercambiados)

        try {
            destino.send(Message.obtain(null, tipo).apply { data = datos })
        } catch (exception: RemoteException) {
            Log.e(TAG, "No se pudo enviar mensaje a la oficina principal", exception)
            detenerTarea()
            stopSelf()
        }
    }

    private companion object {
        const val TAG = "OSPlayground/FabricaRobots"
        const val INTERVALO_ROBOT_MS = 1_000L
    }
}
