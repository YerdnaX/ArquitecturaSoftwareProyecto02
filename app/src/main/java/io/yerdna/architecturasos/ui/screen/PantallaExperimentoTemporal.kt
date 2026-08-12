package io.yerdna.architecturasos.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.yerdna.architecturasos.R
import io.yerdna.architecturasos.ui.Navegacion
import io.yerdna.architecturasos.ui.component.ExperimentoScaffold
import io.yerdna.architecturasos.ui.theme.ArchitecturasOSTheme

@Composable
fun PantallaExperimentoTemporal(
    experimento: ExperimentoResumen,
    onVolver: () -> Unit
) {
    val titulo = stringResource(experimento.nombreResId)

    ExperimentoScaffold(
        titulo = titulo,
        onVolver = onVolver
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(experimento.conceptoResId),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.mensaje_modulo_construccion),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaExperimentoTemporalPreview() {
    ArchitecturasOSTheme {
        PantallaExperimentoTemporal(
            experimento = obtenerExperimentoPorRuta(Navegacion.Ruta.FabricaRobots),
            onVolver = {}
        )
    }
}
