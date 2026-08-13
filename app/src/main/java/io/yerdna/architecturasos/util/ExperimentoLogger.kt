package io.yerdna.architecturasos.util

import android.os.Handler
import android.os.Looper
import android.util.Log

class ExperimentoLogger(
    private val tag: String,
    private val onCambio: (List<EventoExperimento>) -> Unit
) {
    private val lock = Any()
    private val eventos = mutableListOf<EventoExperimento>()
    private val mainHandler = Handler(Looper.getMainLooper())

    // Registra un evento de tipo informacion con el mensaje y origen dados
    fun info(
        mensaje: String,
        timestamp: Long = System.currentTimeMillis(),
        origen: OrigenEvento = OrigenEvento.ProcesoPrincipal
    ) {
        registrar(EventoExperimento(timestamp, TipoEvento.Informacion, mensaje, origen))
    }

    // Registra un evento de tipo advertencia con el mensaje y origen dados
    fun advertencia(
        mensaje: String,
        timestamp: Long = System.currentTimeMillis(),
        origen: OrigenEvento = OrigenEvento.ProcesoPrincipal
    ) {
        registrar(EventoExperimento(timestamp, TipoEvento.Advertencia, mensaje, origen))
    }

    // Registra un evento de tipo error con el mensaje, origen y excepcion opcional dados
    fun error(
        mensaje: String,
        throwable: Throwable? = null,
        timestamp: Long = System.currentTimeMillis(),
        origen: OrigenEvento = OrigenEvento.ProcesoPrincipal
    ) {
        registrar(EventoExperimento(timestamp, TipoEvento.Error, mensaje, origen), throwable)
    }

    // Agrega un evento ya construido al registro
    fun registrar(evento: EventoExperimento) {
        registrar(evento, throwable = null)
    }

    // Devuelve una copia inmutable de todos los eventos registrados hasta el momento
    fun captura(): List<EventoExperimento> {
        return synchronized(lock) {
            eventos.toList()
        }
    }

    // Vacia el registro de eventos y notifica el cambio a los observadores
    fun limpiar() {
        synchronized(lock) {
            eventos.clear()
        }
        notificarCambio()
    }

    // Agrega el evento al registro de forma segura para hilos, lo escribe en logcat y notifica el cambio
    private fun registrar(evento: EventoExperimento, throwable: Throwable?) {
        synchronized(lock) {
            eventos.add(evento)
        }
        escribirLogcat(evento, throwable)
        notificarCambio()
    }

    // Escribe el evento en logcat con el nivel y formato correspondientes segun su tipo
    private fun escribirLogcat(evento: EventoExperimento, throwable: Throwable?) {
        val origen = when (evento.origen) {
            OrigenEvento.ProcesoPrincipal -> "Principal"
            OrigenEvento.Servicio -> "Servicio"
        }
        when (evento.tipo) {
            TipoEvento.Informacion -> Log.i(tag, "[I][$origen] ${evento.mensaje}")
            TipoEvento.Advertencia -> Log.w(tag, "[W][$origen] ${evento.mensaje}")
            TipoEvento.Error -> {
                val mensaje = "[E][$origen] ${evento.mensaje}"
                if (throwable == null) {
                    Log.e(tag, mensaje)
                } else {
                    Log.e(tag, mensaje, throwable)
                }
            }
        }
    }

    // Notifica en el hilo principal el listado actualizado de eventos, directamente o encolado segun el hilo actual
    private fun notificarCambio() {
        val copia = captura()
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onCambio(copia)
        } else {
            mainHandler.post {
                onCambio(copia)
            }
        }
    }
}
