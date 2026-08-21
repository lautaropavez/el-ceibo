# P15 — SEGURIDAD FINAL

## 1. Objetivo

Seguridad real y demostrable para una aplicación MVC con sesión
(Java 8, Spring Boot 2.7.18), sin infraestructura enterprise
innecesaria. No se persigue un checklist genérico de "buenas
prácticas": cada cambio de esta posta cierra una deuda concreta
identificada en la auditoría P15.0/P15.0B.

P15 se implementa después de cerrar P14. La rama completa de P15
solo puede mergearse a `master` una vez cerrada P15.6 — no hay merges
parciales de subpostas intermedias, porque con CSRF a medio activar
la aplicación queda en un estado inconsistente (algunos POST
protegidos, otros no).

## 2. Baseline P15.1 — inventario cerrado

Estado relevado y cerrado por la auditoría previa, no se vuelve a
auditar salvo hallazgo nuevo concreto durante la implementación:

* CSRF actualmente deshabilitado (`SecurityConfig.java:38`,
  `.csrf(csrf -> csrf.disable())`).
* Autorización backend vigente y correctamente ordenada: reglas por
  `antMatchers` evaluadas en orden de declaración, sin rutas
  antiguas expuestas de más (`SecurityConfig.java:39-91`).
* 9 endpoints POST actuales: `/logincheck`, `/registrar` (alta de
  usuario), `/libro/registrar`, `/libro/modificar/{id}`,
  `/autor/registrarAutor`, `/editorial/registrarEditorial`,
  `/usuario/modificar/{id}`, `/prestamo/solicitar`,
  `/prestamo/devolver`.
* 12 endpoints mutadores que todavía usan GET (tabla abajo).
* 1 endpoint de logout (`/logout`) que debe pasar a exigir POST al
  activarse CSRF.
* Total final esperado: **22 endpoints u operaciones HTTP** que
  modifican estado de negocio o cambian el estado de autenticación
  de la sesión (9 + 12 + 1 logout). Esto **no** equivale a 22
  formularios: el logout es un único endpoint invocado hoy desde 4
  ubicaciones de template distintas (ver más abajo), y no hay
  relación 1:1 entre "cantidad de `<form>`" y "cantidad de
  endpoints".
* 7 de los 9 forms POST actuales ya usan `th:action`: `nuevoLibro.html`,
  `nuevoAutor.html`, `nuevaEditorial.html`, `modif-Libro.html`,
  `modif-Usuario.html`, `biblioteca.html` (form de `/prestamo/solicitar`),
  `list-prestamo.html` (form de `/prestamo/devolver`).
* `login.html` (`action="/logincheck"`) y `registro.html`
  (`action="/registrar"`) todavía usan `action` literal, no
  `th:action`.
* 4 enlaces visuales ejecutan logout mediante GET (`<a href="/logout">`):
  `fragments/sidebar.html:57`, `mis-prestamos.html:34`, `index.html:40`,
  `biblioteca.html:60`.
* Registro público sin mass-assignment: `MainController.registrar()`
  (`MainController.java:43`) solo declara `nombre, apellido, mail,
  clave1, clave2` como `@RequestParam`; `rol`, `alta` y `baja` se
  fijan server-side en `ServiceUsuario.registrar()`
  (`ServiceUsuario.java:54,57,58`) sin derivar de ningún parámetro de
  entrada. No existe superficie de escalada de rol vía `/registrar`.
* Navegación ADMIN ya protegida visualmente: los enlaces "Libros",
  "Autores", "Editoriales", "Usuarios" están envueltos en
  `<th:block sec:authorize="hasRole('ADMIN')">` (`fragments/sidebar.html:16-42`).
* Las páginas que incluyen ese sidebar (10 templates: listas de
  catálogo/usuarios/préstamos y sus formularios de alta/edición) ya
  están protegidas también en el backend bajo `hasRole("ADMIN")`, por
  lo que la ocultación visual nunca es la única defensa en esos casos.

**Tabla de los 12 endpoints GET actuales a convertir:**

