# Plan 009 - Monstruo de Memoria

## Objetivo

Demostrar cambios reales, visibles y controlados en el consumo de memoria de la aplicacion Android mediante reservas de memoria en heap mantenidas por referencias reales a `ByteArray`.

El modulo debe servir para exposicion academica de memoria en Android: la UI explica el concepto, pero las metricas y el crecimiento visual deben salir de memoria realmente reservada por el proceso de la app. No debe provocar `OutOfMemoryError`, ANR ni consumo ilimitado.

## Depende de

- `001-foundation-dashboard-common.md`
- Componentes comunes ya existentes:
  - `android/app/src/main/java/io/yerdna/architecturasos/ui/component/ExperimentoScaffold.kt`
  - `android/app/src/main/java/io/yerdna/architecturasos/ui/component/PanelRegistroEventos.kt`
  - `android/app/src/main/java/io/yerdna/architecturasos/util/ExperimentoLogger.kt`
  - `android/app/src/main/java/io/yerdna/architecturasos/util/EventoExperimento.kt`

## Alcance cerrado

- Usar Kotlin, Jetpack Compose, Material 3 y APIs estandar de Android/Kotlin.
- No agregar dependencias nuevas.
- No crear `Service`, proceso secundario, sockets, base de datos, `domain`, `usecase`, `mapper`, `di` ni arquitectura adicional.
- No usar memoria nativa ni archivos temporales para inflar consumo.
- No reservar memoria en el Main/UI Thread si la operacion puede congelar la UI.
- No mostrar boton manual para limpiar el registro de eventos.
- No ejecutar `.\gradlew.bat build` automaticamente durante la implementacion; pedir confirmacion si el usuario quiere build.
- La ruta de navegacion existente `Navegacion.Ruta.MonstruoMemoria` se reutiliza. No crear rutas globales adicionales.

## Archivos exactos a crear

- `android/app/src/main/java/io/yerdna/architecturasos/memoria/EstadoMonstruoMemoria.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/memoria/MonstruoMemoriaViewModel.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaMonstruoMemoria.kt`

## Archivos exactos a modificar

- `android/app/src/main/java/io/yerdna/architecturasos/ui/App.kt`
  - Reemplazar la pantalla temporal de `Navegacion.Ruta.MonstruoMemoria` por `PantallaMonstruoMemoria`.
- `android/app/src/main/res/values/strings.xml`
  - Agregar todos los textos visibles del modulo, acciones, estados, metricas, dialogos, secciones y descripciones de verificacion.

## Archivos que no se deben modificar para este plan

- `android/app/build.gradle.kts`
- `android/gradle/libs.versions.toml`
- `android/app/src/main/AndroidManifest.xml`
- Archivos generados dentro de `build/` o `.gradle/`
- Otros planes dentro de `plans/`

## Modelo y nombres de codigo

Crear `EstadoMonstruoMemoria.kt` con modelos simples:

- `enum class EstadoEjecucionMemoria`
  - `Inactivo`
  - `Reservando`
  - `Exitoso`
  - `Advertencia`
  - `Error`
  - `Liberado`
  - `RecolectorSolicitado`
- `enum class EstadoVisualMonstruo`
  - `Saludable`
  - `Moderado`
  - `Alto`
  - `PresionMemoria`
- `data class BloqueMemoriaReservada`
  - `id: Int`
  - `tamanoMb: Int`
  - `bytes: ByteArray`
  - `timestamp: Long`
- `data class MuestraMemoria`
  - `timestamp: Long`
  - `memoriaUsadaMb: Long`
  - `memoriaLibreRuntimeMb: Long`
  - `memoriaMaximaRuntimeMb: Long`
  - `memoriaDisponibleSistemaMb: Long?`
  - `bloquesReservados: Int`
