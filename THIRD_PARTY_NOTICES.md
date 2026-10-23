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
| Start Bootstrap — One Page Wonder (tema) | `css/style.css`, `css/login-style.css`, `css/legacy-hero.css` | 6.0.5 | MIT, Copyright 2013-2022 Start Bootstrap | https://startbootstrap.com/theme/one-page-wonder |
| Start Bootstrap — Shop Homepage (tema) | `css/biblioteca.css` | 5.0.5 | MIT, Copyright 2013-2022 Start Bootstrap | https://startbootstrap.com/template/shop-homepage |

Las cabeceras de copyright y licencia originales se conservan dentro de esos
archivos CSS.

## 2. Dependencias Java empaquetadas en el JAR

No están en el código fuente del repositorio: Maven las resuelve desde
`pom.xml`. Las dependencias de alcance `runtime` se incorporan al JAR
ejecutable que genera `spring-boot-maven-plugin`; el contenido efectivo de
`BOOT-INF/lib` se verificará en P16.7. Las versiones
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
| Flyway Community (`flyway-core`, `flyway-mysql`) | 8.5.13 | Apache-2.0 (ver nota) |
| MySQL Connector/J (`mysql-connector-j`, scope `runtime`) | 8.4.0 | GPL-2.0 con Universal FOSS Exception 1.0 |
| Logback Classic y Logback Core (`logback-classic`, `logback-core`) | 1.2.12 | Eclipse Public License 1.0 (EPL-1.0) y GNU Lesser General Public License 2.1 (LGPL-2.1): dos opciones de licencia declaradas (ver nota) |

Nota sobre Flyway: los `.pom` de `flyway-core` y `flyway-mysql` no declaran
la licencia; la declara su POM padre, `flyway-parent` 8.5.13 (Apache License,
Version 2.0, con URL `https://flywaydb.org/licenses/flyway-community`). Los JAR
de ambos módulos incluyen `META-INF/LICENSE.txt` con el texto de la Apache
License 2.0 (Copyright Red Gate Software Ltd 2010-2022; comprobado en ambos),
y su `MANIFEST.MF` apunta a esa misma URL en `Bundle-License`.

Nota sobre Logback: la licencia no figura en el `.pom` de `logback-classic` ni
en el de `logback-core`, sino en su POM padre, `logback-parent` 1.2.12, que
declara las dos licencias (EPL-1.0 y LGPL-2.1). El `MANIFEST.MF` del JAR
`logback-classic` 1.2.12 las confirma en `Bundle-License`
(`http://www.eclipse.org/legal/epl-v10.html` y
`http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html`). Esta evidencia
describe qué licencias declara el proyecto; este documento no determina cuál
se elige ni cómo se cumplen las obligaciones de redistribución de cada una.

`spring-boot-devtools` está marcado `optional` y se excluye del JAR
reempaquetado por defecto. Las dependencias de test (`spring-boot-starter-test`,
`spring-security-test`) no se empaquetan.

### Inventario efectivo y dependencias transitivas

La tabla anterior cubre las dependencias principales. Se ejecutó
`mvn dependency:list` con alcance `runtime` (modo offline) sobre el commit
`97567c7`, y resolvió 71 artefactos de ese alcance. Ese listado es el
resultado de la resolución de Maven: **no demuestra por sí solo cuáles quedan
dentro del JAR final** (por ejemplo, incluye `spring-boot-devtools`, marcado
`optional`). El contenido del JAR final se confirmará en la verificación de
publicación (P16.7). Las licencias de la tabla siguiente se tomaron del `.pom`
de cada artefacto o de sus POM padres, y se contrastaron con los metadatos de
los JAR cuando están disponibles (`Bundle-License` en el `MANIFEST.MF`,
archivos `LICENSE`/`NOTICE`).

Dependencias transitivas con licencias distintas de Apache-2.0 que no figuraban
arriba. Cuando se declaran dos o más licencias, son alternativas ofrecidas por
el proyecto y se mantienen distinguidas; este documento no elige cuál se
aplica ni declara resueltas las obligaciones de redistribución de ninguna:

