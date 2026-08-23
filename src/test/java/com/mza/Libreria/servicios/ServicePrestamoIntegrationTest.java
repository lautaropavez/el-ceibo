package com.mza.Libreria.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mza.Libreria.entidades.Autor;
import com.mza.Libreria.entidades.Libro;
import com.mza.Libreria.entidades.Prestamo;
import com.mza.Libreria.entidades.Usuario;
import com.mza.Libreria.enumeradores.Rol;
import com.mza.Libreria.excepciones.MiExcepcion;
import com.mza.Libreria.repositorios.AutorRepository;
import com.mza.Libreria.repositorios.LibroRepository;
import com.mza.Libreria.repositorios.PrestamoRepository;
import com.mza.Libreria.repositorios.UsuarioRepository;
import com.mza.Libreria.support.LibreriaIntegrationTest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@LibreriaIntegrationTest
class ServicePrestamoIntegrationTest {

    @Autowired
    private ServicePrestamo servicePrestamo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private LibroRepository libroRepo;

    @Autowired
    private AutorRepository autorRepo;

    @Autowired
    private PrestamoRepository prestamoRepo;

    @Test
    void solicitar_conStockDisponible_creaPrestamoActivoConFechasYStockCorrectos() throws MiExcepcion {
        Usuario usuario = crearUsuarioActivo("stock-ok");
        Autor autor = crearAutor("stock-ok");
        Libro libro = crearLibroValido("stock-ok", autor, 1, 0);

        Prestamo prestamo = servicePrestamo.solicitar(usuario.getMail(), libro.getId());

        Prestamo prestamoRecargado = recargarPrestamo(prestamo.getId());
        assertThat(prestamoRecargado.getFechaDevolucion()).isNull();

        ZoneId zona = ZoneId.systemDefault();
        LocalDate fechaPrestamoLocal = prestamoRecargado.getFechaPrestamo().toInstant().atZone(zona).toLocalDate();
        LocalDate fechaVencimientoLocal = prestamoRecargado.getFechaVencimiento().toInstant().atZone(zona).toLocalDate();
        assertThat(fechaVencimientoLocal).isEqualTo(fechaPrestamoLocal.plusDays(14));

        assertStockConsistente(libro.getId(), 0, 1);
    }

    @Test
    void solicitar_sinStock_rechazaYNoModificaNada() {
        Usuario usuario = crearUsuarioActivo("stock-cero");
        Autor autor = crearAutor("stock-cero");
        Libro libro = crearLibroValido("stock-cero", autor, 0, 1);

        assertThatThrownBy(() -> servicePrestamo.solicitar(usuario.getMail(), libro.getId()))
                .isInstanceOf(MiExcepcion.class);

        assertStockConsistente(libro.getId(), 0, 1);
        assertThat(prestamoRepo.existsByUsuario_IdAndLibro_IdAndFechaDevolucionIsNull(usuario.getId(), libro.getId()))
                .isFalse();
    }

    @Test
    void solicitar_conTresPrestamosActivosYCantPrestamosLegacyCero_rechazaElCuarto() throws MiExcepcion {
        Usuario usuario = crearUsuarioConCantPrestamosLegacy("cupo-lleno", 0);
        Autor autor = crearAutor("cupo-lleno");
        Libro libro1 = crearLibroValido("cupo-lleno-1", autor, 1, 0);
        Libro libro2 = crearLibroValido("cupo-lleno-2", autor, 1, 0);
        Libro libro3 = crearLibroValido("cupo-lleno-3", autor, 1, 0);
        Libro libro4 = crearLibroValido("cupo-lleno-4", autor, 1, 0);

        servicePrestamo.solicitar(usuario.getMail(), libro1.getId());
        servicePrestamo.solicitar(usuario.getMail(), libro2.getId());
        servicePrestamo.solicitar(usuario.getMail(), libro3.getId());

        assertThatThrownBy(() -> servicePrestamo.solicitar(usuario.getMail(), libro4.getId()))
                .isInstanceOf(MiExcepcion.class);

        assertThat(prestamoRepo.countByUsuario_IdAndFechaDevolucionIsNull(usuario.getId())).isEqualTo(3);
        assertStockConsistente(libro4.getId(), 1, 0);
    }

