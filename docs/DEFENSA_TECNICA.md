# Defensa técnica — Librería Web El Ceibo

Cada sección distingue **qué está implementado** de **qué está validado** y con qué tipo de evidencia (pruebas automáticas, comprobaciones HTTP, pruebas manuales). Lo no validado figura como pendiente.

## 1. Problema y alcance

**Problema.** Gestionar el catálogo de una librería y el préstamo de libros entre usuarios registrados, sin que dos solicitudes simultáneas sobrepasen el stock.

**Alcance.** Acceso anónimo (explora y se registra) y dos roles de seguridad: `USUARIO` (solicita y consulta sus préstamos), `ADMIN` (gestiona catálogo y usuarios, registra devoluciones). Nació como proyecto académico en 2022 y se retomó en 2026 para llevarlo a estado de portfolio.

**Fuera de alcance (declarado):** email, multas, renovaciones, reservas, Docker, CI/CD, despliegue productivo y migración a Spring Boot 3.

## 2. Arquitectura y responsabilidades

- **Implementado:** capas MVC. `controladores/` (HTTP y mensajes), `servicios/` (reglas de negocio y transacciones), `repositorios/` (Spring Data JPA, con consultas `PESSIMISTIC_WRITE`), `entidades/`, `config/` (seguridad, codificador de contraseñas, errores de carga de archivos) y `seed/` (datos demo, solo con el perfil `demo`).
- Vistas Thymeleaf; Spring Security filtra por sesión, CSRF y rol antes de los controladores.
- **Decisión:** la lógica de negocio vive en los servicios, no en los controladores, para que las reglas y las transacciones estén en un solo lugar.
- Diagramas: [`diagramas.md`](diagramas.md) (Mermaid, revisados por lectura; **renderizado pendiente**).

## 3. Modelo de datos y migraciones

- **Entidades:** `Autor`, `Editorial`, `Libro` (con `Portada` opcional), `Usuario`, `Prestamo`.
- **Migraciones Flyway:** V1 línea base; V2 ISBN (`VARCHAR(13)`, único, nulo permitido); V3 integridad de libros (autor obligatorio, portada única, título+autor único); V4 ciclo de vida del préstamo (estado derivado de la fecha de devolución: sin fecha = activo).
- Hibernate en modo `validate`: el esquema solo cambia por migraciones nuevas. Las ya aplicadas son inmutables.
- **Validado:** el arranque real sobre `libreriaweb_demo` aplicó V1–V4 sin errores (ejecución real). Flyway desde cero en un entorno aislado queda pendiente de la verificación de publicación.

## 4. Reglas de préstamo, transacciones, bloqueos y concurrencia

**Reglas implementadas:** máximo 3 préstamos activos por usuario; no se repite un libro activo; vence a los 14 días; solo libros activos con ejemplares y usuarios no dados de baja; la devolución repone el ejemplar y un préstamo devuelto no se devuelve de nuevo.

**Transacciones y bloqueos.** Las operaciones son transaccionales y usan bloqueo pesimista con orden consistente: catálogo `AUTOR → EDITORIAL → LIBRO → PORTADA`; solicitar `USUARIO → LIBRO`; devolver `PRESTAMO → LIBRO`. El orden **reduce** el riesgo de interbloqueo; no lo elimina.

**Fallos de bloqueo (alcance acotado).** Solo `ServicePrestamo` y `ServiceLibro` capturan `PessimisticLockingFailureException`: revierten la transacción y devuelven un mensaje de dominio ("Intente nuevamente"). `ServiceAutor`, `ServiceEditorial`, `ServicePortada` y `ServiceUsuario` **no** tienen esa captura. No se afirma que no haya esperas ni fallos de bloqueo.

**Validado:** tests automáticos de concurrencia de catálogo y préstamos dentro de la suite completa de 228 tests (ver §6). Flujos de solicitud, rechazo de duplicado y devolución: probados manualmente por el autor.

## 5. Seguridad

