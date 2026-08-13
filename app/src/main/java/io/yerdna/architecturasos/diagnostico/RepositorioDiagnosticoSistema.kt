package io.yerdna.architecturasos.diagnostico

import io.yerdna.architecturasos.R

private const val TAGS_LOGCAT_TODOS =
    "adb logcat -d -s OSPlayground/FabricaRobots OSPlayground/BinderIPC OSPlayground/SocketLab " +
        "OSPlayground/ThreadRace OSPlayground/ChaosBank OSPlayground/Mutex OSPlayground/Semaphore OSPlayground/Memory"

// Devuelve el catalogo completo de herramientas de diagnostico disponibles en la app
fun obtenerHerramientasDiagnostico(): List<HerramientaDiagnostico> {
    return listOf(
        herramientaAndroidProfiler(),
        herramientaAdbTop(),
        herramientaAdbLogcat(),
        herramientaBinderIpc(),
        herramientaSockets(),
        herramientaPerfetto(),
        herramientaDumpsys(),
        herramientaAdb(),
        herramientaDumpstate(),
        herramientaBatteryHistorian()
    )
}

// Describe la herramienta Android Profiler y sus experimentos relacionados
private fun herramientaAndroidProfiler() = HerramientaDiagnostico(
    id = "android_profiler",
    nombreResId = R.string.diagnostico_herramienta_android_profiler_nombre,
    categoria = CategoriaDiagnostico.MEMORIA,
    descripcionResId = R.string.diagnostico_herramienta_android_profiler_descripcion,
    cuandoUsarlaResId = R.string.diagnostico_herramienta_android_profiler_cuando_usarla,
    queValidaResId = R.string.diagnostico_herramienta_android_profiler_que_valida,
    limitacionResId = R.string.diagnostico_herramienta_android_profiler_limitacion,
    experimentosRelacionados = listOf(
        RelacionExperimentoDiagnostico(
            R.string.experimento_carrera_hilos_nombre,
            R.string.diagnostico_mecanismo_carrera_hilos
        ),
        RelacionExperimentoDiagnostico(
            R.string.experimento_monstruo_memoria_nombre,
            R.string.diagnostico_mecanismo_monstruo_memoria
        )
    )
)

// Describe la herramienta adb shell top y sus comandos y experimentos relacionados
private fun herramientaAdbTop() = HerramientaDiagnostico(
    id = "adb_shell_top",
    nombreResId = R.string.diagnostico_herramienta_adb_top_nombre,
    categoria = CategoriaDiagnostico.PROCESOS_CPU,
    descripcionResId = R.string.diagnostico_herramienta_adb_top_descripcion,
    cuandoUsarlaResId = R.string.diagnostico_herramienta_adb_top_cuando_usarla,
    queValidaResId = R.string.diagnostico_herramienta_adb_top_que_valida,
    limitacionResId = R.string.diagnostico_herramienta_adb_top_limitacion,
    comandos = listOf(
        ComandoDiagnostico(
            comando = "adb shell top",
            descripcionResId = R.string.diagnostico_comando_top_descripcion,
            cuandoEjecutarloResId = R.string.diagnostico_comando_top_cuando,
            comoInterpretarloResId = R.string.diagnostico_comando_top_interpretar
        )
    ),
    experimentosRelacionados = listOf(
        RelacionExperimentoDiagnostico(
            R.string.experimento_fabrica_robots_nombre,
            R.string.diagnostico_mecanismo_fabrica_robots
        ),
        RelacionExperimentoDiagnostico(
            R.string.experimento_carrera_hilos_nombre,
            R.string.diagnostico_mecanismo_carrera_hilos
        ),
        RelacionExperimentoDiagnostico(
            R.string.experimento_monstruo_memoria_nombre,
            R.string.diagnostico_mecanismo_monstruo_memoria
        )
    )
)

// Describe la herramienta adb logcat, su comando de filtrado por tags y todos los experimentos que aplica
private fun herramientaAdbLogcat() = HerramientaDiagnostico(
    id = "adb_logcat",
    nombreResId = R.string.diagnostico_herramienta_adb_logcat_nombre,
    categoria = CategoriaDiagnostico.LOGS,
    descripcionResId = R.string.diagnostico_herramienta_adb_logcat_descripcion,
    cuandoUsarlaResId = R.string.diagnostico_herramienta_adb_logcat_cuando_usarla,
    queValidaResId = R.string.diagnostico_herramienta_adb_logcat_que_valida,
    limitacionResId = R.string.diagnostico_herramienta_adb_logcat_limitacion,
    comandos = listOf(
        ComandoDiagnostico(
            comando = TAGS_LOGCAT_TODOS,
            descripcionResId = R.string.diagnostico_comando_logcat_descripcion,
            cuandoEjecutarloResId = R.string.diagnostico_comando_logcat_cuando,
            comoInterpretarloResId = R.string.diagnostico_comando_logcat_interpretar
        )
    ),
    experimentosRelacionados = todosLosExperimentos()
)

