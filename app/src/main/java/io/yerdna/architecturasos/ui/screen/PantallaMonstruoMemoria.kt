package io.yerdna.architecturasos.ui.screen

import android.app.ActivityManager
import android.content.ClipData
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.yerdna.architecturasos.R
import io.yerdna.architecturasos.memoria.EstadoEjecucionMemoria
import io.yerdna.architecturasos.memoria.EstadoMonstruoMemoria
import io.yerdna.architecturasos.memoria.EstadoVisualMonstruo
import io.yerdna.architecturasos.memoria.EventoRegistroMonstruoMemoria
import io.yerdna.architecturasos.memoria.MonstruoMemoriaViewModel
import io.yerdna.architecturasos.memoria.MuestraMemoria
import io.yerdna.architecturasos.memoria.TAG_MONSTRUO_MEMORIA
import io.yerdna.architecturasos.ui.component.BotonRegistroEventos
import io.yerdna.architecturasos.ui.component.ExperimentoScaffold
import io.yerdna.architecturasos.ui.component.HojaRegistroEventos
import io.yerdna.architecturasos.ui.component.rememberRegistroExperimento
import io.yerdna.architecturasos.ui.theme.ArchitecturasOSTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ESTADOS_NUEVA_EJECUCION = setOf(
    EstadoEjecucionMemoria.Inactivo,
    EstadoEjecucionMemoria.Liberado,
    EstadoEjecucionMemoria.Error,
    EstadoEjecucionMemoria.RecolectorSolicitado
)

