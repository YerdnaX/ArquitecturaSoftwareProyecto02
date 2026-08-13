Usa este prompt con otros agentes:
Lee `android/AGENTS.md`, luego `plans/lessons.md`, luego `docs/app-requirements.md`.

Después revisa el plan `<RUTA_DEL_PLAN>`.

No implementes nada todavía.

Objetivo:
Verificar si el plan está suficientemente claro para que un agente pueda implementarlo sin asumir decisiones no documentadas.

Proceso:

1. Revisa el plan completo contra:
   - `android/AGENTS.md`
   - `plans/lessons.md`
   - `docs/app-requirements.md`
   - la estructura real actual del proyecto Android dentro de `android/app/src/main/java/io/yerdna/architecturasos/`
   - los componentes comunes ya existentes en el código.

2. Identifica:
   - contradicciones internas;
   - decisiones abiertas;
   - rutas de archivos que no sigan la estructura real;
   - dependencias nuevas no aprobadas;
   - textos visibles que no estén destinados a `strings.xml`;
   - uso incorrecto de logger, ViewModel, Context o recursos Android;
   - problemas de ciclo de vida, limpieza, cancelación o navegación;
   - comandos de verificación que no sean claros o no sean compatibles con Windows/PowerShell;
   - cualquier punto que pueda hacer que dos agentes implementen soluciones distintas.

3. Si encuentras ajustes directos que ya están cerrados por `AGENTS.md`, `lessons.md`, `docs/` o el código existente, actualiza el plan directamente.
   No preguntes por decisiones que ya estén definidas.

4. Mantén los cambios enfocados solo en `<RUTA_DEL_PLAN>`.
   No modifiques otros planes.
   No implementes código Kotlin.
   No agregues dependencias.
   No ejecutes `.\gradlew.bat build` automáticamente.

5. Cuando actualices el plan:
   - convierte decisiones aprobadas en contrato ejecutable;
   - define archivos exactos a crear/modificar;
   - define estados, métricas, responsabilidades, controles por estado, ciclo de vida, limpieza, verificación y criterios de aceptación;
   - elimina frases abiertas como “si existe”, “preferiblemente”, “usar X o Y” cuando el código o documentos ya cierren la decisión;
   - usa nombres propios en español para código del proyecto;
   - mantén APIs oficiales de Android/Kotlin en inglés;
   - deja explícito cómo usar componentes comunes existentes;
   - deja explícito que textos visibles van en `strings.xml`.

6. Al final del plan, reemplaza cualquier sección de dudas por:
   - `## Decisiones resueltas`, si hubo decisiones cerradas durante la revisión;
   - `## Bloqueos restantes`, indicando claramente si no queda ninguno.

7. Al responder, dime:
   - qué archivo actualizaste;
   - qué decisiones cerraste;
   - si el plan queda listo para implementar;
   - si queda algún bloqueo real que requiera respuesta del usuario.

Plan a revisar:
`<RUTA_DEL_PLAN>`