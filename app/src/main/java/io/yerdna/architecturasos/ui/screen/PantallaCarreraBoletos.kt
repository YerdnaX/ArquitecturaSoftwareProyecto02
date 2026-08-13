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
import io.yerdna.architecturasos.carreraboletos.CompradorBoleto
import io.yerdna.architecturasos.carreraboletos.ConfiguracionCarreraBoletos
import io.yerdna.architecturasos.carreraboletos.EstadoCarreraBoletos
import io.yerdna.architecturasos.carreraboletos.EstadoCompradorBoleto
import io.yerdna.architecturasos.carreraboletos.EventoRegistroCarreraBoletos
import io.yerdna.architecturasos.carreraboletos.FaseCarreraBoletos
import io.yerdna.architecturasos.carreraboletos.MetricasCarreraBoletos
import io.yerdna.architecturasos.carreraboletos.ModoCarreraBoletos
import io.yerdna.architecturasos.carreraboletos.ResultadoCarreraBoletos
import io.yerdna.architecturasos.carreraboletos.TAG_CARRERA_BOLETOS
import io.yerdna.architecturasos.carreraboletos.CarreraBoletosViewModel
import io.yerdna.architecturasos.ui.component.BotonRegistroEventos
import io.yerdna.architecturasos.ui.component.ExperimentoScaffold
import io.yerdna.architecturasos.ui.component.HojaRegistroEventos
import io.yerdna.architecturasos.ui.component.rememberRegistroExperimento
import io.yerdna.architecturasos.ui.theme.ArchitecturasOSTheme
import kotlinx.coroutines.launch
import java.util.Locale

