package io.yerdna.architecturasos.ui.screen

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.yerdna.architecturasos.R
import io.yerdna.architecturasos.procesos.ControladorFabricaRobots
import io.yerdna.architecturasos.procesos.EstadoEjecucionFabrica
import io.yerdna.architecturasos.procesos.EstadoFabricaRobots
import io.yerdna.architecturasos.procesos.FabricaRobotsViewModel
import io.yerdna.architecturasos.procesos.ResultadoUltimaEjecucion
import io.yerdna.architecturasos.procesos.UMBRAL_ADVERTENCIA_ROBOTS
import io.yerdna.architecturasos.ui.component.BotonRegistroEventos
import io.yerdna.architecturasos.ui.component.ExperimentoScaffold
import io.yerdna.architecturasos.ui.component.HojaRegistroEventos
import io.yerdna.architecturasos.ui.component.rememberRegistroExperimento
import io.yerdna.architecturasos.ui.theme.ArchitecturasOSTheme
import io.yerdna.architecturasos.util.EventoExperimento
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun PantallaFabricaRobots(
    onVolver: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val viewModel = remember { FabricaRobotsViewModel() }
    val registro = rememberRegistroExperimento("OSPlayground/FabricaRobots")
    val controlador = remember {
        ControladorFabricaRobots(
            applicationContext = context,
            callbacks = object : ControladorFabricaRobots.Callbacks {
                override fun onPidFabrica(pid: Int, mensajes: Int) {
                    viewModel.actualizarPidFabrica(pid, mensajes)
                }

                override fun onEstadoFabrica(estado: EstadoEjecucionFabrica, mensajes: Int) {
                    if (estado == EstadoEjecucionFabrica.Inactivo &&
                        viewModel.estado.robotsEnsamblados >= viewModel.estado.robotsConfigurados &&
                        viewModel.estado.robotsEnsamblados > 0
                    ) {
                        viewModel.marcarCompletado(mensajes)
                        registro.logger.info("Experimento completado")
                    } else {
                        viewModel.actualizarEstadoFabrica(estado, mensajes)
                    }
                }

                override fun onRobotEnsamblado(robots: Int, mensajes: Int) {
                    viewModel.robotEnsamblado(robots, mensajes)
                }

                override fun onError(mensaje: String, mensajes: Int) {
                    val texto = mensaje.ifBlank { "Error en la fabrica secundaria" }
                    registro.logger.error(texto)
                    viewModel.marcarError(texto, mensajes)
                }

                override fun onEventoRegistro(evento: EventoExperimento) {
                    registro.logger.registrar(evento)
                }
            }
        )
    }

    var mostrarConfirmacion by remember { mutableStateOf(false) }
    fun detener(cancelado: Boolean) {
        registro.logger.info("Detencion solicitada por el usuario")
        viewModel.iniciarDetencion()
        controlador.detener()
        if (cancelado) {
            viewModel.marcarCancelado()
            registro.logger.advertencia("Experimento cancelado")
        }
    }

    fun solicitarSalida() {
        when (viewModel.estado.estado) {
            EstadoEjecucionFabrica.Iniciando,
            EstadoEjecucionFabrica.Ejecucion -> mostrarConfirmacion = true
            EstadoEjecucionFabrica.Deteniendo -> Unit
            EstadoEjecucionFabrica.Inactivo,
            EstadoEjecucionFabrica.Error -> onVolver()
        }
    }

    BackHandler {
        solicitarSalida()
    }

    DisposableEffect(controlador) {
        onDispose {
            controlador.liberar()
        }
    }

    if (mostrarConfirmacion) {
        DialogoCancelarExperimento(
            onConfirmar = {
                mostrarConfirmacion = false
                registro.logger.advertencia("Salida confirmada: se detendra la fabrica")
                detener(cancelado = true)
                onVolver()
            },
            onCancelar = { mostrarConfirmacion = false }
        )
    }

    ContenidoFabricaRobots(
        estado = viewModel.estado,
        textoCantidadRobots = viewModel.textoCantidadRobots,
        cantidadValida = viewModel.cantidadRobotsValida != null,
        onCantidadChange = viewModel::actualizarCantidad,
        onIniciar = {
            val cantidad = viewModel.cantidadRobotsValida ?: return@ContenidoFabricaRobots
            registro.limpiar()
            registro.logger.info("Inicio solicitado para $cantidad robots")
            viewModel.prepararInicio(cantidad)
            controlador.iniciar(cantidad)
        },
        onDetener = { detener(cancelado = true) },
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
private fun ContenidoFabricaRobots(
    estado: EstadoFabricaRobots,
    textoCantidadRobots: String,
    cantidadValida: Boolean,
    onCantidadChange: (String) -> Unit,
    onIniciar: () -> Unit,
    onDetener: () -> Unit,
    onVolver: () -> Unit,
    acciones: @Composable RowScope.() -> Unit = {}
) {
    ExperimentoScaffold(
        titulo = stringResource(R.string.experimento_fabrica_robots_nombre),
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
                    text = stringResource(R.string.experimento_fabrica_robots_concepto),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                ConfiguracionFabrica(
                    estado = estado,
                    textoCantidadRobots = textoCantidadRobots,
                    cantidadValida = cantidadValida,
                    onCantidadChange = onCantidadChange,
                    onIniciar = onIniciar,
                    onDetener = onDetener
                )
            }
            item {
                VisualizacionProcesos(estado = estado)
            }
            item {
                ResultadoFabrica(estado = estado)
            }
            item {
                ComoVerificar()
            }
        }
    }
}

