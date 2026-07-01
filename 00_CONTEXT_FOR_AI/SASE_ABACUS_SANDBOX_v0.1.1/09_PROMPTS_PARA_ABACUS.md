# 09 — Prompts seguros para Abacus AI

---
**Versión de contexto:** 0.1  
**Commit base:** 4f4bc84  
**Fecha de generación:** 2026-06-30  
**Uso previsto:** contexto para agentes IA externos  
**Datos sensibles:** no incluidos intencionalmente  
---

> **Importante**: Estos prompts están diseñados para ser usados con un agente externo (Abacus AI) que NO tiene acceso al repositorio. Antes de usarlos, adjuntar los archivos relevantes de `SASE_ABACUS_SANDBOX/` y los archivos fuente necesarios.

---

## Prompt 1: Auditoría de código sin modificar

```
Antes de responder, identifica qué información proviene del contexto proporcionado y qué información no puedes confirmar. No inventes archivos, modelos, tablas, campos ni flujos.

Actúa como auditor de código Kotlin Multiplatform / Compose.

Revisa el siguiente archivo fuente de SASE Light [adjuntar archivo].
Tu tarea es:

1. Identificar posibles problemas de calidad:
   - Código muerto
   - imports no utilizados
   - lógica redundante
   - names inconsistentes
   - violaciones de convenciones Kotlin
2. Identificar riesgos:
   - Exposición de datos sensibles
   - Dependencias frágiles
   - Acoplamiento excesivo
3. Evaluar cobertura de casos borde

PROHIBIDO:
- Modificar el código
- Sugerir cambios que requieran nuevos modelos
- Sugerir conexión a base de datos
- Sugerir implementación de PDF/impresión
- Inventar funcionalidades no existentes

Formato de salida:
- Lista de hallazgos ordenados por severidad (Alta/Media/Baja)
- Cada hallazgo con: archivo, línea, descripción, riesgo
- Sin código nuevo
```

---

## Prompt 2: Creación de componente aislado

```
Antes de responder, identifica qué información proviene del contexto proporcionado y qué información no puedes confirmar. No inventes archivos, modelos, tablas, campos ni flujos.

Actúa como desarrollador Kotlin Multiplatform / Compose.

Necesito crear un nuevo componente de UI para SASE Light.
Contexto del proyecto: [extraer de 01_CONTEXTO_GENERAL_SASE_LIGHT.md]

Especificación:
[describir el componente: propósito, datos que consume, estado visual]

Reglas obligatorias:
- Solo crear el archivo del componente
- No modificar archivos existentes
- No crear nuevos modelos de datos
- No conectar a base de datos
- No implementar PDF/impresión
- Usar el sistema de colores existente (SaseNavy, SaseGreen, etc.)
- Seguir el patrón de los componentes existentes (GlassCard, etc.)
- Composable debe ser @Composable y recibir datos por parámetro
- No usar ViewModel directamente (pasar datos ya resueltos)

Limitaciones:
- No hay autenticación real
- Los datos disponibles al momento del commit base son mock o simulados; verificar estado actual antes de usar
- No hay backend
- No hay storage

Proveer:
- Código completo del componente
- Breve nota de integración (dónde y cómo usarlo)
```

---

## Prompt 3: Documentación técnica

```
Antes de responder, identifica qué información proviene del contexto proporcionado y qué información no puedes confirmar. No inventes archivos, modelos, tablas, campos ni flujos.

Actúa como documentador técnico para proyectos de software escolar.

Contexto: SASE Light, un sistema de administración escolar en Kotlin Multiplatform / Compose.
Lee los archivos de contexto: [listar archivos relevantes de SASE_ABACUS_SANDBOX/].

Necesito documentar:
[describir qué documentar: un flujo, un modelo, una pantalla, etc.]

Reglas:
- Lenguaje claro pero técnico
- Público objetivo: desarrolladores y auditores institucionales
- No incluir datos reales de alumnos
- Marcar claramente qué es mock, qué es placeholder, qué es real
- No inventar funcionalidades
- Si algo no existe, decirlo explícitamente
```

---

## Prompt 4: Revisión UX institucional

```
Antes de responder, identifica qué información proviene del contexto proporcionado y qué información no puedes confirmar. No inventes archivos, modelos, tablas, campos ni flujos.

Actúa como especialista en UX institucional para sistemas escolares.

Revisa la siguiente pantalla de SASE Light: [describir pantalla o adjuntar descripción]

Contexto:
- Usuarios: personal de Secretaría escolar
- Dispositivo: Desktop (Windows)
- Datos: simulados (mocks)
- Flujo institucional: Secretaría → alta oficial → expediente → credencial

Evalúa:
1. Claridad de la información presentada
2. Consistencia con el estilo institucional (formal, oscuro, "Liquid Glass")
3. Jerarquía visual de los datos
4. Navegación y acciones disponibles
5. Manejo de estados vacíos / error / carga

PROHIBIDO:
- Sugerir nuevas funcionalidades no planificadas
- Sugerir cambios que requieran backend
- Sugerir implementación de PDF/impresión
- Sugerir cambios en modelos de datos

Formato: Lista de observaciones con prioridad (Alta/Media/Baja)
```

---

## Prompt 5: Generación de tests

```
Antes de responder, identifica qué información proviene del contexto proporcionado y qué información no puedes confirmar. No inventes archivos, modelos, tablas, campos ni flujos.

Actúa como desarrollador de tests para Kotlin Multiplatform.

Necesito agregar pruebas para el siguiente componente/modelo de SASE Light:
[describir componente o modelo]

Contexto del proyecto: [extraer de documentos de contexto]
Los tests existentes están en: [PreApplicationGuardrailsTest.kt u otros]
Framework de tests: kotlin.test

Reglas:
- Solo pruebas de lógica/modelo, no UI
- Usar datos mock creados en el mismo test
- No depender de datos mock globales si son frágiles
- Probar casos normales y casos borde
- No probar funcionalidad no implementada
- No requerir infraestructura externa
- Cobertura mínima: constructor, factory method, validaciones

Proveer:
- Código de tests
- Breve explicación de cada test
- Nota de integración (dónde agregar el archivo)
```

---

## Prompt 6: Detección de riesgos

```
Antes de responder, identifica qué información proviene del contexto proporcionado y qué información no puedes confirmar. No inventes archivos, modelos, tablas, campos ni flujos.

Actúa como analista de riesgos para software institucional escolar.

Revisa el siguiente cambio/código/arquitectura de SASE Light:
[describir qué revisar]

Contexto del proyecto: [extraer de documentos de contexto]

Identifica:

1. Riesgos de seguridad:
   - Exposición de datos sensibles de alumnos
   - Exposición de datos médicos/familiares
   - Hardcoding de secretos

2. Riesgos funcionales:
   - Ruptura del flujo institucional
   - Dependencias circulares
   - Estados inconsistentes

3. Riesgos de datos:
   - Mezcla de datos mock con datos reales
   - Pérdida de trazabilidad
   - Duplicación no controlada

4. Riesgos de cumplimiento:
   - Funcionalidades fuera de fase
   - Desviación del alcance aprobado
   - Privacidad de datos (LFPDPPP mexicana)

Formato:
- Lista de riesgos ordenados por severidad (Crítico/Alto/Medio/Bajo)
- Cada riesgo con: descripción, impacto probable, recomendación de mitigación
- Sin código nuevo
```
