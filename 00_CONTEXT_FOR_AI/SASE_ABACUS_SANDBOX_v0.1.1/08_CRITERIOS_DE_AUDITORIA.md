# 08 — Criterios de auditoría

---
**Versión de contexto:** 0.1  
**Commit base:** 4f4bc84  
**Fecha de generación:** 2026-06-30  
**Uso previsto:** contexto para agentes IA externos  
**Datos sensibles:** no incluidos intencionalmente  
---

## Propósito

Estos criterios permiten revisar cualquier cambio futuro en SASE Light de manera consistente, asegurando que no se introduzcan regresiones, datos falsos, o funcionalidades fuera de fase.

---

## Criterio 1: Alcance limitado

- [ ] El cambio está dentro del alcance declarado en la tarea
- [ ] No introduce funcionalidades no solicitadas
- [ ] No modifica archivos fuera de la lista permitida
- [ ] No agrega dependencias externas no autorizadas

## Criterio 2: Número de archivos modificados

- [ ] El número de archivos modificados es proporcional al alcance
- [ ] No hay archivos "touched" sin cambios reales
- [ ] No hay archivos de build/ o generated en el diff

## Criterio 3: No rompe flujo existente

- [ ] El flujo Secretaría → alta oficial → expediente → credencial funciona
- [ ] La navegación entre pantallas no se interrumpe
- [ ] Los datos mock existentes siguen siendo válidos
- [ ] No se eliminan rutas de navegación existentes

## Criterio 4: No introduce mocks desconectados

- [ ] Cualquier nuevo dato mock está conectado al flujo existente
- [ ] No hay datos mock que simulen funcionalidad no implementada
- [ ] Los mocks existentes no se modifican sin necesidad

## Criterio 5: No agrega funciones fuera de fase

- [ ] No implementa PDF/impresión sin autorización explícita
- [ ] No conecta Supabase/backend sin autorización
- [ ] No agrega pantallas o módulos no planificados
- [ ] No introduce funciones que requieran infraestructura externa

## Criterio 6: Build y tests deben pasar

- [ ] `compileKotlinDesktop` = BUILD SUCCESSFUL
- [ ] `desktopTest` = BUILD SUCCESSFUL (0 fallos)
- [ ] No se rompen tests existentes
- [ ] Los tests nuevos son relevantes y no frágiles

## Criterio 7: Datos sensibles protegidos

- [ ] No se exponen datos médicos/familiares en vistas públicas (credencial)
- [ ] No se incluyen CURP, teléfonos, domicilios reales en documentación
- [ ] No se hardcodean API keys o tokens
- [ ] No se agregan logs que expongan datos de alumnos

## Criterio 8: Documentación actualizada

- [ ] Los cambios están reflejados en la documentación de contexto si aplica
- [ ] Los modelos nuevos o modificados están documentados
- [ ] Las decisiones técnicas están registradas en la bitácora

## Criterio 9: Commits limpios

- [ ] Mensaje de commit descriptivo (conventional commit)
- [ ] No hay archivos de build/ en el commit
- [ ] No hay archivos no relacionados en el commit
- [ ] El diff muestra solo los cambios esperados

## Criterio 10: No hay regresiones visuales

- [ ] Las pantallas existentes se renderizan correctamente
- [ ] Los colores, fuentes y espaciados son consistentes
- [ ] Los componentes compartidos no se rompen

---

## Checklist resumen para revisión

```
[ ] Alcance limitado
[ ] Archivos correctos
[ ] Flujo intacto
[ ] Mocks conectados
[ ] Sin funciones fuera de fase
[ ] Builds pasan
[ ] Tests pasan
[ ] Datos sensibles protegidos
[ ] Documentación actualizada
[ ] Commits limpios
```

Si algún criterio falla, el cambio debe rechazarse o corregirse antes de integración.