| Dependencia | Versión | Licencia(s) declarada(s) | Evidencia |
|---|---|---|---|
| AspectJ Weaver (`aspectjweaver`) | 1.9.7 | Eclipse Public License 2.0 | `.pom` |
| Jakarta Transaction API (`jakarta.transaction-api`) | 1.3.3 | EPL-2.0 o GPL-2.0 con Classpath Exception | `.pom`; `Bundle-License` del JAR |
| Jakarta Annotations API (`jakarta.annotation-api`) | 1.3.5 | EPL-2.0 o GPL-2.0 con Classpath Exception | `.pom`; `Bundle-License` del JAR; `LICENSE.md` y `NOTICE.md` |
| Jakarta Persistence API (`jakarta.persistence-api`) | 2.2.3 | EPL-2.0 o Eclipse Distribution License 1.0 | `.pom`; `Bundle-License` del JAR |
| JAXB Runtime y TXW2 (`org.glassfish.jaxb`) | 2.3.9 | Eclipse Distribution License 1.0 | POM padre `jaxb-parent`; `LICENSE.md` y `NOTICE.md` en los JAR |
| Istack Commons Runtime (`istack-commons-runtime`) | 3.0.12 | Eclipse Distribution License 1.0 | POM padre `istack-commons`; `LICENSE.md` y `NOTICE.md` |
| Jakarta XML Bind API (`jakarta.xml.bind-api`) | 2.3.3 | Eclipse Distribution License 1.0 | POM padre; `LICENSE.md` y `NOTICE.md` |
| Jakarta Activation API (`jakarta.activation-api`) y `com.sun.activation:jakarta.activation` | 1.2.2 | Eclipse Distribution License 1.0 (EDL 1.0) | POM padre `com.sun.activation:all`; `LICENSE.md` y `NOTICE.md` |
| ANTLR (`antlr`) | 2.7.7 | BSD License (según el `.pom`) | `.pom` |
| Hibernate Commons Annotations | 5.1.2.Final | GNU LGPL 2.1 o posterior | `.pom` |
| Logback Core (`logback-core`) | 1.2.12 | EPL-1.0 y LGPL-2.1 (ver nota de Logback) | POM padre `logback-parent` |
| SLF4J (`slf4j-api`, `jul-to-slf4j`) | 1.7.36 | MIT | POM padre `slf4j-parent` |

El resto de las dependencias de alcance `runtime` resueltas (Spring Framework
5.3.31, Spring Data 2.7.18, Spring Security 5.7.11, Jackson 2.13.5, HikariCP
4.0.3, JBoss Logging, Jandex, Classmate, Byte Buddy, Log4j 2.17.2, SnakeYAML
1.30, Attoparser, Unbescape, Tomcat embebido, entre otras) declara Apache
License 2.0. El análisis de vulnerabilidades de versiones concretas (por
ejemplo Jackson y SnakeYAML) no forma parte de este documento y se hace en la
verificación de publicación.

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
| Font Awesome Free 6.1.0, hoja de estilos `css/all.css` y fuentes `webfonts/` | Iconos del sidebar y de las listas de gestión (`fragments/head.html`) | Iconos CC BY 4.0, fuentes SIL OFL 1.1, código MIT | use.fontawesome.com/releases/v6.1.0 |
| Font Awesome Free 6.1.0, JavaScript `js/all.js` | Script cargado en `index.html`, `login.html`, `registro.html` y `exito.html` | Iconos CC BY 4.0, fuentes SIL OFL 1.1, código MIT | use.fontawesome.com/releases/v6.1.0 |
| Google Fonts: Catamaran y Lato | Tipografías de `exito.html` | SIL Open Font License 1.1 | fonts.googleapis.com |

El resto del sitio usa la pila tipográfica del sistema (`system-ui`, `Segoe UI`,
`sans-serif`) y no carga fuentes externas de texto. Open Sans, Catamaran y Lato
dejaron de cargarse en las demás vistas.

Dependen de la disponibilidad de esos servicios y de la red del visitante.
