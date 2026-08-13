# Plan 010 - System Diagnostics y pulido de verificacion

## Objetivo

Agregar la pantalla `System Diagnostics` (nombre de referencia en ingles usado por este plan; el titulo visible en la UI es en espanol, ver seccion `strings.xml`), cerrar la observabilidad transversal de la aplicacion y dejar verificable que los experimentos generan actividad real observable con herramientas Android/ADB.

Este plan no implementa nuevos mecanismos de sistemas operativos. Solo integra una pantalla educativa de diagnostico, reutiliza patrones comunes ya existentes y corrige la documentacion/verificacion de los 8 experimentos cuando falte o contradiga decisiones cerradas.

## Estructura real del proyecto (corregida)

La raiz real del modulo Android **no** tiene un prefijo `android/`. El proyecto Gradle vive directamente en la raiz del repositorio:

```txt
app/src/main/java/io/yerdna/architecturasos/
app/src/main/res/values/strings.xml
gradlew.bat   (en la raiz del repositorio, sin carpeta android/)
```

Cualquier referencia previa a `android/app/...` o `cd android` es incorrecta para este proyecto y no debe usarse. Todas las rutas de este plan usan la raiz real.

## Contexto obligatorio aplicado

- Proyecto Android nativo con Kotlin, Jetpack Compose, Material 3 y AndroidX.
- Raiz real del proyecto: la raiz del repositorio (no existe carpeta `android/`; ver "Estructura real del proyecto (corregida)").
- Paquete principal: `io.yerdna.architecturasos`.
- No agregar dependencias nuevas.
- No crear arquitectura compleja, `domain`, `usecase`, `mapper`, `di` o capas vacias.
- No ejecutar build automaticamente durante la revision del plan.
- Textos visibles nuevos en `strings.xml`.
- Codigo propio del proyecto en espanol; APIs oficiales Android/Kotlin conservan su idioma.
- Dashboard sin estado global por experimento.
- Cada experimento es duenio de su estado y recursos mientras su pantalla esta abierta.
- Cada experimento debe limpiar recursos al salir, al reiniciar y ante error.
- El boton volver de la barra superior, el boton atras del sistema y el gesto atras deben seguir la misma regla de confirmacion cuando haya ejecucion activa.
- No mostrar boton manual para limpiar registros de eventos.
- Los comandos visibles de verificacion deben ser compatibles con Windows/PowerShell.
- La visualizacion no reemplaza el mecanismo tecnico real.
- La app no reimplementa Android Profiler, Perfetto, Dumpstate ni Battery Historian; solo explica como usarlos sobre actividad real generada por los experimentos.

## Depende de

Este plan debe implementarse despues de que esten aprobados e implementados:

- `001-foundation-dashboard-common.md`
- `002-robot-factory-processes.md`
- `003-restaurant-ipc-binder.md`
- `004-agent-network-sockets.md`
- `005-thread-race-threads.md`
- `006-chaos-bank-race-condition.md`
- `007-ticket-rush-mutex.md`
- `008-smart-parking-semaphore.md`
- `009-memory-monster-memory.md`
- `011-panel-registro-eventos.md`

Todos los planes anteriores, incluido `011-panel-registro-eventos.md`, ya estan implementados en el codigo actual. Si en el futuro alguno dejara de estarlo, se debe detener la implementacion y actualizar primero el estado real de dependencias. No se deben implementar funcionalidades faltantes de experimentos dentro de este plan.

## Alcance incluido

- Crear la pantalla `PantallaDiagnosticoSistema`.
- Crear datos estaticos simples para herramientas de diagnostico.
- Agregar una entrada de dashboard para abrir diagnosticos sin estado global.
- Agregar la ruta de navegacion `Navegacion.Ruta.Diagnosticos` a la navegacion comun ya existente (Navigation Compose).
- Crear o ajustar un componente comun simple para listar comandos copiables si no existe uno equivalente.
- Revisar y corregir las secciones `Como verificar` de los 8 experimentos para que usen comandos concretos, compatibles con Windows y vinculados al comportamiento real del modulo.
- Revisar que los textos visibles nuevos queden en `strings.xml`.
- Revisar que no queden botones manuales `Limpiar` para registros de eventos.
- Revisar que los tags de Logcat sean estables y documentados por experimento.

## Fuera de alcance