- **Roles** `ADMIN` y `USUARIO`, con reglas por ruta en `SecurityConfig` (lo no listado exige sesión). El backend refuerza lo que oculta la navegación: escribir la URL no da acceso; responde 403, o redirige al login sin sesión.
- **CSRF activo**; el logout es `POST /logout`.
- **Contraseñas:** BCrypt. Como BCrypt solo considera 72 bytes UTF-8, `BCrypt72BytePasswordEncoder` **rechaza** las claves más largas (error al registrar; "no coincide" al autenticar), sin truncarlas.
- Páginas de error propias (403, 404, 500) sin detalles técnicos. Sin credenciales en el repositorio.
- **Validado:** seguridad por rol y CSRF mediante MockMvc (suite de 228, incluida la corrección de rutas con barra final); los cuatro controles de permisos, probados manualmente por el autor.
- **CVE-2025-22228 (aviso oficial de Spring, 19/03/2025, severidad HIGH):** `BCryptPasswordEncoder.matches` devuelve `true` para contraseñas de más de 72 caracteres si coinciden los primeros 72. Afecta a Spring Security «5.7.15 y anteriores»; la versión efectiva del proyecto es **5.7.11**, o sea **la biblioteca está afectada**. La corrección 5.7.16 solo está disponible con soporte comercial; las correcciones abiertas están en 6.3.8 y 6.4.4 (otra línea, que implicaría Spring Boot 3, fuera del alcance).
  - **Control compensatorio (leído en el código):** `BCrypt72BytePasswordEncoder` mide bytes UTF-8; `encode` lanza `IllegalArgumentException` por encima de 72 bytes y `matches` devuelve `false` sin delegar. `SecurityConfig` expone un único bean `PasswordEncoder` (el envoltorio sobre `BCryptPasswordEncoder`), `ServiceUsuario` lo usa para codificar, y `UsuarioDetailsService` es el único `UserDetailsService`; por lectura, la autenticación de Spring Security usa ese bean (no hay configuración manual del proveedor). El aviso habla de 72 *caracteres* y el control mide *bytes*, que es el límite real de BCrypt y es más estricto con caracteres multibyte.
  - **Cobertura de tests:** `BCrypt72BytePasswordEncoderTest` (6 tests): exactamente 72 bytes acepta y permite autenticar; 73 bytes rechaza; Unicode con menos de 72 caracteres pero más de 72 bytes rechaza; `matches` con entrada sobredimensionada no delega; un candidato que comparte el prefijo válido es rechazado; carácter de 4 bytes cerca del límite. `ServiceUsuarioIntegrationTest`: registrar y modificar con clave sobredimensionada se rechazan sin persistir ni cambiar el hash. Un test de login con 73 caracteres contra un usuario con otra clave (`logincheck_conPasswordSobredimensionada_fallaControladoSinSesion`) no discrimina la causa del rechazo. Por eso se agregó una regresión del login completo en `CsrfProtectionMockMvcTest`: con el encoder y la configuración reales, un usuario con clave de exactamente 72 bytes inicia sesión y el mismo prefijo más un carácter se rechaza sin sesión autenticada, con CSRF válido y sesiones independientes.
  - **Evidencia de esa regresión:** corrida focalizada de `CsrfProtectionMockMvcTest` sobre `libreriaweb_testsuite`: 17 tests, 0 fallos, 0 errores, 0 omitidos. Fue evidencia **separada de la suite completa de 221** de ese momento (la clase aportaba 16 de esos 221 y la regresión es el test 17); la suite vigente de 228 (ver §6) ya incluye los 17 tests de la clase, así que no se suman.
  - **Qué no se afirma:** que la vulnerabilidad de la biblioteca esté corregida (no lo está), ni cumplimiento fuera del alcance revisado. Un escaneo de dependencias seguirá reportando este CVE sobre 5.7.11; se registra como «mitigado por control compensatorio, dentro del alcance revisado». Si se reutiliza una base con usuarios anteriores, las cuentas con claves de más de 72 bytes quedarían rechazadas hasta un restablecimiento administrativo controlado.
  - Fuente: `https://spring.io/security/cve-2025-22228` (consultada el 21/10/2026). Los datos de NVD (CVSS, CWE) no se pudieron contrastar.