// Describe la herramienta de inspeccion de Binder IPC y su experimento relacionado
private fun herramientaBinderIpc() = HerramientaDiagnostico(
    id = "binder_ipc",
    nombreResId = R.string.diagnostico_herramienta_binder_ipc_nombre,
    categoria = CategoriaDiagnostico.IPC,
    descripcionResId = R.string.diagnostico_herramienta_binder_ipc_descripcion,
    cuandoUsarlaResId = R.string.diagnostico_herramienta_binder_ipc_cuando_usarla,
    queValidaResId = R.string.diagnostico_herramienta_binder_ipc_que_valida,
    limitacionResId = R.string.diagnostico_herramienta_binder_ipc_limitacion,
    experimentosRelacionados = listOf(
        RelacionExperimentoDiagnostico(
            R.string.experimento_restaurante_ipc_nombre,
            R.string.diagnostico_mecanismo_restaurante_ipc
        )
    )
)

// Describe la herramienta de inspeccion de sockets y su experimento relacionado
private fun herramientaSockets() = HerramientaDiagnostico(
    id = "sockets",
    nombreResId = R.string.diagnostico_herramienta_sockets_nombre,
    categoria = CategoriaDiagnostico.SOCKETS,
    descripcionResId = R.string.diagnostico_herramienta_sockets_descripcion,
    cuandoUsarlaResId = R.string.diagnostico_herramienta_sockets_cuando_usarla,
    queValidaResId = R.string.diagnostico_herramienta_sockets_que_valida,
    limitacionResId = R.string.diagnostico_herramienta_sockets_limitacion,
    experimentosRelacionados = listOf(
        RelacionExperimentoDiagnostico(
            R.string.experimento_red_agentes_nombre,
            R.string.diagnostico_mecanismo_red_agentes
        )
    )
)

// Describe la herramienta Perfetto para trazas de sistema y su experimento relacionado
private fun herramientaPerfetto() = HerramientaDiagnostico(
    id = "perfetto_system_trace",
    nombreResId = R.string.diagnostico_herramienta_perfetto_nombre,
    categoria = CategoriaDiagnostico.TRACING,
    descripcionResId = R.string.diagnostico_herramienta_perfetto_descripcion,
    cuandoUsarlaResId = R.string.diagnostico_herramienta_perfetto_cuando_usarla,
    queValidaResId = R.string.diagnostico_herramienta_perfetto_que_valida,
    limitacionResId = R.string.diagnostico_herramienta_perfetto_limitacion,
    experimentosRelacionados = listOf(
        RelacionExperimentoDiagnostico(
            R.string.experimento_carrera_hilos_nombre,
            R.string.diagnostico_mecanismo_carrera_hilos
        )
    )
)

// Describe la herramienta dumpsys, sus comandos de memoria y servicios y los experimentos relacionados
private fun herramientaDumpsys() = HerramientaDiagnostico(
    id = "dumpsys",
    nombreResId = R.string.diagnostico_herramienta_dumpsys_nombre,
    categoria = CategoriaDiagnostico.SERVICIOS,
    descripcionResId = R.string.diagnostico_herramienta_dumpsys_descripcion,
    cuandoUsarlaResId = R.string.diagnostico_herramienta_dumpsys_cuando_usarla,
    queValidaResId = R.string.diagnostico_herramienta_dumpsys_que_valida,
    limitacionResId = R.string.diagnostico_herramienta_dumpsys_limitacion,
    comandos = listOf(
        ComandoDiagnostico(
            comando = "adb shell dumpsys meminfo io.yerdna.architecturasos",
            descripcionResId = R.string.diagnostico_comando_meminfo_descripcion,
            cuandoEjecutarloResId = R.string.diagnostico_comando_meminfo_cuando,
            comoInterpretarloResId = R.string.diagnostico_comando_meminfo_interpretar
        ),
        ComandoDiagnostico(
            comando = "adb shell dumpsys activity services",
            descripcionResId = R.string.diagnostico_comando_services_descripcion,
            cuandoEjecutarloResId = R.string.diagnostico_comando_services_cuando,
            comoInterpretarloResId = R.string.diagnostico_comando_services_interpretar
        ),
        ComandoDiagnostico(
            comando = "adb shell dumpsys activity services | findstr architecturasos",
            descripcionResId = R.string.diagnostico_comando_services_filtrado_descripcion,
            cuandoEjecutarloResId = R.string.diagnostico_comando_services_filtrado_cuando,
            comoInterpretarloResId = R.string.diagnostico_comando_services_filtrado_interpretar
        )
    ),
    experimentosRelacionados = listOf(
        RelacionExperimentoDiagnostico(
            R.string.experimento_fabrica_robots_nombre,
            R.string.diagnostico_mecanismo_fabrica_robots
        ),
        RelacionExperimentoDiagnostico(
            R.string.experimento_restaurante_ipc_nombre,
            R.string.diagnostico_mecanismo_restaurante_ipc
        ),
        RelacionExperimentoDiagnostico(
            R.string.experimento_monstruo_memoria_nombre,
            R.string.diagnostico_mecanismo_monstruo_memoria
        )
    )
)