- No agregar dependencias.
- No ejecutar `.\gradlew.bat build` sin aprobacion explicita del usuario.
- No ejecutar `.\gradlew.bat test` sin aprobacion explicita del usuario.
- No implementar Android Profiler, Perfetto, Dumpstate, Battery Historian, systrace ni atrace dentro de la app.
- No crear backend, persistencia, exportacion, login, red externa ni integraciones cloud.
- No crear un modelo global `EstadoExperimento`.
- No modificar el comportamiento tecnico de los experimentos salvo correcciones minimas de verificacion, textos y limpieza ya cerradas por planes previos.
- No agregar boton manual para limpiar registros de eventos.
- No tocar archivos generados dentro de `build/`, `.gradle/` o salidas del IDE.

## Archivos exactos a crear

- `app/src/main/java/io/yerdna/architecturasos/diagnostico/HerramientaDiagnostico.kt`
- `app/src/main/java/io/yerdna/architecturasos/diagnostico/RepositorioDiagnosticoSistema.kt`
- `app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaDiagnosticoSistema.kt`
- `app/src/main/java/io/yerdna/architecturasos/ui/component/ListaComandosVerificacion.kt`

El paquete usa `diagnostico` (singular, en espanol) para seguir la convencion real ya usada por `memoria`, `procesos`, `restaurante`, `hilos`, `bancocaotico`, `carreraboletos`, `parqueointeligente` y `redagentes`. No usar `diagnostics` (ingles).

## Archivos exactos a modificar

- `app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaPanelExperimentos.kt` (dashboard real; no existe `PantallaDashboard.kt` ni carpeta `ui/dashboard/`)
- `app/src/main/java/io/yerdna/architecturasos/ui/Navegacion.kt` (agregar `Ruta.Diagnosticos`)
- `app/src/main/java/io/yerdna/architecturasos/ui/App.kt`
- `app/src/main/res/values/strings.xml`

Modificar tambien estos archivos solo si les falta o contradicen la seccion `Como verificar`, logs, limpieza o controles ya definidos por sus planes correspondientes (nombres reales verificados contra el codigo actual):

- `app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaFabricaRobots.kt`
- `app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaRestauranteIpc.kt`
- `app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaRedAgentes.kt`
- `app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaCarreraHilos.kt`
- `app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaBancoCaotico.kt`
- `app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaCarreraBoletos.kt`
- `app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaParqueoInteligente.kt`
- `app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaMonstruoMemoria.kt`

Estos 8 nombres ya son los nombres reales existentes en el codigo (confirmado contra `app/src/main/java/io/yerdna/architecturasos/ui/screen/`). No crear pantallas duplicadas ni variantes con otro nombre.

## Responsabilidades por archivo

### `HerramientaDiagnostico.kt`

Debe contener modelos simples:

- `data class HerramientaDiagnostico`
- `data class ComandoDiagnostico`
- `data class RelacionExperimentoDiagnostico`

Campos obligatorios de `HerramientaDiagnostico`:

- `id: String`
- `nombre: String`
- `categoria: CategoriaDiagnostico`
- `descripcion: String`
- `cuandoUsarla: String`
- `queValida: String`
- `limitacion: String`
- `comandos: List<ComandoDiagnostico>`
- `experimentosRelacionados: List<RelacionExperimentoDiagnostico>`

`CategoriaDiagnostico` debe ser un `enum class` con:

- `PROCESOS_CPU`
- `MEMORIA`
- `LOGS`
- `IPC`
- `SOCKETS`
- `SERVICIOS`
- `TRACING`
- `REPORTE_SISTEMA`
- `ENERGIA`

Campos obligatorios de `ComandoDiagnostico`:

- `comando: String`
- `descripcion: String`
- `cuandoEjecutarlo: String`
- `comoInterpretarlo: String`

Campos obligatorios de `RelacionExperimentoDiagnostico`:

- `nombreExperimento: String`
- `mecanismoObservable: String`

Estos modelos no deben depender de Compose ni Android runtime.

### `RepositorioDiagnosticoSistema.kt`

Debe exponer una funcion simple:

```kotlin
fun obtenerHerramientasDiagnostico(): List<HerramientaDiagnostico>
```

Debe devolver datos estaticos para:

- Android Profiler.
- `adb shell top`.
- `adb logcat`.
- Binder IPC.
- sockets.
- Perfetto/System Trace.
- `dumpsys`.
- ADB.
- Dumpstate.
- Battery Historian.

No debe hacer llamadas reales a ADB, shell, Profiler ni red.

### `PantallaDiagnosticoSistema.kt`

Debe mostrar:

- Titulo visible en espanol: `Diagnostico del Sistema` (recurso `experimento_diagnostico_sistema_nombre`, ver seccion `strings.xml`). `System Diagnostics` es solo el nombre de referencia en ingles usado por este plan y por `docs/app-requirements.md`; no debe aparecer como texto visible en la UI.
- Explicacion breve de que la app genera actividad real observable, pero no reimplementa herramientas externas.
- Lista agrupada o seccionada de herramientas desde `RepositorioDiagnosticoSistema`.
- Para cada herramienta:
  - nombre;
  - categoria;
  - que valida;
  - cuando usarla;
  - limitacion;
  - comandos copiables cuando existan;
  - experimentos relacionados.
- Seccion de comparacion academica Android vs Linux como sistema adicional, con al menos 3 aspectos:
  - procesos: Android usa componentes y servicios sobre Linux; Linux de escritorio expone procesos y `fork()` de forma directa al usuario.
  - IPC: Android usa Binder/Messenger; Linux comunmente usa pipes, sockets Unix, colas y memoria compartida.
  - diagnostico: Android usa Android Studio Profiler, ADB, dumpsys y Logcat; Linux usa top, htop, ps, /proc, strace/perf segun disponibilidad.
- Seccion final con guia de demostracion en vivo:
  - abrir experimento;
  - ejecutar actividad real;
  - observar metricas internas;
  - validar con comando externo o Android Studio;
  - detener/reiniciar para dejar recursos limpios.

Debe usar componentes Compose simples: `Scaffold`, `LazyColumn`, `Card`, `Text`, `Button` o el scaffold comun existente. No debe iniciar servicios, hilos, sockets ni cargas de memoria.

### `ListaComandosVerificacion.kt`

Debe ser un componente visual reusable para comandos.

Responsabilidades:

- Recibir `List<ComandoDiagnostico>` o modelo comun equivalente por parametro.
- Mostrar comando en texto monoespaciado.
- Mostrar descripcion, cuando ejecutarlo y como interpretar resultado.
- Incluir accion de copiar comando usando clipboard de Android/Compose sin agregar dependencias.
- No ejecutar comandos.
- No depender de ningun experimento concreto.

Ya se verifico contra el codigo actual que no existe un componente comun equivalente: cada pantalla de experimento (`PantallaParqueoInteligente.kt`, `PantallaMonstruoMemoria.kt`, etc.) define su propio composable privado `ComoVerificarX` y su propia `data class ComandoVerificacionX` duplicada, sin componente compartido. Por eso este plan crea `ListaComandosVerificacion.kt` como componente nuevo.

`ListaComandosVerificacion.kt` se usa **unicamente** dentro de `PantallaDiagnosticoSistema.kt`. Este plan **no** refactoriza los `ComoVerificarX` privados ya existentes en los 8 experimentos para que usen el componente nuevo: eso ampliaria el alcance a un refactor no solicitado por los criterios de aceptacion y arriesgaria romper pantallas ya funcionando. Cada experimento conserva su propia seccion `Como verificar` tal como esta implementada.

### `PantallaPanelExperimentos.kt` (dashboard real)

El dashboard ya renderiza sus tarjetas desde una lista privada `experimentos: List<ExperimentoResumen>` (`ExperimentoResumen.kt`: `id`, `icono`, `colorIcono`, `nombreResId`, `conceptoResId`, `descripcionResId`, `ruta`) y `TarjetaExperimento` reutilizable. Agregar diagnosticos siguiendo exactamente el mismo patron, sin crear un modelo ni un componente de tarjeta nuevo:

- Agregar un `ExperimentoResumen` mas a la lista `experimentos`:
  - `id = "diagnostico_sistema"`
  - `icono = "SD"`
  - `colorIcono = Color(0xFF495057)` (color no usado por ninguna tarjeta existente)
  - `nombreResId = R.string.experimento_diagnostico_sistema_nombre`
  - `conceptoResId = R.string.experimento_diagnostico_sistema_concepto` (texto: `Observabilidad Android`)
  - `descripcionResId = R.string.experimento_diagnostico_sistema_descripcion`
  - `ruta = Navegacion.Ruta.Diagnosticos`
- No modificar `TarjetaExperimento`, `obtenerExperimentoPorRuta` ni el resto del archivo.

No debe agregar estado global de diagnostico ni estado por experimento.

### `Navegacion.kt`

