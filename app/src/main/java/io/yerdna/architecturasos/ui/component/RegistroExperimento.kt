package io.yerdna.architecturasos.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import io.yerdna.architecturasos.R
import io.yerdna.architecturasos.util.EventoExperimento
import io.yerdna.architecturasos.util.ExperimentoLogger

class EstadoRegistroExperimento internal constructor(
    val logger: ExperimentoLogger,
    val eventos: List<EventoExperimento>,
    val visible: Boolean,
    private val onAbrir: () -> Unit,
    private val onCerrar: () -> Unit,
    private val onLimpiarInterno: () -> Unit
) {
    fun abrir() {
        onAbrir()
    }

    fun cerrar() {
        onCerrar()
    }

    fun limpiar() {
        onLimpiarInterno()
    }
}

@Composable
fun rememberRegistroExperimento(tag: String): EstadoRegistroExperimento {
    var eventos by remember { mutableStateOf(emptyList<EventoExperimento>()) }
    var visible by remember { mutableStateOf(false) }
    val logger = remember(tag) {
        ExperimentoLogger(
            tag = tag,
            onCambio = { eventos = it }
        )
    }

    return EstadoRegistroExperimento(
        logger = logger,
        eventos = eventos,
        visible = visible,
        onAbrir = { visible = true },
        onCerrar = { visible = false },
        onLimpiarInterno = { logger.limpiar() }
    )
}

@Composable
fun BotonRegistroEventos(
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.List,
            contentDescription = stringResource(R.string.registro_eventos_accion_monitoreo)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaRegistroEventos(
    visible: Boolean,
    eventos: List<EventoExperimento>,
    onCerrar: () -> Unit
) {
    if (!visible) return

    ModalBottomSheet(
        onDismissRequest = onCerrar
    ) {
        PanelRegistroEventos(eventos = eventos)
    }
}
