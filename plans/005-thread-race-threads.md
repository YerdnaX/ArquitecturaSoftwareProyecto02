# Plan 005 - Thread Race / Hilos

## Objetivo

Implementar el modulo **Thread Race** para demostrar ejecucion concurrente con 1, 2, 4 y 8 hilos reales usando una carga de CPU observable. La visualizacion debe explicar la carrera de corredores, pero el avance y las metricas deben provenir del trabajo real ejecutado fuera del Main Thread.

## Estado del plan

- Estado: implementado dentro de `android/`; build pendiente de confirmacion por el usuario.
- Prioridad: P0.
- Aprobacion de implementacion recibida en esta sesion.
- No agregar dependencias nuevas.
- No ejecutar `.\gradlew.bat build` automaticamente. Si se necesita build, pedir confirmacion primero.

## Depende de

- `001-foundation-dashboard-common.md`
- Componentes comunes ya aprobados por planes anteriores:
  - dashboard sin estado global por experimento;
  - scaffold comun o estructura equivalente de pantalla;
  - registro comun de eventos con `rememberRegistroExperimento`, `BotonRegistroEventos`, `HojaRegistroEventos` y `ExperimentoLogger`.

## Archivos exactos a crear

- `android/app/src/main/java/io/yerdna/architecturasos/hilos/EstadoCarreraHilos.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/hilos/EstadoHiloCarrera.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/hilos/ProgresoHiloCarrera.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/hilos/ResultadoCarreraHilos.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/hilos/ConfiguracionCarreraHilos.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/hilos/EjecutorCarreraHilos.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/hilos/CarreraHilosViewModel.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaCarreraHilos.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/screen/ComponentesCarreraHilos.kt`

## Archivos exactos a modificar

- `android/app/src/main/java/io/yerdna/architecturasos/ui/App.kt`
- `android/app/src/main/res/values/strings.xml`

## Archivos que no se deben modificar en este plan

- No modificar `android/app/build.gradle.kts`.
- No modificar `android/gradle/libs.versions.toml`.
- No modificar `android/app/src/main/AndroidManifest.xml`.
- No crear paquetes `data/model/threads`, `data/repository/threads` ni `ui/screen/threads`; este proyecto ya agrupa los modulos reales en paquetes simples por modulo.
- No crear paquetes `domain`, `usecase`, `mapper`, `di` ni `core`.
- No modificar archivos generados dentro de `build/` o `.gradle/`.

## Decisiones tecnicas cerradas

- Usar `Thread` de Kotlin/JVM para que el concepto de hilo sea directo y explicable.
- No usar coroutines para ejecutar la carga principal del experimento.
- No usar `ExecutorService` en este modulo.
- Usar `AtomicBoolean` para cancelacion cooperativa.
- Usar `Handler(Looper.getMainLooper())` solo para entregar callbacks de progreso al hilo principal antes de actualizar estado observable.
- No usar animaciones como reemplazo de la carga real.
- Usar una carga deterministica de CPU basada en procesamiento numerico por bloques.
- No usar sockets, services ni IPC en este modulo.
- No persistir el historial fuera de la sesion actual.
- Registrar eventos importantes en el panel interno de la pantalla y en Logcat con tag estable `OSPlayground/ThreadRace`.
- Todo texto visible nuevo debe agregarse a `strings.xml`.
- Usar el componente comun de registro existente, no crear un logger local nuevo.
- Ignorar callbacks tardios de ejecuciones anteriores usando `idEjecucion` antes de actualizar estado o registrar eventos.

## Responsabilidades por archivo

### `EstadoCarreraHilos.kt`

Definir el estado general de la pantalla:

```kotlin
enum class EstadoCarreraHilos {
    Inactiva,
    Ejecutando,
    Exitosa,
    Cancelada,
    Error
}
```

### `EstadoHiloCarrera.kt`

Definir el estado individual de cada hilo:

```kotlin
enum class EstadoHiloCarrera {
    Esperando,
    Ejecutando,
    Finalizado,
    Cancelado,
    Error
}
```