Agregar `const val Diagnosticos = "diagnosticos"` dentro de `Navegacion.Ruta`, siguiendo el mismo patron que las 8 rutas existentes (`Panel`, `FabricaRobots`, `RestauranteIpc`, `RedAgentes`, `CarreraHilos`, `BancoCaotico`, `CarreraBoletos`, `ParqueoInteligente`, `MonstruoMemoria`).

### `App.kt`

La app ya usa Navigation Compose (`NavHost`, `composable`, `rememberNavController`; ver `App.kt` y `Navegacion.kt` actuales). Registrar la pantalla de diagnosticos exactamente con el mismo patron que las demas rutas, sin condicionales:

```kotlin
composable(Navegacion.Ruta.Diagnosticos) {
    PantallaDiagnosticoSistema(
        onVolver = { navController.popBackStack() }
    )
}
```

No agregar dependencia de navegacion nueva ni un mecanismo de navegacion alternativo.

### `strings.xml`

Debe incluir todos los textos visibles nuevos, en espanol. Como minimo:

- `experimento_diagnostico_sistema_nombre` = "Diagnostico del Sistema" (tarjeta del dashboard y titulo de la pantalla);
- `experimento_diagnostico_sistema_concepto` = "Observabilidad Android";
- `experimento_diagnostico_sistema_descripcion` (una linea corta para la tarjeta del dashboard);
- descripciones de cada herramienta (`que valida`, `cuando usarla`, `limitacion`) con prefijo `diagnostico_`;
- nombres de categorias (`CategoriaDiagnostico`) con prefijo `diagnostico_categoria_`;
- textos de comandos (descripcion, cuando ejecutarlo, como interpretarlo) con prefijo `diagnostico_comando_`;
- textos de la seccion de comparacion Android vs Linux con prefijo `diagnostico_comparacion_`;
- textos de la guia de demostracion en vivo con prefijo `diagnostico_demo_`;
- `accion_copiar` ya existe (reutilizar, no duplicar).

Los comandos literales (el texto exacto del comando ADB/PowerShell) pueden vivir en los modelos Kotlin de `RepositorioDiagnosticoSistema.kt` porque son contenido tecnico copiable, igual que en los `ComoVerificarX` de los experimentos existentes. Las descripciones que los acompanan (que valida, cuando ejecutarlo, como interpretarlo) si van en `strings.xml`.

## Herramientas diagnosticas obligatorias

Exactamente 10 entradas `HerramientaDiagnostico` (una por subseccion de esta lista). `atrace` no es una entrada separada: se documenta dentro del campo `limitacion` de `Perfetto/System Trace` como alternativa cuando este disponible en el dispositivo, siguiendo la resolucion ya cerrada mas abajo en "Decisiones resueltas". Esto cierra cualquier duda sobre si la pantalla debe mostrar 10 u 11 tarjetas.

### Android Profiler

- Categoria: `MEMORIA` y `PROCESOS_CPU`.
- Valida CPU, memoria y threads desde Android Studio.
- Experimentos principales: `Thread Race`, `Memory Monster`.
- Limitacion: no se ejecuta dentro de la app.

### `adb shell top`

- Categoria: `PROCESOS_CPU`.
- Valida actividad de CPU y procesos visibles mientras se ejecutan experimentos.
- Experimentos principales: `Robot Factory`, `Thread Race`, `Memory Monster`.
- Limitacion: puede mostrar procesos cacheados o actividad agregada segun version Android.

### `adb logcat`

- Categoria: `LOGS`.
- Valida eventos emitidos por el logger de cada pantalla.
- Aplica a todos los experimentos.
- Limitacion: `Log.e` directo en services solo debe quedar como fallback tecnico.

### Binder IPC

- Categoria: `IPC`.
- Valida comunicacion real entre proceso cliente y servicio remoto mediante `Messenger`.
- Experimento principal: `Restaurant IPC`.
- Limitacion: el plan 003 (ya implementado) decidio usar `Messenger` como mecanismo de Binder IPC; no se usa AIDL, `LocalBinder`, broadcasts ni archivos temporales.

### sockets

- Categoria: `SOCKETS`.
- Valida comunicacion TCP local real con `ServerSocket` y `Socket`.
- Experimento principal: `Agent Network`.
- Limitacion: no prueba Internet externo; el permiso `INTERNET` solo habilita APIs de red Android.

### Perfetto/System Trace

- Categoria: `TRACING`.
- Valida actividad de CPU y threads durante carga controlada.
- Experimento principal: `Thread Race`.
- Limitacion: la disponibilidad depende de Android Studio, version de Android y dispositivo/emulador. La herramienta concreta se selecciona segun disponibilidad: Android Studio Profiler/System Trace o Perfetto primero; `atrace` solo cuando este disponible en el dispositivo. La app no ejecuta ninguna de estas herramientas.

