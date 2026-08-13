# Plan 007 - Ticket Rush Mutex

## Objetivo

Demostrar como un mutex protege una seccion critica cuando varios compradores concurrentes intentan comprar boletos de un recurso limitado compartido.

El modulo debe mostrar dos ejecuciones comparables:

- `Sin mutex`: varios hilos acceden al recurso sin proteccion y puede aparecer una venta incorrecta.
- `Con mutex`: varios hilos acceden al recurso usando un lock real y solo un hilo modifica el recurso critico a la vez.

La visualizacion ayuda a explicar el concepto, pero no reemplaza la ejecucion real con hilos y mutex.

## Contrato cerrado

- No implementar este plan hasta que el usuario lo apruebe explicitamente.
- No agregar dependencias nuevas.
- No ejecutar `.\gradlew.bat build` automaticamente.
- Usar Kotlin, Jetpack Compose, Material 3 y componentes comunes existentes.
- Usar nombres propios del proyecto en espanol para paquetes, clases, funciones y modelos.
- Mantener APIs oficiales de Android/Kotlin en ingles.
- Usar `Thread` para los compradores concurrentes.
- Usar `java.util.concurrent.locks.ReentrantLock` como mutex real.
- No usar corrutinas para sustituir la demostracion de hilos del experimento.
- No usar animaciones, numeros aleatorios ni resultados fabricados para fingir el error sin mutex.
- No crear estado global de dashboard ni modelo global `EstadoExperimento`.
- No mostrar boton manual para limpiar el registro de eventos.
- Cada inicio de ejecucion nueva limpia internamente el buffer del registro.
- Todos los textos visibles nuevos deben ir en `android/app/src/main/res/values/strings.xml`.
- El tag estable de Logcat para este modulo es `OSPlayground/Mutex`.

## Estructura real a usar

El codigo Kotlin del modulo debe vivir en:

```txt
android/app/src/main/java/io/yerdna/architecturasos/carreraboletos/
```

La pantalla Compose debe vivir en:

```txt
android/app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaCarreraBoletos.kt
```

Usar la ruta ya existente:

```kotlin
Navegacion.Ruta.CarreraBoletos
```

No crear una ruta nueva llamada `TicketRush`, `Mutex` ni similar. En el dashboard ya existe la tarjeta `Carrera por Boletos` asociada a `Navegacion.Ruta.CarreraBoletos`.

## Archivos a crear

- `android/app/src/main/java/io/yerdna/architecturasos/carreraboletos/CompradorBoleto.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/carreraboletos/EstadoCarreraBoletos.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/carreraboletos/MetricasCarreraBoletos.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/carreraboletos/ResultadoCarreraBoletos.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/carreraboletos/EjecutorCarreraBoletos.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/carreraboletos/CarreraBoletosViewModel.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaCarreraBoletos.kt`

## Archivos a modificar

- `android/app/src/main/java/io/yerdna/architecturasos/ui/App.kt`
- `android/app/src/main/res/values/strings.xml`

No modificar otros archivos para este plan. No modificar `PantallaPanelExperimentos.kt` porque la tarjeta `Carrera por Boletos` ya existe.

## Componentes comunes obligatorios

La pantalla debe reutilizar estos componentes existentes:

- `ExperimentoScaffold` desde `ui/component/ExperimentoScaffold.kt`.
- `rememberRegistroExperimento`, `BotonRegistroEventos` y `HojaRegistroEventos` desde `ui/component/RegistroExperimento.kt`.
- `ExperimentoLogger`, `EventoExperimento`, `TipoEvento` y `OrigenEvento` desde `util/`.

La pantalla es la duena del registro visible:

- Crear `val registro = rememberRegistroExperimento(TAG_CARRERA_BOLETOS)`.
- Pasar `BotonRegistroEventos` en el slot `acciones` de `ExperimentoScaffold`.
- Mostrar `HojaRegistroEventos` con `registro.visible`, `registro.eventos` y `registro::cerrar`.
- Llamar `registro.limpiar()` solo al iniciar una nueva ejecucion.
- No guardar `ExperimentoLogger` en el `ViewModel`.
- No crear singleton global de registro.
- No crear otro panel de eventos para este modulo.

