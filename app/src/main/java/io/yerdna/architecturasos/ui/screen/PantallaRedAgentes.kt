package io.yerdna.architecturasos.ui.screen

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.yerdna.architecturasos.R
import io.yerdna.architecturasos.redagentes.CodigoErrorRedAgentes
import io.yerdna.architecturasos.redagentes.ControladorRedAgentes
import io.yerdna.architecturasos.redagentes.EstadoEnvioRedAgentes
import io.yerdna.architecturasos.redagentes.EstadoRedAgentes
import io.yerdna.architecturasos.redagentes.EstadoServidorRedAgentes
import io.yerdna.architecturasos.redagentes.EventoServidorRedAgentes
import io.yerdna.architecturasos.redagentes.MAX_CARACTERES_MENSAJE
import io.yerdna.architecturasos.redagentes.RedAgentesViewModel
import io.yerdna.architecturasos.redagentes.ResultadoEnvioRedAgentes
import io.yerdna.architecturasos.redagentes.ResultadoRedAgentes
import io.yerdna.architecturasos.redagentes.TAG_RED_AGENTES
import io.yerdna.architecturasos.ui.component.BotonRegistroEventos
import io.yerdna.architecturasos.ui.component.ExperimentoScaffold
import io.yerdna.architecturasos.ui.component.HojaRegistroEventos
import io.yerdna.architecturasos.ui.component.rememberRegistroExperimento
import io.yerdna.architecturasos.ui.theme.ArchitecturasOSTheme
import io.yerdna.architecturasos.util.EventoExperimento
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PantallaRedAgentes(
    onVolver: () -> Unit
) {
    val viewModel = remember { RedAgentesViewModel() }
    val registro = rememberRegistroExperimento(TAG_RED_AGENTES)
    val controlador = remember {
        ControladorRedAgentes(
            callbacks = object : ControladorRedAgentes.Callbacks {
                override fun onEstadoServidor(estado: EstadoServidorRedAgentes) {
                    viewModel.actualizarEstadoServidor(estado)
                }

                override fun onServidorEscuchando(puerto: Int) {
                    viewModel.servidorEscuchando(puerto)
                }

                override fun onEstadoEnvio(estado: EstadoEnvioRedAgentes) {
                    viewModel.actualizarEstadoEnvio(estado)
                }

                override fun onMensajeProcesadoServidor(evento: EventoServidorRedAgentes) {
                    viewModel.mensajeProcesadoServidor(evento)
                }

                override fun onEnvioExitoso(resultado: ResultadoEnvioRedAgentes) {
                    viewModel.envioExitoso(resultado)
                }

                override fun onError(codigo: CodigoErrorRedAgentes, throwable: Throwable?) {
                    registro.logger.error("Error Red de Agentes: $codigo", throwable)
                    viewModel.marcarError(codigo)
                }

                override fun onEventoRegistro(evento: EventoExperimento) {
                    registro.logger.registrar(evento)
                }
            }
        )
    }

    var mostrarConfirmacionSalida by remember { mutableStateOf(false) }

    fun detener() {
        viewModel.detenerServidor()
        controlador.detener()
    }

    fun solicitarSalida() {
        if (viewModel.hayRecursosActivos()) {
            mostrarConfirmacionSalida = true
        } else {
            onVolver()
        }
    }

    BackHandler {
        solicitarSalida()
    }

    DisposableEffect(controlador) {
        onDispose {
            controlador.limpiar()
        }
    }

    if (mostrarConfirmacionSalida) {
        DialogoSalidaRedAgentes(
            onConfirmar = {
                mostrarConfirmacionSalida = false
                registro.logger.advertencia("Salida confirmada: se cerraran sockets")
                detener()
                onVolver()
            },
            onCancelar = { mostrarConfirmacionSalida = false }
        )
    }

    ContenidoRedAgentes(
        estado = viewModel.estado,
        textoMensaje = viewModel.textoMensaje,
        mensajeValido = viewModel.mensajeValido,
        puedeIniciarServidor = viewModel.puedeIniciarServidor(),
        puedeEnviar = viewModel.puedeEnviar(),
        puedeDetener = viewModel.puedeDetener(),
        onMensajeChange = viewModel::actualizarMensaje,
        onIniciarServidor = {
            registro.limpiar()
            viewModel.prepararInicioServidor()
            controlador.iniciarServidor()
        },
        onEnviarMensaje = {
            val mensaje = viewModel.prepararEnvio() ?: return@ContenidoRedAgentes
            val puerto = viewModel.estado.puerto ?: return@ContenidoRedAgentes
            controlador.enviarMensaje(mensaje, puerto)
        },
        onDetenerServidor = ::detener,
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
private fun ContenidoRedAgentes(
    estado: EstadoRedAgentes,
    textoMensaje: String,
    mensajeValido: Boolean,
    puedeIniciarServidor: Boolean,
    puedeEnviar: Boolean,
    puedeDetener: Boolean,
    onMensajeChange: (String) -> Unit,
    onIniciarServidor: () -> Unit,
    onEnviarMensaje: () -> Unit,
    onDetenerServidor: () -> Unit,
    onVolver: () -> Unit,
    acciones: @Composable RowScope.() -> Unit = {}
) {
    ExperimentoScaffold(
        titulo = stringResource(R.string.experimento_red_agentes_nombre),
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
                    text = stringResource(R.string.experimento_red_agentes_concepto),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                Text(
                    text = stringResource(R.string.red_agentes_descripcion_corta),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                ControlesRedAgentes(
                    estado = estado,
                    textoMensaje = textoMensaje,
                    mensajeValido = mensajeValido,
                    puedeIniciarServidor = puedeIniciarServidor,
                    puedeEnviar = puedeEnviar,
                    puedeDetener = puedeDetener,
                    onMensajeChange = onMensajeChange,
                    onIniciarServidor = onIniciarServidor,
                    onEnviarMensaje = onEnviarMensaje,
                    onDetenerServidor = onDetenerServidor
                )
            }
            item {
                VisualizacionRedAgentes(estado = estado)
            }
            item {
                ResultadoRedAgentes(estado = estado)
            }
            item {
                ComoVerificarRedAgentes(estado = estado)
            }
        }
    }
}

@Composable
private fun ControlesRedAgentes(
    estado: EstadoRedAgentes,
    textoMensaje: String,
    mensajeValido: Boolean,
    puedeIniciarServidor: Boolean,
    puedeEnviar: Boolean,
    puedeDetener: Boolean,
    onMensajeChange: (String) -> Unit,
    onIniciarServidor: () -> Unit,
    onEnviarMensaje: () -> Unit,
    onDetenerServidor: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onIniciarServidor,
                enabled = puedeIniciarServidor
            ) {
                Text(stringResource(R.string.accion_iniciar_servidor))
            }
            OutlinedButton(
                onClick = onDetenerServidor,
                enabled = puedeDetener
            ) {
                Text(stringResource(R.string.accion_detener_servidor))
            }
        }

        OutlinedTextField(
            value = textoMensaje,
            onValueChange = onMensajeChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !estado.hayEnvioActivo,
            isError = textoMensaje.isNotBlank() && !mensajeValido,
            label = { Text(stringResource(R.string.red_agentes_mensaje)) },
            supportingText = {
                Text(
                    text = stringResource(
                        R.string.red_agentes_limite_mensaje,
                        textoMensaje.length,
                        MAX_CARACTERES_MENSAJE
                    )
                )
            }
        )

        Button(
            onClick = onEnviarMensaje,
            enabled = puedeEnviar
        ) {
            Text(stringResource(R.string.accion_enviar_mensaje))
        }
    }
}

