# Plan 002 - Fabrica de Robots / Procesos

## Objetivo

Implementar el modulo Fabrica de Robots para demostrar dos procesos Android reales mediante un `Service` ejecutado en un proceso secundario.

Este plan fue aprobado explicitamente por el usuario y se implemento dentro de `android/`.

## Depende de

- `001-foundation-dashboard-common.md`
- Reglas de `android/AGENTS.md`
- Requerimientos del modulo en `docs/app-requirements.md`

## Archivos a crear

- `android/app/src/main/java/io/yerdna/architecturasos/procesos/ContratoFabricaRobots.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/procesos/EstadoFabricaRobots.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/procesos/ControladorFabricaRobots.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/procesos/FabricaRobotsViewModel.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/procesos/ServicioFabricaRobots.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaFabricaRobots.kt`

## Archivos a modificar

- `android/app/src/main/AndroidManifest.xml`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/App.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaPanelExperimentos.kt`
- `android/app/src/main/res/values/strings.xml`

## Implementacion tecnica

Usar un `Service` en proceso separado:

```xml
<service
    android:name=".procesos.ServicioFabricaRobots"
    android:exported="false"
    android:process=":fabrica_robots" />
```

El PID principal se obtiene con `android.os.Process.myPid()` desde la Activity. El PID secundario debe obtenerse dentro de `ServicioFabricaRobots` con `android.os.Process.myPid()`.

El PID secundario y los cambios de estado deben reportarse a la UI usando `Messenger`.

No usar:

- `LocalBinder`
- archivos temporales
- broadcasts

El uso de `Messenger` en este modulo debe mantenerse minimo. Solo debe cubrir PID secundario, estado actual, contador de robots ensamblados, cantidad de mensajes intercambiados y errores. El modulo Restaurante IPC profundizara el concepto de IPC en el plan 003.

## Contrato Messenger

Crear `ContratoFabricaRobots` para centralizar los mensajes y claves usados por `Messenger`.

Mensajes minimos:

- `MSG_REGISTRAR_CLIENTE`
- `MSG_INICIAR_FABRICA`
- `MSG_DETENER_FABRICA`
- `MSG_PID_FABRICA`
- `MSG_ESTADO_FABRICA`
- `MSG_ROBOT_ENSAMBLADO`
- `MSG_ERROR_FABRICA`

Claves minimas de `Bundle`:

- `KEY_PID`
- `KEY_ESTADO`
- `KEY_ROBOTS_CONFIGURADOS`
- `KEY_ROBOTS_ENSAMBLADOS`
- `KEY_MENSAJES_INTERCAMBIADOS`
- `KEY_MENSAJE_ERROR`

## Responsabilidades

### Oficina principal

La oficina principal representa el proceso principal de la app y vive en `PantallaFabricaRobots`.

Responsabilidades:

- Mostrar la UI del experimento.
- Obtener y mostrar el PID principal.
- Iniciar la fabrica secundaria cuando el usuario toca `Iniciar`.
- Detener la fabrica secundaria cuando el usuario toca `Detener`.
- Manejar intentos de salida de la pantalla.
- Recibir el PID secundario y cambios de estado reportados por `ServicioFabricaRobots`.
- Actualizar metricas simples: hora de inicio, hora de fin, cantidad de robots, mensajes intercambiados y estado actual.
- Registrar eventos relevantes en Logcat con tag `OSPlayground/FabricaRobots`.

La oficina principal no debe simular el PID secundario ni inventar estados de la fabrica. Solo debe mostrar datos reportados por el `Service` o derivados de acciones reales de inicio/detencion.

### Fabrica secundaria

La fabrica secundaria representa el proceso separado `:fabrica_robots` y vive dentro de `ServicioFabricaRobots`.

Responsabilidades:

- Ejecutarse en un proceso Android distinto al proceso principal.
- Obtener su propio PID desde el `Service`.
- Reportar a la oficina principal que fue iniciada y cual es su PID.
- Ensamblar una cantidad finita de robots configurada por el usuario.
- Enviar mensajes periodicos simples a la oficina principal, por ejemplo `Robot ensamblado #1`.
- Detener su tarea al recibir orden de detencion o al completar la cantidad configurada.
- Liberar callbacks, handlers o timers usados para enviar actualizaciones.
- Registrar eventos en Logcat con tag `OSPlayground/FabricaRobots`.

