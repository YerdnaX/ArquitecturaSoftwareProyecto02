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

    fun info(
        mensaje: String,
        timestamp: Long = System.currentTimeMillis(),
        origen: OrigenEvento = OrigenEvento.ProcesoPrincipal
    ) {
        registrar(EventoExperimento(timestamp, TipoEvento.Informacion, mensaje, origen))
    }

    fun advertencia(
        mensaje: String,
        timestamp: Long = System.currentTimeMillis(),
        origen: OrigenEvento = OrigenEvento.ProcesoPrincipal
    ) {
        registrar(EventoExperimento(timestamp, TipoEvento.Advertencia, mensaje, origen))
    }

    fun error(
        mensaje: String,
        throwable: Throwable? = null,
        timestamp: Long = System.currentTimeMillis(),
        origen: OrigenEvento = OrigenEvento.ProcesoPrincipal
    ) {
        registrar(EventoExperimento(timestamp, TipoEvento.Error, mensaje, origen), throwable)
    }

    fun registrar(evento: EventoExperimento) {
        registrar(evento, throwable = null)
    }

    fun captura(): List<EventoExperimento> {
        return synchronized(lock) {
            eventos.toList()
        }
    }

    fun limpiar() {
        synchronized(lock) {
            eventos.clear()
        }
        notificarCambio()
    }

    private fun registrar(evento: EventoExperimento, throwable: Throwable?) {
        synchronized(lock) {
            eventos.add(evento)
        }
        escribirLogcat(evento, throwable)
        notificarCambio()
    }

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
