# app-requirements.md

# Android OS Playground — Requerimientos de Aplicación

## 1. Propósito

Desarrollar una aplicación Android educativa, interactiva y visual que permita demostrar de forma práctica conceptos internos de Sistemas Operativos aplicados al entorno Android.

La aplicación debe convertir conceptos técnicos en simulaciones sencillas y fáciles de explicar durante una exposición, sin ocultar el comportamiento real del sistema operativo.

La solución debe cubrir obligatoriamente:

- Creación y ejecución de procesos o su equivalente en Android.
- Comunicación entre procesos mediante Binder IPC y/o sockets.
- Uso de hilos.
- Uso de mutex.
- Uso de semáforos.
- Demostración de condiciones de carrera.
- Visualización y manipulación controlada del uso de memoria.
- Verificación del comportamiento mediante herramientas propias del entorno Android.

---

# 2. Nombre conceptual

Nombre recomendado:

**Android OS Playground**

Subtítulo opcional:

**Interactive Operating System Experiments for Android**

La aplicación debe tener una identidad visual moderna, tecnológica y educativa, evitando verse como una colección de pantallas aisladas.

---

# 3. Objetivos funcionales

La aplicación debe permitir que un usuario:

1. Seleccione un experimento desde un dashboard principal.
2. Entienda visualmente el problema o concepto que se está demostrando.
3. Ejecute el experimento.
4. Observe su comportamiento en tiempo real.
5. Visualice métricas relevantes.
6. Compare comportamiento correcto e incorrecto cuando aplique.
7. Consulte qué mecanismo de Android se está utilizando.
8. Consulte cómo verificar externamente el experimento mediante ADB, Android Profiler, Logcat, dumpsys u otras herramientas.
9. Reinicie cada experimento sin necesidad de reiniciar la aplicación.
10. Utilice todos los módulos sin requerir conexión a Internet.

---

# 4. Módulos obligatorios

La aplicación debe incluir los siguientes 8 módulos.

---

## 4.1 Robot Factory — Procesos

### Objetivo

Demostrar el equivalente práctico de creación de procesos dentro de una aplicación Android.

### Concepto visual

Una oficina principal enciende una fábrica secundaria.

- Oficina = proceso principal.
- Fábrica = proceso secundario.
- Cada proceso debe mostrar su PID.

### Requerimientos funcionales

- Mostrar el PID del proceso principal.
- Permitir iniciar un componente Android en un proceso separado.
- Mostrar el PID del proceso secundario.
- Demostrar visualmente que ambos PID son diferentes.
- Mostrar el estado del proceso secundario:
  - Detenido.
  - Iniciando.
  - Ejecutándose.
  - Finalizado.
- Permitir detener el proceso secundario.
- Registrar los cambios de estado en un panel de eventos.
- Permitir ejecutar el experimento repetidamente.

### Implementación esperada

Utilizar un `Service` configurado para ejecutarse en un proceso distinto mediante `android:process`.

Ejemplo conceptual:

```xml
<service
    android:name=".process.RobotFactoryService"
    android:process=":robot_factory" />
```

### Datos mínimos a mostrar

- PID proceso principal.
- PID proceso secundario.
- Nombre lógico de cada proceso.
- Estado actual.
- Hora de inicio.
- Cantidad de mensajes intercambiados, si aplica.

### Verificación externa

La interfaz debe mostrar comandos sugeridos como:

```bash
adb shell ps -A
adb shell top
adb logcat
```

---

## 4.2 Restaurant IPC — Binder IPC

### Objetivo

Demostrar comunicación real entre dos procesos Android.

### Concepto visual

Un mesero envía una orden a la cocina.

- Mesero = proceso cliente.
- Cocina = proceso servidor.
- Orden = mensaje IPC.
- Respuesta de cocina = mensaje de retorno.

### Requerimientos funcionales

