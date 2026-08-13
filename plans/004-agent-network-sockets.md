# Plan 004 - Red de Agentes Sockets

## Objetivo

Implementar el modulo **Red de Agentes** para demostrar comunicacion cliente-servidor real usando sockets TCP locales dentro de Android.

La simulacion visual ayuda a explicar el concepto, pero no reemplaza el mecanismo tecnico real: el cliente debe enviar bytes por `Socket` y el servidor debe recibirlos mediante `ServerSocket`.

## Depende de

- `001-foundation-dashboard-common.md`
- Componentes comunes ya existentes:
  - `android/app/src/main/java/io/yerdna/architecturasos/ui/component/ExperimentoScaffold.kt`
  - `android/app/src/main/java/io/yerdna/architecturasos/ui/component/RegistroExperimento.kt`
  - `android/app/src/main/java/io/yerdna/architecturasos/ui/component/PanelRegistroEventos.kt`
  - `android/app/src/main/java/io/yerdna/architecturasos/util/ExperimentoLogger.kt`

## Reglas cerradas para este plan

- No agregar dependencias nuevas.
- No ejecutar `.\gradlew.bat build` automaticamente; pedir confirmacion antes.
- Usar nombres propios en espanol para codigo del proyecto.
- Mantener en ingles solo APIs oficiales: `ServerSocket`, `Socket`, `InputStream`, `OutputStream`, `ExecutorService`, `ViewModel`, `BackHandler`, `DisposableEffect`.
- Todo texto visible nuevo debe ir en `android/app/src/main/res/values/strings.xml`.
- Los comandos tecnicos literales de verificacion pueden quedar en codigo; sus descripciones visibles si deben ir en `strings.xml`.
- Los comandos de verificacion visibles deben ser compatibles con Windows/PowerShell.
- La pantalla debe ser duenna del `ExperimentoLogger`; no crear singleton global de registro.
- No mostrar boton manual para limpiar registros.
- Al iniciar una ejecucion nueva del experimento, el registro debe empezar con una lista nueva.
- Registrar eventos importantes en Logcat con tag estable `OSPlayground/SocketLab`.
- No ejecutar sockets bloqueantes en Main/UI Thread.
- No usar red externa ni Internet; el experimento debe funcionar sin conexion.
- No mencionar en `Como verificar` herramientas que no prueban directamente el experimento.
- Agregar `android.permission.INTERNET` en `AndroidManifest.xml`, porque Android lo requiere para abrir sockets TCP incluso cuando se usa `127.0.0.1`.

## Archivos a crear

- `android/app/src/main/java/io/yerdna/architecturasos/redagentes/EstadoRedAgentes.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/redagentes/ServidorRedAgentes.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/redagentes/ClienteRedAgentes.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/redagentes/ControladorRedAgentes.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/redagentes/RedAgentesViewModel.kt`
- `android/app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaRedAgentes.kt`

## Archivos a modificar

- `android/app/src/main/java/io/yerdna/architecturasos/ui/App.kt`
- `android/app/src/main/AndroidManifest.xml`
- `android/app/src/main/res/values/strings.xml`

## Implementacion tecnica

Usar APIs reales:

- `ServerSocket`
- `Socket`
- `InputStream`
- `OutputStream`

Usar `ExecutorService` para mantener el trabajo de sockets fuera del Main Thread:

- Un executor de un solo hilo para el servidor.
- Un executor de un solo hilo para el cliente/envio.
- Cerrar ambos con `shutdownNow()` durante `detener`, error, salida confirmada y `DisposableEffect.onDispose`.

No usar coroutines para sockets en este modulo.

Host:

```text
127.0.0.1
```

Puerto:

- Crear el servidor con `ServerSocket(0)`.
- Leer el puerto real con `serverSocket.localPort`.
- Mostrar ese puerto en la UI.
- Usar ese mismo puerto para el cliente.
- No hardcodear un puerto fijo.

Servidor:

- Mantenerse escuchando mientras este activo.
- Aceptar conexiones secuenciales en un bucle controlado.
- Atender un cliente a la vez.
- Por cada cliente: aceptar conexion, leer mensaje, responder, cerrar socket cliente y volver a escuchar.
- No aceptar clientes concurrentes.
- No crear un hilo por cliente.

Timeouts:

- Cliente: `socket.soTimeout = 3000` ms para esperar respuesta.
- Servidor: `socket.soTimeout = 3000` ms para leer el mensaje del cliente aceptado.
- `ServerSocket.accept()` se desbloquea cerrando el `ServerSocket` en `detener`.
- Ante timeout, registrar error visible, cerrar ese socket y dejar el servidor listo para la siguiente conexion.

## Protocolo TCP local

Usar texto UTF-8 con terminador de mensaje `\u001E` (`0x1E`, ASCII Record Separator).