// Pantalla principal del experimento de carrera de boletos, arma el viewModel, el registro de eventos y el flujo de salida
@Composable
fun PantallaCarreraBoletos(
    onVolver: () -> Unit
) {
    val viewModel = remember { CarreraBoletosViewModel() }
    val registro = rememberRegistroExperimento(TAG_CARRERA_BOLETOS)
    var mostrarConfirmacionSalida by remember { mutableStateOf(false) }
    val textoNoDisponible = stringResource(R.string.valor_no_disponible)
    val logConfiguracion = stringResource(R.string.log_carrera_boletos_configuracion)
    val logEjecucionIniciada = stringResource(R.string.log_carrera_boletos_ejecucion_iniciada)
    val logModo = stringResource(R.string.log_carrera_boletos_modo)
    val logIntento = stringResource(R.string.log_carrera_boletos_intento)
    val logEsperaMutex = stringResource(R.string.log_carrera_boletos_espera_mutex)
    val logMutexAdquirido = stringResource(R.string.log_carrera_boletos_mutex_adquirido)
    val logVenta = stringResource(R.string.log_carrera_boletos_venta)
    val logVentaIncorrecta = stringResource(R.string.log_carrera_boletos_venta_incorrecta)
    val logMutexLiberado = stringResource(R.string.log_carrera_boletos_mutex_liberado)
    val logFinalizada = stringResource(R.string.log_carrera_boletos_finalizada)
    val logCancelada = stringResource(R.string.log_carrera_boletos_cancelada)
    val logSalidaConfirmada = stringResource(R.string.log_carrera_boletos_salida_confirmada)
    val logError = stringResource(R.string.log_carrera_boletos_error)
    val modoSinMutex = stringResource(R.string.modo_sin_mutex)
    val modoConMutex = stringResource(R.string.modo_con_mutex)

    // Traduce el modo de ejecucion al texto que se muestra en el log de eventos
    fun textoModoLog(modo: ModoCarreraBoletos): String {
        return when (modo) {
            ModoCarreraBoletos.SinMutex -> modoSinMutex
            ModoCarreraBoletos.ConMutex -> modoConMutex
        }
    }

    // Convierte cada evento del experimento en una linea de log con el mensaje correspondiente
    fun registrarEvento(evento: EventoRegistroCarreraBoletos) {
        when (evento) {
            is EventoRegistroCarreraBoletos.ConfiguracionAplicada -> registro.logger.info(
                logConfiguracion.formatear(evento.boletosIniciales, evento.compradoresTotales)
            )

            is EventoRegistroCarreraBoletos.EjecucionIniciada -> registro.logger.info(
                logEjecucionIniciada.formatear(evento.idEjecucion)
            )

            is EventoRegistroCarreraBoletos.ModoSeleccionado -> registro.logger.info(
                logModo.formatear(textoModoLog(evento.modo))
            )

            is EventoRegistroCarreraBoletos.CompradorIntentandoEntrar -> registro.logger.info(
                logIntento.formatear(evento.idComprador)
            )

            is EventoRegistroCarreraBoletos.CompradorEsperandoMutex -> registro.logger.info(
                logEsperaMutex.formatear(evento.idComprador)
            )

            is EventoRegistroCarreraBoletos.MutexAdquirido -> registro.logger.info(
                logMutexAdquirido.formatear(evento.idComprador)
            )

            is EventoRegistroCarreraBoletos.VentaRealizada -> registro.logger.info(
                logVenta.formatear(evento.idComprador, evento.boletosRestantes)
            )

            is EventoRegistroCarreraBoletos.VentaIncorrectaDetectada -> registro.logger.advertencia(
                logVentaIncorrecta.formatear(evento.idComprador, evento.ventasRegistradas)
            )

            is EventoRegistroCarreraBoletos.MutexLiberado -> registro.logger.info(
                logMutexLiberado.formatear(evento.idComprador)
            )

            is EventoRegistroCarreraBoletos.EjecucionFinalizada -> registro.logger.info(
                logFinalizada.formatear(evento.resultado.tiempoTotalMs)
            )

            EventoRegistroCarreraBoletos.EjecucionCancelada -> registro.logger.advertencia(logCancelada)
            EventoRegistroCarreraBoletos.SalidaConfirmada -> registro.logger.advertencia(logSalidaConfirmada)
            is EventoRegistroCarreraBoletos.ErrorTecnico -> registro.logger.error(
                logError.formatear(evento.mensaje ?: textoNoDisponible)
            )
        }
    }

    // Decide si hay que pedir confirmacion antes de salir o si se puede volver de inmediato
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
        DialogoSalidaCarreraBoletos(
            onConfirmar = {
                mostrarConfirmacionSalida = false
                registrarEvento(EventoRegistroCarreraBoletos.SalidaConfirmada)
                viewModel.cancelar()
                registrarEvento(EventoRegistroCarreraBoletos.EjecucionCancelada)
                viewModel.limpiarRecursos()
                onVolver()
            },
            onCancelar = { mostrarConfirmacionSalida = false }
        )
    }

    ContenidoCarreraBoletos(
        estado = viewModel.estado,
        puedeIniciar = viewModel.puedeIniciar(),
        puedeCancelar = viewModel.puedeCancelar(),
        puedeReiniciar = viewModel.puedeReiniciar(),
        onBoletos = viewModel::actualizarBoletosIniciales,
        onCompradores = viewModel::actualizarCompradoresTotales,
        onIniciarSinMutex = {
            registro.limpiar()
            viewModel.iniciarSinMutex(::registrarEvento)
        },
        onIniciarConMutex = {
            registro.limpiar()
            viewModel.iniciarConMutex(::registrarEvento)
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

// Arma el scaffold del experimento y ordena en una lista todas las secciones de la pantalla
@Composable
private fun ContenidoCarreraBoletos(
    estado: EstadoCarreraBoletos,
    puedeIniciar: Boolean,
    puedeCancelar: Boolean,
    puedeReiniciar: Boolean,
    onBoletos: (Int) -> Unit,
    onCompradores: (Int) -> Unit,
    onIniciarSinMutex: () -> Unit,
    onIniciarConMutex: () -> Unit,
    onCancelar: () -> Unit,
    onReiniciar: () -> Unit,
    onVolver: () -> Unit,
    acciones: @Composable RowScope.() -> Unit = {}
) {
    ExperimentoScaffold(
        titulo = stringResource(R.string.experimento_carrera_boletos_nombre),
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
                    text = stringResource(R.string.experimento_carrera_boletos_concepto),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                Text(
                    text = stringResource(R.string.carrera_boletos_descripcion_corta),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                ControlesCarreraBoletos(
                    configuracion = estado.configuracion,
                    habilitarConfiguracion = !estado.ejecucionActiva,
                    puedeIniciar = puedeIniciar,
                    puedeCancelar = puedeCancelar,
                    puedeReiniciar = puedeReiniciar,
                    onBoletos = onBoletos,
                    onCompradores = onCompradores,
                    onIniciarSinMutex = onIniciarSinMutex,
                    onIniciarConMutex = onIniciarConMutex,
                    onCancelar = onCancelar,
                    onReiniciar = onReiniciar
                )
            }
            item {
                PanelMutexCarreraBoletos(
                    fase = estado.fase,
                    metricas = estado.metricas
                )
            }
            item {
                ListaCompradoresCarreraBoletos(compradores = estado.compradores)
            }
            item {
                PanelMetricasCarreraBoletos(
                    fase = estado.fase,
                    metricas = estado.metricas,
                    mensajeError = estado.mensajeError
                )
            }
            item {
                ComparacionResultadosCarreraBoletos(
                    resultadoSinMutex = estado.resultadoSinMutex,
                    resultadoConMutex = estado.resultadoConMutex
                )
            }
            item {
                ComoVerificarCarreraBoletos()
            }
        }
    }
}

// Muestra los controles de configuracion (boletos, compradores) y los botones para iniciar, cancelar o reiniciar la carrera
@Composable
private fun ControlesCarreraBoletos(
    configuracion: ConfiguracionCarreraBoletos,
    habilitarConfiguracion: Boolean,
    puedeIniciar: Boolean,
    puedeCancelar: Boolean,
    puedeReiniciar: Boolean,
    onBoletos: (Int) -> Unit,
    onCompradores: (Int) -> Unit,
    onIniciarSinMutex: () -> Unit,
    onIniciarConMutex: () -> Unit,
    onCancelar: () -> Unit,
    onReiniciar: () -> Unit
) {
    SeccionCarrera(titulo = stringResource(R.string.carrera_boletos_controles)) {
        ControlNumeroCarreraBoletos(
            etiqueta = stringResource(R.string.carrera_boletos_boletos),
            valor = configuracion.boletosIniciales,
            minimo = ConfiguracionCarreraBoletos.MIN_BOLETOS,
            maximo = ConfiguracionCarreraBoletos.MAX_BOLETOS,
            habilitado = habilitarConfiguracion,
            onCambiar = onBoletos
        )
        ControlNumeroCarreraBoletos(
            etiqueta = stringResource(R.string.carrera_boletos_compradores),
            valor = configuracion.compradoresTotales,
            minimo = ConfiguracionCarreraBoletos.MIN_COMPRADORES,
            maximo = ConfiguracionCarreraBoletos.MAX_COMPRADORES,
            habilitado = habilitarConfiguracion,
            onCambiar = onCompradores
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onIniciarSinMutex,
                enabled = puedeIniciar
            ) {
                Text(stringResource(R.string.accion_ejecutar_sin_mutex))
            }
            Button(
                onClick = onIniciarConMutex,
                enabled = puedeIniciar
            ) {
                Text(stringResource(R.string.accion_ejecutar_con_mutex))
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

// Dibuja una fila con etiqueta y botones de mas/menos para ajustar un valor numerico de configuracion
@Composable
private fun ControlNumeroCarreraBoletos(
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

// Muestra la tarjeta de la seccion critica con el estado general, el modo y quien tiene el mutex en ese momento
@Composable
private fun PanelMutexCarreraBoletos(
    fase: FaseCarreraBoletos,
    metricas: MetricasCarreraBoletos
) {
    SeccionCarrera(titulo = stringResource(R.string.carrera_boletos_seccion_critica)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DatoCarrera(
                    etiqueta = stringResource(R.string.carrera_boletos_estado_general),
                    valor = textoFaseCarreraBoletos(fase)
                )
                DatoCarrera(
                    etiqueta = stringResource(R.string.carrera_boletos_modo_actual),
                    valor = metricas.modo?.let { textoModoCarreraBoletos(it) }
                        ?: stringResource(R.string.valor_no_disponible)
                )
                DatoCarrera(
                    etiqueta = stringResource(R.string.carrera_boletos_comprador_en_seccion),
                    valor = textoCompradorEnSeccion(metricas.compradorEnSeccionCritica)
                )
                DatoCarrera(
                    etiqueta = stringResource(R.string.carrera_boletos_compradores_esperando),
                    valor = metricas.compradoresEsperando.toString()
                )
            }
        }
    }
}

// Recorre la lista de compradores y dibuja una fila de estado por cada uno
@Composable
private fun ListaCompradoresCarreraBoletos(
    compradores: List<CompradorBoleto>
) {
    SeccionCarrera(titulo = stringResource(R.string.carrera_boletos_compradores_estado)) {
        compradores.forEach { comprador ->
            FilaCompradorBoleto(comprador = comprador)
        }
    }
}

// Dibuja la tarjeta de un comprador individual con su color de estado, nombre y boletos comprados
@Composable
private fun FilaCompradorBoleto(
    comprador: CompradorBoleto
) {
    val color = colorEstadoComprador(comprador.estado)
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
                    text = stringResource(R.string.carrera_boletos_comprador_numero, comprador.id),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = textoEstadoComprador(comprador.estado),
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = stringResource(R.string.carrera_boletos_boletos_comprados, comprador.boletosComprados),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// Muestra la tarjeta con las metricas de la ejecucion actual y el mensaje de error si algo fallo
@Composable
private fun PanelMetricasCarreraBoletos(
    fase: FaseCarreraBoletos,
    metricas: MetricasCarreraBoletos,
    mensajeError: String?
) {
    SeccionCarrera(titulo = stringResource(R.string.carrera_boletos_metricas)) {
        DatoCarrera(
            etiqueta = stringResource(R.string.carrera_boletos_estado_general),
            valor = textoFaseCarreraBoletos(fase)
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.carrera_boletos_boletos_iniciales),
            valor = metricas.boletosIniciales.toString()
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.carrera_boletos_boletos_restantes),
            valor = metricas.boletosRestantes.toString()
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.carrera_boletos_ventas_correctas),
            valor = metricas.ventasCorrectas.toString()
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.carrera_boletos_ventas_incorrectas),
            valor = metricas.ventasIncorrectas.toString()
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.carrera_boletos_tiempo_total),
            valor = stringResource(R.string.carrera_boletos_milisegundos, metricas.tiempoTotalMs)
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

// Muestra lado a lado los resultados finales de la corrida sin mutex y con mutex para compararlos
@Composable
private fun ComparacionResultadosCarreraBoletos(
    resultadoSinMutex: ResultadoCarreraBoletos?,
    resultadoConMutex: ResultadoCarreraBoletos?
) {
    SeccionCarrera(titulo = stringResource(R.string.carrera_boletos_comparacion)) {
        ResultadoModoCarreraBoletos(
            titulo = stringResource(R.string.modo_sin_mutex),
            resultado = resultadoSinMutex
        )
        ResultadoModoCarreraBoletos(
            titulo = stringResource(R.string.modo_con_mutex),
            resultado = resultadoConMutex
        )
    }
}

// Dibuja la tarjeta con el resultado de un modo especifico (sin o con mutex), o un mensaje si aun no se ha ejecutado
@Composable
private fun ResultadoModoCarreraBoletos(
    titulo: String,
    resultado: ResultadoCarreraBoletos?
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (resultado == null) {
                Text(
                    text = stringResource(R.string.resultado_sin_ejecucion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                DatoCarrera(
                    etiqueta = stringResource(R.string.carrera_boletos_ventas_registradas),
                    valor = resultado.ventasRegistradas.toString()
                )
                DatoCarrera(
                    etiqueta = stringResource(R.string.carrera_boletos_boletos_restantes),
                    valor = resultado.boletosRestantes.toString()
                )
                DatoCarrera(
                    etiqueta = stringResource(R.string.carrera_boletos_ventas_incorrectas),
                    valor = resultado.ventasIncorrectas.toString()
                )
                DatoCarrera(
                    etiqueta = stringResource(R.string.carrera_boletos_error_concurrencia),
                    valor = if (resultado.huboErrorDeConcurrencia) {
                        stringResource(R.string.banco_caotico_carrera_si)
                    } else {
                        stringResource(R.string.banco_caotico_carrera_no)
                    }
                )
                DatoCarrera(
                    etiqueta = stringResource(R.string.carrera_boletos_tiempo_total),
                    valor = stringResource(R.string.carrera_boletos_milisegundos, resultado.tiempoTotalMs)
                )
            }
        }
    }
}

// Muestra los comandos adb sugeridos para verificar el experimento desde la terminal, con boton para copiarlos
@Composable
private fun ComoVerificarCarreraBoletos() {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val comandos = listOf(
        ComandoVerificacionCarreraBoletos(
            comando = "adb logcat -d -s OSPlayground/Mutex",
            descripcionResId = R.string.verificacion_logcat_carrera_boletos
        ),
        ComandoVerificacionCarreraBoletos(
            comando = "adb shell top",
            descripcionResId = R.string.verificacion_top_carrera_boletos
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

private data class ComandoVerificacionCarreraBoletos(
    val comando: String,
    val descripcionResId: Int
)

// Muestra el dialogo de confirmacion para salir de la pantalla cancelando la ejecucion en curso
@Composable
private fun DialogoSalidaCarreraBoletos(
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.dialogo_salir_carrera_boletos_titulo)) },
        text = { Text(stringResource(R.string.dialogo_salir_carrera_boletos_mensaje)) },
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

// Traduce la fase actual de la carrera al texto que se muestra en pantalla
@Composable
private fun textoFaseCarreraBoletos(fase: FaseCarreraBoletos): String {
    return when (fase) {
        FaseCarreraBoletos.Inactivo -> stringResource(R.string.estado_inactivo)
        FaseCarreraBoletos.Preparando -> stringResource(R.string.estado_preparando)
        FaseCarreraBoletos.EjecutandoSinMutex -> stringResource(R.string.estado_ejecutando_sin_mutex)
        FaseCarreraBoletos.EjecutandoConMutex -> stringResource(R.string.estado_ejecutando_con_mutex)
        FaseCarreraBoletos.Exitoso -> stringResource(R.string.estado_exitoso)
        FaseCarreraBoletos.Cancelado -> stringResource(R.string.resultado_cancelado)
        FaseCarreraBoletos.Error -> stringResource(R.string.estado_error)
    }
}

// Traduce el estado de un comprador al texto que se muestra en su fila
@Composable
private fun textoEstadoComprador(estado: EstadoCompradorBoleto): String {
    return when (estado) {
        EstadoCompradorBoleto.Esperando -> stringResource(R.string.estado_esperando)
        EstadoCompradorBoleto.IntentandoEntrar -> stringResource(R.string.estado_intentando_entrar)
        EstadoCompradorBoleto.EsperandoMutex -> stringResource(R.string.estado_esperando_mutex)
        EstadoCompradorBoleto.EnSeccionCritica -> stringResource(R.string.estado_en_seccion_critica)
        EstadoCompradorBoleto.Compro -> stringResource(R.string.estado_compro)
        EstadoCompradorBoleto.SinBoleto -> stringResource(R.string.estado_sin_boleto)
        EstadoCompradorBoleto.Finalizado -> stringResource(R.string.estado_finalizado)
        EstadoCompradorBoleto.Cancelado -> stringResource(R.string.resultado_cancelado)
        EstadoCompradorBoleto.Error -> stringResource(R.string.estado_error)
    }
}

// Convierte el id del comprador que esta en la seccion critica en el texto a mostrar, o "no disponible" si no hay ninguno
@Composable
private fun textoCompradorEnSeccion(valor: String?): String {
    val idComprador = valor?.toIntOrNull()
    return if (idComprador == null) {
        stringResource(R.string.valor_no_disponible)
    } else {
        stringResource(R.string.carrera_boletos_comprador_numero, idComprador)
    }
}

// Traduce el modo de ejecucion (sin o con mutex) al texto que se muestra en pantalla
@Composable
private fun textoModoCarreraBoletos(modo: ModoCarreraBoletos): String {
    return when (modo) {
        ModoCarreraBoletos.SinMutex -> stringResource(R.string.modo_sin_mutex)
        ModoCarreraBoletos.ConMutex -> stringResource(R.string.modo_con_mutex)
    }
}

// Elige el color asociado al estado de un comprador para pintar su indicador visual
@Composable
private fun colorEstadoComprador(estado: EstadoCompradorBoleto): Color {
    return when (estado) {
        EstadoCompradorBoleto.EnSeccionCritica -> MaterialTheme.colorScheme.tertiary
        EstadoCompradorBoleto.Compro,
        EstadoCompradorBoleto.Finalizado -> MaterialTheme.colorScheme.primary

        EstadoCompradorBoleto.Error -> MaterialTheme.colorScheme.error
        EstadoCompradorBoleto.EsperandoMutex,
        EstadoCompradorBoleto.IntentandoEntrar -> MaterialTheme.colorScheme.secondary

        EstadoCompradorBoleto.Esperando,
        EstadoCompradorBoleto.SinBoleto,
        EstadoCompradorBoleto.Cancelado -> MaterialTheme.colorScheme.outline
    }
}

// Formatea un string de recurso con los argumentos dados usando el locale por defecto
private fun String.formatear(vararg argumentos: Any): String {
    return String.format(Locale.getDefault(), this, *argumentos)
}

// Preview de la pantalla en su estado inicial, antes de iniciar cualquier ejecucion
@Preview(showBackground = true)
@Composable
private fun PantallaCarreraBoletosInicialPreview() {
    ArchitecturasOSTheme {
        ContenidoCarreraBoletos(
            estado = EstadoCarreraBoletos(),
            puedeIniciar = true,
            puedeCancelar = false,
            puedeReiniciar = true,
            onBoletos = {},
            onCompradores = {},
            onIniciarSinMutex = {},
            onIniciarConMutex = {},
            onCancelar = {},
            onReiniciar = {},
            onVolver = {}
        )
    }
}

// Preview de la pantalla mientras hay una ejecucion con mutex en curso y compradores esperando
@Preview(showBackground = true)
@Composable
private fun PantallaCarreraBoletosEjecutandoPreview() {
    val compradores = (1..8).map { id ->
        CompradorBoleto(
            id = id,
            nombre = "Comprador $id",
            estado = if (id == 1) EstadoCompradorBoleto.EnSeccionCritica else EstadoCompradorBoleto.EsperandoMutex
        )
    }
    ArchitecturasOSTheme {
        ContenidoCarreraBoletos(
            estado = EstadoCarreraBoletos(
                fase = FaseCarreraBoletos.EjecutandoConMutex,
                compradores = compradores,
                metricas = MetricasCarreraBoletos(
                    modo = ModoCarreraBoletos.ConMutex,
                    compradorEnSeccionCritica = "1",
                    compradoresEsperando = 7
                ),
                ejecucionActiva = true
            ),
            puedeIniciar = false,
            puedeCancelar = true,
            puedeReiniciar = false,
            onBoletos = {},
            onCompradores = {},
            onIniciarSinMutex = {},
            onIniciarConMutex = {},
            onCancelar = {},
            onReiniciar = {},
            onVolver = {}
        )
    }
}
