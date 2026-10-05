package com.mza.Libreria.seed.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Prepara la base EXCLUSIVA de los tests del seeder demo
 * (libreriaweb_demo_testsuite) antes de que Spring construya el
 * DataSource, y fija las propiedades de conexion con maxima precedencia.
 *
 * Solo actua si los perfiles "demo" y "test" estan activos; en cualquier
 * otro caso es un no-op. Corre primero (HIGHEST_PRECEDENCE), antes de
 * DemoDatabaseEarlyGuardInitializer (HIGHEST_PRECEDENCE + 100), que luego
 * valida la URL que esta clase dejo resuelta. Se registra unicamente via
 * @ContextConfiguration en el test de integracion del seeder, nunca de
 * forma global. Host/puerto/usuario/password reutilizan TEST_DB_*.
 *
 * El nombre de la base es una constante fija: cualquier condicion
 * inesperada aborta con excepcion antes de ejecutar DROP/CREATE.
 */
public class DemoTestDatabaseInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

    private static final String DEMO_TEST_DB_NAME = "libreriaweb_demo_testsuite";

    private static final Pattern SAFE_HOST = Pattern.compile("^[a-zA-Z0-9.-]+$");

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment env = applicationContext.getEnvironment();

        List<String> perfiles = Arrays.asList(env.getActiveProfiles());
        if (!perfiles.contains("demo") || !perfiles.contains("test")) {
            return;
        }

        assertSafeDemoTestDatabaseName(DEMO_TEST_DB_NAME);
        assertSafeProfiles(env);

        String host = env.getProperty("TEST_DB_HOST", "localhost");
        String port = env.getProperty("TEST_DB_PORT", "3306");
        String username = env.getProperty("TEST_DB_USERNAME", "root");
        String password = env.getProperty("TEST_DB_PASSWORD", "");

        assertSafeHost(host);
        int portNumber = assertSafePort(port);

        String adminUrl = "jdbc:mysql://" + host + ":" + portNumber
                + "/?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=GMT";

        recreateDatabase(env, adminUrl, username, password);

        String appUrl = "jdbc:mysql://" + host + ":" + portNumber
                + "/" + DEMO_TEST_DB_NAME
                + "?allowPublicKeyRetrieval=true&useSSL=false&useTimezone=true"
                + "&serverTimezone=GMT&characterEncoding=UTF-8";

        Map<String, Object> override = new HashMap<>();
        override.put("spring.datasource.url", appUrl);
        override.put("spring.datasource.username", username);
        override.put("spring.datasource.password", password);
        override.put("spring.jpa.hibernate.ddl-auto", "validate");
        override.put("spring.flyway.enabled", "true");
        override.put("spring.flyway.locations", "classpath:db/migration");

        env.getPropertySources().addFirst(
                new MapPropertySource("libreriaDemoTestDatasourceOverride", override));

        String resolvedUrl = env.getProperty("spring.datasource.url");
        if (resolvedUrl == null || !appUrl.equals(resolvedUrl)) {
            throw new IllegalStateException(
                "La URL de datasource resuelta no coincide exactamente con la URL esperada de la base "
                + "demo de test '" + DEMO_TEST_DB_NAME + "'. Abortando antes del refresh del contexto.");
        }
    }

    private void recreateDatabase(ConfigurableEnvironment env, String adminUrl, String username, String password) {
        assertSafeDemoTestDatabaseName(DEMO_TEST_DB_NAME); // re-chequeo justo antes del DROP
        assertSafeProfiles(env); // re-chequeo justo antes del DROP

        try (Connection conn = DriverManager.getConnection(adminUrl, username, password);
             Statement stmt = conn.createStatement()) {

            stmt.execute("DROP DATABASE IF EXISTS `" + DEMO_TEST_DB_NAME + "`");
            stmt.execute("CREATE DATABASE `" + DEMO_TEST_DB_NAME
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");

        } catch (SQLException ex) {
            throw new IllegalStateException(
                "No se pudo preparar la base demo de test '" + DEMO_TEST_DB_NAME + "'", ex);
        }
    }

    private static void assertSafeDemoTestDatabaseName(String name) {
        if (name == null
                || !name.equals("libreriaweb_demo_testsuite")
                || !name.startsWith("libreriaweb_demo_test")
                || name.equalsIgnoreCase("libreriaweb")
                || name.equalsIgnoreCase("libreriaweb_demo")
                || name.equalsIgnoreCase("libreriaweb_testsuite")) {
            throw new IllegalStateException(
                "Nombre de base demo de test inseguro o inesperado: '" + name
                + "'. Abortando sin ejecutar ningun DROP/CREATE.");
        }
    }

    private static void assertSafeProfiles(ConfigurableEnvironment env) {
        List<String> perfiles = Arrays.asList(env.getActiveProfiles());
        if (!perfiles.contains("demo") || !perfiles.contains("test") || perfiles.contains("local")) {
            throw new IllegalStateException(
                "Perfiles activos inesperados para preparar la base demo de test: "
                + perfiles + ". Abortando.");
        }
    }

    // Validacion de formato unicamente (evita caracteres que permitan
    // inyectar path/query/parametros JDBC en la URL). La proteccion
    // destructiva real es el nombre fijo de la base + las guardas de arriba.
    private static void assertSafeHost(String host) {
        if (host == null || host.isEmpty() || !SAFE_HOST.matcher(host).matches()) {
            throw new IllegalStateException(
                "TEST_DB_HOST invalido: '" + host + "'. Abortando.");
        }
    }

    private static int assertSafePort(String port) {
        int parsed;
        try {
            parsed = Integer.parseInt(port);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("TEST_DB_PORT invalido: '" + port + "'. Abortando.", ex);
        }
        if (parsed < 1 || parsed > 65535) {
            throw new IllegalStateException("TEST_DB_PORT fuera de rango: " + parsed + ". Abortando.");
        }
        return parsed;
    }
}