| Endpoint | Rol requerido (backend) | Conversión |
|---|---|---|
| `/autor/alta/{id}` | ADMIN | GET → POST |
| `/autor/baja/{id}` | ADMIN | GET → POST |
| `/autor/eliminar/{id}` | ADMIN | GET → POST |
| `/editorial/alta/{id}` | ADMIN | GET → POST |
| `/editorial/baja/{id}` | ADMIN | GET → POST |
| `/editorial/eliminar/{id}` | ADMIN | GET → POST |
| `/libro/alta/{id}` | ADMIN | GET → POST |
| `/libro/baja/{id}` | ADMIN | GET → POST |
| `/libro/eliminar/{id}` | ADMIN | GET → POST |
| `/usuario/alta/{id}` | ADMIN | GET → POST |
| `/usuario/baja/{id}` | ADMIN | GET → POST |
| `/usuario/eliminar/{id}` | ADMIN | GET → POST |

## 3. P15.2 — Mutaciones GET→POST

Alcance:

* cambiar únicamente los `@GetMapping`→`@PostMapping` de los 12
  endpoints de la tabla anterior y los controles HTML necesarios para
  invocarlos;
* controllers afectados: `AutorController`, `EditorialController`,
  `LibroController`, `UsuarioController`;
* templates afectados: `list-autor.html`, `list-editorial.html`,
  `list-libro.html`, `list-usuario.html`;
* cada enlace `<a th:href="...">` de alta/baja/eliminar pasa a ser un
  `<form method="post" th:action="...">` con su botón;
* no usar JavaScript ni method override (`_method=DELETE`, etc.);
* no cambiar lógica de negocio, redirects, mensajes flash ni
  services — el cambio es exclusivamente de verbo HTTP y de marcado;
* las reglas actuales de `SecurityConfig` protegen por *path*, no por
  verbo, así que no requieren duplicarse ni reescribirse para esta
  subposta.

Criterios de aceptación:

* cada uno de los 12 GET deja de ejecutar la operación;
* como ADMIN, el GET al mismo path debe terminar sin mutación —
  preferentemente `405 Method Not Allowed`; el status real se
  confirma durante la implementación, no se asume de antemano;
* cada POST conserva el comportamiento funcional actual (mismo
  resultado que el GET reemplazado);
* USUARIO y anónimo siguen bloqueados por backend en los 12 endpoints,
  igual que hoy.

No se afirma que estos comportamientos ya estén probados por P13/P14:
esas matrices cubrieron préstamos, no catálogo ni usuarios. P15.2
requiere sus propias pruebas dirigidas (ver sección 8).

## 4. P15.3 — Preparar formularios para CSRF

Alcance:

* `login.html`: `action="/logincheck"` (literal) → `th:action="@{/logincheck}"`;
* `registro.html`: `action="/registrar"` (literal) → `th:action="@{/registrar}"`;
* convertir los cuatro enlaces `<a href="/logout">` en forms POST con
  `th:action="@{/logout}"`, en: `fragments/sidebar.html`,
  `mis-prestamos.html`, `index.html`, `biblioteca.html`;
* los 7 forms que ya usan `th:action` quedan sin cambios;
* confiar en `RequestDataValueProcessor` (auto-registrado por Spring
  Security cuando hay CSRF habilitado y Spring MVC + Thymeleaf en
  classpath) para la inserción automática del input `_csrf` oculto en
  cualquier `<form th:action="...">`;
* no agregar inputs `_csrf` manuales, salvo evidencia real durante la
  implementación de que la integración automática no inserta el
  token en algún caso puntual;
* no agregar `logoutRequestMatcher` personalizado: el comportamiento
  default de `LogoutConfigurer` ya es el correcto (ver sección 5).

CSRF continúa deshabilitado durante toda esta subposta — el único
objetivo es dejar la UI lista para que la activación en P15.4 no
rompa ningún flujo.

## 5. P15.4 — Activar CSRF y adaptar tests

Cambio de configuración:

* eliminar `.csrf(csrf -> csrf.disable())` de `SecurityConfig.java:38`;
* eliminar o corregir el comentario desactualizado de
  `SecurityConfig.java:32-37` (afirma que los forms usan `action`
  plano, lo cual ya no es cierto para 7 de 9 forms tras P13/P14);
* usar la protección CSRF por defecto de Spring Security, sin
  exclusiones de endpoints;
* no usar `CookieCsrfTokenRepository` ni ninguna configuración
  personalizada adicional — el repositorio de sesión por defecto
  alcanza para una app MVC con sesión;