@Composable
private fun VisualizacionRedAgentes(estado: EstadoRedAgentes) {
    SeccionRedAgentes(titulo = stringResource(R.string.red_agentes_recorrido)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BloqueRedAgentes(
                nombre = stringResource(R.string.red_agentes_agente),
                estado = textoEstadoEnvio(estado.estadoEnvio),
                color = colorEstadoEnvio(estado.estadoEnvio),
                modifier = Modifier.weight(1f)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (estado.hayEnvioActivo) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
                Text(
                    text = textoRecorrido(estado.estadoEnvio),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            BloqueRedAgentes(
                nombre = stringResource(R.string.red_agentes_central),
                estado = textoEstadoServidor(estado.estadoServidor),
                color = colorEstadoServidor(estado.estadoServidor),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BloqueRedAgentes(
    nombre: String,
    estado: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                text = estado,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}

@Composable
private fun ResultadoRedAgentes(estado: EstadoRedAgentes) {
    SeccionRedAgentes(titulo = stringResource(R.string.resultado_ultima_ejecucion)) {
        DatoRedAgentes(
            etiqueta = stringResource(R.string.resultado_ultima_ejecucion),
            valor = textoResultado(estado.resultadoUltimaEjecucion)
        )
        DatoRedAgentes(
            etiqueta = stringResource(R.string.red_agentes_estado_servidor),
            valor = textoEstadoServidor(estado.estadoServidor)
        )
        DatoRedAgentes(
            etiqueta = stringResource(R.string.red_agentes_estado_envio),
            valor = textoEstadoEnvio(estado.estadoEnvio)
        )
        DatoRedAgentes(
            etiqueta = stringResource(R.string.red_agentes_host),
            valor = estado.host
        )
        DatoRedAgentes(
            etiqueta = stringResource(R.string.red_agentes_puerto),
            valor = estado.puerto?.toString() ?: stringResource(R.string.valor_no_disponible)
        )
        DatoRedAgentes(
            etiqueta = stringResource(R.string.red_agentes_hora_conexion),
            valor = formatoHoraRedAgentes(estado.horaConexion)
        )
        DatoRedAgentes(
            etiqueta = stringResource(R.string.red_agentes_mensaje_enviado),
            valor = valorOMarcador(estado.mensajeEnviado)
        )
        DatoRedAgentes(
            etiqueta = stringResource(R.string.red_agentes_mensaje_recibido),
            valor = valorOMarcador(estado.mensajeRecibidoServidor)
        )
        DatoRedAgentes(
            etiqueta = stringResource(R.string.red_agentes_respuesta_servidor),
            valor = valorOMarcador(estado.respuestaEnviadaServidor)
        )
        DatoRedAgentes(
            etiqueta = stringResource(R.string.red_agentes_respuesta_cliente),
            valor = valorOMarcador(estado.respuestaRecibidaCliente)
        )
        DatoRedAgentes(
            etiqueta = stringResource(R.string.red_agentes_bytes_enviados),
            valor = estado.bytesEnviados.toString()
        )
        DatoRedAgentes(
            etiqueta = stringResource(R.string.red_agentes_bytes_recibidos),
            valor = estado.bytesRecibidos.toString()
        )
        DatoRedAgentes(
            etiqueta = stringResource(R.string.red_agentes_tiempo_respuesta),
            valor = estado.tiempoRespuestaMs?.let {
                stringResource(R.string.red_agentes_milisegundos, it)
            } ?: stringResource(R.string.valor_no_disponible)
        )
        estado.codigoError?.let {
            Text(
                text = textoCodigoError(it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ComoVerificarRedAgentes(estado: EstadoRedAgentes) {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val comandos = listOf(
        ComandoVerificacionRedAgentes(
            comando = "adb logcat -d -s OSPlayground/SocketLab",
            descripcionResId = R.string.verificacion_logcat_red_agentes
        ),
        ComandoVerificacionRedAgentes(
            comando = "adb shell \"printf 'prueba\\036' | toybox nc 127.0.0.1 PUERTO\"",
            descripcionResId = R.string.verificacion_nc_red_agentes
        )
    )

    SeccionRedAgentes(titulo = stringResource(R.string.seccion_como_verificar)) {
        estado.puerto?.let { puerto ->
            Text(
                text = stringResource(R.string.red_agentes_reemplazar_puerto, puerto),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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

private data class ComandoVerificacionRedAgentes(
    val comando: String,
    val descripcionResId: Int
)

@Composable
private fun SeccionRedAgentes(
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
private fun DatoRedAgentes(
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
private fun DialogoSalidaRedAgentes(
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.dialogo_salir_red_agentes_titulo)) },
        text = { Text(stringResource(R.string.dialogo_salir_red_agentes_mensaje)) },
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
private fun textoEstadoServidor(estado: EstadoServidorRedAgentes): String {
    return when (estado) {
        EstadoServidorRedAgentes.Inactivo -> stringResource(R.string.estado_inactivo)
        EstadoServidorRedAgentes.Iniciando -> stringResource(R.string.estado_iniciando)
        EstadoServidorRedAgentes.Escuchando -> stringResource(R.string.estado_escuchando)
        EstadoServidorRedAgentes.ClienteConectado -> stringResource(R.string.estado_cliente_conectado)
        EstadoServidorRedAgentes.Deteniendo -> stringResource(R.string.estado_deteniendo)
        EstadoServidorRedAgentes.Detenido -> stringResource(R.string.estado_detenido)
        EstadoServidorRedAgentes.Error -> stringResource(R.string.estado_error)
    }
}

@Composable
private fun textoEstadoEnvio(estado: EstadoEnvioRedAgentes): String {
    return when (estado) {
        EstadoEnvioRedAgentes.SinEnvio -> stringResource(R.string.estado_sin_envio)
        EstadoEnvioRedAgentes.Conectando -> stringResource(R.string.estado_conectando)
        EstadoEnvioRedAgentes.Enviando -> stringResource(R.string.estado_enviando)
        EstadoEnvioRedAgentes.EsperandoRespuesta -> stringResource(R.string.estado_esperando_respuesta)
        EstadoEnvioRedAgentes.Exitoso -> stringResource(R.string.estado_exitoso)
        EstadoEnvioRedAgentes.Error -> stringResource(R.string.estado_error)
        EstadoEnvioRedAgentes.Cancelado -> stringResource(R.string.resultado_cancelado)
    }
}

@Composable
private fun textoResultado(resultado: ResultadoRedAgentes): String {
    return when (resultado) {
        ResultadoRedAgentes.SinEjecucion -> stringResource(R.string.resultado_sin_ejecucion)
        ResultadoRedAgentes.Exitoso -> stringResource(R.string.resultado_completado)
        ResultadoRedAgentes.Cancelado -> stringResource(R.string.resultado_cancelado)
        ResultadoRedAgentes.Error -> stringResource(R.string.resultado_error)
    }
}

@Composable
private fun textoCodigoError(codigo: CodigoErrorRedAgentes): String {
    return when (codigo) {
        CodigoErrorRedAgentes.MensajeVacio -> stringResource(R.string.error_red_agentes_mensaje_vacio)
        CodigoErrorRedAgentes.ServidorNoDisponible -> stringResource(R.string.error_red_agentes_servidor_no_disponible)
        CodigoErrorRedAgentes.Timeout -> stringResource(R.string.error_red_agentes_timeout)
        CodigoErrorRedAgentes.MensajeDemasiadoGrande -> stringResource(R.string.error_red_agentes_mensaje_grande)
        CodigoErrorRedAgentes.ErrorLectura -> stringResource(R.string.error_red_agentes_lectura)
        CodigoErrorRedAgentes.ErrorEscritura -> stringResource(R.string.error_red_agentes_escritura)
        CodigoErrorRedAgentes.ErrorCierre -> stringResource(R.string.error_red_agentes_cierre)
        CodigoErrorRedAgentes.ErrorDesconocido -> stringResource(R.string.error_red_agentes_desconocido)
    }
}

@Composable
private fun colorEstadoServidor(estado: EstadoServidorRedAgentes): Color {
    return when (estado) {
        EstadoServidorRedAgentes.Escuchando,
        EstadoServidorRedAgentes.ClienteConectado -> MaterialTheme.colorScheme.primary
        EstadoServidorRedAgentes.Iniciando,
        EstadoServidorRedAgentes.Deteniendo -> MaterialTheme.colorScheme.tertiary
        EstadoServidorRedAgentes.Error -> MaterialTheme.colorScheme.error
        EstadoServidorRedAgentes.Inactivo,
        EstadoServidorRedAgentes.Detenido -> MaterialTheme.colorScheme.outline
    }
}

@Composable
private fun colorEstadoEnvio(estado: EstadoEnvioRedAgentes): Color {
    return when (estado) {
        EstadoEnvioRedAgentes.Exitoso -> MaterialTheme.colorScheme.primary
        EstadoEnvioRedAgentes.Conectando,
        EstadoEnvioRedAgentes.Enviando,
        EstadoEnvioRedAgentes.EsperandoRespuesta -> MaterialTheme.colorScheme.tertiary
        EstadoEnvioRedAgentes.Error -> MaterialTheme.colorScheme.error
        EstadoEnvioRedAgentes.SinEnvio,
        EstadoEnvioRedAgentes.Cancelado -> MaterialTheme.colorScheme.outline
    }
}

@Composable
private fun textoRecorrido(estado: EstadoEnvioRedAgentes): String {
    return when (estado) {
        EstadoEnvioRedAgentes.SinEnvio -> stringResource(R.string.red_agentes_recorrido_sin_mensaje)
        EstadoEnvioRedAgentes.Conectando -> stringResource(R.string.red_agentes_recorrido_conectando)
        EstadoEnvioRedAgentes.Enviando -> stringResource(R.string.red_agentes_recorrido_enviando)
        EstadoEnvioRedAgentes.EsperandoRespuesta -> stringResource(R.string.red_agentes_recorrido_procesando)
        EstadoEnvioRedAgentes.Exitoso -> stringResource(R.string.red_agentes_recorrido_respuesta_recibida)
        EstadoEnvioRedAgentes.Error -> stringResource(R.string.estado_error)
        EstadoEnvioRedAgentes.Cancelado -> stringResource(R.string.resultado_cancelado)
    }
}

@Composable
private fun valorOMarcador(valor: String): String {
    return valor.ifBlank { stringResource(R.string.valor_no_disponible) }
}

private fun formatoHoraRedAgentes(valor: Long?): String {
    if (valor == null) return "--"
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(valor))
}

@Preview(showBackground = true)
@Composable
private fun PantallaRedAgentesInicialPreview() {
    ArchitecturasOSTheme {
        ContenidoRedAgentes(
            estado = EstadoRedAgentes(),
            textoMensaje = "",
            mensajeValido = false,
            puedeIniciarServidor = true,
            puedeEnviar = false,
            puedeDetener = false,
            onMensajeChange = {},
            onIniciarServidor = {},
            onEnviarMensaje = {},
            onDetenerServidor = {},
            onVolver = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaRedAgentesEscuchandoPreview() {
    ArchitecturasOSTheme {
        ContenidoRedAgentes(
            estado = EstadoRedAgentes(
                puerto = 42100,
                estadoServidor = EstadoServidorRedAgentes.Escuchando
            ),
            textoMensaje = "mensaje secreto",
            mensajeValido = true,
            puedeIniciarServidor = false,
            puedeEnviar = true,
            puedeDetener = true,
            onMensajeChange = {},
            onIniciarServidor = {},
            onEnviarMensaje = {},
            onDetenerServidor = {},
            onVolver = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaRedAgentesErrorPreview() {
    ArchitecturasOSTheme {
        ContenidoRedAgentes(
            estado = EstadoRedAgentes(
                estadoServidor = EstadoServidorRedAgentes.Error,
                estadoEnvio = EstadoEnvioRedAgentes.Error,
                resultadoUltimaEjecucion = ResultadoRedAgentes.Error,
                codigoError = CodigoErrorRedAgentes.Timeout
            ),
            textoMensaje = "mensaje secreto",
            mensajeValido = true,
            puedeIniciarServidor = true,
            puedeEnviar = false,
            puedeDetener = false,
            onMensajeChange = {},
            onIniciarServidor = {},
            onEnviarMensaje = {},
            onDetenerServidor = {},
            onVolver = {}
        )
    }
}
