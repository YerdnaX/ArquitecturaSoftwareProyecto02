# Plan 010 - System Diagnostics y pulido de verificacion

## Objetivo

Agregar la pantalla `System Diagnostics`, cerrar la observabilidad transversal de la aplicacion y dejar verificable que los experimentos generan actividad real observable con herramientas Android/ADB.

Este plan no implementa nuevos mecanismos de sistemas operativos. Solo integra una pantalla educativa de diagnostico, reutiliza patrones comunes ya existentes y corrige la documentacion/verificacion de los 8 experimentos cuando falte o contradiga decisiones cerradas.

## Contexto obligatorio aplicado

- Proyecto Android nativo con Kotlin, Jetpack Compose, Material 3 y AndroidX.
- Raiz Android real: `android/`.
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

Si alguno de esos planes no existe implementado al iniciar este plan, se debe detener la implementacion y actualizar primero el estado real de dependencias. No se deben implementar funcionalidades faltantes de experimentos dentro de este plan.

## Alcance incluido

- Crear la pantalla `PantallaDiagnosticoSistema`.
- Crear datos estaticos simples para herramientas de diagnostico.
- Agregar una entrada de dashboard para abrir diagnosticos sin estado global.
- Agregar ruta de navegacion si la navegacion comun ya existe.
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

- `android/app/src/main/java/io/yerdna/architecturasos/diagnostics/HerramientaDiagnostico.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/diagnostics/RepositorioDiagnosticoSistema.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaDiagnosticoSistema.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/component/ListaComandosVerificacion.kt`, solo si no existe un componente comun equivalente.

## Archivos exactos a modificar

- `android/app/src/main/java/io/yerdna/architecturasos/ui/dashboard/PantallaDashboard.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/App.kt`
- `android/app/src/main/res/values/strings.xml`

Modificar tambien estos archivos solo si existen y les falta o contradicen la seccion `Como verificar`, logs, limpieza o controles ya definidos:

- `android/app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaFabricaRobots.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaRestauranteIpc.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaRedAgentes.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaCarreraHilos.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaBancoCaos.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaCarreraTickets.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaEstacionamientoInteligente.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaMonstruoMemoria.kt`

Si los nombres reales de las pantallas difieren por planes previos, usar los nombres reales existentes y mantener nombres propios nuevos en espanol. No crear pantallas duplicadas.

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

- Titulo `System Diagnostics`.
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

Si ya existe un componente equivalente para `Como verificar`, modificar ese componente en lugar de crear uno nuevo.

### `PantallaDashboard.kt`

Debe agregar una entrada para `System Diagnostics` con:

- icono o simbolo consistente con las tarjetas existentes;
- nombre visible;
- concepto tecnico `Observabilidad Android`;
- descripcion breve;
- boton para abrir.

No debe agregar estado global de diagnostico ni estado por experimento.

### `App.kt`

Debe registrar la pantalla `System Diagnostics` en el mecanismo de navegacion existente.

Si la app no usa Navigation Compose y tiene navegacion simple por estado local, agregar una opcion de pantalla local. No agregar dependencia de navegacion nueva.

### `strings.xml`

Debe incluir todos los textos visibles nuevos:

- titulos;
- descripciones;
- nombres de acciones;
- categorias;
- mensajes de copia;
- descripciones de comandos;
- textos de limitaciones;
- textos de comparacion Android vs Linux.

Los comandos literales pueden vivir en modelos Kotlin porque son contenido tecnico copiable.

## Herramientas diagnosticas obligatorias

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
- Limitacion: no usar AIDL, `LocalBinder`, broadcasts ni archivos temporales si el plan 003 decidio `Messenger`.

### sockets

- Categoria: `SOCKETS`.
- Valida comunicacion TCP local real con `ServerSocket` y `Socket`.
- Experimento principal: `Agent Network`.
- Limitacion: no prueba Internet externo; el permiso `INTERNET` solo habilita APIs de red Android.

