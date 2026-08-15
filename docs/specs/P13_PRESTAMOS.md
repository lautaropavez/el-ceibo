# P13 — PRÉSTAMOS MVP

## 1. Objetivo

Definir el modelo funcional, las reglas de negocio y las garantías de
concurrencia/autorización necesarias para que un usuario pueda
solicitar el préstamo de un libro y devolverlo, con stock e historial
consistentes en todo momento.

Esta SPEC es la referencia funcional para implementar `ServicePrestamo`,
las rutas de `PrestamosController` y, si corresponde, una migración
Flyway `V4`. No es una guía de implementación línea por línea: no
contiene código Java ni SQL.

## 2. Alcance

* Solicitud de préstamo por parte de un usuario autenticado con rol
  `USUARIO`, sobre un libro activo y disponible.
* Registro de devolución por parte de un `ADMIN`.
* Actualización consistente de stock (`nroEjemplares`,
  `ejemplaresRestantes`, `ejemplaresPrestados`) al prestar y al devolver.
* Vista "Mis préstamos" para el usuario autenticado (activos + devueltos).
* Vista de administración de préstamos para `ADMIN`.
* Reglas de concurrencia para la última unidad disponible y para evitar
  doble devolución.
* Reglas de autorización para impedir que un usuario opere sobre
  préstamos de otro usuario (IDOR).
* Cambios conceptuales de schema esperados (V4), sin redactar el SQL.

## 3. Fuera de alcance

* Fecha de vencimiento configurable por libro/categoría (queda fija en
  14 días para todo el MVP).
* Renovación de préstamos.
* Notificaciones (mail, recordatorios de vencimiento, mora).
* Multas o penalizaciones por atraso.
* Reservas de libros sin stock disponible.
* Reportes/estadísticas de uso.
* Tests automatizados permanentes (P14.2, según roadmap).
* Eliminación de `Usuario.cantPrestamos` (se marca como deuda, no se
  toca en P13 — ver sección 17).
* Agregar `Libro.prestamos` (`@OneToMany` inverso) — no hay necesidad
  concreta de navegación inversa todavía (ver sección 12).
* Rediseño de `SecurityConfig` más allá de lo estrictamente necesario
  para las rutas nuevas de préstamos.

## 4. Modelo funcional

Entidad `Prestamo` (evolución del modelo actual):

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `id` | `String` (UUID) | sí | identificador del préstamo |
| `usuario` | `Usuario` (`@ManyToOne`) | sí | quien solicita, derivado de la sesión autenticada, nunca de un parámetro de request |
| `libro` | `Libro` (`@ManyToOne`) | sí | libro prestado |
| `fechaPrestamo` | `Date` | sí | momento de creación del préstamo, asignado por el servidor |
| `fechaVencimiento` | `Date` | sí | calculada automáticamente como `fechaPrestamo + 14 días` |
| `fechaDevolucion` | `Date` | no (nullable) | momento en que `ADMIN` confirma la devolución; `null` mientras el préstamo está activo |

`Prestamo.alta` **se elimina** del modelo nuevo. No se reutiliza como
estado ni con ningún otro significado. El estado del préstamo se
deriva exclusivamente de `fechaDevolucion` (ver 5.4) para evitar dos
fuentes de verdad.

## 5. Reglas de negocio

### 5.1 Usuario

* Solo un usuario con rol `USUARIO` puede solicitar un préstamo, y
  únicamente para sí mismo. El usuario se obtiene desde
  `Authentication`/la sesión activa; nunca se acepta un `usuarioId`
  llegado por formulario o request.
* Un usuario dado de baja no puede generar préstamos nuevos.
* Un usuario dado de baja conserva sus préstamos activos previos, que
  pueden devolverse normalmente.

### 5.2 Libro

* Solo se puede solicitar préstamo de un libro activo (`Libro.alta =
  true`).
* Un libro dado de baja no puede generar préstamos nuevos.
* Un libro dado de baja no afecta los préstamos activos que ya existan
  sobre él; esos préstamos pueden devolverse normalmente.

### 5.3 Stock

