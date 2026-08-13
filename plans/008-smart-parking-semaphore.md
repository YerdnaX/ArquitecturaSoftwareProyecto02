# Plan 008 - Smart Parking Semaphore

## Estado del plan

- Estado: pendiente de aprobacion.
- Alcance: solo define el contrato de implementacion del modulo Smart Parking.
- No implementar este plan hasta que el usuario lo apruebe explicitamente.
- No agregar dependencias nuevas.
- No ejecutar build automaticamente; si se requiere `.\gradlew.bat build`, pedir confirmacion primero.

## Objetivo

Demostrar un `Semaphore` real de Java/Kotlin controlando el acceso concurrente a un estacionamiento con cupos limitados.

La capa visual muestra vehiculos entrando, estacionados, esperando y saliendo. La capa tecnica debe crear trabajo concurrente real: cada vehiculo se ejecuta en una tarea/hilo fuera del Main Thread, llama a `Semaphore.acquire()` para entrar y llama a `Semaphore.release()` al salir.

## Dependencias del proyecto

- Depende de `001-foundation-dashboard-common.md` para dashboard, estructura comun de pantalla y componentes comunes ya aprobados.
- Puede reutilizar patrones simples de hilos de `005-thread-race-threads.md` si ya existen al momento de implementar.
- Si existe un logger comun aprobado, usarlo como dueno de pantalla segun `plans/lessons.md`.
- Si el logger comun no existe, registrar eventos visibles en estado de pantalla y enviar eventos importantes a Logcat con tag `OSPlayground/Semaphore`, sin crear un singleton global.
- No crear un estado global de dashboard para este experimento.

## Archivos exactos a crear

Crear estos archivos dentro de `android/app/src/main/java/io/yerdna/architecturasos/`:

- `semaphore/EstadoEstacionamiento.kt`
- `semaphore/VehiculoEstacionamiento.kt`
- `semaphore/ResultadoEstacionamiento.kt`
- `semaphore/EjecutorEstacionamiento.kt`
- `semaphore/SmartParkingViewModel.kt`
- `ui/screen/PantallaSmartParking.kt`

## Archivos exactos a modificar

- `android/app/src/main/java/io/yerdna/architecturasos/ui/App.kt`
  - Agregar la ruta o seleccion del dashboard que abre `PantallaSmartParking`.
  - No agregar estado de ejecucion al dashboard.
- `android/app/src/main/res/values/strings.xml`
  - Agregar todos los textos visibles nuevos del modulo.
  - Incluir textos de titulo, descripcion, estados, acciones, metricas, dialogo de salida, seccion de verificacion, descripciones de comandos y mensajes de error.

No modificar `AndroidManifest.xml`. Este modulo no usa servicios, sockets ni permisos nuevos.

## Responsabilidades por archivo

### `EstadoEstacionamiento.kt`

Definir estados simples del experimento y de los vehiculos.

Estados del experimento:

- `Inactivo`: no hay ejecucion activa y se pueden cambiar parametros.
- `Ejecutando`: hay vehiculos activos, esperando o estacionados.
- `Exitoso`: todos los vehiculos terminaron y no hubo error.
- `Cancelado`: el usuario cancelo o salio confirmando cancelacion.
- `Error`: ocurrio una excepcion y se intento limpiar recursos.

Estados por vehiculo:

- `Esperando`: el vehiculo fue creado y espera un permiso del semaforo.
- `Entrando`: adquirio permiso y esta entrando al estacionamiento.
- `Estacionado`: ocupa un cupo protegido por el semaforo.
- `Saliendo`: esta liberando el cupo.
- `Finalizado`: libero permiso y termino.
- `Cancelado`: fue cancelado antes de terminar.
- `Error`: fallo durante su ejecucion.

### `VehiculoEstacionamiento.kt`

Definir el modelo visible de cada vehiculo:

- `id: Int`
- `nombre: String`
- `estado: EstadoVehiculoEstacionamiento`
- `tienePermiso: Boolean`
- `horaInicioMs: Long?`
- `horaEntradaMs: Long?`
- `horaSalidaMs: Long?`
- `tiempoEsperaMs: Long?`
- `tiempoEstacionadoMs: Long?`
- `mensaje: String`