- Permitir escribir o seleccionar una orden.
- Enviar la orden desde el proceso principal al proceso secundario.
- Mostrar visualmente el recorrido de la orden.
- Mostrar estado:
  - Preparando mensaje.
  - Enviando.
  - Recibido.
  - Procesando.
  - Respuesta enviada.
  - Respuesta recibida.
- Mostrar el contenido enviado.
- Mostrar el contenido de la respuesta.
- Mostrar PID origen.
- Mostrar PID destino.
- Registrar timestamps.
- Registrar errores de comunicación.
- Permitir desconectar y reconectar el servicio.

### Implementación esperada

Usar uno de los mecanismos IPC reales de Android:

- Binder.
- Messenger.
- AIDL.

Para un proyecto estudiantil se recomienda **Messenger o Binder**, siempre que exista comunicación real entre procesos.

### Verificación externa

```bash
adb logcat
adb shell dumpsys activity services
adb shell ps -A
```

---

## 4.3 Agent Network — Sockets

### Objetivo

Demostrar comunicación cliente-servidor mediante sockets TCP.

### Concepto visual

Un agente envía un mensaje secreto a una central.

### Requerimientos funcionales

- Crear un servidor TCP local.
- Mostrar host e IP utilizados.
- Mostrar puerto.
- Mostrar estado del servidor:
  - Offline.
  - Starting.
  - Listening.
  - Client connected.
- Permitir ingresar un mensaje.
- Enviar el mensaje mediante un socket.
- Recibir el mensaje en el servidor.
- Generar una respuesta desde el servidor.
- Mostrar la respuesta en el cliente.
- Registrar:
  - Hora de conexión.
  - Mensaje enviado.
  - Mensaje recibido.
  - Bytes enviados.
  - Bytes recibidos.
  - Tiempo aproximado de respuesta.
- Permitir cerrar cliente y servidor.
- Manejar errores sin cerrar la aplicación.

### Implementación esperada

Utilizar APIs reales:

- `ServerSocket`
- `Socket`
- `InputStream`
- `OutputStream`

El servidor no debe ejecutarse en el hilo principal.

### Verificación externa

```bash
adb logcat
adb shell top
adb shell dumpsys
```

---

## 4.4 Thread Race — Hilos

### Objetivo

Demostrar ejecución concurrente utilizando diferentes cantidades de hilos.

### Concepto visual

Varios corredores compiten por terminar una carga de trabajo.

### Requerimientos funcionales

- Permitir seleccionar:
  - 1 hilo.
  - 2 hilos.
  - 4 hilos.
  - 8 hilos.
- Permitir configurar una cantidad de trabajo.
- Iniciar la ejecución.
- Mostrar cada hilo de forma independiente.
- Mostrar progreso individual.
- Mostrar estado:
  - Waiting.
  - Running.
  - Finished.
  - Cancelled.
- Mostrar tiempo de ejecución por hilo.
- Mostrar tiempo total.
- Permitir cancelar la ejecución.
- Permitir repetir la misma prueba.
- Mantener resultados históricos de la sesión actual.
- Mostrar comparación entre diferentes cantidades de hilos.

### Requerimiento académico

La carga de trabajo debe realizar operaciones reales y no limitarse a animaciones.

Ejemplos aceptables:

- Cálculos matemáticos.
- Procesamiento de arreglos.
- Generación de hashes.
- Ordenamiento de datos.
- Procesamiento de bloques numéricos.

### Restricción

Nunca bloquear el Main/UI Thread.

---

## 4.5 Chaos Bank — Race Condition

### Objetivo

Demostrar una condición de carrera producida por acceso concurrente no sincronizado.

### Concepto visual

Varios cajeros modifican simultáneamente una cuenta compartida.

### Requerimientos funcionales

- Definir un valor compartido.
- Definir múltiples workers.
- Permitir configurar cantidad de operaciones.
- Ejecutar operaciones sin protección.
- Mostrar:
  - Resultado esperado.
  - Resultado real.
  - Diferencia.
- Indicar claramente cuando se detecta una condición de carrera.
- Mostrar cuáles hilos acceden al recurso.
- Permitir repetir el experimento.
- Incluir modo rápido para facilitar una demostración en vivo.
- No simular el error mediante números aleatorios: la condición de carrera debe originarse por concurrencia real.