    @Test
    void solicitar_trasDevolverUnPrestamo_liberaCupoParaOtroLibro() throws MiExcepcion {
        Usuario usuario = crearUsuarioActivo("libera-cupo");
        Autor autor = crearAutor("libera-cupo");
        Libro libro1 = crearLibroValido("libera-cupo-1", autor, 1, 0);
        Libro libro2 = crearLibroValido("libera-cupo-2", autor, 1, 0);
        Libro libro3 = crearLibroValido("libera-cupo-3", autor, 1, 0);
        Libro libro4 = crearLibroValido("libera-cupo-4", autor, 1, 0);

        Prestamo prestamo1 = servicePrestamo.solicitar(usuario.getMail(), libro1.getId());
        servicePrestamo.solicitar(usuario.getMail(), libro2.getId());
        servicePrestamo.solicitar(usuario.getMail(), libro3.getId());

        servicePrestamo.devolver(prestamo1.getId());

        Prestamo prestamo4 = servicePrestamo.solicitar(usuario.getMail(), libro4.getId());

        Prestamo prestamo4Recargado = recargarPrestamo(prestamo4.getId());
        assertThat(prestamo4Recargado.getFechaDevolucion()).isNull();
        assertStockConsistente(libro4.getId(), 0, 1);
    }

    @Test
    void solicitar_libroYaPrestadoActivoAlMismoUsuario_rechazaDuplicado() throws MiExcepcion {
        Usuario usuario = crearUsuarioActivo("duplicado");
        Autor autor = crearAutor("duplicado");
        Libro libro = crearLibroValido("duplicado", autor, 2, 0);

        servicePrestamo.solicitar(usuario.getMail(), libro.getId());

        assertThatThrownBy(() -> servicePrestamo.solicitar(usuario.getMail(), libro.getId()))
                .isInstanceOf(MiExcepcion.class);

        assertStockConsistente(libro.getId(), 1, 1);
    }

    @Test
    void solicitar_trasDevolverElMismoLibro_permiteNuevaSolicitud() throws MiExcepcion {
        Usuario usuario = crearUsuarioActivo("re-solicitar");
        Autor autor = crearAutor("re-solicitar");
        Libro libro = crearLibroValido("re-solicitar", autor, 1, 0);

        Prestamo primerPrestamo = servicePrestamo.solicitar(usuario.getMail(), libro.getId());
        servicePrestamo.devolver(primerPrestamo.getId());

        Prestamo segundoPrestamo = servicePrestamo.solicitar(usuario.getMail(), libro.getId());

        assertThat(segundoPrestamo.getId()).isNotEqualTo(primerPrestamo.getId());

        Prestamo segundoPrestamoRecargado = recargarPrestamo(segundoPrestamo.getId());
        assertThat(segundoPrestamoRecargado.getFechaDevolucion()).isNull();
        assertStockConsistente(libro.getId(), 0, 1);
    }

    @Test
    void solicitar_conCantPrestamosLegacyAltoYCeroActivos_noBloquea() throws MiExcepcion {
        Usuario usuario = crearUsuarioConCantPrestamosLegacy("legado-alto", 99);
        Autor autor = crearAutor("legado-alto");
        Libro libro = crearLibroValido("legado-alto", autor, 1, 0);

        Prestamo prestamo = servicePrestamo.solicitar(usuario.getMail(), libro.getId());

        Prestamo prestamoRecargado = recargarPrestamo(prestamo.getId());
        assertThat(prestamoRecargado.getFechaDevolucion()).isNull();
        assertStockConsistente(libro.getId(), 0, 1);
    }

    @Test
    void solicitar_conUsuarioDadoDeBaja_rechazaSolicitud() {
        Usuario usuario = crearUsuarioDeBaja("usuario-baja");
        Autor autor = crearAutor("usuario-baja");
        Libro libro = crearLibroValido("usuario-baja", autor, 1, 0);

        assertThatThrownBy(() -> servicePrestamo.solicitar(usuario.getMail(), libro.getId()))
                .isInstanceOf(MiExcepcion.class);

        assertStockConsistente(libro.getId(), 1, 0);
    }

