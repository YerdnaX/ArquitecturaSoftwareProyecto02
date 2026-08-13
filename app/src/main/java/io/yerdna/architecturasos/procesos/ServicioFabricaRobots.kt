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
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.KEY_EVENTO_MENSAJE
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.KEY_EVENTO_ORIGEN
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.KEY_EVENTO_TIMESTAMP
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.KEY_EVENTO_TIPO
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.KEY_ESTADO
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.KEY_MENSAJES_INTERCAMBIADOS
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.KEY_MENSAJE_ERROR
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.KEY_PID
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.KEY_ROBOTS_CONFIGURADOS
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.KEY_ROBOTS_ENSAMBLADOS
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.MSG_DETENER_FABRICA
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.MSG_ERROR_FABRICA
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.MSG_ESTADO_FABRICA
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.MSG_EVENTO_REGISTRO
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.MSG_INICIAR_FABRICA
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.MSG_PID_FABRICA
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.MSG_REGISTRAR_CLIENTE
import io.yerdna.architecturasos.procesos.ContratoFabricaRobots.MSG_ROBOT_ENSAMBLADO
import io.yerdna.architecturasos.util.EventoExperimento
import io.yerdna.architecturasos.util.OrigenEvento
import io.yerdna.architecturasos.util.TipoEvento
import java.lang.ref.WeakReference

class ServicioFabricaRobots : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val messenger = Messenger(ManejadorMensajes(this))
    private var cliente: Messenger? = null
    private var robotsConfigurados = ROBOTS_POR_DEFECTO
    private var robotsEnsamblados = 0
    private var mensajesIntercambiados = 0
    private val eventosServicio = mutableListOf<EventoExperimento>()

    private val tareaEnsamblado = object : Runnable {
        // Ensambla un robot y reprograma la siguiente iteración hasta alcanzar la cantidad configurada.
        override fun run() {
            if (robotsEnsamblados >= robotsConfigurados) {
                finalizarCompletado()
                return
            }

            robotsEnsamblados += 1
            publicarEventoRegistro(TipoEvento.Informacion, "Robot ensamblado #$robotsEnsamblados")
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

    // Expone el messenger para que los clientes se enlacen al servicio.
    override fun onBind(intent: Intent?): IBinder {
        return messenger.binder
    }

    // Detiene las tareas pendientes y libera los recursos cuando el servicio se destruye.
    override fun onDestroy() {
        detenerTarea()
        publicarEventoRegistro(TipoEvento.Informacion, "Servicio destruido, recursos liberados")
        eventosServicio.clear()
        super.onDestroy()
    }

    private class ManejadorMensajes(
        servicio: ServicioFabricaRobots
    ) : Handler(Looper.getMainLooper()) {
        private val referenciaServicio = WeakReference(servicio)

        // Despacha los mensajes recibidos del cliente a la acción correspondiente del servicio.
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

    // Guarda la referencia del cliente registrado y le informa el PID de este proceso.
    private fun registrarCliente(messengerCliente: Messenger?) {
        cliente = messengerCliente
        mensajesIntercambiados += 1
        val pid = Process.myPid()
        publicarEventoRegistro(TipoEvento.Informacion, "PID secundario detectado: $pid")
        enviarMensaje(
            tipo = MSG_PID_FABRICA,
            datos = Bundle().apply {
                putInt(KEY_PID, pid)
            }
        )
    }

    // Inicia el ciclo de ensamblado de robots con la cantidad objetivo indicada.
    private fun iniciarFabrica(cantidadRobots: Int) {
        eventosServicio.clear()
        robotsConfigurados = cantidadRobots.coerceAtLeast(ROBOTS_MINIMOS)
        robotsEnsamblados = 0
        mensajesIntercambiados = 1

        publicarEventoRegistro(
            TipoEvento.Informacion,
            "Proceso ejecutandose con objetivo de $robotsConfigurados robots"
        )
        enviarEstado(EstadoEjecucionFabrica.Ejecucion)
        handler.removeCallbacks(tareaEnsamblado)
        handler.postDelayed(tareaEnsamblado, INTERVALO_ROBOT_MS)
    }

    // Detiene el ensamblado por solicitud del usuario y finaliza el servicio.
    private fun detenerPorUsuario() {
        mensajesIntercambiados += 1
        publicarEventoRegistro(TipoEvento.Advertencia, "Detencion solicitada")
        detenerTarea()
        enviarEstado(EstadoEjecucionFabrica.Inactivo)
        stopSelf()
    }

    // Detiene el ensamblado al alcanzar la cantidad objetivo y finaliza el servicio.
    private fun finalizarCompletado() {
        publicarEventoRegistro(
            TipoEvento.Informacion,
            "Finalizacion: $robotsEnsamblados robots ensamblados"
        )
        detenerTarea()
        enviarEstado(EstadoEjecucionFabrica.Inactivo)
        stopSelf()
    }

    // Cancela la tarea de ensamblado que estuviera programada.
    private fun detenerTarea() {
        handler.removeCallbacks(tareaEnsamblado)
    }

    // Envía al cliente el estado actual de ejecución de la fábrica.
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

    // Registra y envía al cliente un mensaje de error ocurrido en el servicio.
    private fun enviarError(mensaje: String) {
        publicarEventoRegistro(TipoEvento.Error, mensaje)
        enviarMensaje(
            tipo = MSG_ERROR_FABRICA,
            datos = Bundle().apply {
                putString(KEY_MENSAJE_ERROR, mensaje)
            }
        )
    }

    // Envía un mensaje al cliente registrado, contando los mensajes intercambiados.
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

    // Registra un evento local y lo envía al cliente como mensaje de registro.
    private fun publicarEventoRegistro(
        tipo: TipoEvento,
        mensaje: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        eventosServicio.add(EventoExperimento(timestamp, tipo, mensaje, OrigenEvento.Servicio))
        enviarMensaje(
            tipo = MSG_EVENTO_REGISTRO,
            datos = Bundle().apply {
                putString(KEY_EVENTO_TIPO, tipo.name)
                putString(KEY_EVENTO_MENSAJE, mensaje)
                putLong(KEY_EVENTO_TIMESTAMP, timestamp)
                putString(KEY_EVENTO_ORIGEN, OrigenEvento.Servicio.name)
            }
        )
    }

    private companion object {
        const val TAG = "OSPlayground/FabricaRobots"
        const val INTERVALO_ROBOT_MS = 1_000L
    }
}
