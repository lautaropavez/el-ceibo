package com.mza.Libreria.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mza.Libreria.entidades.Autor;
import com.mza.Libreria.entidades.Editorial;
import com.mza.Libreria.entidades.Libro;
import com.mza.Libreria.excepciones.MiExcepcion;
import com.mza.Libreria.repositorios.AutorRepository;
import com.mza.Libreria.repositorios.EditorialRepository;
import com.mza.Libreria.repositorios.LibroRepository;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;

/**
 * P16.2.E - unitario puro (JUnit 5 + Mockito, sin contexto Spring, sin
 * MySQL) que verifica que los seis metodos de ServiceLibro con
 * PESSIMISTIC_WRITE (alta, baja, altaxAutor, altaxEditorial, bajaxAutor,
 * bajaxEditorial) traducen SIEMPRE una PessimisticLockingFailureException a
 * un MiExcepcion controlado, tanto si el fallo ocurre durante la
 * adquisicion del lock (buscarYBloquearPorId, via CannotAcquireLockException)
 * como durante el flush final (saveAndFlush, via
 * DeadlockLoserDataAccessException) - ambas subclases reales de
 * PessimisticLockingFailureException. Nunca debe escapar la excepcion cruda
 * de Spring, y ningun metodo debe continuar mutando estado despues del
 * fallo.
 */
@ExtendWith(MockitoExtension.class)
class ServiceLibroLockFailureTest {

    private static final String MENSAJE_LIBRO =
            "El libro está siendo modificado por otra operación. Intente nuevamente.";
    private static final String MENSAJE_AUTOR =
            "El autor está siendo modificado por otra operación. Intente nuevamente.";
    private static final String MENSAJE_EDITORIAL =
            "La editorial está siendo modificada por otra operación. Intente nuevamente.";

    @Mock
    private LibroRepository libroRepo;

    @Mock
    private AutorRepository autorRepo;

    @Mock
    private EditorialRepository editorialRepo;

    @InjectMocks
    private ServiceLibro service;

    private Autor autorActivo(String id) {
        Autor autor = new Autor();
        autor.setId(id);
        autor.setAlta(true);
        return autor;
    }

    private Editorial editorialActiva(String id) {
        Editorial editorial = new Editorial();
        editorial.setId(id);
        editorial.setAlta(true);
        return editorial;
    }

    private Libro libroDe(String id, Autor autor, Editorial editorial) {
        Libro libro = new Libro();
        libro.setId(id);
        libro.setAlta(true);
        libro.setAutor(autor);
        libro.setEditorial(editorial);
        return libro;
    }

    // ------------------------------------------------------------------
    // alta()
    // ------------------------------------------------------------------

    @Test
    void alta_fallaAlAdquirirLockDeAutor_traduceAMiExcepcionSinContinuar() {
        LibroRepository.IdsPadresLibro ids = mock(LibroRepository.IdsPadresLibro.class);
        when(ids.getAutorId()).thenReturn("autor-1");
        when(ids.getEditorialId()).thenReturn("editorial-1");
        when(libroRepo.buscarIdsPadresPorId("libro-1")).thenReturn(ids);
        when(autorRepo.buscarYBloquearPorId("autor-1"))
                .thenThrow(new CannotAcquireLockException("lock timeout simulado"));

        assertThatThrownBy(() -> service.alta("libro-1"))
                .isInstanceOf(MiExcepcion.class)
                .hasMessage(MENSAJE_LIBRO);

        verify(editorialRepo, never()).buscarYBloquearPorId(any());
        verify(libroRepo, never()).buscarYBloquearPorId(any());
        verify(libroRepo, never()).saveAndFlush(any());
    }

    @Test
    void alta_fallaEnFlush_traduceAMiExcepcionYRespetaOrdenDeLocks() {
        LibroRepository.IdsPadresLibro ids = mock(LibroRepository.IdsPadresLibro.class);
        when(ids.getAutorId()).thenReturn("autor-1");
        when(ids.getEditorialId()).thenReturn("editorial-1");
        when(libroRepo.buscarIdsPadresPorId("libro-1")).thenReturn(ids);

        Autor autor = autorActivo("autor-1");
        Editorial editorial = editorialActiva("editorial-1");
        Libro libro = libroDe("libro-1", autor, editorial);

        when(autorRepo.buscarYBloquearPorId("autor-1")).thenReturn(autor);
        when(editorialRepo.buscarYBloquearPorId("editorial-1")).thenReturn(editorial);
        when(libroRepo.buscarYBloquearPorId("libro-1")).thenReturn(libro);
        when(libroRepo.saveAndFlush(libro))
                .thenThrow(new DeadlockLoserDataAccessException("deadlock simulado", null));

        assertThatThrownBy(() -> service.alta("libro-1"))
                .isInstanceOf(MiExcepcion.class)
                .hasMessage(MENSAJE_LIBRO);

        // Orden canonico AUTOR -> EDITORIAL -> LIBRO intacto: los tres locks
        // se adquirieron en ese orden antes del intento de flush.
        InOrder orden = Mockito.inOrder(autorRepo, editorialRepo, libroRepo);
        orden.verify(autorRepo).buscarYBloquearPorId("autor-1");
        orden.verify(editorialRepo).buscarYBloquearPorId("editorial-1");
        orden.verify(libroRepo).buscarYBloquearPorId("libro-1");
        orden.verify(libroRepo).saveAndFlush(libro);
        verify(autorRepo, never()).saveAndFlush(any());
        verify(editorialRepo, never()).saveAndFlush(any());
    }

