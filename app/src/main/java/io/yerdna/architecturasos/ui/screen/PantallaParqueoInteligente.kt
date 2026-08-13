package io.yerdna.architecturasos.ui.screen

import android.content.ClipData
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.yerdna.architecturasos.R
import io.yerdna.architecturasos.parqueointeligente.ConfiguracionParqueoInteligente
import io.yerdna.architecturasos.parqueointeligente.EstadoParqueoInteligente
import io.yerdna.architecturasos.parqueointeligente.EstadoVehiculoParqueoInteligente
import io.yerdna.architecturasos.parqueointeligente.EventoRegistroParqueoInteligente
import io.yerdna.architecturasos.parqueointeligente.FaseParqueoInteligente
import io.yerdna.architecturasos.parqueointeligente.MetricasParqueoInteligente
import io.yerdna.architecturasos.parqueointeligente.ParqueoInteligenteViewModel
import io.yerdna.architecturasos.parqueointeligente.ResultadoParqueoInteligente
import io.yerdna.architecturasos.parqueointeligente.TAG_PARQUEO_INTELIGENTE
import io.yerdna.architecturasos.parqueointeligente.VehiculoParqueoInteligente
import io.yerdna.architecturasos.ui.component.BotonRegistroEventos
import io.yerdna.architecturasos.ui.component.ExperimentoScaffold
import io.yerdna.architecturasos.ui.component.HojaRegistroEventos
import io.yerdna.architecturasos.ui.component.rememberRegistroExperimento
import io.yerdna.architecturasos.ui.theme.ArchitecturasOSTheme
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun PantallaParqueoInteligente(
    onVolver: () -> Unit
) {
    val viewModel = remember { ParqueoInteligenteViewModel() }
    val registro = rememberRegistroExperimento(TAG_PARQUEO_INTELIGENTE)
    var mostrarConfirmacionSalida by remember { mutableStateOf(false) }
    val textoNoDisponible = stringResource(R.string.valor_no_disponible)
    val logConfiguracion = stringResource(R.string.log_parqueo_inteligente_configuracion)
    val logEjecucionIniciada = stringResource(R.string.log_parqueo_inteligente_ejecucion_iniciada)
    val logVehiculoEsperando = stringResource(R.string.log_parqueo_inteligente_vehiculo_esperando)
    val logPermisoAdquirido = stringResource(R.string.log_parqueo_inteligente_permiso_adquirido)
    val logVehiculoEstacionado = stringResource(R.string.log_parqueo_inteligente_vehiculo_estacionado)
    val logVehiculoSaliendo = stringResource(R.string.log_parqueo_inteligente_vehiculo_saliendo)
    val logPermisoLiberado = stringResource(R.string.log_parqueo_inteligente_permiso_liberado)
    val logVehiculoFinalizado = stringResource(R.string.log_parqueo_inteligente_vehiculo_finalizado)
    val logFinalizada = stringResource(R.string.log_parqueo_inteligente_finalizada)
    val logCancelada = stringResource(R.string.log_parqueo_inteligente_cancelada)
    val logSalidaConfirmada = stringResource(R.string.log_parqueo_inteligente_salida_confirmada)
    val logError = stringResource(R.string.log_parqueo_inteligente_error)

    fun registrarEvento(evento: EventoRegistroParqueoInteligente) {
        when (evento) {
            is EventoRegistroParqueoInteligente.ConfiguracionAplicada -> registro.logger.info(
                logConfiguracion.formatear(evento.espaciosDisponibles, evento.vehiculosTotales)
            )

            is EventoRegistroParqueoInteligente.EjecucionIniciada -> registro.logger.info(
                logEjecucionIniciada.formatear(evento.idEjecucion)
            )

            is EventoRegistroParqueoInteligente.VehiculoEsperando -> registro.logger.info(
                logVehiculoEsperando.formatear(evento.idVehiculo)
            )

            is EventoRegistroParqueoInteligente.PermisoAdquirido -> registro.logger.info(
                logPermisoAdquirido.formatear(evento.idVehiculo, evento.permisosDisponibles)
            )

            is EventoRegistroParqueoInteligente.VehiculoEstacionado -> registro.logger.info(
                logVehiculoEstacionado.formatear(evento.idVehiculo)
            )

            is EventoRegistroParqueoInteligente.VehiculoSaliendo -> registro.logger.info(
                logVehiculoSaliendo.formatear(evento.idVehiculo)
            )

            is EventoRegistroParqueoInteligente.PermisoLiberado -> registro.logger.info(
                logPermisoLiberado.formatear(evento.idVehiculo, evento.permisosDisponibles)
            )

            is EventoRegistroParqueoInteligente.VehiculoFinalizado -> registro.logger.info(
                logVehiculoFinalizado.formatear(evento.idVehiculo)
            )

            is EventoRegistroParqueoInteligente.EjecucionFinalizada -> registro.logger.info(
                logFinalizada.formatear(evento.resultado.duracionTotalMs)
            )

            EventoRegistroParqueoInteligente.EjecucionCancelada -> registro.logger.advertencia(logCancelada)
            EventoRegistroParqueoInteligente.SalidaConfirmada -> registro.logger.advertencia(logSalidaConfirmada)
            is EventoRegistroParqueoInteligente.ErrorTecnico -> registro.logger.error(
                logError.formatear(evento.mensaje ?: textoNoDisponible)
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
        DialogoSalidaParqueoInteligente(
            onConfirmar = {
                mostrarConfirmacionSalida = false
                registrarEvento(EventoRegistroParqueoInteligente.SalidaConfirmada)
                viewModel.cancelar()
                registrarEvento(EventoRegistroParqueoInteligente.EjecucionCancelada)
                viewModel.limpiarRecursos()
                onVolver()
            },
            onCancelar = { mostrarConfirmacionSalida = false }
        )
    }

    ContenidoParqueoInteligente(
        estado = viewModel.estado,
        puedeIniciar = viewModel.puedeIniciar(),
        puedeCancelar = viewModel.puedeCancelar(),
        puedeReiniciar = viewModel.puedeReiniciar(),
        onEspacios = viewModel::actualizarEspaciosDisponibles,
        onVehiculos = viewModel::actualizarVehiculosTotales,
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
private fun ContenidoParqueoInteligente(
    estado: EstadoParqueoInteligente,
    puedeIniciar: Boolean,
    puedeCancelar: Boolean,
    puedeReiniciar: Boolean,
    onEspacios: (Int) -> Unit,
    onVehiculos: (Int) -> Unit,
    onIniciar: () -> Unit,
    onCancelar: () -> Unit,
    onReiniciar: () -> Unit,
    onVolver: () -> Unit,
    acciones: @Composable RowScope.() -> Unit = {}
) {
    ExperimentoScaffold(
        titulo = stringResource(R.string.experimento_parqueo_inteligente_nombre),
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
                    text = stringResource(R.string.experimento_parqueo_inteligente_concepto),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                Text(
                    text = stringResource(R.string.parqueo_inteligente_descripcion_corta),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Text(
                    text = stringResource(R.string.parqueo_inteligente_explicacion_mutex_semaforo),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                ControlesParqueoInteligente(
                    configuracion = estado.configuracion,
                    habilitarConfiguracion = !estado.ejecucionActiva,
                    puedeIniciar = puedeIniciar,
                    puedeCancelar = puedeCancelar,
                    puedeReiniciar = puedeReiniciar,
                    onEspacios = onEspacios,
                    onVehiculos = onVehiculos,
                    onIniciar = onIniciar,
                    onCancelar = onCancelar,
                    onReiniciar = onReiniciar
                )
            }
            item {
                PanelEstacionamiento(metricas = estado.metricas)
            }
            item {
                ListaVehiculosParqueoInteligente(vehiculos = estado.vehiculos)
            }
            item {
                PanelMetricasParqueoInteligente(
                    fase = estado.fase,
                    metricas = estado.metricas,
                    mensajeError = estado.mensajeError
                )
            }
            item {
                PanelResultadoParqueoInteligente(resultado = estado.resultadoUltimaEjecucion)
            }
            item {
                ComoVerificarParqueoInteligente()
            }
        }
    }
}

@Composable
private fun ControlesParqueoInteligente(
    configuracion: ConfiguracionParqueoInteligente,
    habilitarConfiguracion: Boolean,
    puedeIniciar: Boolean,
    puedeCancelar: Boolean,
    puedeReiniciar: Boolean,
    onEspacios: (Int) -> Unit,
    onVehiculos: (Int) -> Unit,
    onIniciar: () -> Unit,
    onCancelar: () -> Unit,
    onReiniciar: () -> Unit
) {
    SeccionCarrera(titulo = stringResource(R.string.parqueo_inteligente_controles)) {
        ControlNumeroParqueoInteligente(
            etiqueta = stringResource(R.string.parqueo_inteligente_espacios),
            valor = configuracion.espaciosDisponibles,
            minimo = ConfiguracionParqueoInteligente.MIN_ESPACIOS,
            maximo = ConfiguracionParqueoInteligente.MAX_ESPACIOS,
            habilitado = habilitarConfiguracion,
            onCambiar = onEspacios
        )
        ControlNumeroParqueoInteligente(
            etiqueta = stringResource(R.string.parqueo_inteligente_vehiculos),
            valor = configuracion.vehiculosTotales,
            minimo = ConfiguracionParqueoInteligente.MIN_VEHICULOS,
            maximo = ConfiguracionParqueoInteligente.MAX_VEHICULOS,
            habilitado = habilitarConfiguracion,
            onCambiar = onVehiculos
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
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
private fun ControlNumeroParqueoInteligente(
    etiqueta: String,
    valor: Int,
    minimo: Int,
    maximo: Int,
    habilitado: Boolean,
    onCambiar: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = etiqueta,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onCambiar(valor - 1) },
                enabled = habilitado && valor > minimo
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = stringResource(R.string.accion_disminuir)
                )
            }
            Text(
                text = valor.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = { onCambiar(valor + 1) },
                enabled = habilitado && valor < maximo
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.accion_aumentar)
                )
            }
        }
    }
}

