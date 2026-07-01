# 10 — Bitácora de decisiones

---
**Versión de contexto:** 0.1  
**Commit base:** 4f4bc84  
**Fecha de generación:** 2026-06-30  
**Uso previsto:** contexto para agentes IA externos  
**Datos sensibles:** no incluidos intencionalmente  
---

## D001: Kotlin Multiplatform + Compose como stack

| Campo | Valor |
|-------|-------|
| Decisión | Elegir Kotlin Multiplatform con Compose Multiplatform para la UI |
| Razón | Compartir código entre Android, Desktop (JVM) e iOS desde un solo código base |
| Impacto | Curva de aprendizaje inicial, pero reduce código duplicado a largo plazo |
| Archivos | `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml` |
| Estado | ✅ Vigente |

---

## D002: Datos en memoria (Mock)

| Campo | Valor |
|-------|-------|
| Decisión | Usar singletons en memoria (`MockSaseData`, `MockPreApplicationData`, etc.) en lugar de base de datos |
| Razón | Prototipado rápido sin infraestructura externa. Permite demostrar flujo completo |
| Impacto | Todos los datos se pierden al reiniciar. No hay persistencia real |
| Archivos | `MockSaseData.kt`, `MockPreApplicationData.kt`, `MockOfficialStudentData.kt`, `MockEnrollmentData.kt` |
| Estado | ✅ Vigente |

---

## D003: Sin PDF ni impresión en fase inicial

| Campo | Valor |
|-------|-------|
| Decisión | No implementar exportación PDF ni impresión de credenciales |
| Razón | El alcance es demostrar flujo institucional y vista previa visual. PDF/impresión requieren librerías adicionales y pruebas |
| Impacto | Dirección no puede obtener la credencial física desde el sistema aún |
| Archivos | `CredentialPreviewScreen.kt` (nota de fase futura) |
| Estado | ✅ Vigente |

---

## D004: `app/` como módulo muerto (no migrar)

| Campo | Valor |
|-------|-------|
| Decisión | No incluir el directorio `app/` en el build; mantenerlo como referencia histórica |
| Razón | `app/` contiene código legacy con dependencias no disponibles en el version catalog (KSP, Roborazzi, Firebase). Migrarlo requeriría actualización completa |
| Impacto | No confundir `app/` con código activo. Todo el desarrollo está en `:composeApp` |
| Archivos | `settings.gradle.kts` (solo incluye `:composeApp`) |
| Estado | ✅ Vigente |

---

## D005: Proyección `StudentCredentialPreview` separada de `Student`

| Campo | Valor |
|-------|-------|
| Decisión | Crear una `data class` separada para la proyección de credencial, no reusar `Student` directamente |
| Razón | La credencial solo debe exponer datos institucionales seguros. Separar previene filtrar datos sensibles (domicilio, teléfono, médicos) accidentalmente |
| Impacto | Mantenimiento adicional (mapeo manual) pero seguridad por diseño |
| Archivos | `StudentCredentialPreview.kt` |
| Estado | ✅ Vigente |

---

## D006: Flujo de Pre-solicitud → Alta oficial como vía principal

| Campo | Valor |
|-------|-------|
| Decisión | El flujo principal de incorporación de alumnos es: Portal Familia → Secretaría revisa → Alta oficial → Expediente → Credencial |
| Razón | Refleja el proceso institucional real de la escuela |
| Impacto | Caminos alternativos (inscripción directa, fast track) son secundarios |
| Archivos | `PreApplicationModels.kt`, `OfficialStudentModels.kt`, `SaseEntities.kt` |
| Estado | ✅ Vigente |

---

## D007: `PreApplicationViewModel` como ViewModel compartido con estado estático

| Campo | Valor |
|-------|-------|
| Decisión | Usar `companion object` con `MutableStateFlow` estático para el estado de pre-solicitudes y alumnos oficiales |
| Razón | No hay DI ni framework de navegación que permita compartir ViewModel entre pantallas fácilmente |
| Impacto | El estado es global y persistente en memoria. Los tests deben resetear el estado manualmente |
| Archivos | `PreApplicationViewModel.kt` |
| Riesgo | El estado estático puede causar efectos de borde entre tests si no se resetea |
| Estado | ✅ Vigente |

---

## D008: Sin DI framework

| Campo | Valor |
|-------|-------|
| Decisión | No usar Hilt, Koin, ni ningún framework de inyección de dependencias |
| Razón | Prototipo pequeño con pocas dependencias. Los repositorios se inyectan manualmente en el constructor de `LabViewModel` |
| Impacto | Escalabilidad limitada. Si el proyecto crece, se necesitará DI |
| Estado | ✅ Vigente (podría cambiar) |

---

## D009: Gemini API para imagen mock (no funcional)

| Campo | Valor |
|-------|-------|
| Decisión | Integrar Gemini API (modelo `gemini-3-pro-image-preview`) para generación de imagen en flujo de credencial |
| Razón | Explorar generación de imagen como placeholder de foto de alumno |
| Impacto | Solo funciona en Desktop/Android con API key. iOS retorna `""`. No es funcional para producción |
| Archivos | `GeminiImageGenerator.kt` |
| Estado | ⏳ Placeholder — No integrado al flujo principal |

---

## D010: Toggle Frente/Reverso en credencial (no lado a lado)

| Campo | Valor |
|-------|-------|
| Decisión | Usar selector visual tipo pill (Frente/Reverso) en lugar de mostrar ambas caras lado a lado |
| Razón | Más simple y estable en Compose Desktop. Evita problemas de layout con dos tarjetas |
| Impacto | Usuario debe hacer clic para ver el reverso, pero el diseño es más limpio |
| Archivos | `CredentialPreviewScreen.kt` |
| Estado | ✅ Vigente |

---

## D011: Opción B para placeholders PDF/impresión

| Campo | Valor |
|-------|-------|
| Decisión | Reemplazar el botón "Imprimir" deshabilitado con una nota informativa (Opción B) en lugar de eliminarlo completamente (Opción A) |
| Razón | Ayuda a Dirección a entender la ruta de desarrollo y que la funcionalidad está planificada pero no disponible |
| Impacto | UI más limpio sin botones rotos, pero con expectativas claras |
| Archivos | `CredentialPreviewScreen.kt` |
| Estado | ✅ Vigente |

---

## D012: Sin Supabase / backend en fase actual

| Campo | Valor |
|-------|-------|
| Decisión | No conectar Supabase, Firebase ni ningún backend |
| Razón | El proyecto es un prototipo funcional. Backend agregaría complejidad innecesaria en esta fase |
| Impacto | Datos no persistentes entre sesiones |
| Estado | ✅ Vigente |

---

## Resumen de estados

| Estado | Count | Decisiones |
|--------|-------|------------|
| ✅ Vigente | 11 | D001-D008, D010-D012 |
| ⏳ Pendiente | 0 | — |
| 🔄 Podría cambiar | 1 | D008 (sin DI framework) |
| ❌ Descartada | 0 | — |
