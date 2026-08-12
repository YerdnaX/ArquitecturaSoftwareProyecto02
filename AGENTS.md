# AGENTS.md

Este archivo contiene las reglas de trabajo para agentes de IA que ayuden a desarrollar este proyecto.

El proyecto es una aplicacion Android nativa creada con Kotlin y Jetpack Compose. Esta pensado como un proyecto academico y de aprendizaje, por lo que el codigo debe ser simple, claro y facil de entender para alguien que esta comenzando con Android.

No sobreingenierizar. No crear arquitectura compleja para problemas que el proyecto todavia no tiene.

---

## 1. Principios generales

Al trabajar en este proyecto, priorizar siempre:

1. Simplicidad.
2. Claridad.
3. Codigo facil de leer.
4. Archivos pequenos.
5. Nombres descriptivos.
6. Funciones y composables con una responsabilidad clara.
7. Soluciones practicas antes que patrones avanzados.
8. Codigo adecuado para una persona que esta aprendiendo Android.

Evitar abstracciones prematuras. Si algo solo se usa una vez, puede quedarse local.

---

## 2. Stack del proyecto

El proyecto usa:

```txt
Kotlin
Android nativo
Jetpack Compose
Material 3
AndroidX
Gradle con Kotlin DSL
```

La plataforma objetivo es solamente Android movil.

No agregar dependencias nuevas sin aprobacion explicita. Preferir AndroidX y las librerias oficiales de Android cuando sea posible.

---

## 3. Estructura actual del proyecto

La raiz real del proyecto Android es esta carpeta:

```txt
android/
```

Estructura base:

```txt
android/
|-- app/
|   |-- build.gradle.kts
|   `-- src/
|       |-- main/
|       |   |-- AndroidManifest.xml
|       |   |-- java/io/yerdna/architecturasos/
|       |   `-- res/
|       |-- test/
|       `-- androidTest/
|-- gradle/
|   `-- libs.versions.toml
|-- build.gradle.kts
|-- settings.gradle.kts
|-- gradlew
`-- gradlew.bat
```

El paquete principal es:

```txt
io.yerdna.architecturasos
```

Mantener el codigo Kotlin dentro de:

```txt
app/src/main/java/io/yerdna/architecturasos/
```

No crear carpetas vacias para funcionalidades futuras.

---

## 4. Arquitectura sencilla recomendada

Usar una arquitectura simple, con pocas capas y responsabilidades claras.

Para pantallas pequenas, este flujo es suficiente:

```txt
Pantalla
-> estado local
-> UI
```

Para pantallas con mas logica, usar:

```txt
Pantalla
-> ViewModel
-> Repositorio o Servicio
-> FuenteDatos
```

Significado de cada parte:

```txt
Pantalla: muestra la interfaz y recibe acciones del usuario.
ViewModel: guarda estado de pantalla y coordina acciones.
Repositorio o Servicio: contiene reglas simples de datos o llamadas externas.
FuenteDatos: obtiene datos de una fuente concreta, como memoria, archivo, API o base de datos.
```

No todas las pantallas necesitan todas las capas. Crear un `ViewModel`, `Repositorio`, `Servicio` o `FuenteDatos` solo cuando realmente ayude a entender el codigo.

Ejemplo simple:

```txt
PantallaListaProcesos
-> ListaProcesosViewModel
-> RepositorioProcesos
```

---

## 5. Paquetes recomendados

Dentro del paquete principal, usar una estructura sencilla:

```txt
io.yerdna.architecturasos/
|-- MainActivity.kt
|-- ui/
|   |-- theme/
|   |-- screen/
|   `-- component/
|-- data/
|   |-- model/
|   `-- repository/
`-- util/
```

Responsabilidades:

```txt
ui/theme: tema, colores y tipografias de Compose.
ui/screen: pantallas completas.
ui/component: composables reutilizables.
data/model: clases de datos simples.
data/repository: origen de datos y logica simple de datos.
util: funciones pequenas reutilizables.
```

Si una funcionalidad crece mucho, puede agruparse por feature:

```txt
ui/screen/procesos/
data/repository/procesos/
data/model/Proceso.kt
```

Mantener la estructura simple. No crear paquetes como `domain`, `usecase`, `mapper`, `di` o `core` si no existe una necesidad clara.

---

## 6. MainActivity