@Composable
private fun PanelEstacionamiento(
    metricas: MetricasParqueoInteligente
) {
    SeccionCarrera(titulo = stringResource(R.string.parqueo_inteligente_estacionamiento)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(metricas.permisosTotales) { indice ->
                val ocupado = indice < metricas.recursosOcupados
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (ocupado) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                )
            }
        }
        DatoCarrera(
            etiqueta = stringResource(R.string.parqueo_inteligente_recursos_ocupados),
            valor = "${metricas.recursosOcupados}/${metricas.permisosTotales}"
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.parqueo_inteligente_hilos_esperando),
            valor = metricas.hilosEsperando.toString()
        )
    }
}

@Composable
private fun ListaVehiculosParqueoInteligente(
    vehiculos: List<VehiculoParqueoInteligente>
) {
    SeccionCarrera(titulo = stringResource(R.string.parqueo_inteligente_vehiculos_estado)) {
        vehiculos.forEach { vehiculo ->
            FilaVehiculoParqueoInteligente(vehiculo = vehiculo)
        }
    }
}

@Composable
private fun FilaVehiculoParqueoInteligente(
    vehiculo: VehiculoParqueoInteligente
) {
    val color = colorEstadoVehiculo(vehiculo.estado)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(color, RoundedCornerShape(6.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.parqueo_inteligente_vehiculo_numero, vehiculo.id),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = textoEstadoVehiculo(vehiculo.estado),
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PanelMetricasParqueoInteligente(
    fase: FaseParqueoInteligente,
    metricas: MetricasParqueoInteligente,
    mensajeError: String?
) {
    SeccionCarrera(titulo = stringResource(R.string.parqueo_inteligente_metricas)) {
        DatoCarrera(
            etiqueta = stringResource(R.string.parqueo_inteligente_estado_general),
            valor = textoFaseParqueoInteligente(fase)
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.parqueo_inteligente_permisos_totales),
            valor = metricas.permisosTotales.toString()
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.parqueo_inteligente_permisos_disponibles),
            valor = metricas.permisosDisponibles.toString()
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.parqueo_inteligente_vehiculos_finalizados),
            valor = metricas.vehiculosFinalizados.toString()
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.parqueo_inteligente_maximo_simultaneo),
            valor = metricas.maximoVehiculosSimultaneos.toString()
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.parqueo_inteligente_tiempo_total),
            valor = stringResource(R.string.parqueo_inteligente_milisegundos, metricas.tiempoTotalMs)
        )
        if (mensajeError != null) {
            Text(
                text = mensajeError,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun PanelResultadoParqueoInteligente(
    resultado: ResultadoParqueoInteligente?
) {
    SeccionCarrera(titulo = stringResource(R.string.resultado_ultima_ejecucion)) {
        if (resultado == null) {
            Text(
                text = stringResource(R.string.resultado_sin_ejecucion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            DatoCarrera(
                etiqueta = stringResource(R.string.parqueo_inteligente_permisos_totales),
                valor = resultado.permisosTotales.toString()
            )
            DatoCarrera(
                etiqueta = stringResource(R.string.parqueo_inteligente_vehiculos_totales),
                valor = resultado.vehiculosTotales.toString()
            )
            DatoCarrera(
                etiqueta = stringResource(R.string.parqueo_inteligente_vehiculos_finalizados),
                valor = resultado.vehiculosFinalizados.toString()
            )
            DatoCarrera(
                etiqueta = stringResource(R.string.parqueo_inteligente_vehiculos_cancelados),
                valor = resultado.vehiculosCancelados.toString()
            )
            DatoCarrera(
                etiqueta = stringResource(R.string.parqueo_inteligente_vehiculos_con_error),
                valor = resultado.vehiculosConError.toString()
            )
            DatoCarrera(
                etiqueta = stringResource(R.string.parqueo_inteligente_maximo_simultaneo),
                valor = resultado.maximoVehiculosSimultaneos.toString()
            )
            DatoCarrera(
                etiqueta = stringResource(R.string.parqueo_inteligente_tiempo_total),
                valor = stringResource(R.string.parqueo_inteligente_milisegundos, resultado.duracionTotalMs)
            )
        }
    }
}

@Composable
private fun ComoVerificarParqueoInteligente() {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val comandos = listOf(
        ComandoVerificacionParqueoInteligente(
            comando = "adb logcat -d -s OSPlayground/Semaphore",
            descripcionResId = R.string.verificacion_logcat_parqueo_inteligente
        ),
        ComandoVerificacionParqueoInteligente(
            comando = "adb shell top",
            descripcionResId = R.string.verificacion_top_parqueo_inteligente
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

private data class ComandoVerificacionParqueoInteligente(
    val comando: String,
    val descripcionResId: Int
)

@Composable
private fun DialogoSalidaParqueoInteligente(
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.dialogo_salir_parqueo_inteligente_titulo)) },
        text = { Text(stringResource(R.string.dialogo_salir_parqueo_inteligente_mensaje)) },
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
private fun textoFaseParqueoInteligente(fase: FaseParqueoInteligente): String {
    return when (fase) {
        FaseParqueoInteligente.Inactivo -> stringResource(R.string.estado_inactivo)
        FaseParqueoInteligente.Ejecutando -> stringResource(R.string.estado_ejecucion)
        FaseParqueoInteligente.Exitoso -> stringResource(R.string.estado_exitoso)
        FaseParqueoInteligente.Cancelado -> stringResource(R.string.resultado_cancelado)
        FaseParqueoInteligente.Error -> stringResource(R.string.estado_error)
    }
}

@Composable
private fun textoEstadoVehiculo(estado: EstadoVehiculoParqueoInteligente): String {
    return when (estado) {
        EstadoVehiculoParqueoInteligente.Esperando -> stringResource(R.string.estado_esperando)
        EstadoVehiculoParqueoInteligente.Entrando -> stringResource(R.string.estado_entrando)
        EstadoVehiculoParqueoInteligente.Estacionado -> stringResource(R.string.estado_estacionado)
        EstadoVehiculoParqueoInteligente.Saliendo -> stringResource(R.string.estado_saliendo)
        EstadoVehiculoParqueoInteligente.Finalizado -> stringResource(R.string.estado_finalizado)
        EstadoVehiculoParqueoInteligente.Cancelado -> stringResource(R.string.resultado_cancelado)
        EstadoVehiculoParqueoInteligente.Error -> stringResource(R.string.estado_error)
    }
}

@Composable
private fun colorEstadoVehiculo(estado: EstadoVehiculoParqueoInteligente): Color {
    return when (estado) {
        EstadoVehiculoParqueoInteligente.Estacionado -> MaterialTheme.colorScheme.tertiary
        EstadoVehiculoParqueoInteligente.Finalizado -> MaterialTheme.colorScheme.primary
        EstadoVehiculoParqueoInteligente.Error -> MaterialTheme.colorScheme.error
        EstadoVehiculoParqueoInteligente.Entrando,
        EstadoVehiculoParqueoInteligente.Saliendo -> MaterialTheme.colorScheme.secondary

        EstadoVehiculoParqueoInteligente.Esperando,
        EstadoVehiculoParqueoInteligente.Cancelado -> MaterialTheme.colorScheme.outline
    }
}

private fun String.formatear(vararg argumentos: Any): String {
    return String.format(Locale.getDefault(), this, *argumentos)
}

@Preview(showBackground = true)
@Composable
private fun PantallaParqueoInteligenteInicialPreview() {
    ArchitecturasOSTheme {
        ContenidoParqueoInteligente(
            estado = EstadoParqueoInteligente(),
            puedeIniciar = true,
            puedeCancelar = false,
            puedeReiniciar = true,
            onEspacios = {},
            onVehiculos = {},
            onIniciar = {},
            onCancelar = {},
            onReiniciar = {},
            onVolver = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaParqueoInteligenteEjecutandoPreview() {
    val vehiculos = (1..8).map { id ->
        VehiculoParqueoInteligente(
            id = id,
            nombre = "Vehiculo $id",
            estado = if (id <= 3) {
                EstadoVehiculoParqueoInteligente.Estacionado
            } else {
                EstadoVehiculoParqueoInteligente.Esperando
            }
        )
    }
    ArchitecturasOSTheme {
        ContenidoParqueoInteligente(
            estado = EstadoParqueoInteligente(
                fase = FaseParqueoInteligente.Ejecutando,
                vehiculos = vehiculos,
                metricas = MetricasParqueoInteligente(
                    permisosTotales = 3,
                    permisosDisponibles = 0,
                    recursosOcupados = 3,
                    hilosEsperando = 5
                ),
                ejecucionActiva = true
            ),
            puedeIniciar = false,
            puedeCancelar = true,
            puedeReiniciar = false,
            onEspacios = {},
            onVehiculos = {},
            onIniciar = {},
            onCancelar = {},
            onReiniciar = {},
            onVolver = {}
        )
    }
}
