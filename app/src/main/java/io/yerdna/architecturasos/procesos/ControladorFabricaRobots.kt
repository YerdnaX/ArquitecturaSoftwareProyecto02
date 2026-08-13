package io.yerdna.architecturasos.procesos

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

class ControladorFabricaRobots(
    private val applicationContext: Context,
    private val callbacks: Callbacks
) {
    interface Callbacks {
        fun onPidFabrica(pid: Int, mensajes: Int)
        fun onEstadoFabrica(estado: EstadoEjecucionFabrica, mensajes: Int)
        fun onRobotEnsamblado(robots: Int, mensajes: Int)
        fun onError(mensaje: String, mensajes: Int)
        fun onEventoRegistro(evento: EventoExperimento)
    }

    private val intentServicio = Intent(applicationContext, ServicioFabricaRobots::class.java)
    private val handlerRespuesta = ManejadorRespuesta(this)
    private val messengerCliente = Messenger(handlerRespuesta)
    private var messengerServicio: Messenger? = null
    private var enlazado = false
    private var liberado = false
    private var cantidadPendiente: Int? = null

    private val conexion = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            enlazado = true
            messengerServicio = Messenger(service)
            enviarRegistroCliente()
            cantidadPendiente?.let { cantidad ->
                enviarInicio(cantidad)
                cantidadPendiente = null
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            enlazado = false
            messengerServicio = null
        }
    }

    fun iniciar(cantidadRobots: Int) {
        liberado = false
        cantidadPendiente = cantidadRobots
        try {
            applicationContext.startService(intentServicio)
            applicationContext.bindService(intentServicio, conexion, Context.BIND_AUTO_CREATE)
        } catch (_: RuntimeException) {
            registrarEvento(TipoEvento.Error, "No se pudo iniciar la fabrica secundaria")
            liberar(detenerServicio = true)
            callbacks.onError("No se pudo iniciar la fabrica secundaria", 0)
        }
    }

    fun detener() {
        enviarMensaje(MSG_DETENER_FABRICA)
        liberar(detenerServicio = true)
    }

    fun liberar() {
        liberar(detenerServicio = true)
    }

    private fun liberar(detenerServicio: Boolean) {
        if (liberado) return
        liberado = true
        cantidadPendiente = null

        if (enlazado) {
            try {
                applicationContext.unbindService(conexion)
            } catch (_: IllegalArgumentException) {
                // La limpieza puede llamarse desde varios caminos de salida.
            }
        }

        enlazado = false
        messengerServicio = null
        handlerRespuesta.removeCallbacksAndMessages(null)

        if (detenerServicio) {
            applicationContext.stopService(intentServicio)
        }
    }

    private fun enviarRegistroCliente() {
        enviarMensaje(MSG_REGISTRAR_CLIENTE) { replyTo = messengerCliente }
    }

    private fun enviarInicio(cantidadRobots: Int) {
        enviarMensaje(MSG_INICIAR_FABRICA) {
            data = Bundle().apply {
                putInt(KEY_ROBOTS_CONFIGURADOS, cantidadRobots)
            }
        }
    }

    private fun enviarMensaje(tipo: Int, configurar: Message.() -> Unit = {}) {
        val destino = messengerServicio ?: return
        try {
            destino.send(Message.obtain(null, tipo).apply(configurar))
        } catch (exception: RemoteException) {
            registrarEvento(TipoEvento.Error, "No se pudo enviar mensaje al servicio")
            liberar(detenerServicio = true)
            callbacks.onError("No se pudo comunicar con la fabrica secundaria", 0)
        }
    }

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
        controlador: ControladorFabricaRobots
    ) : Handler(Looper.getMainLooper()) {
        private val referenciaControlador = WeakReference(controlador)

        override fun handleMessage(msg: Message) {
            val controlador = referenciaControlador.get() ?: return
            val mensajes = msg.data.getInt(KEY_MENSAJES_INTERCAMBIADOS, 0)
            when (msg.what) {
                MSG_PID_FABRICA -> controlador.callbacks.onPidFabrica(msg.data.getInt(KEY_PID), mensajes)
                MSG_ESTADO_FABRICA -> {
                    val estado = controlador.obtenerEstado(msg.data.getString(KEY_ESTADO))
                    controlador.callbacks.onEstadoFabrica(estado, mensajes)
                    if (estado == EstadoEjecucionFabrica.Inactivo) {
                        controlador.liberar(detenerServicio = false)
                    }
                }
                MSG_ROBOT_ENSAMBLADO -> controlador.callbacks.onRobotEnsamblado(
                    msg.data.getInt(KEY_ROBOTS_ENSAMBLADOS),
                    mensajes
                )
                MSG_ERROR_FABRICA -> {
                    controlador.callbacks.onError(
                        msg.data.getString(KEY_MENSAJE_ERROR).orEmpty(),
                        mensajes
                    )
                    controlador.liberar(detenerServicio = true)
                }
                MSG_EVENTO_REGISTRO -> controlador.callbacks.onEventoRegistro(msg.aEventoRegistro())
                else -> super.handleMessage(msg)
            }
        }

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

        private fun obtenerOrigen(valor: String?): OrigenEvento {
            return runCatching {
                OrigenEvento.valueOf(valor.orEmpty())
            }.getOrDefault(OrigenEvento.Servicio)
        }
    }

    private fun obtenerEstado(valor: String?): EstadoEjecucionFabrica {
        return EstadoEjecucionFabrica.entries.firstOrNull { it.name == valor }
            ?: EstadoEjecucionFabrica.Error
    }
}
