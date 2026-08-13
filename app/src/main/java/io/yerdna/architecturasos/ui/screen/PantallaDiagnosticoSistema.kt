package io.yerdna.architecturasos.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.yerdna.architecturasos.R
import io.yerdna.architecturasos.diagnostico.CategoriaDiagnostico
import io.yerdna.architecturasos.diagnostico.HerramientaDiagnostico
import io.yerdna.architecturasos.diagnostico.obtenerHerramientasDiagnostico
import io.yerdna.architecturasos.ui.component.ExperimentoScaffold
import io.yerdna.architecturasos.ui.component.ListaComandosVerificacion
import io.yerdna.architecturasos.ui.theme.ArchitecturasOSTheme

private sealed class EstadoDiagnostico {
    data class Listo(val herramientas: List<HerramientaDiagnostico>) : EstadoDiagnostico()
    data class Error(val mensaje: String?) : EstadoDiagnostico()
}

private val TAGS_LOGCAT_DOCUMENTADOS = listOf(
    "OSPlayground/FabricaRobots",
    "OSPlayground/BinderIPC",
    "OSPlayground/SocketLab",
    "OSPlayground/ThreadRace",
    "OSPlayground/ChaosBank",
    "OSPlayground/Mutex",
    "OSPlayground/Semaphore",
    "OSPlayground/Memory"
)

@Composable
fun PantallaDiagnosticoSistema(
    onVolver: () -> Unit
) {
    val estado = remember {
        try {
            EstadoDiagnostico.Listo(obtenerHerramientasDiagnostico())
        } catch (error: RuntimeException) {
            EstadoDiagnostico.Error(error.message)
        }
    }

    ExperimentoScaffold(
        titulo = stringResource(R.string.experimento_diagnostico_sistema_nombre),
        onVolver = onVolver
    ) { innerPadding ->
        when (estado) {
            is EstadoDiagnostico.Listo -> ContenidoDiagnosticoListo(
                innerPadding = innerPadding,
                herramientas = estado.herramientas
            )

            is EstadoDiagnostico.Error -> ContenidoDiagnosticoError(
                innerPadding = innerPadding,
                mensaje = estado.mensaje
            )
        }
    }
}