// Describe la herramienta adb general, sus comandos basicos y todos los experimentos que aplica
private fun herramientaAdb() = HerramientaDiagnostico(
    id = "adb",
    nombreResId = R.string.diagnostico_herramienta_adb_nombre,
    categoria = CategoriaDiagnostico.REPORTE_SISTEMA,
    descripcionResId = R.string.diagnostico_herramienta_adb_descripcion,
    cuandoUsarlaResId = R.string.diagnostico_herramienta_adb_cuando_usarla,
    queValidaResId = R.string.diagnostico_herramienta_adb_que_valida,
    limitacionResId = R.string.diagnostico_herramienta_adb_limitacion,
    comandos = listOf(
        ComandoDiagnostico(
            comando = "adb devices",
            descripcionResId = R.string.diagnostico_comando_devices_descripcion,
            cuandoEjecutarloResId = R.string.diagnostico_comando_devices_cuando,
            comoInterpretarloResId = R.string.diagnostico_comando_devices_interpretar
        ),
        ComandoDiagnostico(
            comando = "adb shell ps -A",
            descripcionResId = R.string.diagnostico_comando_ps_descripcion,
            cuandoEjecutarloResId = R.string.diagnostico_comando_ps_cuando,
            comoInterpretarloResId = R.string.diagnostico_comando_ps_interpretar
        ),
        ComandoDiagnostico(
            comando = "adb shell ps -A | findstr architecturasos",
            descripcionResId = R.string.diagnostico_comando_ps_filtrado_descripcion,
            cuandoEjecutarloResId = R.string.diagnostico_comando_ps_filtrado_cuando,
            comoInterpretarloResId = R.string.diagnostico_comando_ps_filtrado_interpretar
        )
    ),
    experimentosRelacionados = todosLosExperimentos()
)

// Describe la herramienta dumpstate para generar reportes completos del sistema
private fun herramientaDumpstate() = HerramientaDiagnostico(
    id = "dumpstate",
    nombreResId = R.string.diagnostico_herramienta_dumpstate_nombre,
    categoria = CategoriaDiagnostico.REPORTE_SISTEMA,
    descripcionResId = R.string.diagnostico_herramienta_dumpstate_descripcion,
    cuandoUsarlaResId = R.string.diagnostico_herramienta_dumpstate_cuando_usarla,
    queValidaResId = R.string.diagnostico_herramienta_dumpstate_que_valida,
    limitacionResId = R.string.diagnostico_herramienta_dumpstate_limitacion
)

// Describe la herramienta Battery Historian y los experimentos relacionados con consumo de energia
private fun herramientaBatteryHistorian() = HerramientaDiagnostico(
    id = "battery_historian",
    nombreResId = R.string.diagnostico_herramienta_battery_historian_nombre,
    categoria = CategoriaDiagnostico.ENERGIA,
    descripcionResId = R.string.diagnostico_herramienta_battery_historian_descripcion,
    cuandoUsarlaResId = R.string.diagnostico_herramienta_battery_historian_cuando_usarla,
    queValidaResId = R.string.diagnostico_herramienta_battery_historian_que_valida,
    limitacionResId = R.string.diagnostico_herramienta_battery_historian_limitacion,
    experimentosRelacionados = listOf(
        RelacionExperimentoDiagnostico(
            R.string.experimento_carrera_hilos_nombre,
            R.string.diagnostico_mecanismo_carrera_hilos
        ),
        RelacionExperimentoDiagnostico(
            R.string.experimento_monstruo_memoria_nombre,
            R.string.diagnostico_mecanismo_monstruo_memoria
        )
    )
)

// Devuelve la lista completa de experimentos de la app relacionados con una herramienta generica
private fun todosLosExperimentos(): List<RelacionExperimentoDiagnostico> {
    return listOf(
        RelacionExperimentoDiagnostico(
            R.string.experimento_fabrica_robots_nombre,
            R.string.diagnostico_mecanismo_fabrica_robots
        ),
        RelacionExperimentoDiagnostico(
            R.string.experimento_restaurante_ipc_nombre,
            R.string.diagnostico_mecanismo_restaurante_ipc
        ),
        RelacionExperimentoDiagnostico(
            R.string.experimento_red_agentes_nombre,
            R.string.diagnostico_mecanismo_red_agentes
        ),
        RelacionExperimentoDiagnostico(
            R.string.experimento_carrera_hilos_nombre,
            R.string.diagnostico_mecanismo_carrera_hilos
        ),
        RelacionExperimentoDiagnostico(
            R.string.experimento_banco_caotico_nombre,
            R.string.diagnostico_mecanismo_banco_caotico
        ),
        RelacionExperimentoDiagnostico(
            R.string.experimento_carrera_boletos_nombre,
            R.string.diagnostico_mecanismo_carrera_boletos
        ),
        RelacionExperimentoDiagnostico(
            R.string.experimento_parqueo_inteligente_nombre,
            R.string.diagnostico_mecanismo_parqueo_inteligente
        ),
        RelacionExperimentoDiagnostico(
            R.string.experimento_monstruo_memoria_nombre,
            R.string.diagnostico_mecanismo_monstruo_memoria
        )
    )
}
