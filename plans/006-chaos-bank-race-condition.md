# Plan 006 - Banco Caotico Race Condition

## Objetivo

Implementar el modulo **Banco Caotico** para demostrar una condicion de carrera real causada por varios hilos que modifican un saldo compartido sin sincronizacion.

La visualizacion debe explicar el concepto, pero el error debe nacer de concurrencia real. No se permite fabricar la diferencia con numeros aleatorios, valores precomputados ni animaciones.

## Dependencias de plan

- Requiere los componentes comunes que ya existen en el proyecto: `ExperimentoScaffold`, `rememberRegistroExperimento`, `BotonRegistroEventos`, `HojaRegistroEventos`, `PanelRegistroEventos`, `ExperimentoLogger`, `EventoExperimento`, `TipoEvento` y `OrigenEvento`.
- Reutilizar el criterio tecnico del modulo `hilos`: trabajo real fuera del Main Thread, callbacks publicados al Main Thread antes de actualizar estado Compose, limpieza defensiva con `DisposableEffect` y `ViewModel.onCleared()`.
- No agrega dependencias nuevas.
- No cambia la arquitectura general del proyecto.
- No ejecuta build automaticamente. Si un agente necesita correr `.\gradlew.bat build`, debe pedir aprobacion primero.

## Alcance funcional cerrado

El modulo debe permitir:

- Configurar cantidad de cajeros concurrentes.
- Configurar operaciones por cajero con limites seguros.
- Ejecutar una corrida rapida para exposicion.
- Ejecutar una corrida personalizada dentro de limites definidos.
- Cancelar una corrida activa.
- Reiniciar resultados cuando no haya corrida activa.
- Ver el saldo inicial, resultado esperado, resultado real y diferencia.
- Ver que cajeros/hilos accedieron al saldo compartido.
- Ver si se detecto condicion de carrera.
- Ver eventos del experimento en UI y Logcat.
- Ver comandos de verificacion manual compatibles con Windows.

Fuera de alcance de este plan:

- Implementar mutex o comparacion con mutex. Eso pertenece a `007-ticket-rush-mutex.md`.
- Mantener historial persistente entre aperturas de pantalla.
- Agregar dependencias de testing, concurrencia o graficas.
- Agregar estado global al dashboard.
- Agregar boton manual para limpiar el registro de eventos.

## Archivos exactos a crear

- `android/app/src/main/java/io/yerdna/architecturasos/bancocaotico/EjecutorBancoCaotico.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/bancocaotico/EstadoBancoCaotico.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/bancocaotico/BancoCaoticoViewModel.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaBancoCaotico.kt`

## Archivos exactos a modificar

- `android/app/src/main/java/io/yerdna/architecturasos/ui/App.kt`
  - Reemplazar la pantalla temporal de `Navegacion.Ruta.BancoCaotico` por `PantallaBancoCaotico`.
- `android/app/src/main/res/values/strings.xml`
  - Agregar todos los textos visibles del modulo, estados, acciones, dialogos, metricas, errores y descripciones de verificacion.

No modificar otros archivos para este plan. La ruta `Navegacion.Ruta.BancoCaotico` ya existe y no debe cambiarse.

## Nombres de codigo obligatorios

Usar nombres propios del proyecto en espanol:

- `PantallaBancoCaotico`
- `BancoCaoticoViewModel`
- `EstadoBancoCaotico`
- `ResultadoBancoCaotico`
- `ConfiguracionBancoCaotico`
- `CajeroBancoCaotico`
- `EstadoCajeroBancoCaotico`
- `EjecutorBancoCaotico`
- `EventoRegistroBancoCaotico`
- `TAG_BANCO_CAOTICO`

Mantener en ingles solo APIs oficiales de Android/Kotlin como `ViewModel`, `Thread`, `ExecutorService`, `Future`, `AtomicBoolean`, `Handler`, `BackHandler`, `DisposableEffect`, `AlertDialog`, `Composable`, `Modifier`.

## Modelo de estado

Crear `EstadoBancoCaotico.kt` con estos modelos simples:

- `enum class FaseBancoCaotico`
  - `Inactivo`
  - `Ejecutando`
  - `Exitoso`
  - `Cancelado`
  - `Error`

- `enum class EstadoCajeroBancoCaotico`
  - `Esperando`
  - `Trabajando`
  - `Finalizado`
  - `Cancelado`
  - `Error`

- `data class ConfiguracionBancoCaotico`
  - `val saldoInicial: Int`
  - `val cantidadCajeros: Int`
  - `val operacionesPorCajero: Int`
  - `val montoPorOperacion: Int`

- `data class CajeroBancoCaotico`
  - `val id: Int`
  - `val nombre: String`
  - `val estado: EstadoCajeroBancoCaotico`
  - `val operacionesCompletadas: Int`
  - `val ultimoSaldoLeido: Int?`
  - `val ultimoSaldoEscrito: Int?`

- `data class ResultadoBancoCaotico`
  - `val saldoInicial: Int`
  - `val resultadoEsperado: Int`
  - `val resultadoReal: Int`
  - `val diferencia: Int`
  - `val carreraDetectada: Boolean`
  - `val duracionMs: Long`
  - `val cajerosFinalizados: Int`

- `data class EstadoBancoCaotico`
  - `val fase: FaseBancoCaotico`
  - `val configuracion: ConfiguracionBancoCaotico`
  - `val cajeros: List<CajeroBancoCaotico>`
  - `val resultadoUltimaEjecucion: ResultadoBancoCaotico?`
  - `val mensajeError: String?`
  - `val ejecucionActiva: Boolean`

- `sealed class EventoRegistroBancoCaotico`
  - `data class ConfiguracionAplicada(val cantidadCajeros: Int, val operacionesPorCajero: Int) : EventoRegistroBancoCaotico()`
  - `data class EjecucionIniciada(val idEjecucion: Int) : EventoRegistroBancoCaotico()`
  - `data class CajeroIniciado(val idCajero: Int) : EventoRegistroBancoCaotico()`
  - `data class ProgresoCajero(val idCajero: Int, val operacionesCompletadas: Int) : EventoRegistroBancoCaotico()`
  - `data class CajeroFinalizado(val idCajero: Int) : EventoRegistroBancoCaotico()`
  - `data class CajeroCancelado(val idCajero: Int) : EventoRegistroBancoCaotico()`
  - `data class EjecucionFinalizada(val resultado: ResultadoBancoCaotico) : EventoRegistroBancoCaotico()`
  - `data object EjecucionCancelada : EventoRegistroBancoCaotico()`
  - `data class ErrorTecnico(val mensaje: String?) : EventoRegistroBancoCaotico()`

Agregar en el paquete `bancocaotico`:

```kotlin
const val TAG_BANCO_CAOTICO = "OSPlayground/ChaosBank"
```

El `ViewModel` emite `EventoRegistroBancoCaotico`; la pantalla traduce esos eventos a strings visibles usando `stringResource` y los registra con `registro.logger`.

Separar estrictamente:

- `fase`: situacion actual de ejecucion.
- `resultadoUltimaEjecucion`: dato historico de la ultima corrida terminada.
- metricas: valores derivados como diferencia, duracion, operaciones y cajeros finalizados.

No convertir metricas en estados.

## Configuracion y limites

Valores por defecto:

- `saldoInicial = 0`
- `cantidadCajeros = 8`
- `operacionesPorCajero = 50_000`
- `montoPorOperacion = 1`

Limites obligatorios:

- `cantidadCajeros`: minimo `2`, maximo `8`.
- `operacionesPorCajero`: minimo `1_000`, maximo `200_000`.
- `montoPorOperacion`: fijo en `1` para mantener clara la demostracion.

Modo rapido:

- Boton `Modo rapido`.
- Aplica `cantidadCajeros = 8`, `operacionesPorCajero = 50_000`, `saldoInicial = 0`, `montoPorOperacion = 1`.
- Inicia inmediatamente una nueva ejecucion si no hay otra activa.

Calculo esperado:

```text
resultadoEsperado = saldoInicial + (cantidadCajeros * operacionesPorCajero * montoPorOperacion)
diferencia = resultadoEsperado - resultadoReal
carreraDetectada = diferencia != 0
```

