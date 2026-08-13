package io.yerdna.architecturasos.hilos

data class ConfiguracionCarreraHilos(
    val cantidadHilos: Int = 4,
    val cantidadTrabajo: Int = 3
) {
    // Ajusta la cantidad de hilos y de trabajo a los rangos de valores permitidos.
    fun normalizada(): ConfiguracionCarreraHilos {
        val hilosValidos = setOf(1, 2, 4, 8)
        return copy(
            cantidadHilos = if (cantidadHilos in hilosValidos) cantidadHilos else 4,
            cantidadTrabajo = cantidadTrabajo.coerceIn(1, 5)
        )
    }
}
