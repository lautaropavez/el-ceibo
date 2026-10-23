# P16 — PORTFOLIO Y PUBLICACIÓN

## 1. Identidad y baseline

Estado real al abrir esta SPEC, verificado contra Git, no asumido:

* branch de trabajo: `feature/portfolio-publication`;
* rama base: `master` en `43f9a5361222c5b905dd113b1f7a5e8d41f7654e`
  ("Finalize P15 security roadmap");
* HEAD documental de esta rama: `a7438d77ececf4971a40bb6ec841edb9e38bd6e5`
  ("Define P16 portfolio roadmap");
* P15 completa, validada y mergeada a `master` por fast-forward;
* baseline de tests confirmado: **77 tests en 7 clases**, última suite PASS
  (`./mvnw -ntp clean package`, `BUILD SUCCESS`);
* working tree e índice limpios al comenzar P16;
* **sin remoto configurado** (`git remote -v` vacío): no existe hoy ninguna
  posibilidad de push accidental;
* P16 no introdujo todavía ningún cambio funcional — solo documentación
  (roadmap y esta SPEC).

Esta SPEC define el diseño. No autoriza por sí sola ninguna implementación:
cada subposta requiere su propio Gate A con autorización humana explícita.

## 2. Objetivo de P16

Convertir El Ceibo en un proyecto:

* **reproducible**: un clon limpio, en Windows, puede instalar,
  migrar, testear, empaquetar y ejecutar sin depender del entorno histórico
  de desarrollo (la validación en Linux queda como verificación futura
  opcional y no bloquea el cierre; decisión humana, 2026-10-21);
* **seguro para publicar**: sin credenciales reales trackeadas, con el
  historial escaneado por herramientas dedicadas y las dependencias
  auditadas;
* **demostrable con datos sintéticos**: una base demo descartable, con
  usuarios y préstamos generados por el propio dominio, nunca con datos
  reales ni con la base histórica;
* **visualmente coherente**: navegación consistente por rol, sin defectos
  evidentes en los flujos que se muestran;
* **documentado y defendible**: README, diagramas y capturas que permitan
  explicar las decisiones técnicas en una entrevista sin leer el código
  línea por línea.

P16 **no** agrega funcionalidad de negocio. Corrige findings concretos ya
identificados, cierra deuda de publicación y documenta.

## 3. Invariantes generales

Válidos durante toda la fase, sin excepción:

* **nunca** usar `libreriaweb` para pruebas destructivas ni para datos demo;
* `libreriaweb_testsuite` es exclusivamente para la suite de tests, con las
  guardas de `TestDatabaseInitializer` intactas;
* `libreriaweb_demo` es una base fija, descartable y protegida por una guarda
  exacta de nombre, análoga a la de tests;
* ninguna credencial real trackeada en el repositorio, en ningún commit;
* sin push hasta autorización explícita y separada;
* cada subposta atraviesa Gate A (auditoría/diseño), Gate B (ejecución real
  cuando aplica) y Gate C (stage + commit exactos);
* el merge final a `master` es únicamente `--ff-only`, con Gate D propio;
* cambios mínimos y scopes exactos: nada fuera del scope autorizado de cada
  subposta;
* cualquier finding nuevo se reporta y se detiene — nunca se amplía el scope
  por decisión propia del implementador.

## 4. Findings funcionales pendientes

Los tres findings MEDIUM que surgieron de la auditoría P16.0. **Los tres
quedaron corregidos en P16.2, cerrada técnicamente el 30/09/2026 (ver
sección 6)** — las descripciones A/B/C siguientes se conservan como
registro histórico de la auditoría original, no como findings abiertos.

### A. `alta()`/`baja()` de Libro vs préstamo/devolución

**No se declara esta carrera segura por el lock implícito del `UPDATE` de
InnoDB.** Ese razonamiento es insuficiente: aunque el `UPDATE` final espere el
lock exclusivo de la fila, `ServiceLibro.alta()` y `.baja()` leen el `Libro`
sin lock explícito y Hibernate puede conservar en el contexto de persistencia
una entidad administrada con valores de stock ya obsoletos. Una escritura
posterior desde esa entidad puede sobrescribir columnas modificadas
concurrentemente por `ServicePrestamo.solicitar()`/`.devolver()`.

Diseño de corrección:

