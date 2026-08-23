# P14 — TESTS PERMANENTES

## 1. Objetivo

Construir una red de regresión útil y reproducible para el código ya
cerrado del proyecto (P1–P13). No se persigue un porcentaje de
cobertura: cada test debe proteger un comportamiento real, no
existir para subir un número.

## 2. Baseline ya cerrado (P14.1 — commit `657e155`)

Lo siguiente **ya está implementado y no se rediseña** en las
subpostas siguientes. Se documenta como evidencia vigente:

* Maven Wrapper oficial (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`),
  tipo `only-script`, Maven `3.9.16`, sin `.jar` commiteado, verificado
  ejecutable en Git Bash y en `cmd.exe` nativo de Windows.
* Perfil Spring `test`, activado explícitamente vía `@ActiveProfiles("test")`
  (no existe `application.properties` sin sufijo en `src/test/resources`,
  a propósito, para no ocultar el `application.properties` de `main`).
* Base de datos de test fija e inmodificable por configuración externa:
  `libreriaweb_testsuite`, nunca `libreriaweb`.
* `TestDatabaseInitializer` (`ApplicationContextInitializer`) recrea la
  base (`DROP DATABASE IF EXISTS` + `CREATE DATABASE`) antes del
  refresh del contexto, con guardas de nombre fijo y perfil `test`
  repetidas inmediatamente antes del DROP, y con la URL/usuario/password
  de test inyectados con precedencia máxima (`PropertySource.addFirst`)
  para que ninguna variable externa (`SPRING_DATASOURCE_URL`, `DB_URL`,
  etc.) pueda redirigir el DataSource de test — revisado y aprobado por
  `code-reviewer` independiente.
* Flyway aplica `V1→V4` reales sobre `libreriaweb_testsuite` en cada
  corrida (mismas migraciones que producción, sin copia de esquema).
* Hibernate en `ddl-auto=validate` también en el perfil `test`.
* `libreriaweb` (base normal) nunca fue usada ni tocada durante el
  diseño ni la ejecución de P14.1 — verificado en el preflight y en
  los post-checks de las dos corridas de validación.

Comando de referencia: `./mvnw test` (Git Bash) / `mvnw.cmd test`
(Windows nativo).

## 3. P14.2 — Comportamiento de préstamos

Integración real contra `libreriaweb_testsuite`, sobre `ServicePrestamo`
(`solicitar`, `devolver`, `misPrestamos`, `listarTodos`). Casos exigidos:

* solicitud válida: crea el préstamo, `fechaVencimiento = fechaPrestamo + 14`
  días exactos, `fechaDevolucion` nula;
* stock e invariante `nroEjemplares = ejemplaresRestantes + ejemplaresPrestados`
  verificada en cada caso relevante, nunca solo el cambio individual;
* stock 0 rechaza la solicitud sin modificar nada;
* máximo 3 préstamos **activos** por usuario — un préstamo ya devuelto
  no cuenta para el cupo;
* duplicado solo se evalúa sobre el préstamo **activo**: después de
  devolver, el mismo usuario puede volver a solicitar el mismo libro;
* `Usuario.cantPrestamos` no es fuente de verdad — un valor legacy o
  desactualizado en ese campo no debe alterar el conteo real;
* usuario o libro dados de baja bloquean **solicitudes nuevas**;
* devolución válida: fija `fechaDevolucion`, restaura stock, conserva
  el registro (nunca se borra);
* doble devolución secuencial: la segunda se rechaza sin restaurar
  stock una segunda vez;
* devolución permitida aunque el usuario o el libro estén dados de
  baja después de crearse el préstamo;
* listados (`listarTodos`, `misPrestamos`) muestran activos y
  devueltos.

Fuera de esta subposta: controllers, templates, roles/IDOR/CSRF, y las
carreras concurrentes (ver punto 7).

## 4. P14.3 — Usuarios y catálogo

* modificar un usuario sin enviar password nueva conserva el hash
  existente;
* email duplicado sigue rechazado de forma controlada (regresión de
  P12, no una funcionalidad nueva);
* invariantes de catálogo (Libro/Autor/Editorial) ya estabilizadas en
  P12.5 que aporten protección real de regresión — sin repetir cada
  caso ya cubierto manualmente en su momento si no hay riesgo
  concreto de que se rompa.

## 5. P14.4 — MockMvc / capa web

Con `spring-security-test` (`@WithMockUser`, `SecurityMockMvcRequestPostProcessors`):

* anónimo, `USUARIO` y `ADMIN` sobre las rutas relevantes de cada
  controller;
* `403` en cruces de rol, `redirect` a `/login` para anónimo en rutas
  protegidas;
* IDOR:
  * en operaciones propias del `USUARIO`, la identidad debe derivar de
    `Authentication` y no de un `usuarioId` manipulable;
  * los endpoints `ADMIN` pueden recibir identificadores de terceros
    únicamente cuando la ruta y el rol lo autorizan;
  * ningún usuario puede consultar o modificar recursos ajenos sin
    autorización;
* patrón PRG (flash `exito`/`error`) donde ya existe;
* errores de negocio controlados (`MiExcepcion`), nunca un 500 evitable
  ante una entrada inválida conocida.

**Aclaración obligatoria:** durante toda P14, CSRF sigue deshabilitado
(deuda conocida, ver `SecurityConfig`). Los tests de P14.4 no
verifican CSRF — esa verificación activa es exclusiva de P15.

## 6. Estrategia técnica

* Integración contra MariaDB real (`libreriaweb_testsuite`), nunca H2.
* Aislamiento entre tests — **decisión cerrada en P14.2**:
  * `ServicePrestamoIntegrationTest` no usa ninguna transacción
    administrada por Spring Test (sin `@Transactional` de test);
  * `ServicePrestamo` conserva su ciclo transaccional real
    begin→commit/rollback exactamente como en producción;
  * el aislamiento entre tests se logra con fixtures únicas y
    deterministas por caso, no con rollback automático;
  * ningún test supone que la base está vacía: los datos persisten
    durante toda la vida de cada `ApplicationContext` de la suite;
  * no usar `DELETE` masivos, scripts de limpieza ni `@DirtiesContext`
    salvo necesidad demostrada.
* Fixtures mínimas: solo los campos realmente exigidos por las
  constraints de V1–V4 y por las validaciones de cada service, sin
  datos decorativos.
* `@LibreriaIntegrationTest` (anotación compuesta) agrupa:
  * `@SpringBootTest`;
  * `@ActiveProfiles("test")`;
  * `@ContextConfiguration(initializers = TestDatabaseInitializer.class)`.
  La anotación compuesta no incluye `@Transactional`.
* Tests deterministas, sin dependencia del orden de ejecución entre
  clases ni entre métodos.

## 7. Evidencia reutilizada — no se repite en la suite regular

Las tres carreras concurrentes ya cerradas en P13 (última unidad con
stock=1, cupo máximo de 3 entre libros distintos, doble devolución
concurrente) **no entran en la suite de `mvnw test`**. Se repiten de
forma dirigida únicamente si cambia `solicitar()`, `devolver()`, los
locks o las queries relacionadas.

## 8. Fuera de scope de P14

* H2, Testcontainers, Docker;
* cobertura porcentual como objetivo;
* refactors de código productivo sin un bug real demostrado por un
  test;
* CSRF (activación real es P15);
* funcionalidades nuevas de producto.

## 9. Criterios de cierre

* `./mvnw test` reproducible, en limpio, sin intervención manual;
* suite estable (sin tests flaky conocidos);
* review independiente (`code-reviewer`) sin findings bloqueantes;
* commit y merge controlados, con aprobación humana explícita en cada
  paso, igual que el resto del proyecto.
