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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.yerdna.architecturasos.R
import io.yerdna.architecturasos.bancocaotico.BancoCaoticoViewModel
import io.yerdna.architecturasos.bancocaotico.CajeroBancoCaotico
import io.yerdna.architecturasos.bancocaotico.ConfiguracionBancoCaotico
import io.yerdna.architecturasos.bancocaotico.EstadoBancoCaotico
import io.yerdna.architecturasos.bancocaotico.EstadoCajeroBancoCaotico
import io.yerdna.architecturasos.bancocaotico.EventoRegistroBancoCaotico
import io.yerdna.architecturasos.bancocaotico.FaseBancoCaotico
import io.yerdna.architecturasos.bancocaotico.ResultadoBancoCaotico
import io.yerdna.architecturasos.bancocaotico.TAG_BANCO_CAOTICO
import io.yerdna.architecturasos.bancocaotico.cajerosIniciales
import io.yerdna.architecturasos.ui.component.BotonRegistroEventos
import io.yerdna.architecturasos.ui.component.ExperimentoScaffold
import io.yerdna.architecturasos.ui.component.HojaRegistroEventos
import io.yerdna.architecturasos.ui.component.rememberRegistroExperimento
import io.yerdna.architecturasos.ui.theme.ArchitecturasOSTheme
import kotlinx.coroutines.launch
import java.util.Locale

