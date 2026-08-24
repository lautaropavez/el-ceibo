package com.mza.Libreria.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.mza.Libreria.support.LibreriaIntegrationTest;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@LibreriaIntegrationTest
class ServiceLibroIntegrationTest {

    @Autowired
    private ServiceLibro servicioLibro;

    @Autowired
    private ServicePrestamo servicePrestamo;

    @Autowired
    private AutorRepository autorRepo;

    @Autowired
    private EditorialRepository editorialRepo;

    @Autowired
    private LibroRepository libroRepo;

    @Autowired
    private PrestamoRepository prestamoRepo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Test
    void crearLibro_conTituloYAutorDeLibroActivo_rechazaDuplicado() throws MiExcepcion {
        Autor autor = crearAutorActivo("duplicado-activo");
        Editorial editorial = crearEditorialActiva("duplicado-activo");
        String titulo = "P143 LIBRO DUPLICADO ACTIVO";
        Libro original = crearLibroActivo(titulo, autor, editorial);

        assertThatThrownBy(() -> servicioLibro.crearLibro(null, titulo, 2020, autor.getId(), editorial.getId(), 1))
                .isInstanceOf(MiExcepcion.class);

        List<Libro> coincidencias = libroRepo.buscarPorTituloNormalizadoYAutor(titulo, autor.getId());
        assertThat(coincidencias).extracting(Libro::getId).containsExactly(original.getId());
    }

    @Test
    void crearLibro_conTituloYAutorDeLibroDeBaja_rechazaDuplicado() throws MiExcepcion {
        Autor autor = crearAutorActivo("duplicado-baja");
        Editorial editorial = crearEditorialActiva("duplicado-baja");
        String titulo = "P143 LIBRO DUPLICADO BAJA";
        Libro original = crearLibroActivo(titulo, autor, editorial);
        servicioLibro.baja(original.getId());

        assertThatThrownBy(() -> servicioLibro.crearLibro(null, titulo, 2020, autor.getId(), editorial.getId(), 1))
                .isInstanceOf(MiExcepcion.class);

        List<Libro> coincidencias = libroRepo.buscarPorTituloNormalizadoYAutor(titulo, autor.getId());
        assertThat(coincidencias).extracting(Libro::getId).containsExactly(original.getId());
    }

    @Test
    void crearLibro_conAutorDadoDeBaja_rechaza() throws MiExcepcion {
        Autor autor = crearAutorActivo("autor-baja");
        Autor autorActual = autorRepo.findById(autor.getId())
                .orElseThrow(() -> new IllegalStateException("Autor de fixture no encontrado"));
        autorActual.setAlta(false);
        autorRepo.saveAndFlush(autorActual);

        Editorial editorial = crearEditorialActiva("autor-baja");
        String titulo = "P143 LIBRO AUTOR BAJA";

        assertThatThrownBy(() -> servicioLibro.crearLibro(null, titulo, 2020, autor.getId(), editorial.getId(), 1))
                .isInstanceOf(MiExcepcion.class);

        assertThat(libroRepo.buscarPorTituloNormalizadoYAutor(titulo, autor.getId())).isEmpty();
    }

    @Test
    void eliminarLibro_conPrestamoHistorico_rechazaYConservaLibro() throws MiExcepcion {
        Autor autor = crearAutorActivo("prestamo-historico");
        Editorial editorial = crearEditorialActiva("prestamo-historico");
        String titulo = "P143 LIBRO PRESTAMO HISTORICO";
        Libro libro = crearLibroActivo(titulo, autor, editorial);
        Usuario usuario = crearUsuarioParaPrestamo("prestamo-historico");

        Prestamo prestamo = servicePrestamo.solicitar(usuario.getMail(), libro.getId());
        servicePrestamo.devolver(prestamo.getId());
        String prestamoId = prestamo.getId();

        assertThatThrownBy(() -> servicioLibro.eliminarLibro(libro.getId()))
                .isInstanceOf(MiExcepcion.class);

        assertThat(libroRepo.findById(libro.getId())).isPresent();

        Prestamo prestamoRecargado = prestamoRepo.findById(prestamoId)
                .orElseThrow(() -> new IllegalStateException("Prestamo de fixture no encontrado"));
        assertThat(prestamoRecargado.getFechaDevolucion()).isNotNull();
    }

    @Test
    void eliminarAutor_conLibrosAsociados_rechazaYConservaAutorYLibros() throws MiExcepcion {
        Autor autor = crearAutorActivo("eliminar-con-libros");
        Editorial editorial = crearEditorialActiva("eliminar-con-libros");
        String titulo = "P143 LIBRO ELIMINAR AUTOR";
        Libro libro = crearLibroActivo(titulo, autor, editorial);

        assertThatThrownBy(() -> servicioLibro.eliminarAutor(autor.getId()))
                .isInstanceOf(MiExcepcion.class);

        assertThat(autorRepo.findById(autor.getId())).isPresent();
        assertThat(libroRepo.findById(libro.getId())).isPresent();
    }

