# ROADMAP EL CEIBO — ESTADO CANÓNICO Y CIERRE

**Actualizado:** 27 de agosto de 2026

**Objetivo:** terminar El Ceibo como un proyecto Java/Spring Boot funcional, seguro, probado, reproducible y defendible en entrevistas técnicas.

Este archivo es la hoja de ruta ejecutiva. No reemplaza el código, las SPEC ni
las reglas operativas del repositorio.

Orden de autoridad:

```text
repositorio y estado real de Git
        ↓
SPEC vigente de cada fase
        ↓
CLAUDE.md y workflow close-posta
        ↓
este roadmap
```

---

# 1. CHECKPOINT ACTUAL

| Dato | Estado |
|---|---|
| Fecha del checkpoint | 27/08/2026 |
| Branch de trabajo | `feature/final-security-csrf` |
| Último commit funcional | `ce023f8450f09d9ecf4871ff076c7412edff0093` |
| Mensaje | `Enable CSRF protection and tests` |
| Fase actual | P15.5 — Roles, navegación y privilegios |
| Próxima operación | P15.5 Gate A |
| Base normal `libreriaweb` | Flyway V3; protegida |
| Base de tests | `libreriaweb_testsuite`; descartable; Flyway V1→V4 |

---

# 2. ESTADO EJECUTIVO

| Fase | Estado | Resultado principal |
|---|---|---|
| P1–P12.5 | ✅ Cerradas | Base, seguridad inicial, catálogo, usuarios, Flyway e integridad |
| P13 | ✅ Cerrada y mergeada | Préstamos MVP con transacciones, locks, historial y permisos |
| P14 | ✅ Cerrada y mergeada | Infraestructura reproducible y 45 tests permanentes |
| P15 | 🟡 En curso | P15.0–P15.4 cerradas; P15.5 próxima; P15.6 pendiente antes del merge |
| P16 | ⏳ Pendiente | Smoke, documentación, demo, clon limpio y publicación |

Secuencia restante:

```text
P15.5 roles, navegación y privilegios
    ↓
P15.6 validación acumulada y merge
    ↓
P16 migración demo + smoke + portfolio
    ↓
publicación
```

---

# 3. CRONOGRAMA ACTUALIZADO

Las fechas son ventanas de trabajo, no excusas para apurar seguridad o
migraciones.

| Fechas | Objetivo |
|---|---|
| 08–23 ago | ✅ P12.5, P13 y P14 cerradas |
| 24–26 ago | ✅ P15.0–P15.3 y TOOLING.2 cerradas |
| 27–30 ago | ✅ P15.4 — CSRF activado, 72/72 tests y commit |
| 31 ago–3 sep | P15.5: roles, navegación y privilegios |
| 4–6 sep | P15.6: validación acumulada y merge a `master` |
| 7–10 sep | P16.0–P16.2: migración V4 controlada, smoke y pulido visual |
| 11–13 sep | P16.3–P16.7: README, diagramas, demo, clon limpio y publicación |
| 14–20 sep | Margen para findings reales o deudas imprevistas |

Objetivos orientativos:

- P15 mergeada: alrededor del **6 de septiembre**;
- proyecto publicable: alrededor del **13 de septiembre**;
- margen prudente de cierre: hasta el **20 de septiembre**.

Estimación restante aproximada:

| Bloque | Horas efectivas |
|---|---:|
| P15.5 — gaps de seguridad | 2–4 h |
| P15.6 — cierre y merge | 2–3 h |
| P16.0–P16.2 — DB demo, smoke y visual | 2–4 h |
| P16.3–P16.5 — README, diagramas y demo | 5–8 h |
| P16.6–P16.7 — clon limpio y publicación | 2–3 h |

**Total estimado:** 13–22 horas efectivas, más margen si aparece un finding
real. No sumar tiempo artificial repitiendo matrices que ya tienen evidencia
vigente.

---

# 4. P15 — SEGURIDAD FINAL

## P15.0 — Auditoría inicial ✅

Se auditó:

- superficie HTTP;
- mutaciones por GET;
- reglas de `SecurityConfig`;
- formularios de autenticación y logout;
- cobertura MockMvc existente;
- riesgos de CSRF, roles e IDOR.

## P15.1 — SPEC e inventario ✅

Quedaron definidos:

- endpoints mutadores;
- matriz anónimo / USUARIO / ADMIN;
- orden de migración GET→POST;
- preparación del markup;
- activación posterior de CSRF;
- estrategia de tests y gates separados.

## P15.2 — Mutaciones de catálogo GET → POST ✅

Resultado:

- 12 endpoints mutadores de Autor, Editorial, Libro y Usuario convertidos a
  POST;
- ninguna mutación residual mediante GET dentro del scope auditado;
- formularios con `th:action`;
- cobertura permanente en `CatalogMutationsMockMvcTest`;
- 12 tests nuevos;
- suite acumulada en ese punto: 57 tests.

Commit:

`d6249b5`

## P15.3 — Preparación de autenticación y logout ✅

Resultado:

- login y registro usan `th:action`;
- logout dejó de depender de enlaces GET;
- existen exactamente cuatro formularios POST de logout;
- HTML válido sin formularios anidados;
- sidebar y navbars conservaron tipografía, padding y comportamiento visual;
- cero tokens `_csrf` agregados manualmente.

Commit:

`33c6ca3c0fac37a12250f01e2a15089147d7ca62`

## P15.4 — Activar CSRF y adaptar tests ✅

Resultado:

- CSRF activo por la configuración segura predeterminada de Spring Security
  (eliminado `.csrf(csrf -> csrf.disable())`, sin exclusiones agregadas);
- 18 call sites POST existentes adaptados con `with(csrf())` para conservar el
  propósito real de sus tests;
- 15 tests nuevos en `CsrfProtectionMockMvcTest` (login, registro, logout,
  token ausente, token inválido, `GET /logout`, y forms que renderizan el
  input `_csrf` dentro del formulario correcto);
- fixture `"render-biblioteca"` corregido tras una colisión de nombre único
  detectada en la primera corrida real;
- `test-compile`: `BUILD SUCCESS`;
- reviews independientes: PASS (diff completo y delta del fixture);
- suite real: 72/72 tests, 0 failures, 0 errors, 0 skipped, en 7 reportes
  Surefire;
- Flyway V1→V4 con `success = 1` únicamente en `libreriaweb_testsuite`;
- `libreriaweb` intacta.

Commit:

`ce023f8450f09d9ecf4871ff076c7412edff0093`

## P15.5 — Roles, navegación y privilegios ⏳

P15.5 no debe duplicar P15.2/P15.4 ni perseguir una cifra decorativa de tests.
Primero debe contrastar la SPEC y el inventario de rutas contra el baseline
confirmado: 72 tests en 7 clases. Solo se agregan casos cuando exista un gap
demostrable.

Revisar:

- matriz anónimo / USUARIO / ADMIN sobre rutas públicas, autenticadas y
  administrativas;
- coherencia entre `SecurityConfig` y los controladores (privilegios backend
  reales, no solo visuales);
- navegación visible acorde a esos privilegios (qué ve cada rol en
  sidebar/navbar);
- anónimo → redirección/login cuando corresponda;
- IDOR y parámetros de identidad manipulados;
- respuestas 403 y redirecciones correctas según el caso (login para
  anónimo, 403 para rol insuficiente);
- ausencia de mutaciones GET residuales;
- ausencia de 500 en entradas inválidas importantes — respuesta controlada;
- hashes de contraseña no expuestos ni reemplazados accidentalmente;
- ausencia de secretos o credenciales trackeadas;
- coherencia final entre `SecurityConfig`, controladores y templates.

Hallazgos LOW conocidos que deben decidirse, no corregirse por reflejo:

- respuesta 400 genérica cuando falta algún `id`;
- navegación ADMIN poco visible o ausente en ciertos flujos.

El primero no bloquea por sí solo. El segundo debe verificarse durante el smoke
visual de P16 y corregirse solamente si continúa afectando la demo.

Salida esperada de P15.5:

- matriz real cerrada;
- tests dirigidos únicamente para gaps;
- suite PASS;
- review independiente;
- commit propio si hubo cambios.

La cantidad final de tests se fija después de la auditoría de P15.5; no se
inventa anticipadamente.

## P15.6 — Cierre acumulado y merge ⏳

Checklist:

- branch limpia y commits coherentes;
- comparar `feature/final-security-csrf` contra `master`;
- confirmar que `master` no cambió inesperadamente;
- review independiente del diff acumulado de P15;
- revisar secretos, archivos generados y scope;
- confirmar migraciones V1–V4 inmutables;
- ejecutar una validación final limpia contra `libreriaweb_testsuite`;
- verificar artefacto empaquetado;
- Gate D y merge a `master`;
- checkpoint post-merge;
- sin push todavía.

Validación final propuesta:

`./mvnw -ntp clean package`

`package` ya ejecuta la suite. No correr además `clean test` y luego
`clean package`, porque eso repetiría la misma suite y reconstruiría la base dos
veces sin ganar evidencia. Solo separar comandos si un Gate A posterior
demuestra una necesidad concreta. Esta ejecución requiere su propio Gate B
porque vuelve a reconstruir `libreriaweb_testsuite`.

---

# 5. P16 — PORTFOLIO Y PUBLICACIÓN

## P16.0 — Preparar la base demo y aplicar V4 ⏳ NUEVO BLOQUE NECESARIO

La migración V4 existe y está validada en bases de prueba, pero la base normal
`libreriaweb` continúa en Flyway V3. Antes de iniciar la aplicación real para
smoke, capturas o demo:

1. confirmar branch `master` limpia después de P15;
2. identificar de forma explícita host, puerto y schema;
3. verificar que el target sea exactamente `libreriaweb`;
4. generar y comprobar un backup previo;
5. confirmar que Flyway informa versión 3;
6. autorizar separadamente la aplicación de V4;
7. dejar que Flyway sea el único dueño del cambio de schema;
8. validar que la base queda en V4 y que Hibernate
   `ddl-auto=validate` inicia correctamente.

No ejecutar la aplicación contra `libreriaweb` mientras siga en V3. No aplicar
DDL manual ni reutilizar los scripts destructivos de `libreriaweb_testsuite`.

## P16.1 — Smoke funcional y visual ⏳

Recorrer con navegador real:

- catálogo público;
- registro;
- login de USUARIO y ADMIN;
- logout POST;
- solicitud de préstamo;
- Mis préstamos;
- listado ADMIN;
- devolución;
- permisos al escribir rutas manualmente;
- mensajes de éxito/error;
- navbar, sidebar y responsive básico.

El smoke valida integración y experiencia. No reemplaza ni repite toda la
matriz automatizada.

## P16.2 — Limpieza visual final ⏳

Corregir únicamente defectos observables:

- navegación ADMIN;
- alineación y espaciado;
- formularios y mensajes;
- catálogo y vistas de préstamos;
- responsive básico.

No realizar un rediseño completo ni cambiar la arquitectura visual.

## P16.3 — README profesional ⏳

Debe explicar:

- problema y alcance;
- funcionalidades por rol;
- stack exacto;
- arquitectura MVC por capas;
- modelo de datos;
- reglas de préstamo y stock;
- transacciones y lock pesimista;
- seguridad y CSRF;
- Flyway V1–V4;
- configuración mediante variables de entorno;
- ejecución con Maven Wrapper;
- datos demo;
- tests y base aislada;
- decisiones técnicas y tradeoffs;
- límites conocidos y mejoras futuras.

No publicar credenciales reales ni instrucciones que dependan de la máquina de
desarrollo.

## P16.4 — Diagramas y capturas ⏳

Diagramas mínimos:

- Controller → Service → Repository → MariaDB;
- entidades y cardinalidades;
- request → validación → transacción → commit/rollback;
- solicitud/devolución de préstamo;
- carrera por el último ejemplar y lock pesimista;
- matriz USUARIO / ADMIN para rutas sensibles.

Capturas mínimas:

- catálogo;
- login o registro;
- Mis préstamos;
- administración de préstamos;
- flujo exitoso de préstamo/devolución.

## P16.5 — Demo y defensa técnica ⏳

Preparar:

- presentación de aproximadamente 2 minutos;
- demo guiada de aproximadamente 5 minutos;
- explicación de JPA/Hibernate y dirty checking;
- transacciones, rollback y locks;
- CSRF, autenticación, autorización e IDOR;
- Flyway y separación de bases;
- estrategia de tests;
- un problema real encontrado y cómo se corrigió.

Lauti debe poder explicar el flujo sin leer el código línea por línea.

## P16.6 — Prueba desde clon limpio ⏳

Antes del push final:

- clonar en una carpeta nueva;
- configurar solamente desde archivos de ejemplo/documentación;
- usar una base descartable nueva, nunca `libreriaweb`;
- ejecutar Flyway V1→V4 desde cero;
- cargar datos demo;
- ejecutar una sola vez `./mvnw -ntp clean package` con Maven Wrapper,
  incluyendo la suite y la generación del paquete;
- iniciar la aplicación;
- probar los flujos principales de ADMIN y USUARIO;
- confirmar `git status` limpio.

La prueba debe demostrar que el proyecto funciona fuera del entorno histórico
de desarrollo.

## P16.7 — Publicación Git ⏳

- revisar `git ls-files`;
- confirmar ausencia de `.env`, secretos, uploads, logs, backups y artefactos;
- revisar README, enlaces e imágenes;
- confirmar `master` limpia;
- configurar/verificar remoto;
- push explícitamente autorizado;
- verificar el repositorio publicado;
- opcional: crear tag o release `v1.0.0` después de validar el estado remoto.

Condición de cierre de P16:

```text
clon limpio reproducible
+ suite final PASS
+ aplicación ejecutable
+ demo funcional
+ documentación completa
+ repositorio sin secretos
= proyecto listo para portfolio e entrevistas
```

---

# 6. DEUDAS Y RIESGOS CONTROLADOS

## Bloqueantes antes de publicar

- completar P15.5 y P15.6;
- migrar `libreriaweb` de V3 a V4 de forma controlada;
- hacer smoke funcional;
- verificar clon limpio;
- revisar secretos y archivos trackeados.

## No bloqueantes

- mejorar el 400 genérico por parámetros ausentes si aporta UX real;
- limpiar bases temporales históricas cuando ya no hagan falta;
- revisar la cuarentena/mantenimiento de MariaDB;
- pulir navegación ADMIN si el smoke confirma el defecto;
- adaptar `close-posta` a Gastos Express después de cerrar El Ceibo.

## Deuda ya cerrada

- TOOLING.2: eliminado de `settings.json` el permiso amplio `git add *`.

---

# 7. WORKFLOW VIGENTE

Secuencia mental:

```text
Mirar → Acordar → Implementar → Verificar → Revisar → Registrar
```

Gates:

| Gate | Propósito |
|---|---|
| A | Auditoría read-only, diseño, scope y autorización de implementación |
| B | Pruebas reales o acciones con DB, autorizadas por separado |
| C | Stage exacto, revisión del índice y commit |
| D | Revisión acumulada y merge de la fase |

Reglas permanentes:

- checkpoint Git antes de tocar código;
- cambios mínimos y dentro de scope;
- reviewer lee y reporta, no modifica;
- tester intenta romper el comportamiento, no corrige producto;
- finding real → delta mínimo → test dirigido → re-review;
- reutilizar evidencia vigente;
- no repetir matrices sin causa;
- nunca `git add .` ni `git add -A`;
- sin `--amend`, Co-Authored-By, merge o push sin autorización;
- Flyway es dueño del schema;
- migraciones existentes son inmutables;
- tests destructivos solo contra `libreriaweb_testsuite`;
- la base normal permanece protegida.

El detalle operativo vive en `CLAUDE.md` y `close-posta`; no se duplica aquí.

---

# 8. HISTORIAL COMPACTO DE FASES CERRADAS

El roadmap ya no replica cada subcommit. Git conserva el historial exacto y las
SPEC conservan el diseño. Aquí quedan únicamente los hitos útiles para entender
la evolución del proyecto.

| Fase | Resultado | Referencia principal |
|---|---|---|
| P1 | Base del proyecto | `6ca91ec` |
| P2 | Spring Security, login, roles y errores | `d58e5f2` |
| P3 | Blindaje de secretos | `86b3bc4` |
| P4 | Edición segura de usuario y hash oculto | `3389323` |
| P5 | Integración de usuarios | merge `6dd0e02` |
| P6 | Biblioteca pública | `ee52ac1` |
| P7 | Integración de biblioteca | merge `d62ae96` |
| P8 | Catálogo público y stock | `8060f4b` |
| P9 | Seed demo reproducible | `81cb83c`, merge `58effb0` |
| P10 | Roadmap y footer | `8bf6937` |
| P11 | Roles visuales y catálogo ADMIN | `9648297`, merge `351e85f` |
| P12 | Email duplicado controlado | `494486d`, merge `540bf0d` |
| P12.5 | Integridad, portadas, concurrencia y Flyway | merge `d40dc23` |
| P13 | Préstamos MVP | merge `3aa3692` |
| P14 | Tests permanentes | mergeada a `master` antes de P15 |