// Pantalla principal del experimento Banco Caotico: administra el viewmodel, el registro de eventos y la confirmacion de salida.
@Composable
fun PantallaBancoCaotico(
    onVolver: () -> Unit
) {
    val viewModel = remember { BancoCaoticoViewModel() }
    val registro = rememberRegistroExperimento(TAG_BANCO_CAOTICO)
    var mostrarConfirmacionSalida by remember { mutableStateOf(false) }
    val mensajeNoDisponible = stringResource(R.string.valor_no_disponible)
    val logConfiguracion = stringResource(R.string.log_banco_caotico_configuracion)
    val logEjecucionIniciada = stringResource(R.string.log_banco_caotico_ejecucion_iniciada)
    val logCajeroIniciado = stringResource(R.string.log_banco_caotico_cajero_iniciado)
    val logProgreso = stringResource(R.string.log_banco_caotico_progreso)
    val logCajeroFinalizado = stringResource(R.string.log_banco_caotico_cajero_finalizado)
    val logCajeroCancelado = stringResource(R.string.log_banco_caotico_cajero_cancelado)
    val logEjecucionCancelada = stringResource(R.string.log_banco_caotico_ejecucion_cancelada)
    val logError = stringResource(R.string.log_banco_caotico_error)
    val logResultado = stringResource(R.string.log_banco_caotico_resultado)
    val logCarreraDetectada = stringResource(R.string.log_banco_caotico_carrera_detectada)
    val logCarreraNoDetectada = stringResource(R.string.log_banco_caotico_carrera_no_detectada)
    val logSalidaConfirmada = stringResource(R.string.log_banco_caotico_salida_confirmada)

    // Traduce cada evento del experimento en una entrada legible dentro del registro de eventos.
    fun registrarEvento(evento: EventoRegistroBancoCaotico) {
        when (evento) {
            is EventoRegistroBancoCaotico.ConfiguracionAplicada -> registro.logger.info(
                logConfiguracion.formatear(evento.cantidadCajeros, evento.operacionesPorCajero)
            )

            is EventoRegistroBancoCaotico.EjecucionIniciada -> registro.logger.info(
                logEjecucionIniciada.formatear(evento.idEjecucion)
            )

            is EventoRegistroBancoCaotico.CajeroIniciado -> registro.logger.info(
                logCajeroIniciado.formatear(evento.idCajero)
            )

            is EventoRegistroBancoCaotico.ProgresoCajero -> registro.logger.info(
                logProgreso.formatear(evento.idCajero, evento.operacionesCompletadas)
            )

            is EventoRegistroBancoCaotico.CajeroFinalizado -> registro.logger.info(
                logCajeroFinalizado.formatear(evento.idCajero)
            )

            is EventoRegistroBancoCaotico.CajeroCancelado -> registro.logger.advertencia(
                logCajeroCancelado.formatear(evento.idCajero)
            )

            is EventoRegistroBancoCaotico.EjecucionFinalizada -> {
                val resultado = evento.resultado
                registro.logger.info(
                    logResultado.formatear(
                        resultado.resultadoEsperado,
                        resultado.resultadoReal,
                        resultado.diferencia
                    )
                )
                if (resultado.carreraDetectada) {
                    registro.logger.advertencia(logCarreraDetectada)
                } else {
                    registro.logger.info(logCarreraNoDetectada)
                }
            }

            EventoRegistroBancoCaotico.EjecucionCancelada -> registro.logger.advertencia(
                logEjecucionCancelada
            )

            is EventoRegistroBancoCaotico.ErrorTecnico -> registro.logger.error(
                logError.formatear(evento.mensaje ?: mensajeNoDisponible)
            )
        }
    }

    // Pide confirmacion antes de salir si hay una ejecucion activa, o vuelve directamente si no la hay.
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
        DialogoSalidaBancoCaotico(
            onConfirmar = {
                mostrarConfirmacionSalida = false
                registro.logger.advertencia(logSalidaConfirmada)
                viewModel.cancelar()
                viewModel.limpiarRecursos()
                onVolver()
            },
            onCancelar = { mostrarConfirmacionSalida = false }
        )
    }

    ContenidoBancoCaotico(
        estado = viewModel.estado,
        puedeIniciar = viewModel.puedeIniciar(),
        puedeCancelar = viewModel.puedeCancelar(),
        puedeReiniciar = viewModel.puedeReiniciar(),
        onCantidadCajeros = viewModel::actualizarCantidadCajeros,
        onOperacionesPorCajero = viewModel::actualizarOperacionesPorCajero,
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

// Arma el scaffold y la lista con todas las secciones de la pantalla del Banco Caotico.
@Composable
private fun ContenidoBancoCaotico(
    estado: EstadoBancoCaotico,
    puedeIniciar: Boolean,
    puedeCancelar: Boolean,
    puedeReiniciar: Boolean,
    onCantidadCajeros: (Int) -> Unit,
    onOperacionesPorCajero: (Int) -> Unit,
    onIniciar: () -> Unit,
    onCancelar: () -> Unit,
    onReiniciar: () -> Unit,
    onVolver: () -> Unit,
    acciones: @Composable RowScope.() -> Unit = {}
) {
    ExperimentoScaffold(
        titulo = stringResource(R.string.experimento_banco_caotico_nombre),
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
                    text = stringResource(R.string.experimento_banco_caotico_concepto),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                Text(
                    text = stringResource(R.string.banco_caotico_descripcion_corta),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                ControlesBancoCaotico(
                    configuracion = estado.configuracion,
                    habilitarConfiguracion = !estado.ejecucionActiva,
                    puedeIniciar = puedeIniciar,
                    puedeCancelar = puedeCancelar,
                    puedeReiniciar = puedeReiniciar,
                    onCantidadCajeros = onCantidadCajeros,
                    onOperacionesPorCajero = onOperacionesPorCajero,
                    onIniciar = onIniciar,
                    onCancelar = onCancelar,
                    onReiniciar = onReiniciar
                )
            }
            item {
                SeccionCarrera(titulo = stringResource(R.string.banco_caotico_cajeros)) {
                    estado.cajeros.forEach { cajero ->
                        FilaCajeroBancoCaotico(
                            cajero = cajero,
                            estadoTexto = textoEstadoCajero(cajero.estado),
                            colorEstado = colorEstadoCajero(cajero.estado)
                        )
                    }
                }
            }
            item {
                PanelMetricasBancoCaotico(
                    estado = estado,
                    faseTexto = textoFaseBancoCaotico(estado.fase)
                )
            }
            item {
                PanelResultadoBancoCaotico(resultado = estado.resultadoUltimaEjecucion)
            }
            item {
                ComoVerificarBancoCaotico()
            }
        }
    }
}