### Perfetto/System Trace

- Categoria: `TRACING`.
- Valida actividad de CPU y threads durante carga controlada.
- Experimento principal: `Thread Race`.
- Limitacion: la disponibilidad depende de Android Studio, version de Android y dispositivo/emulador.

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
adb logcat -d -s OSPlayground/ProcessLab OSPlayground/BinderIPC OSPlayground/SocketLab OSPlayground/ThreadRace OSPlayground/ChaosBank OSPlayground/Mutex OSPlayground/Semaphore OSPlayground/Memory
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

Cada experimento debe documentar y usar un tag estable:

- `OSPlayground/ProcessLab` para `Robot Factory`.
- `OSPlayground/BinderIPC` para `Restaurant IPC`.
- `OSPlayground/SocketLab` para `Agent Network`.
- `OSPlayground/ThreadRace` para `Thread Race`.
- `OSPlayground/ChaosBank` para `Chaos Bank`.
- `OSPlayground/Mutex` para `Ticket Rush`.
- `OSPlayground/Semaphore` para `Smart Parking`.
- `OSPlayground/Memory` para `Memory Monster`.

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
- Al copiar comando: escribir el texto del comando en clipboard y mostrar feedback visual breve si el patron existente lo permite.
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

### Buscar comandos incompatibles con Windows

```powershell
Select-String -Path android/app/src/main/java/io/yerdna/architecturasos/**/*.kt,android/app/src/main/res/values/strings.xml -Pattern "grep "
```

Debe no devolver comandos visibles nuevos con `grep`. Si aparece texto heredado, reemplazarlo por `findstr` o por `adb logcat -d -s TAG`.

### Revisar botones manuales de limpieza de logs

```powershell
Select-String -Path android/app/src/main/java/io/yerdna/architecturasos/**/*.kt,android/app/src/main/res/values/strings.xml -Pattern "Limpiar|Clear"
```

Debe no mostrar un boton manual de limpieza del registro de eventos. Puede existir una funcion interna `limpiar()` del logger si no es visible como accion manual.

### Revisar tags Logcat documentados

```powershell
Select-String -Path android/app/src/main/java/io/yerdna/architecturasos/**/*.kt -Pattern "OSPlayground/"
```

Debe mostrar tags estables por experimento y no un tag global ambiguo.

### Verificacion manual en dispositivo o emulador

Ejecutar desde una terminal externa cuando la app este instalada:

