# Lessons para futuros agentes

Estas notas resumen decisiones aprendidas al revisar e implementar los planes `001-foundation-dashboard-common.md` y `002-robot-factory-processes.md`. Sirven como material para mejorar `android/AGENTS.md`.

## 1. No implementar sin aprobacion explicita

- Los planes son contratos de implementacion.
- No implementar un plan solo porque existe.
- Primero revisar, corregir ambiguedades y esperar aprobacion explicita del usuario.
- Implementar solo cuando el usuario diga claramente que el plan esta aprobado y que quiere implementarlo.

## 2. Trabajar plan por plan

- Si el usuario pide enfocarse en un plan especifico, no modificar planes posteriores.
- Los planes `003` en adelante pueden depender de decisiones actuales, pero no deben editarse hasta que el usuario lo pida.
- Evitar propagar cambios globales sin confirmacion.

## 3. El dashboard no muestra estado de ejecucion

- Aunque los requerimientos iniciales mencionaban estado por tarjeta, la decision aprobada es que el dashboard no muestra estado por experimento.
- Los estados viven dentro de cada pantalla de modulo.
- Si un experimento se cierra, debe detener/cancelar/limpiar sus recursos antes de volver al dashboard.
- No crear modelo global `EstadoExperimento` para el dashboard.

## 4. Todo experimento debe limpiar recursos al salir

- Ningun experimento debe continuar ejecutandose despues de salir de su pantalla.
- Si hay ejecucion activa y el usuario intenta salir, mostrar confirmacion.
- Si el usuario confirma, detener/cancelar el experimento, liberar recursos y luego navegar.
- Si cancela, permanecer en la pantalla y continuar la ejecucion.
- Usar limpieza defensiva con `DisposableEffect`, pero no usarla para saltarse la confirmacion normal de salida.

## 5. Back del sistema tambien debe manejarse

- El boton volver de la barra superior, el boton atras del sistema y el gesto atras deben seguir la misma regla.
- En Compose, usar `BackHandler` cuando una pantalla tenga ejecucion activa o confirmacion de salida.
- No asumir que solo el boton de la barra superior cubre la navegacion.

## 6. Usar AlertDialog para confirmar cancelaciones

- Si salir de una pantalla cancela o detiene un experimento activo, mostrar confirmacion con `AlertDialog` de Material 3.
- El dialogo debe explicar claramente la consecuencia, por ejemplo que se detendra el experimento y se liberaran recursos.
- Usar acciones claras:
  - confirmar: salir/detener/cancelar;
  - cancelar: continuar en la pantalla.
- Si el usuario confirma, limpiar recursos antes de navegar.
- Si el usuario cancela, cerrar el dialogo y mantener el experimento ejecutandose.
- Los textos del dialogo deben estar en `strings.xml`.

## 7. Estados internos deben estar bien definidos

- Definir estados por modulo antes de implementar.
- Separar estado actual, resultado historico y metricas.
- Ejemplo de separacion:
  - `estado`: situacion actual de ejecucion.
  - `resultadoUltimaEjecucion`: resultado historico de la corrida anterior.
  - metricas: datos como contadores, tiempos, mensajes o recursos usados.
- No convertir metricas en estados.

## 8. Errores no deben dejar recursos activos

- Un estado `Error` debe implicar intento de limpieza.
- Ante error:
  - registrar mensaje visible si aplica;
  - registrar en Logcat;
  - detener servicios/tareas/conexiones;
  - limpiar referencias;
  - permitir reiniciar desde estado controlado.

## 9. Mantener ViewModel sin Context cuando sea posible

- Si el usuario no quiere `Context` dentro del `ViewModel`, crear un controlador Android separado.
- Patron usado:
  - `Controlador...`: maneja `Context`, `ServiceConnection`, `Messenger`, `bindService`, `stopService`, recursos Android.
  - `ViewModel`: mantiene estado puro y acciones logicas.
  - Pantalla Compose: coordina controlador y ViewModel con callbacks simples.
