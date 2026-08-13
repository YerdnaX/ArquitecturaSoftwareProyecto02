# Plan 009 - Monstruo de Memoria

## Objetivo

Demostrar cambios reales, visibles y controlados en el consumo de memoria de la aplicacion Android mediante reservas de memoria en heap mantenidas por referencias reales a `ByteArray`.

El modulo debe servir para exposicion academica de memoria en Android: la UI explica el concepto, pero las metricas y el crecimiento visual deben salir de memoria realmente reservada por el proceso de la app. No debe provocar `OutOfMemoryError`, ANR ni consumo ilimitado.

## Estructura real del proyecto (corregida)

La raiz real del modulo Android **no** tiene un prefijo `android/`. El proyecto Gradle vive directamente en la raiz del repositorio:

```txt
app/src/main/java/io/yerdna/architecturasos/
app/src/main/res/values/strings.xml
gradlew.bat   (en la raiz del repositorio, sin carpeta android/)
```

Cualquier referencia previa a `android/app/...` o `cd android` es incorrecta para este proyecto y no debe usarse. Todas las rutas de este plan usan la raiz real.

## Depende de

- `001-foundation-dashboard-common.md`
- `011-panel-registro-eventos.md` (ya implementado): logger comun (`ExperimentoLogger`, `EventoExperimento`, `TipoEvento`, `OrigenEvento`) y componentes `rememberRegistroExperimento`, `BotonRegistroEventos`, `HojaRegistroEventos`, `PanelRegistroEventos`. Se usan directamente, sin condicion "si existe".
- Componentes comunes ya existentes:
  - `app/src/main/java/io/yerdna/architecturasos/ui/component/ExperimentoScaffold.kt`
  - `app/src/main/java/io/yerdna/architecturasos/ui/component/PanelRegistroEventos.kt`
  - `app/src/main/java/io/yerdna/architecturasos/ui/component/RegistroExperimento.kt`
  - `app/src/main/java/io/yerdna/architecturasos/util/ExperimentoLogger.kt`
  - `app/src/main/java/io/yerdna/architecturasos/util/EventoExperimento.kt`
- Puede revisar `008-smart-parking-semaphore.md` (ya implementado, modulo `parqueointeligente`) como referencia directa de patron de codigo: `ViewModel` sin `Context`, `Thread` + `Handler(Looper.getMainLooper())` para volver al hilo principal (sin coroutines ni `viewModelScope` en la logica de negocio), sealed class de eventos de registro, y estructura de pantalla con `ExperimentoScaffold`, registro y dialogo de salida.

## Alcance cerrado

- Usar Kotlin, Jetpack Compose, Material 3 y APIs estandar de Android/Kotlin.
- No agregar dependencias nuevas.
- No crear `Service`, proceso secundario, sockets, base de datos, `domain`, `usecase`, `mapper`, `di` ni arquitectura adicional.
- No usar memoria nativa ni archivos temporales para inflar consumo.
- La reserva de memoria (crear el `ByteArray` y tocar cada pagina) se ejecuta siempre en un `Thread` dedicado, nunca en el Main/UI Thread; el resultado se publica al estado mediante `Handler(Looper.getMainLooper()).post { ... }`, igual que `EjecutorParqueoInteligente`. No se usan coroutines ni `viewModelScope` en `MonstruoMemoriaViewModel`.
- No mostrar boton manual para limpiar el registro de eventos.
- No ejecutar `.\gradlew.bat build` automaticamente durante la implementacion; pedir confirmacion si el usuario quiere build.
- La ruta de navegacion existente `Navegacion.Ruta.MonstruoMemoria` se reutiliza. No crear rutas globales adicionales.

## Archivos exactos a crear

- `app/src/main/java/io/yerdna/architecturasos/memoria/EstadoMonstruoMemoria.kt`
- `app/src/main/java/io/yerdna/architecturasos/memoria/MonstruoMemoriaViewModel.kt`
- `app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaMonstruoMemoria.kt`

