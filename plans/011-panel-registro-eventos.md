# Plan 011 - Panel Registro Eventos

## Objetivo

Crear el componente comun de registro de eventos para que cada pantalla de experimento mantenga su propio `ExperimentoLogger`, pueda ver sus eventos en un bottom sheet desde la barra superior y envie los mismos eventos a Logcat con el tag del experimento.

Este plan tambien integra el registro en las dos pantallas reales existentes:

- `PantallaFabricaRobots`
- `PantallaRestauranteIpc`

No implementar este plan hasta que el usuario lo apruebe explicitamente.

## Depende de

- `001-foundation-dashboard-common.md`
- `002-robot-factory-processes.md`
- `003-restaurant-ipc-binder.md`

## Decisiones cerradas

- El logger se llama `ExperimentoLogger`.
- Cada pantalla crea y mantiene su propio logger con `rememberRegistroExperimento(tag)`.
- El logger no es singleton global.
- El logger no vive en el `ViewModel`.
- El logger no se pasa a servicios remotos.
- El logger mantiene un buffer privado sin limite fijo.
- El buffer no se persiste en disco.
- El buffer se limpia automaticamente al iniciar o reiniciar una ejecucion real.
- El usuario no puede limpiar manualmente el registro desde la UI.
- El boton de monitoreo vive en la `TopAppBar`.
- El boton de monitoreo abre un `ModalBottomSheet`.
- Se acepta `@OptIn(ExperimentalMaterial3Api::class)` porque `ModalBottomSheet` de Material 3 lo requiere.
- `ExperimentoScaffold` no conoce el logger; solo expone un slot generico de acciones de topbar.
- Los eventos normales que deben verse en UI se registran con `ExperimentoLogger`, no con `Log.x` directo.
- Los eventos normales de servicios remotos se publican al proceso principal por `Messenger`.
- Los servicios mantienen una cola local simple de eventos mientras viven.
- Los servicios no escriben `Log.x` para eventos publicados hacia UI, para evitar duplicados en Logcat.
- Los servicios pueden conservar `Log.e` directo solo como fallback tecnico cuando no se puede reportar por IPC.
- No se agregan dependencias nuevas.
- No se agregan pruebas automatizadas.
- No se ejecuta build automaticamente.

## Archivos a crear

- `android/app/src/main/java/io/yerdna/architecturasos/util/EventoExperimento.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/util/OrigenEvento.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/util/TipoEvento.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/util/ExperimentoLogger.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/component/PanelRegistroEventos.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/component/RegistroExperimento.kt`

## Archivos a modificar

- `android/app/src/main/java/io/yerdna/architecturasos/ui/component/ExperimentoScaffold.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaFabricaRobots.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaRestauranteIpc.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/procesos/ContratoFabricaRobots.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/procesos/ControladorFabricaRobots.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/procesos/ServicioFabricaRobots.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/restaurante/ContratoRestauranteIpc.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/restaurante/ControladorRestauranteIpc.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/restaurante/ServicioRestauranteIpc.kt`
- `android/app/src/main/res/values/strings.xml`

No modificar otros archivos salvo que el codigo existente lo exija para compilar.

## Modelo de evento

Crear `EventoExperimento.kt`:

```kotlin
data class EventoExperimento(
    val timestamp: Long,
    val tipo: TipoEvento,
    val mensaje: String,
    val origen: OrigenEvento
)
```

Crear `OrigenEvento.kt`:

```kotlin
enum class OrigenEvento {
    ProcesoPrincipal,
    Servicio
}
```

Crear `TipoEvento.kt`:

```kotlin
enum class TipoEvento {
    Informacion,
    Advertencia,
    Error
}
```

No crear tipo `Exito`.

## ExperimentoLogger

Crear `ExperimentoLogger.kt` en `util/`.

Responsabilidades:

- Mantener un buffer privado de `EventoExperimento`.
- Escribir cada evento en Logcat usando el tag recibido.
- Notificar cambios mediante `onCambio`.
- Ser instanciable por pantalla.
- No depender de Compose.
- No usar `SnapshotStateList`.
- No tener limite fijo de eventos.
- Proteger el buffer con `synchronized`.
- Entregar `onCambio` siempre en el main thread.