## Responsabilidades por archivo

### `CompradorBoleto.kt`

Definir un modelo simple para cada comprador visible en UI.

Campos minimos:

- `id: Int`
- `nombre: String`
- `estado: EstadoCompradorBoleto`
- `intentos: Int`
- `boletosComprados: Int`
- `mensaje: String?`

Definir `EstadoCompradorBoleto` en el mismo archivo:

- `Esperando`
- `IntentandoEntrar`
- `EsperandoMutex`
- `EnSeccionCritica`
- `Compro`
- `SinBoleto`
- `Finalizado`
- `Cancelado`
- `Error`

El campo `mensaje` debe contener solo texto tecnico no visible o una clave logica. Si se muestra al usuario, la pantalla debe convertirlo a texto desde `strings.xml`.

### `EstadoCarreraBoletos.kt`

Definir el estado principal de pantalla y eventos de registro del modulo.

Constante obligatoria:

```kotlin
const val TAG_CARRERA_BOLETOS = "OSPlayground/Mutex"
```

Fases obligatorias:

- `Inactivo`
- `Preparando`
- `EjecutandoSinMutex`
- `EjecutandoConMutex`
- `Exitoso`
- `Cancelado`
- `Error`

`EstadoCarreraBoletos` debe incluir como minimo:

- `fase: FaseCarreraBoletos`
- `configuracion: ConfiguracionCarreraBoletos`
- `compradores: List<CompradorBoleto>`
- `metricas: MetricasCarreraBoletos`
- `resultadoSinMutex: ResultadoCarreraBoletos?`
- `resultadoConMutex: ResultadoCarreraBoletos?`
- `mensajeError: String?`
- `ejecucionActiva: Boolean`

`ConfiguracionCarreraBoletos` debe incluir:

- `boletosIniciales: Int = 1`
- `compradoresTotales: Int = 8`
- `normalizada()` con boletos entre `1` y `5`, compradores entre `2` y `20`.

Definir `EventoRegistroCarreraBoletos` como `sealed class` sin textos visibles finales. Debe transportar datos para que `PantallaCarreraBoletos.kt` construya mensajes desde `strings.xml`.

Eventos minimos:

- `ConfiguracionAplicada`
- `EjecucionIniciada`
- `ModoSeleccionado`
- `CompradorIntentandoEntrar`
- `CompradorEsperandoMutex`
- `MutexAdquirido`
- `VentaRealizada`
- `VentaIncorrectaDetectada`
- `MutexLiberado`
- `EjecucionFinalizada`
- `EjecucionCancelada`
- `SalidaConfirmada`
- `ErrorTecnico`

### `MetricasCarreraBoletos.kt`

Definir metricas separadas del estado y del resultado historico.

`ModoCarreraBoletos` debe tener:

- `SinMutex`
- `ConMutex`

`MetricasCarreraBoletos` debe incluir:

- `modo: ModoCarreraBoletos?`
- `boletosIniciales: Int`
- `boletosRestantes: Int`
- `compradoresTotales: Int`
- `ventasCorrectas: Int`
- `ventasIncorrectas: Int`
- `compradoresEsperando: Int`
- `compradorEnSeccionCritica: String?`
- `tiempoTotalMs: Long`

Las metricas no deben usarse como fase de ejecucion.

### `ResultadoCarreraBoletos.kt`

Guardar el resultado historico de la ultima ejecucion por modo.

Campos minimos:

- `modo: ModoCarreraBoletos`
- `boletosIniciales: Int`
- `ventasRegistradas: Int`
- `boletosRestantes: Int`
- `ventasIncorrectas: Int`
- `huboErrorDeConcurrencia: Boolean`
- `tiempoTotalMs: Long`

La pantalla debe mostrar separados:

- ultima ejecucion `Sin mutex`;
- ultima ejecucion `Con mutex`.