Fuente de verdad operacional: `Libro.nroEjemplares`,
`Libro.ejemplaresRestantes`, `Libro.ejemplaresPrestados`.

Invariante obligatoria, válida en todo momento para todo libro:

```
nroEjemplares = ejemplaresRestantes + ejemplaresPrestados
```

Al crear un préstamo:
* `ejemplaresRestantes - 1`
* `ejemplaresPrestados + 1`

Al registrar una devolución:
* `ejemplaresRestantes + 1`
* `ejemplaresPrestados - 1`

Ningún valor de stock puede quedar negativo ni violar la invariante
anterior en ningún momento, incluida la ejecución concurrente.

### 5.4 Préstamo activo/devuelto

* Préstamo **activo**: `fechaDevolucion IS NULL`.
* Préstamo **devuelto**: `fechaDevolucion IS NOT NULL`.
* No se crea un enum `Estado`. Esta es la única fuente de verdad sobre
  el estado del préstamo; no debe derivarse de ningún otro campo
  (incluido el eliminado `alta`).
* Un usuario no puede tener más de 3 préstamos activos simultáneos.
  Ese máximo se calcula mediante una consulta real sobre
  `PrestamoRepository` (préstamos del usuario con
  `fechaDevolucion IS NULL`) en el momento de la solicitud, nunca
  mediante un contador duplicado.
* Un usuario no puede tener dos préstamos activos del mismo libro al
  mismo tiempo. También se valida mediante consulta real en el momento
  de la solicitud.

### 5.5 Vencimiento

* Todo préstamo tiene una duración fija de 14 días.
* `fechaVencimiento` se calcula automáticamente al crear el préstamo
  (`fechaPrestamo + 14 días`) y no es editable por el usuario ni por
  `ADMIN`.
* El MVP no implementa ninguna acción automática al vencer (sin mora,
  sin notificación, sin bloqueo adicional) — el vencimiento es un dato
  informativo en esta etapa (ver sección 3, fuera de alcance).

### 5.6 Historial

* Un préstamo nunca se borra, independientemente de su estado o de que
  el usuario/libro asociado sea dado de baja luego.
* "Mis préstamos" muestra tanto préstamos activos como devueltos del
  usuario autenticado.
* `ADMIN` puede consultar el historial completo de todos los usuarios.

## 6. Solicitud de préstamo

Flujo de interacción (alto nivel):

```
USUARIO autenticado
      ↓
selecciona Libro disponible (activo, con stock)
      ↓
confirmación
      ↓
POST
      ↓
ServicePrestamo
```

Orden conceptual obligatorio dentro de `ServicePrestamo`, en una única
transacción:

```
lock Usuario (PESSIMISTIC_WRITE)
      ↓
validar usuario activo
      ↓
contar préstamos activos del usuario (consulta real)
      ↓
validar máximo < 3
      ↓
lock Libro (PESSIMISTIC_WRITE)
      ↓
validar libro activo
      ↓
validar stock > 0
      ↓
validar que no exista préstamo activo del mismo usuario+libro
      ↓
crear préstamo + actualizar stock
      ↓
commit
```

El orden canónico de adquisición de locks es **USUARIO → LIBRO**,
siempre en ese sentido, para evitar que dos transacciones concurrentes
adquieran esos dos recursos en órdenes distintos. Todas las
validaciones cuyo resultado pueda cambiar por una solicitud
concurrente (cupo, duplicado, stock) se ejecutan **después** del lock
correspondiente, nunca antes. Ver §10 para el detalle de por qué el
lock sobre `Usuario` es necesario además del lock sobre `Libro`, y
para el nivel de aislamiento transaccional requerido.

No se crean préstamos mediante `GET`.

## 7. Mis préstamos

* Vista del usuario autenticado únicamente.
* El usuario se deriva de `Authentication`; la vista nunca acepta un
  identificador de usuario arbitrario.
* Muestra préstamos activos y devueltos (ver 5.6).
* Contenido mínimo por préstamo: libro, fecha de préstamo,
  fecha de vencimiento, y estado/fecha de devolución según corresponda.

## 8. Administración

