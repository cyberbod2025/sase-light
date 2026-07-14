# CHECKPOINT — Cierre 01C, 01D, 01F

## Alcance 01C — CURP Duplicate Detection

- `PreApplicationViewModel.curpDuplicateInfo()` — detecta CURP duplicada en padrón maestro y altas oficiales
- `SecretariaPreApplicationDashboardScreen.kt` — bloquea acciones de alta oficial cuando CURP está duplicada
- Muestra tarjeta roja "CURP ya registrada" con opciones: "Abrir expediente existente" / "Corregir CURP"
- Los botones de alta oficial se ocultan mientras exista duplicado
- `fieldColors` unificados para campos `OutlinedTextField` en diálogo de edición

## Alcance 01D — Smart Enrollment Semantics & Navigation

- `SmartEnrollmentTable.kt` — agregados `contentDescription` y `testTag` en todos los elementos interactivos:
  - `student_card_X` — tarjeta de alumno
  - `student_chevron_X` — icono de navegación (contentDescription = "Ver expediente")
  - `student_menu_X` — botón de menú contextual
  - `student_status_chip_X` — chip de estatus
  - `student_docs_chip_X` — chip de documentación
- Callbacks `onDocChipClick` y `onStatusChipClick` expuestos desde `SecretaryDashboardScreen`
- DropdownMenu con items condicionales por estado del alumno:
  - "Abrir expediente" (siempre)
  - "Editar observaciones" (siempre)
  - "Ver documentos pendientes" (solo si documentación incompleta)
  - "Procesar alta oficial" (solo sin inscripción oficial)
  - "Ver credencial" (solo con inscripción oficial)
- Icono `KeyboardArrowRight` reemplaza `MoreVert` en layout compacto para chevron

## Infraestructura 01F — Android Test Tooling

- `tools/android/lib/config.ps1` — configuración compartida (SDK, ADB, emulador, APK)
- `tools/android/build-debug.ps1` — build APK debug
- `tools/android/doctor.ps1` — diagnóstico de entorno (PATH/JAVA_HOME/Gradle)
- `tools/android/install-app.ps1` — instalación en dispositivo
- `tools/android/launch-app.ps1` — lanzamiento de la app
- `tools/android/interact.ps1` — interacción por ADB (tap, dump, filtros)
- `tools/android/collect-state.ps1` — captura de estado completo
- `tools/android/record-screen.ps1` — grabación de pantalla
- `tools/android/start-emulator.ps1` — inicio de emulador
- `tools/android/stop-emulator.ps1` — parada de emulador
- Todos los scripts soportan parámetro `-Serial` para selección de dispositivo

## Dispositivos Utilizados

| Dispositivo | Serial | SO | API |
|------------|--------|----|-----|
| Samsung S20 FE (físico) | RFCT419Q0YE | Android 13 | 33 |
| Pixel 7 (emulador) | emulator-5554 | Android 14 | 35 |

## Matriz de Validación (7/7)

| # | Escenario | Resultado | Dispositivo |
|---|-----------|-----------|-------------|
| A | Body tap → StudentRecord | **PASS** | Ambos |
| B | Chevron tap → StudentRecord | **PASS** | S20 FE |
| C | Menú visible con items | **PASS** | S20 FE |
| D | Cerrar menú tocando fuera | **PASS** | S20 FE |
| E | Docs chip → StudentRecord | **PASS** | S20 FE |
| F | Status chip → StudentRecord | **PASS** | S20 FE |
| G | Menú diferente por estado | **PASS** | S20 FE |

## Rutas de Evidencia Seleccionada

```
evidence/android/01D/20260714-113801/s20-fe/
├── results.json           — matriz completa de resultados
├── metadata.json          — metadatos de sesión
├── device-info.txt        — información del dispositivo
├── scenario-b-chevron-tap.png    — Escenario B post-navegación
├── scenario-d-menu-closed.png    — Escenario D menú cerrado
├── scenario-e-docs-chip.png      — Escenario E docs chip
├── scenario-f-status-chip.png    — Escenario F status chip
└── scenario-g-menu-card4.png     — Escenario G menú card 4
```

