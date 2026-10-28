# Diagramas — Librería Web El Ceibo

## 1. Capas

```mermaid
flowchart TB
    subgraph W["controladores/"]
        MC[MainController]
        LC[LibroController]
        AC["AutorController / EditorialController"]
        UC[UsuarioController]
        PC[PrestamosController]
        PoC[PortadaController]
    end
    subgraph S["servicios/"]
        SL[ServiceLibro]
        SA["ServiceAutor / ServiceEditorial"]
        SU[ServiceUsuario]
        SP[ServicePrestamo]
        SPo[ServicePortada]
    end
    B[Navegador] --> F["Spring Security<br/>sesión, CSRF, reglas por ruta"]
    F --> W
    W --> S
    S --> R["repositorios/<br/>Spring Data JPA"]
    R --> DB[("MySQL<br/>esquema Flyway V1–V4")]
    W --> T["templates/ Thymeleaf + static/"]
    T --> B
    SEED["seed/DemoDataSeeder<br/>solo perfil demo"] -.-> S
    SEED -.-> R
```

## 2. Entidades y restricciones

Cardinalidades y restricciones contrastadas con las entidades JPA y con las migraciones V1–V4:

```mermaid
erDiagram
    AUTOR ||--o{ LIBRO : "escribe"
    EDITORIAL |o--o{ LIBRO : "publica"
    PORTADA |o--o| LIBRO : "ilustra"
    USUARIO ||--o{ PRESTAMO : "solicita"
    LIBRO ||--o{ PRESTAMO : "es prestado en"

    AUTOR {
        string id PK
        string nombre UK
        boolean alta
    }
    EDITORIAL {
        string id PK
        string nombre UK
        boolean alta
    }
    LIBRO {
        string id PK
        string isbn UK "VARCHAR(13), NULL permitido (V2)"
        string titulo "NOT NULL; UK con autor_id (V3)"
        int anio
        int nroejemplares
        int ejemplaresPrestados
        int ejemplaresRestantes
        boolean alta
        string autor_id FK "NOT NULL (V3)"
        string editorial_id FK "NULL permitido"
        string portada_id FK "UNIQUE (V3), NULL permitido"
    }
    PORTADA {
        string id PK
        string nombre
        string mime
        blob contenido
    }
    USUARIO {
        string id PK
        string nombre
        string apellido
        string mail UK
        string clave "BCrypt"
        enum rol "ADMIN | USUARIO"
        datetime alta
        datetime baja "NULL = activo"
        int cantPrestamos "columna cant_prestamos"
    }
    PRESTAMO {
        string id PK
        string usuario_id FK "NOT NULL (V4)"
        string libro_id FK "NOT NULL (V4)"
        datetime fechaPrestamo "columna prestamo"
        datetime fechaVencimiento
        datetime fechaDevolucion "columna devolucion; NULL = activo"
    }
```

Restricción adicional de V3: `UK_libro_titulo_autor` sobre (`titulo`, `autor_id`).

## 3. Solicitud de préstamo (USUARIO)

Las validaciones aparecen en su posición real respecto de cada bloqueo. Las líneas de
cambio (`INSERT`/`UPDATE`) representan **cambios lógicos dentro de la transacción**: no
indican el orden exacto del SQL que emite Hibernate.

```mermaid
sequenceDiagram
    actor U as Usuario
    participant C as PrestamosController
    participant S as ServicePrestamo (@Transactional)
    participant DB as MySQL

    U->>C: POST /prestamo/solicitar (libroId, CSRF)
    Note over C: El usuario sale de la sesión (Authentication), nunca del formulario
    C->>S: solicitar(mail, libroId)
    Note over S: Inicio de transacción (READ_COMMITTED)
    S->>DB: buscar usuario por mail
    S->>DB: bloquear USUARIO (FOR UPDATE)
    Note over S: Valida: usuario existe y no está dado de baja
    S->>DB: contar préstamos activos del usuario
    Note over S: Valida: menos de 3 activos
    S->>DB: bloquear LIBRO (FOR UPDATE)
    Note over S: Valida: libro existe y está activo,<br/>ejemplaresRestantes no nulo y mayor que 0,<br/>ejemplaresPrestados no nulo,<br/>y no hay préstamo activo del mismo libro por este usuario.<br/>No comprueba aquí que nroejemplares = restantes + prestados
    Note over S: Cambios lógicos: nuevo Prestamo (vence a los 14 días)<br/>y contadores del libro (restantes -1, prestados +1)
    S-->>C: retorna el Prestamo
    Note over S,DB: Al salir del método se hace COMMIT
    C-->>U: flash "¡Préstamo solicitado con éxito!"<br/>redirect a /prestamo/mis-prestamos
    alt regla violada
        S-->>C: MiExcepcion → ROLLBACK
        C-->>U: flash con el mensaje, redirect a /libro/biblioteca
    else bloqueo pesimista
        S-->>C: PessimisticLockingFailureException → MiExcepcion<br/>"...modificados por otra operación. Intente nuevamente." → ROLLBACK
        C-->>U: flash con el mensaje, redirect a /libro/biblioteca
    end
```