### `dumpsys`

- Categoria: `SERVICIOS` y `MEMORIA`.
- Valida services, memoria y estado del sistema.
- Experimentos principales: `Robot Factory`, `Restaurant IPC`, `Memory Monster`.
- Limitacion: la salida varia por version Android.

### ADB

- Categoria: `REPORTE_SISTEMA`.
- Valida conexion del dispositivo y permite ejecutar herramientas externas.
- Aplica a todos los experimentos.
- Limitacion: requiere depuracion USB o emulador conectado.

### Dumpstate

- Categoria: `REPORTE_SISTEMA`.
- Valida recopilacion amplia de estado del dispositivo para informe o diagnostico posterior.
- Aplica como herramienta complementaria de exposicion.
- Limitacion: puede tardar y generar mucha salida; no se usa como verificacion puntual de un experimento.

### Battery Historian

- Categoria: `ENERGIA`.
- Valida analisis posterior de consumo, actividad y wakeups con una captura controlada.
- Experimentos principales: `Thread Race`, `Memory Monster`, cualquier experimento con carga sostenida.
- Limitacion: no es requisito ejecutarlo dentro de la app ni integrarlo al proyecto.

## Comandos generales obligatorios

Los comandos mostrados en la UI deben ser compatibles con Windows/PowerShell o aclarar si son para shell Android. No usar `grep` en comandos visibles.

### Conexion ADB

```powershell
adb devices
```

Valida que el emulador o dispositivo este disponible antes de ejecutar otras verificaciones.

### Procesos Android

```powershell
adb shell ps -A
```

Valida procesos visibles. Sirve para observar que se crea un proceso secundario, pero no prueba por si solo que un `Service` siga activo porque Android puede dejar procesos cacheados.

### Filtrar procesos por paquete en PowerShell

```powershell
adb shell ps -A | findstr architecturasos
```

Valida los procesos relacionados con `io.yerdna.architecturasos` cuando existan en la lista.

### CPU en vivo

```powershell
adb shell top
```

Valida consumo de CPU mientras se ejecuta `Thread Race` o cualquier experimento con carga real.

### Logcat por tag

```powershell
adb logcat -d -s OSPlayground/FabricaRobots OSPlayground/BinderIPC OSPlayground/SocketLab OSPlayground/ThreadRace OSPlayground/ChaosBank OSPlayground/Mutex OSPlayground/Semaphore OSPlayground/Memory
```

Valida eventos emitidos por los experimentos y termina la lectura del buffer actual.

### Memoria de la app

```powershell
adb shell dumpsys meminfo io.yerdna.architecturasos
```

Valida cambios reales de memoria, especialmente durante `Memory Monster`.

### Services Android

```powershell
adb shell dumpsys activity services
```

Valida services registrados/activos. Es la verificacion correcta para confirmar estado de services frente a `ps -A`, que solo muestra procesos.

### Filtrar services por paquete en PowerShell

```powershell
adb shell dumpsys activity services | findstr architecturasos
```

Ayuda a ubicar services del proyecto en la salida de `dumpsys activity services`.

## Tags Logcat obligatorios

Cada experimento debe documentar y usar un tag estable. Estos son los valores reales ya implementados en el codigo (verificados contra las constantes `TAG_*` de cada modulo); `docs/app-requirements.md` usa `OSPlayground/ProcessLab` solo como ejemplo generico, pero el codigo real de `Robot Factory` (`ServicioFabricaRobots.kt`) ya usa `OSPlayground/FabricaRobots`, y este plan no debe cambiar ese valor:

- `OSPlayground/FabricaRobots` para `Robot Factory` (`ServicioFabricaRobots.kt`).
- `OSPlayground/BinderIPC` para `Restaurant IPC` (`ContratoRestauranteIpc.TAG_LOGCAT`).
- `OSPlayground/SocketLab` para `Agent Network` (`TAG_RED_AGENTES`).
- `OSPlayground/ThreadRace` para `Thread Race` (`TAG_CARRERA_HILOS`).
- `OSPlayground/ChaosBank` para `Chaos Bank` (`TAG_BANCO_CAOTICO`).
- `OSPlayground/Mutex` para `Ticket Rush` (`TAG_CARRERA_BOLETOS`).
- `OSPlayground/Semaphore` para `Smart Parking` (`TAG_PARQUEO_INTELIGENTE`).
- `OSPlayground/Memory` para `Memory Monster` (`TAG_MONSTRUO_MEMORIA`).

