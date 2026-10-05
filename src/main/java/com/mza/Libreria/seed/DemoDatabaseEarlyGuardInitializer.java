package com.mza.Libreria.seed;

import java.util.Arrays;
import java.util.Collection;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Guarda temprana del perfil "demo": antes de que Spring construya el
 * DataSource/Flyway/Hibernate, valida que la URL resuelta apunte a la base
 * demo que corresponde segun los perfiles activos. Nunca hace DROP/CREATE ni
 * abre conexiones; solo lee el entorno y aborta si algo no calza.
 *
 * Conjunto cerrado de bases permitidas (constantes, nunca configurables):
 * - demo sin test: unicamente libreriaweb_demo.
 * - demo + test: unicamente libreriaweb_demo_testsuite.
 *
 * Sin el perfil "demo" es un no-op. Se registra en META-INF/spring.factories
 * y corre despues de DemoTestDatabaseInitializer (HIGHEST_PRECEDENCE), que
 * es quien fija la URL de la base exclusiva de tests. La validacion
 * autoritativa (catalogo real de la conexion) vive en DemoDataSeeder.run().
 */
public class DemoDatabaseEarlyGuardInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

    static final String PERFIL_DEMO = "demo";
    static final String PERFIL_TEST = "test";
    static final String BASE_DEMO = "libreriaweb_demo";
    static final String BASE_DEMO_TESTSUITE = "libreriaweb_demo_testsuite";

    private static final String JDBC_PREFIX = "jdbc:mysql://";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment env = applicationContext.getEnvironment();
        Collection<String> perfiles = Arrays.asList(env.getActiveProfiles());
        if (!perfiles.contains(PERFIL_DEMO)) {
            return;
        }
        String url = env.getProperty("spring.datasource.url");
        validarCatalogo(extraerCatalogo(url), perfiles);
    }

    /**
     * Valida el nombre de base contra el conjunto cerrado y la regla de
     * perfiles. Compartido con la guarda autoritativa del seeder.
     */
    static void validarCatalogo(String catalogo, Collection<String> perfiles) {
        if (perfiles == null || !perfiles.contains(PERFIL_DEMO)) {
            throw new IllegalStateException(
                "Perfil demo requerido para validar la base demo. Abortando.");
        }
        String esperado = perfiles.contains(PERFIL_TEST) ? BASE_DEMO_TESTSUITE : BASE_DEMO;
        if (!esperado.equals(catalogo)) {
            throw new IllegalStateException(
                "Base de datos inesperada para el perfil demo: '" + catalogo
                + "' (esperada: '" + esperado + "', perfiles: " + perfiles + "). Abortando.");
        }
    }

    /**
     * Extrae el nombre de base de una URL jdbc:mysql://host:puerto/base?...
     * Cualquier forma no reconocida devuelve null (la validacion la rechaza).
     */
    static String extraerCatalogo(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith(JDBC_PREFIX)) {
            return null;
        }
        String resto = jdbcUrl.substring(JDBC_PREFIX.length());
        int barra = resto.indexOf('/');
        if (barra < 0) {
            return null;
        }
        String ruta = resto.substring(barra + 1);
        int query = ruta.indexOf('?');
        return query < 0 ? ruta : ruta.substring(0, query);
    }
}
