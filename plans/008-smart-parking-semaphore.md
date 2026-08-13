# Plan 008 - Parqueo Inteligente (Semaphore)

## Estado del plan

- Estado: pendiente de aprobacion.
- Alcance: solo define el contrato de implementacion del modulo Parqueo Inteligente (`Navegacion.Ruta.ParqueoInteligente`).
- No implementar este plan hasta que el usuario lo apruebe explicitamente.
- No agregar dependencias nuevas.
- No ejecutar `.\gradlew.bat build` automaticamente.

## Objetivo

Demostrar un `java.util.concurrent.Semaphore` real controlando el acceso concurrente a un estacionamiento con cupos limitados.

La capa visual muestra vehiculos esperando, entrando, estacionados, saliendo y finalizados. La capa tecnica crea trabajo concurrente real: cada vehiculo se ejecuta como una tarea en un `ExecutorService`, fuera del Main Thread, llama a `Semaphore.acquire()` para entrar y `Semaphore.release()` al salir.

## Nombre del modulo y convencion de nombres

El nombre tematico del modulo en la app ya existe en el dashboard como **Parqueo Inteligente** (no "Smart Parking"). El proyecto no usa nombres en ingles para conceptos propios (ver paquetes existentes `carreraboletos`, `bancocaotico`, `hilos`, `procesos`, `restaurante`, `redagentes`). Este plan usa exclusivamente `ParqueoInteligente` / `parqueointeligente` para nombres propios del proyecto. Los nombres `SmartParking`, `smart_parking` y `semaphore` (como paquete) no deben usarse en ningun archivo de codigo ni recurso.

## Estructura real del proyecto (corregida)

La raiz real del modulo Android **no** tiene un prefijo `android/`. El proyecto Gradle vive directamente en la raiz del repositorio:

```txt
app/src/main/java/io/yerdna/architecturasos/
app/src/main/res/values/strings.xml
gradlew.bat   (en la raiz del repositorio, sin carpeta android/)
```

Cualquier referencia previa a `android/app/...` o `cd android` es incorrecta para este proyecto y no debe usarse.

## Dependencias del proyecto

- Depende de `001-foundation-dashboard-common.md` para dashboard, `ExperimentoScaffold` y componentes comunes.
- Depende de `011-panel-registro-eventos.md`, ya implementado: el logger comun (`ExperimentoLogger`, `EventoExperimento`, `TipoEvento`, `OrigenEvento`) y los componentes `rememberRegistroExperimento`, `BotonRegistroEventos`, `HojaRegistroEventos`, `PanelRegistroEventos` ya existen en el codigo. Este plan los usa directamente, sin condicion "si existe".
- Puede revisar `007-ticket-rush-mutex.md` (ya implementado, modulo `carreraboletos`) como referencia directa de patron de codigo: separacion Estado/Metricas/Resultado, `Callbacks` del ejecutor con `idEjecucion`, y estructura de pantalla con `ExperimentoScaffold`, registro y dialogo de salida.
- No crear un estado global de dashboard para este experimento.

## Ruta y tarjeta del dashboard (ya existen, no se recrean)

- La ruta ya existe en `app/src/main/java/io/yerdna/architecturasos/ui/Navegacion.kt`: `Navegacion.Ruta.ParqueoInteligente = "parqueoInteligente"`.
- La tarjeta del dashboard ya existe en `PantallaPanelExperimentos.kt` (icono `"PI"`, color `Color(0xFF386641)`), usando los strings ya existentes `experimento_parqueo_inteligente_nombre`, `experimento_parqueo_inteligente_concepto` y `experimento_parqueo_inteligente_descripcion`.
- **No modificar `PantallaPanelExperimentos.kt`.** No crear estos tres strings de nuevo; ya existen en `strings.xml`.
- Actualmente `Navegacion.Ruta.ParqueoInteligente` esta conectada a `PantallaExperimentoTemporal` dentro de `App.kt`. Este plan reemplaza esa conexion por la pantalla real.

## Archivos exactos a crear

Crear estos archivos dentro de `app/src/main/java/io/yerdna/architecturasos/parqueointeligente/`:

- `parqueointeligente/EstadoParqueoInteligente.kt`
- `parqueointeligente/VehiculoParqueoInteligente.kt`
- `parqueointeligente/MetricasParqueoInteligente.kt`
- `parqueointeligente/ResultadoParqueoInteligente.kt`
- `parqueointeligente/EjecutorParqueoInteligente.kt`
- `parqueointeligente/ParqueoInteligenteViewModel.kt`