- No poner logica de Android service/messenger directamente dentro de composables.

## 10. Handlers deben evitar leaks

- Si se crea un `Handler` dentro de `Service` o controlador, no usar `inner class` si genera warning de leak.
- Preferir clase privada no interna con `WeakReference`.
- Cancelar mensajes pendientes en limpieza cuando aplique.

## 11. Los comandos de verificacion deben funcionar en Windows

- El usuario trabaja desde Windows/PowerShell.
- No usar `grep` en comandos visibles de verificacion.
- Usar alternativas compatibles:
  - `findstr` para filtros simples.
  - `adb logcat -d -s TAG` para leer buffer de Logcat y terminar.
- Evitar comandos que queden corriendo si el usuario espera una validacion puntual.

## 12. `ps -A` no prueba que un service siga activo

- En Android, un proceso secundario puede quedar cacheado aunque el `Service` ya se haya detenido.
- `adb shell ps -A` sirve para demostrar que el proceso fue creado.
- Para validar si un `Service` sigue activo, preferir `dumpsys activity services`.
- Documentar esta diferencia en la seccion `Como verificar`.

## 13. Cada comando de verificacion debe explicar que valida

- No listar comandos sin contexto.
- En la UI, cada comando de `Como verificar` debe tener una descripcion breve:
  - que valida;
  - cuando ejecutarlo;
  - como interpretar el resultado.
- Si el comando es para Windows, indicarlo o escribirlo directamente compatible con Windows.

## 14. La visualizacion no reemplaza el mecanismo real

- Cada modulo debe tener:
  - capa visual para explicar el concepto;
  - capa tecnica que ejecuta el mecanismo real.
- Animaciones, contadores y graficos no deben fingir procesos, hilos, locks o memoria.
- La UI debe mostrar datos reales reportados por el mecanismo tecnico cuando aplique.

## 15. Nombrar codigo propio en espanol

- Usar nombres propios del proyecto en espanol:
  - `PantallaFabricaRobots`
  - `ServicioFabricaRobots`
  - `EstadoFabricaRobots`
  - `ControladorFabricaRobots`
- Mantener en ingles solo APIs oficiales de Android/Kotlin:
  - `Service`
  - `ViewModel`
  - `Messenger`
  - `BackHandler`
  - `DisposableEffect`

## 16. Localizacion desde el inicio

- Todo texto visible nuevo debe ir en `strings.xml`.
- Espanol es el idioma por defecto.
- No hardcodear textos visibles en composables.
- Los comandos tecnicos literales de verificacion no necesitan ir en `strings.xml`; sus descripciones visibles si deben localizarse.
- Incluir textos de:
  - estados;
  - acciones;
  - dialogos;
  - metricas;
  - secciones;
  - descripciones de comandos.

## 17. No crear dependencias nuevas sin necesidad

- Antes de agregar dependencias, revisar si AndroidX, Compose, Material 3 o APIs estandar bastan.
- Para copiar comandos, usar clipboard de Android/Compose; no agregar librerias.
- Si una API esta deprecada, migrar a la alternativa oficial disponible.

## 18. Previews sin runtime real

- Las previews deben usar datos de ejemplo.
- No iniciar services, sockets, threads reales, controladores Android ni recursos externos desde previews.
- Crear previews de estados representativos, por ejemplo:
  - estado inicial;
  - estado en ejecucion;
  - estado de error.

## 19. Planes deben ser contratos ejecutables

- Un plan debe decir exactamente:
  - archivos a crear/modificar;
  - estados;
  - responsabilidades;
  - controles por estado;
  - ciclo de vida;
  - limpieza;
  - verificacion;
  - criterios de aceptacion.
- Evitar frases abiertas como "usar X o Y o Z" si ya se tomo una decision.
- Si hay varias opciones tecnicas, pedir decision antes de implementar.