    // ------------------------------------------------------------------
    // baja()
    // ------------------------------------------------------------------

    @Test
    void baja_fallaAlAdquirirLock_traduceAMiExcepcionSinFlush() {
        when(libroRepo.buscarYBloquearPorId("libro-1"))
                .thenThrow(new CannotAcquireLockException("lock timeout simulado"));

        assertThatThrownBy(() -> service.baja("libro-1"))
                .isInstanceOf(MiExcepcion.class)
                .hasMessage(MENSAJE_LIBRO);

        verify(libroRepo, never()).saveAndFlush(any());
    }

    @Test
    void baja_fallaEnFlush_traduceAMiExcepcion() {
        Libro libro = libroDe("libro-1", autorActivo("autor-1"), editorialActiva("editorial-1"));
        when(libroRepo.buscarYBloquearPorId("libro-1")).thenReturn(libro);
        when(libroRepo.saveAndFlush(libro))
                .thenThrow(new DeadlockLoserDataAccessException("deadlock simulado", null));

        assertThatThrownBy(() -> service.baja("libro-1"))
                .isInstanceOf(MiExcepcion.class)
                .hasMessage(MENSAJE_LIBRO);

        assertThat(libro.getAlta()).as("la mutacion en memoria ya ocurrio antes del flush fallido").isFalse();
    }

    // ------------------------------------------------------------------
    // altaxAutor()
    // ------------------------------------------------------------------

    @Test
    void altaxAutor_fallaAlAdquirirLock_traduceAMiExcepcionSinFlush() {
        when(autorRepo.buscarYBloquearPorId("autor-1"))
                .thenThrow(new CannotAcquireLockException("lock timeout simulado"));

        assertThatThrownBy(() -> service.altaxAutor("autor-1"))
                .isInstanceOf(MiExcepcion.class)
                .hasMessage(MENSAJE_AUTOR);

        verify(autorRepo, never()).saveAndFlush(any());
    }

    @Test
    void altaxAutor_fallaEnFlush_traduceAMiExcepcion() {
        Autor autor = autorActivo("autor-1");
        when(autorRepo.buscarYBloquearPorId("autor-1")).thenReturn(autor);
        when(autorRepo.saveAndFlush(autor))
                .thenThrow(new DeadlockLoserDataAccessException("deadlock simulado", null));

        assertThatThrownBy(() -> service.altaxAutor("autor-1"))
                .isInstanceOf(MiExcepcion.class)
                .hasMessage(MENSAJE_AUTOR);
    }

    // ------------------------------------------------------------------
    // altaxEditorial()
    // ------------------------------------------------------------------

    @Test
    void altaxEditorial_fallaAlAdquirirLock_traduceAMiExcepcionSinFlush() {
        when(editorialRepo.buscarYBloquearPorId("editorial-1"))
                .thenThrow(new CannotAcquireLockException("lock timeout simulado"));

        assertThatThrownBy(() -> service.altaxEditorial("editorial-1"))
                .isInstanceOf(MiExcepcion.class)
                .hasMessage(MENSAJE_EDITORIAL);

        verify(editorialRepo, never()).saveAndFlush(any());
    }

    @Test
    void altaxEditorial_fallaEnFlush_traduceAMiExcepcion() {
        Editorial editorial = editorialActiva("editorial-1");
        when(editorialRepo.buscarYBloquearPorId("editorial-1")).thenReturn(editorial);
        when(editorialRepo.saveAndFlush(editorial))
                .thenThrow(new DeadlockLoserDataAccessException("deadlock simulado", null));

        assertThatThrownBy(() -> service.altaxEditorial("editorial-1"))
                .isInstanceOf(MiExcepcion.class)
                .hasMessage(MENSAJE_EDITORIAL);
    }

    // ------------------------------------------------------------------
    // bajaxAutor()
    // ------------------------------------------------------------------