La pantalla de diagnostico solo documenta estos tags. No debe crear un logger global.

## Estados

`PantallaDiagnosticoSistema` no tiene ejecucion tecnica propia. Debe manejar solo estado de UI simple:

- `Cargando`: opcional y breve si los datos se preparan antes de renderizar.
- `Listo`: herramientas renderizadas desde datos estaticos.
- `Error`: solo si ocurre una excepcion inesperada preparando datos locales.

No debe existir estado `Ejecutando`, `Conectado`, `Desconectado`, `Exitoso` o `Cancelado` en diagnosticos porque esta pantalla no ejecuta experimentos.

## Metricas

La pantalla de diagnostico debe mostrar metricas descriptivas, no mediciones runtime propias:

- cantidad de herramientas documentadas;
- cantidad de comandos copiables;
- cantidad de experimentos cubiertos por verificacion;
- lista de tags Logcat cubiertos.

Los experimentos conservan sus metricas reales propias. Este plan no debe convertir metricas de experimentos en estados globales.

## Controles por estado

### `Listo`

- Mostrar lista de herramientas.
- Permitir copiar comandos.
- Permitir volver al dashboard.
- No mostrar `Start`, `Stop`, `Reset` ni `Cancel` en diagnosticos.

### `Error`

- Mostrar mensaje simple.
- Permitir volver al dashboard.
- No intentar reiniciar servicios ni experimentos.

## Ciclo de vida

- Al entrar a diagnosticos: cargar datos estaticos locales.
- Al copiar comando: escribir el texto del comando en clipboard mediante `ClipEntry`/`ClipData`. El patron ya establecido en los 8 experimentos (por ejemplo `ComoVerificarParqueoInteligente`, `ComoVerificarMonstruoMemoria`) no muestra Snackbar ni Toast de confirmacion; el boton `Copiar` es la unica retroalimentacion. `ListaComandosVerificacion.kt` sigue el mismo patron, sin agregar Snackbar/Toast nuevo.
- Al salir: no hay recursos tecnicos que liberar.
- Al recomponer: no duplicar listas ni eventos.

## Limpieza

- `PantallaDiagnosticoSistema` no inicia recursos externos; por tanto no requiere detener hilos, services, sockets ni memoria.
- No limpiar registros de eventos desde diagnosticos.
- En los 8 experimentos, confirmar que cada ejecucion nueva inicia con un registro de eventos nuevo segun su plan correspondiente.
- En los 8 experimentos, confirmar que salir con trabajo activo muestra `AlertDialog`, limpia recursos al confirmar y mantiene la ejecucion al cancelar.

## Verificacion de este plan

No ejecutar build automaticamente. Si un agente necesita compilar, debe pedir aprobacion antes de correr:

```powershell
.\gradlew.bat build
```

Verificaciones manuales permitidas para revisar archivos sin build:

El comodin recursivo `**` no es fiable con `Select-String -Path` en Windows PowerShell 5.1 (version documentada del entorno del usuario): omite archivos en subcarpetas de forma inconsistente. Usar `Get-ChildItem -Recurse` explicito y pasar la lista de rutas resultante:

### Buscar comandos incompatibles con Windows

```powershell
$archivosKt = (Get-ChildItem -Path app/src/main/java/io/yerdna/architecturasos -Recurse -Filter *.kt).FullName
Select-String -Path ($archivosKt + "app/src/main/res/values/strings.xml") -Pattern "grep "
```

Debe no devolver comandos visibles nuevos con `grep`. Si aparece texto heredado, reemplazarlo por `findstr` o por `adb logcat -d -s TAG`.

### Revisar botones manuales de limpieza de logs

```powershell
$archivosKt = (Get-ChildItem -Path app/src/main/java/io/yerdna/architecturasos -Recurse -Filter *.kt).FullName
Select-String -Path ($archivosKt + "app/src/main/res/values/strings.xml") -Pattern "Limpiar|Clear"
```

Debe no mostrar un boton manual de limpieza del registro de eventos. Puede existir una funcion interna `limpiar()` del logger si no es visible como accion manual.

### Revisar tags Logcat documentados

```powershell
Get-ChildItem -Path app/src/main/java/io/yerdna/architecturasos -Recurse -Filter *.kt | Select-String -Pattern "OSPlayground/"
```

Debe mostrar tags estables por experimento y no un tag global ambiguo.