Usar nombres propios en espanol. Mantener nombres de APIs oficiales en ingles solo cuando correspondan.

### `ResultadoEstacionamiento.kt`

Definir metricas y resultado de la ejecucion:

- `permisosTotales: Int`
- `permisosDisponibles: Int`
- `recursosOcupados: Int`
- `hilosEsperando: Int`
- `vehiculosFinalizados: Int`
- `vehiculosCancelados: Int`
- `vehiculosConError: Int`
- `maximoVehiculosSimultaneos: Int`
- `duracionTotalMs: Long?`
- `resultadoUltimaEjecucion: String?`

La metrica `maximoVehiculosSimultaneos` debe servir para aceptar o rechazar la ejecucion: nunca puede ser mayor que `permisosTotales`.

### `EjecutorEstacionamiento.kt`

Ejecutar el mecanismo tecnico real.

Responsabilidades:

- Crear `Semaphore(permisosTotales)`.
- Crear una tarea real por vehiculo usando APIs estandar de concurrencia ya disponibles en Kotlin/JDK/Android.
- Ejecutar trabajo fuera del Main Thread.
- Para cada vehiculo:
  - reportar `Esperando`;
  - llamar a `acquire()`;
  - reportar `Entrando`;
  - reportar `Estacionado`;
  - esperar un tiempo corto y controlado para que la demo sea visible;
  - reportar `Saliendo`;
  - llamar a `release()`;
  - reportar `Finalizado`.
- Proteger contadores compartidos con una tecnica simple y clara.
- Reportar eventos por callbacks hacia el `ViewModel`.
- Reportar errores de forma controlada.
- Implementar `cancelar()` para interrumpir/cancelar tareas activas y liberar permisos retenidos.
- Implementar `limpiar()` para cancelar tareas, liberar referencias internas y dejar el ejecutor en estado reusable.

No usar animaciones como sustituto del semaforo real. La UI solo visualiza estados reportados por el ejecutor.

### `SmartParkingViewModel.kt`

Mantener estado puro de pantalla y coordinar acciones.

Responsabilidades:

- Exponer el estado completo de `PantallaSmartParking`.
- Validar parametros antes de iniciar.
- Crear y usar `EjecutorEstacionamiento`.
- Actualizar vehiculos, metricas y resultado.
- Bloquear acciones no validas segun estado.
- Preparar eventos para la UI y Logcat mediante el logger de pantalla o callbacks simples.
- No guardar `Context`.
- No iniciar trabajo pesado en el Main Thread.
- Cancelar y limpiar recursos en `onCleared()`.

### `PantallaSmartParking.kt`

Implementar la pantalla Compose.

Responsabilidades:

- Mostrar titulo tematico: `Smart Parking`.
- Mostrar concepto tecnico: `Semaphore`.
- Mostrar explicacion corta comparando mutex y semaforo.
- Mostrar controles de parametros.
- Mostrar visualizacion de vehiculos.
- Mostrar metricas.
- Mostrar resultado de la ultima ejecucion.
- Mostrar panel de eventos del experimento.
- Mostrar seccion `Como verificar`.
- Usar `BackHandler` cuando haya ejecucion activa o cancelacion pendiente.
- Mostrar `AlertDialog` si el usuario intenta salir mientras hay ejecucion activa.
- Usar textos desde `strings.xml`, no textos visibles hardcodeados.
- Usar previews solo con datos de ejemplo, sin iniciar hilos reales.

## Parametros definidos

- Espacios disponibles:
  - minimo: `1`
  - maximo: `6`
  - valor inicial: `3`
- Vehiculos:
  - minimo: `1`
  - maximo: `12`
  - valor inicial: `8`
- Duracion visible por vehiculo dentro del estacionamiento:
  - usar valores cortos y controlados para una demo de 30 a 90 segundos.
  - no crear bucles infinitos.