    @Test
    void solicitar_conLibroDadoDeBaja_rechazaSolicitud() {
        Usuario usuario = crearUsuarioActivo("libro-baja");
        Autor autor = crearAutor("libro-baja");
        Libro libro = crearLibroValido("libro-baja", autor, 1, 0);
        libro.setAlta(Boolean.FALSE);
        libroRepo.saveAndFlush(libro);

        assertThatThrownBy(() -> servicePrestamo.solicitar(usuario.getMail(), libro.getId()))
                .isInstanceOf(MiExcepcion.class);

        assertThat(prestamoRepo.existsByUsuario_IdAndLibro_IdAndFechaDevolucionIsNull(usuario.getId(), libro.getId()))
                .isFalse();
        assertStockConsistente(libro.getId(), 1, 0);
    }

    @Test
    void devolver_prestamoActivo_fijaFechaDevolucionYRestauraStockConservandoHistorial() throws MiExcepcion {
        Usuario usuario = crearUsuarioActivo("devolucion-ok");
        Autor autor = crearAutor("devolucion-ok");
        Libro libro = crearLibroValido("devolucion-ok", autor, 1, 0);

        Prestamo prestamo = servicePrestamo.solicitar(usuario.getMail(), libro.getId());

        servicePrestamo.devolver(prestamo.getId());

        Prestamo prestamoRecargado = recargarPrestamo(prestamo.getId());
        assertThat(prestamoRecargado.getFechaDevolucion()).isNotNull();

        assertStockConsistente(libro.getId(), 1, 0);
    }

    @Test
    void devolver_prestamoYaDevuelto_rechazaSinRestaurarStockDeNuevo() throws MiExcepcion {
        Usuario usuario = crearUsuarioActivo("doble-devolucion");
        Autor autor = crearAutor("doble-devolucion");
        Libro libro = crearLibroValido("doble-devolucion", autor, 1, 0);

        Prestamo prestamo = servicePrestamo.solicitar(usuario.getMail(), libro.getId());
        servicePrestamo.devolver(prestamo.getId());

        assertThatThrownBy(() -> servicePrestamo.devolver(prestamo.getId()))
                .isInstanceOf(MiExcepcion.class);

        assertStockConsistente(libro.getId(), 1, 0);
    }

    @Test
    void devolver_conUsuarioYLibroDadosDeBajaDespuesDePrestamo_igualPermiteDevolucion() throws MiExcepcion {
        Usuario usuario = crearUsuarioActivo("baja-post-prestamo");
        Autor autor = crearAutor("baja-post-prestamo");
        Libro libro = crearLibroValido("baja-post-prestamo", autor, 1, 0);

        Prestamo prestamo = servicePrestamo.solicitar(usuario.getMail(), libro.getId());

        Usuario usuarioActual = usuarioRepo.findById(usuario.getId())
                .orElseThrow(() -> new IllegalStateException("Usuario de fixture no encontrado"));
        usuarioActual.setBaja(new Date());
        usuarioRepo.saveAndFlush(usuarioActual);

        Libro libroActual = recargarLibro(libro.getId());
        libroActual.setAlta(Boolean.FALSE);
        libroRepo.saveAndFlush(libroActual);

        servicePrestamo.devolver(prestamo.getId());

        Prestamo prestamoRecargado = recargarPrestamo(prestamo.getId());
        assertThat(prestamoRecargado.getFechaDevolucion()).isNotNull();

        assertStockConsistente(libro.getId(), 1, 0);
    }

    @Test
    void listarTodos_incluyeActivoYDevuelto() throws MiExcepcion {
        Usuario usuario = crearUsuarioActivo("listar-todos");
        Autor autor = crearAutor("listar-todos");
        Libro libroActivo = crearLibroValido("listar-todos-activo", autor, 1, 0);
        Libro libroDevuelto = crearLibroValido("listar-todos-devuelto", autor, 1, 0);

        Prestamo prestamoActivo = servicePrestamo.solicitar(usuario.getMail(), libroActivo.getId());
        Prestamo prestamoDevuelto = servicePrestamo.solicitar(usuario.getMail(), libroDevuelto.getId());
        servicePrestamo.devolver(prestamoDevuelto.getId());

        List<Prestamo> todos = servicePrestamo.listarTodos();

        assertThat(todos)
                .extracting(Prestamo::getId)
                .contains(prestamoActivo.getId(), prestamoDevuelto.getId());
    }