// Pantalla principal del experimento monstruo de memoria, arma el estado y conecta las acciones con el ViewModel
@Composable
fun PantallaMonstruoMemoria(
    onVolver: () -> Unit
) {
    val viewModel = remember { MonstruoMemoriaViewModel() }
    val registro = rememberRegistroExperimento(TAG_MONSTRUO_MEMORIA)
    val context = LocalContext.current.applicationContext
    var mostrarConfirmacionSalida by remember { mutableStateOf(false) }
    val textoNoDisponible = stringResource(R.string.valor_no_disponible)
    val logApertura = stringResource(R.string.log_monstruo_memoria_apertura)
    val logIntentoReserva = stringResource(R.string.log_monstruo_memoria_intento_reserva)
    val logReservaCompletada = stringResource(R.string.log_monstruo_memoria_reserva_completada)
    val logReservaBloqueada = stringResource(R.string.log_monstruo_memoria_reserva_bloqueada)
    val logAdvertenciaPresion = stringResource(R.string.log_monstruo_memoria_advertencia_presion)
    val logLiberacion = stringResource(R.string.log_monstruo_memoria_liberacion)
    val logSolicitudGc = stringResource(R.string.log_monstruo_memoria_solicitud_gc)
    val logReinicio = stringResource(R.string.log_monstruo_memoria_reinicio)
    val logSalidaConfirmada = stringResource(R.string.log_monstruo_memoria_salida_confirmada)
    val logError = stringResource(R.string.log_monstruo_memoria_error)

    // Traduce cada evento del experimento a una linea de log en el registro correspondiente
    fun registrarEvento(evento: EventoRegistroMonstruoMemoria) {
        when (evento) {
            EventoRegistroMonstruoMemoria.AperturaModulo -> registro.logger.info(logApertura)

            is EventoRegistroMonstruoMemoria.IntentoReserva -> registro.logger.info(
                logIntentoReserva.formatear(evento.tamanoMb)
            )

            is EventoRegistroMonstruoMemoria.ReservaCompletada -> registro.logger.info(
                logReservaCompletada.formatear(evento.tamanoMb, evento.idBloque, evento.memoriaReservadaMb)
            )

            is EventoRegistroMonstruoMemoria.ReservaBloqueada -> registro.logger.advertencia(
                logReservaBloqueada.formatear(evento.tamanoMb, evento.memoriaReservadaMb)
            )

            is EventoRegistroMonstruoMemoria.AdvertenciaPresion -> registro.logger.advertencia(
                logAdvertenciaPresion.formatear(evento.porcentajeUsado)
            )

            is EventoRegistroMonstruoMemoria.LiberacionMemoria -> registro.logger.info(
                logLiberacion.formatear(evento.bloquesLiberados)
            )

            EventoRegistroMonstruoMemoria.SolicitudGc -> registro.logger.info(logSolicitudGc)
            EventoRegistroMonstruoMemoria.ReinicioConfirmado -> registro.logger.info(logReinicio)
            EventoRegistroMonstruoMemoria.SalidaConfirmada -> registro.logger.advertencia(logSalidaConfirmada)
            is EventoRegistroMonstruoMemoria.ErrorTecnico -> registro.logger.error(
                logError.formatear(evento.mensaje ?: textoNoDisponible)
            )
        }
    }

    // Decide si hay que pedir confirmacion antes de salir o si se puede volver de una vez
    fun solicitarSalida() {
        if (viewModel.estado.hayTrabajoActivo) {
            mostrarConfirmacionSalida = true
        } else {
            onVolver()
        }
    }

    // Limpia el registro si toca arrancar una ejecucion nueva y dispara la reserva de memoria
    fun onReservar(tamanoMb: Int) {
        if (viewModel.estado.estadoEjecucion in ESTADOS_NUEVA_EJECUCION) {
            registro.limpiar()
        }
        viewModel.reservar(tamanoMb, ::registrarEvento)
    }

    BackHandler {
        solicitarSalida()
    }

    LaunchedEffect(Unit) {
        registrarEvento(EventoRegistroMonstruoMemoria.AperturaModulo)
    }

    LaunchedEffect(viewModel.estado.estadoEjecucion) {
        viewModel.actualizarMedicionSistema(obtenerMemoriaDisponibleSistemaMb(context))
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.limpiarRecursos()
        }
    }

    if (mostrarConfirmacionSalida) {
        DialogoSalidaMonstruoMemoria(
            onConfirmar = {
                mostrarConfirmacionSalida = false
                registrarEvento(EventoRegistroMonstruoMemoria.SalidaConfirmada)
                viewModel.liberarMemoria(::registrarEvento)
                viewModel.limpiarRecursos()
                onVolver()
            },
            onCancelar = { mostrarConfirmacionSalida = false }
        )
    }

    ContenidoMonstruoMemoria(
        estado = viewModel.estado,
        onReservar = ::onReservar,
        onLiberar = { viewModel.liberarMemoria(::registrarEvento) },
        onSolicitarGc = { viewModel.solicitarGc(::registrarEvento) },
        onReiniciar = {
            registro.limpiar()
            viewModel.reiniciar(::registrarEvento)
        },
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

// Consulta al sistema cuanta memoria RAM disponible queda usando ActivityManager
private fun obtenerMemoriaDisponibleSistemaMb(context: Context): Long? {
    val gestor = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return null
    val info = ActivityManager.MemoryInfo()
    gestor.getMemoryInfo(info)
    return info.availMem / (1024 * 1024)
}

// Arma todo el contenido de la pantalla dentro del scaffold: explicacion, visual, controles, metricas e historico
@Composable
private fun ContenidoMonstruoMemoria(
    estado: EstadoMonstruoMemoria,
    onReservar: (Int) -> Unit,
    onLiberar: () -> Unit,
    onSolicitarGc: () -> Unit,
    onReiniciar: () -> Unit,
    onVolver: () -> Unit,
    acciones: @Composable RowScope.() -> Unit = {}
) {
    ExperimentoScaffold(
        titulo = stringResource(R.string.experimento_monstruo_memoria_nombre),
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
                    text = stringResource(R.string.experimento_monstruo_memoria_concepto),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                Text(
                    text = stringResource(R.string.monstruo_memoria_descripcion_corta),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Text(
                    text = stringResource(R.string.monstruo_memoria_explicacion_paginacion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                VisualMonstruoMemoria(
                    estadoVisual = estado.estadoVisual,
                    porcentajeUsado = calcularPorcentajeUsado(estado)
                )
            }
            item {
                ControlesMonstruoMemoria(
                    estado = estado,
                    onReservar = onReservar,
                    onLiberar = onLiberar,
                    onSolicitarGc = onSolicitarGc,
                    onReiniciar = onReiniciar
                )
            }
            item {
                PanelMetricasMonstruoMemoria(estado = estado)
            }
            item {
                PanelResultadoMonstruoMemoria(resultado = estado.resultadoUltimaAccion)
            }
            item {
                PanelHistoricoMonstruoMemoria(historico = estado.historico)
            }
            item {
                ComoVerificarMonstruoMemoria()
            }
        }
    }
}

// Calcula el porcentaje de memoria reservada respecto al limite seguro configurado
private fun calcularPorcentajeUsado(estado: EstadoMonstruoMemoria): Int {
    if (estado.limiteSeguroMb <= 0) return 0
    return (estado.memoriaReservadaMb * 100) / estado.limiteSeguroMb
}

// Dibuja el circulo que crece de tamano y cambia de color segun cuanta presion de memoria hay
@Composable
private fun VisualMonstruoMemoria(
    estadoVisual: EstadoVisualMonstruo,
    porcentajeUsado: Int
) {
    val color = when (estadoVisual) {
        EstadoVisualMonstruo.Saludable -> MaterialTheme.colorScheme.tertiary
        EstadoVisualMonstruo.Moderado -> MaterialTheme.colorScheme.primary
        EstadoVisualMonstruo.Alto -> MaterialTheme.colorScheme.secondary
        EstadoVisualMonstruo.PresionMemoria -> MaterialTheme.colorScheme.error
    }
    val tamano = (80 + porcentajeUsado.coerceIn(0, 100) * 0.6).dp

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(tamano)
                .background(color = color, shape = CircleShape)
        )
        Text(
            text = textoEstadoVisual(estadoVisual),
            style = MaterialTheme.typography.titleSmall,
            color = color
        )
        Text(
            text = stringResource(R.string.monstruo_memoria_porcentaje, porcentajeUsado),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Muestra los botones de reservar, liberar, solicitar gc y reiniciar, junto con la advertencia si la hay
@Composable
private fun ControlesMonstruoMemoria(
    estado: EstadoMonstruoMemoria,
    onReservar: (Int) -> Unit,
    onLiberar: () -> Unit,
    onSolicitarGc: () -> Unit,
    onReiniciar: () -> Unit
) {
    SeccionCarrera(titulo = stringResource(R.string.monstruo_memoria_controles)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onReservar(10) },
                enabled = estado.puedeReservar10Mb
            ) {
                Text(stringResource(R.string.accion_reservar_10mb))
            }
            Button(
                onClick = { onReservar(25) },
                enabled = estado.puedeReservar25Mb
            ) {
                Text(stringResource(R.string.accion_reservar_25mb))
            }
            Button(
                onClick = { onReservar(50) },
                enabled = estado.puedeReservar50Mb
            ) {
                Text(stringResource(R.string.accion_reservar_50mb))
            }
            OutlinedButton(
                onClick = onLiberar,
                enabled = estado.puedeLiberar
            ) {
                Text(stringResource(R.string.accion_liberar_memoria))
            }
            OutlinedButton(
                onClick = onSolicitarGc,
                enabled = estado.puedeSolicitarGc
            ) {
                Text(stringResource(R.string.accion_solicitar_gc))
            }
            OutlinedButton(
                onClick = onReiniciar,
                enabled = estado.puedeReiniciar
            ) {
                Text(stringResource(R.string.accion_reiniciar))
            }
        }
        if (estado.mensajeAdvertencia != null) {
            Text(
                text = estado.mensajeAdvertencia,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

// Lista todas las metricas de memoria del estado actual (usada, maxima, libre, reservada, etc)
@Composable
private fun PanelMetricasMonstruoMemoria(
    estado: EstadoMonstruoMemoria
) {
    val textoNoDisponible = stringResource(R.string.valor_no_disponible)
    SeccionCarrera(titulo = stringResource(R.string.monstruo_memoria_metricas)) {
        DatoCarrera(
            etiqueta = stringResource(R.string.monstruo_memoria_estado_general),
            valor = textoEstadoEjecucion(estado.estadoEjecucion)
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.monstruo_memoria_memoria_usada),
            valor = stringResource(R.string.monstruo_memoria_megabytes, estado.memoriaUsadaMb)
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.monstruo_memoria_memoria_maxima),
            valor = stringResource(R.string.monstruo_memoria_megabytes, estado.memoriaMaximaRuntimeMb)
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.monstruo_memoria_memoria_libre),
            valor = stringResource(R.string.monstruo_memoria_megabytes, estado.memoriaLibreRuntimeMb)
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.monstruo_memoria_memoria_disponible_sistema),
            valor = estado.memoriaDisponibleSistemaMb?.let {
                stringResource(R.string.monstruo_memoria_megabytes, it)
            } ?: textoNoDisponible
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.monstruo_memoria_memoria_reservada),
            valor = stringResource(R.string.monstruo_memoria_megabytes, estado.memoriaReservadaMb)
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.monstruo_memoria_bloques_reservados),
            valor = estado.bloquesReservados.toString()
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.monstruo_memoria_limite_seguro),
            valor = stringResource(R.string.monstruo_memoria_megabytes, estado.limiteSeguroMb)
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.monstruo_memoria_porcentaje_usado),
            valor = stringResource(R.string.monstruo_memoria_porcentaje, calcularPorcentajeUsado(estado))
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.monstruo_memoria_estado_visual),
            valor = textoEstadoVisual(estado.estadoVisual)
        )
        if (estado.mensajeError != null) {
            Text(
                text = estado.mensajeError,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

// Muestra el texto con el resultado de la ultima accion ejecutada o un mensaje de que aun no hay nada
@Composable
private fun PanelResultadoMonstruoMemoria(
    resultado: String?
) {
    SeccionCarrera(titulo = stringResource(R.string.resultado_ultima_ejecucion)) {
        Text(
            text = resultado ?: stringResource(R.string.resultado_sin_ejecucion),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Muestra el listado historico de muestras de memoria, o un mensaje si todavia esta vacio
@Composable
private fun PanelHistoricoMonstruoMemoria(
    historico: List<MuestraMemoria>
) {
    SeccionCarrera(titulo = stringResource(R.string.monstruo_memoria_historico)) {
        if (historico.isEmpty()) {
            Text(
                text = stringResource(R.string.monstruo_memoria_historico_vacio),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val formato = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
            historico.asReversed().forEach { muestra ->
                DatoCarrera(
                    etiqueta = formato.format(Date(muestra.timestamp)),
                    valor = stringResource(
                        R.string.monstruo_memoria_historico_item,
                        muestra.memoriaUsadaMb,
                        muestra.bloquesReservados
                    )
                )
            }
        }
    }
}

// Muestra los comandos adb sugeridos para verificar el experimento, cada uno con boton para copiar
@Composable
private fun ComoVerificarMonstruoMemoria() {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val comandos = listOf(
        ComandoVerificacionMonstruoMemoria(
            comando = "adb shell dumpsys meminfo io.yerdna.architecturasos",
            descripcionResId = R.string.verificacion_meminfo_monstruo_memoria
        ),
        ComandoVerificacionMonstruoMemoria(
            comando = "adb shell top",
            descripcionResId = R.string.verificacion_top_monstruo_memoria
        ),
        ComandoVerificacionMonstruoMemoria(
            comando = "adb logcat -d -s OSPlayground/Memory",
            descripcionResId = R.string.verificacion_logcat_monstruo_memoria
        ),
        ComandoVerificacionMonstruoMemoria(
            comando = "adb logcat -c",
            descripcionResId = R.string.verificacion_logcat_clear_monstruo_memoria
        )
    )

    SeccionCarrera(titulo = stringResource(R.string.seccion_como_verificar)) {
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

private data class ComandoVerificacionMonstruoMemoria(
    val comando: String,
    val descripcionResId: Int
)

// Dialogo de confirmacion que aparece al intentar salir con trabajo de memoria activo
@Composable
private fun DialogoSalidaMonstruoMemoria(
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.dialogo_salir_monstruo_memoria_titulo)) },
        text = { Text(stringResource(R.string.dialogo_salir_monstruo_memoria_mensaje)) },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(stringResource(R.string.accion_salir_y_liberar))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text(stringResource(R.string.accion_continuar))
            }
        }
    )
}

// Traduce el estado de ejecucion a su texto legible correspondiente
@Composable
private fun textoEstadoEjecucion(estadoEjecucion: EstadoEjecucionMemoria): String {
    return when (estadoEjecucion) {
        EstadoEjecucionMemoria.Inactivo -> stringResource(R.string.estado_inactivo)
        EstadoEjecucionMemoria.Reservando -> stringResource(R.string.estado_reservando)
        EstadoEjecucionMemoria.Exitoso -> stringResource(R.string.estado_exitoso)
        EstadoEjecucionMemoria.Advertencia -> stringResource(R.string.estado_advertencia)
        EstadoEjecucionMemoria.Error -> stringResource(R.string.estado_error)
        EstadoEjecucionMemoria.Liberado -> stringResource(R.string.estado_liberado)
        EstadoEjecucionMemoria.RecolectorSolicitado -> stringResource(R.string.estado_recolector_solicitado)
    }
}

// Traduce el estado visual del monstruo a su texto legible correspondiente
@Composable
private fun textoEstadoVisual(estadoVisual: EstadoVisualMonstruo): String {
    return when (estadoVisual) {
        EstadoVisualMonstruo.Saludable -> stringResource(R.string.estado_visual_saludable)
        EstadoVisualMonstruo.Moderado -> stringResource(R.string.estado_visual_moderado)
        EstadoVisualMonstruo.Alto -> stringResource(R.string.estado_visual_alto)
        EstadoVisualMonstruo.PresionMemoria -> stringResource(R.string.estado_visual_presion_memoria)
    }
}

// Extension corta para formatear strings con argumentos usando el locale por defecto
private fun String.formatear(vararg argumentos: Any): String {
    return String.format(Locale.getDefault(), this, *argumentos)
}

// Preview de la pantalla en su estado inicial, sin memoria reservada todavia
@Preview(showBackground = true)
@Composable
private fun PantallaMonstruoMemoriaInactivaPreview() {
    ArchitecturasOSTheme {
        ContenidoMonstruoMemoria(
            estado = EstadoMonstruoMemoria(limiteSeguroMb = 200),
            onReservar = {},
            onLiberar = {},
            onSolicitarGc = {},
            onReiniciar = {},
            onVolver = {}
        )
    }
}

// Preview de la pantalla con una reserva exitosa y su historico con una muestra
@Preview(showBackground = true)
@Composable
private fun PantallaMonstruoMemoriaExitosaPreview() {
    ArchitecturasOSTheme {
        ContenidoMonstruoMemoria(
            estado = EstadoMonstruoMemoria(
                estadoEjecucion = EstadoEjecucionMemoria.Exitoso,
                estadoVisual = EstadoVisualMonstruo.Moderado,
                memoriaUsadaMb = 90,
                memoriaMaximaRuntimeMb = 256,
                memoriaLibreRuntimeMb = 110,
                memoriaDisponibleSistemaMb = 900,
                memoriaReservadaMb = 60,
                bloquesReservados = 2,
                limiteSeguroMb = 200,
                resultadoUltimaAccion = "Reservados 25 MB (bloque #2)",
                historico = listOf(
                    MuestraMemoria(
                        timestamp = System.currentTimeMillis(),
                        memoriaUsadaMb = 90,
                        memoriaLibreRuntimeMb = 110,
                        memoriaMaximaRuntimeMb = 256,
                        memoriaDisponibleSistemaMb = 900,
                        bloquesReservados = 2
                    )
                ),
                puedeLiberar = true
            ),
            onReservar = {},
            onLiberar = {},
            onSolicitarGc = {},
            onReiniciar = {},
            onVolver = {}
        )
    }
}

// Preview de la pantalla en estado de advertencia por presion de memoria alta
@Preview(showBackground = true)
@Composable
private fun PantallaMonstruoMemoriaAdvertenciaPreview() {
    ArchitecturasOSTheme {
        ContenidoMonstruoMemoria(
            estado = EstadoMonstruoMemoria(
                estadoEjecucion = EstadoEjecucionMemoria.Advertencia,
                estadoVisual = EstadoVisualMonstruo.PresionMemoria,
                memoriaUsadaMb = 180,
                memoriaMaximaRuntimeMb = 256,
                memoriaLibreRuntimeMb = 20,
                memoriaReservadaMb = 170,
                bloquesReservados = 4,
                limiteSeguroMb = 200,
                mensajeAdvertencia = "Presion de memoria: 85% del limite seguro.",
                resultadoUltimaAccion = "Reservados 50 MB (bloque #4)",
                historico = listOf(
                    MuestraMemoria(
                        timestamp = System.currentTimeMillis(),
                        memoriaUsadaMb = 180,
                        memoriaLibreRuntimeMb = 20,
                        memoriaMaximaRuntimeMb = 256,
                        memoriaDisponibleSistemaMb = 512,
                        bloquesReservados = 4
                    )
                ),
                puedeLiberar = true
            ),
            onReservar = {},
            onLiberar = {},
            onSolicitarGc = {},
            onReiniciar = {},
            onVolver = {}
        )
    }
}

// Preview de la pantalla en estado de error por falta de memoria
@Preview(showBackground = true)
@Composable
private fun PantallaMonstruoMemoriaErrorPreview() {
    ArchitecturasOSTheme {
        ContenidoMonstruoMemoria(
            estado = EstadoMonstruoMemoria(
                estadoEjecucion = EstadoEjecucionMemoria.Error,
                limiteSeguroMb = 200,
                mensajeError = "OutOfMemoryError",
                resultadoUltimaAccion = "Error: memoria insuficiente"
            ),
            onReservar = {},
            onLiberar = {},
            onSolicitarGc = {},
            onReiniciar = {},
            onVolver = {}
        )
    }
}