- No permitir cambiar espacios ni vehiculos mientras el estado sea `Ejecutando`.

## Controles por estado

| Estado | Espacios | Vehiculos | Iniciar | Cancelar | Reset | Volver |
|---|---|---|---|---|---|---|
| `Inactivo` | habilitado | habilitado | habilitado | deshabilitado | habilitado | sale sin dialogo |
| `Ejecutando` | deshabilitado | deshabilitado | deshabilitado | habilitado | deshabilitado | muestra confirmacion |
| `Exitoso` | habilitado | habilitado | habilitado | deshabilitado | habilitado | sale sin dialogo |
| `Cancelado` | habilitado | habilitado | habilitado | deshabilitado | habilitado | sale sin dialogo |
| `Error` | habilitado | habilitado | habilitado | deshabilitado | habilitado | sale sin dialogo |

Reglas:

- `Iniciar` empieza una ejecucion nueva y limpia el registro de eventos de esa ejecucion.
- `Cancelar` detiene tareas activas, libera permisos retenidos y conserva los eventos de la ejecucion cancelada.
- `Reset` solo esta disponible sin ejecucion activa; deja parametros en valores iniciales, borra vehiculos, metricas de corrida y resultado visible.
- Salir desde boton superior, Back del sistema o gesto atras debe seguir la misma regla.

## Ciclo de vida

1. Al abrir pantalla:
   - estado `Inactivo`;
   - parametros iniciales;
   - sin tareas activas;
   - sin vehiculos creados.
2. Al tocar `Iniciar`:
   - validar parametros;
   - limpiar eventos de ejecucion anterior;
   - crear `Semaphore`;
   - crear lista de vehiculos;
   - cambiar a `Ejecutando`;
   - iniciar tareas fuera del Main Thread.
3. Durante ejecucion:
   - actualizar estados por vehiculo;
   - recalcular permisos disponibles, recursos ocupados e hilos esperando;
   - registrar eventos `acquire` y `release`.
4. Al terminar correctamente:
   - todos los vehiculos quedan en `Finalizado`;
   - estado `Exitoso`;
   - `resultadoUltimaEjecucion` resume duracion y maximo simultaneo.
5. Al cancelar:
   - detener tareas activas;
   - liberar permisos retenidos;
   - vehiculos no terminados pasan a `Cancelado`;
   - estado `Cancelado`.
6. Ante error:
   - registrar mensaje visible;
   - registrar en Logcat;
   - intentar cancelar y liberar recursos;
   - estado `Error`;
   - permitir iniciar de nuevo desde estado controlado.
7. Al salir de pantalla:
   - si esta `Ejecutando`, mostrar confirmacion;
   - si confirma, cancelar, limpiar recursos y navegar;
   - si cancela el dialogo, continuar en la pantalla;
   - usar limpieza defensiva con `DisposableEffect` para cancelar recursos si la pantalla se elimina.
8. En `ViewModel.onCleared()`:
   - cancelar tareas activas;
   - limpiar referencias del ejecutor.

## Registro de eventos

- El registro pertenece a `PantallaSmartParking`.
- No crear singleton global de logs.
- No mostrar boton manual `Limpiar`.
- Al iniciar una ejecucion nueva, el buffer visible empieza vacio.
- Registrar eventos importantes:
  - inicio de ejecucion;
  - vehiculo esperando;
  - `acquire` exitoso;
  - vehiculo estacionado;
  - `release`;
  - vehiculo finalizado;
  - cancelacion;
  - error;
  - limpieza al salir.
- Enviar eventos importantes a Logcat con tag estable `OSPlayground/Semaphore`.
- Si se usa logger comun, no duplicar `Log.i` o `Log.w` directo en ejecutor/controlador.
- Permitir `Log.e` directo solo como fallback tecnico si no se puede reportar por el canal normal.

## UI requerida

La pantalla debe incluir:

- Encabezado con nombre tematico y concepto tecnico.
- Explicacion corta: un mutex permite un solo acceso; un semaforo permite una cantidad configurable de accesos simultaneos.
- Selector de espacios disponibles.
- Selector de cantidad de vehiculos.
- Boton `Iniciar`.
- Boton `Cancelar` cuando aplique.
- Boton `Reset`.
- Visualizacion de espacios del estacionamiento.
- Grilla de vehiculos con estado actual.
- Metricas:
  - permisos totales;
  - permisos disponibles;
  - recursos ocupados;
  - hilos esperando;
  - vehiculos finalizados;
  - maximo simultaneo observado;
  - duracion total.
- Resultado de ultima ejecucion.
- Panel de eventos.
- Seccion `Como verificar` con comandos copiables si el componente comun ya existe.

Usar Material 3 y el tema existente. No crear sistema visual paralelo.

## Textos en `strings.xml`

Agregar, como minimo, textos para:

- `smart_parking_titulo`
- `smart_parking_concepto`
- `smart_parking_descripcion`
- `smart_parking_explicacion_mutex_semaforo`
- `smart_parking_espacios`
- `smart_parking_vehiculos`
- `smart_parking_iniciar`
- `smart_parking_cancelar`
- `smart_parking_reset`
- `smart_parking_estado_inactivo`
- `smart_parking_estado_ejecutando`
- `smart_parking_estado_exitoso`
- `smart_parking_estado_cancelado`
- `smart_parking_estado_error`
- `smart_parking_vehiculo_esperando`
- `smart_parking_vehiculo_entrando`
- `smart_parking_vehiculo_estacionado`
- `smart_parking_vehiculo_saliendo`
- `smart_parking_vehiculo_finalizado`
- `smart_parking_metricas`
- `smart_parking_eventos`
- `smart_parking_como_verificar`
- `smart_parking_dialogo_salir_titulo`
- `smart_parking_dialogo_salir_mensaje`
- `smart_parking_dialogo_salir_confirmar`
- `smart_parking_dialogo_salir_cancelar`
- `smart_parking_error_generico`

Usar estos nombres exactos salvo que ya exista una clave identica con el mismo significado.

## Como verificar

Estos comandos son para verificacion manual desde Windows/PowerShell. No ejecutarlos automaticamente durante la implementacion de este plan.

```powershell
.\gradlew.bat build
```

Valida que el proyecto compile. Ejecutarlo solo si el usuario lo aprueba.

```powershell
adb logcat -d -s OSPlayground/Semaphore
```

Valida que el experimento registre eventos reales del semaforo. Deben verse eventos de inicio, espera, `acquire`, `release`, finalizacion, cancelacion o error segun la prueba realizada.

```powershell
adb shell top
```

Permite observar actividad general del proceso durante la ejecucion. Es una verificacion complementaria; no demuestra por si sola que el semaforo limite accesos.

Verificacion principal en UI:

- Ejecutar con 3 espacios y 8 vehiculos.
- Confirmar que nunca hay mas de 3 vehiculos en `Estacionado`.
- Confirmar que algunos vehiculos quedan en `Esperando` mientras los 3 permisos estan ocupados.
- Confirmar que al salir un vehiculo se libera un permiso y entra otro.
- Confirmar que `maximoVehiculosSimultaneos <= permisosTotales`.

## Criterios de aceptacion

- [ ] El modulo usa `java.util.concurrent.Semaphore` real.
- [ ] Cada vehiculo se ejecuta como tarea/hilo real fuera del Main Thread.
- [ ] Nunca hay mas vehiculos estacionados que permisos configurados.
- [ ] Los vehiculos esperan cuando no hay permisos disponibles.
- [ ] Los permisos se liberan correctamente al finalizar, cancelar o fallar.
- [ ] `maximoVehiculosSimultaneos` nunca supera `permisosTotales`.
- [ ] La UI permanece responsiva durante la ejecucion.
- [ ] Los controles se habilitan y deshabilitan segun la tabla de estados.
- [ ] `Cancelar` detiene tareas activas y no deja recursos corriendo.
- [ ] Salir con ejecucion activa muestra `AlertDialog` y limpia antes de navegar si se confirma.
- [ ] Boton superior, Back del sistema y gesto atras siguen la misma regla.
- [ ] `Reset` no se ejecuta sobre una corrida activa.
- [ ] Una nueva ejecucion inicia con registro de eventos limpio.
- [ ] No existe boton manual para limpiar registros.
- [ ] Los textos visibles nuevos estan en `strings.xml`.
- [ ] Las previews no inician hilos reales.
- [ ] No se agregan dependencias nuevas.
- [ ] La diferencia conceptual entre mutex y semaforo queda visible en la pantalla.
- [ ] La seccion `Como verificar` explica que valida cada comando.

