package com.mza.Libreria.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mza.Libreria.entidades.Autor;
import com.mza.Libreria.entidades.Editorial;
import com.mza.Libreria.entidades.Libro;
import com.mza.Libreria.entidades.Usuario;
import com.mza.Libreria.enumeradores.Rol;
import com.mza.Libreria.excepciones.MiExcepcion;
import com.mza.Libreria.repositorios.AutorRepository;
import com.mza.Libreria.repositorios.EditorialRepository;
import com.mza.Libreria.repositorios.LibroRepository;
import com.mza.Libreria.repositorios.PrestamoRepository;
import com.mza.Libreria.repositorios.UsuarioRepository;
import com.mza.Libreria.servicios.ServiceLibro;
import com.mza.Libreria.support.LibreriaIntegrationTest;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@LibreriaIntegrationTest
class CsrfProtectionMockMvcTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ServiceLibro servicioLibro;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AutorRepository autorRepo;

    @Autowired
    private EditorialRepository editorialRepo;

    @Autowired
    private LibroRepository libroRepo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private PrestamoRepository prestamoRepo;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    // ---- 1. Formularios renderizados: input _csrf oculto dentro del form correcto ----

    @Test
    void loginPage_formLogincheckRenderizaInputCsrfOculto() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn();

        assertFormTieneCsrfOculto(resultado.getResponse().getContentAsString(), "/logincheck");
    }

    @Test
    void registroPage_formRegistrarRenderizaInputCsrfOculto() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andReturn();

        assertFormTieneCsrfOculto(resultado.getResponse().getContentAsString(), "/registrar");
    }

    @Test
    void listAutorPage_formAdminRenderizaInputCsrfOculto() throws Exception {
        Autor autor = crearAutorActivo("render");

        MvcResult resultado = mockMvc.perform(get("/autor/lista")
                        .with(user("p154-render-admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        assertFormTieneCsrfOculto(
                resultado.getResponse().getContentAsString(), "/autor/baja/" + autor.getId());
    }

    @Test
    void bibliotecaPage_formSolicitarRenderizaInputCsrfOculto() throws Exception {
        crearLibroConStock("render-biblioteca");

        MvcResult resultado = mockMvc.perform(get("/libro/biblioteca")
                        .with(user("p154-render-usuario@test.com").roles("USUARIO")))
                .andExpect(status().isOk())
                .andReturn();

        assertFormTieneCsrfOculto(resultado.getResponse().getContentAsString(), "/prestamo/solicitar");
    }

    @Test
    void misPrestamosPage_formLogoutRenderizaInputCsrfOculto() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/prestamo/mis-prestamos")
                        .with(user("p154-render-mis-prestamos@test.com").roles("USUARIO")))
                .andExpect(status().isOk())
                .andReturn();

        assertFormTieneCsrfOculto(resultado.getResponse().getContentAsString(), "/logout");
    }

    // ---- 2. /registrar ----

    @Test
    void registrar_conCsrfValido_creaUsuarioYMuestraExito() throws Exception {
        String mail = "p154-registrar-ok@test.com";

        mockMvc.perform(post("/registrar")
                        .param("nombre", "P154")
                        .param("apellido", "Registrar")
                        .param("mail", mail)
                        .param("clave1", "ClaveSegura123")
                        .param("clave2", "ClaveSegura123")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("exito"));

        assertThat(usuarioRepo.buscarPorMail(mail)).isNotNull();
    }

    @Test
    void registrar_sinCsrf_rechazado403YNoCreaUsuario() throws Exception {
        String mail = "p154-registrar-sin-csrf@test.com";

        mockMvc.perform(post("/registrar")
                        .param("nombre", "P154")
                        .param("apellido", "Registrar")
                        .param("mail", mail)
                        .param("clave1", "ClaveSegura123")
                        .param("clave2", "ClaveSegura123"))
                .andExpect(status().isForbidden());

        assertThat(usuarioRepo.buscarPorMail(mail)).isNull();
    }

    // ---- 3. /logincheck ----

    @Test
    void logincheck_conCsrfValido_autenticaYRedirige() throws Exception {
        String mail = "p154-logincheck-ok@test.com";
        crearUsuarioConClave(mail, "ClaveSegura123");

        mockMvc.perform(post("/logincheck")
                        .param("username", mail)
                        .param("password", "ClaveSegura123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/libro/biblioteca"));
    }

    @Test
    void logincheck_sinCsrf_rechazado403YNoAutentica() throws Exception {
        String mail = "p154-logincheck-sin-csrf@test.com";
        crearUsuarioConClave(mail, "ClaveSegura123");

        mockMvc.perform(post("/logincheck")
                        .param("username", mail)
                        .param("password", "ClaveSegura123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void logincheck_conPasswordSobredimensionada_fallaControladoSinSesion() throws Exception {
        String mail = "p162a-logincheck-sobredimensionada@test.com";
        crearUsuarioConClave(mail, "ClaveSegura123");
        String passwordSobredimensionada = repetir('a', 73);

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/logincheck")
                        .param("username", mail)
                        .param("password", passwordSobredimensionada)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        mockMvc.perform(get("/prestamo/mis-prestamos").session(session))
                .andExpect(status().is3xxRedirection());
    }

    // Regresión del control de 72 bytes por la cadena completa de /logincheck (Spring Security real,
    // UsuarioDetailsService y el encoder de la aplicación). La clave del usuario es la propia de 72 bytes,
    // de modo que el rechazo del segundo intento se debe solo al byte adicional. CSRF válido y
    // sesiones independientes en cada intento.
    @Test
    void logincheck_conPasswordDe72Bytes_autenticaYConElMismoPrefijoMasUnCaracter_rechazaSinSesion() throws Exception {
        String mail = "p165-logincheck-72bytes@test.com";
        String password72 = repetir('a', 72);
        crearUsuarioConClave(mail, password72);

        MockHttpSession sesionValida = new MockHttpSession();
        mockMvc.perform(post("/logincheck")
                        .param("username", mail)
                        .param("password", password72)
                        .session(sesionValida)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/libro/biblioteca"))
                .andExpect(authenticated().withUsername(mail));
        mockMvc.perform(get("/prestamo/mis-prestamos").session(sesionValida))
                .andExpect(status().isOk());

        MockHttpSession sesionRechazada = new MockHttpSession();
        mockMvc.perform(post("/logincheck")
                        .param("username", mail)
                        .param("password", password72 + "b")
                        .session(sesionRechazada)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
        mockMvc.perform(get("/prestamo/mis-prestamos").session(sesionRechazada))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // ---- 4. /logout ----

    @Test
    void logout_conCsrfValido_cierraSesionYRedirigeALogin() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/prestamo/mis-prestamos")
                        .with(user("p154-logout-ok@test.com").roles("USUARIO"))
                        .session(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void logout_sinCsrf_rechazado403YSesionSigueActiva() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/prestamo/mis-prestamos")
                        .with(user("p154-logout-sin-csrf@test.com").roles("USUARIO"))
                        .session(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/logout").session(session))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/prestamo/mis-prestamos").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void logout_conCsrfInvalido_rechazado403() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/prestamo/mis-prestamos")
                        .with(user("p154-logout-csrf-invalido@test.com").roles("USUARIO"))
                        .session(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/logout").session(session).with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getLogout_noCierraLaSesion() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/prestamo/mis-prestamos")
                        .with(user("p154-get-logout@test.com").roles("USUARIO"))
                        .session(session))
                .andExpect(status().isOk());

        MvcResult resultadoGetLogout = mockMvc.perform(get("/logout").session(session)).andReturn();
        assertThat(resultadoGetLogout.getResponse().getRedirectedUrl())
                .as("GET /logout no debe redirigir a /login?logout: eso solo puede ocurrir via POST")
                .isNotEqualTo("/login?logout");

        mockMvc.perform(get("/prestamo/mis-prestamos").session(session))
                .andExpect(status().isOk());
    }

    // ---- 5. Categorías representativas restantes sin token ----

    @Test
    void prestamoSolicitar_sinCsrf_rechazado403YNoCreaPrestamo() throws Exception {
        Usuario usuario = crearUsuario("solicitar-sin-csrf");
        Libro libro = crearLibroConStock("solicitar-sin-csrf");

        mockMvc.perform(post("/prestamo/solicitar")
                        .param("libroId", libro.getId())
                        .with(user(usuario.getMail()).roles("USUARIO")))
                .andExpect(status().isForbidden());

        assertThat(prestamoRepo.findByUsuario_Id(usuario.getId())).isEmpty();
    }

    @Test
    void libroEliminar_sinCsrf_rechazado403YNoElimina() throws Exception {
        Libro libro = crearLibroConStock("eliminar-sin-csrf");

        mockMvc.perform(post("/libro/eliminar/{id}", libro.getId())
                        .with(user("p154-libro-eliminar@test.com").roles("ADMIN")))
                .andExpect(status().isForbidden());

        assertThat(libroRepo.findById(libro.getId())).isPresent();
    }

    // ---- Helper: confirma que UN form específico (identificado por su action)
    // trae method="post" y un input _csrf oculto con value no vacío, sin dar
    // falsos positivos por otros forms presentes en la misma página. ----

    private static void assertFormTieneCsrfOculto(String html, String actionEsperado) {
        String marcadorAction = "action=\"" + actionEsperado + "\"";
        int idxAction = html.indexOf(marcadorAction);
        assertThat(idxAction)
                .as("No se encontro ningun form con action=\"%s\" en el HTML renderizado", actionEsperado)
                .isNotEqualTo(-1);

        int idxFormOpen = html.lastIndexOf("<form", idxAction);
        assertThat(idxFormOpen)
                .as("No se encontro la apertura <form> correspondiente a action=\"%s\"", actionEsperado)
                .isNotEqualTo(-1);

        int idxOpenTagEnd = html.indexOf('>', idxFormOpen);
        String openTag = html.substring(idxFormOpen, idxOpenTagEnd + 1);
        assertThat(openTag.toLowerCase())
                .as("El form con action=\"%s\" no tiene method=\"post\"", actionEsperado)
                .contains("method=\"post\"");

        int idxFormClose = html.indexOf("</form>", idxAction);
        assertThat(idxFormClose)
                .as("No se encontro el cierre </form> correspondiente a action=\"%s\"", actionEsperado)
                .isNotEqualTo(-1);

        String form = html.substring(idxFormOpen, idxFormClose + "</form>".length());

        assertThat(form)
                .as("El form con action=\"%s\" no contiene un input _csrf oculto", actionEsperado)
                .contains("type=\"hidden\"")
                .contains("name=\"_csrf\"");

        // Se aisla primero el <input ...> completo que trae name="_csrf" y
        // recien ahi se busca value dentro de ESE input: asi el resultado no
        // depende del orden en que Spring/Thymeleaf escriban los atributos
        // type/name/value dentro de la etiqueta.
        Matcher inputMatcher = Pattern.compile("<input\\b[^>]*name=\"_csrf\"[^>]*>").matcher(form);
        assertThat(inputMatcher.find())
                .as("No se encontro el <input> con name=\"_csrf\" dentro del form action=\"%s\"", actionEsperado)
                .isTrue();

        Matcher valorMatcher = Pattern.compile("value=\"([^\"]*)\"").matcher(inputMatcher.group());
        assertThat(valorMatcher.find())
                .as("El input _csrf del form action=\"%s\" no tiene atributo value", actionEsperado)
                .isTrue();
        assertThat(valorMatcher.group(1))
                .as("El input _csrf del form action=\"%s\" tiene value vacio", actionEsperado)
                .isNotBlank();
    }

    // ---- Fixtures ----

    private Autor crearAutorActivo(String sufijo) {
        Autor autor = new Autor();
        autor.setNombre("P154 AUTOR " + sufijo.toUpperCase());
        autor.setAlta(true);
        return autorRepo.saveAndFlush(autor);
    }

    private Libro crearLibroConStock(String sufijo) throws MiExcepcion {
        Autor autor = new Autor();
        autor.setNombre("P154 AUTOR " + sufijo.toUpperCase());
        autor.setAlta(true);
        Autor autorPersistido = autorRepo.saveAndFlush(autor);

        Editorial editorial = new Editorial();
        editorial.setNombre("P154 EDITORIAL " + sufijo.toUpperCase());
        editorial.setAlta(true);
        Editorial editorialPersistida = editorialRepo.saveAndFlush(editorial);

        String titulo = "P154 LIBRO " + sufijo.toUpperCase();
        servicioLibro.crearLibro(null, titulo, 2020, autorPersistido.getId(), editorialPersistida.getId(), 1);
        return libroRepo.buscarPorTituloNormalizadoYAutor(titulo, autorPersistido.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Libro de fixture no encontrado: " + titulo));
    }

    private Usuario crearUsuario(String sufijo) {
        Usuario usuario = new Usuario();
        usuario.setNombre("P154");
        usuario.setApellido("Csrf");
        usuario.setClave("clave-no-usada");
        usuario.setMail("p154-" + sufijo + "@test.com");
        usuario.setRol(Rol.USUARIO);
        usuario.setAlta(new Date());
        usuario.setBaja(null);
        return usuarioRepo.saveAndFlush(usuario);
    }

    private void crearUsuarioConClave(String mail, String claveEnTextoPlano) {
        Usuario usuario = new Usuario();
        usuario.setNombre("P154");
        usuario.setApellido("Csrf");
        usuario.setClave(passwordEncoder.encode(claveEnTextoPlano));
        usuario.setMail(mail);
        usuario.setRol(Rol.USUARIO);
        usuario.setAlta(new Date());
        usuario.setBaja(null);
        usuarioRepo.saveAndFlush(usuario);
    }

    private static String repetir(char c, int veces) {
        StringBuilder sb = new StringBuilder(veces);
        for (int i = 0; i < veces; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
