package io.yerdna.architecturasos.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.yerdna.architecturasos.ui.screen.PantallaExperimentoTemporal
import io.yerdna.architecturasos.ui.screen.PantallaFabricaRobots
import io.yerdna.architecturasos.ui.screen.PantallaPanelExperimentos
import io.yerdna.architecturasos.ui.screen.PantallaRestauranteIpc
import io.yerdna.architecturasos.ui.screen.obtenerExperimentoPorRuta

@Composable
fun AplicacionOSPlayground() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Navegacion.Ruta.Panel
    ) {
        composable(Navegacion.Ruta.Panel) {
            PantallaPanelExperimentos(
                onAbrirExperimento = { ruta ->
                    navController.navigate(ruta)
                }
            )
        }

        composable(Navegacion.Ruta.FabricaRobots) {
            PantallaFabricaRobots(
                onVolver = { navController.popBackStack() }
            )
        }
        composable(Navegacion.Ruta.RestauranteIpc) {
            PantallaRestauranteIpc(
                onVolver = { navController.popBackStack() }
            )
        }
        composable(Navegacion.Ruta.RedAgentes) {
            PantallaExperimentoTemporal(
                experimento = obtenerExperimentoPorRuta(Navegacion.Ruta.RedAgentes),
                onVolver = { navController.popBackStack() }
            )
        }
        composable(Navegacion.Ruta.CarreraHilos) {
            PantallaExperimentoTemporal(
                experimento = obtenerExperimentoPorRuta(Navegacion.Ruta.CarreraHilos),
                onVolver = { navController.popBackStack() }
            )
        }
        composable(Navegacion.Ruta.BancoCaotico) {
            PantallaExperimentoTemporal(
                experimento = obtenerExperimentoPorRuta(Navegacion.Ruta.BancoCaotico),
                onVolver = { navController.popBackStack() }
            )
        }
        composable(Navegacion.Ruta.CarreraBoletos) {
            PantallaExperimentoTemporal(
                experimento = obtenerExperimentoPorRuta(Navegacion.Ruta.CarreraBoletos),
                onVolver = { navController.popBackStack() }
            )
        }
        composable(Navegacion.Ruta.ParqueoInteligente) {
            PantallaExperimentoTemporal(
                experimento = obtenerExperimentoPorRuta(Navegacion.Ruta.ParqueoInteligente),
                onVolver = { navController.popBackStack() }
            )
        }
        composable(Navegacion.Ruta.MonstruoMemoria) {
            PantallaExperimentoTemporal(
                experimento = obtenerExperimentoPorRuta(Navegacion.Ruta.MonstruoMemoria),
                onVolver = { navController.popBackStack() }
            )
        }
    }
}