Y en `ui/screen/`:

- `app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaParqueoInteligente.kt`

## Archivos exactos a modificar

- `app/src/main/java/io/yerdna/architecturasos/ui/App.kt`
  - Importar `PantallaParqueoInteligente`.
  - Reemplazar el `composable(Navegacion.Ruta.ParqueoInteligente) { PantallaExperimentoTemporal(...) }` actual por:
    ```kotlin
    composable(Navegacion.Ruta.ParqueoInteligente) {
        PantallaParqueoInteligente(
            onVolver = { navController.popBackStack() }
        )
    }
    ```
  - No modificar otras rutas.
- `app/src/main/res/values/strings.xml`
  - Agregar unicamente los strings nuevos listados en la seccion "Textos en `strings.xml`".
  - Reutilizar los strings genericos ya existentes indicados en esa misma seccion; no duplicarlos.

No modificar `AndroidManifest.xml`. Este modulo no usa servicios, sockets ni permisos nuevos.

## Componentes comunes obligatorios

La pantalla debe reutilizar estos componentes existentes, sin recrearlos:

- `ExperimentoScaffold` desde `ui/component/ExperimentoScaffold.kt`.
- `rememberRegistroExperimento`, `BotonRegistroEventos`, `HojaRegistroEventos` desde `ui/component/RegistroExperimento.kt`.
- `ExperimentoLogger`, `EventoExperimento`, `TipoEvento`, `OrigenEvento` desde `util/`.

La pantalla es la duena del registro visible:

- Crear `val registro = rememberRegistroExperimento(TAG_PARQUEO_INTELIGENTE)`.
- Pasar `BotonRegistroEventos` en el slot `acciones` de `ExperimentoScaffold`.
- Mostrar `HojaRegistroEventos` con `registro.visible`, `registro.eventos` y `registro::cerrar`.
- Llamar `registro.limpiar()` solo al iniciar una nueva ejecucion.
- No guardar `ExperimentoLogger` en el `ViewModel`.
- No crear singleton global de registro ni otro panel de eventos para este modulo.

## Responsabilidades por archivo

### `EstadoParqueoInteligente.kt`

Definir la constante de tag, la fase del experimento y el estado de pantalla, siguiendo el mismo patron que `carreraboletos/EstadoCarreraBoletos.kt`.

```kotlin
const val TAG_PARQUEO_INTELIGENTE = "OSPlayground/Semaphore"
```

`FaseParqueoInteligente`:

- `Inactivo`: no hay ejecucion activa y se pueden cambiar parametros.
- `Ejecutando`: hay vehiculos activos, esperando o estacionados.
- `Exitoso`: todos los vehiculos terminaron y no hubo error.
- `Cancelado`: el usuario cancelo o salio confirmando cancelacion.
- `Error`: ocurrio una excepcion y se intento limpiar recursos.

`ConfiguracionParqueoInteligente`:

- `espaciosDisponibles: Int = ESPACIOS_INICIALES`
- `vehiculosTotales: Int = VEHICULOS_INICIALES`
- `normalizada()` aplica `coerceIn` con los limites de la seccion "Parametros definidos".
- Companion con constantes: `ESPACIOS_INICIALES = 3`, `VEHICULOS_INICIALES = 8`, `MIN_ESPACIOS = 1`, `MAX_ESPACIOS = 6`, `MIN_VEHICULOS = 1`, `MAX_VEHICULOS = 12`.

`EstadoParqueoInteligente` (estado de pantalla):

- `fase: FaseParqueoInteligente = FaseParqueoInteligente.Inactivo`
- `configuracion: ConfiguracionParqueoInteligente = ConfiguracionParqueoInteligente()`
- `vehiculos: List<VehiculoParqueoInteligente>`
- `metricas: MetricasParqueoInteligente = MetricasParqueoInteligente()`
- `resultadoUltimaEjecucion: ResultadoParqueoInteligente? = null`
- `mensajeError: String? = null`
- `ejecucionActiva: Boolean = false`

`EventoRegistroParqueoInteligente` (sealed class, sin textos visibles finales; transporta datos que la pantalla traduce a `strings.xml`), siguiendo el mismo patron que `EventoRegistroCarreraBoletos`:

- `ConfiguracionAplicada(espaciosDisponibles: Int, vehiculosTotales: Int)`
- `EjecucionIniciada(idEjecucion: Int)`
- `VehiculoEsperando(idVehiculo: Int)`
- `PermisoAdquirido(idVehiculo: Int, permisosDisponibles: Int)`
- `VehiculoEstacionado(idVehiculo: Int)`
- `VehiculoSaliendo(idVehiculo: Int)`
- `PermisoLiberado(idVehiculo: Int, permisosDisponibles: Int)`
- `VehiculoFinalizado(idVehiculo: Int)`
- `EjecucionFinalizada(resultado: ResultadoParqueoInteligente)`
- `EjecucionCancelada` (data object)
- `SalidaConfirmada` (data object)
- `ErrorTecnico(mensaje: String?)`

Incluir tambien la funcion `vehiculosIniciales(configuracion: ConfiguracionParqueoInteligente): List<VehiculoParqueoInteligente>`, igual que `compradoresIniciales` en `carreraboletos`.

### `VehiculoParqueoInteligente.kt`

Definir `EstadoVehiculoParqueoInteligente` y `VehiculoParqueoInteligente` en el mismo archivo (mismo patron que `CompradorBoleto.kt`).

`EstadoVehiculoParqueoInteligente`:

- `Esperando`: el vehiculo fue creado y espera un permiso del semaforo.
- `Entrando`: adquirio permiso y esta entrando al estacionamiento.
- `Estacionado`: ocupa un cupo protegido por el semaforo.
- `Saliendo`: esta liberando el cupo.
- `Finalizado`: libero permiso y termino.
- `Cancelado`: fue cancelado antes de terminar.
- `Error`: fallo durante su ejecucion.

`VehiculoParqueoInteligente`:

- `id: Int`
- `nombre: String`
- `estado: EstadoVehiculoParqueoInteligente = EstadoVehiculoParqueoInteligente.Esperando`
- `horaInicioMs: Long? = null`
- `horaEntradaMs: Long? = null`
- `horaSalidaMs: Long? = null`
- `tiempoEsperaMs: Long? = null`
- `tiempoEstacionadoMs: Long? = null`
- `mensaje: String? = null`

`mensaje` es texto tecnico interno, no visible directamente; si la pantalla necesita mostrarlo, lo convierte desde `strings.xml`.

### `MetricasParqueoInteligente.kt`

Metricas de la ejecucion actual (separadas del resultado historico, segun `plans/lessons.md` punto 7):

- `permisosTotales: Int = ConfiguracionParqueoInteligente.ESPACIOS_INICIALES`
- `permisosDisponibles: Int = permisosTotales`
- `recursosOcupados: Int = 0`
- `hilosEsperando: Int = 0`
- `vehiculosFinalizados: Int = 0`
- `vehiculosCancelados: Int = 0`
- `vehiculosConError: Int = 0`
- `maximoVehiculosSimultaneos: Int = 0`
- `tiempoTotalMs: Long = 0L`

Las metricas no deben usarse como fase de ejecucion.

### `ResultadoParqueoInteligente.kt`

Resultado historico de la ultima ejecucion completa:

- `permisosTotales: Int`
- `vehiculosTotales: Int`
- `vehiculosFinalizados: Int`
- `vehiculosCancelados: Int`
- `vehiculosConError: Int`
- `maximoVehiculosSimultaneos: Int`
- `duracionTotalMs: Long`

`maximoVehiculosSimultaneos` es una metrica observada (marca de agua de vehiculos con estado `Estacionado` al mismo tiempo), no un parametro de entrada. Por construccion del `Semaphore(permisosTotales)`, nunca puede superar `permisosTotales`; la seccion "Como verificar" confirma esto en tiempo de ejecucion.

### `EjecutorParqueoInteligente.kt`

Ejecutar el mecanismo tecnico real.

Interfaz de callbacks (misma forma que `EjecutorCarreraBoletos.Callbacks`, con `idEjecucion` para que el `ViewModel` descarte callbacks de ejecuciones antiguas):

```kotlin
interface Callbacks {
    fun onVehiculoActualizado(idEjecucion: Int, vehiculo: VehiculoParqueoInteligente)
    fun onMetricasActualizadas(idEjecucion: Int, metricas: MetricasParqueoInteligente)
    fun onEvento(idEjecucion: Int, evento: EventoRegistroParqueoInteligente)
    fun onFinalizada(idEjecucion: Int, resultado: ResultadoParqueoInteligente)
    fun onCancelada(idEjecucion: Int)
    fun onError(idEjecucion: Int, mensaje: String?, throwable: Throwable?)
}
```

Responsabilidades:

- Crear `Semaphore(permisosTotales)` real por ejecucion.
- Crear un `ExecutorService` con `Executors.newFixedThreadPool(vehiculosTotales)` y enviar una tarea (`Runnable`) por vehiculo. Esta es la decision cerrada del mecanismo de concurrencia para este modulo: no usar `Thread` suelto ni coroutines aqui.
- Ejecutar todo el trabajo de vehiculos fuera del Main Thread.
- Medir tiempos con `SystemClock.elapsedRealtime()`, igual que `EjecutorCarreraBoletos`.
- Para cada vehiculo, en este orden:
  1. reportar `Esperando`;
  2. llamar a `semaforo.acquire()` (metodo interrumpible: una cancelacion mientras espera lanza `InterruptedException` y el vehiculo debe pasar a `Cancelado` sin haber ocupado un permiso);
  3. reportar `Entrando` y `PermisoAdquirido`;
  4. reportar `Estacionado`;
  5. `Thread.sleep` de `300 ms` (entrando) + `1200 ms` (estacionado) + `300 ms` (saliendo) como tiempos fijos de la demo — decision cerrada, no configurable por el usuario;
  6. reportar `Saliendo`;
  7. en `finally`, llamar a `semaforo.release()` **solo si el permiso fue adquirido** y reportar `PermisoLiberado`;
  8. reportar `Finalizado`.
- Proteger los contadores compartidos (`recursosOcupados`, `hilosEsperando`, `maximoVehiculosSimultaneos`) con `java.util.concurrent.atomic.AtomicInteger`, igual que `ventasRegistradas` en `EjecutorCarreraBoletos`. No usar `synchronized` manual para estos contadores.
- Publicar todos los callbacks hacia el `ViewModel` en el hilo principal usando `Handler(Looper.getMainLooper())`, con el mismo patron `publicar { ... }` que `EjecutorCarreraBoletos`.
- Implementar `cancelar()`: marcar bandera de cancelacion, llamar `executorService.shutdownNow()` para interrumpir tareas activas (esto libera vehiculos bloqueados en `acquire()`), esperar terminacion con timeout corto (por ejemplo `awaitTermination(500, TimeUnit.MILLISECONDS)`), y reportar `onCancelada`.
- Implementar `limpiar()`: cancelar tareas si siguen activas, cerrar el `ExecutorService` si no esta cerrado, liberar referencias internas y dejar el ejecutor listo para una nueva llamada a `iniciar()`. Seguir el mismo patron que `EjecutorCarreraBoletos.iniciar()`, que llama `limpiar()` al inicio de cada ejecucion nueva.
- Ante error no controlado en una tarea, capturarlo, intentar liberar el permiso si fue adquirido, marcar cancelacion del resto y reportar `onError`.
- No usar animaciones como sustituto del semaforo real. La UI solo visualiza estados reportados por el ejecutor.

### `ParqueoInteligenteViewModel.kt`

Mismo patron que `CarreraBoletosViewModel`.

Responsabilidades:

- `var estado by mutableStateOf(EstadoParqueoInteligente())` de solo lectura externa.
- Mantener `proximoIdEjecucion`, `idEjecucionActiva`, `ejecutor: EjecutorParqueoInteligente?`, `registrarEvento: ((EventoRegistroParqueoInteligente) -> Unit)?`.
- `actualizarEspaciosDisponibles(valor: Int)` y `actualizarVehiculosTotales(valor: Int)`: solo aplican si `!estado.ejecucionActiva`.
- `iniciar(onEvento: (EventoRegistroParqueoInteligente) -> Unit)`: normaliza configuracion, crea vehiculos iniciales, resetea metricas, incrementa `proximoIdEjecucion`, crea/reusa `EjecutorParqueoInteligente`, cambia a `Ejecutando`.
- `cancelar()`: solo si `estado.ejecucionActiva`; delega en `ejecutor?.cancelar()`.
- `reiniciar()`: solo si `!estado.ejecucionActiva`; vuelve el estado a valores iniciales con la configuracion normalizada vigente.
- `limpiarRecursos()`: `ejecutor?.limpiar()`, luego `ejecutor = null`, `idEjecucionActiva = null`, `registrarEvento = null`.
- `puedeIniciar()`: `true` solo en `Inactivo`, `Exitoso`, `Cancelado` o `Error`.
- Implementar `EjecutorParqueoInteligente.Callbacks`, ignorando callbacks cuyo `idEjecucion != idEjecucionActiva`.
- `override fun onCleared()`: llama `limpiarRecursos()` y `super.onCleared()`.
- No guardar `Context`.
- No iniciar trabajo pesado en el Main Thread.
- No debe contener `Context`, `Log`, `stringResource` ni referencias a composables.

