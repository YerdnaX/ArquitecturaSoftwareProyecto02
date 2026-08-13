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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.yerdna.architecturasos.R
import io.yerdna.architecturasos.restaurante.ControladorRestauranteIpc
import io.yerdna.architecturasos.restaurante.EstadoConexionRestaurante
import io.yerdna.architecturasos.restaurante.EstadoOrdenRestaurante
import io.yerdna.architecturasos.restaurante.EstadoRestauranteIpc
import io.yerdna.architecturasos.restaurante.EventoOrdenRestaurante
import io.yerdna.architecturasos.restaurante.RestauranteIpcViewModel
import io.yerdna.architecturasos.restaurante.ResultadoRestauranteIpc
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
fun PantallaRestauranteIpc(
    onVolver: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val viewModel = remember { RestauranteIpcViewModel() }
    val registro = rememberRegistroExperimento("OSPlayground/BinderIPC")
    val controlador = remember {
        ControladorRestauranteIpc(
            applicationContext = context,
            callbacks = object : ControladorRestauranteIpc.Callbacks {
                override fun onCocinaConectada(pidCocina: Int, timestamp: Long, mensajes: Int) {
                    viewModel.cocinaConectada(pidCocina, timestamp, mensajes)
                }

                override fun onOrdenEnCola(evento: EventoOrdenRestaurante) {
                    viewModel.marcarOrdenEnCola(evento)
                }

                override fun onOrdenRecibida(evento: EventoOrdenRestaurante) {
                    viewModel.marcarOrdenRecibida(evento)
                }

                override fun onOrdenProcesando(evento: EventoOrdenRestaurante) {
                    viewModel.marcarOrdenProcesando(evento)
                }

                override fun onRespuestaEnviada(evento: EventoOrdenRestaurante) {
                    viewModel.marcarRespuestaRecibida(evento)
                }

                override fun onServicioDesconectado() {
                    registro.logger.advertencia("Servicio Restaurante IPC desconectado")
                    viewModel.marcarDesconectado()
                }

                override fun onError(mensaje: String, rompeConexion: Boolean, mensajes: Int) {
                    registro.logger.error(mensaje.ifBlank { "Error de comunicacion con la cocina" })
                    viewModel.marcarError(mensaje, rompeConexion, mensajes)
                }

                override fun onEventoRegistro(evento: EventoExperimento) {
                    registro.logger.registrar(evento)
                }
            }
        )
    }

    var mostrarConfirmacionSalida by remember { mutableStateOf(false) }
    var mostrarConfirmacionReinicio by remember { mutableStateOf(false) }

    fun desconectar(cancelado: Boolean) {
        registro.logger.info("Desconexion solicitada por el usuario")
        controlador.desconectar()
        viewModel.marcarDesconectado(cancelado = cancelado)
    }

    fun solicitarSalida() {
        if (viewModel.hayConexionActiva() || viewModel.hayTrabajoPendiente()) {
            mostrarConfirmacionSalida = true
        } else {
            onVolver()
        }
    }

    fun solicitarReinicio() {
        if (viewModel.hayConexionActiva() || viewModel.hayTrabajoPendiente()) {
            mostrarConfirmacionReinicio = true
        } else {
            viewModel.limpiarDatos()
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

    if (mostrarConfirmacionSalida) {
        DialogoSalidaRestaurante(
            onConfirmar = {
                mostrarConfirmacionSalida = false
                registro.logger.advertencia("Salida confirmada: se desconectara la cocina")
                desconectar(cancelado = true)
                onVolver()
            },
            onCancelar = { mostrarConfirmacionSalida = false }
        )
    }

    if (mostrarConfirmacionReinicio) {
        DialogoReinicioRestaurante(
            onConfirmar = {
                mostrarConfirmacionReinicio = false
                registro.limpiar()
                registro.logger.advertencia("Reinicio confirmado")
                controlador.reiniciar {
                    viewModel.limpiarDatos()
                    viewModel.prepararConexion()
                    controlador.conectar()
                }
            },
            onCancelar = { mostrarConfirmacionReinicio = false }
        )
    }

    ContenidoRestauranteIpc(
        estado = viewModel.estado,
        textoOrden = viewModel.textoOrden,
        ordenValida = viewModel.ordenValida,
        hayDatosPrevios = viewModel.hayDatosPrevios(),
        onOrdenChange = viewModel::actualizarOrden,
        onOrdenRapida = viewModel::cargarOrdenRapida,
        onConectar = {
            registro.limpiar()
            registro.logger.info("Conexion solicitada")
            viewModel.prepararConexion()
            controlador.conectar()
        },
        onDesconectar = { desconectar(cancelado = viewModel.hayTrabajoPendiente()) },
        onEnviarOrden = {
            val orden = viewModel.prepararEnvioOrden() ?: return@ContenidoRestauranteIpc
            registro.logger.info("Orden enviada ${orden.id}: ${orden.texto}")
            controlador.enviarOrden(orden.texto, orden.id, orden.pidOrigen)
            viewModel.marcarOrdenEnviada()
        },
        onReiniciar = ::solicitarReinicio,
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
private fun ContenidoRestauranteIpc(
    estado: EstadoRestauranteIpc,
    textoOrden: String,
    ordenValida: Boolean,
    hayDatosPrevios: Boolean,
    onOrdenChange: (String) -> Unit,
    onOrdenRapida: (String) -> Unit,
    onConectar: () -> Unit,
    onDesconectar: () -> Unit,
    onEnviarOrden: () -> Unit,
    onReiniciar: () -> Unit,
    onVolver: () -> Unit,
    acciones: @Composable RowScope.() -> Unit = {}
) {
    ExperimentoScaffold(
        titulo = stringResource(R.string.experimento_restaurante_ipc_nombre),
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
                    text = stringResource(R.string.experimento_restaurante_ipc_concepto),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                Text(
                    text = stringResource(R.string.restaurante_ipc_descripcion_corta),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                ControlesRestaurante(
                    estado = estado,
                    textoOrden = textoOrden,
                    ordenValida = ordenValida,
                    hayDatosPrevios = hayDatosPrevios,
                    onOrdenChange = onOrdenChange,
                    onOrdenRapida = onOrdenRapida,
                    onConectar = onConectar,
                    onDesconectar = onDesconectar,
                    onEnviarOrden = onEnviarOrden,
                    onReiniciar = onReiniciar
                )
            }
            item {
                VisualizacionRestaurante(estado = estado)
            }
            item {
                ResultadoRestaurante(estado = estado)
            }
            item {
                ComoVerificarRestaurante()
            }
        }
    }
}

@Composable
private fun ControlesRestaurante(
    estado: EstadoRestauranteIpc,
    textoOrden: String,
    ordenValida: Boolean,
    hayDatosPrevios: Boolean,
    onOrdenChange: (String) -> Unit,
    onOrdenRapida: (String) -> Unit,
    onConectar: () -> Unit,
    onDesconectar: () -> Unit,
    onEnviarOrden: () -> Unit,
    onReiniciar: () -> Unit
) {
    val conectando = estado.estadoConexion == EstadoConexionRestaurante.Conectando
    val conectado = estado.estadoConexion == EstadoConexionRestaurante.Conectado
    val desconectado = estado.estadoConexion == EstadoConexionRestaurante.Desconectado
    val errorConexion = estado.estadoConexion == EstadoConexionRestaurante.Error

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onConectar,
                enabled = !conectando && (desconectado || errorConexion)
            ) {
                Text(stringResource(R.string.accion_conectar))
            }
            OutlinedButton(
                onClick = onDesconectar,
                enabled = conectado
            ) {
                Text(stringResource(R.string.accion_desconectar))
            }
            OutlinedButton(
                onClick = onReiniciar,
                enabled = !conectando && (conectado || errorConexion || hayDatosPrevios)
            ) {
                Text(stringResource(R.string.accion_reiniciar))
            }
        }

        OutlinedTextField(
            value = textoOrden,
            onValueChange = onOrdenChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !conectando,
            isError = conectado && !ordenValida,
            label = { Text(stringResource(R.string.restaurante_ipc_orden)) },
            supportingText = {
                if (conectado && !ordenValida) {
                    Text(stringResource(R.string.error_orden_vacia))
                }
            }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OrdenRapidaButton(R.string.restaurante_ipc_orden_pizza, onOrdenRapida)
            OrdenRapidaButton(R.string.restaurante_ipc_orden_hamburguesa, onOrdenRapida)
            OrdenRapidaButton(R.string.restaurante_ipc_orden_ensalada, onOrdenRapida)
        }

        Button(
            onClick = onEnviarOrden,
            enabled = conectado && ordenValida
        ) {
            Text(stringResource(R.string.accion_enviar_orden))
        }
    }
}