// Muestra los controles para configurar la cantidad de cajeros y operaciones, y los botones de iniciar/cancelar/reiniciar.
@Composable
private fun ControlesBancoCaotico(
    configuracion: ConfiguracionBancoCaotico,
    habilitarConfiguracion: Boolean,
    puedeIniciar: Boolean,
    puedeCancelar: Boolean,
    puedeReiniciar: Boolean,
    onCantidadCajeros: (Int) -> Unit,
    onOperacionesPorCajero: (Int) -> Unit,
    onIniciar: () -> Unit,
    onCancelar: () -> Unit,
    onReiniciar: () -> Unit
) {
    SeccionCarrera(titulo = stringResource(R.string.banco_caotico_controles)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onCantidadCajeros(configuracion.cantidadCajeros - 1) },
                enabled = habilitarConfiguracion &&
                    configuracion.cantidadCajeros > ConfiguracionBancoCaotico.MIN_CAJEROS
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.banco_caotico_disminuir_cajeros)
                )
            }
            Text(
                text = stringResource(
                    R.string.banco_caotico_cantidad_cajeros,
                    configuracion.cantidadCajeros
                ),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(
                onClick = { onCantidadCajeros(configuracion.cantidadCajeros + 1) },
                enabled = habilitarConfiguracion &&
                    configuracion.cantidadCajeros < ConfiguracionBancoCaotico.MAX_CAJEROS
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.banco_caotico_aumentar_cajeros)
                )
            }
        }

        OutlinedTextField(
            value = configuracion.operacionesPorCajero.toString(),
            onValueChange = { texto ->
                val valor = texto.filter { it.isDigit() }.toIntOrNull()
                if (valor != null) {
                    onOperacionesPorCajero(valor)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = habilitarConfiguracion,
            label = { Text(stringResource(R.string.banco_caotico_operaciones_por_cajero)) },
            supportingText = { Text(stringResource(R.string.banco_caotico_error_operaciones)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
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
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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

// Renderiza una tarjeta con el estado y los ultimos saldos leidos/escritos de un cajero.
@Composable
private fun FilaCajeroBancoCaotico(
    cajero: CajeroBancoCaotico,
    estadoTexto: String,
    colorEstado: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cajero.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = estadoTexto,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorEstado,
                    fontWeight = FontWeight.SemiBold
                )
            }
            DatoCarrera(
                etiqueta = stringResource(R.string.carrera_hilos_operaciones),
                valor = cajero.operacionesCompletadas.toString()
            )
            DatoCarrera(
                etiqueta = stringResource(R.string.banco_caotico_ultimo_saldo_leido),
                valor = cajero.ultimoSaldoLeido?.toString() ?: stringResource(R.string.valor_no_disponible)
            )
            DatoCarrera(
                etiqueta = stringResource(R.string.banco_caotico_ultimo_saldo_escrito),
                valor = cajero.ultimoSaldoEscrito?.toString() ?: stringResource(R.string.valor_no_disponible)
            )
        }
    }
}

// Muestra el panel con las metricas de configuracion y resultados de la ultima ejecucion del banco caotico.
@Composable
private fun PanelMetricasBancoCaotico(
    estado: EstadoBancoCaotico,
    faseTexto: String
) {
    val config = estado.configuracion
    val resultado = estado.resultadoUltimaEjecucion
    SeccionCarrera(titulo = stringResource(R.string.banco_caotico_metricas)) {
        DatoCarrera(
            etiqueta = stringResource(R.string.carrera_hilos_estado_general),
            valor = faseTexto
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.banco_caotico_saldo_inicial),
            valor = config.saldoInicial.toString()
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.banco_caotico_cajeros_total),
            valor = config.cantidadCajeros.toString()
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.banco_caotico_operaciones_por_cajero),
            valor = config.operacionesPorCajero.toString()
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.banco_caotico_operaciones_totales),
            valor = (config.cantidadCajeros * config.operacionesPorCajero).toString()
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.banco_caotico_resultado_esperado),
            valor = resultado?.resultadoEsperado?.toString() ?: stringResource(R.string.valor_no_disponible)
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.banco_caotico_resultado_real),
            valor = resultado?.resultadoReal?.toString() ?: stringResource(R.string.valor_no_disponible)
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.banco_caotico_diferencia),
            valor = resultado?.diferencia?.toString() ?: stringResource(R.string.valor_no_disponible)
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.banco_caotico_duracion),
            valor = resultado?.let {
                stringResource(R.string.banco_caotico_milisegundos, it.duracionMs)
            } ?: stringResource(R.string.valor_no_disponible)
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.banco_caotico_carrera_detectada),
            valor = resultado?.let {
                if (it.carreraDetectada) {
                    stringResource(R.string.banco_caotico_carrera_si)
                } else {
                    stringResource(R.string.banco_caotico_carrera_no)
                }
            } ?: stringResource(R.string.valor_no_disponible)
        )
    }
}