    @Test
    void bajaxAutor_daDeBajaAutorYTodosSusLibros() throws MiExcepcion {
        Autor autor = crearAutorActivo("baja-cascada");
        Editorial editorial = crearEditorialActiva("baja-cascada");
        Libro libro1 = crearLibroActivo("P143 LIBRO CASCADA UNO", autor, editorial);
        Libro libro2 = crearLibroActivo("P143 LIBRO CASCADA DOS", autor, editorial);

        Autor autorControl = crearAutorActivo("baja-cascada-control");
        Libro libroControl = crearLibroActivo("P143 LIBRO CASCADA CONTROL", autorControl, editorial);

        servicioLibro.bajaxAutor(autor.getId());

        Autor autorRecargado = autorRepo.findById(autor.getId())
                .orElseThrow(() -> new IllegalStateException("Autor de fixture no encontrado"));
        assertThat(autorRecargado.isAlta()).isFalse();

        Libro libro1Recargado = libroRepo.findById(libro1.getId())
                .orElseThrow(() -> new IllegalStateException("Libro de fixture no encontrado"));
        Libro libro2Recargado = libroRepo.findById(libro2.getId())
                .orElseThrow(() -> new IllegalStateException("Libro de fixture no encontrado"));
        assertThat(libro1Recargado.getAlta()).isFalse();
        assertThat(libro2Recargado.getAlta()).isFalse();

        Autor autorControlRecargado = autorRepo.findById(autorControl.getId())
                .orElseThrow(() -> new IllegalStateException("Autor de control no encontrado"));
        Libro libroControlRecargado = libroRepo.findById(libroControl.getId())
                .orElseThrow(() -> new IllegalStateException("Libro de control no encontrado"));
        assertThat(autorControlRecargado.isAlta()).isTrue();
        assertThat(libroControlRecargado.getAlta()).isTrue();
    }

    @Test
    void altaLibro_conAutorDadoDeBaja_rechazaYConservaLibroInactivo() throws MiExcepcion {
        Autor autor = crearAutorActivo("alta-autor-baja");
        Editorial editorial = crearEditorialActiva("alta-autor-baja");
        String titulo = "P143 LIBRO ALTA AUTOR BAJA";
        Libro libro = crearLibroActivo(titulo, autor, editorial);
        servicioLibro.baja(libro.getId());

        Autor autorActual = autorRepo.findById(autor.getId())
                .orElseThrow(() -> new IllegalStateException("Autor de fixture no encontrado"));
        autorActual.setAlta(false);
        autorRepo.saveAndFlush(autorActual);

        assertThatThrownBy(() -> servicioLibro.alta(libro.getId()))
                .isInstanceOf(MiExcepcion.class);

        Libro libroRecargado = libroRepo.findById(libro.getId())
                .orElseThrow(() -> new IllegalStateException("Libro de fixture no encontrado"));
        assertThat(libroRecargado.getAlta()).isFalse();
    }

    private Autor crearAutorActivo(String sufijo) {
        Autor autor = new Autor();
        autor.setNombre("P143 AUTOR " + sufijo.toUpperCase());
        autor.setAlta(true);
        return autorRepo.saveAndFlush(autor);
    }

    private Editorial crearEditorialActiva(String sufijo) {
        Editorial editorial = new Editorial();
        editorial.setNombre("P143 EDITORIAL " + sufijo.toUpperCase());
        editorial.setAlta(true);
        return editorialRepo.saveAndFlush(editorial);
    }

    private Libro crearLibroActivo(String titulo, Autor autor, Editorial editorial) throws MiExcepcion {
        servicioLibro.crearLibro(null, titulo, 2020, autor.getId(), editorial.getId(), 1);
        return libroRepo.buscarPorTituloNormalizadoYAutor(titulo, autor.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Libro de fixture no encontrado tras crearLibro: " + titulo));
    }

    private Usuario crearUsuarioParaPrestamo(String sufijo) {
        Usuario usuario = new Usuario();
        usuario.setNombre("P143");
        usuario.setApellido("Prestamo");
        usuario.setClave("clave-no-usada");
        usuario.setMail("p143-" + sufijo + "@test.com");
        usuario.setRol(Rol.USUARIO);
        usuario.setAlta(new Date());
        usuario.setBaja(null);
        return usuarioRepo.saveAndFlush(usuario);
    }
}
