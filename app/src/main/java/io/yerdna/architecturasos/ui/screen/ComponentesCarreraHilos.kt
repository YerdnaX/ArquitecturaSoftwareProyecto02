package io.yerdna.architecturasos.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.yerdna.architecturasos.R
import io.yerdna.architecturasos.hilos.ConfiguracionCarreraHilos
import io.yerdna.architecturasos.hilos.EstadoCarreraHilos
import io.yerdna.architecturasos.hilos.EstadoHiloCarrera
import io.yerdna.architecturasos.hilos.ProgresoHiloCarrera
import io.yerdna.architecturasos.hilos.ResultadoCarreraHilos
import io.yerdna.architecturasos.ui.theme.ArchitecturasOSTheme

@Composable
fun SelectorCantidadHilos(
    cantidadSeleccionada: Int,
    habilitado: Boolean,
    onSeleccionar: (Int) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(1, 2, 4, 8).forEach { cantidad ->
            val seleccionado = cantidadSeleccionada == cantidad
            if (seleccionado) {
                Button(
                    onClick = { onSeleccionar(cantidad) },
                    enabled = habilitado
                ) {
                    Text(stringResource(R.string.carrera_hilos_cantidad_hilos, cantidad))
                }
            } else {
                OutlinedButton(
                    onClick = { onSeleccionar(cantidad) },
                    enabled = habilitado
                ) {
                    Text(stringResource(R.string.carrera_hilos_cantidad_hilos, cantidad))
                }
            }
        }
    }
}

@Composable
fun ControlCantidadTrabajo(
    cantidadSeleccionada: Int,
    habilitado: Boolean,
    onSeleccionar: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.carrera_hilos_nivel_trabajo, cantidadSeleccionada),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (1..5).forEach { nivel ->
                val seleccionado = nivel == cantidadSeleccionada
                if (seleccionado) {
                    Button(
                        onClick = { onSeleccionar(nivel) },
                        enabled = habilitado
                    ) {
                        Text(nivel.toString())
                    }
                } else {
                    OutlinedButton(
                        onClick = { onSeleccionar(nivel) },
                        enabled = habilitado
                    ) {
                        Text(nivel.toString())
                    }
                }
            }
        }
    }
}

@Composable
fun FilaProgresoHilo(
    progreso: ProgresoHiloCarrera,
    estadoTexto: String,
    colorEstado: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(colorEstado, RoundedCornerShape(6.dp))
                    )
                    Text(
                        text = progreso.nombreHilo,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = estadoTexto,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorEstado,
                    fontWeight = FontWeight.SemiBold
                )
            }
            LinearProgressIndicator(
                progress = { progreso.progreso.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            DatoCarrera(
                etiqueta = stringResource(R.string.carrera_hilos_operaciones),
                valor = stringResource(
                    R.string.carrera_hilos_operaciones_valor,
                    progreso.operacionesCompletadas,
                    progreso.operacionesTotales
                )
            )
            DatoCarrera(
                etiqueta = stringResource(R.string.carrera_hilos_tiempo_hilo),
                valor = stringResource(R.string.carrera_hilos_milisegundos, progreso.tiempoMs)
            )
        }
    }
}

@Composable
fun PanelMetricasCarrera(
    configuracion: ConfiguracionCarreraHilos,
    estadoTexto: String,
    tiempoTotalMs: Long,
    progresos: List<ProgresoHiloCarrera>,
    historial: List<ResultadoCarreraHilos>,
    mejorTiempoMismaCantidad: Long?
) {
    SeccionCarrera(titulo = stringResource(R.string.carrera_hilos_metricas)) {
        DatoCarrera(
            etiqueta = stringResource(R.string.carrera_hilos_estado_general),
            valor = estadoTexto
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.carrera_hilos_hilos_seleccionados),
            valor = configuracion.cantidadHilos.toString()
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.carrera_hilos_trabajo_seleccionado),
            valor = configuracion.cantidadTrabajo.toString()
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.carrera_hilos_tiempo_total),
            valor = stringResource(R.string.carrera_hilos_milisegundos, tiempoTotalMs)
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.carrera_hilos_hilos_finalizados),
            valor = progresos.count { it.estado == EstadoHiloCarrera.Finalizado }.toString()
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.carrera_hilos_hilos_cancelados),
            valor = progresos.count { it.estado == EstadoHiloCarrera.Cancelado }.toString()
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.carrera_hilos_historial_total),
            valor = historial.size.toString()
        )
        DatoCarrera(
            etiqueta = stringResource(R.string.carrera_hilos_mejor_tiempo),
            valor = mejorTiempoMismaCantidad?.let {
                stringResource(R.string.carrera_hilos_milisegundos, it)
            } ?: stringResource(R.string.valor_no_disponible)
        )
    }
}