No borrar estos resultados cuando finaliza una ejecucion nueva de otro modo. Borrarlos solo con `Reiniciar`.

### `EjecutorCarreraBoletos.kt`

Contener la ejecucion tecnica real del experimento.

Responsabilidades:

- Crear compradores usando `Thread`.
- Mantener el recurso compartido `boletosDisponibles`.
- Ejecutar modo sin proteccion sin usar lock.
- Ejecutar modo protegido usando `ReentrantLock`.
- Reportar cambios al `ViewModel` por callbacks simples.
- Reportar eventos tecnicos al `ViewModel` usando eventos de dominio, no strings visibles.
- Cancelar hilos cuando el usuario presiona cancelar, reinicia o sale de la pantalla.
- Usar `AtomicBoolean` para bandera de cancelacion.
- Publicar callbacks hacia el hilo principal con `Handler(Looper.getMainLooper())`.
- Usar un hilo coordinador para iniciar, esperar y calcular resultado final.
- Hacer `join` con timeout corto durante limpieza para no bloquear indefinidamente.

Reglas tecnicas:

- Valores por defecto: `boletosIniciales = 1`, `compradoresTotales = 8`.
- Limites de UI: boletos de `1` a `5`; compradores de `2` a `20`.
- Cada comprador hace un intento de compra por ejecucion.
- En modo `SinMutex`, separar lectura y escritura de `boletosDisponibles` con `Thread.sleep(20)` dentro del hilo para ampliar la ventana de carrera real.
- En modo `ConMutex`, llamar `lock.lock()` antes de leer o modificar `boletosDisponibles` y `lock.unlock()` en `finally`.
- Solo el hilo que adquirio el lock puede liberarlo.
- Nunca ejecutar el trabajo de compradores en el Main/UI Thread.
- No crear loops infinitos.
- No crear hilos ilimitados.
- Ante error, marcar cancelacion, intentar limpiar hilos y publicar `ErrorTecnico`.

Deteccion de venta incorrecta:

- `ventasRegistradas` cuenta todos los compradores que creen haber comprado.
- `ventasCorrectas` no puede superar `boletosIniciales`.
- `ventasIncorrectas` es `maxOf(0, ventasRegistradas - boletosIniciales)`.
- `huboErrorDeConcurrencia` es `true` cuando `ventasIncorrectas > 0` o cuando `boletosRestantes < 0`.

### `CarreraBoletosViewModel.kt`

Mantener estado puro de pantalla.

Responsabilidades:

- Exponer `EstadoCarreraBoletos` como estado observable de Compose.
- Exponer controles habilitados segun `fase` y `ejecucionActiva`.
- Actualizar parametros solo cuando no haya ejecucion activa.
- Iniciar modo `SinMutex`.
- Iniciar modo `ConMutex`.
- Cancelar ejecucion activa.
- Reiniciar el modulo a estado limpio.
- Recibir callbacks del `EjecutorCarreraBoletos`.
- Transformar errores tecnicos en fase `Error`.
- Ignorar callbacks de ejecuciones antiguas con `idEjecucionActiva`.
- Limpiar recursos en `onCleared()`.

No debe contener `Context`, `Log`, `stringResource`, `ExperimentoLogger` ni referencias a composables.

El `ViewModel` debe recibir `onEvento: (EventoRegistroCarreraBoletos) -> Unit` al iniciar cada ejecucion. La pantalla traduce esos eventos a mensajes localizados y los escribe con el logger comun.

### `PantallaCarreraBoletos.kt`

Construir la UI con Jetpack Compose y Material 3.

Debe mostrar:

- Nombre tematico desde `R.string.experimento_carrera_boletos_nombre`.
- Concepto tecnico desde `R.string.experimento_carrera_boletos_concepto`.
- Explicacion corta: el mutex permite acceso exclusivo a la seccion critica.
- Control para cantidad de boletos.
- Control para cantidad de compradores.
- Boton `Ejecutar sin mutex`.
- Boton `Ejecutar con mutex`.
- Boton `Cancelar` cuando haya ejecucion activa.
- Boton `Reiniciar` cuando no haya ejecucion activa.
- Visualizacion de compradores por estado.
- Indicador del comprador dentro de la seccion critica.
- Indicador de compradores esperando.
- Metricas actuales.
- Comparacion de ultimo resultado `Sin mutex` vs `Con mutex`.
- Hoja de registro de eventos usando el componente comun.
- Seccion `Como verificar`.