La fabrica secundaria no debe crear mas procesos, hilos ilimitados ni trabajo pesado. Su funcion es demostrar que existe un segundo proceso Android real con un PID diferente y ciclo de vida controlado.

## Controlador y ViewModel

Crear `ControladorFabricaRobots` como clase Android encargada de conectar con `ServicioFabricaRobots`.

`ControladorFabricaRobots` debe manejar:

- `applicationContext`
- `ServiceConnection`
- `Messenger` hacia el `Service`
- `Messenger` de respuesta hacia la pantalla
- `startService`
- `bindService`
- `unbindService`
- `stopService`
- limpieza idempotente de conexion y recursos Android

Crear `FabricaRobotsViewModel` para manejar estado puro y acciones logicas de UI.

`FabricaRobotsViewModel` no debe recibir ni guardar `Context`.

`PantallaFabricaRobots` debe:

- obtener `LocalContext.current.applicationContext`;
- crear `ControladorFabricaRobots` con `remember`;
- coordinar llamadas entre controlador y `FabricaRobotsViewModel`;
- observar `EstadoFabricaRobots`;
- mostrar confirmaciones de salida;
- renderizar controles, metricas, resultado, visualizacion de procesos y seccion `Como verificar`.

El controlador debe reportar cambios al `FabricaRobotsViewModel` mediante callbacks simples.

No poner la logica de `ServiceConnection`, `Messenger`, `bindService` ni `stopService` directamente dentro de composables.

## Estados y resultado

La fabrica secundaria usa estos estados:

- `Inactivo`: la fabrica no esta activa en este momento.
- `Iniciando`: se esta inicializando el `Service` y la conexion `Messenger`.
- `Ejecucion`: la fabrica esta generando robots.
- `Deteniendo`: se estan limpiando recursos y deteniendo la fabrica.
- `Error`: ocurrio un problema iniciando, ejecutando, comunicando o deteniendo la fabrica.

`EstadoFabricaRobots` debe separar claramente estado actual, resultado historico y metricas:

- `estado`: estado actual de la fabrica.
- `resultadoUltimaEjecucion`: resultado historico de la ultima ejecucion.
- `robotsConfigurados`: cantidad objetivo enviada al `Service`.
- `robotsEnsamblados`: cantidad reportada por el `Service`.
- `mensajesIntercambiados`: cantidad de mensajes `Messenger` relevantes enviados o recibidos durante la ejecucion.

`robotsConfigurados`, `robotsEnsamblados` y `mensajesIntercambiados` son metricas, no estados.

Valores permitidos para `resultadoUltimaEjecucion`:

- `Sin ejecucion`: valor inicial al abrir la pantalla por primera vez.
- `Completado`: se ensamblaron todos los robots configurados.
- `Cancelado`: el usuario toco `Detener` o confirmo salir durante una ejecucion.
- `Error`: ocurrio un error durante inicio, ejecucion, comunicacion o detencion.

Este resultado no reemplaza el estado actual de la fabrica. Por ejemplo, despues de completar normalmente, el estado actual vuelve a `Inactivo` y `resultadoUltimaEjecucion` queda como `Completado`.

Cuando ocurra un error, la pantalla debe registrar el error en UI y Logcat, intentar detener el `Service`, liberar recursos, limpiar la conexion `Messenger`, conservar un mensaje corto de error visible y cambiar el estado de la fabrica a `Error`.

Desde `Error`, el usuario puede tocar `Iniciar` para reiniciar el experimento. Antes de reiniciar desde `Error`, la pantalla debe limpiar cualquier dato anterior y pasar por `Iniciando`.

## Configuracion y ejecucion