@Composable
private fun ConfiguracionFabrica(
    estado: EstadoFabricaRobots,
    textoCantidadRobots: String,
    cantidadValida: Boolean,
    onCantidadChange: (String) -> Unit,
    onIniciar: () -> Unit,
    onDetener: () -> Unit
) {
    val inputHabilitado = estado.estado == EstadoEjecucionFabrica.Inactivo ||
        estado.estado == EstadoEjecucionFabrica.Error
    val iniciarHabilitado = inputHabilitado && cantidadValida
    val detenerHabilitado = estado.estado == EstadoEjecucionFabrica.Ejecucion
    val cantidad = textoCantidadRobots.toIntOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = textoCantidadRobots,
            onValueChange = onCantidadChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = inputHabilitado,
            isError = !cantidadValida,
            label = { Text(stringResource(R.string.metrica_robots_configurados)) },
            supportingText = {
                when {
                    !cantidadValida -> Text(stringResource(R.string.error_cantidad_robots))
                    cantidad != null && cantidad > UMBRAL_ADVERTENCIA_ROBOTS -> {
                        Text(stringResource(R.string.advertencia_duracion_larga, cantidad))
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onIniciar,
                enabled = iniciarHabilitado
            ) {
                Text(stringResource(R.string.accion_iniciar))
            }
            OutlinedButton(
                onClick = onDetener,
                enabled = detenerHabilitado
            ) {
                Text(stringResource(R.string.accion_detener))
            }
        }
    }
}

@Composable
private fun VisualizacionProcesos(estado: EstadoFabricaRobots) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.seccion_procesos),
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BloqueProceso(
                nombre = stringResource(R.string.proceso_oficina_principal),
                pid = estado.pidPrincipal.toString(),
                estado = stringResource(R.string.estado_activo),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (estado.estado == EstadoEjecucionFabrica.Iniciando ||
                    estado.estado == EstadoEjecucionFabrica.Ejecucion
                ) {
                    "->"
                } else {
                    "--"
                },
                style = MaterialTheme.typography.titleMedium
            )
            BloqueProceso(
                nombre = stringResource(R.string.proceso_fabrica_secundaria),
                pid = estado.pidSecundario?.toString() ?: stringResource(R.string.valor_no_disponible),
                estado = textoEstado(estado.estado),
                color = colorEstado(estado.estado),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BloqueProceso(
    nombre: String,
    pid: String,
    estado: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(color, RoundedCornerShape(6.dp))
            )
            Text(
                text = nombre,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.metrica_pid, pid),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = estado,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}

@Composable
private fun ResultadoFabrica(estado: EstadoFabricaRobots) {
    SeccionSimple(titulo = stringResource(R.string.resultado_ultima_ejecucion)) {
        DatoResultado(
            etiqueta = stringResource(R.string.resultado_ultima_ejecucion),
            valor = textoResultado(estado.resultadoUltimaEjecucion)
        )
        DatoResultado(
            etiqueta = stringResource(R.string.metrica_robots_configurados),
            valor = estado.robotsConfigurados.toString()
        )
        DatoResultado(
            etiqueta = stringResource(R.string.metrica_robots_ensamblados),
            valor = estado.robotsEnsamblados.toString()
        )
        DatoResultado(
            etiqueta = stringResource(R.string.metrica_mensajes_intercambiados),
            valor = estado.mensajesIntercambiados.toString()
        )
        DatoResultado(
            etiqueta = stringResource(R.string.metrica_hora_inicio),
            valor = formatoHora(estado.horaInicio)
        )
        DatoResultado(
            etiqueta = stringResource(R.string.metrica_hora_fin),
            valor = formatoHora(estado.horaFin)
        )
        estado.mensajeError?.let { mensaje ->
            DatoResultado(
                etiqueta = stringResource(R.string.estado_error),
                valor = mensaje
            )
        }
    }
}