@Composable
private fun ContenidoDiagnosticoListo(
    innerPadding: PaddingValues,
    herramientas: List<HerramientaDiagnostico>
) {
    val totalComandos = herramientas.sumOf { it.comandos.size }
    val experimentosCubiertos = herramientas
        .flatMap { it.experimentosRelacionados }
        .map { it.nombreExperimentoResId }
        .distinct()
        .size

    LazyColumn(
        modifier = Modifier.padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.experimento_diagnostico_sistema_concepto),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        item {
            Text(
                text = stringResource(R.string.diagnostico_explicacion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            PanelMetricasDiagnostico(
                totalHerramientas = herramientas.size,
                totalComandos = totalComandos,
                experimentosCubiertos = experimentosCubiertos
            )
        }
        item {
            Text(
                text = stringResource(R.string.diagnostico_herramientas_titulo),
                style = MaterialTheme.typography.titleMedium
            )
        }
        items(herramientas) { herramienta ->
            TarjetaHerramientaDiagnostico(herramienta = herramienta)
        }
        item {
            PanelComparacionAndroidLinux()
        }
        item {
            PanelDemostracionEnVivo()
        }
    }
}

@Composable
private fun ContenidoDiagnosticoError(
    innerPadding: PaddingValues,
    mensaje: String?
) {
    Column(
        modifier = Modifier
            .padding(innerPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.diagnostico_error_titulo),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = mensaje ?: stringResource(R.string.valor_no_disponible),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PanelMetricasDiagnostico(
    totalHerramientas: Int,
    totalComandos: Int,
    experimentosCubiertos: Int
) {
    SeccionCarrera(titulo = stringResource(R.string.diagnostico_metricas_titulo)) {
        DatoCarrera(
            etiqueta = stringResource(R.string.diagnostico_metrica_herramientas),
            valor = totalHerramientas.toString()
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.diagnostico_metrica_comandos),
            valor = totalComandos.toString()
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.diagnostico_metrica_experimentos_cubiertos),
            valor = experimentosCubiertos.toString()
        )
        Text(
            text = stringResource(R.string.diagnostico_metrica_tags_logcat),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = TAGS_LOGCAT_DOCUMENTADOS.joinToString(", "),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun TarjetaHerramientaDiagnostico(
    herramienta: HerramientaDiagnostico
) {
    SeccionCarrera(titulo = stringResource(herramienta.nombreResId)) {
        DatoCarrera(
            etiqueta = stringResource(R.string.diagnostico_etiqueta_categoria),
            valor = textoCategoria(herramienta.categoria)
        )
        Text(
            text = stringResource(herramienta.descripcionResId),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = stringResource(
                R.string.diagnostico_formato_cuando_usarla,
                stringResource(herramienta.cuandoUsarlaResId)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(
                R.string.diagnostico_formato_que_valida,
                stringResource(herramienta.queValidaResId)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(
                R.string.diagnostico_formato_limitacion,
                stringResource(herramienta.limitacionResId)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        if (herramienta.comandos.isNotEmpty()) {
            ListaComandosVerificacion(comandos = herramienta.comandos)
        }
        if (herramienta.experimentosRelacionados.isNotEmpty()) {
            Text(
                text = stringResource(R.string.diagnostico_etiqueta_experimentos_relacionados),
                style = MaterialTheme.typography.labelLarge
            )
            herramienta.experimentosRelacionados.forEach { relacion ->
                DatoCarrera(
                    etiqueta = stringResource(relacion.nombreExperimentoResId),
                    valor = stringResource(relacion.mecanismoObservableResId)
                )
            }
        }
    }
}

@Composable
private fun PanelComparacionAndroidLinux() {
    SeccionCarrera(titulo = stringResource(R.string.diagnostico_comparacion_titulo)) {
        FilaComparacion(
            tituloResId = R.string.diagnostico_comparacion_procesos_titulo,
            androidResId = R.string.diagnostico_comparacion_procesos_android,
            linuxResId = R.string.diagnostico_comparacion_procesos_linux
        )
        FilaComparacion(
            tituloResId = R.string.diagnostico_comparacion_ipc_titulo,
            androidResId = R.string.diagnostico_comparacion_ipc_android,
            linuxResId = R.string.diagnostico_comparacion_ipc_linux
        )
        FilaComparacion(
            tituloResId = R.string.diagnostico_comparacion_diagnostico_titulo,
            androidResId = R.string.diagnostico_comparacion_diagnostico_android,
            linuxResId = R.string.diagnostico_comparacion_diagnostico_linux
        )
    }
}

@Composable
private fun FilaComparacion(
    tituloResId: Int,
    androidResId: Int,
    linuxResId: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(tituloResId),
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = stringResource(R.string.diagnostico_formato_comparacion_android, stringResource(androidResId)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.diagnostico_formato_comparacion_linux, stringResource(linuxResId)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PanelDemostracionEnVivo() {
    SeccionCarrera(titulo = stringResource(R.string.diagnostico_demo_titulo)) {
        Text(
            text = stringResource(R.string.diagnostico_demo_paso1),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = stringResource(R.string.diagnostico_demo_paso2),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = stringResource(R.string.diagnostico_demo_paso3),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = stringResource(R.string.diagnostico_demo_paso4),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = stringResource(R.string.diagnostico_demo_paso5),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun textoCategoria(categoria: CategoriaDiagnostico): String {
    return when (categoria) {
        CategoriaDiagnostico.PROCESOS_CPU -> stringResource(R.string.diagnostico_categoria_procesos_cpu)
        CategoriaDiagnostico.MEMORIA -> stringResource(R.string.diagnostico_categoria_memoria)
        CategoriaDiagnostico.LOGS -> stringResource(R.string.diagnostico_categoria_logs)
        CategoriaDiagnostico.IPC -> stringResource(R.string.diagnostico_categoria_ipc)
        CategoriaDiagnostico.SOCKETS -> stringResource(R.string.diagnostico_categoria_sockets)
        CategoriaDiagnostico.SERVICIOS -> stringResource(R.string.diagnostico_categoria_servicios)
        CategoriaDiagnostico.TRACING -> stringResource(R.string.diagnostico_categoria_tracing)
        CategoriaDiagnostico.REPORTE_SISTEMA -> stringResource(R.string.diagnostico_categoria_reporte_sistema)
        CategoriaDiagnostico.ENERGIA -> stringResource(R.string.diagnostico_categoria_energia)
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaDiagnosticoSistemaPreview() {
    ArchitecturasOSTheme {
        PantallaDiagnosticoSistema(onVolver = {})
    }
}