La pantalla debe permitir configurar la cantidad de robots que la fabrica secundaria debe ensamblar.

- Valor por defecto: `60`.
- Valor minimo: `1`.
- No definir un valor maximo fijo en el plan.
- La UI debe validar que el valor ingresado sea un numero entero valido y mayor o igual a `1`.
- Si el valor ingresado es mayor a `300`, la UI debe mostrar una advertencia con duracion aproximada, sin bloquear la ejecucion.
- El valor configurado debe enviarse a `ServicioFabricaRobots` al iniciar el experimento.
- Una vez iniciado el experimento, el campo de cantidad debe quedar deshabilitado hasta que la fabrica vuelva a `Inactivo` o `Error`.

Mientras la fabrica este en `Ejecucion`, `ServicioFabricaRobots` debe ensamblar 1 robot cada 1 segundo hasta alcanzar la cantidad configurada.

La frecuencia no debe ser configurable en este plan. Con el valor por defecto de `60` robots, la ejecucion dura aproximadamente 60 segundos si no se detiene antes.

Cada robot ensamblado incrementa `robotsEnsamblados`, actualiza `mensajesIntercambiados` y envia un mensaje `Messenger` a la oficina principal.

La ejecucion termina cuando:

- se alcanza la cantidad configurada;
- el usuario toca `Detener`;
- el usuario confirma salir;
- ocurre un error.

La tarea periodica debe usar un mecanismo cancelable, por ejemplo `Handler` con `Runnable` removible. No usar loops infinitos ni trabajo pesado.

Cuando se alcanza la cantidad configurada, el `Service` debe detenerse, liberar recursos y la fabrica debe volver a `Inactivo`. La pantalla debe conservar el resultado final visible: total de robots ensamblados, hora de inicio, hora de fin y evento de finalizacion.

Al tocar `Iniciar` desde `Inactivo` o `Error`, la pantalla debe limpiar los datos de la ejecucion anterior antes de pasar a `Iniciando`.

Limpiar:

- PID secundario anterior.
- Contador de robots ensamblados.
- Hora de inicio anterior.
- Hora de fin anterior.
- Mensaje de error anterior.

Mantener:

- Cantidad configurada actual.
- PID principal.

`resultadoUltimaEjecucion` debe cambiar a `Sin ejecucion` al iniciar una nueva ejecucion.

## Controles

Controles por estado:

- `Inactivo`: `Iniciar` habilitado y `Detener` deshabilitado.
- `Iniciando`: `Iniciar` deshabilitado y `Detener` deshabilitado.
- `Ejecucion`: `Iniciar` deshabilitado y `Detener` habilitado.
- `Deteniendo`: `Iniciar` deshabilitado y `Detener` deshabilitado.
- `Error`: `Iniciar` habilitado para reiniciar y `Detener` deshabilitado.

Evitar doble inicio o doble detencion deshabilitando botones segun el estado actual.

## Ciclo de vida y salida

El experimento no debe continuar ejecutandose despues de salir de `PantallaFabricaRobots`.

La confirmacion de salida debe mostrarse solo cuando la fabrica este en `Iniciando` o `Ejecucion`.

Si la fabrica esta en `Deteniendo`, la pantalla debe bloquear nuevas acciones y esperar a que termine la limpieza.

Si la fabrica esta en `Inactivo` o `Error`, salir no requiere confirmacion.

La confirmacion es obligatoria para los flujos normales de salida controlados por la pantalla:

- boton volver de la barra superior;
- boton atras del sistema;
- gesto atras del sistema.

`PantallaFabricaRobots` debe usar `BackHandler` para interceptar el boton atras y gesto atras del sistema.

El `BackHandler` debe:

- mostrar confirmacion si el estado es `Iniciando` o `Ejecucion`;
- ignorar la salida y mantener la pantalla si el estado es `Deteniendo`;
- ejecutar `onVolver` si el estado es `Inactivo` o `Error`.

La confirmacion debe implementarse con `AlertDialog` de Material 3.

Textos del dialogo:

- Titulo: `Cancelar experimento`.
- Mensaje: `La fabrica esta ejecutandose. Si sales ahora, se detendra el experimento y se liberaran los recursos.`
- Accion confirmacion: `Salir y detener`.
- Accion cancelar: `Continuar`.

Al confirmar, la pantalla debe cambiar estado a `Deteniendo`, ejecutar limpieza y navegar al terminar la limpieza.

Al cancelar, debe cerrar el dialogo y mantener el experimento ejecutandose.

`PantallaFabricaRobots` debe usar `DisposableEffect` para llamar `controlador.liberar()` cuando la pantalla salga de composicion. Esta limpieza funciona como respaldo si la composicion se destruye por cualquier motivo.

`DisposableEffect` no debe usarse para saltarse la confirmacion. Solo funciona como limpieza defensiva si la composicion se destruye por un motivo externo o inesperado.

`controlador.liberar()` debe ser idempotente y debe:

- desregistrar el cliente `Messenger` si aplica;
- cancelar mensajes pendientes;
- desenlazar el `Service` si esta enlazado;
- detener el `Service` si la fabrica esta en `Iniciando` o `Ejecucion`;
- limpiar referencias a `Messenger` y callbacks.

## UI requerida

La pantalla debe representar una oficina principal que enciende una fabrica secundaria:

- Oficina principal: proceso principal de la app.
- Fabrica secundaria: proceso secundario donde corre `ServicioFabricaRobots`.

Cada proceso debe mostrarse como una entidad visual separada, con nombre logico, PID y estado.

La oficina principal siempre debe mostrar estado `Activo` mientras la pantalla exista.

La fabrica secundaria debe mostrar su estado actual.

La visualizacion debe:

- mostrar dos bloques separados: `Oficina principal` y `Fabrica secundaria`;
- mostrar nombre logico, PID y estado en cada bloque;
- dibujar una conexion visual simple entre oficina y fabrica cuando el `Service` este iniciando o ejecutandose;
- mostrar la fabrica como apagada o deshabilitada cuando el estado sea `Inactivo`;
- resaltar visualmente que los PIDs son diferentes.

La visualizacion ayuda a explicar el concepto durante la exposicion, pero no sustituye la verificacion real con PID, Logcat y ADB.

La pantalla debe mostrar un bloque `Resultado` con:

- resultado de ultima ejecucion;
- robots configurados;
- robots ensamblados;
- mensajes intercambiados;
- hora de inicio;
- hora de fin o duracion;
- mensaje de error, solo si existe.

Antes de la primera ejecucion, el bloque `Resultado` debe mostrar `Sin ejecucion`.

La pantalla debe incluir una seccion visible `Como verificar`.

La seccion `Como verificar` debe mostrar comandos en texto monoespaciado:

- `adb shell ps -A | findstr architecturasos`
- `adb shell dumpsys activity services io.yerdna.architecturasos | findstr fabrica_robots`
- `adb logcat -d -s OSPlayground/FabricaRobots`

Cada comando de `Como verificar` debe tener un boton simple `Copiar` usando el clipboard de Android sin agregar dependencias nuevas.

## Registro de eventos

En este plan no se implementa todavia el panel visual de Registro de eventos. Ese componente se integrara en un plan posterior.

Este plan solo debe registrar eventos relevantes en Logcat con tag `OSPlayground/FabricaRobots`.

Eventos minimos en Logcat:

- inicio;
- PID detectado;
- proceso ejecutandose;
- robot ensamblado;
- detencion;
- finalizacion;
- error;
- limpieza de recursos.

Si al momento de implementar este plan ya existe una utilidad comun de eventos, puede usarse. Si no existe, no crearla dentro del plan 002.

## Localizacion y recursos

Mantener textos visibles nuevos en `strings.xml`, usando espanol como idioma por defecto.

Agregar recursos de texto para estados, acciones, nombres logicos, metricas, resultado, dialogo de confirmacion y comandos.

Ejemplos de recursos esperados:

- `estado_inactivo`
- `estado_activo`
- `estado_iniciando`
- `estado_ejecucion`
- `estado_deteniendo`
- `estado_error`
- `accion_iniciar`
- `accion_detener`
- `accion_copiar`
- `accion_salir_y_detener`
- `accion_continuar`
- `dialogo_cancelar_experimento_titulo`
- `dialogo_cancelar_experimento_mensaje`
- `proceso_oficina_principal`
- `proceso_fabrica_secundaria`
- `metrica_robots_configurados`
- `metrica_robots_ensamblados`
- `metrica_mensajes_intercambiados`
- `advertencia_duracion_larga`
- `resultado_ultima_ejecucion`
- `resultado_sin_ejecucion`
- `resultado_completado`
- `resultado_cancelado`
- `resultado_error`
- `seccion_como_verificar`

## Integracion con navegacion

- Reemplazar la ruta temporal `fabricaRobots` en `App.kt` por `PantallaFabricaRobots`.
- Eliminar cualquier recurso, parametro, texto o dato de soporte que haya quedado asociado solo a la pantalla temporal de `fabricaRobots` y que ya no se use despues de conectar `PantallaFabricaRobots`.
- Mantener el dashboard sin estado por experimento, segun la decision del plan 001 y el apendice A de `docs/app-requirements.md`.

## Previews

Agregar previews simples para `PantallaFabricaRobots` en estos estados:

- `Inactivo`
- `Ejecucion`
- `Error`

Las previews deben usar datos de ejemplo locales y no iniciar `Service`, `Messenger` ni `ControladorFabricaRobots` real.

## Pasos

- [ ] Crear `ContratoFabricaRobots` con constantes para mensajes `Messenger` y claves de `Bundle`.
- [ ] Crear `EstadoFabricaRobots` con PID principal, PID secundario, estado, resultado de ultima ejecucion, robots configurados, robots ensamblados, mensajes intercambiados, hora de inicio, hora de fin o duracion y mensaje de error opcional.
- [ ] Crear `ControladorFabricaRobots` para manejar `Context`, conexion con `ServicioFabricaRobots`, `ServiceConnection`, `Messenger`, `startService`, `bindService`, `unbindService` y `stopService`.
- [ ] Crear `FabricaRobotsViewModel` sin `Context`, enfocado en estado puro y acciones logicas.
- [ ] Crear `ServicioFabricaRobots`.
- [ ] Configurar el `Service` en `AndroidManifest.xml` con `android:process=":fabrica_robots"`.
- [ ] Al iniciar el `Service`, registrar PID secundario en Logcat con tag `OSPlayground/FabricaRobots`.
- [ ] Implementar ensamblado de 1 robot por segundo hasta alcanzar la cantidad configurada.
- [ ] Enviar por `Messenger` PID secundario, estado actual, contador de robots ensamblados, mensajes intercambiados y errores.
- [ ] Crear `PantallaFabricaRobots`.
- [ ] En `PantallaFabricaRobots`, obtener `LocalContext.current.applicationContext` y crear `ControladorFabricaRobots` con `remember`.
- [ ] Usar `DisposableEffect` en `PantallaFabricaRobots` para llamar `controlador.liberar()` al salir de composicion.
- [ ] Implementar `controlador.liberar()` como limpieza idempotente.
- [ ] Usar `BackHandler` para interceptar boton atras y gesto atras del sistema.
- [ ] Usar `AlertDialog` de Material 3 para confirmar salida durante `Iniciando` o `Ejecucion`.
- [ ] Mantener `PantallaFabricaRobots` principalmente como UI y coordinacion ligera entre controlador y `FabricaRobotsViewModel`.
- [ ] Permitir configurar cantidad de robots a ensamblar con valor por defecto `60` y minimo `1`.
- [ ] Validar que la cantidad de robots ingresada sea un entero mayor o igual a `1`.
- [ ] Mostrar advertencia de duracion aproximada cuando la cantidad ingresada sea mayor a `300`, sin bloquear la ejecucion.
- [ ] Deshabilitar el input de cantidad mientras la fabrica este en `Iniciando`, `Ejecucion` o `Deteniendo`.
- [ ] Aplicar controles por estado.
- [ ] Mostrar PID principal y PID secundario cuando este disponible.
- [ ] Mostrar representacion grafica de oficina principal y fabrica secundaria.
- [ ] Mostrar contador de robots ensamblados y mensajes intercambiados.
- [ ] Mostrar bloque `Resultado`.
- [ ] Mostrar seccion `Como verificar` con comandos copiables.
- [ ] Registrar eventos relevantes en Logcat.
- [ ] No implementar todavia el panel visual de Registro de eventos.
- [ ] Manejar errores intentando detener el `Service`, limpiar conexion `Messenger`, conservar mensaje visible de error y cambiar estado a `Error`.
- [ ] Al alcanzar la cantidad configurada, detener el `Service`, liberar recursos, volver a `Inactivo` y conservar el resultado final visible.
- [ ] Al iniciar una nueva ejecucion, limpiar PID secundario anterior, contador, hora de inicio, hora de fin y mensaje de error.
- [ ] Al iniciar una nueva ejecucion, mantener cantidad configurada actual y PID principal.
- [ ] Permitir repetir el experimento sin reiniciar la app.
- [ ] Reemplazar la ruta temporal `fabricaRobots` en `App.kt` por `PantallaFabricaRobots`.
- [ ] Eliminar recursos temporales no usados asociados solo a la pantalla temporal de `fabricaRobots`.
- [ ] Mantener textos visibles nuevos en `strings.xml`, usando espanol como idioma por defecto.
- [ ] Agregar previews de `PantallaFabricaRobots` para `Inactivo`, `Ejecucion` y `Error`.