@Composable
private fun ComoVerificar() {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val comandos = listOf(
        ComandoVerificacion(
            comando = "adb shell ps -A | findstr architecturasos",
            descripcionResId = R.string.verificacion_ps_fabrica
        ),
        ComandoVerificacion(
            comando = "adb shell dumpsys activity services io.yerdna.architecturasos | findstr fabrica_robots",
            descripcionResId = R.string.verificacion_dumpsys_fabrica
        ),
        ComandoVerificacion(
            comando = "adb shell top",
            descripcionResId = R.string.verificacion_top_fabrica
        ),
        ComandoVerificacion(
            comando = "adb logcat -d -s OSPlayground/FabricaRobots",
            descripcionResId = R.string.verificacion_logcat_fabrica
        )
    )

    SeccionSimple(titulo = stringResource(R.string.seccion_como_verificar)) {
        comandos.forEach { item ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                    ClipEntry(
                                        ClipData.newPlainText("comando adb", item.comando)
                                    )
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

private data class ComandoVerificacion(
    val comando: String,
    val descripcionResId: Int
)

@Composable
private fun SeccionSimple(
    titulo: String,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = contenido
        )
    }
}

@Composable
private fun DatoResultado(
    etiqueta: String,
    valor: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DialogoCancelarExperimento(
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.dialogo_cancelar_experimento_titulo)) },
        text = { Text(stringResource(R.string.dialogo_cancelar_experimento_mensaje)) },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(stringResource(R.string.accion_salir_y_detener))
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
private fun textoEstado(estado: EstadoEjecucionFabrica): String {
    return when (estado) {
        EstadoEjecucionFabrica.Inactivo -> stringResource(R.string.estado_inactivo)
        EstadoEjecucionFabrica.Iniciando -> stringResource(R.string.estado_iniciando)
        EstadoEjecucionFabrica.Ejecucion -> stringResource(R.string.estado_ejecucion)
        EstadoEjecucionFabrica.Deteniendo -> stringResource(R.string.estado_deteniendo)
        EstadoEjecucionFabrica.Error -> stringResource(R.string.estado_error)
    }
}

@Composable
private fun textoResultado(resultado: ResultadoUltimaEjecucion): String {
    return when (resultado) {
        ResultadoUltimaEjecucion.SinEjecucion -> stringResource(R.string.resultado_sin_ejecucion)
        ResultadoUltimaEjecucion.Completado -> stringResource(R.string.resultado_completado)
        ResultadoUltimaEjecucion.Cancelado -> stringResource(R.string.resultado_cancelado)
        ResultadoUltimaEjecucion.Error -> stringResource(R.string.resultado_error)
    }
}

@Composable
private fun colorEstado(estado: EstadoEjecucionFabrica): Color {
    return when (estado) {
        EstadoEjecucionFabrica.Inactivo -> MaterialTheme.colorScheme.outline
        EstadoEjecucionFabrica.Iniciando -> MaterialTheme.colorScheme.tertiary
        EstadoEjecucionFabrica.Ejecucion -> MaterialTheme.colorScheme.primary
        EstadoEjecucionFabrica.Deteniendo -> MaterialTheme.colorScheme.secondary
        EstadoEjecucionFabrica.Error -> MaterialTheme.colorScheme.error
    }
}

private fun formatoHora(valor: Long?): String {
    if (valor == null) return "--"
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(valor))
}

@Preview(showBackground = true)
@Composable
private fun PantallaFabricaRobotsInactivoPreview() {
    ArchitecturasOSTheme {
        ContenidoFabricaRobots(
            estado = EstadoFabricaRobots(),
            textoCantidadRobots = "60",
            cantidadValida = true,
            onCantidadChange = {},
            onIniciar = {},
            onDetener = {},
            onVolver = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaFabricaRobotsEjecucionPreview() {
    ArchitecturasOSTheme {
        ContenidoFabricaRobots(
            estado = EstadoFabricaRobots(
                pidSecundario = 24510,
                estado = EstadoEjecucionFabrica.Ejecucion,
                robotsConfigurados = 60,
                robotsEnsamblados = 18,
                mensajesIntercambiados = 20,
                horaInicio = System.currentTimeMillis()
            ),
            textoCantidadRobots = "60",
            cantidadValida = true,
            onCantidadChange = {},
            onIniciar = {},
            onDetener = {},
            onVolver = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaFabricaRobotsErrorPreview() {
    ArchitecturasOSTheme {
        ContenidoFabricaRobots(
            estado = EstadoFabricaRobots(
                estado = EstadoEjecucionFabrica.Error,
                resultadoUltimaEjecucion = ResultadoUltimaEjecucion.Error,
                mensajeError = "No se pudo comunicar con la fabrica secundaria"
            ),
            textoCantidadRobots = "60",
            cantidadValida = true,
            onCantidadChange = {},
            onIniciar = {},
            onDetener = {},
            onVolver = {}
        )
    }
}