- **CVE-2026-22732 (aviso oficial de Spring):** cuando una aplicación Servlet define cabeceras HTTP de respuesta con Spring Security, «es posible que no se escriban»; el aviso menciona riesgos como exponer datos sensibles por cachés. Afecta a «5.7.21 y anteriores» según spring.io (GitHub Advisories lista otros rangos, que no coinciden); la versión efectiva es **5.7.11**, o sea **la biblioteca está afectada**. La corrección 5.7.22 solo existe con soporte comercial; las abiertas están en 6.5.9 y 7.0.4 (otra línea).
  - **Control compensatorio:** el aviso publica un workaround, `HeaderWriterFilter.shouldWriteHeadersEagerly=true`. Se verificó que el método `setShouldWriteHeadersEagerly` existe en `spring-security-web` 5.7.11 y se activa en `SecurityConfig` con un `ObjectPostProcessor`. Con el valor por defecto, si la aplicación fija alguna cabecera de caché, Spring Security no escribe las suyas; en modo eager las escribe antes de continuar la cadena y lo que la aplicación fije solo pisa esa cabecera. Efecto colateral que declara el fabricante: ninguna cabecera ya escrita puede quitarse después. La aplicación no fija hoy cabeceras de caché propias, así que la política existente no cambia.
  - **Evidencia:** `SecurityHeadersMockMvcTest` (4 tests: el filtro está en modo eager; con un `Cache-Control` propio siguen presentes `Pragma`, `Expires`, `nosniff` y `DENY`; páginas públicas y un estático conservan las cabeceras) y, sobre el JAR ejecutable, 27 respuestas (200, 302 y 403, anónimo y ambos roles) con `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-store, must-revalidate, no-cache, max-age=0`, `Pragma: no-cache` y `Expires: 0`.
  - **Qué no se afirma:** que la vulnerabilidad esté corregida (la biblioteca sigue en 5.7.11). Se registra como «biblioteca afectada, mitigada por el workaround documentado por el fabricante, dentro del alcance revisado».
- **CVE-2026-40973 (Spring Boot 2.7.18):** un atacante local en el mismo host podría apropiarse del directorio de `ApplicationTemp`; el impacto crece con sesiones persistentes, que no están habilitadas. La corrección (2.7.33) es solo comercial. **Riesgo residual aceptado por el autor únicamente para la demo local**, ejecutada en un equipo de un solo usuario; no se extiende a un despliegue productivo, donde habría que usar un host dedicado o un directorio temporal privado (`-Djava.io.tmpdir`).
- **Resto de avisos de dependencias:** gitleaks e OSV-Scanner se ejecutaron sobre un candidato anterior al fix de plantillas. Las dos coincidencias de gitleaks son una contraseña de prueba de un test (falsos positivos justificados). De los avisos con severidad ≥ 7.0 se revisó cada uno contra el código; de los nueve pendientes, siete (los tres de Thymeleaf y otros cuatro) se clasificaron como no aplicables con justificación, CVE-2026-22732 como mitigado por el workaround y CVE-2026-40973 como riesgo residual condicional (p. ej. exigen entrada de usuario evaluada por el motor de plantillas, Querydsl, RouterFunctions o recursos versionados, que el proyecto no usa). Esa clasificación se basa en las condiciones publicadas por las fuentes y en el código; no es una prueba de ausencia. Spring Boot 2.7 es deuda tecnológica explícita y varias correcciones solo existen con soporte comercial. Los escaneos se repetirán sobre el commit final.

## 6. Demo, guardas y pruebas