### Resultado esperado

El usuario debe poder observar que múltiples hilos accediendo al mismo estado sin sincronización pueden producir resultados incorrectos.

---

## 4.6 Ticket Rush — Mutex

### Objetivo

Demostrar cómo un mutex protege una sección crítica.

### Concepto visual

Varias personas intentan comprar simultáneamente el último boleto disponible.

### Requerimientos funcionales

- Mostrar al menos un recurso limitado compartido.
- Crear múltiples compradores concurrentes.
- Permitir ejecutar el experimento con mutex desactivado.
- Permitir ejecutar el experimento con mutex activado.
- Mostrar claramente quién entra a la sección crítica.
- Mostrar qué hilos se encuentran esperando.
- Mostrar cuándo se adquiere el lock.
- Mostrar cuándo se libera el lock.
- Mostrar resultado final.
- Permitir comparar:
  - Sin mutex.
  - Con mutex.
- Explicar dentro de la interfaz que el mutex permite acceso exclusivo a la sección crítica.

### Resultado deseado

Sin protección, el sistema debe poder exhibir comportamiento incorrecto.

Con mutex, únicamente un worker debe modificar el recurso crítico a la vez.

---

## 4.7 Smart Parking — Semaphore

### Objetivo

Demostrar control de acceso concurrente a una cantidad limitada de recursos.

### Concepto visual

Un estacionamiento tiene una cantidad limitada de espacios y múltiples automóviles desean ingresar.

### Requerimientos funcionales

- Permitir seleccionar cantidad de espacios disponibles.
- Permitir seleccionar cantidad de vehículos.
- Cada vehículo debe representar una tarea/hilo.
- Mostrar estados:
  - Waiting.
  - Entering.
  - Parked.
  - Leaving.
  - Finished.
- El número máximo de vehículos simultáneos debe estar controlado mediante un semáforo real.
- Permitir modificar los permisos del semáforo.
- Mostrar:
  - Permisos totales.
  - Permisos disponibles.
  - Recursos ocupados.
  - Hilos esperando.
- Permitir reiniciar la simulación.

### Ejemplo

Con:

```text
Semaphore(3)
```

solo tres vehículos pueden estar dentro de la sección protegida al mismo tiempo.

### Resultado esperado

El usuario debe poder ver claramente la diferencia conceptual entre:

- Mutex: un único acceso exclusivo.
- Semaphore: una cantidad configurable de accesos simultáneos.

---

## 4.8 Memory Monster — Memoria

### Objetivo

Demostrar visualmente cambios reales en el consumo de memoria de la aplicación.

### Concepto visual

Un personaje llamado Memory Monster crece mientras consume memoria.

### Requerimientos funcionales

- Mostrar memoria utilizada por la aplicación.
- Mostrar memoria disponible cuando sea posible.
- Mostrar un indicador gráfico del consumo.
- Permitir reservar memoria de forma controlada.
- Incluir como mínimo:
  - +10 MB.
  - +25 MB o +50 MB.
- Permitir liberar referencias a memoria reservada.
- Permitir solicitar una ejecución de GC únicamente como demostración, aclarando que el sistema decide cuándo recolectar.
- Mostrar advertencia antes de aproximarse a niveles inseguros.
- Impedir intencionalmente provocar un `OutOfMemoryError` que cierre la aplicación durante una exposición.
- Mostrar histórico de consumo dentro de la sesión.
- Cambiar visualmente el estado del personaje según el consumo.

### Estados visuales sugeridos

- Healthy.
- Moderate.
- High.
- Memory Pressure.

### Validación principal

La variación del consumo debe poder observarse en Android Profiler.

### Verificación externa

```bash
adb shell dumpsys meminfo <package>
adb shell top
adb logcat
```

---

# 5. Dashboard principal

La aplicación debe iniciar en un dashboard que muestre los 8 experimentos.