### `PantallaParqueoInteligente.kt`

Implementar la pantalla Compose siguiendo el mismo patron que `PantallaCarreraBoletos.kt`.

Responsabilidades:

- Mostrar titulo desde `R.string.experimento_parqueo_inteligente_nombre` y concepto desde `R.string.experimento_parqueo_inteligente_concepto` (strings ya existentes, no se crean de nuevo).
- Mostrar explicacion corta comparando mutex y semaforo (`R.string.parqueo_inteligente_explicacion_mutex_semaforo`).
- Mostrar controles de parametros (espacios, vehiculos).
- Mostrar visualizacion de espacios del estacionamiento y grilla de vehiculos con estado actual.
- Mostrar metricas y resultado de la ultima ejecucion.
- Usar `ExperimentoScaffold` con `acciones = { BotonRegistroEventos(onClick = registro::abrir) }`.
- Mostrar `HojaRegistroEventos`.
- Conectar el boton volver de `ExperimentoScaffold` a `solicitarSalida()`.
- Usar `BackHandler` cuando haya ejecucion activa.
- Mostrar `AlertDialog` si el usuario intenta salir mientras `estado.ejecucionActiva`, reutilizando `R.string.accion_salir_y_cancelar` (confirmar) y `R.string.accion_continuar` (cancelar) para los botones, con titulo/mensaje propios del modulo.
- Si confirma: registrar `SalidaConfirmada`, llamar `viewModel.cancelar()`/`limpiarRecursos()`, navegar.
- Si cancela el dialogo: cerrarlo y mantener la ejecucion.
- Usar `DisposableEffect(viewModel)` para llamar `viewModel.limpiarRecursos()` si la pantalla se descarta.
- Traducir cada `EventoRegistroParqueoInteligente` a `registro.logger.info` / `.advertencia` / `.error` usando textos de `strings.xml`.
- Usar `stringResource` para todo texto visible; no hardcodear textos.
- Usar previews solo con datos de ejemplo, sin iniciar el ejecutor real.

## Parametros definidos

- Espacios disponibles: minimo `1`, maximo `6`, valor inicial `3`.
- Vehiculos: minimo `1`, maximo `12`, valor inicial `8`.
- Duracion fija por vehiculo dentro del ciclo (decision cerrada, no configurable por el usuario):
  - `Entrando`: `300 ms`.
  - `Estacionado`: `1200 ms`.
  - `Saliendo`: `300 ms`.
- No permitir cambiar espacios ni vehiculos mientras `fase == Ejecutando`.

## Controles por estado

| Estado | Espacios | Vehiculos | Iniciar | Cancelar | Reiniciar | Volver |
|---|---|---|---|---|---|---|
| `Inactivo` | habilitado | habilitado | habilitado | deshabilitado | habilitado | sale sin dialogo |
| `Ejecutando` | deshabilitado | deshabilitado | deshabilitado | habilitado | deshabilitado | muestra confirmacion |
| `Exitoso` | habilitado | habilitado | habilitado | deshabilitado | habilitado | sale sin dialogo |
| `Cancelado` | habilitado | habilitado | habilitado | deshabilitado | habilitado | sale sin dialogo |
| `Error` | habilitado | habilitado | habilitado | deshabilitado | habilitado | sale sin dialogo |

Reglas:

- `Iniciar` empieza una ejecucion nueva y limpia el registro de eventos de esa ejecucion (`registro.limpiar()`).
- `Cancelar` detiene tareas activas, libera permisos retenidos y conserva los eventos de la ejecucion cancelada.
- `Reiniciar` solo esta disponible sin ejecucion activa; deja parametros en valores iniciales, borra vehiculos, metricas de corrida y resultado visible.
- Salir desde boton superior, Back del sistema o gesto atras sigue la misma regla.

## Ciclo de vida

1. Al abrir pantalla: estado `Inactivo`, parametros iniciales, sin tareas activas, sin vehiculos creados.
2. Al tocar `Iniciar`:
   - validar y normalizar parametros;
   - `registro.limpiar()`;
   - crear `Semaphore(permisosTotales)` y lista de vehiculos en el ejecutor;
   - cambiar a `Ejecutando`;
   - enviar tareas al `ExecutorService`.
3. Durante ejecucion:
   - actualizar estados por vehiculo;
   - recalcular permisos disponibles, recursos ocupados e hilos esperando;
   - registrar eventos de espera, adquisicion y liberacion de permiso.
