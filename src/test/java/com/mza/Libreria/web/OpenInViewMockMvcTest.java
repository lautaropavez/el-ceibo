package com.mza.Libreria.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.mza.Libreria.entidades.Autor;
import com.mza.Libreria.entidades.Editorial;
import com.mza.Libreria.entidades.Libro;
import com.mza.Libreria.entidades.Portada;
import com.mza.Libreria.entidades.Usuario;
import com.mza.Libreria.enumeradores.Rol;
import com.mza.Libreria.repositorios.AutorRepository;
import com.mza.Libreria.repositorios.EditorialRepository;
import com.mza.Libreria.repositorios.LibroRepository;
import com.mza.Libreria.repositorios.PortadaRepository;
import com.mza.Libreria.repositorios.PrestamoRepository;
import com.mza.Libreria.repositorios.UsuarioRepository;
import com.mza.Libreria.servicios.ServicePrestamo;
import com.mza.Libreria.support.LibreriaIntegrationTest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * P16.2.C2 - spring.jpa.open-in-view=false.
 *
 * Confirma que la propiedad esta efectivamente desactivada (no solo
 * declarada) y que las seis pantallas que navegan asociaciones fuera de
 * los services siguen renderizando correctamente sin el
 * OpenEntityManagerInViewInterceptor. Todas las asociaciones que estas
 * pantallas recorren (Libro.autor, Libro.editorial, Libro.portada,
 * Prestamo.usuario, Prestamo.libro) son @ManyToOne/@OneToOne EAGER, asi
 * que ya llegan cargadas al cerrar la transaccion del service; este test
 * lo verifica contra el contexto real, no lo asume.
 */
@LibreriaIntegrationTest
class OpenInViewMockMvcTest {

    private static final String PREFIJO = "P162C";
    private static final String MAIL_USUARIO_MIS_PRESTAMOS = "p162c-mis-prestamos@test.com";