`MainActivity` debe ser el punto de entrada de la app.

Debe encargarse principalmente de:

```txt
Configurar el tema.
Llamar al composable raiz de la aplicacion.
Configurar navegacion si existe.
```

Evitar poner toda la UI y toda la logica directamente en `MainActivity`. Si la pantalla crece, moverla a `ui/screen`.

Ejemplo:

```kotlin
setContent {
    ArchitecturasOSTheme {
        AplicacionArquitecturasOS()
    }
}
```

---

## 7. Jetpack Compose

Escribir composables pequenos y declarativos.

Buenas practicas:

```txt
Usar nombres en PascalCase para composables.
Mantener cada composable enfocado en una sola cosa.
Pasar datos por parametros.
Pasar acciones como lambdas.
Evitar logica pesada dentro de composables.
Usar @Preview cuando sea util para revisar UI.
```

Ejemplo:

```kotlin
@Composable
fun TarjetaProceso(
    proceso: Proceso,
    onClick: () -> Unit
) {
    Card(onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = proceso.nombre)
            Text(text = proceso.estado)
        }
    }
}
```

No hacer llamadas de datos directamente desde composables si esa logica empieza a crecer. Moverla a un `ViewModel` o a una funcion separada.

---

## 8. Estado

Usar el estado mas simple posible.

Para estado pequeno de una pantalla:

```kotlin
var texto by remember { mutableStateOf("") }
```

Para estado que debe sobrevivir mejor a cambios de configuracion o coordinar logica:

```txt
ViewModel
```

No agregar librerias de estado global. No son necesarias en este proyecto.

---

## 9. ViewModels

Usar `ViewModel` cuando:

```txt
Una pantalla tenga varios estados.
Exista logica que no deberia vivir en la UI.
Se necesite cargar o transformar datos.
La pantalla empiece a crecer demasiado.
```

Mantener los ViewModels simples.

Ejemplo de responsabilidades:

```txt
Exponer estado para la pantalla.
Actualizar estado cuando el usuario hace una accion.
Llamar a repositorios o servicios si existen.
```

Evitar meter codigo de UI dentro de ViewModels.

---

## 10. Modelos

Usar `data class` para representar datos.

Ejemplo:

```kotlin
data class Proceso(
    val id: Int,
    val nombre: String,
    val estado: String
)
```

Los modelos deben ser simples y faciles de leer.

---

## 11. Repositories y datos

Usar repositories solo cuando haya una fuente de datos real o cuando ayuden a separar la logica.

Ejemplo valido:

```kotlin
class RepositorioProcesos {
    fun obtenerProcesos(): List<Proceso> {
        return listOf(
            Proceso(id = 1, nombre = "Proceso A", estado = "Listo")
        )
    }
}
```

No crear repositories vacios. No crear interfaces si solo existe una implementacion y no hay pruebas que las necesiten.

---

## 12. Navegacion

Si la app tiene una sola pantalla, no agregar navegacion.

Si se necesitan varias pantallas, preferir Navigation Compose, pero agregarla solo con aprobacion si la dependencia aun no existe.

Mantener rutas simples y nombres claros:

```txt
inicio
detalleProceso
configuracion
```

No crear un sistema propio de navegacion.

---

## 13. Recursos Android

Usar `res/` para recursos propios de Android:

```txt
res/drawable: imagenes o drawables.
res/mipmap: iconos de launcher.
res/values: strings, colores y temas XML.
```

Para textos visibles que se repiten o que pertenecen a la app, preferir `strings.xml`.

No hardcodear valores repetidos si ya existe un recurso adecuado.

---

## 14. Idioma y nombres

El proyecto puede usar espanol para codigo propio de la aplicacion:

```txt
Proceso
PantallaListaProcesos
TarjetaProceso
RepositorioProcesos
obtenerProcesos
estadoSeleccionado
```

Mantener en ingles las APIs, clases y convenciones oficiales de Android y Kotlin:

```txt
Activity
ViewModel
Composable
Modifier
State
remember
mutableStateOf
onClick
```

Usar:

```txt
PascalCase para clases y composables.
camelCase para variables y funciones.
UPPER_SNAKE_CASE solo para constantes reales.
```

Evitar abreviaciones innecesarias como `proc`, `cfg`, `tmp` si un nombre claro cabe sin problema.

