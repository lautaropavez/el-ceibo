# Third-party notices

El código propio de este repositorio se distribuye bajo Apache License 2.0
(ver `LICENSE`). Este documento lista componentes de terceros y su estado de
licencia. Se basa en lo verificable en el repositorio; los puntos marcados
como **pendiente** requieren confirmación antes de la publicación.

## 1. Recursos frontend locales (empaquetados en el repositorio)

Estos archivos están dentro de `src/main/resources/static/` y se distribuyen
con el repositorio.

| Componente | Archivos | Versión | Licencia | Fuente |
|---|---|---|---|---|
| Bootstrap CSS (incluido en el CSS del tema) | `css/style.css`, `css/login-style.css`, `css/biblioteca.css` | 5.1.3 | MIT, Copyright 2011-2021 The Bootstrap Authors | https://getbootstrap.com |
| Start Bootstrap — One Page Wonder (tema) | `css/style.css`, `css/login-style.css` | 6.0.5 | MIT, Copyright 2013-2022 Start Bootstrap | https://startbootstrap.com/theme/one-page-wonder |
| Start Bootstrap — Shop Homepage (tema) | `css/biblioteca.css` | 5.0.5 | MIT, Copyright 2013-2022 Start Bootstrap | https://startbootstrap.com/template/shop-homepage |

Las cabeceras de copyright y licencia originales se conservan dentro de esos
archivos CSS.

## 2. Dependencias Java empaquetadas en el JAR

No están en el código fuente del repositorio: Maven las resuelve desde
`pom.xml`. Sí quedan **empaquetadas dentro del JAR ejecutable** que genera
`spring-boot-maven-plugin`, y se redistribuyen con él. Las versiones
corresponden a las gestionadas por Spring Boot 2.7.18, salvo los overrides
explícitos de `pom.xml`: MySQL Connector/J 8.4.0 y Apache Tomcat 9.0.121. La
licencia se tomó del `.pom` de cada artefacto.

| Dependencia | Versión | Licencia |
|---|---|---|
| Spring Boot (starters web, data-jpa, security, thymeleaf) y `spring-boot` | 2.7.18 | Apache-2.0 |
| Spring Security (`spring-security-web`) | 5.7.11 | Apache-2.0 |
| Thymeleaf | 3.0.15.RELEASE | Apache-2.0 |
| Thymeleaf Extras Spring Security 5 | 3.0.5.RELEASE | Apache-2.0 |
| Hibernate ORM (`hibernate-core`) | 5.6.15.Final | LGPL-2.1-or-later |
| Apache Tomcat embebido (`tomcat-embed-core`) | 9.0.121 | Apache-2.0 |
| Flyway Community (`flyway-core`, `flyway-mysql`) | 8.5.13 | Apache-2.0 (verificado en `META-INF/LICENSE.txt` del jar `flyway-core` 8.5.13; el `.pom` no declara el campo `license`) |
| MySQL Connector/J (`mysql-connector-j`, scope `runtime`) | 8.4.0 | GPL-2.0 con Universal FOSS Exception 1.0 |
| Logback Classic | 1.2.12 | EPL-1.0 / LGPL-2.1 (doble licencia; el `.pom` local no declara el campo `license`) |

`spring-boot-devtools` está marcado `optional` y se excluye del JAR
reempaquetado por defecto. Las dependencias de test (`spring-boot-starter-test`,
`spring-security-test`) no se empaquetan.

Esta tabla cubre las dependencias principales. Hay dependencias transitivas
adicionales (Jackson, Jakarta, SLF4J, Spring Framework, HikariCP, etc.), cada
una con su propia licencia. El listado completo con
`mvn dependency:list` queda pendiente para el cierre de publicación.

## 3. Imágenes y recursos gráficos

Los siguientes archivos de `src/main/resources/static/img/` fueron
reemplazados en P16.4 por assets nuevos, generados con herramientas de IA y
seleccionados por el autor para este proyecto. Luego se convirtieron
a JPEG, se redimensionaron y se comprimieron (sin cambios de estilo ni
agregado de texto):

- `ceibo.svg`: logo SVG original; `favicon.ico`: derivado de ese logo;
- `libro.jpg`: tapa neutra de reemplazo (placeholder);
- `portadas-demo/*.jpg`: 8 ilustraciones de portada sin texto;
- `librosmano.jpg`, `mujer_festejando1.jpg`,
  `books-assortment-with-white-background.jpg`: 3 ilustraciones del inicio.

No son fotografías reales, obras dibujadas manualmente ni recursos descargados
de bancos de imágenes. Las portadas son ilustraciones de ejemplo, no
reproducciones de tapas comerciales.

`violeta.jpg` fue eliminado del repositorio.

## 4. Recursos cargados desde CDN (no empaquetados)

Estos recursos no forman parte del repositorio: el navegador los descarga en
tiempo de ejecución desde servicios de terceros, bajo sus propios términos.

| Recurso | Uso | Licencia | Origen |
|---|---|---|---|
| Bootstrap 5.0.2 (CSS y JS) y Bootstrap 5.1.3 (JS bundle) | Estilos y componentes | MIT | cdn.jsdelivr.net |
| Bootstrap Icons 1.5.0 | Iconos en `biblioteca.html`, `mis-prestamos.html` | MIT | cdn.jsdelivr.net |
| Font Awesome Free 6.1.0 | Iconos | Iconos CC BY 4.0, fuentes SIL OFL 1.1, código MIT | use.fontawesome.com |
| Font Awesome Kit | Iconos del sidebar (`fragments/head.html`) | Según el kit; **pendiente** de verificar la cuenta/versión | kit.fontawesome.com |
| Google Fonts: Catamaran, Lato, Open Sans | Tipografías | SIL Open Font License 1.1 | fonts.googleapis.com |

Dependen de la disponibilidad de esos servicios y de la red del visitante.
