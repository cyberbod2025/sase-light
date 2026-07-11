# R-012 — Lectura mínima suficiente

## Propósito

Reducir consumo de tokens, tiempo y ruido contextual sin perder seguridad, trazabilidad ni calidad técnica.

## Principio central

> Un checkpoint aprobado convierte el trabajo anterior en una interfaz confiable.
>
> El microloop siguiente consume esa interfaz; no reaudita todo el pasado.

La lectura del repositorio debe responder a una decisión concreta del ciclo actual. La amplitud de lectura se ajusta al tipo de trabajo y no a una preferencia genérica por acumular contexto.

## Tipos de ciclo

### Macroloop

Se permite lectura amplia cuando el objetivo sea:

- diagnóstico arquitectónico;
- descubrimiento de riesgos;
- levantamiento de dependencias;
- evaluación de alcance;
- diseño de estrategia.

La amplitud debe seguir declarada y orientada por preguntas de investigación. Un macroloop no autoriza lecturas indiscriminadas.

### Microloop

La lectura mínima es obligatoria y se limita inicialmente a:

- archivos objetivo;
- dependencias directas;
- pruebas directamente relacionadas;
- búsquedas puntuales por símbolo.

El checkpoint anterior se consume como contrato operativo. No se reconstruye la arquitectura completa si el objetivo puede demostrarse mediante las interfaces ya aprobadas.

### Recuperación forense

La lectura se limita a:

- artefactos afectados;
- evidencia de integridad;
- hashes;
- estado Git;
- dependencias mínimas necesarias para validar la recuperación.

Antes de interpretar o consolidar contenido se preserva el estado encontrado. La recuperación no atribuye autoría sin evidencia.

## Presupuesto inicial de lectura

Cada microloop debe declarar antes de actuar:

- archivos que leerá completos;
- símbolos que puede buscar puntualmente;
- máximo inicial de archivos completos;
- criterios de expansión;
- archivos expresamente fuera de alcance.

Valor recomendado:

```text
Máximo inicial: 4 archivos completos
```

El valor puede reducirse según el tamaño de la tarea. El presupuesto es un límite inicial, no una cuota que deba agotarse.

Las búsquedas puntuales deben indicar un símbolo, firma, error o dependencia concreta. Encontrar una coincidencia no autoriza automáticamente a leer completo el archivo.

## Expansión permitida

Solo se permite abrir un archivo adicional cuando exista al menos una de estas condiciones demostrables:

- símbolo no resuelto;
- firma incompatible;
- error de compilación;
- dependencia directa demostrada;
- prueba que requiere fixture o contrato real;
- conflicto entre prompt y código fuente.

Antes de ampliar la lectura debe comprobarse si una búsqueda puntual, una firma pública o el checkpoint vigente bastan para resolver la duda.

## Registro de expansión

Toda ampliación debe quedar registrada así:

```text
Archivo adicional:
Motivo:
Símbolo, error o dependencia que obligó a abrirlo:
Decisión tomada:
```

El registro debe aparecer en el reporte del mismo ciclo. Si la expansión supera el máximo declarado, debe explicarse por qué detenerse habría impedido una decisión segura.

## Conductas prohibidas

- leer el repositorio completo;
- enumerar todos los archivos sin necesidad;
- reconstruir arquitectura ya certificada;
- releer módulos no relacionados;
- buscar términos generales sin objetivo;
- abrir UI, stores o ViewModels cuando no son parte del microloop;
- releer documentación histórica sin necesidad;
- usar “por seguridad” como justificación suficiente.

La curiosidad técnica no sustituye una dependencia demostrada. Los hallazgos incidentales fuera de alcance se registran, pero no amplían automáticamente el ciclo.

## Regla de confianza

Los checkpoints aprobados son fuente de verdad operativa para el microloop siguiente.

Solo pueden reabrirse decisiones previas cuando:

- aparece evidencia nueva;
- falla una prueba;
- una firma real contradice el contrato;
- el repositorio no coincide con el commit base;
- existe un riesgo demostrado.

La confianza no elimina la reentrada segura: primero se verifica repositorio, rama, commit y estado Git; después se consume el checkpoint.

## Regla de suficiencia

> Leer más no equivale a razonar mejor.
>
> Toda lectura debe justificar una decisión del alcance actual.

La lectura termina cuando existe evidencia suficiente para ejecutar, verificar o detener el ciclo. No se continúa leyendo solo para aumentar la sensación de certeza.

## Evidencia mínima del microloop

El reporte final debe incluir:

- archivos completos leídos;
- búsquedas puntuales realizadas;
- archivos adicionales abiertos;
- razón de cada expansión;
- presupuesto respetado o excedido;
- justificación si fue excedido.

Cuando no se haya leído ningún archivo completo, también debe declararse. Una lectura cero puede ser correcta si el checkpoint y las firmas puntuales bastan.

## Plantilla reutilizable

```text
==================================================
R-012 — LECTURA MÍNIMA SUFICIENTE
==================================================

Tipo de ciclo:
- Macroloop / Microloop / Recuperación forense

Archivos a leer completos:
1.
2.

Símbolos permitidos para búsqueda puntual:
-

Presupuesto inicial:
- Máximo ___ archivos completos.

Expansión permitida únicamente por:
- símbolo no resuelto;
- firma incompatible;
- error de compilación;
- dependencia directa demostrada.

Por cada archivo adicional registrar:
- ruta;
- motivo;
- símbolo o error que justificó la lectura.

Fuera de alcance:
-
```

## Relación con otras reglas y principios

R-012 opera junto con:

- reentrada segura;
- Git como fuente de verdad;
- alcance mínimo;
- verificación obligatoria;
- evidencia antes que opinión;
- una sola escritura por worktree;
- checkpoints reversibles.

La lectura mínima no reduce las verificaciones obligatorias. Limita el contexto inspeccionado, no la evidencia necesaria para aceptar un resultado. Tampoco autoriza a omitir una dependencia directa, ocultar una contradicción o continuar sobre un estado Git inesperado.

## Criterio de cumplimiento

R-012 se considera respetada cuando:

1. el presupuesto fue declarado antes de la lectura;
2. cada archivo completo tuvo una finalidad explícita;
3. las búsquedas puntuales estuvieron ligadas a símbolos o decisiones concretas;
4. toda expansión quedó registrada;
5. el reporte permite reconstruir por qué la lectura realizada fue suficiente;
6. no se reabrió trabajo certificado sin evidencia nueva.