    private static final byte[] FIRMA_PNG =
            {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private Environment environment;

    @Autowired
    private ServicePrestamo servicePrestamo;

    @Autowired
    private AutorRepository autorRepo;

    @Autowired
    private EditorialRepository editorialRepo;

    @Autowired
    private LibroRepository libroRepo;

    @Autowired
    private PortadaRepository portadaRepo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private PrestamoRepository prestamoRepo;

    private MockMvc mockMvc;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    private final List<String> autorIds = new ArrayList<>();
    private final List<String> editorialIds = new ArrayList<>();
    private final List<String> libroIds = new ArrayList<>();
    private final List<String> portadaIds = new ArrayList<>();
    private final List<String> usuarioIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    // ------------------------------------------------------------------
    // Requisito "a": propiedad efectiva
    // ------------------------------------------------------------------

    @Test
    void openInView_propiedadEfectiva_estaDesactivada() {
        assertThat(environment.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
        // Spring Boot solo registra este interceptor cuando open-in-view
        // resuelve a true (JpaBaseConfiguration.JpaWebConfiguration). Su
        // ausencia real en el contexto confirma el efecto, no solo el texto
        // de la propiedad.
        assertThat(context.getBeanNamesForType(OpenEntityManagerInViewInterceptor.class)).isEmpty();
    }

    // ------------------------------------------------------------------
    // Requisito "b": las seis pantallas
    // ------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void libroLista_comoAdmin_muestraLibroConAutorEditorialYPortada() throws Exception {
        Autor autor = crearAutor("lista");
        Editorial editorial = crearEditorial("lista");
        Portada portada = crearPortadaPng("lista");
        Libro libro = crearLibroConPortada("lista", autor, editorial, portada);

        mockMvc.perform(get("/libro/lista"))
                .andExpect(status().isOk())
                .andExpect(view().name("list-libro"))
                .andExpect(model().attributeExists("libros"))
                .andExpect(content().string(containsString(libro.getTitulo())))
                .andExpect(content().string(containsString(autor.getNombre())))
                .andExpect(content().string(containsString(editorial.getNombre())));
    }

    @Test
    void libroBiblioteca_publico_muestraLibroActivo() throws Exception {
        Autor autor = crearAutor("bib");
        Editorial editorial = crearEditorial("bib");
        Libro libro = crearLibroConPortada("bib", autor, editorial, null);

        mockMvc.perform(get("/libro/biblioteca"))
                .andExpect(status().isOk())
                .andExpect(view().name("biblioteca"))
                .andExpect(content().string(containsString(libro.getTitulo())));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void libroModificar_comoAdmin_prellenaFormularioConDatosDelLibro() throws Exception {
        Autor autor = crearAutor("mod");
        Editorial editorial = crearEditorial("mod");
        Libro libro = crearLibroConPortada("mod", autor, editorial, null);

        mockMvc.perform(get("/libro/modificar/{id}", libro.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("modif-Libro"))
                .andExpect(model().attribute("titulo", libro.getTitulo()))
                .andExpect(model().attribute("nombreAut", autor.getNombre()))
                .andExpect(model().attribute("nombreEdit", editorial.getNombre()))
                .andExpect(content().string(containsString(libro.getTitulo())));
    }

    @Test
    void portadaLibro_descargaImagenConContentTypeCorrecto() throws Exception {
        Autor autor = crearAutor("portada");
        Editorial editorial = crearEditorial("portada");
        Portada portada = crearPortadaPng("portada");
        Libro libro = crearLibroConPortada("portada", autor, editorial, portada);

        mockMvc.perform(get("/portada/libro/{id}", libro.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void prestamoLista_comoAdmin_listaPrestamoActivoConLibroYUsuario() throws Exception {
        Autor autor = crearAutor("plista");
        Editorial editorial = crearEditorial("plista");
        Libro libro = crearLibroConPortada("plista", autor, editorial, null);
        Usuario usuario = crearUsuario("plista", Rol.USUARIO);
        servicePrestamo.solicitar(usuario.getMail(), libro.getId());

        mockMvc.perform(get("/prestamo/lista"))
                .andExpect(status().isOk())
                .andExpect(view().name("list-prestamo"))
                .andExpect(model().attributeExists("prestamos"))
                .andExpect(content().string(containsString(libro.getTitulo())))
                .andExpect(content().string(containsString(usuario.getMail())));
    }

    @Test
    @WithMockUser(username = MAIL_USUARIO_MIS_PRESTAMOS, roles = "USUARIO")
    void prestamoMisPrestamos_comoUsuarioAutenticado_listaSusPropiosPrestamos() throws Exception {
        Autor autor = crearAutor("mp");
        Editorial editorial = crearEditorial("mp");
        Libro libro = crearLibroConPortada("mp", autor, editorial, null);
        Usuario usuario = crearUsuarioConMail("mp", Rol.USUARIO, MAIL_USUARIO_MIS_PRESTAMOS);
        servicePrestamo.solicitar(usuario.getMail(), libro.getId());

        mockMvc.perform(get("/prestamo/mis-prestamos"))
                .andExpect(status().isOk())
                .andExpect(view().name("mis-prestamos"))
                .andExpect(model().attributeExists("prestamos"))
                .andExpect(content().string(containsString(libro.getTitulo())));
    }

    // ------------------------------------------------------------------
    // Fixtures (cada saveAndFlush confirma antes de la llamada MockMvc)
    // ------------------------------------------------------------------

    private Autor crearAutor(String tag) {
        Autor autor = new Autor();
        autor.setNombre(PREFIJO + " AUTOR " + tag + " " + sufijo);
        autor.setAlta(true);
        Autor guardado = autorRepo.saveAndFlush(autor);
        autorIds.add(guardado.getId());
        return guardado;
    }

    private Editorial crearEditorial(String tag) {
        Editorial editorial = new Editorial();
        editorial.setNombre(PREFIJO + " EDITORIAL " + tag + " " + sufijo);
        editorial.setAlta(true);
        Editorial guardada = editorialRepo.saveAndFlush(editorial);
        editorialIds.add(guardada.getId());
        return guardada;
    }

    private Portada crearPortadaPng(String tag) {
        Portada portada = new Portada();
        portada.setNombre(PREFIJO + " " + tag + " " + sufijo + ".png");
        portada.setMime("image/png");
        portada.setContenido(FIRMA_PNG);
        Portada guardada = portadaRepo.saveAndFlush(portada);
        portadaIds.add(guardada.getId());
        return guardada;
    }

    private Libro crearLibroConPortada(String tag, Autor autor, Editorial editorial, Portada portada) {
        Libro libro = new Libro();
        libro.setTitulo(PREFIJO + " " + tag + " " + sufijo);
        libro.setAnio(2000);
        libro.setAlta(Boolean.TRUE);
        libro.setNroejemplares(1);
        libro.setEjemplaresRestantes(1);
        libro.setEjemplaresPrestados(0);
        libro.setAutor(autor);
        libro.setEditorial(editorial);
        libro.setPortada(portada);
        Libro guardado = libroRepo.saveAndFlush(libro);
        libroIds.add(guardado.getId());
        return guardado;
    }

    private Usuario crearUsuario(String tag, Rol rol) {
        String mail = (PREFIJO + "-" + tag + "-" + sufijo).toLowerCase() + "@test.com";
        return crearUsuarioConMail(tag, rol, mail);
    }

    private Usuario crearUsuarioConMail(String tag, Rol rol, String mail) {
        Usuario usuario = new Usuario();
        usuario.setNombre(PREFIJO);
        usuario.setApellido(tag);
        usuario.setClave("clave-no-usada");
        usuario.setMail(mail);
        usuario.setRol(rol);
        usuario.setAlta(new Date());
        usuario.setBaja(null);
        usuario.setCantidadPrestamos(null);
        Usuario guardado = usuarioRepo.saveAndFlush(usuario);
        usuarioIds.add(guardado.getId());
        return guardado;
    }

    /**
     * Limpieza limitada exclusivamente a los fixtures P162C creados por el
     * propio test, en orden dependiente: prestamos, libros, usuarios,
     * autores/editoriales, portadas (despues del libro que las referencia).
     */
    @AfterEach
    void limpiarFixtures() {
        for (String usuarioId : usuarioIds) {
            prestamoRepo.deleteAll(prestamoRepo.findByUsuario_Id(usuarioId));
        }
        for (String libroId : libroIds) {
            if (libroRepo.existsById(libroId)) {
                libroRepo.deleteById(libroId);
            }
        }
        for (String usuarioId : usuarioIds) {
            if (usuarioRepo.existsById(usuarioId)) {
                usuarioRepo.deleteById(usuarioId);
            }
        }
        for (String autorId : autorIds) {
            if (autorRepo.existsById(autorId)) {
                autorRepo.deleteById(autorId);
            }
        }
        for (String editorialId : editorialIds) {
            if (editorialRepo.existsById(editorialId)) {
                editorialRepo.deleteById(editorialId);
            }
        }
        for (String portadaId : portadaIds) {
            if (portadaRepo.existsById(portadaId)) {
                portadaRepo.deleteById(portadaId);
            }
        }
    }
}