4. Al terminar correctamente:
   - todos los vehiculos quedan en `Finalizado`;
   - estado `Exitoso`;
   - `resultadoUltimaEjecucion` resume duracion y maximo simultaneo.
5. Al cancelar:
   - detener tareas activas (`shutdownNow()`);
   - liberar permisos retenidos;
   - vehiculos no terminados pasan a `Cancelado`;
   - estado `Cancelado`.
6. Ante error:
   - registrar mensaje visible y en Logcat (via logger comun desde la pantalla);
   - intentar cancelar y liberar recursos;
   - estado `Error`;
   - permitir iniciar de nuevo desde estado controlado.
7. Al salir de pantalla:
   - si esta `Ejecutando`, mostrar confirmacion;
   - si confirma, cancelar, limpiar recursos y navegar;
   - si cancela el dialogo, continuar en la pantalla;
   - `DisposableEffect` hace limpieza defensiva si la pantalla se elimina sin pasar por el dialogo.
8. En `ViewModel.onCleared()`: cancelar tareas activas y limpiar referencias del ejecutor.

## Registro de eventos

- El registro pertenece a `PantallaParqueoInteligente` (via `rememberRegistroExperimento`).
- No crear singleton global de logs.
- No mostrar boton manual `Limpiar`.
- Al iniciar una ejecucion nueva, el buffer visible empieza vacio (`registro.limpiar()`).
- Registrar eventos importantes: inicio de ejecucion, vehiculo esperando, permiso adquirido, vehiculo estacionado, vehiculo saliendo, permiso liberado, vehiculo finalizado, ejecucion finalizada, cancelacion, error, salida confirmada.
- El logger comun ya escribe en el panel visible y en Logcat con el tag `OSPlayground/Semaphore` (`TAG_PARQUEO_INTELIGENTE`).
- No usar `Log.i`, `Log.w` ni `Log.e` directos en `EjecutorParqueoInteligente`, `ParqueoInteligenteViewModel` ni `PantallaParqueoInteligente`. Todos los eventos pasan por el logger comun desde la pantalla.
- Permitir `Log.e` directo solo como fallback tecnico si no se puede reportar por el canal normal.

## UI requerida

La pantalla debe incluir:

- Encabezado con nombre tematico y concepto tecnico (strings ya existentes del dashboard).
- Explicacion corta: un mutex permite un solo acceso; un semaforo permite una cantidad configurable de accesos simultaneos.
- Selector de espacios disponibles.
- Selector de cantidad de vehiculos.
- Boton `Iniciar` (`R.string.accion_iniciar`).
- Boton `Cancelar` cuando aplique (`R.string.accion_cancelar`).
- Boton `Reiniciar` (`R.string.accion_reiniciar`).
- Visualizacion de espacios del estacionamiento.
- Grilla de vehiculos con estado actual.
- Metricas: permisos totales, permisos disponibles, recursos ocupados, hilos esperando, vehiculos finalizados, maximo simultaneo observado, duracion total.
- Resultado de ultima ejecucion (`R.string.resultado_ultima_ejecucion`).
- Panel de eventos (via `HojaRegistroEventos`).
- Seccion `Como verificar` con comandos copiables.

Usar Material 3 y el tema existente. No crear sistema visual paralelo.

## Textos en `strings.xml`

### Reutilizar (ya existen, no crear de nuevo)

- `experimento_parqueo_inteligente_nombre`, `experimento_parqueo_inteligente_concepto`, `experimento_parqueo_inteligente_descripcion`
- `accion_iniciar`, `accion_cancelar`, `accion_reiniciar`, `accion_volver`, `accion_salir_y_cancelar`, `accion_continuar`
- `estado_inactivo`, `estado_esperando`, `estado_exitoso`, `estado_finalizado`, `estado_error`, `resultado_cancelado`
- `resultado_ultima_ejecucion`
- `registro_eventos_accion_monitoreo`

### Agregar al pool generico de estados (sin prefijo de modulo, junto a los `estado_*` existentes)

- `estado_entrando`
- `estado_estacionado`
- `estado_saliendo`

### Agregar con prefijo `parqueo_inteligente_*`