Constructor:

```kotlin
class ExperimentoLogger(
    private val tag: String,
    private val onCambio: (List<EventoExperimento>) -> Unit
)
```

Metodos publicos:

```kotlin
fun info(
    mensaje: String,
    timestamp: Long = System.currentTimeMillis(),
    origen: OrigenEvento = OrigenEvento.ProcesoPrincipal
)

fun advertencia(
    mensaje: String,
    timestamp: Long = System.currentTimeMillis(),
    origen: OrigenEvento = OrigenEvento.ProcesoPrincipal
)

fun error(
    mensaje: String,
    throwable: Throwable? = null,
    timestamp: Long = System.currentTimeMillis(),
    origen: OrigenEvento = OrigenEvento.ProcesoPrincipal
)

fun registrar(evento: EventoExperimento)

fun captura(): List<EventoExperimento>

fun limpiar()
```

Reglas:

- `info` crea un evento `TipoEvento.Informacion`.
- `advertencia` crea un evento `TipoEvento.Advertencia`.
- `error` crea un evento `TipoEvento.Error`.
- `registrar(evento)` conserva el timestamp recibido.
- `captura()` devuelve una copia del buffer actual.
- `limpiar()` borra el buffer y notifica `onCambio`.
- `onCambio` se llama con `captura()` despues de registrar o limpiar.
- `onCambio` se ejecuta en el main thread usando `Handler(Looper.getMainLooper())` cuando haga falta.
- No llamar `onCambio` dentro de un bloque `synchronized`.

Mapeo Logcat:

- `TipoEvento.Informacion` -> `Log.i(tag, "[I][Origen] $mensaje")`
- `TipoEvento.Advertencia` -> `Log.w(tag, "[W][Origen] $mensaje")`
- `TipoEvento.Error` sin throwable -> `Log.e(tag, "[E][Origen] $mensaje")`
- `TipoEvento.Error` con throwable -> `Log.e(tag, "[E][Origen] $mensaje", throwable)`

El buffer UI guarda el mensaje limpio sin prefijo `[I]`, `[W]` o `[E]`.

## Componentes comunes de UI

### ExperimentoScaffold

Modificar `ExperimentoScaffold.kt` para aceptar un slot generico de acciones:

```kotlin
@Composable
fun ExperimentoScaffold(
    titulo: String,
    onVolver: (() -> Unit)? = null,
    acciones: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
)
```

Reglas:

- Pasar `acciones` a `TopAppBar(actions = acciones)`.
- Mantener el boton de volver actual.
- No agregar logica de registro dentro del scaffold.
- No renderizar bottom sheet desde el scaffold.

### PanelRegistroEventos

Crear `PanelRegistroEventos.kt`.

API:

```kotlin
@Composable
fun PanelRegistroEventos(
    eventos: List<EventoExperimento>,
    modifier: Modifier = Modifier
)
```

Contenido:

- Titulo visible `Registro de eventos`.
- Si no hay eventos, mostrar `Sin eventos todavía`.
- Si hay eventos, mostrar una `LazyColumn`.
- Cada fila muestra:
  - hora en formato `HH:mm:ss`;
  - tipo visible;
  - origen visible;
  - mensaje.
- Formatear hora con `SimpleDateFormat("HH:mm:ss", Locale.getDefault())`.
- Usar textos visibles desde `strings.xml`.
- No mostrar boton `Limpiar`.
- No permitir limpieza manual.
- No crear filtros, busqueda, exportacion ni persistencia.

Colores:

- `Informacion`: color normal de texto.
- `Advertencia`: color secundario o terciario del tema.
- `Error`: `MaterialTheme.colorScheme.error`.

Preview:

- Crear `PanelRegistroEventosPreview`.
- Usar eventos estaticos de ejemplo.
- No crear `ExperimentoLogger`.
- No iniciar servicios, controladores ni recursos externos.

### RegistroExperimento

Crear `RegistroExperimento.kt`.

Debe contener:

- `EstadoRegistroExperimento`
- `rememberRegistroExperimento(tag: String): EstadoRegistroExperimento`
- `BotonRegistroEventos`
- `HojaRegistroEventos`