### Verificacion manual en dispositivo o emulador

Ejecutar desde una terminal externa cuando la app este instalada:

```powershell
adb devices
adb shell ps -A | findstr architecturasos
adb shell top
adb logcat -d -s OSPlayground/FabricaRobots OSPlayground/BinderIPC OSPlayground/SocketLab OSPlayground/ThreadRace OSPlayground/ChaosBank OSPlayground/Mutex OSPlayground/Semaphore OSPlayground/Memory
adb shell dumpsys meminfo io.yerdna.architecturasos
adb shell dumpsys activity services | findstr architecturasos
```

Interpretacion:

- `adb devices`: debe listar el dispositivo/emulador.
- `ps -A | findstr architecturasos`: puede mostrar proceso principal y procesos secundarios cuando esten creados; no prueba por si solo que un service siga activo.
- `top`: debe reflejar actividad CPU durante cargas reales como `Thread Race`.
- `logcat -d -s ...`: debe mostrar eventos relevantes de los experimentos ejecutados.
- `dumpsys meminfo`: debe cambiar durante `Memory Monster`.
- `dumpsys activity services | findstr architecturasos`: debe reflejar services activos cuando corresponda.

## Criterios de aceptacion

- [ ] Existe una entrada de dashboard para `System Diagnostics`.
- [ ] El dashboard sigue sin mostrar estado global por experimento.
- [ ] Existe `PantallaDiagnosticoSistema`.
- [ ] La pantalla de diagnostico muestra las 10 herramientas obligatorias.
- [ ] La pantalla explica que no reimplementa herramientas externas.
- [ ] La pantalla relaciona herramientas con experimentos reales.
- [ ] La pantalla incluye comparacion Android vs Linux en al menos 3 aspectos.
- [ ] Todos los comandos visibles son compatibles con Windows/PowerShell o estan claramente marcados como shell Android.
- [ ] No quedan comandos visibles nuevos con `grep`.
- [ ] Cada comando tiene descripcion, cuando ejecutarlo y como interpretar el resultado.
- [ ] Existe accion para copiar comandos sin agregar dependencias.
- [ ] Cada experimento conserva su propia seccion `Como verificar`.
- [ ] Cada experimento conserva su propio Event Log interno sin boton manual `Limpiar`.
- [ ] Cada experimento envia eventos importantes a Logcat con tag estable.
- [ ] `Thread Race` queda documentado como escenario principal para CPU/profiler/tracing.
- [ ] `Memory Monster` queda documentado como escenario principal para Android Profiler y `dumpsys meminfo`.
- [ ] `Robot Factory` y `Restaurant IPC` quedan documentados como escenarios principales para services/procesos.
- [ ] La app se puede usar sin Internet.
- [ ] Diagnosticos no inicia hilos, sockets, services ni reserva memoria.
- [ ] Las demostraciones siguen siendo repetibles en 30 a 90 segundos.

## Checklist de implementacion

- [ ] Revisar estructura real de `app/src/main/java/io/yerdna/architecturasos/`.
- [ ] Confirmar nombres reales de pantallas existentes antes de editar.
- [ ] Crear modelos simples de diagnostico.
- [ ] Crear repositorio estatico de diagnostico.
- [ ] Crear o reutilizar componente de comandos copiables.
- [ ] Crear `PantallaDiagnosticoSistema`.
- [ ] Agregar textos visibles a `strings.xml`.
- [ ] Conectar diagnosticos al dashboard.
- [ ] Conectar diagnosticos a la navegacion existente sin dependencia nueva.
- [ ] Revisar `Como verificar` de cada experimento existente.
- [ ] Reemplazar comandos `grep` visibles por `findstr` o filtros `adb logcat -d -s`.
- [ ] Revisar que no exista boton manual visible para limpiar logs.
- [ ] Revisar tags Logcat por experimento.
- [ ] Revisar que diagnosticos no cree recursos runtime.
- [ ] Ejecutar verificaciones de busqueda con `Select-String`.
- [ ] Pedir aprobacion antes de cualquier build o test Gradle.

## Decisiones resueltas