## Criterios de aceptacion

- [ ] El PID principal se muestra siempre.
- [ ] Al iniciar el experimento aparece un PID secundario diferente.
- [ ] El proceso secundario aparece en `adb shell ps -A`.
- [ ] La pantalla muestra graficamente la oficina principal y la fabrica secundaria como procesos separados.
- [ ] La oficina principal muestra estado `Activo`.
- [ ] La fabrica secundaria muestra `Inactivo`, `Iniciando`, `Ejecucion`, `Deteniendo` o `Error`.
- [ ] La fabrica secundaria reporta mensajes reales al proceso principal mientras esta ejecutandose.
- [ ] El contador de robots ensamblados aumenta solo cuando el `Service` secundario esta activo.
- [ ] La cantidad de robots es configurable, inicia por defecto en `60` y rechaza valores menores a `1`.
- [ ] La UI muestra advertencia de duracion aproximada para valores mayores a `300` sin imponer maximo fijo.
- [ ] La ejecucion termina al alcanzar la cantidad configurada, al detener manualmente, al confirmar salida o ante error.
- [ ] Al completar la cantidad configurada, la pantalla vuelve a `Inactivo` y conserva total ensamblado, hora de inicio, hora de fin y evento de finalizacion.
- [ ] El resultado de ultima ejecucion distingue `Sin ejecucion`, `Completado`, `Cancelado` y `Error` sin agregar estados extra a la fabrica.
- [ ] Las metricas `robotsConfigurados`, `robotsEnsamblados` y `mensajesIntercambiados` se muestran como datos de ejecucion, no como estados.
- [ ] El bloque `Resultado` muestra `Sin ejecucion` antes de la primera ejecucion.
- [ ] Cada nueva ejecucion inicia con metricas limpias.
- [ ] `Detener` detiene el `Service`.
- [ ] Intentar salir de `PantallaFabricaRobots` durante `Iniciando` o `Ejecucion` muestra confirmacion antes de detener el `Service` secundario.
- [ ] Cancelar la confirmacion mantiene al usuario en la pantalla y el experimento sigue ejecutandose.
- [ ] Aceptar la confirmacion detiene el `Service` secundario y permite volver al panel.
- [ ] Un error no deja el `Service` secundario ejecutandose sin control.
- [ ] Desde `Error`, `Iniciar` limpia datos anteriores y permite repetir el experimento.
- [ ] Al volver a abrir `PantallaFabricaRobots` despues de salir, la pantalla muestra estado `Inactivo`.
- [ ] Despues de salir de la pantalla, `adb shell dumpsys activity services io.yerdna.architecturasos | findstr fabrica_robots` no muestra el servicio de Fabrica de Robots como activo.
- [ ] `adb shell ps -A | findstr architecturasos` puede seguir mostrando el proceso `:fabrica_robots` si Android lo conserva cacheado, pero no debe haber actividad de ensamblado ni servicio activo.
- [ ] La pantalla puede reiniciarse varias veces.
- [ ] Los eventos relevantes aparecen en Logcat.
- [ ] La pantalla incluye seccion `Como verificar` con comandos visibles y copiables.
- [ ] No se implemento el panel visual de Registro de eventos dentro del plan 002.
- [ ] Existen previews de `PantallaFabricaRobots` para `Inactivo`, `Ejecucion` y `Error` sin dependencias de runtime Android.
- [ ] El dashboard no muestra estado por experimento.