`EstadoRegistroExperimento`:

```kotlin
class EstadoRegistroExperimento internal constructor(...) {
    val logger: ExperimentoLogger
    val eventos: List<EventoExperimento>
    val visible: Boolean
    fun abrir()
    fun cerrar()
    fun limpiar()
}
```

Reglas:

- `rememberRegistroExperimento(tag)` crea el `ExperimentoLogger`.
- El estado Compose guarda:
  - lista actual de eventos;
  - si el bottom sheet esta visible.
- `abrir()` muestra la hoja.
- `cerrar()` oculta la hoja.
- `limpiar()` llama `logger.limpiar()`.
- `limpiar()` es solo para uso interno de la pantalla al iniciar o reiniciar ejecuciones.
- No exponer limpieza manual al usuario.

`BotonRegistroEventos`:

```kotlin
@Composable
fun BotonRegistroEventos(
    onClick: () -> Unit
)
```

Reglas:

- Usar `IconButton`.
- Usar `Icons.Filled.List`.
- Usar `registro_eventos_accion_monitoreo` como `contentDescription`.
- No mostrar contador ni badge.

`HojaRegistroEventos`:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaRegistroEventos(
    visible: Boolean,
    eventos: List<EventoExperimento>,
    onCerrar: () -> Unit
)
```

Reglas:

- Si `visible == false`, no renderizar nada.
- Si `visible == true`, renderizar `ModalBottomSheet`.
- `onDismissRequest` llama `onCerrar`.
- El contenido del sheet es `PanelRegistroEventos(eventos = eventos)`.
- No recibir `onLimpiar`.

## Integracion en pantallas actuales

### PantallaFabricaRobots

Modificar `PantallaFabricaRobots.kt`.

Crear al inicio de `PantallaFabricaRobots`:

```kotlin
val registro = rememberRegistroExperimento("OSPlayground/FabricaRobots")
```

Pasar accion al scaffold:

```kotlin
acciones = {
    BotonRegistroEventos(onClick = registro::abrir)
}
```

Renderizar hoja:

```kotlin
HojaRegistroEventos(
    visible = registro.visible,
    eventos = registro.eventos,
    onCerrar = registro::cerrar
)
```

Reglas de limpieza:

- Al tocar `Iniciar`, llamar `registro.limpiar()` antes de `viewModel.prepararInicio(...)` y `controlador.iniciar(...)`.
- No limpiar automaticamente al detener, cancelar, completar o salir.

Eventos minimos a registrar con `registro.logger`:

- Usuario solicita iniciar experimento.
- Servicio reporta PID secundario.
- Servicio reporta estado de fabrica.
- Servicio reporta robot ensamblado.
- Usuario solicita detener.
- Usuario confirma salida y cancelacion.
- Experimento completado.
- Experimento cancelado.
- Error reportado por controlador o servicio.

Los eventos que nacen en `ServicioFabricaRobots` deben viajar por `Messenger` hasta `ControladorFabricaRobots`, y de ahi a la pantalla por `onEventoRegistro(evento)`.

### PantallaRestauranteIpc

Modificar `PantallaRestauranteIpc.kt`.

Crear al inicio de `PantallaRestauranteIpc`:

```kotlin
val registro = rememberRegistroExperimento("OSPlayground/BinderIPC")
```

Pasar accion al scaffold:

```kotlin
acciones = {
    BotonRegistroEventos(onClick = registro::abrir)
}
```

Renderizar hoja:

```kotlin
HojaRegistroEventos(
    visible = registro.visible,
    eventos = registro.eventos,
    onCerrar = registro::cerrar
)
```

Reglas de limpieza:

- Al tocar `Conectar`, llamar `registro.limpiar()` antes de `viewModel.prepararConexion()` y `controlador.conectar()`.
- Al confirmar `Reiniciar`, llamar `registro.limpiar()` antes de reconectar.
- No limpiar automaticamente al desconectar, cancelar ordenes, recibir respuesta, error o salir.

Eventos minimos a registrar con `registro.logger`:

- Usuario solicita conectar.
- Servicio reporta cocina conectada.
- Usuario envia orden.
- Servicio reporta orden en cola.
- Servicio reporta orden recibida.
- Servicio reporta orden procesando.
- Servicio reporta respuesta enviada o recibida.
- Usuario solicita desconectar.
- Usuario confirma reinicio.
- Usuario confirma salida y desconexion.
- Error reportado por controlador o servicio.

Los eventos que nacen en `ServicioRestauranteIpc` deben viajar por `Messenger` hasta `ControladorRestauranteIpc`, y de ahi a la pantalla por `onEventoRegistro(evento)`.

## Contrato IPC para eventos de registro

Agregar en `ContratoFabricaRobots.kt` y `ContratoRestauranteIpc.kt` constantes equivalentes:

```kotlin
const val MSG_EVENTO_REGISTRO = ...
const val KEY_EVENTO_TIPO = "evento_tipo"
const val KEY_EVENTO_MENSAJE = "evento_mensaje"
const val KEY_EVENTO_TIMESTAMP = "evento_timestamp"
const val KEY_EVENTO_ORIGEN = "evento_origen"
```

Reglas:

- Usar valores numericos que no choquen con mensajes existentes en cada contrato.
- Direccion: `Service -> Controlador -> Pantalla`.
- `KEY_EVENTO_TIPO` es `String` y usa `TipoEvento.name`.
- `KEY_EVENTO_MENSAJE` es `String`.
- `KEY_EVENTO_TIMESTAMP` es `Long`.
- `KEY_EVENTO_ORIGEN` es `String` y usa `OrigenEvento.name`.
- Si el tipo viene vacio o invalido, el controlador usa `TipoEvento.Informacion`.
- Si el origen viene vacio o invalido, el controlador usa `OrigenEvento.Servicio` para eventos recibidos desde `Service`.

Callbacks nuevos:

```kotlin
fun onEventoRegistro(evento: EventoExperimento)
```

Agregar este callback en:

- `ControladorFabricaRobots.Callbacks`
- `ControladorRestauranteIpc.Callbacks`

Reglas:

- El controlador no recibe `ExperimentoLogger`.
- El controlador solo traduce `Message` a `EventoExperimento`.
- La pantalla implementa `onEventoRegistro(evento)` y llama `registro.logger.registrar(evento)`.

## Servicios remotos

Modificar:

- `ServicioFabricaRobots.kt`
- `ServicioRestauranteIpc.kt`

Cada servicio debe tener una cola local:

```kotlin
private val eventosServicio = mutableListOf<EventoExperimento>()
```

Crear una funcion privada:

```kotlin
private fun publicarEventoRegistro(
    tipo: TipoEvento,
    mensaje: String,
    timestamp: Long = System.currentTimeMillis()
)
```

Comportamiento:

- Agrega `EventoExperimento(timestamp, tipo, mensaje)` a `eventosServicio`.
- Si existe cliente `Messenger`, envia `MSG_EVENTO_REGISTRO` con tipo, mensaje y timestamp.
- Si no existe cliente, deja el evento solo en `eventosServicio`.
- No reenvia eventos antiguos cuando el cliente se registra.
- No bloquea esperando cliente.
- No escribe `Log.x` para eventos publicados.
- Si falla el envio por IPC, usar `Log.e(TAG, ...)` como fallback tecnico y continuar.
- Limpiar `eventosServicio` al iniciar/reiniciar trabajo real y al destruir el servicio.

## Reemplazo de Log.x existentes

Objetivo:

- Reemplazar los `Log.x` normales agregados en Fábrica y Restaurante por `ExperimentoLogger` o por publicacion de eventos del servicio.
- Los eventos visibles en UI tambien deben aparecer en Logcat con el tag de la pantalla.

Reglas:

- En `ControladorFabricaRobots.kt` y `ControladorRestauranteIpc.kt`, no usar `Log.i` ni `Log.w` para eventos normales.
- En controladores, errores normales reportables a UI deben pasar por callback y luego por `ExperimentoLogger`.
- En servicios, eventos normales visibles en UI deben usar `publicarEventoRegistro(...)`.
- En servicios, permitir `Log.e` directo solo cuando falle el canal IPC o no se pueda reportar al cliente.
- Mantener los tags actuales:
  - Fábrica: `OSPlayground/FabricaRobots`
  - Restaurante IPC: `OSPlayground/BinderIPC`
- No cambiar comandos de verificacion existentes salvo ajuste de descripcion si hace falta.

## Strings

Modificar `strings.xml` solo para textos nuevos de registro o textos directamente relacionados con Logcat/registro.

Agregar como minimo:

```xml
<string name="registro_eventos_titulo">Registro de eventos</string>
<string name="registro_eventos_vacio">Sin eventos todavía</string>
<string name="registro_eventos_accion_monitoreo">Abrir registro de eventos</string>
<string name="tipo_evento_informacion">Información</string>
<string name="tipo_evento_advertencia">Advertencia</string>
<string name="tipo_evento_error">Error</string>
<string name="origen_evento_proceso_principal">Proceso principal</string>
<string name="origen_evento_servicio">Servicio</string>
```

Reglas:

- Todo texto visible nuevo debe estar en `strings.xml`.
- Usar español correcto con acentos para los textos nuevos.
- No hacer correccion global de codificacion o acentos de textos existentes en este plan.

## Estados y metricas

Este plan no agrega estados de ejecucion nuevos a los experimentos.

Estado propio del registro:

- `visible`: indica si el bottom sheet esta abierto.
- `eventos`: lista actual para UI.

Metricas:

- Este plan no agrega metricas nuevas obligatorias.
- No mostrar contador de eventos en el boton.
- La lista de eventos es informacion de monitoreo, no estado global del dashboard.

## Ciclo de vida y limpieza

- Cada pantalla crea su registro con `rememberRegistroExperimento(tag)`.
- El registro vive mientras vive la pantalla composable.
- El registro no sobrevive al cierre/recreacion de la pantalla.
- Al iniciar una ejecucion real, la pantalla llama `registro.limpiar()` antes de iniciar recursos.
- Al reiniciar una ejecucion real, la pantalla llama `registro.limpiar()` antes de reiniciar recursos.
- Salir de una pantalla mantiene las reglas existentes de confirmacion y limpieza de recursos del experimento.
- `DisposableEffect` existente sigue siendo responsable de liberar controladores.
- El bottom sheet solo muestra eventos; no administra recursos del experimento.
- Los servicios limpian `eventosServicio` al iniciar/reiniciar trabajo real y al destruirse.

## Controles por estado

Boton de monitoreo:

- Siempre visible en pantallas reales integradas.
- Siempre habilitado.
- No inicia, detiene ni reinicia experimentos.

Bottom sheet:

- Puede abrirse con experimento inactivo, ejecutando, error o completado.
- Puede cerrarse sin afectar la ejecucion.
- No contiene boton de limpiar.

Acciones de experimento:

- Mantener las reglas existentes de habilitado/deshabilitado en cada pantalla.
- Agregar solo llamadas a `registro.limpiar()` y `registro.logger...` donde corresponda.

## Verificacion

No ejecutar build automaticamente.

Verificacion estatica desde `android/`:

```powershell
Select-String -Path app/src/main/java/io/yerdna/architecturasos/procesos/*.kt,app/src/main/java/io/yerdna/architecturasos/restaurante/*.kt -Pattern 'Log\.i|Log\.w|Log\.e'
```

Interpretacion:

- No deben aparecer `Log.i` ni `Log.w` para eventos normales en los archivos de procesos/restaurante.
- `Log.e` solo debe quedar como fallback tecnico cuando falle IPC o envio de mensajes.

Revisar que no se agregaron dependencias:

```powershell
Select-String -Path build.gradle.kts,app/build.gradle.kts,gradle/libs.versions.toml -Pattern 'implementation|api|version'
```

Verificacion manual en Android Studio/emulador:

- Abrir Fábrica de Robots.
- Tocar el boton de monitoreo en la barra superior.
- Confirmar que se abre el bottom sheet.
- Iniciar el experimento.
- Confirmar que aparecen eventos en el bottom sheet.
- Confirmar que los eventos aparecen en Logcat con tag `OSPlayground/FabricaRobots`.
- Detener o completar el experimento.
- Confirmar que los eventos permanecen visibles hasta una nueva ejecucion o cierre de pantalla.
- Iniciar una nueva ejecucion y confirmar que el registro inicia limpio.
- Repetir flujo en Restaurante IPC.
- Confirmar Logcat con tag `OSPlayground/BinderIPC`.

Build opcional solo con aprobacion explicita del usuario:

```powershell
cd android
.\gradlew.bat build
```

## Criterios de aceptacion

- [ ] Existe `EventoExperimento`.
- [ ] Existe `OrigenEvento` con `ProcesoPrincipal` y `Servicio`.
- [ ] Existe `TipoEvento` con `Informacion`, `Advertencia` y `Error`.
- [ ] No existe tipo `Exito`.
- [ ] Existe `ExperimentoLogger`.
- [ ] `ExperimentoLogger` mantiene buffer privado sin limite fijo.
- [ ] `ExperimentoLogger.captura()` devuelve copia del buffer.
- [ ] `ExperimentoLogger.limpiar()` borra el buffer y notifica cambio.
- [ ] `ExperimentoLogger.info(...)` registra evento y escribe `Log.i` con prefijo `[I]`.
- [ ] `ExperimentoLogger.advertencia(...)` registra evento y escribe `Log.w` con prefijo `[W]`.
- [ ] `ExperimentoLogger.error(...)` registra evento y escribe `Log.e` con prefijo `[E]`.
- [ ] `ExperimentoLogger.error(...)` acepta `Throwable?`.
- [ ] `ExperimentoLogger.registrar(evento)` conserva el timestamp recibido.
- [ ] `ExperimentoLogger` protege el buffer con `synchronized`.
- [ ] `ExperimentoLogger` ejecuta `onCambio` en main thread.
- [ ] No se usa `SnapshotStateList` dentro del logger.
- [ ] Existe `PanelRegistroEventos`.
- [ ] `PanelRegistroEventos` muestra hora, tipo, origen y mensaje.
- [ ] `PanelRegistroEventos` no muestra boton de limpiar.
- [ ] Existe preview de `PanelRegistroEventos` con datos estaticos.
- [ ] Existe `RegistroExperimento.kt` con `rememberRegistroExperimento`, `EstadoRegistroExperimento`, `BotonRegistroEventos` y `HojaRegistroEventos`.
- [ ] `HojaRegistroEventos` usa `ModalBottomSheet`.
- [ ] `BotonRegistroEventos` usa `Icons.Filled.List`.
- [ ] `ExperimentoScaffold` tiene slot `acciones`.
- [ ] `ExperimentoScaffold` no conoce el logger ni renderiza la hoja.
- [ ] `PantallaFabricaRobots` crea su registro con tag `OSPlayground/FabricaRobots`.
- [ ] `PantallaRestauranteIpc` crea su registro con tag `OSPlayground/BinderIPC`.
- [ ] Ambas pantallas muestran boton de monitoreo en topbar.
- [ ] Ambas pantallas muestran bottom sheet con eventos.
- [ ] Ambas pantallas limpian registro automaticamente al iniciar o reiniciar ejecucion real.
- [ ] El usuario no puede limpiar manualmente los eventos.
- [ ] Ambos controladores exponen `onEventoRegistro(evento: EventoExperimento)`.
- [ ] Ambos contratos IPC definen `MSG_EVENTO_REGISTRO`, `KEY_EVENTO_TIPO`, `KEY_EVENTO_MENSAJE`, `KEY_EVENTO_TIMESTAMP` y `KEY_EVENTO_ORIGEN`.
- [ ] Ambos servicios publican eventos visibles con `publicarEventoRegistro(...)`.
- [ ] Ambos servicios mantienen `eventosServicio`.
- [ ] No se pasa `ExperimentoLogger` a servicios remotos.
- [ ] No quedan `Log.i` ni `Log.w` normales en controladores/servicios de Fábrica y Restaurante.
- [ ] `Log.e` directo solo queda como fallback tecnico cuando no se puede reportar por IPC.
- [ ] No se agregan dependencias nuevas.
- [ ] No se agregan pruebas automatizadas.
- [ ] No se ejecuta build automatico.
