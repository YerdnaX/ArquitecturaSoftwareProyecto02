# Plan 003 - Restaurante IPC / Binder IPC

## Objetivo

Implementar el modulo Restaurante IPC para demostrar comunicacion real entre dos procesos Android usando Binder IPC mediante `Messenger`.

Concepto visual:

- Mesero: proceso principal de la app.
- Cocina: `Service` en proceso secundario.
- Orden: mensaje IPC enviado desde el mesero.
- Respuesta: mensaje IPC de retorno enviado por la cocina.

La visualizacion de mesero/cocina no sustituye el mecanismo real. La orden debe enviarse realmente al `Service` en proceso separado usando `Messenger`.

## Depende de

- `001-foundation-dashboard-common.md`
- Reglas de `android/AGENTS.md`
- Reglas de `plans/lessons.md`
- Requerimientos del modulo en `docs/app-requirements.md`
- Requerimientos crudos en `docs/raw-requirements.pdf`

## Cobertura academica

Este modulo cubre:

- Comunicacion real entre procesos Android.
- Binder IPC mediante `Messenger`.
- Proceso principal como cliente/mesero.
- Proceso secundario como servidor/cocina.
- Mensajes de ida y vuelta con `Message.replyTo`.
- Visualizacion de PID origen y PID destino.
- Cola FIFO de mensajes IPC en el servicio.
- Observabilidad mediante Logcat, `ps -A` y `dumpsys activity services`.

## Archivos a crear

- `android/app/src/main/java/io/yerdna/architecturasos/restaurante/ContratoRestauranteIpc.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/restaurante/EstadoRestauranteIpc.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/restaurante/ServicioRestauranteIpc.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/restaurante/ControladorRestauranteIpc.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/restaurante/RestauranteIpcViewModel.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaRestauranteIpc.kt`

## Archivos a modificar

- `android/app/src/main/AndroidManifest.xml`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/App.kt`
- `android/app/src/main/res/values/strings.xml`

No modificar rutas de otros experimentos. No modificar el dashboard salvo que exista una necesidad estricta relacionada con este modulo.

## Decisiones tecnicas

- Usar `Messenger` como unica tecnologia IPC de este modulo.
- Describirlo como Binder IPC mediante `Messenger`, porque `Messenger` usa Binder por debajo.
- No usar AIDL.
- No usar `LocalBinder`.
- No usar broadcasts.
- No usar archivos temporales.
- No agregar dependencias nuevas.
- El servicio debe ejecutarse en proceso separado `:cocina_restaurante`.
- El proceso principal envia ordenes con `Messenger`.
- El servicio responde usando `Message.replyTo`.
- El servicio mantiene una cola FIFO de ordenes en memoria.
- La cocina procesa una orden a la vez, pero puede recibir varias ordenes y dejarlas en cola.
- No definir limite fijo de ordenes pendientes en este plan.
- No implementar panel visual de eventos en este plan. Solo registrar eventos relevantes en Logcat.

## Manifest

Agregar el servicio sin modificar `ServicioFabricaRobots`:

```xml
<service
    android:name=".restaurante.ServicioRestauranteIpc"
    android:exported="false"
    android:process=":cocina_restaurante" />