## Como verificar

No ejecutar build automaticamente.

Si el agente necesita verificar compilacion, debe pedir confirmacion al usuario antes de ejecutar desde `android/`:

```powershell
.\gradlew.bat build
```

Con la app abierta:

```bash
adb shell ps -A | findstr architecturasos
adb shell dumpsys activity services io.yerdna.architecturasos | findstr fabrica_robots
adb logcat -d -s OSPlayground/FabricaRobots
```

Validar manualmente:

- Abrir Fabrica de Robots desde el panel principal.
- Confirmar que el estado inicial de la fabrica es `Inactivo`.
- Confirmar que el input de robots inicia con `60`.
- Iniciar el experimento y confirmar que aparece un PID secundario diferente al PID principal.
- Ejecutar `adb shell ps -A | findstr architecturasos` y confirmar que aparece el proceso `:fabrica_robots`.
- Confirmar que el contador aumenta aproximadamente 1 robot por segundo.
- Ingresar un valor mayor a `300` y confirmar que aparece advertencia de duracion aproximada sin bloquear la ejecucion.
- Tocar `Detener` y confirmar que la UI vuelve a `Inactivo` con resultado `Cancelado`.
- Iniciar de nuevo el experimento, tocar el boton de volver de la barra superior, cancelar la confirmacion y confirmar que el experimento sigue ejecutandose.
- Tocar de nuevo el boton de volver de la barra superior, aceptar la confirmacion, volver a abrir Fabrica de Robots y confirmar que la pantalla inicia en `Inactivo`.
- Iniciar de nuevo el experimento, usar el boton atras o gesto atras del sistema, cancelar la confirmacion y confirmar que el experimento sigue ejecutandose.
- Usar de nuevo el boton atras o gesto atras del sistema, aceptar la confirmacion, volver a abrir Fabrica de Robots y confirmar que la pantalla inicia en `Inactivo`.
- Ejecutar con una cantidad pequena, por ejemplo `3`, y confirmar que al completarse vuelve a `Inactivo` con resultado `Completado`.
- Volver a ejecutar `adb shell dumpsys activity services io.yerdna.architecturasos | findstr fabrica_robots` despues de detener, salir o completar, y confirmar que el servicio de Fabrica de Robots ya no queda activo.
- Si `adb shell ps -A | findstr architecturasos` todavia muestra `:fabrica_robots`, interpretarlo como proceso cacheado por Android mientras no haya servicio activo ni nuevos logs de ensamblado.
- Revisar Logcat con `adb logcat -d -s OSPlayground/FabricaRobots` y confirmar eventos de inicio, PID detectado, robot ensamblado, detencion, finalizacion y limpieza.

## Estado de aprobacion

- [x] Aprobado explicitamente por el usuario.
- [x] Implementado dentro de `android/`.
- [ ] Build pendiente de confirmacion por el usuario.