## Fuera de alcance

- No implementar navegacion nueva si el proyecto ya tiene una forma aprobada de abrir pantallas.
- No agregar persistencia de resultados.
- No agregar historial global de dashboard.
- No agregar servicios Android.
- No agregar permisos al manifest.
- No agregar librerias de graficas, formularios, logging o testing.
- No ejecutar pruebas ni build sin aprobacion.

## Bloqueos, contradicciones y ambiguedades restantes

### 1. Punto: mecanismo exacto para crear las tareas de vehiculos

Por que bloquea o puede causar implementaciones distintas:

El plan exige tareas/hilos reales y permite usar APIs estandar, pero no define si cada vehiculo debe ejecutarse con `Thread`, `ExecutorService` o coroutines. Las tres opciones pueden cumplir el objetivo, pero producen codigo, cancelacion y limpieza distintos.

Solucion concreta alineada con `AGENTS.md`, `lessons.md` y `docs/`:

Definir `ExecutorService` con pool fijo de tamano `vehiculos` dentro de `EjecutorEstacionamiento`, creando una tarea por vehiculo. Es parte de Java/Kotlin estandar en Android, no agrega dependencias, permite concurrencia real, cancelacion clara con `Future.cancel(true)` y cierre con `shutdownNow()`.

> Sugerencia aprobada

### 2. Punto: duraciones exactas de entrada, estacionamiento y salida

Por que bloquea o puede causar implementaciones distintas:

El plan pide una demo de 30 a 90 segundos y estados visibles, pero no fija tiempos. Un agente podria crear una corrida demasiado rapida para exponer o demasiado lenta para la clase.

Solucion concreta alineada con `AGENTS.md`, `lessons.md` y `docs/`:

Usar tiempos constantes y simples: `300 ms` para `Entrando`, `1200 ms` para `Estacionado` y `300 ms` para `Saliendo`. Con 8 vehiculos y 3 espacios, la demo termina rapido y permite observar espera, acquire y release.

> Sugerencia aprobada

### 3. Punto: componente comun exacto para eventos y comandos copiables

Por que bloquea o puede causar implementaciones distintas:

El plan depende de planes previos para componentes comunes, pero no confirma los nombres reales disponibles al momento de implementar. Un agente podria duplicar paneles o crear un logger paralelo.

Solucion concreta alineada con `AGENTS.md`, `lessons.md` y `docs/`:

Antes de implementar, revisar los componentes comunes existentes. Si existe el panel/logger comun aprobado, usarlo. Si no existe, mantener el registro local minimo dentro de `PantallaSmartParking` y `SmartParkingViewModel`, con Logcat por tag `OSPlayground/Semaphore`, sin crear infraestructura comun nueva en este plan.

> Sugerencia aprobada

### 4. Punto: ruta exacta de navegacion en `App.kt`

Por que bloquea o puede causar implementaciones distintas:

El plan indica modificar `App.kt`, pero la forma concreta de navegacion depende de lo que haya quedado implementado en planes anteriores. Un agente podria inventar rutas o agregar Navigation Compose si no existe.

Solucion concreta alineada con `AGENTS.md`, `lessons.md` y `docs/`:

Usar exclusivamente el mecanismo de seleccion de pantallas ya existente en `App.kt`. Si no hay Navigation Compose instalada, no agregarla. La entrada del dashboard debe abrir `PantallaSmartParking` siguiendo el patron local del proyecto.

> Sugerencia aprobada