## 4. Devolución (ADMIN)

```mermaid
sequenceDiagram
    actor A as Admin
    participant C as PrestamosController
    participant S as ServicePrestamo (@Transactional)
    participant DB as MySQL

    A->>C: POST /prestamo/devolver (prestamoId, CSRF)
    C->>S: devolver(prestamoId)
    Note over S: Inicio de transacción (READ_COMMITTED)
    S->>DB: bloquear PRESTAMO (FOR UPDATE)
    Note over S: Valida: el préstamo existe y no fue devuelto
    S->>DB: bloquear LIBRO (FOR UPDATE)
    Note over S: Valida: el libro existe y el stock es consistente:<br/>nroejemplares, restantes y prestados no nulos,<br/>restantes no negativo, prestados mayor que 0,<br/>restantes menor que nroejemplares<br/>y nroejemplares = restantes + prestados
    Note over S: Cambios lógicos: fechaDevolucion = ahora<br/>y contadores del libro (restantes +1, prestados -1)
    S-->>C: retorna el Prestamo
    Note over S,DB: Camino exitoso: al salir del método se hace COMMIT
    C-->>A: flash "Devolución registrada con éxito."<br/>redirect a /prestamo/lista
    Note over C,S: Camino alternativo, en lugar del COMMIT y del éxito:<br/>regla violada o fallo de bloqueo pesimista
    S-->>C: MiExcepcion (o PessimisticLockingFailureException<br/>por bloqueo pesimista) → ROLLBACK
    C-->>A: flash con el mensaje, redirect a /prestamo/lista
```

## 5. Estados del préstamo

```mermaid
stateDiagram-v2
    [*] --> Activo: solicitar (fechaDevolucion = NULL)
    Activo --> Devuelto: devolver (ADMIN)
    Devuelto --> [*]
```

## Notas y límites

- **Estado de los diagramas:** revisados por lectura contra las entidades JPA, las migraciones V1–V4, `ServicePrestamo` y `PrestamosController`. Los cinco diagramas se renderizaron e inspeccionaron visualmente en local (28/10/2026) con las notas actuales de los diagramas 3 y 4. **La comprobación del renderizado en GitHub sigue pendiente.**
- **Fallos de bloqueo:** solo `ServicePrestamo` y `ServiceLibro` capturan `PessimisticLockingFailureException`; `ServiceAutor`, `ServiceEditorial`, `ServicePortada` y `ServiceUsuario` no tienen esa captura.
- **Cardinalidades:** `PORTADA |o--o| LIBRO` refleja la restricción `UNIQUE` de `portada_id` (V3), con ambos lados opcionales.
- **Cobertura de pruebas de catálogo y portadas:** los tests comprueban que las altas, bajas y eliminaciones no mutan por GET y sí por POST con CSRF, las reglas de catálogo y de usuarios, la concurrencia de catálogo y la descarga de portadas creadas por fixture. Ningún test sube por HTTP un archivo de portada con contenido; el único test multipart envía una parte `archivo` vacía con datos inválidos. Alta, modificación, baja y eliminación, y la carga de portadas, se validaron por HTTP contra el JAR del clon de `fdedf95` (sesión `ADMIN`, base demo; ver README, sección Tests), no con navegador.
- Si cambian `ServicePrestamo` o `PrestamosController`, repetir el contraste de las secuencias 3 y 4.