### `ConfiguracionCarreraHilos.kt`

Modelo simple con:

- `cantidadHilos: Int`
- `cantidadTrabajo: Int`

Valores permitidos:

- `cantidadHilos`: solo `1`, `2`, `4`, `8`.
- `cantidadTrabajo`: minimo `1`, maximo `5`.
- valor inicial: `cantidadHilos = 4`, `cantidadTrabajo = 3`.

`cantidadTrabajo` representa niveles de demo, no unidades internas directas. El `EjecutorCarreraHilos` traduce cada nivel a bloques numericos suficientes para generar CPU visible sin convertir la demo en una ejecucion larga.

Duracion esperada:

- Implementar niveles conservadores mediante constantes internas privadas de `EjecutorCarreraHilos`.
- Mantener la UI limitada a `1` a `5`; no agregar configuracion avanzada ni valores ilimitados.
- El objetivo verificable es que el nivel `3` con `4` hilos dure lo suficiente para observarse en Android Studio Profiler sin congelar la UI.
- El nivel `5` debe seguir siendo cancelable y adecuado para una demostracion controlada.
- Si se ajustan constantes durante implementacion, hacerlo solo dentro de `EjecutorCarreraHilos`.

### `ProgresoHiloCarrera.kt`

Modelo simple con:

- `idHilo: Int`
- `nombreHilo: String`
- `estado: EstadoHiloCarrera`
- `progreso: Float`
- `operacionesCompletadas: Long`
- `operacionesTotales: Long`
- `tiempoMs: Long`
- `mensaje: String?`

`progreso` debe mantenerse entre `0f` y `1f`.

### `ResultadoCarreraHilos.kt`

Modelo historico de una ejecucion terminada, cancelada o fallida:

- `idEjecucion: Int`
- `cantidadHilos: Int`
- `cantidadTrabajo: Int`
- `estadoFinal: EstadoCarreraHilos`
- `tiempoTotalMs: Long`
- `operacionesTotales: Long`
- `finalizados: Int`
- `cancelados: Int`
- `mensaje: String?`

El historial vive solo en memoria dentro del `ViewModel`.

### `EjecutorCarreraHilos.kt`

Contener la ejecucion tecnica real.

Responsabilidades:

- Crear exactamente `cantidadHilos` instancias de `Thread`.
- Dividir el trabajo total en bloques por hilo.
- Ejecutar calculos numericos reales en cada hilo.
- Reportar progreso individual mediante callbacks.
- Medir tiempo por hilo y tiempo total con `SystemClock.elapsedRealtime()`.
- Respetar cancelacion cooperativa con una bandera compartida segura.
- Entregar callbacks hacia el `ViewModel` en el hilo principal usando `Handler(Looper.getMainLooper())`.
- Incluir `idEjecucion` en los callbacks para que el `ViewModel` ignore eventos tardios de ejecuciones anteriores.
- Capturar excepciones por hilo y reportarlas como `Error`.
- Crear un hilo coordinador para hacer `join()` fuera del Main Thread y reportar el cierre de la ejecucion.
- No tocar estado Compose directamente.
- No recibir `Context`.
- No escribir textos visibles de UI.

Carga tecnica cerrada:

- Cada hilo procesa un rango de numeros `Long`.
- Para cada numero ejecuta operaciones aritmeticas deterministicas con modulo, multiplicacion, suma y comparaciones simples.
- Cada hilo acumula un checksum local para evitar que el compilador elimine el trabajo.
- El checksum solo se usa como dato tecnico interno; no se muestra como resultado principal.
- Reportar progreso cada bloque completo, no en cada operacion.

### `CarreraHilosViewModel.kt`

Mantener estado de pantalla y coordinar acciones.

Responsabilidades:

- Exponer `EstadoCarreraHilos`.
- Exponer `ConfiguracionCarreraHilos`.
- Exponer lista de `ProgresoHiloCarrera`.
- Exponer `tiempoTotalMs`.
- Exponer `resultadoUltimaEjecucion`.
- Exponer historial de `ResultadoCarreraHilos` de la sesion actual.
- Bloquear cambios de configuracion mientras `estado == Ejecutando`.
- Iniciar una nueva ejecucion solo desde `Inactiva`, `Exitosa`, `Cancelada` o `Error`.
- Cancelar una ejecucion activa.
- Reiniciar estado local cuando no haya ejecucion activa.
- Convertir callbacks del ejecutor en estado observable para la UI.
- Registrar eventos mediante callback entregado por la pantalla, no mediante un singleton global.
- Ignorar cualquier callback cuyo `idEjecucion` no coincida con la ejecucion activa o ultima ejecucion esperada.
- No contener `Context`.
- No iniciar trabajo real desde previews.

### `PantallaCarreraHilos.kt`

Composable principal del modulo.

Responsabilidades:

- Mostrar nombre tematico: `Thread Race`.
- Mostrar concepto tecnico: `Hilos`.
- Mostrar explicacion corta del experimento.
- Mostrar controles.
- Mostrar corredores/hilos con progreso individual.
- Mostrar metricas.
- Mostrar resultado de ultima ejecucion.
- Mostrar historial de la sesion actual.
- Mostrar panel de eventos.
- Mostrar seccion "Como verificar".
- Crear el registro con `rememberRegistroExperimento(tag = "OSPlayground/ThreadRace")`.
- Mostrar `BotonRegistroEventos` en las acciones de la barra superior si el `ExperimentoScaffold` tiene slot de acciones disponible.
- Mostrar `HojaRegistroEventos` para visualizar los eventos.
- Llamar a `registro.limpiar()` solo al iniciar una nueva ejecucion; no mostrar boton manual de limpiar.
- Convertir eventos tecnicos del `ViewModel` o del ejecutor a mensajes visibles localizados con `stringResource(...)` antes de llamar al logger.
- Manejar boton volver de la barra superior, back del sistema y gesto atras con la misma regla.
- Usar `AlertDialog` de Material 3 al intentar salir con ejecucion activa.
- Usar `DisposableEffect` como limpieza defensiva para cancelar ejecucion si la pantalla se destruye inesperadamente.

### `ComponentesCarreraHilos.kt`

Composables pequenos y reutilizables solo dentro de esta pantalla:

- `SelectorCantidadHilos`
- `ControlCantidadTrabajo`
- `FilaProgresoHilo`
- `PanelMetricasCarrera`
- `TablaHistorialCarrera`
- `ResultadoCarrera`

Los previews deben usar datos de ejemplo y no iniciar hilos reales.

### `App.kt`

Conectar la ruta o entrada existente del dashboard hacia `PantallaCarreraHilos`.

Reglas:

- No agregar estado global de experimento al dashboard.
- No crear sistema de navegacion propio.
- Usar la navegacion o estructura existente de `001-foundation-dashboard-common.md`.

### `strings.xml`

Agregar todos los textos visibles nuevos:

- titulos;
- subtitulos;
- etiquetas de estado;
- botones;
- dialogo de salida;
- metricas;
- mensajes de error;
- textos del panel de verificacion;
- descripciones cortas de comandos.

No mover a `strings.xml` comandos tecnicos literales como `adb shell top`; si tienen descripcion visible, esa descripcion si debe estar localizada.

## Estados

### Estado general

- `Inactiva`: no hay ejecucion activa. Permite editar configuracion, iniciar y reiniciar.
- `Ejecutando`: hay hilos activos. Bloquea configuracion e inicio doble. Permite cancelar.
- `Exitosa`: todos los hilos finalizaron. Permite iniciar otra ejecucion y reiniciar.
- `Cancelada`: el usuario cancelo una ejecucion activa. Permite iniciar otra ejecucion y reiniciar.
- `Error`: ocurrio una excepcion tecnica. Debe haberse intentado cancelar y limpiar la ejecucion. Permite reiniciar e iniciar de nuevo desde estado controlado.

### Estado por hilo