```powershell
adb devices
adb shell ps -A | findstr architecturasos
adb shell top
adb logcat -d -s OSPlayground/ProcessLab OSPlayground/BinderIPC OSPlayground/SocketLab OSPlayground/ThreadRace OSPlayground/ChaosBank OSPlayground/Mutex OSPlayground/Semaphore OSPlayground/Memory
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

- [ ] Revisar estructura real de `android/app/src/main/java/io/yerdna/architecturasos/`.
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

## Bloqueos, contradicciones y ambiguedades restantes

### 1. Punto: nombres reales y rutas de las pantallas de experimentos

Por que bloquea o puede causar implementaciones distintas:
Los planes y requisitos usan nombres conceptuales y algunos nombres propuestos en espanol, pero la estructura real de los planes anteriores puede haber creado rutas distintas. Si un agente crea los archivos propuestos sin revisar los existentes, puede duplicar pantallas o dejar la navegacion apuntando a clases incorrectas.

Solucion concreta alineada:
Antes de implementar, revisar `android/app/src/main/java/io/yerdna/architecturasos/` con `rg --files` y usar los nombres reales existentes. Si difieren, modificar las pantallas reales y mantener solo los nombres nuevos de este plan en espanol: `HerramientaDiagnostico`, `RepositorioDiagnosticoSistema`, `PantallaDiagnosticoSistema` y `ListaComandosVerificacion`.

### 2. Punto: estado real de implementacion de los planes 001 a 009

Por que bloquea o puede causar implementaciones distintas:
Este plan depende de que existan dashboard, navegacion, componentes comunes, logger y los 8 experimentos. Si alguno falta, un agente podria intentar completar funcionalidades de modulos anteriores dentro del plan 010, ampliando el alcance y rompiendo la regla de trabajar plan por plan.

Solucion concreta alineada:
Al iniciar implementacion, verificar presencia de archivos y pantallas de los planes 001 a 009. Si faltan dependencias funcionales, detener este plan y pedir al usuario completar o aprobar el plan correspondiente. El plan 010 solo puede agregar diagnosticos y pulir verificacion de archivos ya existentes.

### 3. Punto: componente comun de comandos copiables

Por que bloquea o puede causar implementaciones distintas:
El plan pide crear `ListaComandosVerificacion.kt` solo si no existe equivalente. Si existe un componente con otro nombre, crear uno nuevo duplicaria UI y responsabilidades.

Solucion concreta alineada:
Buscar primero componentes existentes relacionados con `Comando`, `Verificacion`, `HowToVerify` o `ComoVerificar`. Si existe uno reusable, extenderlo manteniendo su estilo. Si no existe, crear `ListaComandosVerificacion.kt` con responsabilidad visual simple y sin ejecutar comandos.

### 4. Punto: textos visibles heredados con boton `Limpiar`

Por que bloquea o puede causar implementaciones distintas:
`docs/app-requirements.md` menciona boton para limpiar logs, pero `plans/lessons.md` lo corrige: ninguna pantalla debe mostrar limpieza manual. Si un agente sigue solo el requerimiento original, agregaria o conservaria una accion visible ya descartada.

Solucion concreta alineada:
Aplicar la decision mas reciente de `lessons.md`: eliminar o no crear botones visibles `Limpiar` para registros de eventos. Mantener `limpiar()` solo como API interna del logger o del ciclo de vida cuando una nueva ejecucion de experimento deba iniciar con buffer nuevo.

### 5. Punto: comandos de tracing disponibles por version Android

Por que bloquea o puede causar implementaciones distintas:
Perfetto, System Trace, systrace y atrace no estan disponibles igual en todos los entornos. Si se documenta un unico comando obligatorio, puede fallar durante la exposicion segun emulador, version de Android o Android Studio.

Solucion concreta alineada:
Documentar `Thread Race` como escenario de carga para tracing y explicar que la herramienta concreta se selecciona segun disponibilidad: Android Studio Profiler/System Trace o Perfetto primero; `atrace` solo cuando este disponible. No exigir que la app ejecute esas herramientas.

### 6. Punto: comparacion con otro sistema operativo

Por que bloquea o puede causar implementaciones distintas:
La rubrica del PDF exige comparar Android con al menos un sistema operativo adicional en al menos 3 aspectos, pero los requisitos de app no dicen si esa comparacion debe vivir dentro de la aplicacion, informe o ambos.

Solucion concreta alineada:
Para este plan, incluir en `PantallaDiagnosticoSistema` una comparacion breve Android vs Linux en procesos, IPC y diagnostico, porque es contenido estatico, no agrega dependencias y ayuda a cubrir la exposicion. La comparacion extensa y citas APA pertenecen al informe externo, no al codigo.

### 7. Punto: ejecucion de verificaciones Gradle

Por que bloquea o puede causar implementaciones distintas:
El plan original ordenaba ejecutar `build` y `test`, pero el usuario indico no ejecutar build y `lessons.md` exige pedir confirmacion antes. Un agente podria ejecutar comandos no autorizados.

Solucion concreta alineada:
Mantener `build` y `test` como verificacion opcional bajo aprobacion explicita. Para la implementacion normal de este plan, usar revisiones estaticas con `Select-String` y pruebas manuales de UI/ADB descritas, sin iniciar Gradle automaticamente.
