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
        // Notifica un cambio en el estado del servidor
        fun onEstadoServidor(estado: EstadoServidorRedAgentes)
        // Notifica que el servidor quedo escuchando en el puerto indicado
        fun onServidorEscuchando(puerto: Int)
        // Notifica un cambio en el estado del envio del cliente
        fun onEstadoEnvio(estado: EstadoEnvioRedAgentes)
        // Notifica que el servidor proceso un mensaje entrante
        fun onMensajeProcesadoServidor(evento: EventoServidorRedAgentes)
        // Notifica que el envio del cliente finalizo con exito
        fun onEnvioExitoso(resultado: ResultadoEnvioRedAgentes)
        // Notifica que ocurrio un error, con su codigo y causa opcional
        fun onError(codigo: CodigoErrorRedAgentes, throwable: Throwable? = null)
        // Notifica un nuevo evento de registro para mostrar en el log
        fun onEventoRegistro(evento: EventoExperimento)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var executorServidor: ExecutorService? = null
    private var executorCliente: ExecutorService? = null
    private var servidor: ServidorRedAgentes? = null
    private val cliente = ClienteRedAgentes()

    @Volatile
    private var liberado = true

    // Arranca el servidor en un hilo dedicado si no hay uno ya en ejecucion
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

    // Envia un mensaje al servidor en un hilo aparte, publicando cada cambio de estado del envio
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

    // Detiene el servidor y cancela cualquier envio o escucha en curso
    fun detener() {
        publicar { callbacks.onEstadoServidor(EstadoServidorRedAgentes.Deteniendo) }
        registrar(TipoEvento.Advertencia, "Detencion solicitada")
        servidor?.detener()
        executorCliente?.shutdownNow()
        executorCliente = null
        executorServidor?.shutdownNow()
        executorServidor = null
    }

    // Detiene todo y marca el controlador como liberado para dejar de publicar callbacks
    fun limpiar() {
        detener()
        liberado = true
    }

    // Construye el conjunto de callbacks que el servidor usara para reportar sus eventos hacia este controlador
    private fun callbacksServidor(): ServidorRedAgentes.Callbacks {
        return object : ServidorRedAgentes.Callbacks {
            // Propaga el evento de que el servidor esta iniciando
            override fun onIniciando() {
                publicar { callbacks.onEstadoServidor(EstadoServidorRedAgentes.Iniciando) }
            }

            // Registra y propaga que el servidor quedo escuchando en el puerto dado
            override fun onEscuchando(puerto: Int) {
                registrar(TipoEvento.Informacion, "Servidor escuchando en $HOST_RED_AGENTES:$puerto")
                publicar { callbacks.onServidorEscuchando(puerto) }
            }

            // Registra y propaga que un cliente se conecto al servidor
            override fun onClienteConectado() {
                registrar(TipoEvento.Informacion, "Cliente conectado")
                publicar { callbacks.onEstadoServidor(EstadoServidorRedAgentes.ClienteConectado) }
            }

            // Registra el mensaje recibido y la respuesta enviada, y propaga el evento procesado
            override fun onMensajeProcesado(evento: EventoServidorRedAgentes) {
                registrar(TipoEvento.Informacion, "Servidor recibio: ${evento.mensaje}", OrigenEvento.Servicio)
                registrar(TipoEvento.Informacion, "Servidor respondio: ${evento.respuesta}", OrigenEvento.Servicio)
                publicar { callbacks.onMensajeProcesadoServidor(evento) }
            }

            // Registra y propaga que el servidor se detuvo
            override fun onDetenido() {
                registrar(TipoEvento.Informacion, "Servidor detenido")
                publicar { callbacks.onEstadoServidor(EstadoServidorRedAgentes.Detenido) }
            }

            // Registra y propaga un error ocurrido en el servidor
            override fun onError(codigo: CodigoErrorRedAgentes, throwable: Throwable?) {
                registrar(TipoEvento.Error, "Error de servidor: $codigo", OrigenEvento.Servicio)
                publicar { callbacks.onError(codigo, throwable) }
            }
        }
    }

    // Crea un evento de registro con el tipo, mensaje y origen indicados y lo publica
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

    // Ejecuta la accion en el hilo principal si el controlador sigue activo, usando el handler si hace falta
    private fun publicar(accion: () -> Unit) {
        if (liberado) return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            accion()
        } else {
            mainHandler.post(accion)
        }
    }
}