- `Esperando`: el hilo esta creado en el modelo visual pero aun no empieza su bloque.
- `Ejecutando`: el hilo esta procesando carga real.
- `Finalizado`: el hilo completo su bloque.
- `Cancelado`: el hilo observo la bandera de cancelacion y termino sin completar todo su bloque.
- `Error`: el hilo fallo y reporto mensaje tecnico.

## Metricas

Mostrar en la UI:

- cantidad de hilos seleccionada;
- nivel de trabajo seleccionado;
- progreso individual por hilo;
- operaciones completadas por hilo;
- operaciones totales por hilo;
- tiempo por hilo en ms;
- tiempo total en ms;
- hilos finalizados;
- hilos cancelados;
- cantidad de ejecuciones en historial;
- mejor tiempo de la sesion para la misma cantidad de hilos;
- comparacion simple entre resultados de 1, 2, 4 y 8 hilos cuando existan datos.

No convertir metricas en estados.

## Controles por estado

| Control | Inactiva | Ejecutando | Exitosa | Cancelada | Error |
|---|---|---|---|---|---|
| Selector 1/2/4/8 hilos | habilitado | deshabilitado | habilitado | habilitado | habilitado |
| Control cantidad de trabajo | habilitado | deshabilitado | habilitado | habilitado | habilitado |
| Iniciar | habilitado | deshabilitado | habilitado | habilitado | habilitado |
| Cancelar | deshabilitado | habilitado | deshabilitado | deshabilitado | deshabilitado |
| Reiniciar | habilitado | deshabilitado | habilitado | habilitado | habilitado |
| Volver | directo | pide confirmacion | directo | directo | directo |

Reglas adicionales:

- Doble toque en `Iniciar` no debe crear ejecuciones paralelas.
- `Reiniciar` no borra el historial de la sesion; solo vuelve la pantalla a estado inicial y limpia progreso actual.
- Una nueva ejecucion limpia el registro de eventos de la pantalla y crea una lista nueva de eventos.
- No mostrar boton manual para limpiar registro.

## Ciclo de vida

1. La pantalla entra en `Inactiva`.
2. El usuario elige cantidad de hilos y cantidad de trabajo.
3. Al tocar `Iniciar`, el `ViewModel` crea una nueva ejecucion con `idEjecucion` incremental.
4. La pantalla limpia el registro interno de eventos para esa nueva ejecucion.
5. El `EjecutorCarreraHilos` crea los hilos reales y empieza el trabajo fuera del Main Thread.
6. Cada hilo reporta progreso y eventos.
7. Al finalizar todos los hilos, el estado cambia a `Exitosa` y se agrega un `ResultadoCarreraHilos` al historial.
8. Si el usuario toca `Cancelar`, el `ViewModel` solicita cancelacion al ejecutor.
9. Si todos los hilos salen por cancelacion, el estado cambia a `Cancelada` y se agrega resultado historico.
10. Si ocurre un error, se registra el mensaje, se solicita cancelacion, se limpia la ejecucion activa y el estado cambia a `Error`.
11. Si el usuario intenta salir mientras hay ejecucion activa, se muestra confirmacion.
12. Si el usuario confirma salida, se cancela la ejecucion, se limpian recursos y luego se navega.
13. Si el usuario cancela el dialogo, la pantalla permanece abierta y la ejecucion continua.

## Limpieza

- `EjecutorCarreraHilos` debe exponer una funcion de cancelacion.
- La cancelacion debe ser cooperativa y revisarse dentro del ciclo de trabajo.
- Despues de finalizar, cancelar o fallar:
  - no deben quedar hilos creados por el experimento ejecutandose;
  - no deben quedar callbacks activos hacia una pantalla destruida;
  - no deben quedar referencias innecesarias al ejecutor anterior.
- `DisposableEffect` en la pantalla debe llamar a la limpieza defensiva si la pantalla sale del arbol de Compose con ejecucion activa.
- La confirmacion normal de salida no se reemplaza por `DisposableEffect`; ambos deben existir.

## Registro de eventos

Usar el componente comun de registro existente:

- `rememberRegistroExperimento(tag = "OSPlayground/ThreadRace")`
- `BotonRegistroEventos`
- `HojaRegistroEventos`
- `ExperimentoLogger`

Reglas:

- No crear un logger local nuevo.
- No crear singleton global.
- No guardar el logger dentro del `ViewModel`.
- La pantalla es duena del registro de este experimento.
- El `ViewModel` puede emitir callbacks o codigos de evento simples para que la pantalla registre mensajes localizados.
- Todo mensaje visible del registro debe salir de `strings.xml`.
- Una nueva ejecucion debe llamar a `registro.limpiar()` antes de registrar el nuevo inicio.
- No mostrar boton manual para limpiar el registro.

Eventos minimos:

- ejecucion iniciada;
- cantidad de hilos seleccionada;
- cantidad de trabajo seleccionada;
- hilo iniciado;
- progreso relevante por hilo;
- hilo finalizado;
- hilo cancelado;
- ejecucion finalizada;
- ejecucion cancelada;
- error tecnico.

Tag Logcat:

```text
OSPlayground/ThreadRace
```

No duplicar eventos si ya pasan por el logger comun. `Log.e` directo solo queda permitido como fallback tecnico cuando no se pueda reportar por el canal normal.

## UI esperada

La pantalla debe tener:

- barra superior con volver;
- titulo `Thread Race`;
- subtitulo `Hilos`;
- explicacion breve de que varios hilos procesan bloques de trabajo real;
- selector segmentado para `1`, `2`, `4`, `8`;
- control numerico o slider discreto para cantidad de trabajo `1` a `5`;
- botones `Iniciar`, `Cancelar`, `Reiniciar`;
- lista visual de corredores/hilos con progreso;
- panel de metricas;
- historial de resultados de la sesion;
- comparacion por cantidad de hilos;
- panel de eventos;
- seccion `Como verificar`.

## Como verificar

Estos comandos deben mostrarse en la UI con descripcion breve y compatible con Windows/PowerShell. No obligan al agente a ejecutarlos durante implementacion.

### Ver actividad CPU y threads desde Android

Valida que el experimento genera carga observable mientras esta en `Ejecutando`.

```powershell
adb shell top
```

Interpretacion esperada:

- durante la ejecucion debe observarse actividad de CPU asociada al paquete de la app;
- al cancelar o finalizar, la actividad debe bajar despues de unos segundos.

### Ver eventos del experimento en Logcat

Valida que el modulo emite eventos importantes con tag estable.

```powershell
adb logcat -d -s OSPlayground/ThreadRace
```

Interpretacion esperada:

- deben aparecer eventos de inicio, hilos iniciados, finalizacion, cancelacion o error;
- el comando termina y no queda siguiendo el buffer.

### Ver threads en Android Studio Profiler

Valida visualmente que hay multiples hilos reales durante la ejecucion.

```text
Android Studio > Profiler > CPU > seleccionar proceso de la app > ejecutar Thread Race
```

Interpretacion esperada:

- al iniciar con 4 u 8 hilos deben verse hilos activos adicionales;
- la carga debe ser temporal y finalizar al completar o cancelar.

### Ver tracing con Perfetto/System Trace

Valida que Thread Race genera actividad suficiente para una captura corta de CPU.

```text
Android Studio > Profiler > CPU > Record trace mientras Thread Race esta ejecutando
```

Interpretacion esperada:

- la captura debe mostrar actividad de CPU durante la ventana de ejecucion;
- la captura no reemplaza la verificacion funcional dentro de la app.

## Pasos de implementacion