* adquirir `PESSIMISTIC_WRITE` sobre el `Libro` **antes** de validar o de
  modificar el campo `alta`, usando el patrón `buscarYBloquearPorId` ya
  existente en `LibroRepository`;
* verificar mediante un test adversarial concurrente real;
* afirmar únicamente invariantes finales serializables — nunca imponer que una
  de las dos operaciones deba fallar siempre.

### B. Autor/Editorial vs crear, modificar o reactivar Libro

`AutorRepository` y `EditorialRepository` no tienen ningún método de lock
pesimista, a diferencia de `UsuarioRepository`, `LibroRepository`,
`PrestamoRepository` y `PortadaRepository`. Las validaciones de "autor activo"
y "editorial activa" se hacen sobre lecturas sin lock, así que una baja
concurrente puede dejar un `Libro` activo asociado a un padre inactivo.

Diseño de corrección:

* agregar `buscarYBloquearPorId` con `PESSIMISTIC_WRITE` a `AutorRepository` y
  `EditorialRepository`, mismo patrón que los repositorios existentes;
* **revalidar el estado del padre después de adquirir el lock**, no antes: la
  lectura previa al lock no sirve como evidencia;
* aplicar el lock en `alta()`, `bajaxAutor()`, `altaxAutor()`,
  `bajaxEditorial()`, `altaxEditorial()`, `crearLibro()` y `modificarLibro()`.

**Orden canónico de catálogo:**

```text
AUTOR → EDITORIAL → LIBRO → PORTADA
```

No se afirma que el orden entre Autor y Editorial sea indiferente:
`crearLibro()` y `modificarLibro()` pueden necesitar bloquear **ambos** en la
misma transacción, por lo que el orden entre ellos debe respetarse siempre.

Órdenes existentes que se conservan sin cambios:

```text
USUARIO  → LIBRO
PRESTAMO → LIBRO
```

Ninguna operación puede adquirir los mismos recursos en orden inverso al
declarado acá.

### C. `modificarLibro()` y la constraint única

`UK_libro_titulo_autor` puede violarse por concurrencia. Hoy
`modificarLibro()` solo captura `PessimisticLockingFailureException`.

**No alcanza con envolver `repository.save()`**: Hibernate puede diferir el
`UPDATE` hasta el flush o el commit, es decir, fuera del bloque `try` del
método, con lo que la excepción escaparía sin manejo y terminaría en un `500`.

Diseño de corrección:

* usar `saveAndFlush()` **dentro** del bloque controlado, para forzar que el
  SQL se ejecute en el punto donde el `catch` sí aplica;
* capturar `DataIntegrityViolationException` ahí mismo;
* convertirla en `MiExcepcion` con un mensaje de dominio, coherente con el que
  ya usa `crearLibro()`;
* confirmar mediante test que ese camino nunca termina en `500`.

## 5. Estrategia de tests

* baseline inicial: **77 tests en 7 clases**;
* **no se fija todavía un total futuro.** El número exacto de tests nuevos se
  define en el Gate A de P16.2, después de auditar qué casos aportan evidencia
  real y cuáles serían decorativos;
* las pruebas de concurrencia usan datos sintéticos exclusivamente en
  `libreriaweb_testsuite`;
* los hilos se coordinan de forma determinista (barreras/latches), no con
  `sleep` ni con esperanza de que el scheduler colabore;
* se verifican **invariantes finales** del sistema tras la carrera; no se
  exige artificialmente que uno de los dos actores falle siempre — ambos
  resultados serializables son aceptables mientras el estado final sea
  consistente;
* se conservan intactas las pruebas de regresión de préstamos ya cerradas en
  P13/P14;
* **ninguna prueba toca `libreriaweb`**, en ninguna circunstancia.

## 6. Subpostas canónicas

### P16.0 — Baseline canónico y SPEC

**Objetivo:** fijar el estado real de partida y el diseño de toda la fase.
**Scope conceptual:** `ROADMAP_EL_CEIBO.md` y esta SPEC. Sin código.
**Acceptance criteria:** roadmap coherente con Git; SPEC completa; findings
registrados sin declararlos resueltos ni seguros.
**Validación:** revisión documental independiente; `git diff --check`.
**Fuera de scope:** cualquier cambio de código, configuración o tests.
**STOP conditions:** cualquier discrepancia entre el documento y el estado
real de Git.

### P16.1 — Dependencias, secretos, wrapper y licencias

