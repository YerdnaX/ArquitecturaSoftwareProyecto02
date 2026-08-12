package io.yerdna.architecturasos.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.yerdna.architecturasos.R
import io.yerdna.architecturasos.ui.theme.ArchitecturasOSTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentoScaffold(
    titulo: String,
    onVolver: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    if (onVolver != null) {
                        IconButton(onClick = onVolver) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.accion_volver)
                            )
                        }
                    }
                }
            )
        },
        content = content
    )
}

@Preview(showBackground = true)
@Composable
private fun ExperimentoScaffoldPreview() {
    ArchitecturasOSTheme {
        ExperimentoScaffold(titulo = "Vista previa", onVolver = {}) {
            Text(text = "Contenido")
        }
    }
}
