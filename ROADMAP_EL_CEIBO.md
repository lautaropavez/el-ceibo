# ROADMAP EL CEIBO — ESTADO CANÓNICO Y CIERRE

**Actualizado:** 30 de septiembre de 2026

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
| Fecha del checkpoint | 26/10/2026 |
| Rama activa | `feature/portfolio-publication` |
| Último commit | `ac4c77e83ff48c10f8e7751f3f35bf7c42a57773` — "Fix packaged template resolution and harden security headers" (anterior: `3120ac5`, "Document portfolio setup, architecture and technical defense") |
| Rama base | `master` en `43f9a5361222c5b905dd113b1f7a5e8d41f7654e` |
| Mensaje del HEAD base | `Finalize P15 security roadmap` |
| Working tree / índice | la corrección de fragmentos Thymeleaf, el control de cabeceras (CVE-2026-22732), los 6 tests nuevos y `scripts/smoke-jar-demo.ps1` están en `ac4c77e`; solo quedan sin commit y sin stage estas actualizaciones de README y ROADMAP (el commit documental queda para el cierre); sin push ni merge |
| P15 | completa, validada y mergeada a `master` mediante fast-forward (Gate D cerrado) |
| `feature/final-security-csrf` | conservada únicamente como rama histórica, sin cambios pendientes |
| P16 | P16.1 cerrada con alcance reubicado (commit `5386f06`); **P16.2 cerrada técnicamente** (`8de0cbc`); **P16.3 y P16.4 integradas** en `feature/portfolio-publication` (`f9ad152`, `97567c7`); cambios de interfaz y seguridad guardados en `d28dbbd`; **validación funcional de P16.5 consolidada** (capturas públicas incorporadas el 27/10/2026; recorrido completo por teclado diferido); P16.6 con documentación en `3120ac5` y actualizada en el árbol de trabajo; **P16.7 en curso**: escaneos ejecutados sobre `3120ac5`, defecto de plantillas en `java -jar` hallado y corregido (`ac4c77e`), JAR verificado contra la demo aislada; verificación en clon limpio de `ac4c77e` hecha (228 tests, escaneos sin hallazgos nuevos); pendientes: capturas, comprobación de Mermaid en GitHub y revisión acumulada |
| Base normal `libreriaweb` | Flyway V3; protegida; no tocada por P16 |
| Base de tests | `libreriaweb_testsuite` y `libreriaweb_demo_testsuite`; descartables; Flyway V1→V4 |
| Baseline de tests vigente | **228 tests en 17 clases, 0 fallos, BUILD SUCCESS** (`./mvnw -ntp clean package` con tests, sin perfil demo, solo sobre las dos bases de test), ejecutada sobre el árbol de trabajo previo al commit de la corrección. Total verificado sumando los reportes de surefire; BUILD SUCCESS aportado por el autor al leer la salida; el código de salida original de Maven no se recuperó. Incluye la regresión de login (72 bytes) y 6 tests nuevos. Evidencias históricas, incluidas y no sumadas: 221 tests en 15 clases (anterior a la regresión) y corrida focalizada de `CsrfProtectionMockMvcTest` (17) |

---

# 2. ESTADO EJECUTIVO

| Fase | Estado | Resultado principal |
|---|---|---|
| P1–P12.5 | ✅ Cerradas | Base, seguridad inicial, catálogo, usuarios, Flyway e integridad |
| P13 | ✅ Cerrada y mergeada | Préstamos MVP con transacciones, locks, historial y permisos |
| P14 | ✅ Cerrada y mergeada | Infraestructura reproducible y 45 tests permanentes |
| P15 | ✅ Cerrada y mergeada | P15.0–P15.6 cerradas; mergeada a `master` por fast-forward (commit `43f9a53`) |
| P16 | 🟡 En curso | P16.1 cerrada con alcance reubicado (`5386f06`); P16.2 cerrada técnicamente (`8de0cbc`, 12 commits, 126/126); P16.3 y P16.4 integradas; interfaz y seguridad en `d28dbbd`; validación funcional de P16.5 consolidada (capturas públicas incorporadas, teclado completo diferido); P16.6 en curso (documentación en `3120ac5` y `ac4c77e`; actualización posterior pendiente de commit); P16.7 en curso (JAR verificado en la demo aislada tras corregir las plantillas; verificación en clon limpio de `ac4c77e` hecha, ver P16.7; capturas, Mermaid y revisión acumulada pendientes) |

