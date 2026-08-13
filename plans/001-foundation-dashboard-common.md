# Plan 001 - Base Panel Experimentos Comun

## Objetivo

Preparar la base de la app Android OS Playground para que los 8 experimentos puedan integrarse de forma ordenada, repetible y facil de explicar.

## Depende de

- Proyecto Android existente en `android/`.
- Reglas de `android/AGENTS.md`.

## Alcance

- Reemplazar el `Greeting` inicial por una app Compose real.
- Crear panel principal con tarjetas para los 8 experimentos.
- Crear componentes comunes minimos para el panel principal y navegacion.
- Usar Navigation Compose para navegar entre el panel principal y las pantallas de experimentos.
- Preparar la UI para localizacion usando espanol como idioma por defecto.

## Archivos a crear

- `android/app/src/main/java/io/yerdna/architecturasos/ui/App.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/Navegacion.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaPanelExperimentos.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaExperimentoTemporal.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/screen/ExperimentoResumen.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/component/ExperimentoScaffold.kt`

## Archivos a modificar

- `android/gradle/libs.versions.toml`
- `android/app/build.gradle.kts`
- `android/app/src/main/java/io/yerdna/architecturasos/MainActivity.kt`
- `android/app/src/main/res/values/strings.xml`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/theme/Color.kt`, solo si hacen falta ajustes minimos de color.
- `android/app/src/main/java/io/yerdna/architecturasos/ui/theme/Theme.kt`, solo si hacen falta ajustes minimos de tema.

## Pasos

- [ ] Cambiar `app_name` a `Android OS Playground`.
- [ ] Usar nombres en espanol para clases propias del proyecto, excepto `App.kt`, `ExperimentoScaffold` y APIs oficiales de Android/Kotlin.
- [ ] Mantener todos los textos visibles del panel principal, pantalla temporal, botones y componentes comunes en recursos de `strings.xml`.
- [ ] Usar espanol como idioma por defecto en `android/app/src/main/res/values/strings.xml`.
- [ ] No crear `values-es/strings.xml` para el espanol inicial. `values/strings.xml` sera la fuente por defecto; otros idiomas se agregaran despues con carpetas calificadas como `values-en/`.
- [ ] Evitar textos visibles escritos directamente dentro de composables; usar `stringResource(...)` para etiquetas, titulos, descripciones y botones.
- [ ] Escribir los textos visibles de `strings.xml` en espanol correcto, con acentos y signos cuando aplique.
- [ ] Los valores escritos en este plan usan ASCII para evitar mojibake, pero los valores finales en `strings.xml` deben usar acentos correctos.
- [ ] Mantener nombres de recursos sin acentos y en snake_case, por ejemplo `titulo_panel_experimentos`, `descripcion_fabrica_robots`, `accion_abrir`.
- [ ] Mantener nombres de clases, funciones y variables Kotlin sin acentos.
- [ ] Mantener rutas tecnicas y nombres de tecnologias como texto tecnico literal cuando corresponda, porque no requieren traduccion.
- [ ] Definir nombres de recursos claros en espanol, por ejemplo `titulo_panel_experimentos`, `accion_abrir`, `mensaje_modulo_construccion`, `experimento_fabrica_robots_nombre`.

### Dependencias

- [ ] Agregar Navigation Compose como dependencia aprobada para este plan.
- [ ] Usar `androidx.navigation:navigation-compose:2.9.8`.
- [ ] Agregar `navigationCompose = "2.9.8"` en `[versions]` de `android/gradle/libs.versions.toml`.
- [ ] Agregar `androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }` en `[libraries]`.
- [ ] Agregar `implementation(libs.androidx.navigation.compose)` en `android/app/build.gradle.kts`.
- [ ] Agregar Material Icons Extended como dependencia aprobada para este plan.
- [ ] Agregar `androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }` en `[libraries]`; la version debe venir de la Compose BOM existente.
- [ ] Agregar `implementation(libs.androidx.compose.material.icons.extended)` en `android/app/build.gradle.kts`.
- [ ] No usar rutas tipadas en el plan 001.
- [ ] No agregar Kotlin Serialization ni `kotlinx-serialization-json` en el plan 001.
- [ ] No agregar plugins nuevos aparte de los existentes.

### MainActivity y raiz Compose

- [ ] Crear `AplicacionOSPlayground()` como composable raiz.
- [ ] Mantener `enableEdgeToEdge()` en `MainActivity`.
- [ ] `MainActivity` debe llamar solo a `ArchitecturasOSTheme { AplicacionOSPlayground() }` dentro de `setContent`.
- [ ] Quitar `Scaffold`, `Greeting` y `GreetingPreview` de `MainActivity`.
- [ ] El `Scaffold` debe vivir en `ExperimentoScaffold`, no en `MainActivity`.

### Navegacion

- [ ] Crear `Navegacion.kt` con objeto `Navegacion`.
- [ ] Crear `Navegacion.Ruta` con constantes `String` de rutas.
- [ ] Las rutas deben ser simples, sin acentos y en camelCase: `panel`, `fabricaRobots`, `restauranteIpc`, `redAgentes`, `carreraHilos`, `bancoCaotico`, `carreraBoletos`, `parqueoInteligente`, `monstruoMemoria`.
- [ ] Usar las rutas como `Navegacion.Ruta.Panel`, `Navegacion.Ruta.FabricaRobots`, `Navegacion.Ruta.RestauranteIpc`, `Navegacion.Ruta.RedAgentes`, `Navegacion.Ruta.CarreraHilos`, `Navegacion.Ruta.BancoCaotico`, `Navegacion.Ruta.CarreraBoletos`, `Navegacion.Ruta.ParqueoInteligente` y `Navegacion.Ruta.MonstruoMemoria`.
- [ ] Usar `rememberNavController()` y `NavHost` en `AplicacionOSPlayground()`.
- [ ] Configurar `Navegacion.Ruta.Panel` como destino inicial.
- [ ] Navegar desde cada tarjeta del panel principal hacia su ruta de experimento.
- [ ] Cada pantalla de experimento debe tener una barra superior con titulo y boton de volver.
- [ ] El boton de volver debe llamar a `navController.popBackStack()`.
- [ ] El boton atras del sistema debe volver al panel principal por el back stack de Navigation Compose.

### ExperimentoScaffold

- [ ] Crear `ExperimentoScaffold` como componente comun para evitar duplicar `Scaffold` y `TopAppBar`.
- [ ] `ExperimentoScaffold` debe recibir titulo, accion opcional de volver y contenido.
- [ ] Si recibe accion de volver, debe mostrar `IconButton` con icono Material de volver.
- [ ] Usar `Icons.AutoMirrored.Filled.ArrowBack` para respetar direccion de layout cuando aplique.
- [ ] El icono de volver debe tener `contentDescription` desde `strings.xml`, por ejemplo `Volver`.
- [ ] Si no recibe accion de volver, no debe mostrar boton de navegacion.
- [ ] `PantallaPanelExperimentos` debe usar `ExperimentoScaffold` sin accion de volver.
- [ ] `PantallaExperimentoTemporal` debe usar `ExperimentoScaffold` con accion de volver.

### Previews

- [ ] Agregar previews minimas para `PantallaPanelExperimentos`, `PantallaExperimentoTemporal` y `ExperimentoScaffold`.
- [ ] Las previews deben ser simples y no depender de navegacion real.
- [ ] Las previews pueden usar datos de ejemplo locales cuando sea necesario.

### Modelo del panel

- [ ] Crear `ExperimentoResumen` como modelo simple para pintar las tarjetas del panel principal.
- [ ] `ExperimentoResumen` debe tener estos campos: id, icono, colorIcono, nombreResId, conceptoResId, descripcionResId y ruta.
- [ ] `ExperimentoResumen.ruta` debe ser `String`.
- [ ] `colorIcono` debe ser de tipo `Color`, porque `ExperimentoResumen` es un modelo local de UI para el panel principal.
- [ ] Usar `@StringRes` en `nombreResId`, `conceptoResId` y `descripcionResId`.
- [ ] No guardar textos visibles como `String` dentro de `ExperimentoResumen`.
- [ ] Crear la lista de experimentos como constante privada `experimentos` dentro de `PantallaPanelExperimentos.kt`.
- [ ] No crear archivo separado, repository ni capa de datos para esta lista.

### Experimentos del panel

- [ ] Crear tarjetas del panel principal para:
  - Fabrica de Robots / Procesos / `Inicia un servicio en un proceso separado y compara los PID del proceso principal y secundario.`
  - Restaurante IPC / Binder IPC / `Envia una orden entre procesos usando comunicacion Binder y muestra la respuesta del servicio.`
  - Red de Agentes / Sockets / `Conecta un cliente y servidor TCP local para enviar mensajes mediante sockets reales.`
  - Carrera de Hilos / Hilos / `Ejecuta trabajo real con diferentes cantidades de hilos y compara tiempos de ejecucion.`
  - Banco Caotico / Condicion de carrera / `Muestra como varios hilos modifican un valor compartido sin sincronizacion y producen resultados incorrectos.`
  - Carrera por Boletos / Mutex / `Compara el acceso a un recurso critico con y sin proteccion mediante mutex.`
  - Parqueo Inteligente / Semaforo / `Controla cuantos vehiculos pueden entrar al mismo tiempo usando un semaforo real.`
  - Monstruo de Memoria / Memoria / `Reserva y libera memoria de forma controlada para observar cambios en el consumo de la app.`
- [ ] El panel principal debe mostrar titulo `Android OS Playground`.
- [ ] El panel principal debe mostrar subtitulo `Experimentos interactivos de Sistemas Operativos en Android`.
- [ ] Usar `LazyColumn` para listar los experimentos en movil.
- [ ] No usar emojis como iconos para evitar problemas de codificacion.
- [ ] Usar iconos textuales cortos:
  - `FR` para Fabrica de Robots.
  - `IPC` para Restaurante IPC.
  - `RA` para Red de Agentes.
  - `CH` para Carrera de Hilos.
  - `BC` para Banco Caotico.
  - `CB` para Carrera por Boletos.
  - `PI` para Parqueo Inteligente.
  - `MM` para Monstruo de Memoria.
- [ ] Mostrar nombres visibles de modulos y conceptos en espanol desde `strings.xml`.
- [ ] Usar acentos en textos visibles cuando corresponda. Por ejemplo, los valores finales deben mostrarse como Fabrica, Caotico, Condicion y Semaforo con acentos correctos.
- [ ] Mantener rutas tecnicas de Navigation Compose sin acentos y en camelCase.

### TarjetaExperimento

- [ ] Cada tarjeta debe tener icono textual corto, nombre, concepto, descripcion y boton `Abrir`.
- [ ] La tarjeta completa debe ser clickeable y ejecutar la misma accion que el boton `Abrir`.
- [ ] Crear `TarjetaExperimento` como composable privado dentro de `PantallaPanelExperimentos.kt`.
- [ ] No crear `TarjetaExperimento.kt` separado en el plan 001.
- [ ] Extraer `TarjetaExperimento` a `ui/component` solo si otra pantalla lo reutiliza en un plan posterior.
- [ ] `TarjetaExperimento` debe usar `Card` de Material 3.
- [ ] `TarjetaExperimento` debe usar padding interno de `16.dp` y separacion aproximada de `12.dp`.
- [ ] La estructura base debe ser una `Column` con una `Row` superior para icono y textos, y boton `Abrir` debajo alineado al final.
- [ ] El icono textual debe usar caja fija de `48.dp`.
- [ ] El icono textual debe tener forma redondeada.
- [ ] El icono textual debe usar `colorIcono` como fondo.
- [ ] El texto del icono debe estar centrado y tener contraste legible.
- [ ] Cada modulo debe tener un color de icono diferente.
- [ ] Evitar cards dentro de cards.

### Recursos de texto

- [ ] Mostrar textos de UI en espanol por defecto, por ejemplo `Abrir` y `Modulo en construccion`.
- [ ] Crear estos recursos minimos en `strings.xml`; los valores aqui estan escritos en ASCII para evitar mojibake en el plan, pero en `strings.xml` deben escribirse con acentos correctos:
  - `app_name`: `Android OS Playground`.
  - `titulo_panel_experimentos`: `Android OS Playground`.
  - `subtitulo_panel_experimentos`: `Experimentos interactivos de Sistemas Operativos en Android`.
  - `accion_abrir`: `Abrir`.
  - `accion_volver`: `Volver`.
  - `mensaje_modulo_construccion`: `Modulo en construccion`.
  - `experimento_fabrica_robots_nombre`: `Fabrica de Robots`.
  - `experimento_fabrica_robots_concepto`: `Procesos`.
  - `experimento_fabrica_robots_descripcion`: `Inicia un servicio en un proceso separado y compara los PID del proceso principal y secundario.`.
  - `experimento_restaurante_ipc_nombre`: `Restaurante IPC`.
  - `experimento_restaurante_ipc_concepto`: `Binder IPC`.
  - `experimento_restaurante_ipc_descripcion`: `Envia una orden entre procesos usando comunicacion Binder y muestra la respuesta del servicio.`.
  - `experimento_red_agentes_nombre`: `Red de Agentes`.
  - `experimento_red_agentes_concepto`: `Sockets`.
  - `experimento_red_agentes_descripcion`: `Conecta un cliente y servidor TCP local para enviar mensajes mediante sockets reales.`.
  - `experimento_carrera_hilos_nombre`: `Carrera de Hilos`.
  - `experimento_carrera_hilos_concepto`: `Hilos`.
  - `experimento_carrera_hilos_descripcion`: `Ejecuta trabajo real con diferentes cantidades de hilos y compara tiempos de ejecucion.`.
  - `experimento_banco_caotico_nombre`: `Banco Caotico`.
  - `experimento_banco_caotico_concepto`: `Condicion de carrera`.
  - `experimento_banco_caotico_descripcion`: `Muestra como varios hilos modifican un valor compartido sin sincronizacion y producen resultados incorrectos.`.
  - `experimento_carrera_boletos_nombre`: `Carrera por Boletos`.
  - `experimento_carrera_boletos_concepto`: `Mutex`.
  - `experimento_carrera_boletos_descripcion`: `Compara el acceso a un recurso critico con y sin proteccion mediante mutex.`.
  - `experimento_parqueo_inteligente_nombre`: `Parqueo Inteligente`.
  - `experimento_parqueo_inteligente_concepto`: `Semaforo`.
  - `experimento_parqueo_inteligente_descripcion`: `Controla cuantos vehiculos pueden entrar al mismo tiempo usando un semaforo real.`.
  - `experimento_monstruo_memoria_nombre`: `Monstruo de Memoria`.
  - `experimento_monstruo_memoria_concepto`: `Memoria`.
  - `experimento_monstruo_memoria_descripcion`: `Reserva y libera memoria de forma controlada para observar cambios en el consumo de la app.`.

### Estado y alcance futuro

- [ ] No mostrar etiqueta de estado por experimento en el panel principal.
- [ ] No crear `EstadoExperimento` en el plan 001. Los estados de ejecucion se definiran dentro de cada modulo real cuando se implementen los planes 002-009.
- [ ] Crear una sola pantalla temporal reutilizable `PantallaExperimentoTemporal`.
- [ ] `PantallaExperimentoTemporal` debe recibir por parametros el experimento y accion de volver.
- [ ] Usar `PantallaExperimentoTemporal` para las 8 rutas de experimento durante el plan 001.
- [ ] Reemplazar cada uso temporal por la pantalla real correspondiente cuando se ejecuten los planes 002-009.
- [ ] La pantalla temporal debe ser una vista WIP minima.
- [ ] `PantallaExperimentoTemporal` es una vista transitoria de navegacion y no intenta cumplir los requisitos funcionales de cada experimento.
- [ ] Los planes 002-009 deben reemplazar la pantalla temporal por implementaciones reales con controles, metricas, registro de eventos y verificacion cuando aplique.
- [ ] La pantalla temporal debe mostrar barra superior con titulo, boton volver, nombre, concepto y mensaje `Modulo en construccion`.
- [ ] La pantalla temporal no debe mostrar Registro de eventos, Como verificar, metricas ni simulacion visual.
- [ ] La pantalla temporal no debe mostrar acciones de ejecucion como `Iniciar`, `Reiniciar`, `Detener` o `Cancelar`.

### Tema visual

- [ ] Usar `MaterialTheme` y el tema existente.
- [ ] No crear un sistema visual paralelo.
- [ ] Modificar `Color.kt` solo si se necesitan 2 a 4 colores base para identidad visual tecnologica/educativa.
- [ ] Modificar `Theme.kt` solo si hace falta conectar esos colores al tema existente.
- [ ] Mantener contraste legible.
- [ ] No agregar gradientes complejos, decoraciones pesadas ni cambios visuales fuera del alcance del plan 001.

### Pruebas y verificacion

- [ ] No crear pruebas automatizadas nuevas en el plan 001.
- [ ] Verificar que la UI sea responsiva y no dependa de internet.

## Criterios de aceptacion

- [ ] La app abre en el panel principal.
- [ ] Se ven 8 experimentos.
- [ ] El panel principal usa lista vertical con `LazyColumn`.
- [ ] Cada tarjeta puede abrir una pantalla de experimento.
- [ ] Cada tarjeta muestra icono textual corto y boton `Abrir`.
- [ ] Navigation Compose controla la navegacion y el boton atras del sistema.
- [ ] Cada pantalla de experimento tiene barra superior con titulo y navegacion atras.
- [ ] `ExperimentoScaffold` evita duplicar estructura de pantalla entre el panel principal y las pantallas de experimento.
- [ ] Las 8 rutas de experimento usan temporalmente `PantallaExperimentoTemporal`.
- [ ] La pantalla temporal no muestra controles de ejecucion hasta que el experimento real exista.
- [ ] Cada ruta temporal muestra una vista WIP minima con el mensaje `Modulo en construccion`.
- [ ] `MainActivity` solo configura tema y llama al composable raiz.
- [ ] `MainActivity` mantiene `enableEdgeToEdge()`.
- [ ] Los textos visibles comunes y del panel principal salen de `strings.xml`.
- [ ] Las clases propias nuevas usan nombres en espanol, salvo `App.kt` y `ExperimentoScaffold`.
- [ ] La app queda preparada para agregar otros idiomas sin cambiar codigo Kotlin de UI.
- [ ] Solo se agregaron las dependencias aprobadas de Navigation Compose y Material Icons Extended.
- [ ] El panel principal no muestra estado por experimento.
- [ ] No se creo un sistema visual paralelo al tema Material 3 existente.
- [ ] Existen previews minimas para revisar la UI base en Android Studio.
- [ ] No se agregaron pruebas automatizadas nuevas.

## Verificacion

Ejecutar desde `android/`:

```powershell
.\gradlew.bat build
```

Validar manualmente:

- Abrir la app.
- Entrar y salir de cada experimento.
- Confirmar que las 8 tarjetas abren su ruta temporal.
- Confirmar que el boton de volver regresa al panel principal.
- Confirmar que el boton atras del sistema regresa al panel principal desde una ruta temporal.
- Confirmar que ninguna pantalla crashea.
- Cambiar temporalmente el idioma del emulador o revisar recursos para confirmar que la UI usa `stringResource` y no textos visibles escritos directamente en Kotlin.

## Estado de aprobacion

- [x] Aprobado explicitamente por el usuario.
- [x] Implementado dentro de `android/`.