- `parqueo_inteligente_descripcion_corta`
- `parqueo_inteligente_explicacion_mutex_semaforo`
- `parqueo_inteligente_controles`
- `parqueo_inteligente_espacios`
- `parqueo_inteligente_vehiculos`
- `parqueo_inteligente_estacionamiento`
- `parqueo_inteligente_vehiculos_estado`
- `parqueo_inteligente_vehiculo_numero` (`%1$d`)
- `parqueo_inteligente_metricas`
- `parqueo_inteligente_estado_general`
- `parqueo_inteligente_permisos_totales`
- `parqueo_inteligente_permisos_disponibles`
- `parqueo_inteligente_recursos_ocupados`
- `parqueo_inteligente_hilos_esperando`
- `parqueo_inteligente_vehiculos_finalizados`
- `parqueo_inteligente_maximo_simultaneo`
- `parqueo_inteligente_tiempo_total`
- `parqueo_inteligente_milisegundos` (`%1$d ms`)

### Dialogo de salida

- `dialogo_salir_parqueo_inteligente_titulo`
- `dialogo_salir_parqueo_inteligente_mensaje`

(Los botones del dialogo reutilizan `accion_salir_y_cancelar` y `accion_continuar`; no crear claves nuevas para ellos.)

### Registro (`log_parqueo_inteligente_*`)

- `log_parqueo_inteligente_configuracion` (`%1$d` espacios, `%2$d` vehiculos)
- `log_parqueo_inteligente_ejecucion_iniciada` (`%1$d`)
- `log_parqueo_inteligente_vehiculo_esperando` (`%1$d`)
- `log_parqueo_inteligente_permiso_adquirido` (`%1$d`, `%2$d` permisos disponibles)
- `log_parqueo_inteligente_vehiculo_estacionado` (`%1$d`)
- `log_parqueo_inteligente_vehiculo_saliendo` (`%1$d`)
- `log_parqueo_inteligente_permiso_liberado` (`%1$d`, `%2$d` permisos disponibles)
- `log_parqueo_inteligente_vehiculo_finalizado` (`%1$d`)
- `log_parqueo_inteligente_finalizada` (`%1$d` ms)
- `log_parqueo_inteligente_cancelada`
- `log_parqueo_inteligente_salida_confirmada`
- `log_parqueo_inteligente_error` (`%1$s`)

### Verificacion

- `verificacion_logcat_parqueo_inteligente`
- `verificacion_top_parqueo_inteligente`

## Como verificar

Estos comandos son para verificacion manual desde Windows/PowerShell, ejecutados desde la raiz del repositorio (no existe carpeta `android/`). No ejecutarlos automaticamente durante la implementacion de este plan.

```powershell
.\gradlew.bat build
```

Valida que el proyecto compile. Ejecutarlo solo si el usuario lo aprueba.

```powershell
adb logcat -d -s OSPlayground/Semaphore
```

Valida que el experimento registre eventos reales del semaforo. Deben verse eventos de inicio, espera, permiso adquirido, permiso liberado, finalizacion, cancelacion o error segun la prueba realizada. Ejecutarlo despues de correr el experimento; el comando termina solo porque usa `-d` (vuelca el buffer y finaliza).

```powershell
adb shell top
```

Permite observar actividad general del proceso durante la ejecucion. Es un comando interactivo que debe detenerse manualmente; es una verificacion complementaria y no demuestra por si sola que el semaforo limite accesos.

```powershell
Select-String -Path app/src/main/java/io/yerdna/architecturasos/parqueointeligente/*.kt,app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaParqueoInteligente.kt -Pattern 'Log\.i|Log\.w|Log\.e|Context|stringResource'
```

Interpretacion: `Log.i`, `Log.w` y `Log.e` no deben aparecer en los archivos del modulo (salvo el fallback tecnico documentado). `Context` no debe aparecer en `ParqueoInteligenteViewModel.kt`. `stringResource` solo debe aparecer en `PantallaParqueoInteligente.kt` o sus previews.

Verificacion principal en UI:

- Ejecutar con 3 espacios y 8 vehiculos.
- Confirmar que nunca hay mas de 3 vehiculos en `Estacionado` a la vez.
- Confirmar que algunos vehiculos quedan en `Esperando` mientras los 3 permisos estan ocupados.
- Confirmar que al salir un vehiculo se libera un permiso y entra otro que estaba esperando.
- Confirmar que `maximoVehiculosSimultaneos <= permisosTotales` en el resultado final.
- Confirmar que `Cancelar`, `Reiniciar`, volver desde la barra superior y el boton atras del sistema detienen tareas activas y no dejan hilos corriendo.

## Criterios de aceptacion

