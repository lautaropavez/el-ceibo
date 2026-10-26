package com.mza.Libreria.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mza.Libreria.support.LibreriaIntegrationTest;
import java.util.Arrays;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Control compensatorio de CVE-2026-22732: las cabeceras de seguridad deben
 * escribirse aunque la aplicacion fije su propio Cache-Control, y las
 * politicas existentes (nosniff, frame DENY, no-store) deben conservarse.
 */
@LibreriaIntegrationTest
class SecurityHeadersMockMvcTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    private HeaderWriterFilter headerWriterFilter() {
        return springSecurityFilterChain.getFilterChains().stream()
                .flatMap(c -> c.getFilters().stream())
                .filter(HeaderWriterFilter.class::isInstance)
                .map(HeaderWriterFilter.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("la cadena debe incluir HeaderWriterFilter"));
    }

    @Test
    void headerWriterFilterEscribeCabecerasDeFormaEager() {
        assertThat(ReflectionTestUtils.getField(headerWriterFilter(), "shouldWriteHeadersEagerly"))
                .isEqualTo(Boolean.TRUE);
    }

    @Test
    void cabecerasDeSeguridadSeEscribenAunqueLaAplicacionFijeCacheControl() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/libro/biblioteca");
        MockHttpServletResponse res = new MockHttpServletResponse();

        headerWriterFilter().doFilter(req, res, (rq, rs) -> {
            ((HttpServletResponse) rs).setHeader("Cache-Control", "max-age=60");
            rs.getWriter().write("ok");
            rs.flushBuffer();
        });

        assertThat(res.getHeader("Cache-Control")).isEqualTo("max-age=60");
        assertThat(res.getHeader("Pragma")).isEqualTo("no-cache");
        assertThat(res.getHeader("Expires")).isEqualTo("0");
        assertThat(res.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(res.getHeader("X-Frame-Options")).isEqualTo("DENY");
    }

    @Test
    void paginasPublicasConservanCabecerasYPoliticaDeCache() throws Exception {
        for (String url : Arrays.asList("/login", "/libro/biblioteca")) {
            MockHttpServletResponse res = mockMvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andReturn().getResponse();

            assertThat(res.getHeader("X-Content-Type-Options")).as(url).isEqualTo("nosniff");
            assertThat(res.getHeader("X-Frame-Options")).as(url).isEqualTo("DENY");
            assertThat(res.getHeader("Cache-Control")).as(url).contains("no-store");
            assertThat(res.getHeader("Pragma")).as(url).isEqualTo("no-cache");
            assertThat(res.getHeader("Expires")).as(url).isEqualTo("0");
        }
    }

    @Test
    void recursosEstaticosConservanCabeceras() throws Exception {
        MockHttpServletResponse res = mockMvc.perform(get("/css/style.css"))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        assertThat(res.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(res.getHeader("Cache-Control")).isNotNull();
    }
}