`ADMIN` puede:

* listar todos los préstamos;
* distinguir activos de devueltos;
* identificar el usuario asociado a cada préstamo;
* registrar la devolución de un préstamo (ver sección 9).

`ADMIN` opera sobre préstamos identificándolos por `prestamoId`, en
rutas administrativas protegidas — nunca reutiliza la vista "Mis
préstamos" de otro usuario para esto.

## 9. Devolución

* Solo `ADMIN` registra una devolución (representa al personal/
  bibliotecario que recibe físicamente el libro del usuario).
* La carrera de doble devolución se protege bloqueando el propio
  `Prestamo`, no solamente `Libro`. No debe existir ningún flujo donde
  se lea o valide el estado del préstamo antes de adquirir su lock.
* Flujo:

```
ADMIN autenticado
      ↓
POST devolución (por prestamoId)
      ↓
ServicePrestamo
      ↓
buscar Prestamo con lock (PESSIMISTIC_WRITE)
      ↓
si no existe → error controlado
      ↓
con Prestamo ya bloqueado, validar fechaDevolucion IS NULL
      ↓
si ya tiene fechaDevolucion → error controlado (ya devuelto)
      ↓
lock Libro (PESSIMISTIC_WRITE)
      ↓
registrar fechaDevolucion
      ↓
ejemplaresPrestados - 1 / ejemplaresRestantes + 1
      ↓
validar invariantes de stock
      ↓
commit
```

* Nunca se borra el préstamo al devolverlo.
* Dos devoluciones concurrentes del mismo `prestamoId` deben producir:
  una devolución exitosa, una segunda operación rechazada como "ya
  devuelto", y stock incrementado exactamente una vez.
* Debe rechazarse de forma controlada: préstamo inexistente, préstamo
  ya devuelto.

## 10. Concurrencia

* **Aislamiento transaccional:** las operaciones críticas de creación y
  devolución de préstamo se ejecutan bajo aislamiento `READ_COMMITTED`,
  replicando el patrón ya utilizado en el proyecto (`ServiceLibro`,
  operaciones sobre `Portada`) para evitar trabajar con estado o
  snapshots obsoletos leídos antes de adquirir el lock correspondiente.
  Toda validación cuyo resultado pueda cambiar por una operación
  concurrente se ejecuta después de adquirir el lock del recurso que
  esa validación consulta, nunca antes.
* **Última unidad:** al crear un préstamo se usa lock pesimista sobre
  `Libro` (adquirido después del lock sobre `Usuario`, ver §6). Con
  stock = 1 y 2 requests concurrentes, exactamente 1 préstamo debe
  persistir; la otra request debe ser rechazada de forma controlada (no
  debe romper la transacción de la primera ni dejar stock
  inconsistente). Estado final esperado: `ejemplaresRestantes = 0`,
  `ejemplaresPrestados` incrementado una sola vez.
* **Cupo máximo entre libros distintos:** dos solicitudes concurrentes
  del mismo usuario sobre libros distintos (A y B) no pueden hacer que
  ese usuario supere 3 préstamos activos. El lock pesimista sobre
  `Usuario` (adquirido antes del lock sobre `Libro`, ver §6) serializa
  las solicitudes concurrentes de un mismo usuario aunque involucren
  libros distintos, porque ambas transacciones deben adquirir el mismo
  lock sobre la misma fila de `Usuario` antes de poder contar y validar
  el cupo.
* **Doble devolución:** dos requests concurrentes intentando devolver
  el mismo préstamo deben resultar en exactamente una devolución
  efectiva; la segunda debe ser rechazada de forma controlada
  (préstamo ya devuelto), sin descontar stock dos veces. Se protege
  bloqueando el propio `Prestamo` con `PESSIMISTIC_WRITE` antes de
  validar su estado (ver §9) — no alcanza con bloquear únicamente
  `Libro`.

## 11. Seguridad y autorización

* Roles: solicitar préstamo → `USUARIO` (para sí mismo); registrar
  devolución y administrar → `ADMIN`.
