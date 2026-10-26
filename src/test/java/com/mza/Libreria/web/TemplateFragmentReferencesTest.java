package com.mza.Libreria.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Las referencias a fragmentos no deben empezar con barra: dentro de un JAR
 * ejecutable "templates//fragments/x.html" no se encuentra (desde directorio
 * si), y todas las paginas HTML respondian 500 en java -jar sin que los tests
 * MockMvc, que corren desde target/classes, lo detectaran. No necesita base de
 * datos. La comprobacion sobre el JAR ejecutable real esta en
 * scripts/smoke-jar-demo.ps1.
 */
class TemplateFragmentReferencesTest {

    private static final Pattern REFERENCIA = Pattern.compile(
            "th:(?:replace|insert|include)\\s*=\\s*\"\\s*(?:~\\{)?\\s*([^\":}\\s]+)");

    private static Path raizPlantillas() throws URISyntaxException {
        URL url = TemplateFragmentReferencesTest.class.getClassLoader().getResource("templates");
        assertThat(url).as("debe existir templates/ en el classpath").isNotNull();
        return Paths.get(url.toURI());
    }

    private static List<Path> plantillas(Path raiz) throws IOException {
        try (Stream<Path> s = Files.walk(raiz)) {
            return s.filter(p -> p.toString().endsWith(".html")).collect(Collectors.toList());
        }
    }

    private static List<String> referencias(Path raiz) throws IOException {
        List<String> refs = new ArrayList<>();
        for (Path p : plantillas(raiz)) {
            Matcher m = REFERENCIA.matcher(new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
            while (m.find()) {
                refs.add(m.group(1));
            }
        }
        return refs;
    }

    @Test
    void ningunaReferenciaAFragmentoEmpiezaConBarra() throws Exception {
        List<String> refs = referencias(raizPlantillas());

        assertThat(refs).as("referencias a fragmentos encontradas").hasSizeGreaterThan(60);
        assertThat(refs).noneMatch(r -> r.startsWith("/"));
    }

    @Test
    void todasLasReferenciasResuelvenDentroDeUnJarEmpaquetado() throws Exception {
        Path raiz = raizPlantillas();
        Path jar = Files.createTempFile("plantillas-", ".jar");
        try {
            try (OutputStream os = Files.newOutputStream(jar);
                 JarOutputStream out = new JarOutputStream(os)) {
                for (Path p : plantillas(raiz)) {
                    out.putNextEntry(new JarEntry("templates/" + raiz.relativize(p).toString().replace('\\', '/')));
                    out.write(Files.readAllBytes(p));
                    out.closeEntry();
                }
            }
            try (URLClassLoader cl = new URLClassLoader(new URL[] {jar.toUri().toURL()}, null)) {
                List<String> noResueltas = referencias(raiz).stream()
                        .filter(r -> cl.getResource("templates/" + r + ".html") == null)
                        .distinct()
                        .collect(Collectors.toList());

                assertThat(noResueltas).as("fragmentos no resueltos dentro de un JAR").isEmpty();
            }
        } finally {
            Files.deleteIfExists(jar);
        }
    }
}