- [ ] Crear modelos `EstadoCarreraHilos`, `EstadoHiloCarrera`, `ConfiguracionCarreraHilos`, `ProgresoHiloCarrera` y `ResultadoCarreraHilos`.
- [ ] Crear `EjecutorCarreraHilos` con hilos reales, carga numerica, progreso, medicion de tiempos y cancelacion cooperativa.
- [ ] Crear `CarreraHilosViewModel` con estado general, progreso por hilo, historial de sesion y acciones de iniciar/cancelar/reiniciar.
- [ ] Crear componentes Compose pequenos para controles, progreso, metricas, resultado e historial.
- [ ] Crear `PantallaCarreraHilos` usando Material 3, textos desde `strings.xml`, back handling, dialogo de salida y limpieza defensiva.
- [ ] Conectar `PantallaCarreraHilos` desde `App.kt` usando la estructura existente.
- [ ] Agregar strings visibles del modulo en `strings.xml`.
- [ ] Agregar comandos y descripciones de `Como verificar`.
- [ ] Revisar que no exista estado global de dashboard para este experimento.
- [ ] Revisar que previews usen datos de ejemplo y no ejecuten hilos.

## Criterios de aceptacion

- [ ] El experimento permite seleccionar exactamente `1`, `2`, `4` u `8` hilos.
- [ ] El experimento permite seleccionar cantidad de trabajo entre `1` y `5`.
- [ ] La carga ejecutada es computo real en hilos `Thread`, no una animacion.
- [ ] Ninguna carga pesada corre en el Main Thread.
- [ ] La UI permanece responsiva durante la ejecucion.
- [ ] Cada hilo muestra estado, progreso y tiempo.
- [ ] El estado general separa situacion actual, resultado historico y metricas.
- [ ] `Cancelar` detiene la ejecucion de forma cooperativa.
- [ ] Salir con ejecucion activa muestra `AlertDialog`.
- [ ] Confirmar salida cancela hilos, limpia recursos y luego navega.
- [ ] Cancelar el dialogo mantiene la ejecucion activa.
- [ ] El back del sistema y el boton volver siguen la misma regla.
- [ ] `DisposableEffect` realiza limpieza defensiva.
- [ ] Una nueva ejecucion limpia el registro de eventos de pantalla.
- [ ] No existe boton manual para limpiar registro.
- [ ] El historial de la sesion permite comparar ejecuciones con 1, 2, 4 y 8 hilos.
- [ ] Los eventos importantes aparecen en UI y Logcat con tag `OSPlayground/ThreadRace`.
- [ ] Los textos visibles nuevos estan en `strings.xml`.
- [ ] No se agregan dependencias nuevas.
- [ ] No se modifica el dashboard para mostrar estado global del experimento.
- [ ] La seccion `Como verificar` explica que valida cada comando y como interpretar el resultado.

## Decisiones resueltas

### 1. Registro comun de eventos

Decision:

El componente comun ya existe en el codigo y debe usarse directamente. La pantalla debe crear el registro con `rememberRegistroExperimento(tag = "OSPlayground/ThreadRace")`, mostrarlo con `BotonRegistroEventos` y `HojaRegistroEventos`, y registrar mediante el `ExperimentoLogger` expuesto por ese estado.

Contrato cerrado:

- No crear logger local nuevo.
- No crear singleton global.
- No guardar el logger dentro del `ViewModel`.
- No mostrar boton manual de limpiar.
- Limpiar internamente el registro solo al iniciar una nueva ejecucion.
- Localizar en `strings.xml` todos los mensajes visibles del registro.

### 2. Duracion de carga por nivel de trabajo

Decision:

La UI mantiene `cantidadTrabajo` entre `1` y `5`. El `EjecutorCarreraHilos` define constantes internas conservadoras para traducir cada nivel a trabajo numerico real. No se agregan controles avanzados ni valores ilimitados.

Contrato cerrado:

- El nivel `3` con `4` hilos debe durar lo suficiente para observarse en Android Studio Profiler sin congelar la UI.
- El nivel `5` debe seguir siendo cancelable y estable para una exposicion.
- Si hay que ajustar duracion durante implementacion, cambiar solo constantes privadas internas de `EjecutorCarreraHilos`.

## Bloqueos restantes

No quedan bloqueos, contradicciones ni ambiguedades conocidas en el contrato del plan.

## Estado de implementacion

- [x] Aprobado explicitamente por el usuario.
- [x] Implementado dentro de `android/`.
- [ ] Build pendiente de confirmacion por el usuario.