## 20. Registro de eventos puede ser plan separado

- Si el componente comun de Registro de eventos aun no existe, no implementarlo dentro de un modulo que no lo tiene aprobado.
- El modulo puede registrar en Logcat mientras tanto.
- Si la utilidad comun ya existe en el momento de implementar, se puede integrar sin recrearla.

## 21. Validaciones deben favorecer demo estable

- Las pruebas son para exposicion academica.
- Evitar ejecuciones infinitas.
- Usar valores por defecto que entren en la ventana de demo.
- Si no hay maximo por decision del usuario, mostrar advertencias para valores altos.
- Bloquear acciones dobles como doble inicio o doble detencion.

## 22. Cerrar decisiones tecnicas en los planes

- No dejar frases como "usar X o Y" si ya se decidio una tecnologia.
- Si un modulo usara `Messenger`, `Service`, sockets, threads, mutex o semaphore, el plan debe decirlo como decision cerrada.
- Evitar palabras como "preferiblemente" cuando el agente necesita una instruccion concreta para implementar.
- Si existe una excepcion, escribir la condicion exacta y el comportamiento esperado.

## 23. Binder IPC puede implementarse con Messenger

- Para Android, `Messenger` es una forma valida de demostrar Binder IPC porque usa Binder por debajo.
- Si se decide usar `Messenger`, el plan debe decir que no se usara AIDL, `LocalBinder`, broadcasts ni archivos temporales.
- El contrato debe definir mensajes `MSG_*`, claves `KEY_*`, direccion de cada mensaje y que respuestas vuelven por `Message.replyTo`.

## 24. Colas IPC deben definir alcance visible

- Si un service recibe varias solicitudes, decidir si rechaza solicitudes concurrentes o si las encola.
- Si se usa cola FIFO, el plan debe especificar si tiene limite fijo o no.
- Si no se implementa historial visual de eventos, aclarar que ids como `idOrdenActual` representan la orden en proceso o la ultima orden reportada, no una lista historica completa.

## 25. Desconectar no siempre requiere confirmacion

- La confirmacion depende de la accion, no solo del hecho de liberar recursos.
- Si el usuario toca explicitamente `Desconectar`, puede limpiarse todo sin confirmacion si el plan lo decide asi.
- Salir de la pantalla debe seguir pidiendo confirmacion cuando implique cancelar trabajo activo o pendiente.

## 26. Build solo con aprobacion cuando el usuario lo pida

- Si el usuario indica que no quiere build automatico, el plan debe decirlo en `Como verificar`.
- Un agente debe pedir confirmacion antes de ejecutar `.\gradlew.bat build`.
- Los comandos ADB pueden quedar en la UI como verificacion manual sin obligar al agente a ejecutarlos.

## 27. El registro de eventos debe tener dueño por pantalla

- Cada pantalla real debe mantener su propio logger de experimento.
- No crear un singleton global de registro.
- No guardar el logger dentro del `ViewModel` si el usuario quiere que la pantalla sea la dueña.
- Usar un helper Compose comun para conectar el logger con la UI cuando varias pantallas repiten el patron.
- El logger debe poder escribir en Logcat y mantener buffer para UI sin depender de Compose.

## 28. Logger comun no debe usar estado Compose internamente

- Evitar `SnapshotStateList` dentro del logger.
- El logger puede recibir un callback `onCambio` para notificar una copia del buffer.
- Si `onCambio` actualiza Compose, el logger debe entregarlo en el main thread.
- Si el logger puede recibir eventos desde callbacks concurrentes, proteger su buffer con `synchronized` u otra tecnica simple.
- No llamar callbacks externos mientras se sostiene un lock.

## 29. Eventos de servicios remotos deben volver por IPC