* `logoutUrl("/logout")` pasa a exigir POST automáticamente al
  activarse CSRF, sin tocar `SecurityConfig` — es el comportamiento
  default de `LogoutConfigurer` (con CSRF deshabilitado acepta
  `/logout` por cualquier método; con CSRF habilitado, solo POST).

Aclaración obligatoria sobre `permitAll()`: que una ruta esté marcada
`permitAll()` en `SecurityConfig` (p. ej. `/logincheck`, `/registrar`)
significa que se puede acceder **sin autenticación** — no exime a un
POST de la protección CSRF. `/logincheck` y `/registrar` deben seguir
siendo públicos en cuanto a autenticación, pero deben requerir un
token CSRF válido igual que cualquier otro POST.

Tests — cambios de configuración:

* agregar `.with(csrf())` a todos los POST positivos ya existentes en
  la suite de P14.4, para que sigan pasando con la protección activa;
* comprobar ausencia de token en cinco categorías representativas, no
  en los 22 endpoints: `/registrar` (público), `/logincheck`
  (público), `/prestamo/solicitar` (USUARIO), una mutación ADMIN de
  catálogo (p. ej. `/libro/eliminar/{id}` tras P15.2), y `/logout`;
* comprobar al menos un caso de token inválido (valor no vacío pero
  incorrecto), no solo ausencia de token;
* comprobar que `POST /logout` con token válido cierra la sesión;
* comprobar que `GET /logout` ya no cierra la sesión;
* no repetir la misma prueba negativa de CSRF para los 22 endpoints —
  las cinco categorías representativas alcanzan porque la protección
  es transversal (filtro único), no por-endpoint.

Distinción obligatoria entre dos niveles de prueba distintos:

* `.with(csrf())` en un test de MockMvc demuestra que el filtro de
  Spring Security **acepta** una request que ya trae un token válido
  — no demuestra que el navegador real vaya a tener ese token
  disponible;
* que el template se **renderice** con un input `_csrf` real es lo
  que demuestra que el formulario, tal como lo recibe el navegador,
  efectivamente entrega ese token al hacer submit.

Por eso, además de los tests de aceptación/rechazo de CSRF, P15.4
exige una comprobación automatizada (o smoke dirigido documentado, si
no se automatiza) de que aparece un input `_csrf` en el HTML
renderizado de: login, registro, al menos un form ADMIN convertido
desde GET en P15.2, el form de préstamo de USUARIO, y el form de
logout. No se incorpora Selenium, Playwright ni infraestructura de
navegador solo para esto — `MockMvc` inspeccionando el HTML devuelto
(`content().string(containsString(...))` o equivalente) o un smoke
manual documentado alcanza.

## 6. P15.5 — Roles, navegación y privilegios

Reutilizar la matriz de roles ya diseñada en P14.4 (anónimo, USUARIO,
ADMIN), no rediseñarla.

Verificar:

* mutaciones de catálogo y usuarios: solo ADMIN;
* solicitar préstamo y "mis préstamos": solo USUARIO;
* listar y devolver préstamos: solo ADMIN;
* registro público no puede elegir rol, alta, baja ni ningún campo
  interno (evidencia ya cerrada en la sección 2, se verifica aquí a
  nivel HTTP con `MockMvc`, no se vuelve a auditar el código fuente);
* la identidad de las operaciones de préstamo continúa derivando de
  `Authentication`, nunca de un parámetro de request
  (`PrestamosController.java:28-29,63-64`);
* ningún botón oculto por `sec:authorize` es la única defensa — todo
  lo que se oculta visualmente ya está bloqueado también en backend;
* los enlaces ADMIN del sidebar siguen ocultos para USUARIO
  (`fragments/sidebar.html:16-42`);
* no se agrega `sec:authorize` redundante dentro de páginas que ya
  son completamente ADMIN por ruta backend (listas de catálogo,
  usuarios y préstamos, y sus formularios).

Comprobación estática adicional, read-only y acotada — salida HTML
sin escape: se buscó `th:utext` (el mecanismo de Thymeleaf que
imprime HTML sin escapar) en todo `src/main/resources/templates/**` y
no se encontró ninguna coincidencia. Se registra como **PASS**
verificado en la etapa de diseño de esta SPEC — P15.5 solo debe
repetir esa búsqueda puntual antes del cierre, sin ampliar el scope.
Si esa búsqueda no encuentra ningún patrón nuevo, no se agrega CSP,
sanitizadores ni dependencias nuevas sin un finding real que lo
justifique.

