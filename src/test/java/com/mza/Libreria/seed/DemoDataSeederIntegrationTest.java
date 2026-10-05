package com.mza.Libreria.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mza.Libreria.entidades.Libro;
import com.mza.Libreria.entidades.Portada;
import com.mza.Libreria.entidades.Usuario;
import com.mza.Libreria.enumeradores.Rol;
import com.mza.Libreria.excepciones.MiExcepcion;
import com.mza.Libreria.repositorios.AutorRepository;
import com.mza.Libreria.repositorios.EditorialRepository;
import com.mza.Libreria.repositorios.LibroRepository;
import com.mza.Libreria.repositorios.PortadaRepository;
import com.mza.Libreria.repositorios.PrestamoRepository;
import com.mza.Libreria.repositorios.UsuarioRepository;
import com.mza.Libreria.seed.support.DemoTestDatabaseInitializer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integracion end-to-end contra libreriaweb_demo_testsuite (base exclusiva
 * recreada por DemoTestDatabaseInitializer). La primera corrida del seeder
 * ocurre al arrancar el contexto (CommandLineRunner); el primer test verifica
 * su resultado y luego ejecuta una segunda corrida para comprobar
 * idempotencia. El segundo test provoca un fallo controlado despues de
 * escrituras del seeder y comprueba el rollback. El orden es explicito
 * porque el primero asume el estado inicial del seeder.
 * Las credenciales son valores ficticios inline: no se usan variables DEMO_*.
 */