Si una ejecucion termina con `diferencia == 0`, mostrarla como ejecucion completada sin carrera detectada y permitir repetir. No forzar un error artificial.

## Implementacion tecnica

Crear `EjecutorBancoCaotico` con una responsabilidad unica: ejecutar la carrera real fuera del hilo principal.

Contrato tecnico:

- Usar `ExecutorService` creado con `Executors.newFixedThreadPool(cantidadCajeros)`.
- Usar un `Thread` coordinador separado llamado `BancoCaotico-Coordinador-$idEjecucion` para esperar los `Future` de los cajeros. No esperar los `Future` dentro del mismo pool fijo para evitar bloqueo por falta de threads.
- Publicar todos los callbacks hacia `BancoCaoticoViewModel` en el Main Thread usando `Handler(Looper.getMainLooper())`, igual que el patron de `EjecutorCarreraHilos`.
- Crear el saldo compartido como propiedad mutable normal: `var saldoCompartido = saldoInicial`.
- Cada cajero ejecuta exactamente `operacionesPorCajero` operaciones mientras no haya cancelacion.
- Cada operacion debe hacer read-modify-write no atomico:

```kotlin
val saldoLeido = saldoCompartido
Thread.yield()
val saldoNuevo = saldoLeido + montoPorOperacion
saldoCompartido = saldoNuevo
```

- `Thread.yield()` se usa solo para aumentar la intercalacion entre hilos. No reemplaza la carrera: la perdida nace del acceso compartido no sincronizado.
- No usar `synchronized`, `Mutex`, `Semaphore`, `AtomicInteger`, `AtomicLong`, `volatile` ni locks en el saldo compartido.
- Usar `AtomicBoolean` solo para senal de cancelacion.
- Reportar progreso de manera limitada para no saturar la UI: actualizar cada 1_000 operaciones por cajero y al finalizar.
- Cerrar siempre el `ExecutorService` con `shutdownNow()` en cancelacion, error, reinicio activo, salida de pantalla o destruccion del `ViewModel`.
- Interrumpir el thread coordinador en `cancelar()` y limpiar su referencia al finalizar.
- Capturar excepciones, cambiar a `Error`, emitir evento y liberar recursos.

## Responsabilidades por archivo

### `EjecutorBancoCaotico.kt`

- Crear y administrar el `ExecutorService`.
- Ejecutar los cajeros concurrentes.
- Mantener el saldo compartido no sincronizado.
- Reportar inicio, progreso, finalizacion, cancelacion y error mediante callbacks.
- Calcular `ResultadoBancoCaotico` al terminar.
- Exponer `cancelar()` y `limpiar()`.
- No conocer Compose, recursos Android ni navegacion.
- No escribir textos visibles.

### `EstadoBancoCaotico.kt`

- Definir modelos, enums y constantes simples del modulo.
- No ejecutar trabajo.
- No depender de Compose.

### `BancoCaoticoViewModel.kt`

- Mantener `EstadoBancoCaotico`.
- Validar configuracion dentro de limites.
- Crear una nueva ejecucion.
- Cancelar ejecucion activa.
- Reiniciar estado cuando no hay ejecucion activa.
- Recibir callbacks del `EjecutorBancoCaotico` y actualizar estado.
- Exponer acciones simples para la pantalla:
  - `actualizarCantidadCajeros(valor: Int)`
  - `actualizarOperacionesPorCajero(valor: Int)`
  - `iniciar()`
  - `iniciarModoRapido()`
  - `cancelar()`
  - `reiniciar()`
  - `limpiarRecursos()`
- No guardar `Context`.
- No crear strings visibles.
- Emitir eventos estructurados `EventoRegistroBancoCaotico` mediante callback recibido en `iniciar(onEvento: (EventoRegistroBancoCaotico) -> Unit)` y `iniciarModoRapido(onEvento: (EventoRegistroBancoCaotico) -> Unit)`.
- Ignorar callbacks de ejecuciones antiguas mediante `idEjecucionActiva`, igual que `CarreraHilosViewModel`.