- **Raiz real del proyecto**: no existe carpeta `android/`; todas las rutas de archivo y comandos `gradlew.bat`/`Select-String` se ejecutan desde la raiz del repositorio (ver "Estructura real del proyecto (corregida)").
- **Nombres reales y rutas de las pantallas de experimentos**: verificados directamente contra `app/src/main/java/io/yerdna/architecturasos/ui/screen/`. El dashboard real es `PantallaPanelExperimentos.kt` (no `PantallaDashboard.kt`; no existe carpeta `ui/dashboard/`). Los 8 experimentos son `PantallaFabricaRobots.kt`, `PantallaRestauranteIpc.kt`, `PantallaRedAgentes.kt`, `PantallaCarreraHilos.kt`, `PantallaBancoCaotico.kt` (no `PantallaBancoCaos.kt`), `PantallaCarreraBoletos.kt` (no `PantallaCarreraTickets.kt`), `PantallaParqueoInteligente.kt` (no `PantallaEstacionamientoInteligente.kt`) y `PantallaMonstruoMemoria.kt`. Los nombres nuevos de este plan (`HerramientaDiagnostico`, `RepositorioDiagnosticoSistema`, `PantallaDiagnosticoSistema`, `ListaComandosVerificacion`) usan el paquete `diagnostico` (singular, espanol), no `diagnostics`.
- **Estado real de implementacion de los planes 001 a 011**: verificado que los planes `001` a `009` y `011-panel-registro-eventos.md` ya estan implementados en el codigo actual (los 8 modulos, el dashboard, la navegacion y el logger comun existen y compilan). El plan 010 no necesita detenerse por dependencias faltantes.
- **Componente comun de comandos copiables**: verificado que no existe ningun componente equivalente reusable; cada pantalla define su propio `ComoVerificarX` privado con su propia `data class ComandoVerificacionX`. Se crea `ListaComandosVerificacion.kt` como componente nuevo, usado unicamente por `PantallaDiagnosticoSistema.kt`. Este plan no refactoriza los `ComoVerificarX` existentes de los 8 experimentos.
- **Boton `Limpiar` heredado**: aplicada la decision de `lessons.md` #34/#37: ninguna pantalla muestra boton manual de limpieza de registro. `limpiar()` sigue siendo API interna del logger, invocada solo al iniciar una ejecucion nueva.
- **Comandos de tracing por version de Android**: `Perfetto/System Trace` es la unica `HerramientaDiagnostico` de categoria `TRACING`; `atrace` se documenta dentro de su campo `limitacion` como alternativa cuando este disponible, no como tarjeta separada. Total de herramientas: exactamente 10.
- **Comparacion con otro sistema operativo**: se incluye dentro de `PantallaDiagnosticoSistema` una comparacion breve Android vs Linux en procesos, IPC y diagnostico (contenido estatico, sin dependencias nuevas). La comparacion extensa pertenece al informe externo, no al codigo.
- **Ejecucion de verificaciones Gradle**: `build` y `test` quedan como verificacion opcional bajo aprobacion explicita del usuario. La implementacion normal usa `Select-String`/`Get-ChildItem` y pruebas manuales de UI/ADB, sin iniciar Gradle automaticamente.
- **Titulo visible de la pantalla**: `System Diagnostics` es solo el nombre de referencia en ingles de este plan (coincide con `docs/app-requirements.md` y con la convencion de nombres de archivo de plan en ingles, igual que `009-memory-monster-memory.md` para "Monstruo de Memoria"). El texto visible en la UI y en `strings.xml` es en espanol: `Diagnostico del Sistema`.
- **Tag Logcat real de `Robot Factory`**: el codigo ya implementado usa `OSPlayground/FabricaRobots` (`ServicioFabricaRobots.kt:203`), no `OSPlayground/ProcessLab` (que es solo un ejemplo generico en `docs/app-requirements.md`). Este plan documenta el valor real sin modificar el codigo existente.
- **Entrada del dashboard**: se agrega reutilizando el modelo `ExperimentoResumen` y el componente `TarjetaExperimento` ya existentes en `PantallaPanelExperimentos.kt`, con `icono = "SD"` y `colorIcono = Color(0xFF495057)` (color no usado por ninguna tarjeta existente), sin crear un modelo ni tarjeta nueva.
- **Navegacion**: la app ya usa Navigation Compose; se agrega `Navegacion.Ruta.Diagnosticos` y un `composable(...)` en `App.kt` exactamente con el mismo patron que las 8 rutas existentes, sin condicionales ni mecanismo alternativo.
- **Retroalimentacion al copiar comando**: sigue el patron ya establecido en los 8 experimentos (sin Snackbar ni Toast); el boton `Copiar` es la unica retroalimentacion.

## Bloqueos restantes

Ninguno. No queda ninguna decision abierta que requiera respuesta del usuario antes de implementar este plan.
