package io.yerdna.architecturasos.redagentes

import android.os.Handler
import android.os.Looper
import io.yerdna.architecturasos.util.EventoExperimento
import io.yerdna.architecturasos.util.OrigenEvento
import io.yerdna.architecturasos.util.TipoEvento
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ControladorRedAgentes(
    private val callbacks: Callbacks
) {
    interface Callbacks {
        fun onEstadoServidor(estado: EstadoServidorRedAgentes)
        fun onServidorEscuchando(puerto: Int)
        fun onEstadoEnvio(estado: EstadoEnvioRedAgentes)
        fun onMensajeProcesadoServidor(evento: EventoServidorRedAgentes)
        fun onEnvioExitoso(resultado: ResultadoEnvioRedAgentes)
        fun onError(codigo: CodigoErrorRedAgentes, throwable: Throwable? = null)
        fun onEventoRegistro(evento: EventoExperimento)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var executorServidor: ExecutorService? = null
    private var executorCliente: ExecutorService? = null
    private var servidor: ServidorRedAgentes? = null
    private val cliente = ClienteRedAgentes()

    @Volatile
    private var liberado = true

    fun iniciarServidor() {
        if (executorServidor?.isShutdown == false) return

        liberado = false
        val servidorActual = ServidorRedAgentes(callbacksServidor())
        servidor = servidorActual
        executorServidor = Executors.newSingleThreadExecutor()
        registrar(TipoEvento.Informacion, "Inicio de servidor solicitado")
        executorServidor?.execute {
            try {
                servidorActual.ejecutar()
            } finally {
                executorServidor?.shutdownNow()
                executorServidor = null
            }
        }
    }

    fun enviarMensaje(mensaje: String, puerto: Int) {
        if (executorCliente?.isShutdown == false) return

        liberado = false
        executorCliente = Executors.newSingleThreadExecutor()
        publicar { callbacks.onEstadoEnvio(EstadoEnvioRedAgentes.Conectando) }
        registrar(TipoEvento.Informacion, "Cliente conectando a $HOST_RED_AGENTES:$puerto")

        executorCliente?.execute {
            try {
                publicar { callbacks.onEstadoEnvio(EstadoEnvioRedAgentes.Enviando) }
                registrar(TipoEvento.Informacion, "Mensaje enviado: $mensaje")
                publicar { callbacks.onEstadoEnvio(EstadoEnvioRedAgentes.EsperandoRespuesta) }
                val resultado = cliente.enviarMensaje(HOST_RED_AGENTES, puerto, mensaje)
                registrar(TipoEvento.Informacion, "Respuesta recibida: ${resultado.respuesta}")
                publicar { callbacks.onEnvioExitoso(resultado) }
            } catch (exception: ErrorRedAgentesException) {
                registrar(TipoEvento.Error, "Error de envio: ${exception.codigo}")
                publicar { callbacks.onError(exception.codigo, exception.cause ?: exception) }
            } finally {
                executorCliente?.shutdownNow()
                executorCliente = null
            }
        }
    }

    fun detener() {
        publicar { callbacks.onEstadoServidor(EstadoServidorRedAgentes.Deteniendo) }
        registrar(TipoEvento.Advertencia, "Detencion solicitada")
        servidor?.detener()
        executorCliente?.shutdownNow()
        executorCliente = null
        executorServidor?.shutdownNow()
        executorServidor = null
    }

    fun limpiar() {
        detener()
        liberado = true
    }

    private fun callbacksServidor(): ServidorRedAgentes.Callbacks {
        return object : ServidorRedAgentes.Callbacks {
            override fun onIniciando() {
                publicar { callbacks.onEstadoServidor(EstadoServidorRedAgentes.Iniciando) }
            }

            override fun onEscuchando(puerto: Int) {
                registrar(TipoEvento.Informacion, "Servidor escuchando en $HOST_RED_AGENTES:$puerto")
                publicar { callbacks.onServidorEscuchando(puerto) }
            }

            override fun onClienteConectado() {
                registrar(TipoEvento.Informacion, "Cliente conectado")
                publicar { callbacks.onEstadoServidor(EstadoServidorRedAgentes.ClienteConectado) }
            }

            override fun onMensajeProcesado(evento: EventoServidorRedAgentes) {
                registrar(TipoEvento.Informacion, "Servidor recibio: ${evento.mensaje}", OrigenEvento.Servicio)
                registrar(TipoEvento.Informacion, "Servidor respondio: ${evento.respuesta}", OrigenEvento.Servicio)
                publicar { callbacks.onMensajeProcesadoServidor(evento) }
            }

            override fun onDetenido() {
                registrar(TipoEvento.Informacion, "Servidor detenido")
                publicar { callbacks.onEstadoServidor(EstadoServidorRedAgentes.Detenido) }
            }

            override fun onError(codigo: CodigoErrorRedAgentes, throwable: Throwable?) {
                registrar(TipoEvento.Error, "Error de servidor: $codigo", OrigenEvento.Servicio)
                publicar { callbacks.onError(codigo, throwable) }
            }
        }
    }

    private fun registrar(
        tipo: TipoEvento,
        mensaje: String,
        origen: OrigenEvento = OrigenEvento.ProcesoPrincipal
    ) {
        publicar {
            callbacks.onEventoRegistro(
                EventoExperimento(
                    timestamp = System.currentTimeMillis(),
                    tipo = tipo,
                    mensaje = mensaje,
                    origen = origen
                )
            )
        }
    }

    private fun publicar(accion: () -> Unit) {
        if (liberado) return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            accion()
        } else {
            mainHandler.post(accion)
        }
    }
}