### `PantallaBancoCaotico.kt`

- Renderizar UI con Jetpack Compose y Material 3.
- Usar `ExperimentoScaffold(titulo = stringResource(R.string.experimento_banco_caotico_nombre), onVolver = ::solicitarSalida, acciones = { BotonRegistroEventos(onClick = registro::abrir) })`.
- Crear `val registro = rememberRegistroExperimento(TAG_BANCO_CAOTICO)`.
- Mostrar `HojaRegistroEventos(visible = registro.visible, eventos = registro.eventos, onCerrar = registro::cerrar)`.
- Registrar eventos recibidos del `ViewModel` en UI y Logcat con `registro.logger.info`, `registro.logger.advertencia` o `registro.logger.error`.
- Llamar `registro.limpiar()` solo antes de `viewModel.iniciar(...)` y antes de `viewModel.iniciarModoRapido(...)`.
- Mostrar controles, visualizacion, metricas, resultado y `Como verificar`.
- Usar `BackHandler` para aplicar la misma regla del boton volver.
- Usar `AlertDialog` al intentar salir con ejecucion activa.
- Usar `DisposableEffect` como limpieza defensiva si la pantalla sale de composicion.
- Usar `@Preview` solo con datos de ejemplo; no iniciar hilos reales en previews.

### `App.kt`

- Importar `PantallaBancoCaotico`.
- Cambiar la ruta `Navegacion.Ruta.BancoCaotico` para llamar a `PantallaBancoCaotico(onVolver = { navController.popBackStack() })`.
- No modificar `Navegacion.kt` ni `PantallaPanelExperimentos.kt` porque la ruta y la tarjeta ya existen.

### `strings.xml`

Agregar textos visibles para:

- Titulo y descripcion corta del modulo.
- Acciones: iniciar, modo rapido, cancelar, reiniciar, volver, salir y cancelar.
- Estados del experimento y de cajeros.
- Etiquetas de configuracion.
- Etiquetas de metricas.
- Resultado esperado, resultado real, diferencia y carrera detectada.
- Mensajes de error y validacion.
- Dialogo de salida con ejecucion activa.
- Descripciones de comandos de verificacion.
- Mensajes de log que la pantalla usa para traducir `EventoRegistroBancoCaotico`.

No hardcodear textos visibles en composables.

## Controles por estado

| Fase | Controles habilitados | Controles deshabilitados | Comportamiento |
|---|---|---|---|
| `Inactivo` | Configuracion, `Iniciar`, `Modo rapido` | `Cancelar` | Permite preparar y empezar una ejecucion. |
| `Ejecutando` | `Cancelar` | Configuracion, `Iniciar`, `Modo rapido`, `Reiniciar` | Muestra progreso y evita doble inicio. |
| `Exitoso` | Configuracion, `Iniciar`, `Modo rapido`, `Reiniciar` | `Cancelar` | Conserva resultado hasta una nueva ejecucion o reinicio. |
| `Cancelado` | Configuracion, `Iniciar`, `Modo rapido`, `Reiniciar` | `Cancelar` | Muestra que la corrida fue cancelada y permite repetir. |
| `Error` | Configuracion, `Iniciar`, `Modo rapido`, `Reiniciar` | `Cancelar` | Muestra error visible y permite reiniciar desde estado controlado. |

Reglas adicionales:

- `Iniciar` limpia el registro de eventos internamente porque crea una ejecucion nueva.
- `Modo rapido` tambien limpia el registro porque crea una ejecucion nueva.
- `Cancelar` no requiere confirmacion porque el usuario lo solicita explicitamente.
- `Reiniciar` no se habilita durante `Ejecutando`.
- No mostrar boton manual `Limpiar registro`.

## Ciclo de vida y limpieza

Inicio de ejecucion:

1. Validar configuracion.
2. Limpiar registro de eventos interno de la pantalla.
3. Cancelar defensivamente cualquier ejecutor anterior.
4. Crear cajeros en estado `Esperando`.
5. Cambiar fase a `Ejecutando`.
6. Iniciar `EjecutorBancoCaotico`.
7. Registrar evento de inicio en UI y Logcat.