// Muestra si la ultima ejecucion detecto o no una condicion de carrera en el saldo.
@Composable
private fun PanelResultadoBancoCaotico(
    resultado: ResultadoBancoCaotico?
) {
    SeccionCarrera(titulo = stringResource(R.string.resultado_ultima_ejecucion)) {
        if (resultado == null) {
            Text(
                text = stringResource(R.string.resultado_sin_ejecucion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = if (resultado.carreraDetectada) {
                    stringResource(R.string.banco_caotico_resultado_detectado)
                } else {
                    stringResource(R.string.banco_caotico_resultado_no_detectado)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (resultado.carreraDetectada) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

// Lista los comandos adb para verificar externamente el comportamiento del banco caotico, con opcion de copiarlos.
@Composable
private fun ComoVerificarBancoCaotico() {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val comandos = listOf(
        ComandoVerificacionBancoCaotico(
            comando = "adb logcat -d -s OSPlayground/ChaosBank",
            descripcionResId = R.string.verificacion_logcat_banco_caotico
        ),
        ComandoVerificacionBancoCaotico(
            comando = "adb shell top",
            descripcionResId = R.string.verificacion_top_banco_caotico
        ),
        ComandoVerificacionBancoCaotico(
            comando = "adb shell dumpsys activity top",
            descripcionResId = R.string.verificacion_dumpsys_top_banco_caotico
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

private data class ComandoVerificacionBancoCaotico(
    val comando: String,
    val descripcionResId: Int
)

// Muestra el dialogo de confirmacion para salir cancelando la ejecucion en curso del banco caotico.
@Composable
private fun DialogoSalidaBancoCaotico(
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.dialogo_salir_banco_caotico_titulo)) },
        text = { Text(stringResource(R.string.dialogo_salir_banco_caotico_mensaje)) },
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

// Traduce la fase actual del banco caotico a un texto legible para el usuario.
@Composable
private fun textoFaseBancoCaotico(fase: FaseBancoCaotico): String {
    return when (fase) {
        FaseBancoCaotico.Inactivo -> stringResource(R.string.estado_inactivo)
        FaseBancoCaotico.Ejecutando -> stringResource(R.string.estado_ejecucion)
        FaseBancoCaotico.Exitoso -> stringResource(R.string.estado_exitoso)
        FaseBancoCaotico.Cancelado -> stringResource(R.string.resultado_cancelado)
        FaseBancoCaotico.Error -> stringResource(R.string.estado_error)
    }
}

// Traduce el estado individual de un cajero a un texto legible para el usuario.
@Composable
private fun textoEstadoCajero(estado: EstadoCajeroBancoCaotico): String {
    return when (estado) {
        EstadoCajeroBancoCaotico.Esperando -> stringResource(R.string.estado_esperando)
        EstadoCajeroBancoCaotico.Trabajando -> stringResource(R.string.estado_ejecucion)
        EstadoCajeroBancoCaotico.Finalizado -> stringResource(R.string.estado_finalizado)
        EstadoCajeroBancoCaotico.Cancelado -> stringResource(R.string.resultado_cancelado)
        EstadoCajeroBancoCaotico.Error -> stringResource(R.string.estado_error)
    }
}

// Elige el color asociado al estado actual de un cajero para resaltarlo visualmente.
@Composable
private fun colorEstadoCajero(estado: EstadoCajeroBancoCaotico): Color {
    return when (estado) {
        EstadoCajeroBancoCaotico.Trabajando -> MaterialTheme.colorScheme.tertiary
        EstadoCajeroBancoCaotico.Finalizado -> MaterialTheme.colorScheme.primary
        EstadoCajeroBancoCaotico.Error -> MaterialTheme.colorScheme.error
        EstadoCajeroBancoCaotico.Esperando,
        EstadoCajeroBancoCaotico.Cancelado -> MaterialTheme.colorScheme.outline
    }
}

// Formatea una cadena de recurso con los argumentos dados usando el locale por defecto.
private fun String.formatear(vararg argumentos: Any): String {
    return String.format(Locale.getDefault(), this, *argumentos)
}

// Vista previa de la pantalla del banco caotico en su estado inicial, sin ejecucion.
@Preview(showBackground = true)
@Composable
private fun PantallaBancoCaoticoInicialPreview() {
    ArchitecturasOSTheme {
        val config = ConfiguracionBancoCaotico()
        ContenidoBancoCaotico(
            estado = EstadoBancoCaotico(
                configuracion = config,
                cajeros = cajerosIniciales(config)
            ),
            puedeIniciar = true,
            puedeCancelar = false,
            puedeReiniciar = true,
            onCantidadCajeros = {},
            onOperacionesPorCajero = {},
            onIniciar = {},
            onCancelar = {},
            onReiniciar = {},
            onVolver = {}
        )
    }
}

// Vista previa de la pantalla del banco caotico mostrando un resultado con carrera detectada.
@Preview(showBackground = true)
@Composable
private fun PantallaBancoCaoticoResultadoPreview() {
    ArchitecturasOSTheme {
        val config = ConfiguracionBancoCaotico(cantidadCajeros = 4, operacionesPorCajero = 10_000)
        val resultado = ResultadoBancoCaotico(
            saldoInicial = 0,
            resultadoEsperado = 40_000,
            resultadoReal = 31_420,
            diferencia = 8_580,
            carreraDetectada = true,
            duracionMs = 640L,
            cajerosFinalizados = 4
        )
        ContenidoBancoCaotico(
            estado = EstadoBancoCaotico(
                fase = FaseBancoCaotico.Exitoso,
                configuracion = config,
                cajeros = cajerosIniciales(config).map {
                    it.copy(
                        estado = EstadoCajeroBancoCaotico.Finalizado,
                        operacionesCompletadas = config.operacionesPorCajero,
                        ultimoSaldoLeido = resultado.resultadoReal - 1,
                        ultimoSaldoEscrito = resultado.resultadoReal
                    )
                },
                resultadoUltimaEjecucion = resultado
            ),
            puedeIniciar = true,
            puedeCancelar = false,
            puedeReiniciar = true,
            onCantidadCajeros = {},
            onOperacionesPorCajero = {},
            onIniciar = {},
            onCancelar = {},
            onReiniciar = {},
            onVolver = {}
        )
    }
}