**Objetivo:** dejar el repositorio en condiciones de ser publicado.
**Scope conceptual:** `.gitattributes` (nuevo), `maven-wrapper.properties`,
`pom.xml`, README/CLAUDE.md si corresponde. Licencias y assets reubicados
a P16.4 por decisión humana (ver sección 9).
**Acceptance criteria:** ver reglas anticipadas en la sección 7.
**Validación:** `test-compile`; SCA ejecutada; scanner de secretos ejecutado;
revisión independiente.
**Fuera de scope:** cambios funcionales; upgrade a Spring Boot 3.
**STOP conditions:** la SCA revela un CVE explotable que exija un upgrade
mayor; aparece un secreto real en el historial.

### P16.2 — Concurrencia, integridad y robustez ✅ CERRADA TÉCNICAMENTE (30/09/2026)

**Objetivo:** corregir los tres findings MEDIUM de la sección 4.
**Scope conceptual:** `AutorRepository`, `EditorialRepository`,
`ServiceLibro`, y los tests de concurrencia nuevos.
**Acceptance criteria:** locks en el orden canónico; revalidación posterior al
lock; `modificarLibro()` sin caminos a `500`; suite completa PASS.
**Validación:** `test-compile`; revisión independiente; Gate B con la suite
real contra `libreriaweb_testsuite`.
**Fuera de scope:** refactors del dominio sin finding; tocar `ServicePrestamo`
más allá de lo que exija la corrección.
**STOP conditions:** aparece un deadlock reproducible; la corrección exige
cambiar migraciones o entidades. Ninguna condición se disparó durante la
implementación real.

**Resultado final (12 commits, HEAD técnico
`8de0cbc63432d1569c0afe0a2621cebd72136bf6`):**

* BCrypt: `encode()`/`matches()` fail closed sobre 72 bytes UTF-8, sin
  delegar al encoder real cuando se excede el límite (ver Delta A);
* orden canónico `AUTOR → EDITORIAL → LIBRO(S)` implementado sin inversión
  vigente; libros múltiples siempre bloqueados por id ascendente;
* frescura Hibernate: toda lectura preliminar no autoritativa quedó como
  proyección escalar, nunca entity-typed, antes de su propio
  `buscarYBloquearPorId`;
* revalidaciones autoritativas bajo lock en `alta()`, `modificarLibro()`
  (decisión de "sin cambios" única, después de los tres locks) y en las
  bajas masivas de Autor/Editorial;
* matriz real de concurrencia: 11 casos con hilos reales, sin `sleep`,
  detección determinista de `LOCK WAIT` vía `INNODB_LOCK_WAITS`/
  `INNODB_TRX`, lectura final por JDBC directo, limpieza sin enmascarar la
  causa original (`causaPrincipal`/`addSuppressed`);
* `spring.jpa.open-in-view=false` efectivo (ausencia confirmada del bean
  `OpenEntityManagerInViewInterceptor`), con smoke de las seis rutas web
  afectadas;
* **criterio cumplido:** los **11 métodos públicos** de `ServiceLibro` que
  adquieren `PESSIMISTIC_WRITE` (`crearLibro`, `modificarLibro`, `alta`,
  `baja`, `altaxAutor`, `bajaxAutor`, `altaxEditorial`, `bajaxEditorial`,
  `eliminarLibro`, `eliminarAutor`, `eliminarEditorial`) traducen
  `PessimisticLockingFailureException` a `MiExcepcion` controlada — ninguno
  deja escapar la excepción cruda de Spring, y el flush/`saveAndFlush`/
  `delete`+`flush` de cada uno ocurre dentro del mismo `try` que cubre el
  lock.

**Evidencia:** review acumulado principal PASS; `code-reviewer` independiente
PASS; baseline fresco **126 tests, 0 failures, 0 errors, 0 skipped, en 11
clases** Surefire; Flyway V1→V4 con `success = 1` exclusivamente en
`libreriaweb_testsuite`; `libreriaweb` normal no fue objetivo de ninguna
conexión ni operación. No se ejecutó `clean package`, generación de JAR ni
clon limpio en esta posta — esas comprobaciones corresponden a P16.7.

**Deudas diferidas, no reabiertas:** reset administrativo de claves legacy
>72 bytes (ver Delta A); dialecto Hibernate conservado por decisión humana;
`spring.jpa.show-sql=true` (P16.6); carga EAGER de `Portada.contenido`
(P16.4); observaciones preexistentes de `ServicePrestamo` (P13) y de la
protección del último ADMIN en `ServiceUsuario`, fuera de este scope.