- Un objeto creado en la pantalla no se comparte con un `Service` en otro proceso.
- No pasar el logger de pantalla a un servicio remoto.
- Si un evento del `Service` debe verse en UI, el `Service` debe publicarlo por el canal IPC ya existente, por ejemplo `Messenger`.
- El controlador traduce el mensaje IPC a `EventoExperimento` y la pantalla lo registra con su logger.
- El timestamp del evento debe nacer en el proceso donde ocurre el evento si se quiere mostrar la hora real del origen.

## 30. Evitar duplicar eventos en Logcat

- Si un evento del `Service` se publica hacia UI, el `Service` no debe escribir tambien `Log.x` para ese mismo evento.
- El logger de la pantalla escribe en Logcat con el tag del experimento.
- Permitir `Log.e` directo en el `Service` solo como fallback tecnico cuando falla el envio IPC o no se puede reportar al cliente.
- Los comandos `adb logcat -d -s TAG` deben seguir usando tags estables por experimento.

## 31. Cola local de servicio debe tener alcance claro

- Si se decide que un `Service` tenga cola local de eventos, definir si sera consultable o solo interna.
- Para mantener el alcance simple, una cola interna puede vivir solo mientras vive el `Service`.
- Si no hay cliente registrado, el evento puede quedar solo en la cola interna.
- No reenviar eventos antiguos ni agregar consulta remota de historial si el plan no lo pide explicitamente.
- Limpiar la cola local al iniciar/reiniciar trabajo real y al destruir el servicio.

## 32. Scaffold comun debe usar slots para acciones variables

- Si varias pantallas necesitan botones distintos en la barra superior, agregar un slot generico `acciones` al scaffold comun.
- No acoplar el scaffold a una funcionalidad especifica como registro, diagnostico o exportacion.
- El scaffold debe encargarse de estructura comun; cada pantalla decide que acciones mostrar.

## 33. Bottom sheet de registro debe ser componente comun

- Si varias pantallas usaran el mismo panel de registro, crear componentes comunes para el boton, la hoja y el panel visual.
- El componente visual del panel debe recibir datos por parametro y no crear recursos reales.
- Si se usa `ModalBottomSheet` de Material 3, documentar que requiere `@OptIn(ExperimentalMaterial3Api::class)`.
- No agregar navegacion nueva para un panel de monitoreo si un bottom sheet resuelve el flujo.

## 34. Limpieza de logs debe ser decision explicita

- Distinguir entre limpieza interna del logger y limpieza manual por usuario.
- Si el usuario no quiere limpieza manual, no mostrar boton `Limpiar` en UI.
- Mantener `limpiar()` como API interna si el ciclo de vida del experimento necesita iniciar una nueva ejecucion con registro limpio.
- Definir exactamente en que acciones se limpia el registro: iniciar, reiniciar, salir, detener o ninguna.

## 35. Reemplazar Log.x requiere criterios verificables

- Si el objetivo es reemplazar logs directos por un logger comun, el plan debe decir en que archivos aplica.
- Definir que `Log.i` y `Log.w` normales no deben quedar en los controladores/servicios cubiertos.
- Definir que `Log.e` directo solo queda como fallback tecnico cuando no se puede reportar por el canal normal.
- Agregar un comando de verificacion con `Select-String` compatible con Windows.
- La verificacion debe explicar como interpretar los resultados.

## 36. Planes deben aplicar lecciones obvias antes de preguntar

- Antes de abrir preguntas decision por decision, el agente debe aplicar al plan las correcciones directas que ya estan cerradas en `lessons.md`.
- Ejemplos: no agregar dependencias nuevas, no ejecutar build automatico, usar textos visibles en `strings.xml`, comandos compatibles con Windows, no crear estados globales de dashboard.
- Las preguntas deben reservarse para decisiones reales que no esten cerradas por `AGENTS.md`, `lessons.md` o `docs/`.
- Despues de aplicar esas correcciones, revisar el plan punto por punto y trabajar solo las ambiguedades restantes con aprobacion del usuario.