Cada tarjeta debe incluir:

- Icono.
- Nombre temático.
- Concepto técnico.
- Breve descripción.
- Estado.
- Botón para abrir.

Ejemplo:

```text
🏭 Robot Factory
Processes

🍔 Restaurant IPC
Binder IPC

🕵️ Agent Network
Sockets

🏎️ Thread Race
Threads

🏦 Chaos Bank
Race Condition

🎟️ Ticket Rush
Mutex

🚗 Smart Parking
Semaphore

👾 Memory Monster
Memory
```

---

# 6. System Diagnostics

Además de los 8 módulos, la aplicación debe incluir una sección de diagnóstico.

## Objetivo

Relacionar cada experimento con las herramientas utilizadas para analizar Android.

## Herramientas que deben aparecer

- Android Profiler.
- `adb shell top`.
- `adb logcat`.
- Binder IPC.
- sockets.
- systrace / Perfetto cuando sea aplicable.
- atrace cuando esté disponible en el entorno.
- dumpsys.
- ADB.
- Dumpstate.
- Battery Historian.

## Importante

No es obligatorio recrear estas herramientas dentro de la aplicación.

La aplicación debe generar actividad real que posteriormente pueda ser observada mediante dichas herramientas.

---

# 7. Panel "How to Verify"

Cada módulo debe incluir una sección:

**How to Verify / Cómo verificar**

Debe explicar qué herramienta utilizar para comprobar que el experimento realmente está ocurriendo.

Ejemplo para memoria:

```bash
adb shell dumpsys meminfo com.example.osplayground
```

Ejemplo para procesos:

```bash
adb shell ps -A | grep osplayground
```

Ejemplo para logs:

```bash
adb logcat
```

El usuario debe poder copiar fácilmente cada comando.

---

# 8. Event Log interno

Cada experimento debe poseer un pequeño panel de eventos.

Formato recomendado:

```text
20:41:02  Process started
20:41:02  PID 14591 created
20:41:04  Binder connected
20:41:06  Message sent
20:41:06  Message received
```

## Requerimientos

- Timestamp.
- Tipo de evento.
- Mensaje corto.
- Scroll.
- Botón para limpiar.
- No almacenar indefinidamente los registros.

Los mismos eventos importantes también deben enviarse a Logcat usando tags identificables.

Ejemplo:

```text
OSPlayground/ProcessLab
OSPlayground/BinderIPC
OSPlayground/SocketLab
OSPlayground/ThreadRace
OSPlayground/ChaosBank
OSPlayground/Mutex
OSPlayground/Semaphore
OSPlayground/Memory
```

---

# 9. Requerimientos de interfaz

## Diseño

La interfaz debe ser:

- Moderna.
- Tecnológica.
- Visual.
- Fácil de utilizar.
- Adecuada para una demostración universitaria.
- Consistente entre módulos.

## Cada experimento debe presentar

1. Nombre temático.
2. Concepto de sistema operativo.
3. Explicación corta.
4. Controles.
5. Simulación visual.
6. Métricas.
7. Resultado.
8. Event Log.
9. Sección "Cómo verificar".

---

# 10. Requerimientos de UX

- Un experimento debe poder iniciarse en máximo 2 interacciones desde el dashboard.
- Cada experimento debe tener un botón claro de `Start`.
- Cada experimento debe tener un botón `Reset`.
- Cuando aplique, debe existir `Stop` o `Cancel`.
- El usuario nunca debe necesitar reiniciar la aplicación para repetir una prueba.
- Los errores deben mostrarse visualmente sin provocar un cierre inesperado.
- Debe existir feedback visual mientras una operación está ejecutándose.
- Las acciones peligrosas o de alto consumo deben estar limitadas.
- Las animaciones deben representar eventos reales del experimento y no sustituir la ejecución real.
- La UI debe permanecer responsiva durante todas las pruebas.

---

# 11. Requerimientos técnicos generales

## Plataforma

- Android nativo.
- El proyecto debe poder ejecutarse desde Android Studio.
- Debe ejecutarse como mínimo en un emulador Android moderno.