No se repiten las carreras concurrentes de P13 ni las pruebas
funcionales cuyo código productivo no cambió en P15.

## 7. P15.6 — Cierre

* suite completa `./mvnw test` en limpio;
* review independiente (`code-reviewer`);
* `adversarial-tester` dirigido específicamente a CSRF, métodos HTTP,
  roles y escalada de privilegios — no una pasada genérica;
* diff acumulado de toda la rama `P15`;
* revisión de la rama contra `master` antes de proponer merge;
* no merge hasta que P15.1 a P15.6 estén cerradas por completo;
* STOP y aprobación humana explícita antes de cualquier stage, commit
  o merge, en cada subposta — mismo patrón que P13/P14.

## 8. Estrategia de tests y proporcionalidad

* un test de mapping por cada uno de los 12 endpoints convertidos en
  P15.2 (GET ya no muta, POST conserva el comportamiento);
* tests CSRF negativos representativos (sección 5), no duplicados por
  endpoint;
* reutilización directa de la matriz de roles de P14.4, sin
  rediseñarla;
* smoke funcional de login, registro, catálogo, préstamos y logout
  tras activar CSRF, para confirmar que los flujos legítimos siguen
  funcionando de punta a punta;
* ninguna prueba corre contra `libreriaweb` (base normal);
* toda prueba contra base de datos usa exclusivamente
  `libreriaweb_testsuite`, con la misma infraestructura de P14.1;
* no se repiten las tres carreras concurrentes ya cerradas en P13.

## 9. Fuera de scope y deudas conservadas

**Diferidas a P16:**

* búsqueda rota de `list-usuario.html:17` — el form de búsqueda usa
  `th:action="@{/libros/lista}"` (con "s"), verificado directamente
  contra el archivo del repo; la ruta real de listado de usuarios es
  `/usuario/lista` (sin "s"), por lo que ese buscador apunta a un
  path inexistente. Es un bug funcional preexistente, no una
  vulnerabilidad — se corrige en P16;
* página 500 personalizada y `@ControllerAdvice` global — solo si
  P16 demuestra valor real, no por completitud;
* `spring.jpa.show-sql=true`;
* coordenadas Maven del driver MySQL (`mysql:mysql-connector-java` →
  `com.mysql:mysql-connector-j`);
* dialecto Hibernate (`MySQL5InnoDBDialect`);
* warning de `open-in-view`.

**Fuera del cierre actual (sin fecha comprometida):**

* Docker;
* migración a Java 17;
* upgrade de Spring Boot/Spring Security;
* infraestructura enterprise adicional (gateway, WAF, rate limiting,
  etc.) no justificada por el perfil de portfolio del proyecto.

**Deuda ya cerrada, no pendiente:** `/libro/biblioteca` y su template
`biblioteca.html` existen y funcionan correctamente
(`LibroController.java:94-109`); no requiere acción en P15 ni en P16.

**Deuda LOW aceptada, sin acción:** parámetros `@RequestParam`
obligatorios ausentes en una request manipulada a mano pueden producir
un `400` genérico (`MissingServletRequestParameterException` sin
handler dedicado). No alcanzable vía uso normal de la UI; bajo riesgo,
no bloquea el cierre de P15.

## 10. Criterios de aceptación globales

* ninguna operación que modifique estado de negocio o de
  autenticación es accesible mediante GET;
* los 22 endpoints mutadores quedan bajo protección CSRF;
* requests sin token o con token inválido son rechazadas;
* los flujos legítimos que sí entregan un token válido siguen
  funcionando sin regresión;
* el logout solo es efectivo mediante POST con token válido;
* las reglas de autorización ADMIN/USUARIO/anónimo no sufren
  regresiones respecto al estado actual;
* el registro público sigue sin permitir escalada de privilegios;
* la suite `./mvnw test` es estable, sin tests flaky conocidos;
* no hay cambios fuera del scope definido en esta SPEC;
* P15 solo se mergea a `master` al cerrarse P15.6, no antes.
