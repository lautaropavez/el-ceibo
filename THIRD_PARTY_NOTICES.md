# Third-party notices

El código propio y la documentación propia de El Ceibo se distribuyen bajo Apache
License 2.0 (ver `LICENSE`; decisión del autor, 28/10/2026). Los componentes de terceros
conservan sus licencias y avisos, y las imágenes se documentan por separado en la sección 3.
Este documento lista componentes de terceros y su estado de licencia. Se basa en lo verificable en el repositorio; los puntos marcados
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
`pom.xml`. Las dependencias de alcance `runtime` se incorporan al JAR
ejecutable que genera `spring-boot-maven-plugin`; el contenido efectivo de
`BOOT-INF/lib` se verificó en P16.7 (64 bibliotecas; ver ROADMAP). Las versiones
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
`optional`). El contenido del JAR final se verificó en P16.7: 64 bibliotecas
empaquetadas, sin DevTools (ver ROADMAP). Las licencias de la tabla siguiente se tomaron del `.pom`
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
ejemplo Jackson y SnakeYAML) no forma parte de este documento: se hizo con OSV-Scanner en
P16.7 y consta en el ROADMAP.

## 3. Imágenes y recursos gráficos

Origen declarado por el autor (confirmado el 28/10/2026). Esta tabla registra solo lo
confirmado: no afirma herramientas concretas, autoría exclusiva ni la relación con tapas
comerciales, y tampoco las niega. El autor no declara derechos exclusivos sobre las imágenes
generadas con IA: este documento no les asigna una licencia ni determina su titularidad ni
sus condiciones de uso.

| Archivo(s) | Ubicación | Origen |
|---|---|---|
| `ceibo.svg` (logo) | `src/main/resources/static/img/` | Generado con IA (confirmado por el autor) |
| `favicon.ico` | `src/main/resources/static/img/` | Derivado del logo `ceibo.svg` (comprobado por comparación visual, observación de sesión, sin evidencia archivada: mismos elementos y colores; no se registró el proceso de conversión) |
| `portadas-demo/*.jpg` (8: `portada_1984`, `portada_cien_anios`, `portada_deja_de_ser_tu`, `portada_harry_potter`, `portada_iliada`, `portada_odisea`, `portada_principito`, `portada_rayuela`) | `src/main/resources/static/img/` | Generadas con IA (confirmado por el autor) |
| `librosmano.jpg`, `mujer_festejando1.jpg`, `books-assortment-with-white-background.jpg` (imágenes del inicio; `librosmano.jpg` también se usa en login y registro) | `src/main/resources/static/img/` | Generadas con IA (confirmado por el autor) |
| `libro.jpg` (tapa neutra que se muestra en `biblioteca.html` cuando un libro no tiene portada) | `src/main/resources/static/img/` | Generada con IA (confirmado por el autor) |
| `inicio.png`, `catalogo.png`, `prestamos-admin.png`, `mis-prestamos.png`, `libros-admin.png`, `buscador.png`, `registro-movil.png` | `docs/img/` | Capturas de pantalla de la aplicación en ejecución (perfil demo, datos ficticios). No son imágenes generadas directamente con IA; incluyen, dentro de la captura, las imágenes de las filas anteriores |

`violeta.jpg` fue eliminado del repositorio.

## 4. Recursos cargados desde CDN (no empaquetados)

Estos recursos no forman parte del repositorio: el navegador los descarga en
tiempo de ejecución desde servicios de terceros, bajo sus propios términos.

| Recurso | Uso | Licencia | Origen |
|---|---|---|---|
| Bootstrap 5.0.2 (CSS y JS) y Bootstrap 5.1.3 (JS bundle) | Estilos y componentes | MIT | cdn.jsdelivr.net |
| Bootstrap Icons 1.5.0 | Iconos en `biblioteca.html`, `mis-prestamos.html` | MIT | cdn.jsdelivr.net |
| Font Awesome Free 6.1.0, hoja de estilos `css/all.css` y fuentes `webfonts/` | Iconos del sidebar y de las listas de gestión (`fragments/head.html`) | Iconos CC BY 4.0, fuentes SIL OFL 1.1, código MIT | use.fontawesome.com/releases/v6.1.0 |
| Font Awesome Free 6.1.0, JavaScript `js/all.js` | Script cargado en `index.html`, `login.html` y `registro.html` | Iconos CC BY 4.0, fuentes SIL OFL 1.1, código MIT | use.fontawesome.com/releases/v6.1.0 |

El resto del sitio usa la pila tipográfica del sistema (`system-ui`, `Segoe UI`,
`sans-serif`) y no carga fuentes externas de texto: ninguna vista usa ya Google Fonts (Open Sans,
Catamaran y Lato dejaron de cargarse; sus nombres solo quedan como primera opción
de `font-family` en el CSS del tema, con respaldo de la pila del sistema).

Dependen de la disponibilidad de esos servicios y de la red del visitante.