## P12.5 — Resultado consolidado ✅

- relaciones e invariantes del catálogo estabilizadas;
- edición y borrado conscientes del historial;
- portadas JPEG/PNG validadas, servidas y eliminadas de forma segura;
- ISBN bibliográfico opcional sin valores falsos;
- rollback y concurrencia protegidos;
- último ADMIN protegido;
- operaciones de portadas serializadas;
- Flyway 8.5.13 adoptado;
- Hibernate `ddl-auto=validate`;
- V1 baseline, V2 ISBN y V3 integridad de Libro;
- `Prestamo.usuario` corregido a `@ManyToOne`;
- instalación desde cero, adopción e idempotencia verificadas.

## P13 — Préstamos MVP ✅

Resultado:

- vencimiento a 14 días mediante V4;
- préstamo activo definido por `fechaDevolucion == null`;
- máximo de tres préstamos activos por usuario;
- sin préstamo activo duplicado del mismo libro/usuario;
- identidad tomada de `Authentication.getName()`;
- USUARIO solicita para sí mismo;
- ADMIN lista y registra devoluciones;
- usuario/libro inactivo bloquea nuevas solicitudes;
- devolución permitida aunque usuario o libro hayan quedado inactivos;
- historial preservado;
- stock consistente;
- locks pesimistas y orden de locks probado;
- carrera por la última unidad: un éxito y un rechazo controlado;
- reviewer y adversarial tester en PASS.

## P14 — Tests permanentes ✅

Resultado:

- Maven Wrapper reproducible;
- perfil de test aislado;
- `libreriaweb_testsuite` reconstruida con guardas;
- Flyway V1→V4 aplicado únicamente en tests;
- anotación reutilizable `@LibreriaIntegrationTest`;
- integración de préstamos;
- regresiones de usuario y catálogo;
- MockMvc para autenticación, roles, IDOR y PRG;
- baseline al cierre: 45 tests permanentes;
- suite estable y fase mergeada.

## Agent Engineering V1 ✅ OPERATIVO

- reglas permanentes en `CLAUDE.md`;
- reviewer independiente;
- adversarial tester;
- gates humanos;
- staging exacto;
- workflow `close-posta` creado, validado y utilizado;
- permisos peligrosos acotados.

`/goal`, worktrees, Dynamic Workflows, Agent Teams y swarms dejan de formar
parte del camino crítico de El Ceibo. Pueden evaluarse después en otro proyecto
si aportan una mejora medible.

---

# 9. FUERA DE ALCANCE PARA EL CIERRE

No bloquean la publicación:

- mails reales;
- multas;
- pagos;
- renovaciones;
- puntuaciones;
- lista de espera;
- generación de PDF;
- Docker;
- SweetAlert;
- Java 17;
- migración a Spring Boot 3;
- arquitectura distribuida;
- rediseño visual completo;
- suite gigantesca orientada a cobertura;
- CI/CD complejo;
- Agent Teams o swarm como requisito.

Backlog posterior opcional:

- modernización Java 17 + Spring Boot 3;
- Docker y CI simple;
- despliegue público;
- funcionalidades bibliotecarias adicionales.

Nada de este backlog debe reabrir P15 o retrasar P16.

---

# 10. DEFINICIÓN FINAL DE TERMINADO

El Ceibo queda cerrado cuando:

1. P15 está mergeada a `master` con suite y package PASS;
2. `libreriaweb` está migrada de forma controlada a V4;
3. los flujos principales funcionan en navegador;
4. un clon limpio puede instalar, migrar, probar y ejecutar el proyecto;
5. README, diagramas y capturas explican el sistema;
6. no existen secretos ni archivos locales trackeados;
7. el repositorio está publicado con autorización explícita;
8. Lauti puede explicar las decisiones técnicas principales.

No buscamos perfección infinita. Buscamos evidencia suficiente de que el
proyecto funciona, resiste sus riesgos reales y puede ser defendido con
claridad.

---

# SIGUIENTE ACCIÓN EXACTA

**P15.5 — Gate A: auditoría read-only de roles, navegación y privilegios.**