## 37. No mostrar limpieza manual de registros

- Ninguna pantalla debe mostrar boton manual para limpiar el registro de eventos.
- Cada experimento debe iniciar una ejecucion nueva con una lista nueva de eventos.
- Mantener `limpiar()` como API interna del logger cuando el ciclo de vida del experimento necesite reiniciar el buffer.
- Definir en cada plan que accion cuenta como inicio de ejecucion nueva.
- No limpiar el registro en acciones intermedias si forman parte de la misma ejecucion activa.

## 38. Verificacion debe probar directamente el experimento

- No mencionar en `Como verificar` herramientas o comandos que no prueban directamente el comportamiento del modulo.
- Si una herramienta solo da contexto general del sistema, dejarla fuera de la verificacion del modulo.
- Cuando se incluya una verificacion parcial u opcional, documentar exactamente que prueba y que no prueba.
- Para sockets TCP locales, preferir una conexion real al puerto mostrado por la UI cuando el dispositivo tenga una herramienta disponible, por ejemplo `adb shell "printf 'prueba\036' | toybox nc 127.0.0.1 PUERTO"`.
- No usar `/proc/net/tcp` como verificacion principal en Android; puede estar vacio o filtrado segun version, permisos o restricciones del dispositivo.

## 39. Sockets TCP locales requieren permiso INTERNET

- En Android, abrir sockets TCP requiere `android.permission.INTERNET` aunque el host sea local, por ejemplo `127.0.0.1`.
- Si un modulo usa `ServerSocket` o `Socket`, el plan debe incluir `AndroidManifest.xml` y agregar `<uses-permission android:name="android.permission.INTERNET" />`.
- Este permiso no implica que el experimento use Internet externo; solo habilita APIs de red del sistema.

## Prompt ejemplo para validar planes

Usar este prompt cuando se quiera revisar otro plan con el mismo grado de detalle antes de implementarlo:

```txt
Lee `android/AGENTS.md`, luego `plans/lessons.md`, luego `docs/app-requirements.md` y, si existe, extrae el texto de `docs/raw-requirements.pdf` usando `pypdf`.

Despues revisa el plan `<ruta-del-plan>`.

No implementes nada todavia.

Primero actualiza el plan aplicando directamente las correcciones que ya esten cerradas por `android/AGENTS.md`, `plans/lessons.md` y `docs/`.

En esta primera pasada:

- No me preguntes por decisiones que ya esten definidas en esos documentos.
- No agregues dependencias nuevas.
- No cambies la arquitectura sin aprobacion.
- No ejecutes build automaticamente.
- Mantén el cambio enfocado en el plan solicitado.
- Convierte instrucciones obvias en contrato ejecutable cuando no requieran una decision nueva.

Despues de esa primera actualizacion, relee el plan completo punto por punto.

Identifica bloqueos, contradicciones y ambiguedades restantes que impedirian que un agente ejecute el plan sin asumir decisiones.

Agrega una nueva seccion al plan al final del documento con los items identificados, luego los usare para responder cada item por separado en el mismo documento. 

Trabajemos decision por decision:

- Presenta solo un punto a la vez.
- Explica por que el punto bloquea o puede causar implementaciones distintas.
- Propone una solucion concreta alineada con `AGENTS.md`, `lessons.md` y `docs/`.

Cuando actualices el plan:

- Convierte el plan en un contrato ejecutable.
- Define archivos exactos a crear/modificar.
- Define estados, metricas, responsabilidades, controles por estado, ciclo de vida, limpieza, verificacion y criterios de aceptacion.
- Evita opciones abiertas como "usar X o Y" si ya se tomo una decision.
- Usa nombres propios en espanol para codigo del proyecto.
- No agregues dependencias nuevas sin aprobacion explicita.
- No ejecutes build automaticamente; si necesitas correr `.\gradlew.bat build`, pideme confirmacion primero.
```
