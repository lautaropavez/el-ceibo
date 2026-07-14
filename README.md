# Librería Web El Ceibo

Aplicación web CRUD para la gestión de una librería, desarrollada como proyecto
académico con **Java, Spring Boot, Spring Data JPA, MySQL y Thymeleaf**.
Implementa una arquitectura **MVC** con separación por capas entre controladores,
servicios, repositorios y entidades.

> Proyecto académico (2022). No es una aplicación productiva: el foco estuvo en
> practicar arquitectura MVC, persistencia relacional con JPA y renderizado de
> vistas con Thymeleaf.

## Objetivo

Practicar el desarrollo de una aplicación web completa con el stack Java/Spring,
modelando el dominio de una librería y resolviendo la persistencia de datos en
una base relacional.

## Funcionalidades

- Gestión (alta, baja, modificación y listado) de **libros**
- Gestión de **autores**
- Gestión de **editoriales**
- Gestión básica de **usuarios** (registro y administración)
- Carga y visualización de **portadas** de libros
- Persistencia en base de datos **MySQL** mediante Spring Data JPA
- Vistas renderizadas con **Thymeleaf**
- Organización por capas siguiendo el patrón **MVC**

## Tecnologías

| Capa | Tecnología |
|------|------------|
| Lenguaje | Java 8 |
| Framework | Spring Boot 2.7.0 |
| Persistencia | Spring Data JPA (Hibernate) |
| Base de datos | MySQL |
| Vistas | Thymeleaf |
| Build | Maven |

## Estructura del proyecto

```
src/main/java/com/mza/Libreria/
├── controladores/   # Controladores MVC (manejan las peticiones HTTP)
├── servicios/       # Lógica de negocio
├── repositorios/    # Acceso a datos (Spring Data JPA)
├── entidades/       # Entidades del dominio (Libro, Autor, Editorial, Usuario...)
├── enumeradores/    # Enums (Rol)
└── excepciones/     # Excepción propia (MiExcepcion)

src/main/resources/
├── templates/       # Vistas Thymeleaf
└── static/          # CSS, JS e imágenes
```

## Cómo ejecutar localmente

Requisitos: **JDK 8**, **Maven** y **MySQL** corriendo localmente.

1. Crear la base de datos:

   ```sql
   CREATE DATABASE libreriaweb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. Configurar las credenciales de tu MySQL local en
   `src/main/resources/application.properties` (`spring.datasource.username` y
   `spring.datasource.password`). Las que vienen por defecto son de ejemplo y
   deben adaptarse a tu entorno.

3. Levantar la aplicación:

   ```bash
   mvn spring-boot:run
   ```

4. Abrir el navegador en `http://localhost:8080`.

## Estado y posibles mejoras

Este proyecto fue un ejercicio académico. Si lo retomara hoy, lo evolucionaría con:

- Autenticación y autorización reales con **Spring Security** + **BCrypt**
- Credenciales externalizadas mediante **variables de entorno**
- **Tests** automatizados (unitarios y de integración)
- Contenerización con **Docker**

## Autor

**Lautaro Pavez** — [GitHub](https://github.com/lautaropavez)