- [ ] El modulo usa `java.util.concurrent.Semaphore` real.
- [ ] Cada vehiculo se ejecuta como tarea real en `ExecutorService` fuera del Main Thread.
- [ ] Nunca hay mas vehiculos estacionados que permisos configurados.
- [ ] Los vehiculos esperan cuando no hay permisos disponibles.
- [ ] Los permisos se liberan correctamente al finalizar, cancelar o fallar (nunca se libera un permiso no adquirido).
- [ ] `maximoVehiculosSimultaneos` nunca supera `permisosTotales`.
- [ ] La UI permanece responsiva durante la ejecucion.
- [ ] Los controles se habilitan y deshabilitan segun la tabla de estados.
- [ ] `Cancelar` detiene tareas activas y no deja recursos corriendo.
- [ ] Salir con ejecucion activa muestra `AlertDialog` y limpia antes de navegar si se confirma.
- [ ] Boton superior, Back del sistema y gesto atras siguen la misma regla.
- [ ] `Reiniciar` no se ejecuta sobre una corrida activa.
- [ ] Una nueva ejecucion inicia con registro de eventos limpio.
- [ ] No existe boton manual para limpiar registros.
- [ ] La pantalla usa `ExperimentoScaffold`, `rememberRegistroExperimento`, `BotonRegistroEventos` y `HojaRegistroEventos`.
- [ ] Los textos visibles nuevos estan en `strings.xml`; los strings ya existentes del dashboard y las claves genericas (`accion_*`, `estado_*`, `resultado_*`) no se duplican.
- [ ] Las previews no inician el ejecutor real.
- [ ] No se agregan dependencias nuevas.
- [ ] La diferencia conceptual entre mutex y semaforo queda visible en la pantalla.
- [ ] La seccion `Como verificar` explica que valida cada comando.
- [ ] `PantallaPanelExperimentos.kt` no fue modificado.
- [ ] `App.kt` ya no muestra `PantallaExperimentoTemporal` para `Navegacion.Ruta.ParqueoInteligente`.

## Fuera de alcance

- No implementar navegacion nueva; la ruta `Navegacion.Ruta.ParqueoInteligente` ya existe.
- No agregar persistencia de resultados.
- No agregar historial global de dashboard.
- No agregar servicios Android.
- No agregar permisos al manifest.
- No agregar librerias de graficas, formularios, logging o testing.
- No ejecutar pruebas ni build sin aprobacion.

## Decisiones resueltas

- **Mecanismo de concurrencia por vehiculo**: `ExecutorService` con `Executors.newFixedThreadPool(vehiculosTotales)`, una tarea por vehiculo. No se usa `Thread` suelto ni coroutines para este modulo.
- **Duraciones de la demo**: `300 ms` entrando, `1200 ms` estacionado, `300 ms` saliendo; valores fijos, no configurables por el usuario.
- **Componentes comunes**: ya existen en el codigo (`ExperimentoScaffold`, `rememberRegistroExperimento`, `BotonRegistroEventos`, `HojaRegistroEventos`, `ExperimentoLogger`). Se usan directamente; no hay condicion "si existe".
- **Ruta de navegacion**: se usa `Navegacion.Ruta.ParqueoInteligente`, ya definida en `Navegacion.kt` y ya conectada a una tarjeta del dashboard. `App.kt` solo reemplaza el destino `PantallaExperimentoTemporal` por `PantallaParqueoInteligente`. No se modifica `PantallaPanelExperimentos.kt` ni se crean strings de tarjeta nuevos.
- **Raiz real del proyecto**: no existe carpeta `android/`; todas las rutas de archivo y comandos de `gradlew.bat` se ejecutan desde la raiz del repositorio.
- **Nombres propios**: el modulo se llama `ParqueoInteligente` / `parqueointeligente` en todo el codigo, siguiendo la convencion en espanol ya usada por `carreraboletos`, `bancocaotico`, `hilos`, `procesos`, `restaurante` y `redagentes`. No se usan los nombres `SmartParking` ni `semaphore` como paquete.
- **Separacion Estado/Metricas/Resultado**: `MetricasParqueoInteligente.kt` (corrida actual) se separa de `ResultadoParqueoInteligente.kt` (historico de la ultima ejecucion), igual que en `carreraboletos`.
- **Contadores compartidos**: se protegen con `AtomicInteger`, igual que `EjecutorCarreraBoletos`.
- **Etiquetas de botones**: se reutilizan las claves genericas `accion_iniciar`, `accion_cancelar`, `accion_reiniciar`, `accion_salir_y_cancelar`, `accion_continuar` en vez de crear claves nuevas por modulo.

## Bloqueos restantes

No queda ningun bloqueo real que requiera respuesta del usuario antes de implementar este plan.