@Composable
fun ResultadoCarrera(
    resultado: ResultadoCarreraHilos?,
    estadoTexto: String
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
                etiqueta = stringResource(R.string.carrera_hilos_resultado_estado),
                valor = estadoTexto
            )
            DatoCarrera(
                etiqueta = stringResource(R.string.carrera_hilos_tiempo_total),
                valor = stringResource(R.string.carrera_hilos_milisegundos, resultado.tiempoTotalMs)
            )
            DatoCarrera(
                etiqueta = stringResource(R.string.carrera_hilos_operaciones_totales),
                valor = resultado.operacionesTotales.toString()
            )
            DatoCarrera(
                etiqueta = stringResource(R.string.carrera_hilos_hilos_finalizados),
                valor = resultado.finalizados.toString()
            )
            DatoCarrera(
                etiqueta = stringResource(R.string.carrera_hilos_hilos_cancelados),
                valor = resultado.cancelados.toString()
            )
        }
    }
}

@Composable
fun TablaHistorialCarrera(
    historial: List<ResultadoCarreraHilos>,
    estadoTexto: @Composable (EstadoCarreraHilos) -> String
) {
    SeccionCarrera(titulo = stringResource(R.string.carrera_hilos_historial)) {
        if (historial.isEmpty()) {
            Text(
                text = stringResource(R.string.carrera_hilos_historial_vacio),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            historial.takeLast(5).asReversed().forEach { resultado ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.carrera_hilos_historial_item,
                                resultado.idEjecucion,
                                resultado.cantidadHilos,
                                resultado.cantidadTrabajo
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(
                                R.string.carrera_hilos_historial_detalle,
                                estadoTexto(resultado.estadoFinal),
                                resultado.tiempoTotalMs
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ComparacionCarrera(
    historial: List<ResultadoCarreraHilos>
) {
    SeccionCarrera(titulo = stringResource(R.string.carrera_hilos_comparacion)) {
        listOf(1, 2, 4, 8).forEach { cantidad ->
            val mejor = historial
                .filter {
                    it.cantidadHilos == cantidad &&
                        it.estadoFinal == EstadoCarreraHilos.Exitosa
                }
                .minOfOrNull { it.tiempoTotalMs }
            DatoCarrera(
                etiqueta = stringResource(R.string.carrera_hilos_cantidad_hilos, cantidad),
                valor = mejor?.let {
                    stringResource(R.string.carrera_hilos_milisegundos, it)
                } ?: stringResource(R.string.valor_no_disponible)
            )
        }
    }
}

@Composable
fun SeccionCarrera(
    titulo: String,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = contenido
        )
    }
}

@Composable
fun DatoCarrera(
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

@Preview(showBackground = true)
@Composable
private fun FilaProgresoHiloPreview() {
    ArchitecturasOSTheme {
        FilaProgresoHilo(
            progreso = ProgresoHiloCarrera(
                idHilo = 1,
                nombreHilo = "ThreadRace-1-1",
                estado = EstadoHiloCarrera.Ejecutando,
                progreso = 0.45f,
                operacionesCompletadas = 450_000L,
                operacionesTotales = 1_000_000L,
                tiempoMs = 320L
            ),
            estadoTexto = "Ejecutando",
            colorEstado = MaterialTheme.colorScheme.primary
        )
    }
}
