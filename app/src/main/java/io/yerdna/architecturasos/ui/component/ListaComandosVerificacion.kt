package io.yerdna.architecturasos.ui.component

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.yerdna.architecturasos.R
import io.yerdna.architecturasos.diagnostico.ComandoDiagnostico
import kotlinx.coroutines.launch

// Lista los comandos de verificacion de una herramienta de diagnostico, con descripcion, cuando ejecutarlos y como interpretarlos.
@Composable
fun ListaComandosVerificacion(
    comandos: List<ComandoDiagnostico>
) {
    if (comandos.isEmpty()) return

    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        comandos.forEach { comando ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = comando.comando,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                clipboard.setClipEntry(
                                    ClipEntry(ClipData.newPlainText("comando adb", comando.comando))
                                )
                            }
                        }
                    ) {
                        Text(stringResource(R.string.accion_copiar))
                    }
                }
                Text(
                    text = stringResource(comando.descripcionResId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(
                        R.string.diagnostico_formato_cuando_ejecutarlo,
                        stringResource(comando.cuandoEjecutarloResId)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(
                        R.string.diagnostico_formato_como_interpretar,
                        stringResource(comando.comoInterpretarloResId)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
