package com.mza.Libreria.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.mza.Libreria.entidades.Autor;
import com.mza.Libreria.entidades.Editorial;
import com.mza.Libreria.entidades.Libro;
import com.mza.Libreria.entidades.Prestamo;
import com.mza.Libreria.entidades.Usuario;
import com.mza.Libreria.enumeradores.Rol;
import com.mza.Libreria.excepciones.MiExcepcion;
import com.mza.Libreria.repositorios.AutorRepository;
import com.mza.Libreria.repositorios.EditorialRepository;
import com.mza.Libreria.repositorios.LibroRepository;
import com.mza.Libreria.repositorios.PrestamoRepository;
import com.mza.Libreria.repositorios.UsuarioRepository;
import com.mza.Libreria.servicios.ServiceLibro;
import com.mza.Libreria.servicios.ServicePrestamo;
import com.mza.Libreria.support.LibreriaIntegrationTest;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@LibreriaIntegrationTest
class WebSecurityMockMvcTest {

    // Mismo formato que los IDs reales (uuid2, varchar(36)), pero fijo y
    // deliberadamente inexistente: la request debe llegar al manejo de
    // negocio, no fallar antes por formato o binding.
    private static final String PRESTAMO_ID_INEXISTENTE = "00000000-0000-4000-8000-000000000144";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ServicePrestamo servicePrestamo;

    @Autowired
    private ServiceLibro servicioLibro;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private LibroRepository libroRepo;

    @Autowired
    private AutorRepository autorRepo;

    @Autowired
    private EditorialRepository editorialRepo;

    @Autowired
    private PrestamoRepository prestamoRepo;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @ParameterizedTest
    @CsvSource({
        "ANONIMO, GET,  /libro/biblioteca,       OK",
        "ANONIMO, GET,  /libro/lista,            LOGIN",
        "ANONIMO, GET,  /prestamo/mis-prestamos, LOGIN",
        "USUARIO, GET,  /libro/lista,            FORBIDDEN",
        "USUARIO, GET,  /autor/lista,            FORBIDDEN",
        "USUARIO, GET,  /editorial/lista,        FORBIDDEN",
        "USUARIO, GET,  /usuario/lista,          FORBIDDEN",
        "ADMIN,   GET,  /prestamo/mis-prestamos, FORBIDDEN",
        "ADMIN,   POST, /prestamo/solicitar,     FORBIDDEN",
        "USUARIO, GET,  /prestamo/lista,         FORBIDDEN",
        "USUARIO, POST, /prestamo/devolver,      FORBIDDEN",
        "ADMIN,   GET,  /libro/lista,            OK",
        "ADMIN,   GET,  /prestamo/lista,         OK",
        "USUARIO, GET,  /autor/registroAutor,        FORBIDDEN",
        "USUARIO, GET,  /editorial/registroEditorial, FORBIDDEN",
        "USUARIO, GET,  /libro/registro,              FORBIDDEN",
        "USUARIO, GET,  /libro/modificar/1,           FORBIDDEN",
        "USUARIO, GET,  /usuario/modificar/1,         FORBIDDEN"
    })
    void autorizacionPorRutaYRol_respetaLasReglasDeSecurityConfig(
            String actor, String metodo, String ruta, String esperado) throws Exception {

        MockHttpServletRequestBuilder request = "POST".equals(metodo) ? post(ruta).with(csrf()) : get(ruta);

        if (!"ANONIMO".equals(actor)) {
            request = request.with(
                    user("p144-matriz-" + actor.toLowerCase() + "@test.com").roles(actor));
        }

        ResultActions resultado = mockMvc.perform(request);

        if ("OK".equals(esperado)) {
            resultado.andExpect(status().isOk());
        } else if ("FORBIDDEN".equals(esperado)) {
            resultado.andExpect(status().isForbidden());
        } else if ("LOGIN".equals(esperado)) {
            resultado.andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        } else {
            throw new IllegalArgumentException("Expectativa desconocida: " + esperado);
        }
    }

