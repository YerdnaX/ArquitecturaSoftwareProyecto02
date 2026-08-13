package io.yerdna.architecturasos.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.yerdna.architecturasos.ui.screen.PantallaBancoCaotico
import io.yerdna.architecturasos.ui.screen.PantallaCarreraBoletos
import io.yerdna.architecturasos.ui.screen.PantallaCarreraHilos
import io.yerdna.architecturasos.ui.screen.PantallaExperimentoTemporal
import io.yerdna.architecturasos.ui.screen.PantallaFabricaRobots
import io.yerdna.architecturasos.ui.screen.PantallaPanelExperimentos
import io.yerdna.architecturasos.ui.screen.PantallaParqueoInteligente
import io.yerdna.architecturasos.ui.screen.PantallaRedAgentes
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
            PantallaRedAgentes(
                onVolver = { navController.popBackStack() }
            )
        }
        composable(Navegacion.Ruta.CarreraHilos) {
            PantallaCarreraHilos(
                onVolver = { navController.popBackStack() }
            )
        }
        composable(Navegacion.Ruta.BancoCaotico) {
            PantallaBancoCaotico(
                onVolver = { navController.popBackStack() }
            )
        }
        composable(Navegacion.Ruta.CarreraBoletos) {
            PantallaCarreraBoletos(
                onVolver = { navController.popBackStack() }
            )
        }
        composable(Navegacion.Ruta.ParqueoInteligente) {
            PantallaParqueoInteligente(
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
