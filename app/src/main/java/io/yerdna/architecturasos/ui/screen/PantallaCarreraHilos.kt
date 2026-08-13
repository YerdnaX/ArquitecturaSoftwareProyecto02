package io.yerdna.architecturasos.ui.screen

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.yerdna.architecturasos.R
import io.yerdna.architecturasos.hilos.CarreraHilosViewModel
import io.yerdna.architecturasos.hilos.ConfiguracionCarreraHilos
import io.yerdna.architecturasos.hilos.EstadoCarreraHilos
import io.yerdna.architecturasos.hilos.EstadoHiloCarrera
import io.yerdna.architecturasos.hilos.EventoRegistroCarreraHilos
import io.yerdna.architecturasos.hilos.ProgresoHiloCarrera
import io.yerdna.architecturasos.hilos.ResultadoCarreraHilos
import io.yerdna.architecturasos.hilos.TAG_CARRERA_HILOS
import io.yerdna.architecturasos.ui.component.BotonRegistroEventos
import io.yerdna.architecturasos.ui.component.ExperimentoScaffold
import io.yerdna.architecturasos.ui.component.HojaRegistroEventos
import io.yerdna.architecturasos.ui.component.rememberRegistroExperimento
import io.yerdna.architecturasos.ui.theme.ArchitecturasOSTheme
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun PantallaCarreraHilos(
    onVolver: () -> Unit
) {
    val viewModel = remember { CarreraHilosViewModel() }
    val registro = rememberRegistroExperimento(TAG_CARRERA_HILOS)
    var mostrarConfirmacionSalida by remember { mutableStateOf(false) }
    val mensajeNoDisponible = stringResource(R.string.valor_no_disponible)
    val logEjecucionIniciada = stringResource(R.string.log_carrera_hilos_ejecucion_iniciada)
    val logCantidadHilos = stringResource(R.string.log_carrera_hilos_cantidad_hilos)
    val logCantidadTrabajo = stringResource(R.string.log_carrera_hilos_cantidad_trabajo)
    val logHiloIniciado = stringResource(R.string.log_carrera_hilos_hilo_iniciado)
    val logHiloFinalizado = stringResource(R.string.log_carrera_hilos_hilo_finalizado)
    val logHiloCancelado = stringResource(R.string.log_carrera_hilos_hilo_cancelado)
    val logEjecucionFinalizada = stringResource(R.string.log_carrera_hilos_ejecucion_finalizada)
    val logEjecucionCancelada = stringResource(R.string.log_carrera_hilos_ejecucion_cancelada)
    val logError = stringResource(R.string.log_carrera_hilos_error)
    val logSalidaConfirmada = stringResource(R.string.log_carrera_hilos_salida_confirmada)

    fun registrarEvento(evento: EventoRegistroCarreraHilos) {
        when (evento) {
            is EventoRegistroCarreraHilos.EjecucionIniciada -> registro.logger.info(
                logEjecucionIniciada.formatear(evento.idEjecucion)
            )

            is EventoRegistroCarreraHilos.CantidadHilos -> registro.logger.info(
                logCantidadHilos.formatear(evento.cantidad)
            )

            is EventoRegistroCarreraHilos.CantidadTrabajo -> registro.logger.info(
                logCantidadTrabajo.formatear(evento.cantidad)
            )

            is EventoRegistroCarreraHilos.HiloIniciado -> registro.logger.info(
                logHiloIniciado.formatear(evento.idHilo)
            )

            is EventoRegistroCarreraHilos.HiloFinalizado -> registro.logger.info(
                logHiloFinalizado.formatear(evento.idHilo)
            )

            is EventoRegistroCarreraHilos.HiloCancelado -> registro.logger.advertencia(
                logHiloCancelado.formatear(evento.idHilo)
            )

            is EventoRegistroCarreraHilos.EjecucionFinalizada -> registro.logger.info(
                logEjecucionFinalizada.formatear(evento.tiempoMs)
            )

            EventoRegistroCarreraHilos.EjecucionCancelada -> registro.logger.advertencia(
                logEjecucionCancelada
            )

            is EventoRegistroCarreraHilos.ErrorTecnico -> registro.logger.error(
                logError.formatear(
                    evento.idHilo,
                    evento.mensaje ?: mensajeNoDisponible
                )
            )
        }
    }

    fun solicitarSalida() {
        if (viewModel.hayEjecucionActiva()) {
            mostrarConfirmacionSalida = true
        } else {
            onVolver()
        }
    }

    BackHandler {
        solicitarSalida()
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.limpiarRecursos()
        }
    }

    if (mostrarConfirmacionSalida) {
        DialogoSalidaCarreraHilos(
            onConfirmar = {
                mostrarConfirmacionSalida = false
                registro.logger.advertencia(
                    logSalidaConfirmada
                )
                viewModel.cancelar()
                viewModel.limpiarRecursos()
                onVolver()
            },
            onCancelar = { mostrarConfirmacionSalida = false }
        )
    }

    ContenidoCarreraHilos(
        estado = viewModel.estado,
        configuracion = viewModel.configuracion,
        progresos = viewModel.progresos,
        tiempoTotalMs = viewModel.tiempoTotalMs,
        resultadoUltimaEjecucion = viewModel.resultadoUltimaEjecucion,
        historial = viewModel.historial,
        mejorTiempoMismaCantidad = viewModel.mejorTiempoMismaCantidad(),
        puedeIniciar = viewModel.puedeIniciar(),
        puedeCancelar = viewModel.puedeCancelar(),
        puedeReiniciar = viewModel.puedeReiniciar(),
        onCantidadHilos = viewModel::actualizarCantidadHilos,
        onCantidadTrabajo = viewModel::actualizarCantidadTrabajo,
        onIniciar = {
            registro.limpiar()
            viewModel.iniciar(::registrarEvento)
        },
        onCancelar = viewModel::cancelar,
        onReiniciar = viewModel::reiniciar,
        onVolver = ::solicitarSalida,
        acciones = {
            BotonRegistroEventos(onClick = registro::abrir)
        }
    )

    HojaRegistroEventos(
        visible = registro.visible,
        eventos = registro.eventos,
        onCerrar = registro::cerrar
    )
}

