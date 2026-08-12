package io.yerdna.architecturasos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.yerdna.architecturasos.ui.AplicacionOSPlayground
import io.yerdna.architecturasos.ui.theme.ArchitecturasOSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArchitecturasOSTheme {
                AplicacionOSPlayground()
            }
        }
    }
}