Finalizacion exitosa:

1. Recibir resultado del ejecutor.
2. Cambiar fase a `Exitoso`.
3. Guardar `resultadoUltimaEjecucion`.
4. Marcar cajeros como `Finalizado`.
5. Registrar resultado esperado, resultado real, diferencia y deteccion de carrera.
6. Liberar `ExecutorService`.

Cancelacion:

1. Activar bandera de cancelacion.
2. Ejecutar `shutdownNow()`.
3. Marcar fase `Cancelado`.
4. Marcar cajeros no finalizados como `Cancelado`.
5. Registrar evento de cancelacion.
6. Liberar referencias del ejecutor.

Error:

1. Capturar excepcion.
2. Intentar cancelar y liberar executor.
3. Cambiar fase a `Error`.
4. Guardar mensaje visible.
5. Registrar evento de error en UI y Logcat.
6. Permitir reiniciar desde estado controlado.

Salida de pantalla:

- Si `ejecucionActiva == false`, volver inmediatamente.
- Si `ejecucionActiva == true`, mostrar `AlertDialog`.
- Confirmar: cancelar ejecucion, liberar recursos y navegar.
- Cancelar dialogo: cerrar dialogo y permanecer en pantalla.
- El boton volver superior, el boton atras del sistema y el gesto atras deben seguir esta misma regla con `BackHandler`.
- `DisposableEffect` debe llamar limpieza defensiva para que ningun hilo quede activo si la composicion se destruye.

`ViewModel.onCleared()`:

- Llamar `limpiarRecursos()`.
- Cancelar ejecutor activo si existe.

## Registro de eventos

- La pantalla crea y posee el `ExperimentoLogger`.
- Tag estable de Logcat: `TAG_BANCO_CAOTICO`, con valor exacto `OSPlayground/ChaosBank`.
- No crear singleton global.
- No guardar el logger dentro del `ViewModel`.
- No usar `Log.i` o `Log.w` directo en `EjecutorBancoCaotico` ni `BancoCaoticoViewModel`.
- `Log.e` directo solo se permite como fallback tecnico si el evento no puede reportarse por el canal normal.
- Eventos minimos:
  - configuracion aplicada;
  - inicio de ejecucion;
  - inicio de cada cajero;
  - progreso parcial por cajero cada 1_000 operaciones;
  - fin de cada cajero;
  - cancelacion;
  - error;
  - resultado esperado;
  - resultado real;
  - diferencia;
  - carrera detectada o no detectada.

## UI obligatoria

`PantallaBancoCaotico` debe mostrar:

- Nombre tematico: `Banco Caotico`.
- Concepto: `Condicion de carrera`.
- Explicacion corta: varios cajeros actualizan el mismo saldo sin sincronizacion.
- Controles de configuracion:
  - stepper de cantidad de cajeros;
  - `TextField` numerico de operaciones por cajero;
  - boton `Iniciar`;
  - boton `Modo rapido`;
  - boton `Cancelar`;
  - boton `Reiniciar`.
- Visualizacion de cajeros:
  - `LazyColumn` de `CajeroBancoCaotico`;
  - nombre de hilo/cajero;
  - estado;
  - operaciones completadas;
  - ultimo saldo leido y escrito cuando exista.
- Metricas:
  - saldo inicial;
  - cajeros;
  - operaciones por cajero;
  - operaciones totales esperadas;
  - resultado esperado;
  - resultado real;
  - diferencia;
  - duracion;
  - carrera detectada.
- Resultado:
  - mensaje visible cuando `diferencia != 0`;
  - mensaje visible cuando `diferencia == 0` explicando que esa corrida no mostro perdida y se puede repetir.
- Registro de eventos comun.
- Seccion `Como verificar`.
- Boton de registro en la barra superior usando `BotonRegistroEventos`.
- Bottom sheet de registro usando `HojaRegistroEventos`.

La UI debe permanecer responsiva durante toda la ejecucion.

## Como verificar

No ejecutar estos comandos automaticamente durante la implementacion. Deben aparecer en la UI como verificacion manual y poder copiarse.