Tambien debe:

- Usar `BackHandler` cuando haya ejecucion activa.
- Conectar el boton volver de `ExperimentoScaffold` a una funcion `solicitarSalida()`.
- Mostrar `AlertDialog` si el usuario intenta salir con ejecucion activa.
- Si el usuario confirma salida, registrar `SalidaConfirmada`, cancelar ejecucion, limpiar recursos y navegar.
- Si el usuario cancela el dialogo, cerrar el dialogo y mantener la ejecucion.
- Usar `DisposableEffect(viewModel)` para llamar `viewModel.limpiarRecursos()` al descartarse la pantalla.
- Usar previews con datos de ejemplo sin iniciar hilos reales.
- Usar `stringResource` para todo texto visible.
- Convertir `EventoRegistroCarreraBoletos` a llamadas `registro.logger.info`, `registro.logger.advertencia` o `registro.logger.error` en la pantalla.

### `App.kt`

Conectar la pantalla al flujo existente de la aplicacion.

Cambios exactos:

- Importar `io.yerdna.architecturasos.ui.screen.PantallaCarreraBoletos`.
- Reemplazar el destino temporal de `Navegacion.Ruta.CarreraBoletos` por:

```kotlin
composable(Navegacion.Ruta.CarreraBoletos) {
    PantallaCarreraBoletos(
        onVolver = { navController.popBackStack() }
    )
}
```

No modificar las rutas existentes. No agregar estado de ejecucion del experimento al dashboard.

### `strings.xml`

Agregar todos los textos visibles nuevos:

- Descripcion corta de Carrera por Boletos.
- Nombres de fases.
- Nombres de estados de comprador.
- Nombres de botones.
- Etiquetas de parametros.
- Etiquetas de metricas.
- Textos de resultados.
- Textos del dialogo de salida.
- Titulos y descripciones de comandos en `Como verificar`.
- Mensajes de error visibles.
- Mensajes de registro visibles generados por la pantalla.

Los comandos tecnicos literales de verificacion pueden vivir como texto literal en la UI si se muestran como comandos copiables, pero sus titulos y descripciones deben venir de `strings.xml`.

## Controles por estado

| Estado | Ejecutar sin mutex | Ejecutar con mutex | Cancelar | Reiniciar | Cambiar parametros | Salir sin dialogo |
|---|---:|---:|---:|---:|---:|---:|
| `Inactivo` | Si | Si | No | Si | Si | Si |
| `Preparando` | No | No | Si | No | No | No |
| `EjecutandoSinMutex` | No | No | Si | No | No | No |
| `EjecutandoConMutex` | No | No | Si | No | No | No |
| `Exitoso` | Si | Si | No | Si | Si | Si |
| `Cancelado` | Si | Si | No | Si | Si | Si |
| `Error` | Si | Si | No | Si | Si | Si |

## Ciclo de vida

1. Estado inicial: `Inactivo`, sin ejecucion activa, metricas en cero, sin resultados historicos y compradores en `Esperando`.
2. Al iniciar una ejecucion nueva:
   - cancelar y limpiar cualquier ejecucion anterior defensivamente;
   - llamar `registro.limpiar()` desde la pantalla;
   - normalizar configuracion;
   - inicializar compradores;
   - inicializar metricas;
   - pasar por `Preparando`;
   - iniciar hilos reales;
   - pasar a `EjecutandoSinMutex` o `EjecutandoConMutex`;
   - registrar configuracion, inicio y modo.
3. Durante la ejecucion:
   - actualizar estado de compradores;
   - actualizar comprador en seccion critica;
   - actualizar compradores esperando;
   - registrar intentos, espera de mutex, mutex adquirido, ventas, mutex liberado y errores.