Constantes esperadas en codigo:

```kotlin
private const val TERMINADOR_MENSAJE = '\u001E'
private const val MAX_CARACTERES_MENSAJE = 240
private const val MAX_BYTES_MENSAJE = 1024
private const val TIMEOUT_SOCKET_MS = 3000
```

Agregar un comentario breve junto a estas constantes explicando que el terminador y los limites existen para mantener el protocolo simple y la demostracion estable.

Reglas:

- La UI limita el mensaje a 240 caracteres visibles.
- El servidor lee como maximo 1024 bytes antes de devolver error y cerrar ese socket cliente.
- Si no llega `\u001E` dentro de 1024 bytes, reportar `MensajeDemasiadoGrande`.
- El campo de texto no debe permitir el caracter `\u001E`.
- La respuesta exacta del servidor es:

```text
Central recibio: <mensaje>
```

- La respuesta tambien termina con `\u001E`.
- No usar JSON.
- No incluir timestamps dentro del payload; las metricas se calculan aparte.
- Contar bytes reales escritos/leidos en el socket, incluyendo el terminador `\u001E`.

## Estados

Definir estados propios del modulo en `EstadoRedAgentes.kt`.

Estados de servidor:

- `Inactivo`
- `Iniciando`
- `Escuchando`
- `ClienteConectado`
- `Deteniendo`
- `Detenido`
- `Error`

Estados de cliente/envio:

- `SinEnvio`
- `Conectando`
- `Enviando`
- `EsperandoRespuesta`
- `Exitoso`
- `Error`
- `Cancelado`

Resultado de ultima ejecucion:

- `SinEjecucion`
- `Exitoso`
- `Cancelado`
- `Error`

Codigos de error:

- `MensajeVacio`
- `ServidorNoDisponible`
- `Timeout`
- `MensajeDemasiadoGrande`
- `ErrorLectura`
- `ErrorEscritura`
- `ErrorCierre`
- `ErrorDesconocido`

La UI debe convertir cada estado y codigo de error a texto desde `strings.xml`. El detalle tecnico de excepciones se registra en Logcat, pero no se muestra crudo al usuario.

## Estado, metricas y resultado

Separar claramente:

- Estado actual del servidor.
- Estado actual del cliente/envio.
- Resultado de la ultima ejecucion.
- Metricas de la conexion actual o ultima conexion.
- Codigo de ultimo error visible, cuando aplique.

Metricas minimas:

- Host.
- Puerto.
- Hora de conexion.
- Mensaje enviado.
- Mensaje recibido por el servidor.
- Respuesta enviada por el servidor.
- Respuesta recibida por el cliente.
- Bytes enviados, incluyendo terminador.
- Bytes recibidos, incluyendo terminador.
- Tiempo aproximado de respuesta en milisegundos.

Las metricas no deben modelarse como estados.

## Responsabilidades

`ServidorRedAgentes.kt`:

- Crear y cerrar `ServerSocket`.
- Usar `ServerSocket(0)` y reportar `localPort`.
- Aceptar conexiones secuenciales mientras el servidor este activo.
- Leer bytes reales desde `InputStream` hasta `TERMINADOR_MENSAJE`.
- Aplicar `MAX_BYTES_MENSAJE`.
- Generar la respuesta exacta `Central recibio: <mensaje>`.
- Escribir la respuesta por `OutputStream` con el mismo terminador.
- Reportar eventos, bytes, mensaje recibido y respuesta mediante callbacks hacia `ControladorRedAgentes`.
- Manejar excepciones sin cerrar la app.
- Liberar `ServerSocket` y sockets cliente en `detener` y en bloques `finally`.

`ClienteRedAgentes.kt`:

- Abrir `Socket` hacia `127.0.0.1` y el puerto reportado por el servidor.
- Aplicar `socket.soTimeout = 3000`.
- Enviar el mensaje del usuario en UTF-8 con `TERMINADOR_MENSAJE`.
- Leer la respuesta real hasta `TERMINADOR_MENSAJE`.
- Medir tiempo aproximado de respuesta.
- Reportar bytes enviados, bytes recibidos, respuesta y errores.
- Cerrar su socket al finalizar, cancelar o fallar.

`ControladorRedAgentes.kt`:

- Poseer `ServidorRedAgentes`, `ClienteRedAgentes` y los dos `ExecutorService`.
- Exponer acciones:
  - `iniciarServidor()`
  - `enviarMensaje(mensaje: String)`
  - `detener()`
  - `limpiar()`
- Traducir callbacks tecnicos a eventos simples para el ViewModel y el logger.
- Garantizar que `detener`, `limpiar` y salida confirmada cierren sockets y executors.
- No depender de `Context`.

`RedAgentesViewModel.kt`:

- Mantener estado observable de la pantalla.
- Validar que el mensaje no este vacio.
- Validar limite de 240 caracteres visibles.
- Bloquear doble inicio del servidor.
- Bloquear doble envio mientras existe un envio activo.
- Coordinar cambios de estado recibidos desde el controlador.
- Mantener metricas y resultado de ultima ejecucion.
- No poseer sockets, executors ni `Context`.
- No escribir textos visibles hardcodeados.

`PantallaRedAgentes.kt`:

- Mostrar controles, visualizacion, metricas, resultado, registro de eventos y seccion `Como verificar`.
- Crear y poseer el `ExperimentoLogger` de la pantalla.
- Crear `ControladorRedAgentes` con `remember`.
- Conectar callbacks del controlador al ViewModel y al logger.
- Limpiar el registro al tocar `Iniciar servidor`.
- No limpiar el registro por cada mensaje enviado mientras el servidor siga activo.
- Usar `BackHandler` cuando salir implique detener servidor o cancelar envio activo.
- Usar `AlertDialog` de Material 3 para confirmar salida si hay servidor escuchando, cliente conectado, envio activo o recursos pendientes de cerrar.
- Usar `DisposableEffect` para limpieza defensiva al abandonar composicion.
- No iniciar sockets ni threads reales desde previews.

`App.kt`:

- Reemplazar la pantalla temporal de `Navegacion.Ruta.RedAgentes` por `PantallaRedAgentes`.

`AndroidManifest.xml`:

- Agregar `<uses-permission android:name="android.permission.INTERNET" />` como hijo directo de `<manifest>`.
- No agregar permisos de red adicionales.

`strings.xml`:

- Agregar todos los textos visibles del modulo: titulos, estados, acciones, etiquetas de metricas, errores, dialogos y descripciones de verificacion.

## Controles por estado

- `Iniciar servidor`: habilitado solo cuando el servidor esta `Inactivo`, `Detenido` o `Error`.
- `Enviar mensaje`: habilitado solo cuando el servidor esta `Escuchando`, no hay envio activo y el mensaje es valido.
- Campo de mensaje: editable solo cuando no hay envio activo.
- `Detener servidor`: habilitado cuando el servidor esta `Iniciando`, `Escuchando` o `ClienteConectado`.
- Salir de la pantalla con recursos activos: debe pedir confirmacion antes de navegar.

Transicion despues de envio exitoso:

- `estadoServidor = Escuchando`
- `estadoEnvio = Exitoso`
- `resultadoUltimaEjecucion = Exitoso`
- Mantener visibles las metricas del ultimo mensaje.
- Volver a habilitar `Enviar mensaje`.
- Editar un nuevo mensaje no borra el resultado anterior; se reemplaza al enviar otra vez.

Transicion al detener:

- `estadoServidor = Detenido`
- Si no habia envio activo, conservar el resultado del ultimo envio.
- Si habia envio activo, `estadoEnvio = Cancelado` y `resultadoUltimaEjecucion = Cancelado`.

## Ciclo de vida y limpieza

- Al iniciar servidor, limpiar registro y metricas de ejecucion anterior.
- Al detener servidor, cerrar `ServerSocket`, sockets cliente aceptados y tareas en segundo plano.
- Al finalizar un envio exitoso, cerrar el socket cliente.
- En estado `Error`, intentar cerrar sockets y dejar el modulo listo para iniciar una nueva ejecucion.
- Al confirmar salida, detener/cancelar todo antes de navegar al dashboard.
- En `DisposableEffect.onDispose`, ejecutar limpieza defensiva por si la pantalla sale por ciclo de vida.
- Ningun servidor, cliente, socket o executor del modulo debe quedar activo despues de salir de la pantalla.

## Registro de eventos

Usar el logger comun de la pantalla y tag:

```text
OSPlayground/SocketLab
```

Registrar como minimo:

- Inicio de servidor.
- Host y puerto usados.
- Servidor escuchando.
- Cliente conectando.
- Cliente conectado.
- Mensaje enviado.
- Mensaje recibido por servidor.
- Respuesta enviada.
- Respuesta recibida.
- Bytes enviados y recibidos.
- Tiempo aproximado de respuesta.
- Stop/cierre de sockets.
- Reset.
- Errores de servidor o cliente.

No mostrar boton manual de limpiar registro. El registro empieza con lista nueva al iniciar una ejecucion nueva del experimento.

## Visualizacion

Implementar una visualizacion simple con Material 3 y sin dependencias nuevas:

- Bloque visible **Agente**.
- Bloque visible **Central**.
- Indicador de recorrido entre ambos con estados:
  - `Sin mensaje`
  - `Conectando`
  - `Enviando`
  - `Procesando`
  - `Respuesta recibida`
  - `Error`