```

## Contrato Messenger

Crear `ContratoRestauranteIpc` para centralizar constantes de mensajes, claves de `Bundle`, tag de Logcat y demora fija.

Tag Logcat:

- `OSPlayground/BinderIPC`

Demora fija:

- `DEMORA_PREPARACION_MS = 1500L`

Mensajes cliente -> servicio:

- `MSG_REGISTRAR_CLIENTE`
- `MSG_ENVIAR_ORDEN`
- `MSG_DESCONECTAR_CLIENTE`

Mensajes servicio -> cliente:

- `MSG_COCINA_CONECTADA`
- `MSG_ORDEN_EN_COLA`
- `MSG_ORDEN_RECIBIDA`
- `MSG_ORDEN_PROCESANDO`
- `MSG_RESPUESTA_ENVIADA`
- `MSG_ERROR_IPC`

Estado local del cliente:

- `RespuestaRecibida` se maneja en `RestauranteIpcViewModel`, no como mensaje enviado por el servicio.

Claves de `Bundle`:

- `KEY_ID_ORDEN`
- `KEY_ORDEN`
- `KEY_RESPUESTA`
- `KEY_PID_ORIGEN`
- `KEY_PID_DESTINO`
- `KEY_TIMESTAMP`
- `KEY_MENSAJE_ERROR`
- `KEY_ORDENES_EN_COLA`
- `KEY_TOTAL_PROCESADAS`

## Responsabilidades por archivo

### `ContratoRestauranteIpc.kt`

- Definir constantes `MSG_*`.
- Definir constantes `KEY_*`.
- Definir tag Logcat `OSPlayground/BinderIPC`.
- Definir `DEMORA_PREPARACION_MS = 1500L`.

### `EstadoRestauranteIpc.kt`

- Definir `data class EstadoRestauranteIpc`.
- Definir `enum class EstadoConexionRestaurante`.
- Definir `enum class EstadoOrdenRestaurante`.
- Definir `enum class ResultadoRestauranteIpc`.
- Separar estado actual, resultado historico y metricas.

### `ServicioRestauranteIpc.kt`

- Implementar un `Service` con `Messenger`.
- Ejecutarse en el proceso `:cocina_restaurante`.
- Obtener el PID de cocina con `android.os.Process.myPid()`.
- Usar un `Handler` sin leaks con clase privada y `WeakReference`.
- Mantener cola FIFO de ordenes en memoria.
- Procesar una orden a la vez con `Handler.postDelayed`.
- Responder al cliente usando `Message.replyTo`.
- Cancelar orden actual y vaciar cola al desconectar, reiniciar o destruir el servicio.
- Registrar eventos relevantes en Logcat.

### `ControladorRestauranteIpc.kt`

- Manejar `applicationContext`.
- Manejar `Intent`.
- Manejar `ServiceConnection`.
- Manejar `Messenger` hacia el servicio.
- Manejar `Messenger` de respuesta hacia la pantalla.
- Implementar `conectar()`.
- Implementar `enviarOrden(orden, idOrden, pidOrigen)`.
- Implementar `desconectar()`.
- Implementar reinicio con limpieza y reconexion.
- Implementar `liberar()` como limpieza idempotente.
- Reportar eventos al `RestauranteIpcViewModel` mediante callbacks simples.

### `RestauranteIpcViewModel.kt`

- No recibir ni guardar `Context`.
- Mantener estado puro de UI.
- Generar `idOrden`.
- Actualizar estados de conexion y orden.
- Actualizar metricas.
- Guardar orden actual, ultima respuesta y errores.
- Preparar orden antes de enviarla.
- Marcar orden enviada.
- Procesar eventos recibidos desde el controlador.
- Reiniciar estado cuando corresponda.

### `PantallaRestauranteIpc.kt`

- Obtener `LocalContext.current.applicationContext`.
- Crear `ControladorRestauranteIpc` con `remember`.
- Usar `RestauranteIpcViewModel`.
- Coordinar callbacks del controlador hacia el ViewModel.
- Usar `BackHandler` para boton atras del sistema y gesto.
- Usar `AlertDialog` para confirmar salida cuando exista conexion activa o trabajo pendiente.
- Usar `AlertDialog` para confirmar reinicio cuando exista servicio activo o trabajo pendiente.
- Usar `DisposableEffect` para llamar `controlador.liberar()` como limpieza defensiva.
- Renderizar UI pura mediante un composable interno `ContenidoRestauranteIpc(...)`.
- Mostrar seccion `Como verificar`.
- Agregar previews con datos falsos.

## Estado interno

`EstadoRestauranteIpc` debe separar estado de conexion, estado de orden, resultado historico y metricas.

Estados de conexion:

- `Desconectado`
- `Conectando`
- `Conectado`
- `Error`

Estados de orden:

- `SinOrden`
- `PreparandoMensaje`
- `Enviando`
- `EnCola`
- `Recibido`
- `Procesando`
- `RespuestaEnviada`
- `RespuestaRecibida`
- `Error`

Resultado historico:

- `SinEjecucion`
- `Completado`
- `Cancelado`
- `Error`

Metricas y datos:

- `pidMesero`
- `pidCocina`
- `ordenActual`
- `respuestaActual`
- `idOrdenActual`
- `timestampUltimoEvento`
- `mensajeError`
- `mensajesIntercambiados`
- `ordenesEnCola`
- `totalOrdenesProcesadas`

`idOrdenActual` representa la orden que la cocina esta procesando o la ultima orden reportada por el servicio. El historial visual de eventos queda fuera de este plan.

## Ordenes y validacion

- La pantalla debe permitir escribir una orden en un campo de texto libre.
- Valor inicial del input: vacio.
- `Enviar orden` se habilita solo si el texto no esta vacio despues de `trim()`.
- No definir limite fijo de caracteres.
- La UI debe manejar textos largos con `maxLines` y `TextOverflow.Ellipsis` donde sea necesario.
- El detalle o resultado puede mostrar la orden completa si el espacio lo permite.
- Al enviar, usar el texto con `trim()`.
- El input queda editable mientras la cocina este conectada para permitir enviar varias ordenes y demostrar la cola.
- Agregar botones rapidos para cargar ejemplos:
  - `Pizza`
  - `Hamburguesa`
  - `Ensalada`

## Flujo exacto de ordenes

1. El usuario escribe o selecciona una orden.
2. El usuario toca `Enviar orden`.
3. `RestauranteIpcViewModel` cambia estado a `PreparandoMensaje`, crea `idOrdenActual`, guarda `ordenActual`, timestamp y PID origen.
4. `ControladorRestauranteIpc` envia `MSG_ENVIAR_ORDEN` al servicio con `KEY_ID_ORDEN`, `KEY_ORDEN`, `KEY_PID_ORIGEN`, `KEY_TIMESTAMP` y `replyTo`.
5. La UI cambia a `Enviando`.
6. `ServicioRestauranteIpc` recibe el mensaje.
7. Si no hay otra orden en proceso, la orden pasa a procesamiento.
8. Si hay otra orden en proceso, la orden entra a la cola FIFO y el servicio responde `MSG_ORDEN_EN_COLA`.
9. El servicio responde `MSG_ORDEN_RECIBIDA` cuando toma una orden para procesarla.
10. La UI muestra estado `Recibido`.
11. El servicio responde `MSG_ORDEN_PROCESANDO`.
12. La UI muestra estado `Procesando`.
13. El servicio espera `DEMORA_PREPARACION_MS`.
14. El servicio genera respuesta con formato `Orden lista: <orden>`.
15. El servicio responde `MSG_RESPUESTA_ENVIADA` con id de orden, orden original, respuesta, PID destino, timestamp, ordenes en cola y total procesadas.
16. El cliente recibe la respuesta.
17. `RestauranteIpcViewModel` marca estado local `RespuestaRecibida`, resultado `Completado`, guarda respuesta, timestamp y metricas.
18. Si hay ordenes pendientes, el servicio toma la siguiente orden de la cola.

Cada respuesta debe incluir:

- `idOrden`
- orden original
- respuesta
- PID origen del mesero
- PID destino de la cocina
- timestamp
- ordenes en cola
- total de ordenes procesadas

## Controles

Controles por estado:

- `Desconectado`:
  - `Conectar` habilitado.
  - `Desconectar` deshabilitado.
  - `Enviar orden` deshabilitado.
  - Input de orden habilitado.
  - `Reiniciar` habilitado solo si hay datos previos.
- `Conectando`:
  - Botones principales deshabilitados.
  - Input deshabilitado.
- `Conectado`:
  - `Conectar` deshabilitado.
  - `Desconectar` habilitado.
  - `Enviar orden` habilitado si el input no esta vacio.
  - Input habilitado.
  - `Reiniciar` habilitado.
- `Conectado` con orden en proceso o cola pendiente:
  - `Enviar orden` sigue habilitado si el input no esta vacio.
  - Input sigue habilitado.
  - `Desconectar` habilitado.
  - `Reiniciar` habilitado, pero debe pedir confirmacion.
- `Error` de conexion:
  - `Conectar` habilitado para reintentar.
  - `Desconectar` deshabilitado si no hay servicio activo.
  - `Enviar orden` deshabilitado.
  - Input habilitado.
  - `Reiniciar` habilitado.

No permitir doble conexion. Durante reinicio o limpieza, deshabilitar controles para evitar doble accion.

## Desconectar

`Desconectar` no pide confirmacion.

Al tocar `Desconectar`, siempre debe:

- registrar evento en Logcat;
- cancelar la orden actual si existe;
- cancelar la cola pendiente si existe;
- si existe `Messenger` del servicio disponible, enviar `MSG_DESCONECTAR_CLIENTE` antes de liberar recursos;
- si no existe `Messenger` del servicio disponible, continuar con limpieza local;
- liberar `Messenger`;
- hacer `unbindService` si esta enlazado;
- hacer `stopService`;
- limpiar datos de orden, respuesta, error y metricas de ejecucion;
- cambiar conexion a `Desconectado`;
- cambiar orden a `SinOrden`.

Si habia trabajo pendiente, el resultado historico queda como `Cancelado`.

## Reiniciar

Si no hay servicio activo ni trabajo pendiente:

- limpiar datos de orden, respuesta, error y metricas.

Si hay servicio conectado, orden en proceso o cola pendiente:

- mostrar `AlertDialog`.
- Si el usuario cancela, no cambiar nada.
- Si el usuario confirma:
  - registrar evento en Logcat;
  - si existe `Messenger` del servicio disponible, enviar `MSG_DESCONECTAR_CLIENTE` antes de liberar recursos;
  - si no existe `Messenger` del servicio disponible, continuar con limpieza local;
  - cancelar orden actual;
  - vaciar cola;
  - liberar `Messenger`;
  - hacer `unbindService`;
  - hacer `stopService`;
  - limpiar estado y metricas;
  - volver a iniciar/enlazar el servicio para dejar el experimento listo.

Textos del dialogo:

- Titulo: `Reiniciar experimento`.
- Mensaje: `Se detendra la cocina, se cancelaran las ordenes pendientes y se limpiaran los datos actuales antes de volver a conectar.`
- Confirmar: `Reiniciar`.
- Cancelar: `Continuar`.

## Ciclo de vida y salida

- Al entrar a `PantallaRestauranteIpc`, el servicio no se conecta automaticamente.
- El usuario toca `Conectar` para iniciar/enlazar el servicio.
- Salir de la pantalla pide confirmacion si hay conexion activa, orden en proceso o cola pendiente.
- Confirmar salida ejecuta la misma limpieza que `Desconectar` y luego navega al dashboard.
- Cancelar salida mantiene la pantalla y el procesamiento.
- Si el estado es `Desconectado`, salir no pide confirmacion.
- El boton volver de la barra superior, el boton atras del sistema y el gesto atras deben usar la misma logica.
- `BackHandler` debe cubrir boton atras del sistema y gesto.
- `DisposableEffect` debe llamar `controlador.liberar()` como limpieza defensiva.
- `DisposableEffect` no reemplaza la confirmacion normal de salida.

Textos del dialogo de salida:

- Titulo: `Salir del experimento`.
- Mensaje: `La cocina esta conectada. Si sales ahora, se cancelaran las ordenes pendientes y se liberaran los recursos.`
- Confirmar: `Salir y desconectar`.
- Cancelar: `Continuar`.

## Manejo de errores

Errores minimos a manejar:

- fallo al conectar o bindear el servicio;
- servicio desconectado inesperadamente;
- intento de enviar orden sin `Messenger` disponible;
- `RemoteException` al enviar mensaje;
- respuesta sin datos requeridos;
- error interno del servicio.

Comportamiento:

- Mostrar error inline, no `AlertDialog`.
- Registrar error en Logcat con `OSPlayground/BinderIPC`.
- Cambiar `estadoConexion` a `Error` si el problema rompe la conexion.
- Cambiar `estadoOrden` a `Error` si el problema afecta solo una orden.
- Intentar limpiar recursos si el error deja la conexion en estado dudoso.
- Permitir reintentar con `Conectar` desde error de conexion.
- Permitir `Reiniciar` desde error.
- No cerrar la app.

## UI requerida

La pantalla debe mostrar:

- titulo del modulo;
- concepto Binder IPC;
- explicacion corta;
- controles de conexion, envio, desconexion y reinicio;
- campo de texto para orden;
- botones rapidos de orden;
- visualizacion de mesero y cocina como entidades separadas;
- PID del mesero;
- PID de la cocina cuando este disponible;
- estado de conexion;
- estado de orden;
- orden actual;
- ultima respuesta;
- ordenes en cola;
- total de ordenes procesadas;
- mensajes intercambiados;
- ultimo evento o timestamp;
- mensaje de error inline cuando exista;
- seccion `Como verificar`.

La visualizacion debe mostrar el recorrido de la orden y la respuesta de forma simple, pero los datos mostrados deben venir del mecanismo real de `Messenger`.

## Recursos de texto

Mantener todos los textos visibles nuevos en `android/app/src/main/res/values/strings.xml`, usando espanol como idioma por defecto.

Agregar recursos para:

- titulo del modulo;
- concepto Binder IPC;
- explicacion corta;
- acciones `Conectar`, `Desconectar`, `Enviar orden`, `Reiniciar`, `Copiar`, `Continuar`;
- botones rapidos `Pizza`, `Hamburguesa`, `Ensalada`;
- estados de conexion `Desconectado`, `Conectando`, `Conectado`, `Error`;
- estados de orden `Sin orden`, `Preparando mensaje`, `Enviando`, `En cola`, `Recibido`, `Procesando`, `Respuesta enviada`, `Respuesta recibida`;
- nombres visuales `Mesero`, `Cocina`, `Orden`, `Respuesta`;
- metricas `PID mesero`, `PID cocina`, `Ordenes en cola`, `Ordenes procesadas`, `Mensajes intercambiados`, `Ultimo evento`;
- dialogo de salida;
- dialogo de reinicio;
- error de orden vacia;
- error de conexion;
- error de comunicacion;
- seccion `Como verificar`;
- descripciones de comandos de verificacion.

## Integracion con navegacion

- Modificar `android/app/src/main/java/io/yerdna/architecturasos/ui/App.kt`.
- Reemplazar la ruta temporal `Navegacion.Ruta.RestauranteIpc` para que use `PantallaRestauranteIpc(onVolver = { navController.popBackStack() })`.
- Mantener la constante existente `Navegacion.Ruta.RestauranteIpc = "restauranteIpc"`.
- No modificar rutas de otros experimentos.
- No modificar el dashboard salvo necesidad estricta.
- No tocar `PantallaExperimentoTemporal` salvo que quede algun import no usado al compilar.

## Registro de eventos

En este plan no se implementa panel visual de eventos.

No crear componente comun de eventos. No crear lista visual local de eventos.

Registrar solo en Logcat con tag `OSPlayground/BinderIPC`.

Eventos minimos:

- conexion solicitada;
- cocina conectada;
- PID mesero;
- PID cocina;
- orden enviada;
- orden recibida;
- orden en cola cuando una orden entre a la cola FIFO;
- orden procesando;
- respuesta enviada;
- respuesta recibida;
- desconexion;
- reinicio;
- orden actual cancelada;
- cola vaciada;
- limpieza de recursos;
- error.

## Previews

Agregar previews simples de `PantallaRestauranteIpc`.

Las previews:

- usan datos falsos/locales;
- no inician `Service`;
- no crean `Messenger`;
- no crean `ControladorRestauranteIpc`;
- no usan recursos Android externos de runtime.

Estados minimos para previews:

- `Desconectado`;
- `Conectado`;
- `Procesando con cola`;
- `Error`.

Para permitir previews, separar UI pura en un composable interno:

- `ContenidoRestauranteIpc(...)`

`PantallaRestauranteIpc` coordina controlador/ViewModel. `ContenidoRestauranteIpc` solo renderiza estado y callbacks.

## Pasos

- [ ] Crear `ContratoRestauranteIpc` con constantes `MSG_*`, `KEY_*`, tag Logcat y demora fija.
- [ ] Crear `EstadoRestauranteIpc` con estados de conexion, estados de orden, resultado historico, datos y metricas.
- [ ] Crear `ServicioRestauranteIpc` con `Messenger`, `Handler` sin leaks y cola FIFO.
- [ ] Configurar `ServicioRestauranteIpc` en `AndroidManifest.xml` con `android:process=":cocina_restaurante"`.
- [ ] Crear `ControladorRestauranteIpc` para manejar conexion, mensajes, desconexion, reinicio y limpieza idempotente.
- [ ] Crear `RestauranteIpcViewModel` sin `Context`.
- [ ] Crear `PantallaRestauranteIpc`.
- [ ] Crear `ContenidoRestauranteIpc(...)` para UI pura y previews.
- [ ] Implementar conexion manual con boton `Conectar`.
- [ ] Implementar envio de ordenes mediante `Messenger`.
- [ ] Implementar cola FIFO en el servicio.
- [ ] Implementar procesamiento de una orden cada `1500 ms`.
- [ ] Implementar respuesta `Orden lista: <orden>`.
- [ ] Mostrar PID mesero y PID cocina.
- [ ] Mostrar estado de conexion y estado de orden.
- [ ] Mostrar orden actual, ultima respuesta, ordenes en cola, total procesadas y mensajes intercambiados.
- [ ] Implementar `Desconectar` sin confirmacion y con limpieza completa.
- [ ] Implementar `Reiniciar` con confirmacion cuando exista servicio activo o trabajo pendiente.
- [ ] Implementar confirmacion de salida cuando exista conexion activa o trabajo pendiente.
- [ ] Implementar `BackHandler`.
- [ ] Implementar `DisposableEffect` como limpieza defensiva.
- [ ] Manejar errores inline y en Logcat.
- [ ] Registrar eventos relevantes en Logcat con tag `OSPlayground/BinderIPC`.
- [ ] No implementar panel visual de eventos.
- [ ] Agregar textos visibles nuevos en `strings.xml`.
- [ ] Reemplazar la ruta temporal `restauranteIpc` en `App.kt`.
- [ ] Agregar seccion `Como verificar` con comandos compatibles con Windows/PowerShell y botones `Copiar`.
- [ ] Agregar previews para `Desconectado`, `Conectado`, `Procesando con cola` y `Error`.

## Criterios de aceptacion

- [ ] El servicio corre en proceso separado `:cocina_restaurante`.
- [ ] El mesero envia ordenes reales al servicio mediante `Messenger`.
- [ ] La cocina responde usando `Message.replyTo`.
- [ ] La UI muestra PID del mesero y PID de la cocina.
- [ ] Los PID son diferentes cuando el servicio esta conectado.
- [ ] La UI permite conectar, enviar ordenes, desconectar y reiniciar.
- [ ] La conexion no inicia automaticamente al abrir la pantalla.
- [ ] La pantalla permite escribir una orden libre.
- [ ] La pantalla incluye botones rapidos para `Pizza`, `Hamburguesa` y `Ensalada`.
- [ ] `Enviar orden` solo se habilita cuando la orden no esta vacia despues de `trim()`.
- [ ] La cocina acepta varias ordenes y las procesa con cola FIFO.
- [ ] La cocina procesa una orden a la vez.
- [ ] La UI muestra orden actual, ultima respuesta, ordenes en cola y total procesadas.
- [ ] La respuesta usa el formato `Orden lista: <orden>`.
- [ ] La demora de preparacion es fija de `1500 ms` por orden.
- [ ] `Desconectar` no pide confirmacion y limpia orden actual, cola, servicio, conexion y metricas.
- [ ] `Reiniciar` pide confirmacion si hay servicio activo o trabajo pendiente.
- [ ] Confirmar reinicio cancela orden actual, vacia cola, limpia recursos y vuelve a conectar.
- [ ] Salir con conexion activa o trabajo pendiente pide confirmacion.
- [ ] Confirmar salida limpia recursos antes de navegar.
- [ ] Cancelar salida mantiene la pantalla y el procesamiento.
- [ ] Boton volver, back del sistema y gesto atras usan la misma logica.
- [ ] `DisposableEffect` funciona como limpieza defensiva.
- [ ] Los errores se muestran inline y no cierran la app.
- [ ] Los errores relevantes se registran en Logcat.
- [ ] Los eventos relevantes aparecen en Logcat con tag `OSPlayground/BinderIPC`.
- [ ] No se implementa panel visual de eventos en este plan.
- [ ] Todos los textos visibles nuevos estan en `strings.xml`.
- [ ] `App.kt` reemplaza la ruta temporal `restauranteIpc` por `PantallaRestauranteIpc`.
- [ ] La seccion `Como verificar` usa comandos compatibles con Windows/PowerShell.
- [ ] La seccion `Como verificar` explica que valida cada comando.
- [ ] Existen previews sin runtime real para estados representativos.

## Como verificar

No ejecutar build automaticamente.

Si el agente necesita verificar compilacion, debe pedir confirmacion al usuario antes de ejecutar:

```powershell
.\gradlew.bat build
```

Comandos para mostrar en la UI:

```powershell
adb shell ps -A | findstr architecturasos
```

Valida que existan el proceso principal y, al conectar la cocina, el proceso secundario `:cocina_restaurante`.

```powershell
adb shell dumpsys activity services io.yerdna.architecturasos | findstr cocina_restaurante
```

Valida si `ServicioRestauranteIpc` esta registrado como servicio activo. Despues de desconectar o salir, no deberia aparecer como activo.

```powershell
adb logcat -d -s OSPlayground/BinderIPC
```

Muestra eventos de conexion, envio de orden, cola, procesamiento, respuesta, desconexion, reinicio, limpieza y errores.

Notas:

- No usar `grep` en comandos visibles de verificacion.
- Usar `findstr` para filtros simples en Windows/PowerShell.
- `ps -A` puede seguir mostrando `:cocina_restaurante` si Android conserva el proceso cacheado.
- Para validar si el servicio sigue activo, usar `dumpsys activity services`.

Validacion manual:

- Abrir Restaurante IPC desde el panel principal.
- Confirmar que inicia desconectado.
- Tocar `Conectar` y confirmar que aparece PID de cocina diferente al PID del mesero.
- Enviar una orden y confirmar que pasa por estados de envio, procesamiento y respuesta.
- Enviar varias ordenes seguidas y confirmar que la cocina usa cola FIFO.
- Confirmar que la UI muestra ordenes en cola y total procesadas.
- Tocar `Desconectar` y confirmar que se limpia la pantalla sin pedir confirmacion.
- Conectar de nuevo, enviar ordenes y tocar `Reiniciar`; confirmar el dialogo y validar que la cocina se reconecta limpia.
- Conectar de nuevo, enviar ordenes e intentar salir; cancelar la confirmacion y confirmar que la pantalla sigue procesando.
- Intentar salir de nuevo, confirmar salida y verificar que el servicio se limpia antes de volver al dashboard.
- Revisar Logcat con `adb logcat -d -s OSPlayground/BinderIPC`.

## Estado de aprobacion

- [x] Aprobado explicitamente por el usuario.
- [x] Implementado dentro de `android/`.
- [ ] Build pendiente de confirmacion por el usuario.
