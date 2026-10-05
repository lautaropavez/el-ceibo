package com.mza.Libreria.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mza.Libreria.seed.support.DemoTestDatabaseInitializer;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * Unitario puro (Mockito, sin MySQL) de la guarda temprana del perfil demo
 * y de la validacion de catalogo que comparte con la guarda autoritativa de
 * DemoDataSeeder. Los casos con SpringApplication real usan un DataSource
 * centinela (mock, sin driver ni socket): ninguno abre una conexion.
 */
class DemoDatabaseEarlyGuardInitializerTest {

    /**
     * Configuracion exclusiva de estos tests. @TestConfiguration la excluye
     * del escaneo de @SpringBootApplication, asi que no se filtra a otros
     * tests. El bean registra si Spring llego a crear el DataSource.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class ConfigCentinela {

        static final AtomicBoolean DATASOURCE_CREADO = new AtomicBoolean(false);

        @Bean
        DataSource dataSource() {
            DATASOURCE_CREADO.set(true);
            return mock(DataSource.class);
        }
    }

    private static final String URL_PREFIJO = "jdbc:mysql://localhost:3306/";
    private static final String URL_SUFIJO = "?useSSL=false&serverTimezone=GMT";

    private final DemoDatabaseEarlyGuardInitializer guarda = new DemoDatabaseEarlyGuardInitializer();

    private ConfigurableEnvironment env;
    private ConfigurableApplicationContext ctx;

    private void entorno(String url, String... perfiles) {
        env = mock(ConfigurableEnvironment.class);
        ctx = mock(ConfigurableApplicationContext.class);
        when(ctx.getEnvironment()).thenReturn(env);
        when(env.getActiveProfiles()).thenReturn(perfiles);
        if (url != null) {
            when(env.getProperty("spring.datasource.url")).thenReturn(url);
        }
    }

    private static String url(String base) {
        return URL_PREFIJO + base + URL_SUFIJO;
    }

    // --- Guarda temprana -------------------------------------------------

    @Test
    void sinPerfilDemo_esNoOpYNoLeeLaUrl() {
        entorno(null, "local");

        assertThatCode(() -> guarda.initialize(ctx)).doesNotThrowAnyException();
        verify(env, never()).getProperty("spring.datasource.url");
    }

    @Test
    void soloPerfilTest_esNoOp() {
        entorno(null, "test");

        assertThatCode(() -> guarda.initialize(ctx)).doesNotThrowAnyException();
        verify(env, never()).getProperty("spring.datasource.url");
    }

    @Test
    void demoSinTest_aceptaSoloLibreriawebDemo() {
        entorno(url("libreriaweb_demo"), "local", "demo");

        assertThatCode(() -> guarda.initialize(ctx)).doesNotThrowAnyException();
    }

    @Test
    void demoSinTest_rechazaBaseRealYOtrasBases() {
        for (String base : new String[] {"libreriaweb", "libreriaweb_testsuite", "otra_base"}) {
            entorno(url(base), "local", "demo");
            assertThatThrownBy(() -> guarda.initialize(ctx))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void demoSinTest_rechazaTestsuiteDemoSinPerfilTest() {
        entorno(url("libreriaweb_demo_testsuite"), "local", "demo");

        assertThatThrownBy(() -> guarda.initialize(ctx)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void demoConTest_aceptaSoloTestsuiteDemo() {
        entorno(url("libreriaweb_demo_testsuite"), "demo", "test");

        assertThatCode(() -> guarda.initialize(ctx)).doesNotThrowAnyException();
    }

    @Test
    void demoConTest_rechazaLibreriawebDemoYBaseRealYSuiteNormal() {
        for (String base : new String[] {"libreriaweb_demo", "libreriaweb", "libreriaweb_testsuite"}) {
            entorno(url(base), "demo", "test");
            assertThatThrownBy(() -> guarda.initialize(ctx))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void urlAusenteOMalformada_seRechaza() {
        for (String malformada : new String[] {
                null,
                "",
                "jdbc:h2:mem:libreriaweb_demo",
                "jdbc:mysql://localhost:3306",
                "jdbc:mysql://localhost:3306/libreriaweb_demo/../libreriaweb",
                "jdbc:mysql://localhost:3306/libreriaweb_demo_x"}) {
            entorno(malformada, "demo");
            assertThatThrownBy(() -> guarda.initialize(ctx))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void nuncaIntentaCrearOBorrar_soloLeeElEntorno() {
        entorno(url("libreriaweb_demo"), "demo");

        guarda.initialize(ctx);

        verify(env).getActiveProfiles();
        verify(env).getProperty("spring.datasource.url");
    }

    // --- Registro via spring.factories y efecto real ----------------------

    // Argumentos de linea de comandos: maxima precedencia, no los pisa
    // application-demo.properties (a diferencia de las default properties).
    private static ConfigurableApplicationContext arrancar(String urlDatasource) {
        ConfigCentinela.DATASOURCE_CREADO.set(false);
        SpringApplication app = new SpringApplication(ConfigCentinela.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.setBannerMode(Banner.Mode.OFF);
        return app.run("--spring.profiles.active=demo",
                "--spring.datasource.url=" + urlDatasource);
    }

    private static Throwable arrancarEsperandoFallo(String urlDatasource) {
        ConfigurableApplicationContext contexto = null;
        try {
            contexto = arrancar(urlDatasource);
            return null;
        } catch (RuntimeException ex) {
            return ex;
        } finally {
            if (contexto != null) {
                contexto.close();
            }
        }
    }

    @Test
    void guardaTemprana_estaRegistradaViaSpringFactories() {
        assertThat(SpringFactoriesLoader.loadFactoryNames(
                ApplicationContextInitializer.class, getClass().getClassLoader()))
                .contains(DemoDatabaseEarlyGuardInitializer.class.getName());

        // El constructor de SpringApplication carga spring.factories sin
        // arrancar ningun contexto.
        SpringApplication app = new SpringApplication(ConfigCentinela.class);
        assertThat(app.getInitializers())
                .anyMatch(i -> i instanceof DemoDatabaseEarlyGuardInitializer);
    }

    @Test
    void configuracionInvalida_seRechazaAntesDeCrearElDataSource() {
        for (String base : new String[] {"libreriaweb", "libreriaweb_testsuite",
                "libreriaweb_demo_testsuite"}) {
            Throwable fallo = arrancarEsperandoFallo(url(base));

            assertThat(fallo).as("arranque con base %s debe fallar", base).isNotNull();
            Throwable raiz = fallo;
            while (raiz.getCause() != null) {
                assertThat(raiz).isNotInstanceOf(SQLException.class);
                raiz = raiz.getCause();
            }
            assertThat(raiz).isNotInstanceOf(SQLException.class)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Base de datos inesperada para el perfil demo");
            assertThat(ConfigCentinela.DATASOURCE_CREADO.get())
                    .as("el DataSource no debio crearse con base %s", base).isFalse();
        }
    }

    @Test
    void controlPositivo_conUrlPermitida_elContextoArrancaYElCentinelaSeActiva() {
        try (ConfigurableApplicationContext contexto = arrancar(url("libreriaweb_demo"))) {
            assertThat(ConfigCentinela.DATASOURCE_CREADO.get()).isTrue();
            assertThat(contexto.getBean(DataSource.class)).isNotNull();
        }
    }

    // --- Orden determinista ---------------------------------------------

    @Test
    void ordenGuarda_esPosteriorAlInitializerDeBaseDemoDeTest() {
        int ordenGuarda = guarda.getOrder();
        int ordenTest = new DemoTestDatabaseInitializer().getOrder();

        assertThat(ordenTest).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
        assertThat(ordenGuarda).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 100);
        assertThat(ordenTest).isLessThan(ordenGuarda);
    }

    // --- Validacion de catalogo (escenarios de la guarda autoritativa) ---

    @Test
    void validarCatalogo_aplicaConjuntoCerradoYReglaDePerfiles() {
        assertThatCode(() -> DemoDatabaseEarlyGuardInitializer.validarCatalogo(
                "libreriaweb_demo", Arrays.asList("local", "demo"))).doesNotThrowAnyException();
        assertThatCode(() -> DemoDatabaseEarlyGuardInitializer.validarCatalogo(
                "libreriaweb_demo_testsuite", Arrays.asList("demo", "test"))).doesNotThrowAnyException();

        for (String catalogo : new String[] {null, "", "libreriaweb", "libreriaweb_testsuite",
                "LIBRERIAWEB_DEMO", "libreriaweb_demo "}) {
            assertThatThrownBy(() -> DemoDatabaseEarlyGuardInitializer.validarCatalogo(
                    catalogo, Arrays.asList("local", "demo")))
                    .isInstanceOf(IllegalStateException.class);
        }
        assertThatThrownBy(() -> DemoDatabaseEarlyGuardInitializer.validarCatalogo(
                "libreriaweb_demo_testsuite", Arrays.asList("local", "demo")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> DemoDatabaseEarlyGuardInitializer.validarCatalogo(
                "libreriaweb_demo", Arrays.asList("demo", "test")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validarCatalogo_sinPerfilDemo_seRechazaAunConNombreValido() {
        assertThatThrownBy(() -> DemoDatabaseEarlyGuardInitializer.validarCatalogo(
                "libreriaweb_demo", Arrays.asList("local")))
                .isInstanceOf(IllegalStateException.class);
    }
}