@SpringBootTest(properties = {
        "app.demo.admin.mail=admin-demo-it@example.test",
        "app.demo.admin.password=AdminDemoIt-2026",
        "app.demo.usuario.mail=usuario-demo-it@example.test",
        "app.demo.usuario.password=UsuarioDemoIt-2026"
})
@ActiveProfiles({"demo", "test"})
@ContextConfiguration(initializers = DemoTestDatabaseInitializer.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DemoDataSeederIntegrationTest {

    private static final String ADMIN_MAIL = "admin-demo-it@example.test";
    private static final String USUARIO_MAIL = "usuario-demo-it@example.test";
    private static final String TITULO_ANILLOS = "El Señor de los Anillos";
    private static final String TITULO_ODISEA = "La Odisea";
    private static final String PORTADA_ODISEA = "portada_odisea.jpg";
    private static final String[] TABLAS = {
            "autor", "editorial", "portada", "libro", "usuario", "prestamo"};

    @Autowired private DemoDataSeeder seeder;
    @Autowired private AutorRepository autorRepo;
    @Autowired private EditorialRepository editorialRepo;
    @Autowired private LibroRepository libroRepo;
    @Autowired private PortadaRepository portadaRepo;
    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private PrestamoRepository prestamoRepo;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @Order(1)
    void seederCompleto_cargaDatosYLaSegundaCorridaNoDuplicaNada() throws Exception {
        long autores = autorRepo.count();
        long editoriales = editorialRepo.count();
        long libros = libroRepo.count();
        long usuarios = usuarioRepo.count();
        long prestamos = prestamoRepo.count();

        assertThat(autores).isEqualTo(14);
        assertThat(editoriales).isEqualTo(12);
        assertThat(libros).isEqualTo(17);
        assertThat(usuarios).isEqualTo(2);
        assertThat(prestamos).isEqualTo(1);

        Usuario admin = usuarioRepo.buscarPorMail(ADMIN_MAIL);
        Usuario usuario = usuarioRepo.buscarPorMail(USUARIO_MAIL);
        assertThat(admin).isNotNull();
        assertThat(admin.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(usuario).isNotNull();
        assertThat(usuario.getRol()).isEqualTo(Rol.USUARIO);

        List<Libro> anillos = librosPorTitulo(TITULO_ANILLOS);
        assertThat(anillos).hasSize(1);
        assertThat(anillos.get(0).getEjemplaresPrestados()).isEqualTo(1);
        assertThat(anillos.get(0).getEjemplaresRestantes()).isEqualTo(0);
        assertThat(prestamoRepo.existsByUsuario_IdAndLibro_Id(usuario.getId(), anillos.get(0).getId())).isTrue();

        seeder.run();

        assertThat(autorRepo.count()).isEqualTo(autores);
        assertThat(editorialRepo.count()).isEqualTo(editoriales);
        assertThat(libroRepo.count()).isEqualTo(libros);
        assertThat(usuarioRepo.count()).isEqualTo(usuarios);
        assertThat(prestamoRepo.count()).isEqualTo(prestamos);
        assertThat(usuarioRepo.buscarPorMail(ADMIN_MAIL).getRol()).isEqualTo(Rol.ADMIN);
        assertThat(usuarioRepo.buscarPorMail(USUARIO_MAIL).getRol()).isEqualTo(Rol.USUARIO);
    }

    @Test
    @Order(2)
    void falloControladoDespuesDeEscrituras_revierteTodaLaCorridaYElControlRecuperaElEstado() throws Exception {
        // run() debe invocarse sobre el bean Spring (proxy transaccional),
        // nunca sobre una instancia creada a mano.
        assertThat(AopUtils.isAopProxy(seeder)).isTrue();

        Map<String, Long> baselineInicial = contarFilas();

        // --- Preparacion: dejar trabajo pendiente para el seeder ---------
        Usuario usuarioDemo = usuarioRepo.buscarPorMail(USUARIO_MAIL);
        Libro odisea = unicoLibro(TITULO_ODISEA);
        Portada portadaOdisea = odisea.getPortada();
        assertThat(portadaOdisea).isNotNull();

        prestamoRepo.deleteAll();
        usuarioRepo.delete(usuarioDemo);
        libroRepo.delete(odisea);
        portadaRepo.delete(portadaOdisea);

        // Confirmar los borrados preparatorios antes de tomar el snapshot.
        assertThat(prestamoRepo.count()).isEqualTo(0);
        assertThat(usuarioRepo.buscarPorMail(USUARIO_MAIL)).isNull();
        assertThat(librosPorTitulo(TITULO_ODISEA)).isEmpty();
        assertThat(portadaRepo.findAll().stream()
                .anyMatch(p -> PORTADA_ODISEA.equals(p.getNombre()))).isFalse();
        // Sin prestamo, "El Señor de los Anillos" sigue sin ejemplares
        // restantes: solicitar() fallara de forma natural al final de run().
        assertThat(unicoLibro(TITULO_ANILLOS).getEjemplaresRestantes()).isEqualTo(0);

        // Snapshot que se comparara contra el estado posterior al fallo.
        Map<String, Long> snapshot = contarFilas();
        assertThat(snapshot.get("prestamo")).isEqualTo(0L);
        assertThat(snapshot.get("usuario")).isEqualTo(baselineInicial.get("usuario") - 1);
        assertThat(snapshot.get("libro")).isEqualTo(baselineInicial.get("libro") - 1);
        assertThat(snapshot.get("portada")).isEqualTo(baselineInicial.get("portada") - 1);

        // --- Fallo controlado despues de escrituras -----------------------
        // Antes de solicitar() el seeder recrea portada+libro y registra el
        // usuario; luego solicitar() falla sin ejemplares y todo se revierte.
        assertThatThrownBy(() -> seeder.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("prestamo demo")
                .hasCauseInstanceOf(MiExcepcion.class);

        // --- Verificacion fuera de la transaccion fallida -----------------
        // El test no es @Transactional: JdbcTemplate toma su propia conexion
        // (puede ser reutilizada del pool) y lee solo estado confirmado.
        assertThat(contarFilas()).isEqualTo(snapshot);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM libro WHERE titulo = ?", Long.class, TITULO_ODISEA)).isEqualTo(0L);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM portada WHERE nombre = ?", Long.class, PORTADA_ODISEA)).isEqualTo(0L);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM usuario WHERE mail = ?", Long.class, USUARIO_MAIL)).isEqualTo(0L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM prestamo", Long.class)).isEqualTo(0L);

        // --- Control positivo: sin la causa del fallo, el seeder recupera
        // exactamente lo que el rollback dejo sin crear. -------------------
        Libro anillos = unicoLibro(TITULO_ANILLOS);
        anillos.setEjemplaresRestantes(1);
        anillos.setEjemplaresPrestados(0);
        libroRepo.save(anillos);

        seeder.run();

        assertThat(contarFilas()).isEqualTo(baselineInicial);
        assertThat(unicoLibro(TITULO_ODISEA).getPortada()).isNotNull();
        Usuario recreado = usuarioRepo.buscarPorMail(USUARIO_MAIL);
        assertThat(recreado).isNotNull();
        assertThat(recreado.getRol()).isEqualTo(Rol.USUARIO);
        assertThat(usuarioRepo.buscarPorMail(ADMIN_MAIL).getRol()).isEqualTo(Rol.ADMIN);
        Libro anillosFinal = unicoLibro(TITULO_ANILLOS);
        assertThat(anillosFinal.getEjemplaresPrestados()).isEqualTo(1);
        assertThat(anillosFinal.getEjemplaresRestantes()).isEqualTo(0);
        assertThat(prestamoRepo.existsByUsuario_IdAndLibro_Id(recreado.getId(), anillosFinal.getId())).isTrue();
    }

    private List<Libro> librosPorTitulo(String titulo) {
        return libroRepo.findAll().stream()
                .filter(l -> titulo.equals(l.getTitulo()))
                .collect(Collectors.toList());
    }

    private Libro unicoLibro(String titulo) {
        List<Libro> libros = librosPorTitulo(titulo);
        assertThat(libros).hasSize(1);
        return libros.get(0);
    }

    private Map<String, Long> contarFilas() {
        Map<String, Long> conteos = new LinkedHashMap<>();
        for (String tabla : TABLAS) {
            conteos.put(tabla, jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla, Long.class));
        }
        return conteos;
    }
}