- `data class EstadoMonstruoMemoria`
  - `estadoEjecucion: EstadoEjecucionMemoria`
  - `estadoVisual: EstadoVisualMonstruo`
  - `memoriaUsadaMb: Long`
  - `memoriaLibreRuntimeMb: Long`
  - `memoriaMaximaRuntimeMb: Long`
  - `memoriaDisponibleSistemaMb: Long?`
  - `memoriaReservadaMb: Int`
  - `bloquesReservados: Int`
  - `limiteSeguroMb: Int`
  - `historico: List<MuestraMemoria>`
  - `resultadoUltimaAccion: String?`
  - `mensajeAdvertencia: String?`
  - `mensajeError: String?`
  - `puedeReservar10Mb: Boolean`
  - `puedeReservar25Mb: Boolean`
  - `puedeReservar50Mb: Boolean`
  - `puedeLiberar: Boolean`
  - `puedeSolicitarGc: Boolean`
  - `hayTrabajoActivo: Boolean`

## Responsabilidades

### `MonstruoMemoriaViewModel`

- Mantener la lista privada de `BloqueMemoriaReservada`.
- Reservar memoria real creando `ByteArray` y conservando referencias.
- Tocar cada bloque reservado para forzar asignacion observable:
  - escribir al menos un byte por pagina aproximada usando paso de `4096` bytes;
  - no ejecutar loops infinitos.
- Medir memoria con `Runtime.getRuntime()`:
  - usada: `(totalMemory() - freeMemory())`;
  - libre runtime: `freeMemory()`;
  - maxima runtime: `maxMemory()`.
- Medir memoria disponible del sistema con `ActivityManager.MemoryInfo` cuando la pantalla entregue `Context` mediante una funcion de refresco segura.
- Calcular estado visual y controles habilitados.
- Exponer `var estado by mutableStateOf(EstadoMonstruoMemoria()) private set`, siguiendo el patron real de los `ViewModel` ya implementados.
- Registrar eventos usando callbacks hacia la pantalla; el `ViewModel` no debe guardar `Context` ni logger de UI.
- Capturar `OutOfMemoryError` defensivamente:
  - pasar a `Error`;
  - liberar referencias reservadas;
  - solicitar medicion nueva;
  - reportar mensaje visible;
  - registrar evento de error.
- Limpiar recursos en `liberarMemoria()` y `onCleared()`.

### `PantallaMonstruoMemoria`

- Usar `ExperimentoScaffold`.
- Crear y poseer el `ExperimentoLogger` de pantalla con tag `OSPlayground/Memory`.
- Conectar eventos del `ViewModel` con el logger de pantalla.
- Mostrar:
  - nombre del experimento;
  - concepto tecnico;
  - explicacion corta;
  - visual del monstruo;
  - controles;
  - metricas;
  - historico de sesion;
  - panel de eventos;
  - seccion `Como verificar`.
- Usar `PanelRegistroEventos` para el registro interno.
- Usar `BackHandler` y boton volver con la misma regla de salida.
- Mostrar `AlertDialog` si el usuario intenta salir mientras hay memoria reservada.
- Usar `DisposableEffect` para limpieza defensiva al salir de composicion.
- No iniciar trabajo real desde `@Preview`; las previews deben usar datos de ejemplo.

### `App.kt`

- Importar `PantallaMonstruoMemoria`.
- En `composable(Navegacion.Ruta.MonstruoMemoria)`, llamar:
  - `PantallaMonstruoMemoria(onVolver = { navController.popBackStack() })`

### `strings.xml`

Agregar recursos para:

- titulo y descripcion corta;
- acciones `Reservar 10 MB`, `Reservar 25 MB`, `Reservar 50 MB`, `Liberar memoria`, `Solicitar GC`, `Salir y liberar`, `Continuar`;
- estados de ejecucion;
- estados visuales;
- metricas;
- advertencias;
- errores;
- dialogo de salida;
- seccion `Como verificar`;
- descripciones de cada comando.

## Reglas de memoria

- Bloques permitidos:
  - `10 MB`
  - `25 MB`
  - `50 MB`
- Unidad:
  - `1 MB = 1024 * 1024 bytes`
- Limite seguro:
  - `limiteSeguroMb = minOf(200, maxMemoryMb / 2)`
- Margen obligatorio antes de reservar:
  - una reserva solo se permite si `memoriaReservadaMb + tamanoSolicitadoMb <= limiteSeguroMb`.
- Umbral de advertencia:
  - mostrar advertencia desde `80%` del `limiteSeguroMb`.
- Umbral de bloqueo:
  - deshabilitar botones que excedan el `limiteSeguroMb`.
