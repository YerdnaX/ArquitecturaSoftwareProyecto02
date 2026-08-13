package io.yerdna.architecturasos.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.yerdna.architecturasos.R
import io.yerdna.architecturasos.ui.theme.ArchitecturasOSTheme
import io.yerdna.architecturasos.util.EventoExperimento
import io.yerdna.architecturasos.util.OrigenEvento
import io.yerdna.architecturasos.util.TipoEvento
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Muestra el listado de eventos registrados durante un experimento, o un mensaje si aun no hay eventos.
@Composable
fun PanelRegistroEventos(
    eventos: List<EventoExperimento>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.registro_eventos_titulo),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        if (eventos.isEmpty()) {
            Text(
                text = stringResource(R.string.registro_eventos_vacio),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(eventos) { evento ->
                    FilaEvento(evento = evento)
                }
            }
        }
    }
}

// Renderiza una fila con la hora, tipo, origen y mensaje de un evento del registro.
@Composable
private fun FilaEvento(evento: EventoExperimento) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = formatoHoraEvento(evento.timestamp),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = textoTipoEvento(evento.tipo),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = colorTipoEvento(evento.tipo)
        )
        Text(
            text = textoOrigenEvento(evento.origen),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = evento.mensaje,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// Traduce el tipo de un evento (informacion, advertencia o error) a un texto legible.
@Composable
private fun textoTipoEvento(tipo: TipoEvento): String {
    return when (tipo) {
        TipoEvento.Informacion -> stringResource(R.string.tipo_evento_informacion)
        TipoEvento.Advertencia -> stringResource(R.string.tipo_evento_advertencia)
        TipoEvento.Error -> stringResource(R.string.tipo_evento_error)
    }
}

// Elige el color asociado al tipo de un evento para resaltarlo visualmente.
@Composable
private fun colorTipoEvento(tipo: TipoEvento): Color {
    return when (tipo) {
        TipoEvento.Informacion -> MaterialTheme.colorScheme.onSurface
        TipoEvento.Advertencia -> MaterialTheme.colorScheme.tertiary
        TipoEvento.Error -> MaterialTheme.colorScheme.error
    }
}

// Traduce el origen de un evento (proceso principal o servicio) a un texto legible.
@Composable
private fun textoOrigenEvento(origen: OrigenEvento): String {
    return when (origen) {
        OrigenEvento.ProcesoPrincipal -> stringResource(R.string.origen_evento_proceso_principal)
        OrigenEvento.Servicio -> stringResource(R.string.origen_evento_servicio)
    }
}

// Formatea un timestamp en milisegundos como una hora legible HH:mm:ss.
private fun formatoHoraEvento(timestamp: Long): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

// Vista previa del panel de registro de eventos con ejemplos de cada tipo de evento.
@Preview(showBackground = true)
@Composable
private fun PanelRegistroEventosPreview() {
    ArchitecturasOSTheme {
        PanelRegistroEventos(
            eventos = listOf(
                EventoExperimento(
                    timestamp = System.currentTimeMillis(),
                    tipo = TipoEvento.Informacion,
                    mensaje = "Experimento iniciado",
                    origen = OrigenEvento.ProcesoPrincipal
                ),
                EventoExperimento(
                    timestamp = System.currentTimeMillis(),
                    tipo = TipoEvento.Advertencia,
                    mensaje = "No hay cliente registrado",
                    origen = OrigenEvento.Servicio
                ),
                EventoExperimento(
                    timestamp = System.currentTimeMillis(),
                    tipo = TipoEvento.Error,
                    mensaje = "No se pudo enviar mensaje",
                    origen = OrigenEvento.Servicio
                )
            )
        )
    }
}
