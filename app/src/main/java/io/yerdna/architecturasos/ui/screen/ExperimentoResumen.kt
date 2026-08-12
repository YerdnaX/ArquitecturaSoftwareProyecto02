package io.yerdna.architecturasos.ui.screen

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color

data class ExperimentoResumen(
    val id: String,
    val icono: String,
    val colorIcono: Color,
    @param:StringRes val nombreResId: Int,
    @param:StringRes val conceptoResId: Int,
    @param:StringRes val descripcionResId: Int,
    val ruta: String
)