## Stack recomendado

Para una implementación estudiantil moderna:

- Kotlin.
- Jetpack Compose para UI.
- Coroutines cuando sean apropiadas.
- APIs Java/Kotlin de concurrencia cuando el concepto requiera threads explícitos.
- Android Services para procesos.
- Binder / Messenger / AIDL para IPC.
- Java/Kotlin Socket APIs para sockets.

Si el proyecto existente utiliza XML Views, puede mantenerse esa arquitectura; no es obligatorio migrar a Compose.

---

# 12. Concurrencia

La aplicación debe distinguir correctamente entre:

- Thread.
- Coroutine.
- Process.
- Mutex.
- Semaphore.

No se debe utilizar una animación para fingir concurrencia.

Las operaciones deben ejecutarse realmente de manera concurrente cuando el experimento así lo requiere.

---

# 13. Main Thread

Está prohibido ejecutar cargas pesadas directamente en el hilo principal.

Ningún experimento debe:

- Congelar la UI.
- Producir ANR intencionalmente.
- Bloquear navegación.
- Ejecutar sockets bloqueantes en Main Thread.

---

# 14. Manejo de estado

Cada módulo debe manejar al menos:

```text
Idle
Running
Success
Error
Cancelled
```

Cuando aplique también:

```text
Waiting
Locked
Connected
Disconnected
```

Los estados deben mostrarse visualmente.

---

# 15. Seguridad y estabilidad de la demostración

La aplicación será utilizada durante una exposición, por lo que debe priorizar estabilidad.

## No permitido

- Crash intencional como única demostración.
- `OutOfMemoryError` deliberado.
- ANR deliberado.
- Loops infinitos.
- Consumo ilimitado de memoria.
- Creación ilimitada de threads.
- Creación ilimitada de procesos.
- Sockets sin mecanismo de cierre.
- Servicios que continúen ejecutándose sin control después de salir del módulo.

## Obligatorio

- Limitar recursos.
- Liberar sockets.
- Cancelar threads/tareas.
- Detener services cuando corresponda.
- Limpiar recursos al reiniciar experimentos.
- Manejar excepciones.

---

# 16. Observabilidad

La aplicación debe facilitar la demostración mediante herramientas externas.

Los eventos relevantes deben aparecer en Logcat.

Se recomienda agregar logs para:

- Creación de proceso.
- PID.
- Conexión Binder.
- Envío IPC.
- Recepción IPC.
- Apertura/cierre de sockets.
- Inicio/finalización de threads.
- Lock/unlock de mutex.
- Acquire/release de semaphore.
- Reserva/liberación de memoria.

---

# 17. Android Profiler

El proyecto debe permitir demostrar como mínimo:

- CPU activity.
- Memory usage.
- Threads.
- Consumo generado por experimentos.

El módulo Memory Monster debe estar especialmente diseñado para verse claramente desde Android Profiler.

---

# 18. ADB

Durante la exposición se debe poder utilizar ADB para validar lo que sucede.

Comandos sugeridos:

```bash
adb devices
adb shell ps -A
adb shell top
adb logcat
adb shell dumpsys meminfo <package>
adb shell dumpsys activity services
```

Los comandos exactos podrán variar según versión de Android y dispositivo.

---

# 19. Dumpsys

La aplicación debe generar escenarios que puedan inspeccionarse mediante `dumpsys`.

Ejemplos:

- Información de memoria.
- Services.
- Activities.
- Procesos.
- Estado general de componentes.

---

# 20. Tracing

Al menos un escenario debe permitir generar carga suficiente para demostrar tracing.

El candidato recomendado es:

**Thread Race**

La ejecución debe generar actividad de CPU suficientemente visible para analizarla posteriormente utilizando herramientas disponibles en el entorno Android, por ejemplo Perfetto/System Trace, systrace o atrace según compatibilidad de la versión.

---

# 21. Dumpstate

Dumpstate debe tratarse como una herramienta externa de diagnóstico.

