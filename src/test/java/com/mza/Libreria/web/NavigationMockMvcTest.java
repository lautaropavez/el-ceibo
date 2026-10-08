package com.mza.Libreria.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.mza.Libreria.support.LibreriaIntegrationTest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * P16.4: la navegacion renderizada debe coincidir con los privilegios reales
 * de backend por rol, sin enlaces muertos (href="#").
 */
@LibreriaIntegrationTest
class NavigationMockMvcTest {

    private static final String[] ADMIN_LINKS_SIDEBAR = {
        "href=\"/libro/lista\"", "href=\"/autor/lista\"",
        "href=\"/editorial/lista\"", "href=\"/usuario/lista\"",
        "href=\"/prestamo/lista\""
    };

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    private static RequestPostProcessor admin() {
        return user("p164-nav-admin@test.com").roles("ADMIN");
    }

    private static RequestPostProcessor usuario() {
        return user("p164-nav-usuario@test.com").roles("USUARIO");
    }

    private String html(String url, RequestPostProcessor actor) throws Exception {
        return mockMvc.perform(get(url).with(actor))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void sidebarAdminTieneEnlacesRealesYSinPlaceholders() throws Exception {
        // Se acota al contenedor del sidebar: la pagina tambien incluye
        // fragments/nav.html, fuera del alcance de este requisito.
        String sidebar = extraerSidebar(html("/autor/lista", admin()));

        for (String link : ADMIN_LINKS_SIDEBAR) {
            assertThat(sidebar).contains(link);
        }
        assertThat(sidebar).doesNotContain("href=\"#\"").doesNotContain("Nosotros");
        assertThat(sidebar).doesNotContain("/prestamo/mis-prestamos");
    }

    /**
     * Devuelve el div.menu__side completo, balanceando divs anidados (una
     * regex que corte en el primer cierre truncaria el sidebar).
     */
    private static String extraerSidebar(String body) {
        int inicio = body.indexOf("<div class=\"menu__side\"");
        assertThat(inicio).as("debe existir el contenedor del sidebar").isGreaterThanOrEqualTo(0);

        Matcher tags = Pattern.compile("<div\\b|</div>").matcher(body);
        int profundidad = 0;
        int pos = inicio;
        while (tags.find(pos)) {
            profundidad += tags.group().startsWith("</") ? -1 : 1;
            pos = tags.end();
            if (profundidad == 0) {
                return body.substring(inicio, pos);
            }
        }
        throw new AssertionError("el contenedor del sidebar no cierra de forma balanceada");
    }

    @Test
    void indexAdminMuestraAccesoAlPanel() throws Exception {
        String body = html("/", admin());

        assertThat(body).contains("href=\"/libro/lista\"");
        assertThat(body).doesNotContain("href=\"/login\"");
    }

    @Test
    void indexUsuarioMuestraBibliotecaSinNavegacionAdmin() throws Exception {
        String body = html("/", usuario());

        assertThat(body).contains("href=\"/libro/biblioteca\"");
        assertThat(body).doesNotContain("/libro/lista").doesNotContain("/usuario/lista");
    }

    @Test
    void bibliotecaAdminVeNavegacionAdminYSinRestosDePlantilla() throws Exception {
        String body = html("/libro/biblioteca", admin());

        assertThat(body).contains("href=\"/libro/lista\"");
        assertThat(body).doesNotContain("/prestamo/mis-prestamos");
        assertBibliotecaSinPlantillaGenerica(body);
    }

    @Test
    void bibliotecaUsuarioVeMisPrestamosSinNavegacionAdmin() throws Exception {
        String body = html("/libro/biblioteca", usuario());

        assertThat(body).contains("href=\"/prestamo/mis-prestamos\"");
        assertThat(body).doesNotContain("/libro/lista");
        assertBibliotecaSinPlantillaGenerica(body);
    }

    @Test
    void bibliotecaAnonimaVeLoginYRegistro() throws Exception {
        String body = mockMvc.perform(get("/libro/biblioteca"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("href=\"/login\"").contains("href=\"/registro\"");
        assertThat(body).doesNotContain("/libro/lista").doesNotContain("/prestamo/mis-prestamos");
        assertBibliotecaSinPlantillaGenerica(body);
    }

    @Test
    void misPrestamosUsuarioUsaFaviconUnificado() throws Exception {
        String body = html("/prestamo/mis-prestamos", usuario());

        Matcher icono = Pattern.compile("<link[^>]*\\brel=\"icon\"[^>]*>").matcher(body);
        assertThat(icono.find()).as("debe existir <link rel=\"icon\">").isTrue();
        assertThat(icono.group()).contains("/img/favicon.ico").doesNotContain("ceibo.svg");
        assertThat(body).as("el logo del navbar sigue siendo ceibo.svg")
                .contains("<img src=\"/img/ceibo.svg\"");
    }

    @Test
    void misPrestamosProhibidoParaAdmin() throws Exception {
        mockMvc.perform(get("/prestamo/mis-prestamos").with(admin()))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginAsociaLabelsConSusCampos() throws Exception {
        String body = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("id=\"username\"").contains("<label for=\"username\">");
        assertThat(body).contains("id=\"password\"").contains("<label for=\"password\">");
        assertThat(body).contains("/img/favicon.ico");
    }

    @Test
    void paginaDeError500EsGenericaYSinDetallesTecnicos() throws Exception {
        String marcador = "MARCADOR-SENSIBLE-P164";
        String body = mockMvc.perform(get("/error")
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr("javax.servlet.error.status_code", 500)
                        .requestAttr("javax.servlet.error.message", marcador + "-MENSAJE")
                        .requestAttr("javax.servlet.error.exception",
                                new IllegalStateException(marcador + "-EXCEPCION")))
                .andExpect(view().name("error/500"))
                .andReturn().getResponse().getContentAsString();

        // La ausencia del marcador solo vale junto con la prueba de que se
        // renderizo la pagina 500 (vista + mensaje generico aprobado).
        assertThat(body).contains("500")
                .contains("Ocurrió un error inesperado. Intentá nuevamente más tarde")
                .contains("Volver al inicio");
        assertThat(body).doesNotContain(marcador);
    }

    private static void assertBibliotecaSinPlantillaGenerica(String body) {
        assertThat(body).doesNotContain("href=\"#!\"").doesNotContain("Shop")
                .doesNotContain("About").doesNotContain("Cart")
                .doesNotContain("All Products");
    }
}