- Historico:
  - guardar maximo `30` muestras por sesion;
  - eliminar la muestra mas antigua cuando se exceda el limite.
- Nueva ejecucion de experimento:
  - la primera reserva despues de estado `Inactivo`, `Liberado`, `Error` o `RecolectorSolicitado` inicia una ejecucion nueva;
  - al iniciar una ejecucion nueva, la pantalla debe llamar `ExperimentoLogger.limpiar()` antes de registrar el primer evento nuevo;
  - no mostrar boton manual `Limpiar` en UI.

## Estados y transiciones

| Estado | Entrada | Acciones permitidas | Salida |
|---|---|---|---|
| `Inactivo` | Pantalla recien abierta o memoria liberada sin bloques | Reservar 10/25/50 MB, solicitar GC, volver sin dialogo | `Reservando`, `RecolectorSolicitado` |
| `Reservando` | Usuario toca una reserva permitida | Sin reservas adicionales, sin liberar hasta terminar la accion | `Exitoso`, `Advertencia`, `Error` |
| `Exitoso` | Reserva completada bajo 80% del limite | Reservar dentro del limite, liberar, solicitar GC, volver con dialogo | `Reservando`, `Liberado`, `RecolectorSolicitado` |
| `Advertencia` | Reserva completada desde 80% del limite seguro o intento bloqueado | Reservar solo botones aun seguros, liberar, solicitar GC, volver con dialogo | `Reservando`, `Liberado`, `RecolectorSolicitado`, `Error` |
| `Error` | Error recuperable u `OutOfMemoryError` capturado | Reservar desde estado limpio, solicitar GC, volver sin dialogo si no quedan bloques | `Reservando`, `RecolectorSolicitado`, `Inactivo` |
| `Liberado` | Usuario libera referencias o confirma salida | Reservar 10/25/50 MB, solicitar GC, volver sin dialogo | `Reservando`, `RecolectorSolicitado` |
| `RecolectorSolicitado` | Usuario solicita GC | Reservar dentro del limite, liberar si quedan bloques, volver con dialogo si quedan bloques | `Reservando`, `Liberado`, `Exitoso`, `Advertencia` |

`hayTrabajoActivo` debe ser `true` cuando existan bloques reservados o el estado sea `Reservando`. Debe ser `false` cuando no queden bloques y no haya reserva en curso.

## Estado visual del monstruo

Calcular sobre `memoriaReservadaMb / limiteSeguroMb`:

- `Saludable`: `0%` a `24%`
- `Moderado`: `25%` a `49%`
- `Alto`: `50%` a `79%`
- `PresionMemoria`: `80%` a `100%`

La UI debe cambiar visualmente el personaje con Material 3 y Compose sin agregar assets obligatorios. Puede usar formas, color, escala y texto localizado. La visualizacion no reemplaza la reserva real; siempre debe estar vinculada a `memoriaReservadaMb` y `estadoVisual`.

## Metricas obligatorias

Mostrar al menos:

- Memoria usada por la app en MB.
- Memoria maxima del runtime en MB.
- Memoria libre del runtime en MB.
- Memoria disponible del sistema en MB cuando `ActivityManager.MemoryInfo` la entregue.
- Memoria reservada por el experimento en MB.
- Cantidad de bloques reservados.
- Limite seguro calculado en MB.
- Porcentaje usado del limite seguro.
- Estado visual actual.
- Resultado de la ultima accion.

Separar:

- `estadoEjecucion`: situacion actual de la pantalla.
- `resultadoUltimaAccion`: resultado historico de la ultima accion del usuario.
- metricas: numeros medidos o derivados.

No convertir metricas en estados.

## Controles por estado

- `Reservar 10 MB`
  - habilitado solo si no esta `Reservando` y la reserva no supera `limiteSeguroMb`.
- `Reservar 25 MB`
  - habilitado solo si no esta `Reservando` y la reserva no supera `limiteSeguroMb`.
- `Reservar 50 MB`
  - habilitado solo si no esta `Reservando` y la reserva no supera `limiteSeguroMb`.
- `Liberar memoria`
  - habilitado si existe al menos un bloque reservado y no esta `Reservando`.
  - libera todas las referencias del experimento.
- `Solicitar GC`
  - habilitado si no esta `Reservando`.
  - llama `System.gc()` solo como demostracion y muestra texto visible indicando que Android decide cuando recolectar.