* El usuario relevante para "Mis préstamos" y para la creación del
  préstamo se deriva siempre de `Authentication`, nunca de un parámetro
  de formulario o de query string.
* `ADMIN` opera sobre préstamos por `prestamoId`, en rutas
  administrativas separadas y protegidas por rol.
* El service valida siempre, en el propio backend, la existencia y el
  estado del recurso (préstamo, usuario, libro) antes de operar sobre
  él — ocultar botones o rutas en la UI no reemplaza esta validación,
  es únicamente una ayuda de experiencia de usuario.
* Objetivo explícito: prevenir IDOR (un usuario no debe poder leer ni
  operar sobre préstamos de otro usuario manipulando identificadores).

## 12. Persistencia / Flyway

Se espera que P13 requiera una migración `V4` conceptualmente
compuesta por:

* eliminar la columna `alta` de `prestamo`;
* agregar `fechaVencimiento` (no nula);
* `usuario_id` pasa a `NOT NULL`;
* `libro_id` pasa a `NOT NULL`;
* `fechaPrestamo` pasa a `NOT NULL`;
* revisar la nullability de `fechaDevolucion` (debe permanecer
  nullable: es la representación del estado "activo");
* revisar/agregar los índices o constraints que surjan del diseño
  final (por ejemplo, para soportar eficientemente las consultas de
  "préstamos activos por usuario" y "préstamo activo por usuario+libro"
  de la sección 5.4).

No se redacta el SQL de `V4` en esta SPEC. `Libro.prestamos`
(`@OneToMany` inverso) no se agrega — `Prestamo.libro` unidireccional
alcanza mientras no exista una necesidad concreta de navegar de
`Libro` hacia sus préstamos.

## 13. Invariantes

* `nroEjemplares = ejemplaresRestantes + ejemplaresPrestados`, siempre,
  para todo libro.
* `ejemplaresRestantes >= 0` y `ejemplaresPrestados >= 0`, siempre.
* Un préstamo devuelto nunca vuelve a estar activo: un préstamo solo
  puede pasar de activo a devuelto una única vez.
* Un préstamo nunca se elimina físicamente.
* Un usuario nunca tiene más de 3 préstamos activos simultáneos,
  incluso bajo solicitudes concurrentes sobre libros distintos.
* Un usuario nunca tiene dos préstamos activos del mismo libro al
  mismo tiempo.
* Todo préstamo tiene `usuario` y `libro` no nulos.
* El estado del préstamo se deriva únicamente de `fechaDevolucion`.
* Una devolución incrementa `ejemplaresRestantes` (y decrementa
  `ejemplaresPrestados`) exactamente una vez por préstamo.

## 14. Casos de error controlados

* Usuario inexistente o inactivo (dado de baja) intenta solicitar.
* Libro inexistente o inactivo (dado de baja) intenta ser solicitado.
* Stock en 0 al momento de solicitar.
* Usuario ya tiene 3 préstamos activos.
* Usuario ya tiene un préstamo activo del mismo libro.
* Devolución sobre `prestamoId` inexistente.
* Devolución sobre un préstamo ya devuelto.
* Carrera por la última unidad disponible (2+ solicitudes concurrentes,
  stock = 1).
* Carrera de doble devolución sobre el mismo préstamo.
* Acceso no autorizado: usuario intentando ver/operar sobre préstamos
  de otro usuario, o usuario sin rol `ADMIN` intentando registrar una
  devolución o acceder al panel de administración.

## 15. Criterios de aceptación

* Un usuario `USUARIO` autenticado con menos de 3 préstamos activos y
  sin préstamo activo del libro elegido puede solicitar un préstamo de
  un libro activo con stock > 0, y el préstamo queda persistido con
  `fechaPrestamo`, `fechaVencimiento = fechaPrestamo + 14 días` y
  `fechaDevolucion = null`.
* Tras la solicitud, `ejemplaresRestantes` disminuye en 1 y
  `ejemplaresPrestados` aumenta en 1, sin romper la invariante de
  stock.
* Un usuario con 3 préstamos activos no puede solicitar un cuarto; la
  solicitud es rechazada de forma controlada (sin excepción no
  manejada, sin HTTP 500).