No se requiere implementar una copia de Dumpstate dentro de la aplicación.

Se debe documentar:

- Qué información recopila.
- Cuándo sería útil.
- Cómo se relaciona con los experimentos realizados.

---

# 22. Battery Historian

Battery Historian debe utilizarse como herramienta complementaria.

Se recomienda ejecutar uno o varios experimentos durante un periodo controlado y posteriormente analizar:

- Actividad.
- Procesos.
- CPU.
- Wakeups cuando aplique.
- Impacto energético observable.

No es requisito que la aplicación reproduzca Battery Historian internamente.

---

# 23. Requerimientos de demostración

Cada módulo debe ser demostrable en aproximadamente 30 a 90 segundos.

Cada demostración debe seguir:

1. Explicar el concepto.
2. Ejecutar la simulación.
3. Mostrar el resultado.
4. Mostrar el mecanismo técnico utilizado.
5. Verificar mediante Android/ADB cuando corresponda.

---

# 24. Requerimientos de código

El código debe:

- Ser funcional.
- Estar organizado por módulos.
- Tener nombres descriptivos.
- Contener comentarios únicamente donde aporten valor académico.
- Separar UI de lógica de experimentación.
- Evitar duplicación innecesaria.
- Manejar excepciones.
- Liberar recursos.
- Ser suficientemente sencillo para que los integrantes puedan explicarlo.

---

# 25. Organización recomendada

```text
app/
└── src/main/java/.../
    ├── dashboard/
    ├── process/
    │   └── RobotFactory
    ├── ipc/
    │   └── RestaurantIPC
    ├── socket/
    │   └── AgentNetwork
    ├── threads/
    │   └── ThreadRace
    ├── racecondition/
    │   └── ChaosBank
    ├── mutex/
    │   └── TicketRush
    ├── semaphore/
    │   └── SmartParking
    ├── memory/
    │   └── MemoryMonster
    ├── diagnostics/
    └── common/
```

La estructura exacta puede adaptarse a la arquitectura existente.

---

# 26. Criterios de aceptación globales

La aplicación se considera completa cuando:

- [ ] Existe un dashboard funcional.
- [ ] Robot Factory demuestra dos procesos con PID diferentes.
- [ ] Restaurant IPC realiza comunicación real entre procesos.
- [ ] Agent Network transmite datos mediante sockets reales.
- [ ] Thread Race utiliza múltiples threads reales.
- [ ] Chaos Bank genera una condición de carrera real.
- [ ] Ticket Rush demuestra el efecto de un mutex.
- [ ] Smart Parking utiliza un semaphore real.
- [ ] Memory Monster modifica de forma controlada el consumo real de memoria.
- [ ] Todos los experimentos pueden reiniciarse.
- [ ] Ninguna prueba bloquea permanentemente la UI.
- [ ] Existen logs internos.
- [ ] Existen logs útiles en Logcat.
- [ ] Cada módulo indica cómo verificar el resultado.
- [ ] La aplicación puede analizarse con Android Profiler.
- [ ] Se puede utilizar `adb shell top`.
- [ ] Se puede utilizar `adb logcat`.
- [ ] Se puede inspeccionar información mediante `dumpsys`.
- [ ] Binder IPC está demostrado.
- [ ] socket está demostrado.
- [ ] Se documenta tracing con systrace/atrace o herramienta equivalente disponible.
- [ ] Se documenta Dumpstate.
- [ ] Se documenta Battery Historian.
- [ ] El código fuente está comentado y organizado.
- [ ] Las demostraciones son repetibles durante una exposición.

---

# 27. Alcance académico que cubre el proyecto

Con esta aplicación se deben cubrir los siguientes conceptos:

| Tema | Módulo |
|---|---|
| Procesos / equivalente de fork | Robot Factory |
| IPC | Restaurant IPC |
| Binder IPC | Restaurant IPC |
| Sockets | Agent Network |
| Hilos | Thread Race |
| Condiciones de carrera | Chaos Bank |
| Mutex | Ticket Rush |
| Semáforos | Smart Parking |
| Memoria | Memory Monster |
| Android Profiler | Memory Monster / Thread Race |
| adb shell top | Process / Thread / Memory |
| adb logcat | Todos |
| dumpsys | Process / Memory / Services |
| systrace / atrace / tracing equivalente | Thread Race |
| ADB | Todos |
| Dumpstate | Diagnostics |
| Battery Historian | Diagnostics |

---

# 28. Fuera de alcance

Para mantener el proyecto realizable por estudiantes, inicialmente queda fuera de alcance:

- Backend.
- Base de datos remota.
- Login.
- Registro de usuarios.
- Servicios cloud.
- Firebase.
- Networking externo.
- Persistencia compleja.
- Cuenta de usuario.
- Sincronización online.
- Integración con APIs externas.
- Emulación completa de herramientas de sistema.
- Root del dispositivo.
- Modificación del kernel.
- Ejecución directa de comandos privilegiados desde la aplicación.

La prioridad es demostrar correctamente mecanismos de Sistemas Operativos dentro de Android.

---

# 29. Prioridad de implementación

## P0 — Obligatorio

1. Dashboard.
2. Robot Factory.
3. Restaurant IPC.
4. Agent Network.
5. Thread Race.
6. Chaos Bank.
7. Ticket Rush.
8. Smart Parking.
9. Memory Monster.
10. Logcat.
11. Panel de verificación.

## P1 — Muy recomendado

- Métricas en vivo.
- Historial de eventos.
- Animaciones.
- Comparaciones.
- Diagnóstico.
- System information.
- Gráficas simples.

## P2 — Opcional

- Gamificación.
- Progreso de experimentos completados.
- Tema oscuro.
- Animaciones avanzadas.
- Exportación de resultados.
- Historial persistente.

---

# 30. Principio principal del proyecto

**La simulación visual nunca debe reemplazar el mecanismo técnico real.**

Cada módulo debe tener dos capas:

### Capa visual

Permite al usuario comprender fácilmente el concepto.

### Capa técnica

Ejecuta realmente el mecanismo Android correspondiente.

Ejemplo:

```text
🚗 Smart Parking
        ↓
Animación de vehículos
        ↓
Threads reales
        ↓
Semaphore real
        ↓
Logs
        ↓
Verificación mediante herramientas Android
```

Este principio debe mantenerse en todos los módulos.

---

# Apendice A - Ajuste de estado en el dashboard

Aunque la seccion de dashboard principal menciona inicialmente que cada tarjeta puede incluir un estado, el diseno aprobado para este proyecto no mostrara una etiqueta de estado por experimento en el dashboard.

## Decision

El dashboard principal debe enfocarse en:

- Icono.
- Nombre tematico.
- Concepto tecnico.
- Breve descripcion.
- Boton para abrir el experimento.

No debe mostrar un estado global como `Pendiente`, `Disponible`, `Inactivo`, `Ejecutando`, `Exitoso`, `Error` o `Cancelado`.

## Razon

Los experimentos no deben continuar ejecutandose despues de salir de su pantalla. Si el usuario intenta salir mientras algo esta corriendo, el modulo real debe cancelar, detener o liberar recursos antes de volver al dashboard.

Por esa razon, el dashboard no necesita reflejar estado de ejecucion. Los estados pertenecen a cada experimento mientras su pantalla esta abierta.

## Alcance de los estados

Cada modulo real podra manejar internamente estados como:

- Inactivo.
- Ejecutando.
- Exitoso.
- Error.
- Cancelado.
- Conectado o desconectado, cuando aplique.
- Esperando, bloqueado o similar, cuando el concepto lo requiera.

Estos estados deben mostrarse dentro de la pantalla del experimento correspondiente, no en el dashboard principal.

## Impacto en implementacion

El plan base de la aplicacion no debe crear un modelo global `EstadoExperimento`.

Los planes de implementacion de cada modulo deben definir sus propios estados de ejecucion segun el comportamiento real del experimento.