- **Demo con base separada, cuyo arranque y carga inicial fueron verificados** (la reproducción desde un clon limpio sigue pendiente): el perfil `demo` usa la base descartable `libreriaweb_demo` y carga 17 libros, 14 autores, 12 editoriales, 2 usuarios y 1 préstamo, de forma idempotente. Las credenciales demo se toman del entorno y no tienen valor por defecto.
- **Guardas:** el nombre de la base está fijo en el código; el arranque aborta si la conexión no apunta a `libreriaweb_demo` y verifica el catálogo real antes de cargar datos. Es una salvaguarda frente a errores de configuración, no una protección absoluta.
- **Pruebas automáticas (evidencia separada, no se suman):**
  - **Suite completa vigente:** `./mvnw -ntp clean package`, sin perfil demo, solo sobre `libreriaweb_testsuite` y `libreriaweb_demo_testsuite`: **228 tests en 17 clases, 0 fallos/errores/omitidos, BUILD SUCCESS**, ejecutada sobre el árbol de trabajo previo al commit que incorpora las correcciones. El total se verificó sumando los reportes de surefire; BUILD SUCCESS lo aportó el autor al leer la salida, y **el código de salida original de Maven no se recuperó**. Incluye la regresión de login con 72 bytes y 6 tests nuevos (fragmentos Thymeleaf y cabeceras).
  - **Evidencias históricas (no se suman):** 221 tests en 15 clases, anterior a la regresión de login, y la corrida focalizada de `CsrfProtectionMockMvcTest` con 17 tests (ver §5); ambas quedan incluidas en los 228. Un resultado más antiguo (165 tests) precede al rediseño visual.
  - **`java -jar` verificado, y el defecto que apareció:** el primer JAR candidato arrancaba, migraba y cargaba los datos, pero todas las páginas HTML respondían 500 (`Error resolving template [/fragments/nav]`). Las referencias `th:replace="/fragments/…"` llevaban barra inicial (67 en 18 plantillas); desde directorio (`target/classes`, que es como corren la demo habitual y MockMvc) se resuelven, y dentro del JAR anidado no. Se quitó la barra, sin tocar nada más de las plantillas. La causa se confirmó con el JAR: las mismas páginas pasaron de 500 a 200 con ese único cambio. Regresiones: `TemplateFragmentReferencesTest` (ninguna referencia empieza con barra y todas resuelven dentro de un JAR temporal armado con las plantillas) y `scripts/smoke-jar-demo.ps1` contra el JAR real.
  - **Prueba del JAR (Windows, instancia MariaDB aislada en el puerto 3307, perfil `demo`, HTTP 8081):** Flyway V1–V4 aplicado desde un esquema vacío, seeder (17 libros, 14 autores, 12 editoriales, 2 usuarios, 1 préstamo, 8 portadas), arranque en unos 9 s; smoke de 5 rutas en 200; 27 comprobaciones por rol sin fallos (anónimo redirige al login; `ADMIN` ve listas y formularios de gestión y recibe 403 en «Mis préstamos»; `USUARIO` ve catálogo y «Mis préstamos» y recibe 403 en las rutas de gestión). Alcance: solo lectura y control de acceso; no se probaron altas, bajas, préstamos ni cierre de sesión por el JAR. El JAR empaqueta `show-sql=true` y `thymeleaf.cache=false` (deuda no corregida).
- **Comprobaciones HTTP (lecturas y autenticación sobre la demo, sin mutaciones):** matriz de roles con y sin barra final y `?format=pdf` sin accesos indebidos; sidebar 28/28; listas de gestión y buscador público 31/31. No validan aspecto, responsive ni foco.
- **Pruebas manuales del autor:** tema oscuro aprobado visualmente; registro, ingreso, solicitud, rechazo de duplicado, devolución y los cuatro controles de permisos (incluyen login ADMIN, listado ADMIN y mensajes de éxito/error); aspecto de las listas de gestión (sin buscadores, PDF «Próximamente») y enlace «Saltar al contenido»; resaltado por sección, «Inicio» hacia `/`, sidebar contraído y expandido; logout y 404 propia con sesión.
- **Responsive:** la interfaz incorpora adaptación responsive. Validado manualmente a 390 × 844 únicamente en: inicio, catálogo, login, registro, Mis préstamos (con desplazamiento horizontal de la tabla) y menú lateral (apertura, cierre por fondo y Escape). No se extiende a los formularios de gestión.
- **Teclado y accesibilidad:** validación parcial (Tab/Shift+Tab con orden lógico y foco visible en el login; etiquetas de formulario). El recorrido completo por teclado queda pendiente; no se declara cumplimiento de WCAG.
- **CRUD y portadas:** los tests automáticos cubren mutaciones solo por POST con CSRF, reglas de catálogo y de usuarios, concurrencia y descarga de portadas (creadas por fixture); ningún test sube por HTTP un archivo de portada con contenido. Alta, modificación, baja y eliminación desde la interfaz y la carga de portadas están implementadas, sin validación manual registrada.
- **Capturas de pantalla:** pendientes; se incorporarán al final, antes de publicar.

