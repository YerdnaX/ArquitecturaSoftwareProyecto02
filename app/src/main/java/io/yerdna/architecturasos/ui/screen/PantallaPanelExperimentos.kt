package io.yerdna.architecturasos.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.yerdna.architecturasos.R
import io.yerdna.architecturasos.ui.Navegacion
import io.yerdna.architecturasos.ui.component.ExperimentoScaffold
import io.yerdna.architecturasos.ui.theme.ArchitecturasOSTheme

private val experimentos = listOf(
    ExperimentoResumen(
        id = "fabrica_robots",
        icono = "FR",
        colorIcono = Color(0xFF006D77),
        nombreResId = R.string.experimento_fabrica_robots_nombre,
        conceptoResId = R.string.experimento_fabrica_robots_concepto,
        descripcionResId = R.string.experimento_fabrica_robots_descripcion,
        ruta = Navegacion.Ruta.FabricaRobots
    ),
    ExperimentoResumen(
        id = "restaurante_ipc",
        icono = "IPC",
        colorIcono = Color(0xFF8F2D56),
        nombreResId = R.string.experimento_restaurante_ipc_nombre,
        conceptoResId = R.string.experimento_restaurante_ipc_concepto,
        descripcionResId = R.string.experimento_restaurante_ipc_descripcion,
        ruta = Navegacion.Ruta.RestauranteIpc
    ),
    ExperimentoResumen(
        id = "red_agentes",
        icono = "RA",
        colorIcono = Color(0xFF2F4858),
        nombreResId = R.string.experimento_red_agentes_nombre,
        conceptoResId = R.string.experimento_red_agentes_concepto,
        descripcionResId = R.string.experimento_red_agentes_descripcion,
        ruta = Navegacion.Ruta.RedAgentes
    ),
    ExperimentoResumen(
        id = "carrera_hilos",
        icono = "CH",
        colorIcono = Color(0xFF5A189A),
        nombreResId = R.string.experimento_carrera_hilos_nombre,
        conceptoResId = R.string.experimento_carrera_hilos_concepto,
        descripcionResId = R.string.experimento_carrera_hilos_descripcion,
        ruta = Navegacion.Ruta.CarreraHilos
    ),
    ExperimentoResumen(
        id = "banco_caotico",
        icono = "BC",
        colorIcono = Color(0xFF9A031E),
        nombreResId = R.string.experimento_banco_caotico_nombre,
        conceptoResId = R.string.experimento_banco_caotico_concepto,
        descripcionResId = R.string.experimento_banco_caotico_descripcion,
        ruta = Navegacion.Ruta.BancoCaotico
    ),
    ExperimentoResumen(
        id = "carrera_boletos",
        icono = "CB",
        colorIcono = Color(0xFFCA6702),
        nombreResId = R.string.experimento_carrera_boletos_nombre,
        conceptoResId = R.string.experimento_carrera_boletos_concepto,
        descripcionResId = R.string.experimento_carrera_boletos_descripcion,
        ruta = Navegacion.Ruta.CarreraBoletos
    ),
    ExperimentoResumen(
        id = "parqueo_inteligente",
        icono = "PI",
        colorIcono = Color(0xFF386641),
        nombreResId = R.string.experimento_parqueo_inteligente_nombre,
        conceptoResId = R.string.experimento_parqueo_inteligente_concepto,
        descripcionResId = R.string.experimento_parqueo_inteligente_descripcion,
        ruta = Navegacion.Ruta.ParqueoInteligente
    ),
    ExperimentoResumen(
        id = "monstruo_memoria",
        icono = "MM",
        colorIcono = Color(0xFF3D348B),
        nombreResId = R.string.experimento_monstruo_memoria_nombre,
        conceptoResId = R.string.experimento_monstruo_memoria_concepto,
        descripcionResId = R.string.experimento_monstruo_memoria_descripcion,
        ruta = Navegacion.Ruta.MonstruoMemoria
    )
)

fun obtenerExperimentoPorRuta(ruta: String): ExperimentoResumen {
    return experimentos.first { it.ruta == ruta }
}

@Composable
fun PantallaPanelExperimentos(
    onAbrirExperimento: (String) -> Unit
) {
    ExperimentoScaffold(
        titulo = stringResource(R.string.titulo_panel_experimentos)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.subtitulo_panel_experimentos),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(experimentos) { experimento ->
                TarjetaExperimento(
                    experimento = experimento,
                    onAbrir = { onAbrirExperimento(experimento.ruta) }
                )
            }
        }
    }
}

@Composable
private fun TarjetaExperimento(
    experimento: ExperimentoResumen,
    onAbrir: () -> Unit
) {
    Card(
        onClick = onAbrir,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(experimento.colorIcono),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = experimento.icono,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(experimento.nombreResId),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(experimento.conceptoResId),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(experimento.descripcionResId),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onAbrir,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = stringResource(R.string.accion_abrir))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaPanelExperimentosPreview() {
    ArchitecturasOSTheme {
        PantallaPanelExperimentos(onAbrirExperimento = {})
    }
}