    @Test
    void bajaxAutor_fallaAlAdquirirLockDeUnLibro_detieneElLoopSinFlushDelPadre() {
        Autor autor = autorActivo("autor-1");
        when(autorRepo.buscarYBloquearPorId("autor-1")).thenReturn(autor);
        when(libroRepo.buscarIdsPorAutorId("autor-1")).thenReturn(Arrays.asList("libro-1", "libro-2"));
        when(libroRepo.buscarYBloquearPorId("libro-1"))
                .thenThrow(new CannotAcquireLockException("lock timeout simulado"));

        assertThatThrownBy(() -> service.bajaxAutor("autor-1"))
                .isInstanceOf(MiExcepcion.class)
                .hasMessage(MENSAJE_AUTOR);

        // El loop se detiene: el segundo libro nunca se intenta bloquear, y
        // no hay mutaciones ni flush posteriores al fallo.
        verify(libroRepo, never()).buscarYBloquearPorId("libro-2");
        verify(libroRepo, never()).save(any());
        verify(autorRepo, never()).saveAndFlush(any());
    }

    @Test
    void bajaxAutor_fallaEnFlushDelPadre_unSoloFlushCubreLosLibrosYaGuardados() {
        Autor autor = autorActivo("autor-1");
        Libro libro1 = libroDe("libro-1", autor, editorialActiva("editorial-1"));
        Libro libro2 = libroDe("libro-2", autor, editorialActiva("editorial-1"));

        when(autorRepo.buscarYBloquearPorId("autor-1")).thenReturn(autor);
        when(libroRepo.buscarIdsPorAutorId("autor-1")).thenReturn(Arrays.asList("libro-1", "libro-2"));
        when(libroRepo.buscarYBloquearPorId("libro-1")).thenReturn(libro1);
        when(libroRepo.buscarYBloquearPorId("libro-2")).thenReturn(libro2);
        when(autorRepo.saveAndFlush(autor))
                .thenThrow(new DeadlockLoserDataAccessException("deadlock simulado", null));

        assertThatThrownBy(() -> service.bajaxAutor("autor-1"))
                .isInstanceOf(MiExcepcion.class)
                .hasMessage(MENSAJE_AUTOR);

        assertThat(libro1.getAlta()).isFalse();
        assertThat(libro2.getAlta()).isFalse();
        // El algoritmo original se conserva: un save() por libro (sin
        // flush individual) y un unico saveAndFlush del padre al final, que
        // descarga todo el Persistence Context. Nunca N flushes.
        verify(libroRepo, times(1)).save(libro1);
        verify(libroRepo, times(1)).save(libro2);
        verify(libroRepo, never()).saveAndFlush(any());
        verify(autorRepo, times(1)).saveAndFlush(autor);
    }

    // ------------------------------------------------------------------
    // bajaxEditorial()
    // ------------------------------------------------------------------

    @Test
    void bajaxEditorial_fallaAlAdquirirLockDeUnLibro_detieneElLoopSinFlushDelPadre() {
        Editorial editorial = editorialActiva("editorial-1");
        when(editorialRepo.buscarYBloquearPorId("editorial-1")).thenReturn(editorial);
        when(libroRepo.buscarIdsPorEditorialId("editorial-1")).thenReturn(Arrays.asList("libro-1", "libro-2"));
        when(libroRepo.buscarYBloquearPorId("libro-1"))
                .thenThrow(new CannotAcquireLockException("lock timeout simulado"));

        assertThatThrownBy(() -> service.bajaxEditorial("editorial-1"))
                .isInstanceOf(MiExcepcion.class)
                .hasMessage(MENSAJE_EDITORIAL);

        verify(libroRepo, never()).buscarYBloquearPorId("libro-2");
        verify(libroRepo, never()).save(any());
        verify(editorialRepo, never()).saveAndFlush(any());
    }

    @Test
    void bajaxEditorial_fallaEnFlushDelPadre_unSoloFlushCubreLosLibrosYaGuardados() {
        Editorial editorial = editorialActiva("editorial-1");
        Libro libro1 = libroDe("libro-1", autorActivo("autor-1"), editorial);
        Libro libro2 = libroDe("libro-2", autorActivo("autor-1"), editorial);

        when(editorialRepo.buscarYBloquearPorId("editorial-1")).thenReturn(editorial);
        when(libroRepo.buscarIdsPorEditorialId("editorial-1")).thenReturn(Arrays.asList("libro-1", "libro-2"));
        when(libroRepo.buscarYBloquearPorId("libro-1")).thenReturn(libro1);
        when(libroRepo.buscarYBloquearPorId("libro-2")).thenReturn(libro2);
        when(editorialRepo.saveAndFlush(editorial))
                .thenThrow(new DeadlockLoserDataAccessException("deadlock simulado", null));

        assertThatThrownBy(() -> service.bajaxEditorial("editorial-1"))
                .isInstanceOf(MiExcepcion.class)
                .hasMessage(MENSAJE_EDITORIAL);

        assertThat(libro1.getAlta()).isFalse();
        assertThat(libro2.getAlta()).isFalse();
        verify(libroRepo, times(1)).save(libro1);
        verify(libroRepo, times(1)).save(libro2);
        verify(libroRepo, never()).saveAndFlush(any());
        verify(editorialRepo, times(1)).saveAndFlush(editorial);
    }
}