    @Test
    void misPrestamos_incluyeActivosYDevueltosYSeparaUsuarios() throws MiExcepcion {
        Usuario usuarioA = crearUsuarioActivo("mis-prestamos-a");
        Usuario usuarioB = crearUsuarioActivo("mis-prestamos-b");
        Autor autor = crearAutor("mis-prestamos");
        Libro libroActivoA = crearLibroValido("mis-prestamos-a-activo", autor, 1, 0);
        Libro libroDevueltoA = crearLibroValido("mis-prestamos-a-devuelto", autor, 1, 0);
        Libro libroB = crearLibroValido("mis-prestamos-b", autor, 1, 0);

        Prestamo prestamoActivoA = servicePrestamo.solicitar(usuarioA.getMail(), libroActivoA.getId());
        Prestamo prestamoDevueltoA = servicePrestamo.solicitar(usuarioA.getMail(), libroDevueltoA.getId());
        Prestamo prestamoB = servicePrestamo.solicitar(usuarioB.getMail(), libroB.getId());

        servicePrestamo.devolver(prestamoDevueltoA.getId());

        List<Prestamo> prestamosDeA = servicePrestamo.misPrestamos(usuarioA.getMail());

        assertThat(prestamosDeA).hasSize(2);
        assertThat(prestamosDeA)
                .extracting(Prestamo::getId)
                .containsExactlyInAnyOrder(prestamoActivoA.getId(), prestamoDevueltoA.getId())
                .doesNotContain(prestamoB.getId());

        Prestamo activoEnResultado = prestamosDeA.stream()
                .filter(p -> p.getId().equals(prestamoActivoA.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Prestamo activo A no encontrado en misPrestamos"));
        assertThat(activoEnResultado.getFechaDevolucion()).isNull();

        Prestamo devueltoEnResultado = prestamosDeA.stream()
                .filter(p -> p.getId().equals(prestamoDevueltoA.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Prestamo devuelto A no encontrado en misPrestamos"));
        assertThat(devueltoEnResultado.getFechaDevolucion()).isNotNull();
    }

    private void assertStockConsistente(String libroId, int restantesEsperados, int prestadosEsperados) {
        Libro libro = recargarLibro(libroId);
        assertThat(libro.getEjemplaresRestantes()).isEqualTo(restantesEsperados);
        assertThat(libro.getEjemplaresPrestados()).isEqualTo(prestadosEsperados);
        assertThat(libro.getNroejemplares())
                .isEqualTo(libro.getEjemplaresRestantes() + libro.getEjemplaresPrestados());
    }

    private Usuario crearUsuarioActivo(String sufijo) {
        return crearUsuario(sufijo, null);
    }

    private Usuario crearUsuarioConCantPrestamosLegacy(String sufijo, int cantPrestamosLegacy) {
        return crearUsuario(sufijo, cantPrestamosLegacy);
    }

    private Usuario crearUsuarioDeBaja(String sufijo) {
        Usuario usuario = crearUsuario(sufijo, null);
        usuario.setBaja(new Date());
        return usuarioRepo.saveAndFlush(usuario);
    }

    private Usuario crearUsuario(String sufijo, Integer cantPrestamosLegacy) {
        Usuario usuario = new Usuario();
        usuario.setNombre("P142");
        usuario.setApellido("Test");
        usuario.setClave("clave-no-usada");
        usuario.setMail("p142-" + sufijo + "@test.com");
        usuario.setRol(Rol.USUARIO);
        usuario.setAlta(new Date());
        usuario.setBaja(null);
        usuario.setCantidadPrestamos(cantPrestamosLegacy);
        return usuarioRepo.saveAndFlush(usuario);
    }

    private Autor crearAutor(String sufijo) {
        Autor autor = new Autor();
        autor.setNombre("P142 Autor " + sufijo);
        autor.setAlta(true);
        return autorRepo.saveAndFlush(autor);
    }

    private Libro crearLibroValido(String sufijo, Autor autor, int restantes, int prestados) {
        Libro libro = new Libro();
        libro.setTitulo("P142 Libro " + sufijo);
        libro.setAutor(autor);
        libro.setAlta(Boolean.TRUE);
        libro.setEjemplaresRestantes(restantes);
        libro.setEjemplaresPrestados(prestados);
        libro.setNroejemplares(restantes + prestados);
        return libroRepo.saveAndFlush(libro);
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
