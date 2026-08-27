package com.mza.Libreria.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
import com.mza.Libreria.repositorios.UsuarioRepository;
import com.mza.Libreria.servicios.ServiceLibro;
import com.mza.Libreria.support.LibreriaIntegrationTest;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@LibreriaIntegrationTest
class CatalogMutationsMockMvcTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ServiceLibro servicioLibro;

    @Autowired
    private AutorRepository autorRepo;

    @Autowired
    private EditorialRepository editorialRepo;

    @Autowired
    private LibroRepository libroRepo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void autorAlta_yaNoMutaPorGet_yMutaPorPost() throws Exception {
        Autor autor = crearAutor("alta", false);

        mockMvc.perform(get("/autor/alta/{id}", autor.getId()))
                .andExpect(status().isMethodNotAllowed());
        assertThat(recargarAutor(autor.getId()).isAlta()).isFalse();

        mockMvc.perform(post("/autor/alta/{id}", autor.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/autor/lista"));
        assertThat(recargarAutor(autor.getId()).isAlta()).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void autorBaja_yaNoMutaPorGet_yMutaPorPost() throws Exception {
        Autor autor = crearAutor("baja", true);

        mockMvc.perform(get("/autor/baja/{id}", autor.getId()))
                .andExpect(status().isMethodNotAllowed());
        assertThat(recargarAutor(autor.getId()).isAlta()).isTrue();

        mockMvc.perform(post("/autor/baja/{id}", autor.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/autor/lista"));
        assertThat(recargarAutor(autor.getId()).isAlta()).isFalse();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void autorEliminar_yaNoMutaPorGet_yMutaPorPost() throws Exception {
        Autor autor = crearAutor("eliminar", true);

        mockMvc.perform(get("/autor/eliminar/{id}", autor.getId()))
                .andExpect(status().isMethodNotAllowed());
        assertThat(autorRepo.findById(autor.getId())).isPresent();

        mockMvc.perform(post("/autor/eliminar/{id}", autor.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/autor/lista"));
        assertThat(autorRepo.findById(autor.getId())).isEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editorialAlta_yaNoMutaPorGet_yMutaPorPost() throws Exception {
        Editorial editorial = crearEditorial("alta", false);

        mockMvc.perform(get("/editorial/alta/{id}", editorial.getId()))
                .andExpect(status().isMethodNotAllowed());
        assertThat(recargarEditorial(editorial.getId()).isAlta()).isFalse();

        mockMvc.perform(post("/editorial/alta/{id}", editorial.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/editorial/lista"));
        assertThat(recargarEditorial(editorial.getId()).isAlta()).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editorialBaja_yaNoMutaPorGet_yMutaPorPost() throws Exception {
        Editorial editorial = crearEditorial("baja", true);

        mockMvc.perform(get("/editorial/baja/{id}", editorial.getId()))
                .andExpect(status().isMethodNotAllowed());
        assertThat(recargarEditorial(editorial.getId()).isAlta()).isTrue();

        mockMvc.perform(post("/editorial/baja/{id}", editorial.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/editorial/lista"));
        assertThat(recargarEditorial(editorial.getId()).isAlta()).isFalse();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editorialEliminar_yaNoMutaPorGet_yMutaPorPost() throws Exception {
        Editorial editorial = crearEditorial("eliminar", true);

        mockMvc.perform(get("/editorial/eliminar/{id}", editorial.getId()))
                .andExpect(status().isMethodNotAllowed());
        assertThat(editorialRepo.findById(editorial.getId())).isPresent();

        mockMvc.perform(post("/editorial/eliminar/{id}", editorial.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/editorial/lista"));
        assertThat(editorialRepo.findById(editorial.getId())).isEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void libroAlta_yaNoMutaPorGet_yMutaPorPost() throws Exception {
        Autor autor = crearAutor("libro-alta", true);
        Editorial editorial = crearEditorial("libro-alta", true);
        Libro libro = crearLibroActivo("libro-alta", autor, editorial);
        servicioLibro.baja(libro.getId());
        assertThat(recargarLibro(libro.getId()).getAlta()).isFalse();

        mockMvc.perform(get("/libro/alta/{id}", libro.getId()))
                .andExpect(status().isMethodNotAllowed());
        assertThat(recargarLibro(libro.getId()).getAlta()).isFalse();

        mockMvc.perform(post("/libro/alta/{id}", libro.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/libro/lista"));
        assertThat(recargarLibro(libro.getId()).getAlta()).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void libroBaja_yaNoMutaPorGet_yMutaPorPost() throws Exception {
        Autor autor = crearAutor("libro-baja", true);
        Editorial editorial = crearEditorial("libro-baja", true);
        Libro libro = crearLibroActivo("libro-baja", autor, editorial);
        assertThat(recargarLibro(libro.getId()).getAlta()).isTrue();

        mockMvc.perform(get("/libro/baja/{id}", libro.getId()))
                .andExpect(status().isMethodNotAllowed());
        assertThat(recargarLibro(libro.getId()).getAlta()).isTrue();

        mockMvc.perform(post("/libro/baja/{id}", libro.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/libro/lista"));
        assertThat(recargarLibro(libro.getId()).getAlta()).isFalse();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void libroEliminar_yaNoMutaPorGet_yMutaPorPost() throws Exception {
        Autor autor = crearAutor("libro-eliminar", true);
        Editorial editorial = crearEditorial("libro-eliminar", true);
        Libro libro = crearLibroActivo("libro-eliminar", autor, editorial);

        mockMvc.perform(get("/libro/eliminar/{id}", libro.getId()))
                .andExpect(status().isMethodNotAllowed());
        assertThat(libroRepo.findById(libro.getId())).isPresent();

        mockMvc.perform(post("/libro/eliminar/{id}", libro.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/libro/lista"));
        assertThat(libroRepo.findById(libro.getId())).isEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void usuarioAlta_yaNoMutaPorGet_yMutaPorPost() throws Exception {
        Usuario usuario = crearUsuario("alta", new Date());

        mockMvc.perform(get("/usuario/alta/{id}", usuario.getId()))
                .andExpect(status().isMethodNotAllowed());
        assertThat(recargarUsuario(usuario.getId()).getBaja()).isNotNull();

        mockMvc.perform(post("/usuario/alta/{id}", usuario.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuario/lista"));
        assertThat(recargarUsuario(usuario.getId()).getBaja()).isNull();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void usuarioBaja_yaNoMutaPorGet_yMutaPorPost() throws Exception {
        Usuario usuario = crearUsuario("baja", null);

        mockMvc.perform(get("/usuario/baja/{id}", usuario.getId()))
                .andExpect(status().isMethodNotAllowed());
        assertThat(recargarUsuario(usuario.getId()).getBaja()).isNull();

        mockMvc.perform(post("/usuario/baja/{id}", usuario.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuario/lista"));
        assertThat(recargarUsuario(usuario.getId()).getBaja()).isNotNull();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void usuarioEliminar_yaNoMutaPorGet_yMutaPorPost() throws Exception {
        Usuario usuario = crearUsuario("eliminar", null);

        mockMvc.perform(get("/usuario/eliminar/{id}", usuario.getId()))
                .andExpect(status().isMethodNotAllowed());
        assertThat(usuarioRepo.findById(usuario.getId())).isPresent();

        mockMvc.perform(post("/usuario/eliminar/{id}", usuario.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuario/lista"));
        assertThat(usuarioRepo.findById(usuario.getId())).isEmpty();
    }

    private Autor crearAutor(String sufijo, boolean altaInicial) {
        Autor autor = new Autor();
        autor.setNombre("P152 AUTOR " + sufijo.toUpperCase());
        autor.setAlta(altaInicial);
        return autorRepo.saveAndFlush(autor);
    }

    private Editorial crearEditorial(String sufijo, boolean altaInicial) {
        Editorial editorial = new Editorial();
        editorial.setNombre("P152 EDITORIAL " + sufijo.toUpperCase());
        editorial.setAlta(altaInicial);
        return editorialRepo.saveAndFlush(editorial);
    }

    private Libro crearLibroActivo(String sufijo, Autor autor, Editorial editorial) throws MiExcepcion {
        String titulo = "P152 LIBRO " + sufijo.toUpperCase();
        servicioLibro.crearLibro(null, titulo, 2020, autor.getId(), editorial.getId(), 1);
        return libroRepo.buscarPorTituloNormalizadoYAutor(titulo, autor.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Libro de fixture no encontrado: " + titulo));
    }

    private Usuario crearUsuario(String sufijo, Date bajaInicial) {
        Usuario usuario = new Usuario();
        usuario.setNombre("P152");
        usuario.setApellido("Catalogo");
        usuario.setClave("clave-no-usada");
        usuario.setMail("p152-" + sufijo + "@test.com");
        usuario.setRol(Rol.USUARIO);
        usuario.setAlta(new Date());
        usuario.setBaja(bajaInicial);
        return usuarioRepo.saveAndFlush(usuario);
    }

    private Autor recargarAutor(String id) {
        return autorRepo.findById(id)
                .orElseThrow(() -> new IllegalStateException("Autor de fixture no encontrado: " + id));
    }

    private Editorial recargarEditorial(String id) {
        return editorialRepo.findById(id)
                .orElseThrow(() -> new IllegalStateException("Editorial de fixture no encontrada: " + id));
    }

    private Libro recargarLibro(String id) {
        return libroRepo.findById(id)
                .orElseThrow(() -> new IllegalStateException("Libro de fixture no encontrado: " + id));
    }

    private Usuario recargarUsuario(String id) {
        return usuarioRepo.findById(id)
                .orElseThrow(() -> new IllegalStateException("Usuario de fixture no encontrado: " + id));
    }
}