- Mostrar mensaje y respuesta abreviados en tarjetas o filas de datos.
- Usar cambios de color/estado e indicadores de progreso de Material 3 cuando haya envio activo.
- No crear animacion compleja.
- La visualizacion debe reflejar estados reales reportados por cliente/servidor, no temporizadores ficticios.

## Seccion Como verificar

La pantalla debe mostrar comandos con descripcion breve de que valida, cuando ejecutarlo y como interpretar el resultado.

Comandos compatibles con Windows/PowerShell:

```powershell
adb logcat -d -s OSPlayground/SocketLab
```

Valida directamente eventos del modulo en Logcat: inicio de servidor, puerto usado, conexion, envio, respuesta, cierre y errores.

```powershell
adb shell "printf 'prueba\036' | toybox nc 127.0.0.1 PUERTO"
```

Valida el servidor abriendo una conexion real desde Android. Reemplazar `PUERTO` por el puerto mostrado por la UI. Si el dispositivo incluye `toybox nc`, debe responder `Central recibio: prueba`. Si `toybox nc` no existe, el comando falla por herramienta ausente, no necesariamente por error del servidor.

No usar `grep` en comandos visibles de verificacion. Si hace falta filtrar desde PowerShell, usar `findstr`.

## Verificacion para agentes

No ejecutar build automaticamente.

Si el usuario aprueba correr build, ejecutar desde `android/`:

```powershell
.\gradlew.bat build
```

Revision manual esperada antes de pedir build:

- Confirmar que no se agregaron dependencias nuevas.
- Confirmar que `AndroidManifest.xml` incluye `android.permission.INTERNET`.
- Confirmar que los textos visibles nuevos estan en `strings.xml`.
- Confirmar que no quedan sockets bloqueantes en Main Thread.
- Confirmar que `App.kt` abre `PantallaRedAgentes` en `Navegacion.Ruta.RedAgentes`.
- Confirmar que no existe boton manual para limpiar registro.
- Confirmar que `Como verificar` solo menciona comandos que prueban directamente eventos o sockets del modulo.

## Criterios de aceptacion

- [ ] La pantalla `PantallaRedAgentes` reemplaza la pantalla temporal del modulo.
- [ ] El servidor TCP local inicia con `ServerSocket(0)` y queda escuchando fuera del Main Thread.
- [ ] La UI muestra host `127.0.0.1` y el puerto real obtenido con `localPort`.
- [ ] El cliente envia bytes reales por `Socket`.
- [ ] El servidor recibe el mensaje por `InputStream`.
- [ ] El protocolo usa UTF-8 con terminador `\u001E`.
- [ ] La UI limita mensajes a 240 caracteres visibles.
- [ ] El servidor limita lectura a 1024 bytes.
- [ ] Cliente y servidor usan timeout de 3000 ms para lecturas.
- [ ] El servidor responde exactamente `Central recibio: <mensaje>` con terminador `\u001E`.
- [ ] El cliente muestra la respuesta recibida.
- [ ] Bytes enviados y recibidos incluyen el terminador del protocolo.
- [ ] La UI muestra mensaje enviado, mensaje recibido, bytes enviados, bytes recibidos y tiempo aproximado de respuesta.
- [ ] El servidor acepta conexiones secuenciales y permite varios envios sin detener.
- [ ] No se aceptan clientes concurrentes ni se crea un hilo por cliente.
- [ ] Los controles se habilitan/deshabilitan segun el estado.
- [ ] Los errores se representan con `CodigoErrorRedAgentes` y textos de `strings.xml`.
- [ ] Los detalles tecnicos de excepciones se registran en Logcat sin mostrarse crudos al usuario.
- [ ] Stop cierra servidor, cliente, sockets aceptados y executors.
- [ ] Detener y volver a iniciar deja el experimento listo para repetirse sin reiniciar la app.
- [ ] Salir con recursos activos muestra confirmacion y limpia antes de navegar.
- [ ] `DisposableEffect` cubre limpieza defensiva.
- [ ] El registro de eventos no tiene boton manual de limpiar.
- [ ] Al iniciar una ejecucion nueva, el registro empieza con lista nueva.
- [ ] Los eventos importantes aparecen en el panel interno y en Logcat con tag `OSPlayground/SocketLab`.
- [ ] La visualizacion Agente -> Central refleja estados reales, no temporizadores ficticios.
- [ ] La seccion `Como verificar` usa comandos compatibles con Windows y explica que valida cada uno.
- [ ] La seccion `Como verificar` no menciona herramientas que no prueban directamente el experimento.
- [ ] No se agregan dependencias nuevas.
- [ ] `AndroidManifest.xml` incluye `android.permission.INTERNET` para permitir sockets TCP locales.
- [ ] No quedan textos visibles hardcodeados en la pantalla.