* Un usuario no puede solicitar un segundo préstamo activo del mismo
  libro mientras el primero siga activo.
* Un usuario no puede solicitar préstamo de un libro con stock 0, ni
  siendo él mismo un usuario dado de baja.
* Un libro dado de baja no admite nuevas solicitudes, pero sus
  préstamos activos previos pueden devolverse con normalidad.
* Solo `ADMIN` puede registrar una devolución; al hacerlo,
  `fechaDevolucion` queda seteada, `ejemplaresRestantes` aumenta en 1 y
  `ejemplaresPrestados` disminuye en 1.
* Un préstamo ya devuelto no puede volver a devolverse.
* Con stock = 1 y 2 solicitudes concurrentes del mismo libro,
  exactamente una persiste; la otra es rechazada de forma controlada;
  el stock final es consistente (`restantes = 0`, `prestados`
  incrementado una sola vez).
* Con 2 solicitudes concurrentes de devolución sobre el mismo
  préstamo, exactamente una tiene efecto; la otra es rechazada de
  forma controlada; el stock no se descuenta dos veces.
* "Mis préstamos" de un usuario nunca expone préstamos de otro
  usuario, incluso manipulando identificadores en la request.
* Ningún préstamo se elimina físicamente en ningún flujo del sistema.

## 16. Plan de pruebas adversariales

Casos que recibirá `adversarial-tester` (a completar como agente
independiente en P13.0E, aquí solo se enumeran los casos que debe
cubrir):

* Intentar solicitar préstamo mandando un `usuarioId` distinto al de
  la sesión autenticada (manipulación directa de request).
* Intentar acceder a "Mis préstamos" de otro usuario cambiando un
  identificador en la URL o en el body.
* Intentar registrar una devolución sin rol `ADMIN`.
* Intentar registrar una devolución sobre un `prestamoId` que no
  existe.
* Intentar devolver el mismo préstamo dos veces, secuencial y
  concurrentemente.
* Disparar 2+ solicitudes concurrentes sobre un libro con stock = 1 y
  verificar que solo una persiste y el stock queda consistente.
* Usuario con 2 préstamos activos dispara dos requests simultáneas de
  solicitud sobre libro A y libro B (ambos con stock disponible):
  verificar que exactamente uno de los dos nuevos préstamos persiste y
  que el total de activos final es 3, nunca 4.
* Intentar solicitar dos préstamos activos del mismo libro en
  paralelo (misma carrera que el punto anterior pero por duplicado, no
  por stock).
* Intentar solicitar préstamo de un libro inactivo (`alta = false`).
* Intentar solicitar préstamo estando el propio usuario dado de baja.
* Intentar solicitar préstamo de un libro con stock 0.
* Verificar que un préstamo nunca puede eliminarse físicamente a
  través de ningún endpoint expuesto.
* Verificar que la invariante `nroEjemplares = ejemplaresRestantes +
  ejemplaresPrestados` se mantiene después de cada escenario anterior,
  incluidos los rechazados.

## 17. Decisiones pendientes / deuda

* `Usuario.cantPrestamos` no se usa como fuente de verdad en P13 (la
  cantidad de préstamos activos se calcula siempre mediante
  `PrestamoRepository`). Si al finalizar P13 se confirma que el campo
  sigue completamente muerto (sin ningún código que lo escriba o lo
  lea), queda marcado como candidato a eliminación en una posta
  posterior — no se elimina desde esta SPEC.
* `Libro.prestamos` (`@OneToMany` inverso) queda deliberadamente fuera
  de P13; se agregará si surge una necesidad concreta de navegar de
  `Libro` hacia sus préstamos.
* Vencimiento sin consecuencias automáticas (sin mora, sin
  notificaciones, sin bloqueo por atraso) es una decisión deliberada
  de alcance del MVP, no un olvido — queda para una posta futura si el
  producto lo requiere.
* El SQL exacto de `V4` no se decide en esta SPEC; se diseñará en la
  posta de implementación correspondiente, respetando los cambios
  conceptuales listados en la sección 12.