## 7. Limitaciones, decisiones y mejoras futuras

**Hallazgos funcionales corregidos:**
1. Los buscadores de las listas de gestión de Libros, Autores y Editoriales no filtraban (los controladores `lista` no leen `buscar`): se retiraron de las vistas. Solo funciona, y se conserva, el buscador del catálogo público. Búsqueda en las listas de gestión: mejora futura.
2. **Seguridad.** Con barra final, un `USUARIO` recibía 200 con contenido administrativo en `/libro/lista/`, `/autor/lista/`, `/editorial/lista/` y `/prestamo/lista/`; la causa eran las reglas `antMatchers` de ruta exacta, que ahora son `mvcMatchers` en `SecurityConfig`. Validación, por tipo: tests automáticos de la suite completa (incluye 80 de `WebSecurityMockMvcTest` con las variantes con barra final y `?format=pdf`); matriz HTTP de lectura sobre la demo tras la corrección (USUARIO 403, anónimo → login, ADMIN 200). Limitación: las regresiones no se ejecutaron contra el código anterior, y las mutaciones con barra final se verifican solo en MockMvc.

También se retiró el buscador de Usuarios y el enlace «Inicio» del sidebar apunta a `/` con selección por sección.

**Mejora futura (diferida):** exportación PDF de las listas de gestión. Los cuatro botones están deshabilitados con «PDF · Próximamente»; el proyecto se presenta sin exportación PDF.

**Limitaciones:** una instancia y una base; recursos de terceros por CDN (Bootstrap JS, Bootstrap Icons, Font Awesome, Google Fonts), con dependencia de red; imágenes y portadas generadas con IA, no tapas comerciales; la base histórica `libreriaweb` no se migra automáticamente.

**Pendiente antes de publicar:** clon limpio del commit final con su empaquetado, repetición de los escaneos de dependencias y secretos sobre ese commit, capturas de pantalla, renderizado de los diagramas Mermaid y revisión acumulada. Ya hecho: empaquetado, Flyway desde cero en un entorno aislado y `java -jar` contra la demo (ver arriba). El recorrido completo por teclado queda diferido por decisión del autor. La validación en Linux queda como verificación futura opcional.

**Mejoras futuras (no comprometidas):** ampliar la captura de fallos de bloqueo a los demás servicios, agregar pruebas automáticas del rediseño, completar la validación por teclado y evaluar migrar a Spring Boot 3.

## 8. Preguntas habituales de entrevista

1. **¿Qué problema resuelve y para quién?** Gestiona el catálogo y los préstamos de una librería con tres perfiles (visitante, usuario, administrador). Es un proyecto de portfolio, no una aplicación productiva.

2. **¿Por qué bloqueo pesimista y no optimista?** El stock es un contador con contención sobre pocas filas. Con bloqueo pesimista, la segunda solicitud espera a la primera y ve el stock ya actualizado. Si la espera termina en un fallo de bloqueo, `ServicePrestamo` lo informa con un mensaje y el usuario puede reintentar. No se promete ausencia de esperas.

3. **¿Cómo redujeron el riesgo de interbloqueos?** Con un orden consistente de adquisición (Autor → Editorial → Libro → Portada; Usuario → Libro; Préstamo → Libro). Reduce el riesgo, no lo elimina. Solo `ServicePrestamo` y `ServiceLibro` capturan el fallo de bloqueo, revierten y devuelven un mensaje de dominio.