@Composable
private fun OrdenRapidaButton(
    textoResId: Int,
    onOrdenRapida: (String) -> Unit
) {
    val texto = stringResource(textoResId)
    OutlinedButton(onClick = { onOrdenRapida(texto) }) {
        Text(text = texto)
    }
}

@Composable
private fun VisualizacionRestaurante(estado: EstadoRestauranteIpc) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.restaurante_ipc_seccion_recorrido),
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BloqueRestaurante(
                nombre = stringResource(R.string.restaurante_ipc_mesero),
                pid = estado.pidMesero.toString(),
                estado = stringResource(R.string.estado_activo),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = textoFlechaRestaurante(estado.estadoOrden),
                style = MaterialTheme.typography.titleMedium
            )
            BloqueRestaurante(
                nombre = stringResource(R.string.restaurante_ipc_cocina),
                pid = estado.pidCocina?.toString() ?: stringResource(R.string.valor_no_disponible),
                estado = textoEstadoConexionRestaurante(estado.estadoConexion),
                color = colorEstadoConexionRestaurante(estado.estadoConexion),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BloqueRestaurante(
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
private fun ResultadoRestaurante(estado: EstadoRestauranteIpc) {
    SeccionRestaurante(titulo = stringResource(R.string.resultado_ultima_ejecucion)) {
        DatoRestaurante(
            etiqueta = stringResource(R.string.resultado_ultima_ejecucion),
            valor = textoResultadoRestaurante(estado.resultadoUltimaEjecucion)
        )
        DatoRestaurante(
            etiqueta = stringResource(R.string.restaurante_ipc_estado_conexion),
            valor = textoEstadoConexionRestaurante(estado.estadoConexion)
        )
        DatoRestaurante(
            etiqueta = stringResource(R.string.restaurante_ipc_estado_orden),
            valor = textoEstadoOrdenRestaurante(estado.estadoOrden)
        )
        DatoRestaurante(
            etiqueta = stringResource(R.string.restaurante_ipc_orden),
            valor = estado.ordenActual.ifBlank { stringResource(R.string.valor_no_disponible) }
        )
        DatoRestaurante(
            etiqueta = stringResource(R.string.restaurante_ipc_respuesta),
            valor = estado.respuestaActual.ifBlank { stringResource(R.string.valor_no_disponible) }
        )
        DatoRestaurante(
            etiqueta = stringResource(R.string.restaurante_ipc_ordenes_en_cola),
            valor = estado.ordenesEnCola.toString()
        )
        DatoRestaurante(
            etiqueta = stringResource(R.string.restaurante_ipc_ordenes_procesadas),
            valor = estado.totalOrdenesProcesadas.toString()
        )
        DatoRestaurante(
            etiqueta = stringResource(R.string.metrica_mensajes_intercambiados),
            valor = estado.mensajesIntercambiados.toString()
        )
        DatoRestaurante(
            etiqueta = stringResource(R.string.restaurante_ipc_ultimo_evento),
            valor = formatoHoraRestaurante(estado.timestampUltimoEvento)
        )
        estado.mensajeError?.let { mensaje ->
            Text(
                text = mensaje,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ComoVerificarRestaurante() {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val comandos = listOf(
        ComandoVerificacionRestaurante(
            comando = "adb shell ps -A | findstr architecturasos",
            descripcionResId = R.string.verificacion_ps_restaurante
        ),
        ComandoVerificacionRestaurante(
            comando = "adb shell dumpsys activity services io.yerdna.architecturasos | findstr cocina_restaurante",
            descripcionResId = R.string.verificacion_dumpsys_restaurante
        ),
        ComandoVerificacionRestaurante(
            comando = "adb logcat -d -s OSPlayground/BinderIPC",
            descripcionResId = R.string.verificacion_logcat_restaurante
        )
    )

    SeccionRestaurante(titulo = stringResource(R.string.seccion_como_verificar)) {
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

private data class ComandoVerificacionRestaurante(
    val comando: String,
    val descripcionResId: Int
)

@Composable
private fun SeccionRestaurante(
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
private fun DatoRestaurante(
    etiqueta: String,
    valor: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = etiqueta,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = valor,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DialogoSalidaRestaurante(
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.dialogo_salir_restaurante_titulo)) },
        text = { Text(stringResource(R.string.dialogo_salir_restaurante_mensaje)) },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(stringResource(R.string.accion_salir_y_desconectar))
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
private fun DialogoReinicioRestaurante(
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.dialogo_reiniciar_restaurante_titulo)) },
        text = { Text(stringResource(R.string.dialogo_reiniciar_restaurante_mensaje)) },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(stringResource(R.string.accion_reiniciar))
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
private fun textoEstadoConexionRestaurante(estado: EstadoConexionRestaurante): String {
    return when (estado) {
        EstadoConexionRestaurante.Desconectado -> stringResource(R.string.estado_desconectado)
        EstadoConexionRestaurante.Conectando -> stringResource(R.string.estado_conectando)
        EstadoConexionRestaurante.Conectado -> stringResource(R.string.estado_conectado)
        EstadoConexionRestaurante.Error -> stringResource(R.string.estado_error)
    }
}

@Composable
private fun textoEstadoOrdenRestaurante(estado: EstadoOrdenRestaurante): String {
    return when (estado) {
        EstadoOrdenRestaurante.SinOrden -> stringResource(R.string.estado_orden_sin_orden)
        EstadoOrdenRestaurante.PreparandoMensaje -> stringResource(R.string.estado_orden_preparando_mensaje)
        EstadoOrdenRestaurante.Enviando -> stringResource(R.string.estado_orden_enviando)
        EstadoOrdenRestaurante.EnCola -> stringResource(R.string.estado_orden_en_cola)
        EstadoOrdenRestaurante.Recibido -> stringResource(R.string.estado_orden_recibido)
        EstadoOrdenRestaurante.Procesando -> stringResource(R.string.estado_orden_procesando)
        EstadoOrdenRestaurante.RespuestaEnviada -> stringResource(R.string.estado_orden_respuesta_enviada)
        EstadoOrdenRestaurante.RespuestaRecibida -> stringResource(R.string.estado_orden_respuesta_recibida)
        EstadoOrdenRestaurante.Error -> stringResource(R.string.estado_error)
    }
}

@Composable
private fun textoResultadoRestaurante(resultado: ResultadoRestauranteIpc): String {
    return when (resultado) {
        ResultadoRestauranteIpc.SinEjecucion -> stringResource(R.string.resultado_sin_ejecucion)
        ResultadoRestauranteIpc.Completado -> stringResource(R.string.resultado_completado)
        ResultadoRestauranteIpc.Cancelado -> stringResource(R.string.resultado_cancelado)
        ResultadoRestauranteIpc.Error -> stringResource(R.string.resultado_error)
    }
}

@Composable
private fun colorEstadoConexionRestaurante(estado: EstadoConexionRestaurante): Color {
    return when (estado) {
        EstadoConexionRestaurante.Desconectado -> MaterialTheme.colorScheme.outline
        EstadoConexionRestaurante.Conectando -> MaterialTheme.colorScheme.tertiary
        EstadoConexionRestaurante.Conectado -> MaterialTheme.colorScheme.primary
        EstadoConexionRestaurante.Error -> MaterialTheme.colorScheme.error
    }
}

private fun textoFlechaRestaurante(estado: EstadoOrdenRestaurante): String {
    return when (estado) {
        EstadoOrdenRestaurante.PreparandoMensaje,
        EstadoOrdenRestaurante.Enviando,
        EstadoOrdenRestaurante.EnCola,
        EstadoOrdenRestaurante.Recibido,
        EstadoOrdenRestaurante.Procesando -> "->"
        EstadoOrdenRestaurante.RespuestaEnviada,
        EstadoOrdenRestaurante.RespuestaRecibida -> "<-"
        EstadoOrdenRestaurante.SinOrden,
        EstadoOrdenRestaurante.Error -> "--"
    }
}

private fun formatoHoraRestaurante(valor: Long?): String {
    if (valor == null) return "--"
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(valor))
}

@Preview(showBackground = true)
@Composable
private fun PantallaRestauranteDesconectadoPreview() {
    ArchitecturasOSTheme {
        ContenidoRestauranteIpc(
            estado = EstadoRestauranteIpc(),
            textoOrden = "",
            ordenValida = false,
            hayDatosPrevios = false,
            onOrdenChange = {},
            onOrdenRapida = {},
            onConectar = {},
            onDesconectar = {},
            onEnviarOrden = {},
            onReiniciar = {},
            onVolver = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaRestauranteConectadoPreview() {
    ArchitecturasOSTheme {
        ContenidoRestauranteIpc(
            estado = EstadoRestauranteIpc(
                pidCocina = 24520,
                estadoConexion = EstadoConexionRestaurante.Conectado
            ),
            textoOrden = "Pizza",
            ordenValida = true,
            hayDatosPrevios = true,
            onOrdenChange = {},
            onOrdenRapida = {},
            onConectar = {},
            onDesconectar = {},
            onEnviarOrden = {},
            onReiniciar = {},
            onVolver = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaRestauranteProcesandoConColaPreview() {
    ArchitecturasOSTheme {
        ContenidoRestauranteIpc(
            estado = EstadoRestauranteIpc(
                pidCocina = 24520,
                estadoConexion = EstadoConexionRestaurante.Conectado,
                estadoOrden = EstadoOrdenRestaurante.Procesando,
                ordenActual = "Hamburguesa con queso",
                idOrdenActual = 3,
                ordenesEnCola = 2,
                mensajesIntercambiados = 8,
                timestampUltimoEvento = System.currentTimeMillis()
            ),
            textoOrden = "Ensalada",
            ordenValida = true,
            hayDatosPrevios = true,
            onOrdenChange = {},
            onOrdenRapida = {},
            onConectar = {},
            onDesconectar = {},
            onEnviarOrden = {},
            onReiniciar = {},
            onVolver = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaRestauranteErrorPreview() {
    ArchitecturasOSTheme {
        ContenidoRestauranteIpc(
            estado = EstadoRestauranteIpc(
                estadoConexion = EstadoConexionRestaurante.Error,
                estadoOrden = EstadoOrdenRestaurante.Error,
                resultadoUltimaEjecucion = ResultadoRestauranteIpc.Error,
                mensajeError = "No se pudo comunicar con la cocina"
            ),
            textoOrden = "Pizza",
            ordenValida = true,
            hayDatosPrevios = true,
            onOrdenChange = {},
            onOrdenRapida = {},
            onConectar = {},
            onDesconectar = {},
            onEnviarOrden = {},
            onReiniciar = {},
            onVolver = {}
        )
    }
}