@Composable
private fun ContenidoCarreraHilos(
    estado: EstadoCarreraHilos,
    configuracion: ConfiguracionCarreraHilos,
    progresos: List<ProgresoHiloCarrera>,
    tiempoTotalMs: Long,
    resultadoUltimaEjecucion: ResultadoCarreraHilos?,
    historial: List<ResultadoCarreraHilos>,
    mejorTiempoMismaCantidad: Long?,
    puedeIniciar: Boolean,
    puedeCancelar: Boolean,
    puedeReiniciar: Boolean,
    onCantidadHilos: (Int) -> Unit,
    onCantidadTrabajo: (Int) -> Unit,
    onIniciar: () -> Unit,
    onCancelar: () -> Unit,
    onReiniciar: () -> Unit,
    onVolver: () -> Unit,
    acciones: @Composable RowScope.() -> Unit = {}
) {
    ExperimentoScaffold(
        titulo = stringResource(R.string.experimento_carrera_hilos_nombre),
        onVolver = onVolver,
        acciones = acciones
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.experimento_carrera_hilos_concepto),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                Text(
                    text = stringResource(R.string.carrera_hilos_descripcion_corta),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                ControlesCarreraHilos(
                    configuracion = configuracion,
                    habilitarConfiguracion = estado != EstadoCarreraHilos.Ejecutando,
                    puedeIniciar = puedeIniciar,
                    puedeCancelar = puedeCancelar,
                    puedeReiniciar = puedeReiniciar,
                    onCantidadHilos = onCantidadHilos,
                    onCantidadTrabajo = onCantidadTrabajo,
                    onIniciar = onIniciar,
                    onCancelar = onCancelar,
                    onReiniciar = onReiniciar
                )
            }
            item {
                SeccionCarrera(titulo = stringResource(R.string.carrera_hilos_corredores)) {
                    progresos.forEach { progreso ->
                        FilaProgresoHilo(
                            progreso = progreso,
                            estadoTexto = textoEstadoHilo(progreso.estado),
                            colorEstado = colorEstadoHilo(progreso.estado)
                        )
                    }
                }
            }
            item {
                PanelMetricasCarrera(
                    configuracion = configuracion,
                    estadoTexto = textoEstadoCarrera(estado),
                    tiempoTotalMs = tiempoTotalMs,
                    progresos = progresos,
                    historial = historial,
                    mejorTiempoMismaCantidad = mejorTiempoMismaCantidad
                )
            }
            item {
                ResultadoCarrera(
                    resultado = resultadoUltimaEjecucion,
                    estadoTexto = resultadoUltimaEjecucion?.let {
                        textoEstadoCarrera(it.estadoFinal)
                    } ?: stringResource(R.string.resultado_sin_ejecucion)
                )
            }
            item {
                TablaHistorialCarrera(
                    historial = historial,
                    estadoTexto = { textoEstadoCarrera(it) }
                )
            }
            item {
                ComparacionCarrera(historial = historial)
            }
            item {
                ComoVerificarCarreraHilos()
            }
        }
    }
}