- `Volver`
  - si `hayTrabajoActivo` es `false`, navega inmediatamente.
  - si `hayTrabajoActivo` es `true`, muestra `AlertDialog`.
- `Reset`
  - no crear boton separado si `Liberar memoria` deja el modulo estable para otra demostracion.

## Ciclo de vida y limpieza

- Al abrir pantalla:
  - crear estado inicial;
  - medir memoria actual;
  - mostrar registro vacio.
- Al reservar:
  - validar limite;
  - registrar evento de intento;
  - reservar `ByteArray`;
  - tocar el bloque;
  - guardar referencia;
  - medir memoria;
  - agregar muestra al historico;
  - actualizar estado visual;
  - registrar evento de exito o advertencia.
- Al bloquear reserva insegura:
  - no crear `ByteArray`;
  - mantener referencias existentes;
  - mostrar advertencia visible;
  - registrar advertencia.
- Al liberar:
  - limpiar lista de bloques;
  - medir memoria antes y despues de liberar referencias;
  - no depender de que GC ocurra inmediatamente;
  - registrar evento de liberacion.
- Al solicitar GC:
  - llamar `System.gc()`;
  - medir despues de la solicitud;
  - mostrar mensaje: Android decide cuando recolectar;
  - registrar evento.
- Al salir con memoria reservada:
  - mostrar `AlertDialog`;
  - si confirma, liberar referencias antes de `onVolver`;
  - si cancela, cerrar dialogo y permanecer en pantalla.
- Al salir por boton del sistema o gesto atras:
  - aplicar la misma regla mediante `BackHandler`.
- En `DisposableEffect`:
  - llamar limpieza defensiva para liberar referencias si la pantalla sale de composicion.
- En `onCleared()`:
  - limpiar referencias como respaldo.

## Registro de eventos

- Tag estable: `OSPlayground/Memory`.
- La pantalla es dueña del logger.
- El `ViewModel` emite eventos hacia la pantalla mediante callbacks o un flujo simple de eventos.
- Registrar en UI y Logcat:
  - apertura del modulo;
  - intento de reserva;
  - reserva completada;
  - reserva bloqueada por limite seguro;
  - advertencia de presion de memoria;
  - liberacion de referencias;
  - solicitud de GC;
  - salida con limpieza;
  - errores recuperados.
- No duplicar logs directos normales si se usa `ExperimentoLogger`.
- `Log.e` directo solo queda permitido como fallback tecnico si no se puede reportar por el canal normal.

## Interfaz

- Primera pantalla real del modulo, sin landing page.
- Usar una jerarquia simple:
  - encabezado breve con concepto;
  - visual del monstruo;
  - controles;
  - metricas;
  - historico;
  - registro de eventos;
  - `Como verificar`.
- Usar `LazyColumn` si el contenido completo puede exceder la pantalla.
- Usar componentes Material 3 existentes.
- Todo texto visible nuevo debe estar en `strings.xml`.
- La UI debe permanecer responsiva en reservas repetidas.
- Las previews deben cubrir:
  - estado `Inactivo`;
  - estado `Exitoso`;
  - estado `Advertencia`;
  - estado `Error`.

## Como verificar

No ejecutar build automaticamente para este plan. Si se requiere compilar, pedir confirmacion antes de ejecutar desde `android/`:

```powershell
.\gradlew.bat build
```

Verificaciones manuales que la UI debe mostrar con descripcion copiable:

```powershell
adb shell dumpsys meminfo io.yerdna.architecturasos
```

Valida el consumo de memoria reportado por Android para el paquete. Ejecutarlo antes de reservar, despues de reservar varios bloques y despues de liberar referencias.

```powershell
adb shell top
```

Permite observar el proceso de la app y su actividad general durante la demostracion. No reemplaza Android Profiler para validar el crecimiento de heap.

```powershell
adb logcat -d -s OSPlayground/Memory
```

Muestra el buffer actual de eventos del modulo: reservas, bloqueos por limite, liberacion, solicitud de GC y errores.

```powershell
adb logcat -c
```

Limpia el buffer de Logcat antes de una demostracion manual. Es opcional y no limpia el registro interno de la app.