**Delta A — control compensatorio BCrypt (CVE-2025-22228):** política
fail closed. Toda contraseña que supere 72 bytes UTF-8 se rechaza,
incluida cualquier contraseña legacy — sin fallback al BCrypt vulnerable,
sin truncar ni prehashear. No se implementa ahora un flujo nuevo de
recuperación; una cuenta legacy afectada requerirá un restablecimiento
administrativo controlado, autorizado por separado, antes de publicar.
No se intenta detectar cuentas afectadas inspeccionando hashes (BCrypt no
conserva la longitud original). Decisión humana, 2026-09-11.

**Siguiente posta:** P16.3 — Base y datos demo seguros.

### P16.3 — Base y datos demo seguros

**Objetivo:** una demo reproducible que nunca toque datos reales.
**Scope conceptual:** `DemoDataSeeder`, configuración del profile demo, guarda
de schema.
**Acceptance criteria:** ver sección 8.
**Validación:** Gate B propio contra `libreriaweb_demo`; verificación de que
la guarda aborta si el schema no es el esperado.
**Fuera de scope:** migrar `libreriaweb` histórica.
**STOP conditions:** el seeder podría alcanzar cualquier schema distinto de
`libreriaweb_demo`.

### P16.4 — Navegación, UX, accesibilidad y assets

**Objetivo:** coherencia visual y de navegación por rol.
**Scope conceptual:** templates y CSS; navegación ADMIN ausente en
`index.html`, `biblioteca.html` y `mis-prestamos.html` (deuda diferida desde
P15.5); licencias y assets reubicados aquí desde P16.1 por decisión
humana (ver sección 9).
**Acceptance criteria:** cada rol ve una navegación coherente con sus
privilegios reales de backend; sin regresiones de seguridad.
**Validación:** `test-compile`; suite PASS; revisión independiente.
**Fuera de scope:** rediseño visual completo; cambio de framework CSS.
**STOP conditions:** un cambio visual exigiría tocar reglas de autorización.

### P16.5 — Smoke funcional y visual

**Objetivo:** validar integración real en navegador, contra la base demo.
**Scope conceptual:** ejecución manual documentada; sin cambios de código
salvo findings concretos.
**Acceptance criteria:** catálogo, registro, login ADMIN y USUARIO, logout
POST, solicitud, mis préstamos, listado ADMIN, devolución, permisos por URL
directa, mensajes de éxito/error, navbar/sidebar/responsive.
**Validación:** checklist completo, con evidencia de cada paso.
**Fuera de scope:** repetir la matriz automatizada.
**STOP conditions:** aparece un defecto funcional no cubierto por la suite.

### P16.6 — README, diagramas, capturas y defensa técnica

**Objetivo:** documentación que explique el sistema sin el código delante.
**Scope conceptual:** `README.md`, diagramas, capturas, guion de defensa.
**Acceptance criteria:** README cubre problema, roles, stack, arquitectura,
modelo de datos, reglas de préstamo, transacciones y locks, seguridad y CSRF,
Flyway V1–V4, variables de entorno, ejecución con wrapper, datos demo, tests,
decisiones y límites.
**Validación:** revisión documental independiente; coherencia con el código
real.
**Fuera de scope:** documentar features inexistentes.
**STOP conditions:** el README requeriría afirmar algo no verificable.

### P16.7 — Clon limpio, package, `java -jar` y publicación

**Objetivo:** demostrar reproducibilidad y publicar.
**Scope conceptual:** validación en carpeta nueva; ejecución del artefacto;
publicación autorizada.
**Acceptance criteria:** clon limpio en Windows (Linux: validación futura
opcional, no bloquea el cierre; decisión humana, 2026-10-21); Flyway V1→V4
desde cero; `clean package` PASS; `java -jar` funcional contra el entorno
demo; `git status` limpio; sin secretos.
**Validación:** ejecución real con Gate B; revisión final acumulada.
**Fuera de scope:** despliegue en la nube; CI/CD.
**STOP conditions:** el clon limpio falla; aparece cualquier secreto.

## 7. P16.1 — reglas anticipadas

* agregar `.gitattributes` para proteger `mvnw` con finales de línea LF (hoy
  no existe, `core.autocrlf=true` está activo y `mvnw` no tiene ninguna
  protección: un clon limpio puede romper su shebang);