@Composable
private fun ControlesCarreraHilos(
    configuracion: ConfiguracionCarreraHilos,
    habilitarConfiguracion: Boolean,
    puedeIniciar: Boolean,
    puedeCancelar: Boolean,
    puedeReiniciar: Boolean,
    onCantidadHilos: (Int) -> Unit,
    onCantidadTrabajo: (Int) -> Unit,
    onIniciar: () -> Unit,
    onCancelar: () -> Unit,
    onReiniciar: () -> Unit
) {
    SeccionCarrera(titulo = stringResource(R.string.carrera_hilos_controles)) {
        SelectorCantidadHilos(
            cantidadSeleccionada = configuracion.cantidadHilos,
            habilitado = habilitarConfiguracion,
            onSeleccionar = onCantidadHilos
        )
        ControlCantidadTrabajo(
            cantidadSeleccionada = configuracion.cantidadTrabajo,
            habilitado = habilitarConfiguracion,
            onSeleccionar = onCantidadTrabajo
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onIniciar,
                enabled = puedeIniciar
            ) {
                Text(stringResource(R.string.accion_iniciar))
            }
            OutlinedButton(
                onClick = onCancelar,
                enabled = puedeCancelar
            ) {
                Text(stringResource(R.string.accion_cancelar))
            }
            OutlinedButton(
                onClick = onReiniciar,
                enabled = puedeReiniciar
            ) {
                Text(stringResource(R.string.accion_reiniciar))
            }
        }
    }
}

@Composable
private fun ComoVerificarCarreraHilos() {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val comandos = listOf(
        ComandoVerificacionCarrera(
            comando = "adb shell top",
            descripcionResId = R.string.verificacion_top_carrera_hilos
        ),
        ComandoVerificacionCarrera(
            comando = "adb logcat -d -s OSPlayground/ThreadRace",
            descripcionResId = R.string.verificacion_logcat_carrera_hilos
        ),
        ComandoVerificacionCarrera(
            comando = "Android Studio > Profiler > CPU > seleccionar proceso de la app > ejecutar Thread Race",
            descripcionResId = R.string.verificacion_profiler_carrera_hilos
        ),
        ComandoVerificacionCarrera(
            comando = "Android Studio > Profiler > CPU > Record trace mientras Thread Race esta ejecutando",
            descripcionResId = R.string.verificacion_trace_carrera_hilos
        )
    )

    SeccionCarrera(titulo = stringResource(R.string.seccion_como_verificar)) {
        comandos.forEach { item ->
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(item.descripcionResId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.comando,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                clipboard.setClipEntry(
                                    ClipEntry(ClipData.newPlainText("comando adb", item.comando))
                                )
                            }
                        }
                    ) {
                        Text(stringResource(R.string.accion_copiar))
                    }
                }
            }
        }
    }
}

private data class ComandoVerificacionCarrera(
    val comando: String,
    val descripcionResId: Int
)

@Composable
private fun DialogoSalidaCarreraHilos(
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.dialogo_salir_carrera_hilos_titulo)) },
        text = { Text(stringResource(R.string.dialogo_salir_carrera_hilos_mensaje)) },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(stringResource(R.string.accion_salir_y_cancelar))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text(stringResource(R.string.accion_continuar))
            }
        }
    )
}