4. Al finalizar correctamente:
   - calcular resultado;
   - guardar resultado como ultima ejecucion del modo usado;
   - pasar a `Exitoso`;
   - dejar todos los hilos terminados;
   - limpiar referencias del ejecutor.
5. Al cancelar:
   - marcar bandera de cancelacion;
   - interrumpir hilos cuando aplique;
   - liberar lock solo desde el hilo que lo adquirio;
   - esperar terminacion con timeout corto;
   - pasar a `Cancelado`;
   - limpiar referencias del ejecutor.
6. Al reiniciar:
   - cancelar la ejecucion activa cuando `ejecucionActiva == true`;
   - limpiar compradores, metricas actuales, resultados historicos y estado;
   - dejar la pantalla como recien abierta;
   - no mostrar boton manual de limpiar registro.
7. Al salir de pantalla:
   - si no hay ejecucion activa, navegar directamente;
   - si hay ejecucion activa, mostrar confirmacion;
   - si confirma, registrar salida confirmada, cancelar, limpiar y navegar;
   - si no confirma, mantener ejecucion.
8. `DisposableEffect` debe hacer limpieza defensiva si la pantalla se descarta, sin reemplazar la confirmacion normal de salida.

## Limpieza

La limpieza del modulo debe:

- Marcar cancelacion.
- Interrumpir compradores activos.
- Hacer `join` con timeout corto.
- Dejar `compradorEnSeccionCritica = null`.
- Dejar `compradoresEsperando = 0`.
- Liberar el lock mediante `finally` en el hilo que lo adquirio.
- No dejar hilos activos despues de salir de la pantalla.
- Registrar cancelacion o error antes de limpiar el estado visible.
- No limpiar manualmente el registro por boton.

## Registro de eventos y Logcat

Eventos obligatorios:

- Inicio de ejecucion.
- Configuracion aplicada.
- Modo seleccionado.
- Comprador intenta entrar.
- Comprador espera mutex.
- Mutex adquirido.
- Venta realizada.
- Venta incorrecta detectada.
- Mutex liberado.
- Ejecucion finalizada.
- Ejecucion cancelada.
- Salida confirmada.
- Error tecnico.

La pantalla debe escribir estos eventos en el logger comun. El logger comun ya escribe en el panel visible y en Logcat con el tag `OSPlayground/Mutex`.

No usar `Log.i`, `Log.w` ni `Log.e` directos en `EjecutorCarreraBoletos`, `CarreraBoletosViewModel` ni `PantallaCarreraBoletos`. Todos los eventos y errores del modulo deben pasar por el logger comun desde la pantalla.

## Como verificar

No ejecutar build automaticamente. Si se necesita compilar, pedir aprobacion antes de correr:

```powershell
cd android
.\gradlew.bat build
```

Comandos manuales compatibles con Windows/PowerShell:

```powershell
adb logcat -d -s OSPlayground/Mutex
```

Valida que los eventos reales del experimento aparezcan en Logcat. Deben verse eventos de inicio, intentos de entrada, mutex adquirido, mutex liberado, ventas, ventas incorrectas cuando ocurran, cancelacion y finalizacion. Ejecutarlo despues de correr el experimento.

```powershell
adb shell top
```

Valida actividad general de CPU e hilos durante la ejecucion. Este comando es interactivo y debe detenerse manualmente. No prueba por si solo que el mutex este funcionando.

Verificacion principal desde la UI:

- Ejecutar `Sin mutex` varias veces con `1` boleto y `8` compradores.
- Confirmar que el resultado puede mostrar ventas incorrectas o mas compradores exitosos que boletos disponibles.
- Ejecutar `Con mutex` con los mismos parametros.
- Confirmar que solo un comprador entra a la seccion critica a la vez y que no hay ventas incorrectas.
- Confirmar que `Cancelar`, `Reiniciar`, volver desde barra superior y atras del sistema limpian hilos activos.
- Confirmar que cada nueva ejecucion empieza con un registro visible nuevo.