Android Studio Profiler > Memory:

- iniciar captura antes de reservar;
- tocar `Reservar 10 MB`, `Reservar 25 MB` o `Reservar 50 MB`;
- observar crecimiento de memoria;
- tocar `Liberar memoria`;
- tocar `Solicitar GC`;
- explicar que la liberacion observada depende del recolector de Android.

Verificacion de codigo sin build:

```powershell
Select-String -Path android/app/src/main/java/io/yerdna/architecturasos/memoria/*.kt,android/app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaMonstruoMemoria.kt -Pattern "Log\\.i|Log\\.w|Log\\.d|grep|OutOfMemoryError"
```

Interpreta resultados asi:

- no deben quedar `Log.i`, `Log.w` o `Log.d` directos en el modulo si el logger comun cubre el evento;
- `OutOfMemoryError` debe aparecer solo como manejo defensivo;
- `grep` no debe aparecer en comandos visibles porque la verificacion debe ser compatible con Windows.

## Criterios de aceptacion

- [ ] `PantallaMonstruoMemoria` reemplaza la pantalla temporal para `Navegacion.Ruta.MonstruoMemoria`.
- [ ] Los botones `Reservar 10 MB`, `Reservar 25 MB` y `Reservar 50 MB` reservan memoria real con `ByteArray` y conservan referencias.
- [ ] Cada bloque reservado se toca en memoria para que la reserva sea observable.
- [ ] La pantalla muestra memoria usada, memoria maxima, memoria libre runtime, memoria disponible del sistema cuando aplique, memoria reservada, cantidad de bloques, limite seguro, porcentaje usado y estado visual.
- [ ] El estado visual cambia entre `Saludable`, `Moderado`, `Alto` y `PresionMemoria` segun los umbrales definidos.
- [ ] La app bloquea reservas que superen `limiteSeguroMb`.
- [ ] Al llegar a `80%` del limite seguro, la UI muestra advertencia clara.
- [ ] `Liberar memoria` elimina referencias y deja el modulo listo para otra demostracion.
- [ ] `Solicitar GC` existe, registra evento y aclara que Android decide cuando recolectar.
- [ ] El historico mantiene maximo `30` muestras de la sesion.
- [ ] Salir con memoria reservada muestra confirmacion para boton volver, boton atras del sistema y gesto atras.
- [ ] Confirmar salida libera referencias antes de navegar.
- [ ] Cancelar salida mantiene la pantalla y las referencias actuales.
- [ ] `DisposableEffect` y `onCleared()` hacen limpieza defensiva.
- [ ] Los eventos relevantes aparecen en el panel interno y en Logcat con tag `OSPlayground/Memory`.
- [ ] No hay boton manual para limpiar el registro de eventos.
- [ ] Todo texto visible nuevo vive en `strings.xml`.
- [ ] Las previews no reservan memoria real ni inician recursos runtime.
- [ ] No se agregan dependencias ni cambios de Gradle.
- [ ] La verificacion documentada usa comandos compatibles con Windows y explica que valida cada comando.

## Items pendientes para ejecutar sin asumir decisiones

### 1. Alcance academico exacto de paginacion y memoria virtual

- Punto: el PDF original exige abordar "Gestion de Memoria Virtual y Paginacion", mientras que `docs/app-requirements.md` define para Memory Monster una manipulacion controlada del consumo de memoria de la aplicacion observable con Android Profiler y `dumpsys meminfo`.
- Por que bloquea o puede causar implementaciones distintas: un agente podria implementar solo reservas de heap administrado por ART, o podria intentar explicar/manipular paginacion, page faults, memoria nativa o `/proc`, cambiando el alcance tecnico y aumentando riesgo de complejidad o inestabilidad.
- Solucion concreta alineada con `AGENTS.md`, `lessons.md` y `docs/`: cerrar el alcance del codigo a reservas controladas de heap con `ByteArray`, medicion con `Runtime` y `ActivityManager.MemoryInfo`, y verificacion externa con Android Profiler y `dumpsys meminfo`. Agregar en la UI una explicacion breve localizada de que Android administra memoria virtual y paginacion internamente, y que este experimento demuestra el efecto observable desde la app sin intentar controlar paginas directamente.

> Aprobada sugerencia