---

## 15. Estilo visual

Usar Material 3 y el tema existente en:

```txt
ui/theme/
```

No crear un sistema visual paralelo.

Antes de agregar colores, revisar si pueden vivir en `Color.kt` o en el tema.

Mantener la UI simple:

```txt
Scaffold para estructura general.
Column y Row para layouts basicos.
Card para elementos agrupados.
Button para acciones principales.
TextField para formularios.
LazyColumn para listas largas.
```

No agregar librerias visuales nuevas sin aprobacion.

---

## 16. Listas

Usar `LazyColumn` para listas que puedan crecer.

Ejemplo:

```kotlin
LazyColumn {
    items(procesos) { proceso ->
        TarjetaProceso(proceso = proceso)
    }
}
```

Usar `Column` solo para listas pequenas y estaticas.

---

## 17. Formularios

Mantener formularios simples.

Para formularios pequenos, usar estado local:

```kotlin
var nombre by remember { mutableStateOf("") }
```

No agregar librerias de formularios sin aprobacion.

Validar solo lo necesario para el objetivo academico del proyecto.

---

## 18. Pruebas

El proyecto tiene carpetas y dependencias base para pruebas, pero no es obligatorio agregar pruebas automatizadas para cada cambio.

Agregar pruebas solo si:

```txt
El usuario lo pide.
```

No agregar configuraciones complejas de testing sin aprobacion.

---

## 19. Comandos utiles

Desde la carpeta `android/`, usar:

```powershell
.\gradlew.bat build
.\gradlew.bat test
.\gradlew.bat connectedAndroidTest
```

Notas:

```txt
build: compila el proyecto.
test: corre pruebas unitarias locales.
connectedAndroidTest: requiere un emulador o dispositivo conectado.
```

No modificar archivos generados dentro de `build/`, `.gradle/` o salidas del IDE.

---

## 20. Dependencias

No agregar dependencias nuevas sin permiso.

Antes de proponer una dependencia, revisar si el problema puede resolverse con:

```txt
Kotlin
Jetpack Compose
Material 3
AndroidX
APIs estandar de Android
```

Evitar instalar librerias para problemas pequenos.

---

## 21. Que no hacer

Evitar:

```txt
Clean Architecture compleja.
Use cases para todo.
Dependency Injection si no es necesaria.
Interfaces sin motivo.
Mappers innecesarios.
Capas vacias.
Paquetes creados por anticipado.
Librerias nuevas sin aprobacion.
Codigo no usado.
Logica grande dentro de MainActivity.
Logica pesada dentro de composables.
Sistemas propios de navegacion.
```

---

## 22. Reglas para agentes de IA

Cuando un agente trabaje en este proyecto, debe:

1. Mantener cambios pequenos y enfocados.
2. Revisar la estructura existente antes de editar.
3. Usar Kotlin y Jetpack Compose.
4. Seguir el paquete `io.yerdna.architecturasos`.
5. Preferir codigo simple sobre codigo "inteligente".
6. No modificar archivos no relacionados.
7. No agregar dependencias sin permiso.
8. No cambiar la arquitectura sin permiso.
9. No crear carpetas vacias.
10. No tocar archivos generados.
11. Explicar en que archivo va cada cambio cuando proponga codigo.
12. Escribir codigo entendible para alguien que esta aprendiendo Android.

---

## 23. Estilo de respuesta esperado

Cuando el agente explique cambios, debe ser concreto.

Ejemplo:

```txt
Crear este archivo:

app/src/main/java/io/yerdna/architecturasos/ui/screen/PantallaListaProcesos.kt

Agregar aqui la UI principal de la lista.

Luego actualizar:

app/src/main/java/io/yerdna/architecturasos/MainActivity.kt

Para llamar a PantallaListaProcesos dentro del tema de la app.
```

Evitar respuestas vagas como:

```txt
Se recomienda desacoplar la logica y usar una arquitectura escalable.
```

Preferir explicaciones cortas, aplicables y conectadas con archivos reales del proyecto.

---

## 24. Regla final

Este es un proyecto academico y de aprendizaje.

El objetivo no es construir una arquitectura perfecta. El objetivo es construir una aplicacion Android clara, funcional y facil de entender.

Mantener el codigo simple. Mantener el proyecto ordenado. No disenar para problemas que todavia no existen.