4. **¿Cómo protegen la base real al correr tests o la demo?** Con nombres de base fijos en constantes, guardas previas al arranque y verificación del catálogo real de la conexión antes de cargar datos. Las bases de test se recrean solo con esos nombres. Son salvaguardas frente a errores de configuración, no una garantía absoluta.

5. **¿Qué está probado y qué no?** Tres tipos de evidencia, separados. Automática: 228 tests de la suite completa (incluyen la regresión de login y las comprobaciones de plantillas y cabeceras). HTTP: lecturas y control de acceso sobre la demo y sobre el JAR ejecutable, sin valor visual. Manual del autor: flujo principal, tema oscuro, sidebar, listas de gestión, móvil 390 × 844 en las vistas listadas y teclado en el login. No probado todavía: recorrido completo por teclado (diferido), capturas, altas/bajas/préstamos por el JAR, clon limpio del commit final y escaneos repetidos sobre él.

6. **¿Cómo manejan las contraseñas largas?** BCrypt solo mira 72 bytes. En lugar de truncar en silencio, el envoltorio rechaza las claves más largas. Es un control compensatorio: la versión de Spring Security (5.7.11) está afectada por CVE-2025-22228 y la corrección abierta no existe en esa línea. Lo cubren tests del envoltorio y una regresión del login completo con 72 bytes y 72 bytes más un carácter.

   **Corolario de seguridad en cabeceras:** la misma línea 5.7.11 está afectada por CVE-2026-22732 (las cabeceras de seguridad podrían no escribirse si la aplicación fija cabeceras de caché). Se aplicó el workaround oficial `shouldWriteHeadersEagerly=true` y se midieron las cabeceras en el JAR; es un control compensatorio, no una corrección de la biblioteca.

7. **¿Qué mejorarían?** Primero búsqueda en las listas de gestión y exportación PDF (mejoras futuras, hoy fuera de la oferta); después, ampliar el manejo de fallos de bloqueo, completar la validación por teclado y evaluar Spring Boot 3.

## 9. Guiones de presentación y demo

Los guiones suponen la demo corriendo con el perfil `demo`. Ensayarlos completos antes de presentar.

### A. Presentación de 2 minutos (≈ 290 palabras habladas)

**[0:00 – 0:20] Qué es**
"Librería El Ceibo es una aplicación web para gestionar el catálogo de una librería y los préstamos de libros. La hice en 2022 como proyecto académico y este año la retomé para completar el ciclo de préstamos y fortalecer seguridad, integridad de datos y pruebas."

**[0:20 – 0:50] Qué hace**
"Hay acceso anónimo y dos roles de seguridad. Quien entra sin sesión explora el catálogo y se registra. Un usuario solicita libros, ve su historial y los vencimientos. Un administrador gestiona libros, autores, editoriales y usuarios, y registra las devoluciones. Cada rol ve solo la navegación que le corresponde, y el backend lo refuerza: no alcanza con escribir la URL."

**[0:50 – 1:25] Cómo está hecha**
"Java 8 con Spring Boot: controladores, servicios y repositorios con JPA sobre MySQL, y vistas con Thymeleaf. El esquema ya no lo crea Hibernate: lo versiona Flyway con cuatro migraciones, y Hibernate solo valida. La seguridad usa Spring Security con roles, contraseñas con BCrypt y protección CSRF; el logout es un POST."

**[1:25 – 1:50] Lo que más me interesó: la concurrencia**
"El problema más interesante fue la concurrencia: dos personas pidiendo el último ejemplar al mismo tiempo. Lo resolví con transacciones y bloqueos pesimistas con un orden consistente de adquisición, que reduce el riesgo de interbloqueos, y lo comprobé con tests concurrentes, no solo con tests felices."

**[1:50 – 2:00] Cierre**
"Tiene una suite de 228 tests automatizados que pasan, una demo con base separada, cuyo arranque y carga inicial verifiqué también con el JAR ejecutable, y límites declarados: sin email, sin Docker, sin despliegue productivo. Les muestro cómo funciona."