* el checksum del Maven Wrapper (`distributionSha256Sum`) debe obtenerse
  **únicamente de una fuente oficial de Apache Maven**;
* **no inventar ni copiar un hash sin verificarlo** contra esa fuente;
* cambiar `mysql:mysql-connector-java` por `com.mysql:mysql-connector-j`;
* permitir que Spring Boot 2.7.18 gestione la versión compatible del conector;
* **no actualizar arbitrariamente a la versión más nueva** disponible;
* eliminar `spring-boot-starter-mail` (y evaluar `ServiceNotificacion`, hoy un
  stub vacío) **solo tras reconfirmar** que no tienen ningún consumidor;
* ejecutar SCA de dependencias y analizar **alcance y explotabilidad real**
  antes de decidir cualquier upgrade;
* registrar Spring Boot 2.7 como **deuda tecnológica** explícita (línea sin
  soporte activo de la comunidad);
* **no** migrar a Spring Boot 3 dentro de P16, salvo que la SCA demuestre un
  blocker concreto; si eso ocurre, se estima y autoriza por separado;
* escaneo dedicado de secretos sobre working tree **e historial completo**
  antes de cualquier push;
* la instalación de herramientas (gitleaks, trufflehog, plugins de SCA)
  requiere autorización separada;
* **no afirmar "cero secretos" ni "cero vulnerabilidades"**: la fórmula
  correcta es "no encontrados dentro del alcance auditado".

## 8. Demo segura

* base fija `libreriaweb_demo`, con el nombre como constante del código;
* **guarda exacta del nombre antes de sembrar**, que aborte si el schema
  destino no coincide — análoga a `assertSafeTestDatabaseName`;
* **el profile `demo` no alcanza por sí solo**: solo controla si el bean se
  instancia, no contra qué base apunta el `DataSource`;
* credenciales demo provistas por entorno, **nunca hardcodeadas** en el
  código ni trackeadas;
* al menos un usuario **ADMIN** y un usuario **USUARIO**, para poder demostrar
  ambos flujos;
* el préstamo demo se crea mediante el **flujo real de dominio**
  (`ServicePrestamo.solicitar()`);
* **no fabricar stock prestado manipulando contadores** — el seeder actual
  hace exactamente eso en `crearLibroConStockCero`, dejando un
  `ejemplaresPrestados` sin ningún `Prestamo` que lo respalde;
* Flyway V1→V4 aplicado desde cero sobre la base demo;
* migrar `libreriaweb` histórica a V4 queda como **mantenimiento opcional**,
  con backup previo y autorización separada — **no bloquea** la publicación.

## 9. Assets y licencias

* **no asumir** que `ceibo.svg` ni las demás imágenes son propias sin
  confirmar su procedencia;
* resolver antes del push las **8 portadas de libros comerciales**
  (`portadas-demo/`) y cualquier imagen con watermark o de banco de imágenes
  sin licencia clara;
* decidir explícitamente la **licencia del repositorio**;
* crear atribuciones (`THIRD_PARTY_NOTICES` o equivalente) cuando corresponda;
* **ningún asset sin procedencia puede quedar presentado como propio**.

## 10. Definición de terminado

P16 queda cerrada únicamente cuando:

1. los tres findings funcionales están corregidos y probados;
2. suite y `package` PASS;
3. la demo descartable es reproducible desde cero;
4. el smoke funcional y visual está completo y documentado;
5. navegación revisada y accesibilidad parcialmente validada (teclado en
   login, foco visible y etiquetas de formulario); el recorrido completo por
   teclado queda diferido por decisión del autor (2026-10-23). No se declara
   cumplimiento de WCAG;
6. README, diagramas y capturas coherentes con el código real;
7. assets y licencias resueltos;
8. escaneo de dependencias y de secretos ejecutado con herramientas
   dedicadas;
9. clon limpio validado;
10. `java -jar` probado **exclusivamente contra el entorno demo**;
11. revisión acumulada independiente PASS;
12. publicación autorizada explícitamente por el humano.

## 11. Fuera de scope

* migración completa a Spring Boot 3;
* funcionalidades nuevas de negocio;
* notificaciones por email;
* nube, despliegue productivo o CI/CD complejo;
* migración automática de la base normal `libreriaweb`;
* refactors arquitectónicos sin un finding concreto que los justifique.