Desde Windows/PowerShell:

```powershell
adb logcat -d -s OSPlayground/ChaosBank
```

Valida eventos del modulo en Logcat: inicio, progreso, cancelacion, error, resultado esperado, resultado real, diferencia y deteccion de carrera.

```powershell
adb shell top
```

Valida que el experimento genera actividad real de CPU mientras la ejecucion esta activa. Este comando es interactivo; detenerlo manualmente cuando termine la observacion.

```powershell
adb shell dumpsys activity top
```

Valida que la pantalla de la app sigue responsiva y en primer plano durante la demostracion. No prueba por si solo la condicion de carrera.

Comando para revisar logs directos en el codigo cubierto:

```powershell
Select-String -Path android/app/src/main/java/io/yerdna/architecturasos/bancocaotico/*.kt -Pattern 'Log\.i|Log\.w'
```

Debe devolver vacio para `EjecutorBancoCaotico.kt` y `BancoCaoticoViewModel.kt`. Si aparece algun resultado, debe justificarse o reemplazarse por eventos del logger de pantalla.

## Criterios de aceptacion

- [ ] El modulo reemplaza la pantalla temporal de `BancoCaotico`.
- [ ] El codigo nuevo vive en el paquete `io.yerdna.architecturasos.bancocaotico` y la pantalla en `ui/screen`.
- [ ] Todos los textos visibles nuevos estan en `strings.xml`.
- [ ] No se agregan dependencias nuevas.
- [ ] No se modifica el dashboard para mostrar estado global.
- [ ] La ejecucion usa multiples hilos reales.
- [ ] El saldo compartido se modifica con read-modify-write no atomico y sin locks.
- [ ] El error no se simula con numeros aleatorios ni valores falsos.
- [ ] La UI no bloquea el Main Thread.
- [ ] Los controles respetan la matriz de estados.
- [ ] `Iniciar` y `Modo rapido` bloquean doble inicio mientras `Ejecutando`.
- [ ] `Cancelar` detiene hilos y libera executor.
- [ ] Salir con ejecucion activa pide confirmacion con `AlertDialog`.
- [ ] Boton volver superior, atras del sistema y gesto atras aplican la misma confirmacion.
- [ ] `DisposableEffect` y `ViewModel.onCleared()` hacen limpieza defensiva.
- [ ] `Error` intenta limpiar recursos y permite reiniciar.
- [ ] El registro de eventos pertenece a la pantalla y escribe en Logcat con `TAG_BANCO_CAOTICO`.
- [ ] No hay boton manual para limpiar el registro.
- [ ] Las previews usan datos de ejemplo y no arrancan hilos.
- [ ] La seccion `Como verificar` usa comandos compatibles con Windows y explica que valida cada comando.
- [ ] La demostracion puede completarse en 30 a 90 segundos con `Modo rapido`.

## Decisiones resueltas

- El paquete del modulo queda cerrado como `io.yerdna.architecturasos.bancocaotico`, alineado con la ruta real `Navegacion.Ruta.BancoCaotico` y el nombre visible existente en `strings.xml`.
- La pantalla usa los componentes comunes reales: `ExperimentoScaffold`, `rememberRegistroExperimento`, `BotonRegistroEventos` y `HojaRegistroEventos`.
- El logger pertenece a la pantalla y se limpia solo al iniciar una ejecucion nueva con `Iniciar` o `Modo rapido`; no hay boton manual de limpieza.
- El `ViewModel` no usa `Context`, no guarda logger y emite eventos estructurados para que la pantalla los traduzca a strings.
- Los callbacks del ejecutor se publican al Main Thread antes de actualizar estado Compose.
- La coordinacion de `Future` se hace con un thread separado para no bloquear el pool fijo de cajeros.
- `Navegacion.kt` y `PantallaPanelExperimentos.kt` no se modifican porque la ruta y tarjeta ya existen.

## Bloqueos restantes

No quedan bloqueos tecnicos para implementar el plan. La implementacion todavia requiere aprobacion explicita del usuario por la regla de `plans/lessons.md`, pero no queda ninguna decision tecnica abierta dentro del plan.