Si se menciona la cifra de tests: decir "228 tests automáticos aprobados en la suite completa" y, si se pregunta por la regresión de login, "está incluida: el test de 72 bytes forma parte de esos 228". No decir que validan el aspecto visual ni que el JAR se probó en escritura (solo lectura y control de acceso).

### B. Demo de 5 minutos

Preparación previa (fuera de cámara): app corriendo en perfil `demo` sobre `libreriaweb_demo`; navegador en `/`; credenciales demo fuera de pantalla. Estado inicial: 17 libros, 1 préstamo (El Señor de los Anillos, sin disponibilidad).

| Tiempo | Rol | Acción | Qué decir | Datos |
|---|---|---|---|---|
| 0:00–0:30 | Anónimo | Inicio → clic en **Explorar catálogo** | "El inicio lleva directo al catálogo público." | Lee |
| 0:30–1:00 | Anónimo | Catálogo: portadas, búsqueda, libro "Sin disponibilidad" | "Ve la disponibilidad real de cada libro; este está agotado." | Lee |
| 1:00–1:20 | Anónimo | `/login` | "Login con labels accesibles; el registro está al lado." | Lee |
| 1:20–2:30 | Usuario | Login → **solicitar** un libro disponible (p. ej. *El Hobbit*) → **Mis préstamos** | "Se crea el préstamo con vencimiento a 14 días y el stock baja en uno." | **Escribe** |
| 2:30–2:50 | Usuario | Intentar solicitar el mismo libro | "Las reglas del negocio devuelven un mensaje claro: ya tiene un préstamo activo." | Lee |
| 2:50–3:10 | Usuario | Escribir `/libro/lista` en la barra | "Aunque escriba la URL, el backend responde 403." | Lee |
| 3:10–3:20 | Usuario | **Cerrar sesión** | "El logout es POST con CSRF." | — |
| 3:20–4:20 | Admin | Login → **Gestión de catálogo** → recorrer Libros, Autores, Editoriales, Usuarios | "El administrador ve la navegación de gestión; el usuario no." | Lee |
| 4:20–5:00 | Admin | **Préstamos** → devolver el préstamo creado durante la demo | "La devolución repone el ejemplar y deja el historial. Un préstamo ya devuelto no se puede devolver de nuevo." | **Escribe** |

Notas para quien presenta:
- Texto para decir en voz alta: "Devolvemos el préstamo creado durante esta demostración para conservar el préstamo semilla. El préstamo devuelto queda en el historial y el usuario registrado permanece; la base no vuelve por completo al estado inicial."
- El estado "No hay préstamos para mostrar" de la lista de admin no es alcanzable con este dataset; no prometerlo.
- La página 404/403 puede mostrarse con una URL inexistente o con la de otro rol.
- No hay sesión simultánea de dos roles en el mismo navegador: usar ventana privada para el admin o cerrar sesión entre medio.
- No mostrar la exportación PDF ni buscadores en las listas de gestión: son mejoras futuras.

### C. Respuestas preparadas (30 segundos cada una)

1. **¿Por qué bloqueo pesimista y no optimista?** El stock es un contador con contención sobre pocas filas; con lock pesimista la segunda solicitud espera a la primera y ve el stock ya actualizado. Si la espera termina en un fallo de bloqueo, el servicio de préstamos lo informa con un mensaje y el usuario puede reintentar: no se promete ausencia de esperas ni de fallos de lock.
2. **¿Cómo redujeron el riesgo de interbloqueos?** Con un orden consistente de adquisición (Autor → Editorial → Libro → Portada; Usuario → Libro; Préstamo → Libro). Eso reduce el riesgo, no lo elimina: en préstamos y libros, un fallo de bloqueo se captura, se revierte la transacción y se informa con un mensaje de dominio.
3. **¿Cómo protegen la base real al correr tests o la demo?** Nombres de base fijos en constantes, guardas previas al arranque y verificación del catálogo real de la conexión antes de cargar datos; las bases de test se recrean solo con esos nombres. Son salvaguardas frente a errores de configuración, no una garantía absoluta.
4. **¿Qué quedó afuera?** Email, multas, renovaciones, Docker, CI/CD y despliegue productivo; la migración a Spring Boot 3.