## Archivos exactos a modificar

- `app/src/main/java/io/yerdna/architecturasos/ui/App.kt`
  - Reemplazar la pantalla temporal de `Navegacion.Ruta.MonstruoMemoria` por `PantallaMonstruoMemoria`.
- `app/src/main/res/values/strings.xml`
  - Agregar solo los textos visibles nuevos del modulo (ver seccion `strings.xml` mas abajo); reutilizar los recursos genericos ya existentes.

## Archivos que no se deben modificar para este plan

- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaPanelExperimentos.kt` (la tarjeta y la ruta del dashboard ya existen)
- Archivos generados dentro de `build/` o `.gradle/`
- Otros planes dentro de `plans/`

## Modelo y nombres de codigo

Crear `EstadoMonstruoMemoria.kt` con modelos simples:

- `const val TAG_MONSTRUO_MEMORIA = "OSPlayground/Memory"`, igual que `TAG_PARQUEO_INTELIGENTE` en `EstadoParqueoInteligente.kt`.
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
- `class BloqueMemoriaReservada` (clase normal, no `data class`: contiene un `ByteArray` y `equals`/`hashCode`/`toString` generados por `data class` serian enganosos)
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
  - `puedeReiniciar: Boolean`
  - `hayTrabajoActivo: Boolean`
- `sealed class EventoRegistroMonstruoMemoria`, siguiendo el patron de `EventoRegistroParqueoInteligente`:
  - `data object AperturaModulo`
  - `data class IntentoReserva(val tamanoMb: Int)`
  - `data class ReservaCompletada(val idBloque: Int, val tamanoMb: Int, val memoriaReservadaMb: Int)`
  - `data class ReservaBloqueada(val tamanoMb: Int, val memoriaReservadaMb: Int)`
  - `data class AdvertenciaPresion(val porcentajeUsado: Int)`
  - `data class LiberacionMemoria(val bloquesLiberados: Int)`
  - `data object SolicitudGc`
  - `data object ReinicioConfirmado`
  - `data object SalidaConfirmada`
  - `data class ErrorTecnico(val mensaje: String?)`

## Responsabilidades

### `MonstruoMemoriaViewModel`

`MonstruoMemoriaViewModel` nunca recibe ni guarda `Context`. Contrato publico exacto:

- `fun reservar(tamanoMb: Int, onEvento: (EventoRegistroMonstruoMemoria) -> Unit)`
- `fun liberarMemoria(onEvento: (EventoRegistroMonstruoMemoria) -> Unit)`
- `fun solicitarGc(onEvento: (EventoRegistroMonstruoMemoria) -> Unit)`
- `fun reiniciar(onEvento: (EventoRegistroMonstruoMemoria) -> Unit)`
- `fun actualizarMedicionSistema(memoriaDisponibleSistemaMb: Long?)`
- `fun limpiarRecursos()`

Responsabilidades:

- Mantener la lista privada de `BloqueMemoriaReservada`.
- `reservar(tamanoMb, onEvento)`:
  - emitir `IntentoReserva(tamanoMb)`;
  - si `memoriaReservadaMb + tamanoMb > limiteSeguroMb`: no crear `ByteArray`, emitir `ReservaBloqueada`, actualizar estado a `Advertencia` con `mensajeAdvertencia`, terminar sin tocar hilos;
  - si pasa la validacion: pasar `estadoEjecucion` a `Reservando`, lanzar un `Thread` dedicado que crea el `ByteArray`, toca cada bloque (escribe al menos un byte cada `4096` bytes, sin loops infinitos) y publica el resultado con `Handler(Looper.getMainLooper()).post { ... }`;
  - al publicar: guardar la referencia, medir memoria, agregar `MuestraMemoria` al historico, recalcular estado visual, y emitir `ReservaCompletada` o, si el resultado ya esta sobre el `80%` del limite, tambien `AdvertenciaPresion`.
- Medir memoria con `Runtime.getRuntime()`:
  - usada: `(totalMemory() - freeMemory())`;
  - libre runtime: `freeMemory()`;
  - maxima runtime: `maxMemory()`.
- `actualizarMedicionSistema(memoriaDisponibleSistemaMb)`: guarda el valor ya calculado por la pantalla en el estado; el `ViewModel` nunca calcula `ActivityManager.MemoryInfo` directamente porque eso requiere `Context`.
- Calcular estado visual y controles habilitados (incluido `puedeReiniciar`) despues de cada cambio de estado.
- Exponer `var estado by mutableStateOf(EstadoMonstruoMemoria()) private set`, siguiendo el patron real de los `ViewModel` ya implementados.
- No guardar el `onEvento` recibido como campo persistente: cada funcion publica lo recibe como parametro y lo invoca directamente o dentro del `Handler.post`, igual que `iniciar(onEvento)` en `ParqueoInteligenteViewModel` pero sin campo `registrarEvento` porque aqui no hay una ejecucion de fondo que sobreviva a la llamada salvo el `Thread` de `reservar`.
- Capturar `OutOfMemoryError` defensivamente dentro del `Thread` de `reservar`:
  - publicar en el hilo principal via `Handler.post`;
  - pasar a `Error`;
  - liberar referencias reservadas;
  - solicitar medicion nueva;
  - reportar mensaje visible;
  - emitir `ErrorTecnico`.
- Limpiar recursos en `liberarMemoria()` y `onCleared()`.

### `PantallaMonstruoMemoria`

- Usar `ExperimentoScaffold`, siguiendo exactamente el patron de `PantallaParqueoInteligente`:
  - crear el `ViewModel` con `remember { MonstruoMemoriaViewModel() }`;
  - crear el registro con `val registro = rememberRegistroExperimento(TAG_MONSTRUO_MEMORIA)`;
  - definir una funcion local `registrarEvento(evento: EventoRegistroMonstruoMemoria)` con un `when` exhaustivo que llama `registro.logger.info/advertencia/error` segun el tipo de evento (info para `AperturaModulo`, `IntentoReserva`, `ReservaCompletada`, `LiberacionMemoria`, `SolicitudGc`, `ReinicioConfirmado`; advertencia para `ReservaBloqueada`, `AdvertenciaPresion`, `SalidaConfirmada`; error para `ErrorTecnico`);
  - antes de la primera reserva de una ejecucion nueva (ver "Nueva ejecucion de experimento"), llamar `registro.limpiar()`;
  - pasar `acciones = { BotonRegistroEventos(onClick = registro::abrir) }` a `ExperimentoScaffold`;
  - renderizar `HojaRegistroEventos(visible = registro.visible, eventos = registro.eventos, onCerrar = registro::cerrar)` junto al contenido principal.
- Al entrar en composicion (primer render), llamar `registrarEvento(EventoRegistroMonstruoMemoria.AperturaModulo)` y medir memoria inicial.
- Calcular `memoriaDisponibleSistemaMb` con una funcion local no-Composable `obtenerMemoriaDisponibleSistemaMb(context: Context): Long?` que usa `LocalContext.current` y `ActivityManager.MemoryInfo`, y pasar el valor ya calculado a `viewModel.actualizarMedicionSistema(...)`. El `ViewModel` nunca recibe `Context`.
- Mostrar:
  - nombre del experimento;
  - concepto tecnico;
  - explicacion corta;
  - visual del monstruo;
  - controles;
  - metricas;
  - historico de sesion;
  - panel de eventos (via `BotonRegistroEventos` + `HojaRegistroEventos`, que internamente usa `PanelRegistroEventos`);
  - seccion `Como verificar`.
- Usar `BackHandler` y boton volver con la misma regla de salida.
- Mostrar `AlertDialog` si el usuario intenta salir mientras `hayTrabajoActivo` es `true`.
- Usar `DisposableEffect` para limpieza defensiva al salir de composicion (llama `viewModel.limpiarRecursos()`).
- No iniciar trabajo real desde `@Preview`; las previews deben usar datos de ejemplo.

### `App.kt`

- Importar `PantallaMonstruoMemoria`.
- En `composable(Navegacion.Ruta.MonstruoMemoria)`, llamar:
  - `PantallaMonstruoMemoria(onVolver = { navController.popBackStack() })`

### `strings.xml`

Reutilizar directamente estos recursos ya existentes (no duplicarlos): `experimento_monstruo_memoria_nombre`, `experimento_monstruo_memoria_concepto`, `experimento_monstruo_memoria_descripcion` (lineas 37-39), `estado_inactivo`, `estado_error`, `estado_exitoso`, `accion_cancelar`, `accion_continuar`, `accion_copiar`, `accion_reiniciar`, `accion_volver`, `resultado_ultima_ejecucion`, `resultado_sin_ejecucion`, `seccion_como_verificar`, mas los recursos genericos de `registro_eventos_*`, `tipo_evento_*` y `origen_evento_*` ya usados por `PanelRegistroEventos`.

Crear unicamente los recursos especificos del modulo:

- Estados: `estado_reservando`, `estado_advertencia`, `estado_liberado`, `estado_recolector_solicitado`.
- Estados visuales: `estado_visual_saludable`, `estado_visual_moderado`, `estado_visual_alto`, `estado_visual_presion_memoria`.
- Acciones: `accion_reservar_10mb`, `accion_reservar_25mb`, `accion_reservar_50mb`, `accion_liberar_memoria`, `accion_solicitar_gc`, `accion_salir_y_liberar` (mismo patron que `accion_salir_y_cancelar`).
- Dialogo de salida: `dialogo_salir_monstruo_memoria_titulo`, `dialogo_salir_monstruo_memoria_mensaje` (mismo patron que `dialogo_salir_parqueo_inteligente_*`).
- Metricas y etiquetas: textos con prefijo `monstruo_memoria_...` para cada metrica obligatoria (memoria usada, maxima runtime, libre runtime, disponible del sistema, reservada, bloques, limite seguro, porcentaje usado), mensajes de advertencia y error especificos del modulo.
- Verificacion: `verificacion_meminfo_monstruo_memoria`, `verificacion_top_monstruo_memoria`, `verificacion_logcat_monstruo_memoria`, `verificacion_logcat_clear_monstruo_memoria` (mismo patron que `verificacion_logcat_parqueo_inteligente`).
- Textos de eventos de registro necesarios para `registrarEvento(...)` en la pantalla (uno por caso de `EventoRegistroMonstruoMemoria`, mismo patron que `log_parqueo_inteligente_*`).

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
  - los botones de reserva permanecen habilitados (no se deshabilitan por tamano); una reserva que excederia `limiteSeguroMb` se bloquea en tiempo de ejecucion dentro de `reservar()`: no crea `ByteArray`, emite `ReservaBloqueada` y pasa el estado a `Advertencia` con `mensajeAdvertencia` visible.
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
| `Exitoso` | Reserva completada bajo 80% del limite | Reservar 10/25/50 MB (el bloqueo por `limiteSeguroMb` se evalua en tiempo de ejecucion dentro de `reservar()`, no deshabilitando botones), liberar, solicitar GC, reiniciar, volver con dialogo | `Reservando`, `Liberado`, `RecolectorSolicitado` |
| `Advertencia` | Reserva completada desde 80% del limite seguro o intento bloqueado | Reservar 10/25/50 MB (mismo criterio que en `Exitoso`), liberar, solicitar GC, reiniciar, volver con dialogo | `Reservando`, `Liberado`, `RecolectorSolicitado`, `Error` |
| `Error` | Error recuperable u `OutOfMemoryError` capturado | Reservar desde estado limpio, solicitar GC, reiniciar, volver sin dialogo (`hayTrabajoActivo` es siempre `false` en `Error`, porque las referencias ya se liberaron) | `Reservando`, `RecolectorSolicitado`, `Inactivo` |
| `Liberado` | Usuario libera referencias o confirma salida | Reservar 10/25/50 MB, solicitar GC, reiniciar, volver sin dialogo | `Reservando`, `RecolectorSolicitado` |
| `RecolectorSolicitado` | Usuario solicita GC | Reservar 10/25/50 MB (mismo criterio que en `Exitoso`), liberar si quedan bloques, reiniciar, volver con dialogo si quedan bloques | `Reservando`, `Liberado`, `Exitoso`, `Advertencia` |

`hayTrabajoActivo` debe ser `true` cuando existan bloques reservados o el estado sea `Reservando`. Debe ser `false` cuando no queden bloques y no haya reserva en curso.

`Reiniciar` (`puedeReiniciar`) esta disponible en todos los estados excepto `Reservando`, independientemente de si hay bloques activos; ver "Controles por estado".

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

- `Reservar 10 MB`, `Reservar 25 MB`, `Reservar 50 MB`
  - `puedeReservar10Mb`, `puedeReservar25Mb` y `puedeReservar50Mb` se calculan todos con la misma formula (`estadoEjecucion != Reservando`); no se deshabilitan por tamano de la reserva.
  - al tocarlos, `reservar(tamanoMb, onEvento)` valida el limite en tiempo de ejecucion; ver "Umbral de bloqueo".
- `Liberar memoria`
  - habilitado si existe al menos un bloque reservado y `estadoEjecucion` no es `Reservando`.
  - libera todas las referencias del experimento.
- `Solicitar GC`
  - habilitado si `estadoEjecucion` no es `Reservando`.
  - llama `System.gc()` solo como demostracion y muestra texto visible indicando que Android decide cuando recolectar.
- `Reiniciar`
  - habilitado si `estadoEjecucion` no es `Reservando` (sin exigir bloques activos, a diferencia de `Liberar memoria`).
  - limpia bloques, historico, `resultadoUltimaAccion`, `mensajeError` y `mensajeAdvertencia`; vuelve a `Inactivo`; mide memoria despues; emite `ReinicioConfirmado`.
- `Volver`
  - si `hayTrabajoActivo` es `false`, navega inmediatamente.
  - si `hayTrabajoActivo` es `true`, muestra `AlertDialog`.

## Ciclo de vida y limpieza

- Al abrir pantalla:
  - crear estado inicial;
  - medir memoria actual;
  - agregar muestra al historico;
  - mostrar registro vacio;
  - registrar evento `AperturaModulo`.
- Al reservar:
  - validar limite;
  - registrar evento `IntentoReserva`;
  - reservar `ByteArray`;
  - tocar el bloque;
  - guardar referencia;
  - medir memoria;
  - agregar muestra al historico;
  - actualizar estado visual;
  - registrar evento `ReservaCompletada` (y `AdvertenciaPresion` si aplica).
- Al bloquear reserva insegura:
  - no crear `ByteArray`;
  - mantener referencias existentes;
  - medir memoria;
  - agregar muestra al historico;
  - mostrar advertencia visible;
  - registrar evento `ReservaBloqueada`.
- Al liberar:
  - limpiar lista de bloques;
  - medir memoria antes y despues de liberar referencias;
  - agregar muestra al historico con la medicion posterior a liberar;
  - no depender de que GC ocurra inmediatamente;
  - registrar evento `LiberacionMemoria`.
- Al solicitar GC:
  - llamar `System.gc()`;
  - medir despues de la solicitud;
  - agregar muestra al historico;
  - mostrar mensaje: Android decide cuando recolectar;
  - registrar evento `SolicitudGc`.
- Al reiniciar:
  - limpiar lista de bloques, historico, `resultadoUltimaAccion`, `mensajeError` y `mensajeAdvertencia`;
  - volver a `Inactivo`;
  - medir memoria;
  - agregar muestra al historico;
  - registrar evento `ReinicioConfirmado`.
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

- Tag estable: `TAG_MONSTRUO_MEMORIA` (`"OSPlayground/Memory"`), definido en `EstadoMonstruoMemoria.kt`.
- La pantalla es dueña del logger (`rememberRegistroExperimento(TAG_MONSTRUO_MEMORIA)`).
- El `ViewModel` emite eventos hacia la pantalla mediante el parametro `onEvento: (EventoRegistroMonstruoMemoria) -> Unit` que recibe cada funcion publica (ver "Responsabilidades > `MonstruoMemoriaViewModel`"). No se usa `Flow`, `LiveData` ni ningun otro mecanismo.
- Registrar en UI y Logcat, uno por cada caso de `EventoRegistroMonstruoMemoria`:
  - `AperturaModulo`: apertura del modulo;
  - `IntentoReserva`: intento de reserva;
  - `ReservaCompletada`: reserva completada;
  - `ReservaBloqueada`: reserva bloqueada por limite seguro;
  - `AdvertenciaPresion`: advertencia de presion de memoria;
  - `LiberacionMemoria`: liberacion de referencias;
  - `SolicitudGc`: solicitud de GC;
  - `ReinicioConfirmado`: reinicio del modulo;
  - `SalidaConfirmada`: salida con limpieza;
  - `ErrorTecnico`: errores recuperados.
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

No ejecutar build automaticamente para este plan. Si se requiere compilar, pedir confirmacion antes de ejecutar desde la raiz del repositorio (no existe carpeta `android/`):

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
Select-String -Path app/src/main/java/io/yerdna/architecturasos/memoria/*.kt,app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaMonstruoMemoria.kt -Pattern "Log\.i|Log\.w|Log\.d|grep|OutOfMemoryError"
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
- [ ] Los botones de reserva permanecen habilitados y una reserva que superaria `limiteSeguroMb` se bloquea en tiempo de ejecucion (sin crear `ByteArray`), pasando el estado a `Advertencia`.
- [ ] Al llegar a `80%` del limite seguro, la UI muestra advertencia clara.
- [ ] `Liberar memoria` elimina referencias y deja el modulo listo para otra demostracion.
- [ ] `Solicitar GC` existe, registra evento y aclara que Android decide cuando recolectar.
- [ ] `Reiniciar` existe, esta habilitado siempre que `estadoEjecucion` no sea `Reservando`, limpia bloques/historico/mensajes y vuelve a `Inactivo`.
- [ ] El historico agrega una muestra en cada medicion (apertura, reserva, bloqueo, liberacion, GC, reinicio) y mantiene maximo `30` muestras de la sesion.
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

## Decisiones resueltas

- **Alcance academico de paginacion y memoria virtual**: el codigo se limita a reservas controladas de heap con `ByteArray`, medicion con `Runtime` y `ActivityManager.MemoryInfo`, y verificacion externa con Android Profiler y `dumpsys meminfo`. La UI agrega una explicacion breve localizada de que Android administra memoria virtual y paginacion internamente, y que este experimento demuestra el efecto observable desde la app sin intentar controlar paginas directamente. (Aprobada por el usuario.)
- **Raiz real del proyecto**: no existe carpeta `android/`; todas las rutas de archivo y el comando `gradlew.bat` se ejecutan desde la raiz del repositorio (ver "Estructura real del proyecto (corregida)").
- **Patron de registro de eventos**: se usa el patron cerrado por `011-panel-registro-eventos.md` y ya aplicado en los 7 modulos existentes: `rememberRegistroExperimento(TAG_MONSTRUO_MEMORIA)`, `ExperimentoScaffold(acciones = { BotonRegistroEventos(onClick = registro::abrir) })`, `HojaRegistroEventos(...)` y `registro.limpiar()` al iniciar una ejecucion nueva. No se usa `PanelRegistroEventos` directamente desde la pantalla.
- **Modelo de eventos del `ViewModel`**: `sealed class EventoRegistroMonstruoMemoria` con casos concretos (`AperturaModulo`, `IntentoReserva`, `ReservaCompletada`, `ReservaBloqueada`, `AdvertenciaPresion`, `LiberacionMemoria`, `SolicitudGc`, `ReinicioConfirmado`, `SalidaConfirmada`, `ErrorTecnico`), pasado como parametro `onEvento` en cada funcion publica del `ViewModel`. No se usan callbacks genericos ni `Flow`.
- **`Context` y medicion de memoria del sistema**: `MonstruoMemoriaViewModel` nunca recibe `Context`. La pantalla calcula `ActivityManager.MemoryInfo` con `LocalContext.current` en una funcion local no-Composable y pasa el valor ya calculado (`Long?`) a `viewModel.actualizarMedicionSistema(...)`. No se crea un `Controlador...` porque no hay `Service`/`Messenger` involucrado.
- **Hilo de ejecucion de la reserva**: `reservar()` lanza un `Thread` dedicado (crear `ByteArray` + tocar paginas) y publica el resultado con `Handler(Looper.getMainLooper()).post { ... }`, igual que `EjecutorParqueoInteligente`. No se usan coroutines ni `viewModelScope` en el `ViewModel`.
- **`BloqueMemoriaReservada`**: se declara como `class` normal, no `data class`, porque contiene un `ByteArray`.
- **Botones de reserva vs. bloqueo por limite seguro**: los botones `Reservar 10/25/50 MB` quedan habilitados siempre que `estadoEjecucion` no sea `Reservando`; el bloqueo por `limiteSeguroMb` ocurre en tiempo de ejecucion dentro de `reservar()` (no crea `ByteArray`, emite `ReservaBloqueada`, pasa a `Advertencia`), no deshabilitando el boton.
- **Estado `Error`**: `hayTrabajoActivo` es siempre `false` en `Error` porque las referencias ya se liberaron; `Volver` nunca muestra dialogo desde `Error`.
- **Boton `Reiniciar`**: existe como boton separado (requerido por `docs/app-requirements.md`), habilitado siempre que `estadoEjecucion` no sea `Reservando` (sin exigir bloques activos), reutilizando `R.string.accion_reiniciar`. Limpia bloques, historico, `resultadoUltimaAccion`, `mensajeError` y `mensajeAdvertencia`, vuelve a `Inactivo` y mide memoria despues.
- **Historico de memoria**: toda medicion (apertura, reservar, bloqueo de reserva, liberar, solicitar GC, reiniciar) agrega una `MuestraMemoria`, respetando el maximo de `30`.
- **Comando `Select-String` de verificacion**: corregido a una sola barra invertida (`Log\.i|Log\.w|Log\.d|grep|OutOfMemoryError`) y a rutas sin prefijo `android/`.
- **Recursos de `strings.xml`**: se reutilizan los recursos genericos ya existentes (`experimento_monstruo_memoria_*`, `estado_inactivo`, `estado_error`, `estado_exitoso`, `accion_cancelar`, `accion_continuar`, `accion_copiar`, `accion_reiniciar`, `accion_volver`, `resultado_ultima_ejecucion`, `resultado_sin_ejecucion`, `seccion_como_verificar`, `registro_eventos_*`, `tipo_evento_*`, `origen_evento_*`); solo se crean los recursos especificos del modulo listados en la seccion `strings.xml`.

## Bloqueos restantes

Ninguno. No queda ninguna decision abierta que requiera respuesta del usuario antes de implementar este plan.