## Regresión

- `compileKotlinDesktop` — BUILD SUCCESSFUL
- `desktopTest --rerun-tasks` — BUILD SUCCESSFUL
- `assembleDebug` — BUILD SUCCESSFUL
- `git diff --check` — sin errores de espacios en blanco

## Limitaciones Conocidas

1. **uiautomator en Pixel 7**: No captura texto de DropdownMenu (NAF nodes). El menú se abre visualmente pero el texto no es extraíble.
2. **`student_chevron_X` semántica**: El icono `KeyboardArrowRight` sobrescribe el `contentDescription` — el identificador accesible es "Ver expediente" en lugar de `student_chevron_X`.
3. **Sin deep links**: No hay rutas URL schemes para navegación directa a StudentRecord.
4. **Datos mock**: Toda la información es in-memory (`MockSaseData`). No hay backend ni base de datos.
5. **Sin pruebas automatizadas**: Los test suites no están conectados al build activo (pertenecen al módulo `app/` stale).

## Deuda Técnica — Deep Links

- No existe infraestructura de deep linking para acceder directamente a StudentRecord(id) desde fuera de la app.
- Los callbacks `onDocChipClick` y `onStatusChipClick` navegan a `StudentRecord(student.id)` — consistentes con `onStudentClick`.
- El menú "Procesar alta oficial" y "Ver documentos pendientes" también usan `onDocChipClick` como acción provisional.

## Estado Git Previo al Commit

```
Staged:
  M composeApp/.../ui/SaseScreens.kt
  M composeApp/.../ui/enrollment/SmartEnrollmentTable.kt
  M composeApp/.../ui/presolicitud/SecretariaPreApplicationDashboardScreen.kt
  M composeApp/.../viewmodel/PreApplicationViewModel.kt
  ?? tools/android/*.ps1               (9 scripts)
  ?? tools/android/lib/config.ps1
  ?? .gitignore
  ?? CHECKPOINT-CIERRE-01C-01D-01F.md
  ?? evidence/android/01D/20260714-113801/s20-fe/results.json
  ?? evidence/android/01D/20260714-113801/s20-fe/metadata.json
  ?? evidence/android/01D/20260714-113801/s20-fe/device-info.txt
  ?? evidence/android/01D/20260714-113801/s20-fe/scenario-b-chevron-tap.png
  ?? evidence/android/01D/20260714-113801/s20-fe/scenario-d-menu-closed.png
  ?? evidence/android/01D/20260714-113801/s20-fe/scenario-e-docs-chip.png
  ?? evidence/android/01D/20260714-113801/s20-fe/scenario-f-status-chip.png
  ?? evidence/android/01D/20260714-113801/s20-fe/scenario-g-menu-card4.png

Excluded (never stage):
  REPORTE-DE-REINDUCCION-Y-EXPLICACION-L5B.md    — protegido, gitignored
  evidence/android/**/*.xml                       — jerarquías redundantes
  evidence/android/**/screenrecord.mp4            — video pesado
  evidence/android/**/logcat.txt                  — logs crudos
  evidence/android/**/screenshot-before.png       — captura intermedia
  evidence/android/**/emulator/                   — evidencia de emulador
  evidence/android/01D/20260714-112820/            — sesión anterior
  evidence/android/20260714-110909/                — sesión anterior
  evidence/android/20260714-110924/                — sesión anterior
  evidence/android/01F-logs/                       — logs de build
  evidence/android/boot-failure*.log               — logs de arranque
  evidence/android/emulator-startup.log            — logs de emulador
  evidence/android/launch-dumpsys.log              — dump técnico
  composeApp/build/                                 — build outputs
  app/                                              — módulo stale
```

---

*Checkpoint preparado. Sin commit. Sin push. Esperando autorización.*
