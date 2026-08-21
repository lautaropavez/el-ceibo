package com.mza.Libreria.support;

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
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Prepara la base de datos de test (libreriaweb_testsuite) ANTES de que
 * Spring intente construir el DataSource/Flyway/Hibernate, y fija las
 * propiedades de conexion con maxima precedencia para que ninguna
 * variable de entorno externa (SPRING_DATASOURCE_URL, DB_URL, etc.)
 * pueda redirigir el DataSource de test hacia otra base.
 *
 * El nombre de la base es una constante fija: nunca se lee de
 * configuracion externa, nunca se normaliza si algo no calza -
 * cualquier condicion inesperada aborta con excepcion.
 */
public class TestDatabaseInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String TEST_DB_NAME = "libreriaweb_testsuite";

    private static final Pattern SAFE_HOST = Pattern.compile("^[a-zA-Z0-9.-]+$");

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment env = applicationContext.getEnvironment();

        assertSafeTestDatabaseName(TEST_DB_NAME);
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
                + "/" + TEST_DB_NAME
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
                new MapPropertySource("libreriaTestDatasourceOverride", override));

        String resolvedUrl = env.getProperty("spring.datasource.url");
        if (resolvedUrl == null || !appUrl.equals(resolvedUrl)) {
            throw new IllegalStateException(
                "La URL de datasource resuelta ('" + resolvedUrl
                + "') no coincide exactamente con la URL de test esperada ('"
                + appUrl + "'). Abortando antes del refresh del contexto.");
        }
    }

    private void recreateDatabase(ConfigurableEnvironment env, String adminUrl, String username, String password) {
        assertSafeTestDatabaseName(TEST_DB_NAME); // re-chequeo justo antes del DROP
        assertSafeProfiles(env); // re-chequeo justo antes del DROP

        try (Connection conn = DriverManager.getConnection(adminUrl, username, password);
             Statement stmt = conn.createStatement()) {

            stmt.execute("DROP DATABASE IF EXISTS `" + TEST_DB_NAME + "`");
            stmt.execute("CREATE DATABASE `" + TEST_DB_NAME
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");

        } catch (SQLException ex) {
            throw new IllegalStateException(
                "No se pudo preparar la base de test '" + TEST_DB_NAME + "'", ex);
        }
    }

    private static void assertSafeTestDatabaseName(String name) {
        if (name == null
                || !name.equals("libreriaweb_testsuite")
                || !name.startsWith("libreriaweb_test")
                || name.equalsIgnoreCase("libreriaweb")) {
            throw new IllegalStateException(
                "Nombre de base de test inseguro o inesperado: '" + name
                + "'. Abortando sin ejecutar ningun DROP/CREATE.");
        }
    }

    private static void assertSafeProfiles(ConfigurableEnvironment env) {
        List<String> perfiles = Arrays.asList(env.getActiveProfiles());
        if (!perfiles.contains("test") || perfiles.contains("local") || perfiles.contains("demo")) {
            throw new IllegalStateException(
                "Perfiles activos inesperados para preparar la base de test: "
                + perfiles + ". Abortando.");
        }
    }

    // Validacion de formato unicamente (evita caracteres que permitan
    // inyectar path/query/parametros JDBC en la URL). No es una
    // garantia de que el host sea local: la proteccion destructiva
    // real es el nombre fijo de la base + las guardas de arriba.
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
