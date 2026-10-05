package com.mza.Libreria.seed;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mza.Libreria.entidades.Autor;
import com.mza.Libreria.entidades.Editorial;
import com.mza.Libreria.entidades.Libro;
import com.mza.Libreria.entidades.Usuario;
import com.mza.Libreria.excepciones.MiExcepcion;
import com.mza.Libreria.repositorios.AutorRepository;
import com.mza.Libreria.repositorios.EditorialRepository;
import com.mza.Libreria.repositorios.LibroRepository;
import com.mza.Libreria.repositorios.PortadaRepository;
import com.mza.Libreria.repositorios.PrestamoRepository;
import com.mza.Libreria.repositorios.UsuarioRepository;
import com.mza.Libreria.servicios.ServicePrestamo;
import com.mza.Libreria.servicios.ServiceUsuario;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unitario puro (Mockito, sin Spring ni MySQL) de DemoDataSeeder: guarda
 * autoritativa del catalogo conectado (antes de cualquier repo/service) e
 * idempotencia de usuarios y prestamo.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DemoDataSeederTest {

    private static final String ADMIN_MAIL = "admin-demo@example.test";
    private static final String USUARIO_MAIL = "usuario-demo@example.test";

    @Mock private AutorRepository autorRepo;
    @Mock private EditorialRepository editorialRepo;
    @Mock private LibroRepository libroRepo;
    @Mock private PortadaRepository portadaRepo;
    @Mock private UsuarioRepository usuarioRepo;
    @Mock private PrestamoRepository prestamoRepo;
    @Mock private ServiceUsuario serviceUsuario;
    @Mock private ServicePrestamo servicePrestamo;
    @Mock private DataSource dataSource;
    @Mock private Connection connection;
    @Mock private Environment environment;

    @InjectMocks private DemoDataSeeder seeder;

    @BeforeEach
    void preparar() throws SQLException {
        ReflectionTestUtils.setField(seeder, "adminMail", ADMIN_MAIL);
        ReflectionTestUtils.setField(seeder, "adminPassword", "clave-admin-demo");
        ReflectionTestUtils.setField(seeder, "usuarioMail", USUARIO_MAIL);
        ReflectionTestUtils.setField(seeder, "usuarioPassword", "clave-usuario-demo");

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("libreriaweb_demo");
        when(environment.getActiveProfiles()).thenReturn(new String[] {"local", "demo"});

        Autor autor = new Autor();
        autor.setId("autor-id");
        when(autorRepo.buscarPorNombre(anyString())).thenReturn(autor);
        Editorial editorial = new Editorial();
        editorial.setId("editorial-id");
        when(editorialRepo.buscarPorNombre(anyString())).thenReturn(editorial);
    }

    private Libro libroExistente() {
        Libro libro = new Libro();
        libro.setId("libro-id");
        return libro;
    }

    private Usuario usuario(String id, String mail) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setMail(mail);
        return u;
    }

    private void catalogoCompletoExistente() {
        when(libroRepo.buscarPorTituloNormalizadoYAutor(anyString(), anyString()))
                .thenReturn(Collections.singletonList(libroExistente()));
    }

    // --- Guarda autoritativa --------------------------------------------

    @Test
    void catalogoInesperado_abortaAntesDeCualquierRepoOService() throws Exception {
        for (String catalogo : new String[] {"libreriaweb", "libreriaweb_testsuite", "otra", null}) {
            when(connection.getCatalog()).thenReturn(catalogo);

            assertThatThrownBy(() -> seeder.run())
                    .isInstanceOf(IllegalStateException.class);
        }

        verifyNoInteractions(autorRepo, editorialRepo, libroRepo, portadaRepo, usuarioRepo,
                prestamoRepo, serviceUsuario, servicePrestamo);
    }

    @Test
    void testsuiteDemoSinPerfilTest_abortaAntesDeCualquierSave() throws Exception {
        when(connection.getCatalog()).thenReturn("libreriaweb_demo_testsuite");

        assertThatThrownBy(() -> seeder.run()).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(autorRepo, editorialRepo, libroRepo, portadaRepo, usuarioRepo,
                prestamoRepo, serviceUsuario, servicePrestamo);
    }

    @Test
    void libreriawebDemoConPerfilTest_abortaAntesDeCualquierSave() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[] {"demo", "test"});

        assertThatThrownBy(() -> seeder.run()).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(autorRepo, editorialRepo, libroRepo, portadaRepo, usuarioRepo,
                prestamoRepo, serviceUsuario, servicePrestamo);
    }

    @Test
    void fallaAlObtenerConexion_abortaSinTocarNada() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("sin conexion"));

        assertThatThrownBy(() -> seeder.run()).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(autorRepo, editorialRepo, libroRepo, portadaRepo, usuarioRepo,
                prestamoRepo, serviceUsuario, servicePrestamo);
    }

    @Test
    void catalogoValido_cierraLaConexionDeLaGuarda() throws Exception {
        catalogoCompletoExistente();
        when(usuarioRepo.buscarPorMail(ADMIN_MAIL)).thenReturn(usuario("a", ADMIN_MAIL));
        when(usuarioRepo.buscarPorMail(USUARIO_MAIL)).thenReturn(usuario("u", USUARIO_MAIL));
        when(prestamoRepo.existsByUsuario_IdAndLibro_Id(anyString(), anyString())).thenReturn(true);

        seeder.run();

        verify(connection).close();
    }

    // --- Idempotencia ----------------------------------------------------

    @Test
    void reejecucion_conTodoExistente_noCreaNiModificaNada() throws Exception {
        catalogoCompletoExistente();
        when(usuarioRepo.buscarPorMail(ADMIN_MAIL)).thenReturn(usuario("a", ADMIN_MAIL));
        when(usuarioRepo.buscarPorMail(USUARIO_MAIL)).thenReturn(usuario("u", USUARIO_MAIL));
        when(prestamoRepo.existsByUsuario_IdAndLibro_Id("u", "libro-id")).thenReturn(true);

        seeder.run();

        verify(libroRepo, never()).save(any());
        verify(autorRepo, never()).save(any());
        verify(editorialRepo, never()).save(any());
        verify(serviceUsuario, never()).registrar(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(serviceUsuario, never()).cambiarRol(anyString());
        verify(servicePrestamo, never()).solicitar(anyString(), anyString());
    }

    @Test
    void prestamoPrevioEnCualquierEstado_noSeSolicitaOtro() throws Exception {
        // existsByUsuario_IdAndLibro_Id no filtra por fechaDevolucion: cubre
        // prestamos activos y devueltos.
        catalogoCompletoExistente();
        when(usuarioRepo.buscarPorMail(ADMIN_MAIL)).thenReturn(usuario("a", ADMIN_MAIL));
        when(usuarioRepo.buscarPorMail(USUARIO_MAIL)).thenReturn(usuario("u", USUARIO_MAIL));
        when(prestamoRepo.existsByUsuario_IdAndLibro_Id("u", "libro-id")).thenReturn(true);

        seeder.run();

        verify(prestamoRepo).existsByUsuario_IdAndLibro_Id("u", "libro-id");
        verify(prestamoRepo, never()).existsByUsuario_IdAndLibro_IdAndFechaDevolucionIsNull(anyString(), anyString());
        verify(servicePrestamo, never()).solicitar(anyString(), anyString());
    }

    // --- Primera ejecucion ----------------------------------------------

    @Test
    void primeraEjecucion_registraUsuariosPorServicio_cambiaRolSoloDelAdmin_ySolicitaPrestamo() throws Exception {
        catalogoCompletoExistente();
        when(usuarioRepo.buscarPorMail(ADMIN_MAIL)).thenReturn(null, usuario("a", ADMIN_MAIL));
        when(usuarioRepo.buscarPorMail(USUARIO_MAIL)).thenReturn(null, usuario("u", USUARIO_MAIL));
        when(prestamoRepo.existsByUsuario_IdAndLibro_Id("u", "libro-id")).thenReturn(false);

        seeder.run();

        verify(serviceUsuario).registrar(eq("Admin"), eq("Demo"), eq(ADMIN_MAIL),
                eq("clave-admin-demo"), eq("clave-admin-demo"));
        verify(serviceUsuario).registrar(eq("Usuario"), eq("Demo"), eq(USUARIO_MAIL),
                eq("clave-usuario-demo"), eq("clave-usuario-demo"));
        verify(serviceUsuario).cambiarRol("a");
        verify(serviceUsuario, never()).cambiarRol("u");
        verify(servicePrestamo).solicitar(USUARIO_MAIL, "libro-id");
    }

    @Test
    void libroNuevo_seCreaConStockRealSinPrestamosFicticios() throws Exception {
        when(libroRepo.buscarPorTituloNormalizadoYAutor(anyString(), anyString()))
                .thenReturn(Collections.<Libro>emptyList());
        when(libroRepo.save(any(Libro.class))).thenAnswer(inv -> {
            Libro l = inv.getArgument(0);
            l.setId("libro-id");
            return l;
        });
        when(portadaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepo.buscarPorMail(anyString())).thenReturn(usuario("x", "x@example.test"));
        when(prestamoRepo.existsByUsuario_IdAndLibro_Id(anyString(), anyString())).thenReturn(true);

        seeder.run();

        org.mockito.ArgumentCaptor<Libro> captor = org.mockito.ArgumentCaptor.forClass(Libro.class);
        verify(libroRepo, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        for (Libro l : captor.getAllValues()) {
            org.junit.jupiter.api.Assertions.assertEquals(0, l.getEjemplaresPrestados().intValue());
            org.junit.jupiter.api.Assertions.assertEquals(l.getNroejemplares(), l.getEjemplaresRestantes());
        }
    }

    // --- Fallos de servicios -> excepcion no chequeada (rollback) --------

    @Test
    void falloAlRegistrarUsuario_seConvierteEnExcepcionNoChequeada() throws Exception {
        catalogoCompletoExistente();
        when(usuarioRepo.buscarPorMail(anyString())).thenReturn(null);
        doThrow(new MiExcepcion("falla")).when(serviceUsuario)
                .registrar(anyString(), anyString(), anyString(), anyString(), anyString());

        assertThatThrownBy(() -> seeder.run()).isInstanceOf(IllegalStateException.class);

        verify(servicePrestamo, never()).solicitar(anyString(), anyString());
    }

    @Test
    void falloAlSolicitarPrestamo_seConvierteEnExcepcionNoChequeada() throws Exception {
        catalogoCompletoExistente();
        when(usuarioRepo.buscarPorMail(ADMIN_MAIL)).thenReturn(usuario("a", ADMIN_MAIL));
        when(usuarioRepo.buscarPorMail(USUARIO_MAIL)).thenReturn(usuario("u", USUARIO_MAIL));
        when(prestamoRepo.existsByUsuario_IdAndLibro_Id(anyString(), anyString())).thenReturn(false);
        when(servicePrestamo.solicitar(anyString(), anyString())).thenThrow(new MiExcepcion("sin stock"));

        assertThatThrownBy(() -> seeder.run()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void credencialesVacias_abortanSinRegistrar() throws Exception {
        catalogoCompletoExistente();
        ReflectionTestUtils.setField(seeder, "adminPassword", " ");

        assertThatThrownBy(() -> seeder.run()).isInstanceOf(IllegalStateException.class);

        verify(serviceUsuario, never()).registrar(anyString(), anyString(), anyString(), anyString(), anyString());
    }
}