Verificacion de codigo compatible con Windows/PowerShell:

```powershell
Select-String -Path android/app/src/main/java/io/yerdna/architecturasos/carreraboletos/*.kt,android/app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaCarreraBoletos.kt -Pattern 'Log\.i|Log\.w|Log\.e|Context|stringResource'
```

Interpretacion:

- `Log.i`, `Log.w` y `Log.e` no deben aparecer en los archivos del modulo.
- `Context` no debe aparecer en `CarreraBoletosViewModel.kt`.
- `stringResource` solo debe aparecer en `PantallaCarreraBoletos.kt` o previews/composables de UI.

## Criterios de aceptacion

- [ ] La pantalla Carrera por Boletos abre desde el dashboard usando `Navegacion.Ruta.CarreraBoletos`.
- [ ] `App.kt` ya no muestra `PantallaExperimentoTemporal` para `Navegacion.Ruta.CarreraBoletos`.
- [ ] La UI muestra el concepto `Mutex` y la explicacion corta de acceso exclusivo.
- [ ] El modo `Sin mutex` ejecuta compradores concurrentes reales con `Thread`.
- [ ] El modo `Con mutex` usa `ReentrantLock` real.
- [ ] La seccion critica queda visible en UI.
- [ ] La UI muestra compradores esperando, comprador activo y resultado final.
- [ ] El modo `Sin mutex` puede exhibir comportamiento incorrecto por concurrencia real.
- [ ] El modo `Con mutex` no permite que mas de un comprador modifique el recurso critico al mismo tiempo.
- [ ] Los resultados de ultima ejecucion `Sin mutex` y `Con mutex` se muestran separados.
- [ ] El estado actual, metricas y resultado historico estan separados.
- [ ] `Cancelar` detiene hilos y deja el modulo en estado controlado.
- [ ] `Reiniciar` deja el modulo como recien abierto.
- [ ] Salir con ejecucion activa muestra `AlertDialog`.
- [ ] Confirmar salida cancela, limpia recursos y navega.
- [ ] Cancelar el dialogo mantiene la ejecucion activa.
- [ ] `BackHandler` cubre boton atras del sistema y gesto atras.
- [ ] `DisposableEffect` limpia defensivamente si la pantalla se descarta.
- [ ] Los errores intentan limpiar recursos y pasan a estado `Error`.
- [ ] Los eventos aparecen en panel interno y Logcat con tag `OSPlayground/Mutex`.
- [ ] El modulo usa `rememberRegistroExperimento`, `BotonRegistroEventos` y `HojaRegistroEventos`.
- [ ] No existe boton manual para limpiar registros.
- [ ] Todos los textos visibles nuevos estan en `strings.xml`.
- [ ] No se agregan dependencias nuevas.
- [ ] No se ejecuta build sin aprobacion.

## Decisiones resueltas

- La ruta real de navegacion es `Navegacion.Ruta.CarreraBoletos`; no se crea ruta `TicketRush`.
- Los nombres propios de codigo del proyecto usan espanol: paquete `carreraboletos`, pantalla `PantallaCarreraBoletos`, `CarreraBoletosViewModel`, `EjecutorCarreraBoletos`.
- El dashboard no se modifica porque ya contiene la tarjeta `Carrera por Boletos`.
- El destino temporal en `App.kt` se reemplaza por `PantallaCarreraBoletos`.
- El registro usa los componentes comunes existentes y la pantalla es su duena.
- No se crea `EventoTicketRush.kt`; los eventos de registro viven como sealed class en `EstadoCarreraBoletos.kt` y la pantalla los traduce a textos localizados.
- La condicion incorrecta sin mutex se genera con hilos reales y ventana de carrera controlada, no con numeros aleatorios ni datos fabricados.
- La verificacion usa comandos compatibles con Windows/PowerShell y no ejecuta build automaticamente.

## Bloqueos restantes

No queda ningun bloqueo real que requiera respuesta del usuario antes de implementar este plan.