@Composable
private fun textoEstadoCarrera(estado: EstadoCarreraHilos): String {
    return when (estado) {
        EstadoCarreraHilos.Inactiva -> stringResource(R.string.estado_inactivo)
        EstadoCarreraHilos.Ejecutando -> stringResource(R.string.estado_ejecucion)
        EstadoCarreraHilos.Exitosa -> stringResource(R.string.estado_exitoso)
        EstadoCarreraHilos.Cancelada -> stringResource(R.string.resultado_cancelado)
        EstadoCarreraHilos.Error -> stringResource(R.string.estado_error)
    }
}

@Composable
private fun textoEstadoHilo(estado: EstadoHiloCarrera): String {
    return when (estado) {
        EstadoHiloCarrera.Esperando -> stringResource(R.string.estado_esperando)
        EstadoHiloCarrera.Ejecutando -> stringResource(R.string.estado_ejecucion)
        EstadoHiloCarrera.Finalizado -> stringResource(R.string.estado_finalizado)
        EstadoHiloCarrera.Cancelado -> stringResource(R.string.resultado_cancelado)
        EstadoHiloCarrera.Error -> stringResource(R.string.estado_error)
    }
}

@Composable
private fun colorEstadoHilo(estado: EstadoHiloCarrera): Color {
    return when (estado) {
        EstadoHiloCarrera.Ejecutando -> MaterialTheme.colorScheme.tertiary
        EstadoHiloCarrera.Finalizado -> MaterialTheme.colorScheme.primary
        EstadoHiloCarrera.Error -> MaterialTheme.colorScheme.error
        EstadoHiloCarrera.Esperando,
        EstadoHiloCarrera.Cancelado -> MaterialTheme.colorScheme.outline
    }
}

private fun String.formatear(vararg argumentos: Any): String {
    return String.format(Locale.getDefault(), this, *argumentos)
}

@Preview(showBackground = true)
@Composable
private fun PantallaCarreraHilosInicialPreview() {
    ArchitecturasOSTheme {
        ContenidoCarreraHilos(
            estado = EstadoCarreraHilos.Inactiva,
            configuracion = ConfiguracionCarreraHilos(),
            progresos = listOf(
                ProgresoHiloCarrera(1, "Hilo 1", operacionesTotales = 1_000_000L),
                ProgresoHiloCarrera(2, "Hilo 2", operacionesTotales = 1_000_000L),
                ProgresoHiloCarrera(3, "Hilo 3", operacionesTotales = 1_000_000L),
                ProgresoHiloCarrera(4, "Hilo 4", operacionesTotales = 1_000_000L)
            ),
            tiempoTotalMs = 0L,
            resultadoUltimaEjecucion = null,
            historial = emptyList(),
            mejorTiempoMismaCantidad = null,
            puedeIniciar = true,
            puedeCancelar = false,
            puedeReiniciar = true,
            onCantidadHilos = {},
            onCantidadTrabajo = {},
            onIniciar = {},
            onCancelar = {},
            onReiniciar = {},
            onVolver = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaCarreraHilosEjecutandoPreview() {
    ArchitecturasOSTheme {
        ContenidoCarreraHilos(
            estado = EstadoCarreraHilos.Ejecutando,
            configuracion = ConfiguracionCarreraHilos(cantidadHilos = 4, cantidadTrabajo = 3),
            progresos = (1..4).map { id ->
                ProgresoHiloCarrera(
                    idHilo = id,
                    nombreHilo = "ThreadRace-1-$id",
                    estado = EstadoHiloCarrera.Ejecutando,
                    progreso = id * 0.18f,
                    operacionesCompletadas = id * 250_000L,
                    operacionesTotales = 2_000_000L,
                    tiempoMs = id * 120L
                )
            },
            tiempoTotalMs = 480L,
            resultadoUltimaEjecucion = null,
            historial = emptyList(),
            mejorTiempoMismaCantidad = null,
            puedeIniciar = false,
            puedeCancelar = true,
            puedeReiniciar = false,
            onCantidadHilos = {},
            onCantidadTrabajo = {},
            onIniciar = {},
            onCancelar = {},
            onReiniciar = {},
            onVolver = {}
        )
    }
}