    @Test
    @WithMockUser(username = "p144-solicitar-ok@test.com", roles = "USUARIO")
    void solicitar_comoUsuarioConStock_redirigeAMisPrestamosConFlashExito() throws Exception {
        Usuario usuario = crearUsuario("solicitar-ok", Rol.USUARIO);
        Libro libro = crearLibroConStock("solicitar-ok");

        mockMvc.perform(post("/prestamo/solicitar").param("libroId", libro.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/prestamo/mis-prestamos"))
                .andExpect(flash().attributeExists("exito"));

        List<Prestamo> prestamosDelUsuario = prestamoRepo.findByUsuario_Id(usuario.getId());
        assertThat(prestamosDelUsuario).hasSize(1);
        assertThat(prestamosDelUsuario.get(0).getFechaDevolucion()).isNull();

        Libro libroRecargado = recargarLibro(libro.getId());
        assertThat(libroRecargado.getEjemplaresRestantes()).isEqualTo(0);
        assertThat(libroRecargado.getEjemplaresPrestados()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "p144-sin-stock-b@test.com", roles = "USUARIO")
    void solicitar_sinStock_redirigeABibliotecaConFlashErrorSin500() throws Exception {
        Usuario usuarioA = crearUsuario("sin-stock-a", Rol.USUARIO);
        Usuario usuarioB = crearUsuario("sin-stock-b", Rol.USUARIO);
        Libro libro = crearLibroConStock("sin-stock");

        // El stock llega a cero por un prestamo real previo de otro usuario,
        // no por manipular contadores a mano.
        servicePrestamo.solicitar(usuarioA.getMail(), libro.getId());

        Libro libroAgotado = recargarLibro(libro.getId());
        assertThat(libroAgotado.getEjemplaresRestantes()).isEqualTo(0);
        assertThat(libroAgotado.getEjemplaresPrestados()).isEqualTo(1);

        mockMvc.perform(post("/prestamo/solicitar").param("libroId", libro.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/libro/biblioteca"))
                .andExpect(flash().attributeExists("error"));

        assertThat(prestamoRepo.findByUsuario_Id(usuarioB.getId())).isEmpty();

        Libro libroTrasRechazo = recargarLibro(libro.getId());
        assertThat(libroTrasRechazo.getEjemplaresRestantes()).isEqualTo(0);
        assertThat(libroTrasRechazo.getEjemplaresPrestados()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "p144-idor-a@test.com", roles = "USUARIO")
    void solicitar_conUsuarioIdManipulado_ignoraElParametroYAsignaAlAutenticado() throws Exception {
        Usuario usuarioA = crearUsuario("idor-a", Rol.USUARIO);
        Usuario usuarioB = crearUsuario("idor-b", Rol.USUARIO);
        Libro libro = crearLibroConStock("idor");

        mockMvc.perform(post("/prestamo/solicitar")
                        .param("libroId", libro.getId())
                        .param("usuarioId", usuarioB.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/prestamo/mis-prestamos"))
                .andExpect(flash().attributeExists("exito"));

        assertThat(prestamoRepo.findByUsuario_Id(usuarioA.getId())).hasSize(1);
        assertThat(prestamoRepo.findByUsuario_Id(usuarioB.getId())).isEmpty();
    }

    @Test
    @WithMockUser(username = "p144-mis-prestamos-a@test.com", roles = "USUARIO")
    void misPrestamos_comoUsuario_exponeSoloSusPrestamos() throws Exception {
        Usuario usuarioA = crearUsuario("mis-prestamos-a", Rol.USUARIO);
        Usuario usuarioB = crearUsuario("mis-prestamos-b", Rol.USUARIO);
        Libro libroA = crearLibroConStock("mis-prestamos-a");
        Libro libroB = crearLibroConStock("mis-prestamos-b");

        Prestamo prestamoA = servicePrestamo.solicitar(usuarioA.getMail(), libroA.getId());
        Prestamo prestamoB = servicePrestamo.solicitar(usuarioB.getMail(), libroB.getId());

        MvcResult resultado = mockMvc.perform(get("/prestamo/mis-prestamos"))
                .andExpect(status().isOk())
                .andExpect(view().name("mis-prestamos"))
                .andExpect(model().attributeExists("prestamos"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<Prestamo> prestamosEnModelo =
                (List<Prestamo>) resultado.getModelAndView().getModel().get("prestamos");

        assertThat(prestamosEnModelo)
                .extracting(Prestamo::getId)
                .containsExactly(prestamoA.getId())
                .doesNotContain(prestamoB.getId());
    }

    @Test
    @WithMockUser(username = "p144-devolver-admin@test.com", roles = "ADMIN")
    void devolver_comoAdmin_redirigeAListaConFlashExito() throws Exception {
        Usuario usuario = crearUsuario("devolver-ok", Rol.USUARIO);
        Libro libro = crearLibroConStock("devolver-ok");

        Prestamo prestamo = servicePrestamo.solicitar(usuario.getMail(), libro.getId());

        mockMvc.perform(post("/prestamo/devolver").param("prestamoId", prestamo.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/prestamo/lista"))
                .andExpect(flash().attributeExists("exito"));

        Prestamo prestamoRecargado = recargarPrestamo(prestamo.getId());
        assertThat(prestamoRecargado.getFechaDevolucion()).isNotNull();

        Libro libroRecargado = recargarLibro(libro.getId());
        assertThat(libroRecargado.getEjemplaresRestantes()).isEqualTo(1);
        assertThat(libroRecargado.getEjemplaresPrestados()).isEqualTo(0);
    }

    @Test
    @WithMockUser(username = "p144-devolver-inexistente@test.com", roles = "ADMIN")
    void devolver_conPrestamoInexistente_redirigeConFlashErrorSin500() throws Exception {
        assertThat(prestamoRepo.findById(PRESTAMO_ID_INEXISTENTE)).isEmpty();

        mockMvc.perform(post("/prestamo/devolver").param("prestamoId", PRESTAMO_ID_INEXISTENTE).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/prestamo/lista"))
                .andExpect(flash().attributeExists("error"));

        assertThat(prestamoRepo.findById(PRESTAMO_ID_INEXISTENTE)).isEmpty();
    }

    private Usuario crearUsuario(String sufijo, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setNombre("P144");
        usuario.setApellido("Web");
        usuario.setClave("clave-no-usada");
        usuario.setMail("p144-" + sufijo + "@test.com");
        usuario.setRol(rol);
        usuario.setAlta(new Date());
        usuario.setBaja(null);
        return usuarioRepo.saveAndFlush(usuario);
    }

    private Libro crearLibroConStock(String sufijo) throws MiExcepcion {
        Autor autor = new Autor();
        autor.setNombre("P144 AUTOR " + sufijo.toUpperCase());
        autor.setAlta(true);
        Autor autorPersistido = autorRepo.saveAndFlush(autor);

        Editorial editorial = new Editorial();
        editorial.setNombre("P144 EDITORIAL " + sufijo.toUpperCase());
        editorial.setAlta(true);
        Editorial editorialPersistida = editorialRepo.saveAndFlush(editorial);

        String titulo = "P144 LIBRO " + sufijo.toUpperCase();
        servicioLibro.crearLibro(null, titulo, 2020,
                autorPersistido.getId(), editorialPersistida.getId(), 1);

        return libroRepo.buscarPorTituloNormalizadoYAutor(titulo, autorPersistido.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Libro de fixture no encontrado: " + titulo));
    }

    private Libro recargarLibro(String id) {
        return libroRepo.findById(id)
                .orElseThrow(() -> new IllegalStateException("Libro de fixture no encontrado: " + id));
    }

    private Prestamo recargarPrestamo(String id) {
        return prestamoRepo.findById(id)
                .orElseThrow(() -> new IllegalStateException("Prestamo de fixture no encontrado: " + id));
    }
}