Secuencia restante:

```text
P16.0 — baseline canónico y SPEC
    ↓
P16.1–P16.7 — dependencias, concurrencia, demo, UX, smoke, documentación, publicación
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
| 31 ago–3 sep | ✅ P15.5 — roles y privilegios, 5 casos nuevos, 77/77 (cerrada 28 ago, antes de lo previsto) |
| 4–6 sep | ✅ P15.6 — build final PASS y merge fast-forward a `master` (cerrada 30 ago, antes de lo previsto) |
| 1 sep | ✅ P16.0 — baseline canónico y SPEC |
| 2–3 sep | ✅ P16.1 — dependencias, wrapper y build tooling; cerrada con alcance reubicado (commit `5386f06`, 3 sep) |
| 4–6 sep | ✅ P16.2 — concurrencia, integridad y robustez; cerrada técnicamente el 30/09/2026 (ver sección 5) |
| 30 sep–01 oct | Cierre documental/checkpoint de P16.2 (esta actualización de roadmap/SPEC) |
| 02 oct | Gate A comparativo P16.3/P16.4 — determina si el paralelismo es seguro |
| 03–08 oct | P16.3 y P16.4 — en paralelo **solo si** el Gate A comparativo determina `PARALLEL SAFE` o `PARALLEL WITH EXPLICIT SHARED CONTRACTS`; si no lo determina, se ejecutan de forma secuencial dentro de la misma ventana |
| 09–10 oct | Integración secuencial de P16.3/P16.4 |
| 11–12 oct | P16.5 — smoke funcional y visual |
| 13–16 oct | P16.6 — portfolio y documentación |
| 17–19 oct | P16.7 — cierre reproducible y publicación (completamente secuencial) |
| 20–21 oct | Margen para findings reales |

Aclaraciones sobre este cronograma:

- son ventanas estimadas, no compromisos rígidos;
- un BLOCKER/HIGH puede replanificar el cronograma;
- el paralelismo de P16.3/P16.4 todavía **no está autorizado**: depende
  exclusivamente del resultado del Gate A comparativo del 02 oct;
- P16.7 permanece completamente secuencial, sin excepción;
- P16 sigue **abierta** hasta terminar P16.7;
- P16.3 es la próxima posta, después de congelar este checkpoint documental
  de P16.2;
- el objetivo del paralelismo evaluado en el Gate A es reducir tiempo
  calendario sin aumentar la coordinación necesaria.

Objetivos orientativos:

- P15 mergeada: **30 de agosto** (real, ya cerrada);
- P16.2 cerrada técnicamente: **30 de septiembre** (real, ya cerrada);
- proyecto publicable: alrededor del **19 de octubre**;
- margen prudente de cierre: hasta el **21 de octubre**.

**Estimación base:** 22–36 horas efectivas, más margen si aparece un finding
real. No sumar tiempo artificial repitiendo matrices que ya tienen evidencia
vigente. Una migración obligatoria a Spring Boot 3, si la SCA de P16.1 la
demuestra necesaria, se estima por separado y no se oculta dentro de este
rango.

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

## P15.5 — Roles, navegación y privilegios ✅

Resultado:

- auditoría completa de roles, navegación y privilegios — no se encontraron
  vulnerabilidades de aplicación dentro del scope auditado;
- matriz anónimo/USUARIO/ADMIN, IDOR, escalada de privilegios y navegación
  verificadas contra el código real, no solo contra templates;
- 5 casos HTTP nuevos: USUARIO por GET sobre 5 rutas administrativas →
  `403` (`/autor/registroAutor`, `/editorial/registroEditorial`,
  `/libro/registro`, `/libro/modificar/{id}`, `/usuario/modificar/{id}`);
- `WebSecurityMockMvcTest`: 19 → 24 tests;
- suite resultante: 77/77 en 7 clases;
- review independiente: PASS, 0 findings;
- 3 deudas diferidas identificadas (ver sección 6).

Commit:

`cafe44215ac2d27816c236f85da2ef5ce6ed606f`

## P15.6 — Cierre acumulado y validación final ✅

Resultado:

- topología confirmada: `master` (`bea2a36`) ancestro directo de HEAD, sin
  drift, 5 commits exclusivos de P15, sin merges/fixups — fast-forward
  posible;
- scope acumulado auditado íntegro (`master...HEAD`, 23 paths): ninguna
  migración V1–V4, servicio, entidad, repositorio ni `pom.xml` tocados fuera
  de lo previsto por la SPEC de P15;
- revisión acumulada independiente (diff completo `master...HEAD`): PASS,
  0 BLOCKER, 0 HIGH, 0 MEDIUM;
- `./mvnw -ntp clean package` → `BUILD SUCCESS`;
- 77 tests, 0 failures, 0 errors, 0 skipped, en 7 reportes Surefire frescos;
- Flyway V1→V4 con `success = 1` exclusivamente en `libreriaweb_testsuite`;
  `libreriaweb` no fue objetivo de ninguna conexión ni operación;
- JAR generado: `target/Libreria-0.0.1-SNAPSHOT.jar`, 47116438 bytes,
  SHA-256 `34bc1e010075d5084431661de727da7de90aa8e2d0ceb9a42b248bd1b639d78e`
  — inspeccionado read-only: manifest válido (`Main-Class`
  `org.springframework.boot.loader.JarLauncher`, `Start-Class`
  `com.mza.Libreria.LibreriaWebApplication`), clases productivas y
  migraciones V1–V4 presentes, sin recursos ni clases de `src/test`, sin
  secretos. El hash identifica el artefacto de este Gate B — no constituye
  todavía una garantía de build reproducible desde clon limpio, eso se valida
  recién en P16.6;
- `java -jar` diferido a P16.

Gate D (revisión pre-merge y merge fast-forward de
`feature/final-security-csrf` a `master`) se autorizó y ejecutó por
separado a continuación de este cierre — ver checkpoint (sección 1) para el
HEAD final resultante.

---

# 5. P16 — PORTFOLIO Y PUBLICACIÓN

## P16.0 — Baseline canónico y SPEC 🟡 EN AUDITORÍA (Gate A)

Auditoría read-only completa realizada: identidad y topología Git,
comparación del snapshot exportado contra el contenido ya auditado, secretos
en todo el historial (69 commits, todas las ramas), higiene de repo
(`.gitattributes`, checksum del wrapper, coordenadas del conector MySQL,
dependencias sin uso), concurrencia en `ServiceLibro`/`ServicePrestamo`,
`DemoDataSeeder`, procedencia de imágenes, y estado de README/CLAUDE.md.

No se encontraron secretos reales en la auditoría manual disponible sobre
todo el historial; **scanners dedicados (gitleaks/trufflehog) quedan
pendientes antes de publicar** — no hay ninguno instalado en este entorno y
no se instaló nada sin autorización (ver P16.1).

### Findings MEDIUM de concurrencia — diseño pendiente, sin corregir todavía

**MEDIUM 1 — Carrera préstamo/devolución vs alta/baja de Libro.**
El `UPDATE` de InnoDB espera el lock, pero no refresca la entidad ya leída en
memoria. Sin `@Version` ni lock explícito, `ServiceLibro.alta()`/`baja()`
pueden terminar operando sobre una instancia de `Libro` con datos de stock
obsoletos. Finding pendiente de corrección y de una matriz de test
concurrente dedicada.

**MEDIUM 2 — Baja de Autor/Editorial vs crear, modificar o reactivar Libro.**
Puede terminar con un `Libro` activo asociado a un Autor/Editorial inactivo.
`AutorRepository` y `EditorialRepository` no tienen ningún método de lock
pesimista, a diferencia de `UsuarioRepository`, `LibroRepository`,
`PrestamoRepository` y `PortadaRepository`.

**MEDIUM 3 — `modificarLibro()` puede terminar en `500`.**
Puede recibir `DataIntegrityViolationException` durante el flush/commit por
la constraint `UK_libro_titulo_autor`, sin ningún `catch` que la convierta en
`MiExcepcion`. La solución deberá usar `saveAndFlush()` dentro del `try` para
forzar el flush en el punto controlado, y capturar la colisión ahí mismo.

**Orden de locks a diseñar** (diseño pendiente, no implementación cerrada):

- `AUTOR → EDITORIAL → LIBRO → PORTADA`;
- `USUARIO → LIBRO`;
- `PRÉSTAMO → LIBRO`.

Los tests concurrentes deben comprobar serialización real y los invariantes
finales del sistema — no imponer artificialmente que una de las dos
operaciones concurrentes siempre falle.

**Siguiente paso de P16.0:** cerrar la SPEC de portfolio/publicación y
diseñar exactamente la corrección de estos findings. Sin implementación
todavía.

## P16.1 — Dependencias, secretos, wrapper y licencias — CERRADA CON ALCANCE REUBICADO POR DECISIÓN HUMANA

Implementación técnica cerrada en el commit `5386f06` (3 sep, Gate B 77/77
tests, JAR verificado). El alcance original completo de esta subposta **no**
se ejecutó dentro de P16.1: dos de sus ítems quedaron reubicados por
decisión humana —

- licencias/`LICENSE` y procedencia de imágenes → reubicado a **P16.4**;
- README y documentación de portfolio → reubicado a **P16.6**.

Lo efectivamente implementado y cerrado en `5386f06`:

- `.gitattributes` y line endings del wrapper (`mvnw` sin protección
  `eol=lf`: un clon limpio en Linux/Mac, o en Windows con `core.autocrlf`
  activo, puede romper el shebang del script);
- `distributionSha256Sum` oficial de Maven 3.9.16 en
  `.mvn/wrapper/maven-wrapper.properties`;
- cambio de coordenadas del conector MySQL a `com.mysql:mysql-connector-j`,
  sin subir de versión arbitrariamente antes de la SCA;
- eliminar `spring-boot-starter-mail` y `ServiceNotificacion` solo después de
  confirmar nuevamente que no tienen consumidores;
- scanner dedicado de secretos sobre todo el historial Git (Gitleaks,
  instalado y ejecutado con autorización explícita; sin secretos reales
  encontrados);
- SCA de dependencias y análisis de alcanzabilidad (OSV-Scanner, con
  triage completo por advisory).

## P16.2 — Concurrencia, integridad y robustez ✅ CERRADA TÉCNICAMENTE

**Fecha de cierre técnico:** 30/09/2026.
**Commit técnico final:** `8de0cbc63432d1569c0afe0a2621cebd72136bf6`.

Alcance completado:

- control BCrypt de 72 bytes UTF-8, fail-closed (CVE-2025-22228): `encode()`
  y `matches()` rechazan sin delegar al encoder real cuando se excede el
  límite; `ServiceUsuario.registrar()`/`.modificar()` validan antes de
  llegar al encoder;
- orden canónico `AUTOR → EDITORIAL → LIBRO(S)` implementado sin ninguna
  inversión vigente; libros múltiples siempre bloqueados por id ascendente;
- corrección de frescura frente al Persistence Context de Hibernate: toda
  lectura preliminar no autoritativa quedó como proyección escalar, nunca
  entity-typed, antes del `buscarYBloquearPorId` correspondiente;
- revalidaciones autoritativas bajo lock en `alta()`, `modificarLibro()`
  ("sin cambios" decidido una sola vez, después de los tres locks) y en las
  bajas masivas de Autor/Editorial;
- matriz real de concurrencia: 11 casos con hilos reales, sin `sleep`,
  detector determinista de `LOCK WAIT` (`INNODB_LOCK_WAITS`/`INNODB_TRX`),
  estado final leído por JDBC directo y limpieza sin enmascarar la causa
  original;
- `spring.jpa.open-in-view=false` efectivo (ausencia confirmada del bean
  `OpenEntityManagerInViewInterceptor`) y smoke de las seis rutas web
  afectadas;
- traducción controlada de fallos pesimistas en los **11 métodos públicos**
  de `ServiceLibro` que adquieren locks (`crearLibro`, `modificarLibro`,
  `alta`, `baja`, `altaxAutor`, `bajaxAutor`, `altaxEditorial`,
  `bajaxEditorial`, `eliminarLibro`, `eliminarAutor`, `eliminarEditorial`):
  ninguno deja escapar `PessimisticLockingFailureException` cruda, y el
  flush de cada uno ocurre dentro del mismo `try` que cubre el lock;
- preservación del diagnóstico original del harness de concurrencia
  (`causaPrincipal`/`addSuppressed` en el `finally` de `correrCarrera`).

Evidencia:

- 12 commits funcionales: `0fb3b62`, `aab547b`, `9dd2c23`, `521c52f`,
  `c4368a5`, `ce8174c`, `77db113`, `c542b95`, `cbe6ac1`, `0728cb0`,
  `eb2b18f`, `8de0cbc`;
- review acumulado principal: PASS;
- `code-reviewer` independiente: PASS;
- baseline fresco: **126 tests, 0 failures, 0 errors, 0 skipped, en 11
  clases** Surefire;
- Flyway V1→V4 con `success = 1` exclusivamente en `libreriaweb_testsuite`;
- `libreriaweb` normal no fue objetivo de ninguna conexión ni operación.

No se ejecutó `clean package`, generación de JAR ni clon limpio en esta
posta — esas comprobaciones corresponden a P16.7.

Deudas diferidas, explícitamente no resueltas ni reabiertas en P16.2 (ver
también sección 6):

- recuperación/reset administrativo para cuentas legacy con claves >72
  bytes (ver Delta A);
- dialecto Hibernate conservado por decisión humana, sin cambio de código;
- `spring.jpa.show-sql=true` → P16.6;
- carga EAGER de `Portada.contenido` → P16.4;
- observaciones preexistentes de `ServicePrestamo` (P13) y de la protección
  del último ADMIN en `ServiceUsuario`, fuera del scope de P16.2.

**Delta A — control compensatorio BCrypt (CVE-2025-22228):** política
fail closed. Toda contraseña que supere 72 bytes UTF-8 se rechaza,
incluida cualquier contraseña legacy — sin fallback al BCrypt vulnerable,
sin truncar ni prehashear. No se implementó un flujo nuevo de recuperación;
una cuenta legacy afectada requerirá un restablecimiento administrativo
controlado, autorizado por separado, antes de publicar. Decisión humana,
2026-09-11.

**Siguiente posta:** P16.3 — Base y datos demo seguros (diseño preliminar
cerrado, implementación no iniciada). P16 sigue en curso: esta sección
cierra únicamente P16.2.

## P16.3 — Base y datos demo seguros ✅ INTEGRADA

Integrada en `feature/portfolio-publication` (`f9ad152`, `--ff-only`; sin push
ni merge a `master`). Diseño original:

- crear `libreriaweb_demo` como schema descartable y fijo, con una guarda
  equivalente a `TestDatabaseInitializer` que impida ejecutar el seeder
  contra cualquier otro schema;
- `DemoDataSeeder`: agregar usuarios ADMIN y USUARIO demo, con contraseñas
  externalizadas (nunca hardcodeadas en el código);
- reemplazar `crearLibroConStockCero` por un préstamo demo real vía
  `ServicePrestamo.solicitar()`, no manipulación directa de contadores;
- no tocar `libreriaweb` histórica durante todo el desarrollo de P16.

La migración de `libreriaweb` histórica a V4 queda como **mantenimiento
opcional**, con backup y autorización separada — no bloquea la demo ni la
publicación del proyecto (ver sección 6).

## P16.4 — Navegación, UX, accesibilidad y assets ✅ INTEGRADA

Integrada en `feature/portfolio-publication` (`97567c7`, `--ff-only`; sin push
ni merge a `master`). Los cambios posteriores de interfaz (tema oscuro,
sidebar, listas de gestión) y de seguridad (reglas de ruta con `mvcMatchers`
frente a la barra final, regresión de login con 72 bytes) están guardados en
`d28dbbd`. Alcance original:

Corregir únicamente defectos observables:

- navegación ADMIN ausente en `index.html`, `biblioteca.html` y
  `mis-prestamos.html` (deuda diferida desde P15.5);
- alineación y espaciado;
- formularios y mensajes;
- catálogo y vistas de préstamos;
- responsive básico;
- licencias/`LICENSE` y procedencia de imágenes (identificado en P16.1,
  reubicado aquí por decisión humana).

No realizar un rediseño completo ni cambiar la arquitectura visual.

## P16.5 — Smoke funcional y visual 🟡 VALIDACIÓN FUNCIONAL CONSOLIDADA

Validación funcional consolidada con evidencia separada por tipo: suite
completa (221 tests en ese momento; la suite vigente es de 228, ver sección 1), comprobaciones HTTP sobre la demo y pruebas manuales del
autor (flujo principal, permisos, sidebar, listas de gestión, «Saltar al
contenido» y móvil 390 × 844 en las vistas confirmadas). Capturas
públicas incorporadas (27/10/2026, `docs/img/`, datos ficticios). Pendiente
explícito: **recorrido completo por teclado** (diferido por decisión
del autor; no se declara cumplimiento de WCAG). No se confirmó ningún defecto
en lo diferido. Definición original:

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

## P16.6 — README, diagramas, capturas y defensa técnica 🟡 EN CURSO

README, `docs/diagramas.md` y `docs/DEFENSA_TECNICA.md` (con guiones de
presentación y demo) incorporados y actualizados en el commit de cierre documental.
Los cinco diagramas Mermaid se renderizaron y revisaron localmente (26/10/2026);
se corrigieron las etiquetas `alt` de los diagramas 3 y 4, que una línea vertical
cruzaba, y en el 4 el error se marcó como camino alternativo. Capturas
incorporadas el 27/10/2026: siete archivos en `docs/img/` (cuatro principales y tres
en «Más capturas» del README), con datos ficticios; no se reutilizaron las
anteriores por contener datos personales. Pendiente: comprobación de los diagramas
en GitHub. Incluye
el README y la documentación de portfolio reubicados desde P16.1.

El README debe explicar:

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

No publicar credenciales reales ni instrucciones que dependan de la máquina
de desarrollo.

Diagramas mínimos:

- Controller → Service → Repository → MariaDB;
- entidades y cardinalidades;
- request → validación → transacción → commit/rollback;
- solicitud/devolución de préstamo;
- carrera por el último ejemplar y lock pesimista;
- matriz USUARIO / ADMIN para rutas sensibles.

Capturas mínimas (cubiertas el 27/10/2026 salvo el flujo de préstamo/devolución, que no tiene captura):

- catálogo;
- login o registro;
- Mis préstamos;
- administración de préstamos;
- flujo exitoso de préstamo/devolución.

Defensa técnica a preparar:

- presentación de aproximadamente 2 minutos;
- demo guiada de aproximadamente 5 minutos;
- explicación de JPA/Hibernate y dirty checking;
- transacciones, rollback y locks;
- CSRF, autenticación, autorización e IDOR;
- Flyway y separación de bases;
- estrategia de tests;
- un problema real encontrado y cómo se corrigió.

Lauti debe poder explicar el flujo sin leer el código línea por línea.

## P16.7 — Clon limpio, package, `java -jar` y publicación 🟡 EN CURSO

Sin push ni merge realizados hasta la fecha de este checkpoint.

**Avances (26/10/2026, Windows):**

- Clon limpio de `3120ac5` y JAR construido una vez; gitleaks (historial, 16
  merges incluidos) y OSV-Scanner sobre un SBOM de las 64 bibliotecas
  empaquetadas. Gitleaks: 2 coincidencias, falsos positivos justificados
  (contraseña de fixture en un test). Avisos ≥ 7.0 clasificados uno por uno
  (la clasificación es por condiciones publicadas y revisión del código; no es
  prueba de ausencia). Sin blocker por avisos; Spring Boot 2.7 sigue como deuda
  explícita.
- Flyway V1→V4 desde cero en una instancia MariaDB aislada (puerto 3307, base
  `libreriaweb_demo`) y carga inicial del seeder: correctos.
- **Hallazgo de `java -jar`:** todas las páginas HTML respondían 500 dentro del
  JAR por `th:replace="/fragments/…"` con barra inicial (67 referencias en 18
  plantillas); desde directorio y en MockMvc no se manifestaba. Corregido
  quitando la barra, confirmado con el JAR (500 → 200) y con regresiones
  (`TemplateFragmentReferencesTest`, `scripts/smoke-jar-demo.ps1`). La
  corrección está commiteada en `ac4c77e`.
- **CVE-2026-22732** (Spring Security 5.7.11, corrección solo comercial):
  biblioteca afectada, mitigada con el workaround oficial
  `HeaderWriterFilter.shouldWriteHeadersEagerly=true` en `SecurityConfig`
  (`SecurityHeadersMockMvcTest` + 27 respuestas medidas en el JAR). No se declara
  resuelto. **CVE-2026-40973** (Spring Boot 2.7.18, atacante local en el mismo
  host): riesgo residual aceptado por el autor solo para la demo local, sin
  extender a un despliegue productivo.
- `./mvnw -ntp clean package` con tests: 228 tests, 0 fallos, BUILD SUCCESS (ver
  sección 1 para el origen de la evidencia). JAR en 8081 contra la instancia
  3307: páginas públicas, vistas de ambos roles y cabeceras sin fallos (solo
  lectura y control de acceso; sin altas, bajas ni préstamos por el JAR).

**Verificación final en clon limpio de `ac4c77e` (26/10/2026, Windows):** una
sola corrida de `./mvnw -ntp clean package` con tests, sin perfil demo y solo
sobre las dos bases de test: **228 tests en 17 clases, 0 fallos, 0 errores, 0
omitidos** (suma de los reportes de surefire) y JAR generado. Limitación: en esta
corrida no se recuperó el resultado final de Maven (ni «BUILD SUCCESS» ni el
código de salida); que los tests pasaron y el JAR se generó es lo comprobado.
El JAR del clon **no se ejecutó**; la prueba HTTP corresponde al JAR del
checkout. Comparados ambos JAR, sus 29 recursos de texto distintos en bytes (CSS,
JS, HTML) son idénticos al normalizar únicamente CRLF/LF (0 diferencias
restantes); ninguna clase difiere y las 64 bibliotecas son idénticas por hash.
JAR sin DevTools, sin clases de test y sin `application-local`; 0 plantillas con
barra inicial; sigue empaquetado `show-sql=true` y `thymeleaf.cache=false`
(deuda). gitleaks: las mismas 2 coincidencias (falsos positivos justificados)
y 0 en las clases del JAR; OSV-Scanner: 82 pares aviso/paquete en 19 paquetes, 0
nuevos y 0 ausentes frente a `3120ac5` (base viva, vigente al 26/10/2026). Las
bibliotecas de terceros no se escanearon con gitleaks. Es evidencia dentro del
alcance auditado, no una afirmación de ausencia.

**Limpieza de recursos sin uso (27/10/2026):** se eliminaron del árbol
de trabajo `static/css/styleFormModif.css`, `static/js/validUsuarioModif.js`,
`templates/exito.html` y `static/css/legacy-hero.css`, sin referencias en
`src/`, tests, scripts ni documentos (ningún controlador devuelve la vista
`exito`; los tests usan solo el atributo de modelo/flash `exito`). Se actualizó
`THIRD_PARTY_NOTICES.md`: ninguna vista carga ya Google Fonts. La suite y el JAR
validados corresponden a `ac4c77e`, que aún incluía esos archivos; no se repitió
la suite por este cambio. Validación puntual posterior: `TemplateFragmentReferencesTest`
(sin base de datos), 2 tests, 0 fallos, BUILD SUCCESS, que cubre las referencias de
fragmentos tras la limpieza; los 228 tests y el smoke corresponden a `ac4c77e`. Falta
el smoke del JAR tras el próximo empaquetado. El botón «PDF · Próximamente» y los tres CSS del tema se
conservan por decisión del autor.

**Pendiente manual de apagado:** el JAR de prueba (puerto 8081) y la instancia
MariaDB 3307 siguen encendidos; se detienen a mano (Ctrl+C en la ventana del JAR
y `mysqladmin -h 127.0.0.1 -P 3307 -u root -p shutdown`). La limpieza de
temporales, del clon, de las libs y del datadir 3307 va al final, con
confirmación.

**Pendiente de P16.7:** comprobación de Mermaid en GitHub, revisión acumulada independiente y
publicación solo con autorización. El recorrido completo por teclado sigue
diferido por decisión del autor.

Antes del push final:

- clonar en una carpeta nueva;
- configurar solamente desde archivos de ejemplo/documentación;
- usar una base descartable nueva, nunca `libreriaweb`;
- ejecutar Flyway V1→V4 desde cero;
- cargar datos demo (`libreriaweb_demo`, ver P16.3);
- ejecutar una sola vez `./mvnw -ntp clean package` con Maven Wrapper,
  incluyendo la suite y la generación del paquete;
- ejecutar `java -jar` e iniciar la aplicación;
- probar los flujos principales de ADMIN y USUARIO;
- confirmar `git status` limpio.

La prueba debe demostrar que el proyecto funciona fuera del entorno histórico
de desarrollo, en Windows (la validación en Linux queda como verificación
futura opcional y no bloquea el cierre).

Publicación:

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

Controles obligatorios antes de cualquier push o publicación:

- auditoría de dependencias y CVEs;
- escaneo de secretos sobre todo el historial Git con una herramienta
  dedicada (gitleaks/trufflehog) — la auditoría manual disponible en P16.0
  no encontró secretos reales, pero no reemplaza un scanner real;
- clon limpio y build reproducible en Windows (Linux: validación futura
  opcional, no bloquea el cierre);
- ejecución `java -jar`;
- smoke funcional y visual;
- navegación ADMIN y deudas LOW;
- README, diagramas, capturas y demo.

## Mantenimiento opcional (no bloquea demo ni publicación)

- migración de `libreriaweb` histórica a Flyway V4 — requiere backup previo
  y autorización separada; el portfolio y la demo se validan enteramente
  contra `libreriaweb_demo` (P16.3), no contra la base histórica.

## No bloqueantes

- navegación ADMIN ausente en `index.html`, `biblioteca.html` y
  `mis-prestamos.html` → diferida a P16.4;
- `cambiarRol()` sin ruta HTTP → código muerto, evaluar en backlog/P16.1;
- `UsuarioController` sin null-check para ID inexistente en
  `/usuario/modificar/{id}` → un ADMIN sí puede alcanzar el controller con un
  id inválido y provocar un `500`; deuda LOW de robustez/UX, no
  vulnerabilidad de autorización;
- mejorar el 400 genérico por parámetros ausentes si aporta UX real (ya
  aceptado y documentado en la SPEC de P15, sin cambios);
- limpiar bases temporales históricas cuando ya no hagan falta;
- revisar la cuarentena/mantenimiento de MariaDB;
- adaptar `close-posta` a Gastos Express después de cerrar El Ceibo.

## Deuda ya cerrada

- TOOLING.2: eliminado de `settings.json` el permiso amplio `git add *`.
- TOOLING.3: integración de Codex CLI como segundo runtime de agentes
  junto a Claude Code — no reemplazo — (`AGENTS.md`, contrato compartido
  en `docs/AGENT_CONTRACT.md`, dos agentes `.codex/agents/*.toml`, límite
  de concurrencia, smoke real PASS). Commit `9854b3f`.

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
2. la demo funciona enteramente sobre `libreriaweb_demo` (P16.3); la
   migración de `libreriaweb` histórica a V4 queda como mantenimiento
   opcional, no como condición de cierre;
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

**El cierre documental está commiteado; la corrección de plantillas y cabeceras y la
verificación en clon limpio son de `ac4c77e`. Apagar a mano el JAR de prueba (8081)
y la instancia 3307. Pendientes transversales: comprobación
de Mermaid en GitHub, revisión acumulada y recorrido completo por teclado
(diferido por decisión del autor). P16 sigue abierta; sin push ni merge a
`master`.**
