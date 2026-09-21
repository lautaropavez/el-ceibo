package com.mza.Libreria.servicios;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.mza.Libreria.support.LibreriaIntegrationTest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * P16.2.B4c - matriz de concurrencia real del catalogo.
 *
 * Intercalado determinista, sin hooks en codigo productivo:
 * - el hilo RETENEDOR ejecuta el service real dentro de un TransactionTemplate
 *   externo; el service se une a esa transaccion y sus locks pesimistas
 *   quedan retenidos hasta que el test libera un latch de commit;
 * - el hilo ESPERADOR ejecuta el service real en su propia transaccion y
 *   debe quedar bloqueado detras del retenedor;
 * - el bloqueo se comprueba en INFORMATION_SCHEMA (MariaDB): una transaccion
 *   en LOCK WAIT bloqueada especificamente por la transaccion del retenedor,
 *   identificada por su CONNECTION_ID;
 * - recien entonces se libera el retenedor y se verifica el resultado.
 *
 * El estado final se relee por JDBC (fuera de Hibernate) despues de ambas
 * transacciones. Los fixtures se confirman en transacciones independientes
 * antes de iniciar los hilos y solo se borran fixtures P162B4C.
 */
@LibreriaIntegrationTest
class CatalogConcurrencyIntegrationTest {

    private static final String PREFIJO = "P162B4C";
    private static final long TIMEOUT_SEGUNDOS = 15;
    private static final long PASO_SONDEO_MS = 100;

    private static final String SQL_ESPERAS_BLOQUEADAS_POR =
            "SELECT COUNT(DISTINCT w.requesting_trx_id) "
            + "FROM information_schema.INNODB_LOCK_WAITS w "
            + "JOIN information_schema.INNODB_TRX bloqueante ON bloqueante.trx_id = w.blocking_trx_id "
            + "JOIN information_schema.INNODB_TRX esperante ON esperante.trx_id = w.requesting_trx_id "
            + "WHERE bloqueante.trx_mysql_thread_id = ? AND esperante.trx_state = 'LOCK WAIT'";

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
    private UsuarioRepository usuarioRepo;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager txManager;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    private final List<String> autorIds = new ArrayList<>();
    private final List<String> editorialIds = new ArrayList<>();
    private final List<String> usuarioIds = new ArrayList<>();

    // ------------------------------------------------------------------
    // 1-4: baja de padre vs alta(libro)
    // ------------------------------------------------------------------

    @Test
    void bajaxAutorRetiene_altaLibroEspera_altaRechazadaYNingunLibroActivoBajoAutorInactivo() throws Exception {
        Autor autor = crearAutor("C1");
        Editorial editorial = crearEditorial("C1");
        Libro inactivo = crearLibro("C1 INACTIVO", autor, editorial, false, 1, 1);
        Libro activo = crearLibro("C1 ACTIVO", autor, editorial, true, 1, 1);

        Resultado r = correrCarrera(TransactionDefinition.ISOLATION_DEFAULT,
                () -> servicioLibro.bajaxAutor(autor.getId()),
                () -> servicioLibro.alta(inactivo.getId()));

        assertThat(r.retenedor).as("retenedor bajaxAutor").isNull();
        assertMiExcepcion(r.esperador, "autor", "baja");
        assertThat(autorAlta(autor.getId())).isFalse();
        assertThat(libroAlta(inactivo.getId())).isFalse();
        assertThat(libroAlta(activo.getId())).isFalse();
        assertNingunLibroActivoBajoPadreInactivo();
    }

    @Test
    void altaLibroRetiene_bajaxAutorEspera_terminaConTodosLosLibrosDadosDeBaja() throws Exception {
        Autor autor = crearAutor("C2");
        Editorial editorial = crearEditorial("C2");
        Libro inactivo = crearLibro("C2 INACTIVO", autor, editorial, false, 1, 1);
        Libro activo = crearLibro("C2 ACTIVO", autor, editorial, true, 1, 1);

        Resultado r = correrCarrera(TransactionDefinition.ISOLATION_DEFAULT,
                () -> servicioLibro.alta(inactivo.getId()),
                () -> servicioLibro.bajaxAutor(autor.getId()));

        assertThat(r.retenedor).as("retenedor alta").isNull();
        assertThat(r.esperador).as("esperador bajaxAutor").isNull();
        assertThat(autorAlta(autor.getId())).isFalse();
        assertThat(libroAlta(inactivo.getId())).as("libro reactivado y luego dado de baja por el autor").isFalse();
        assertThat(libroAlta(activo.getId())).isFalse();
        assertNingunLibroActivoBajoPadreInactivo();
    }

    @Test
    void bajaxEditorialRetiene_altaLibroEspera_altaRechazadaYNingunLibroActivoBajoEditorialInactiva() throws Exception {
        Autor autor = crearAutor("C3");
        Editorial editorial = crearEditorial("C3");
        Libro inactivo = crearLibro("C3 INACTIVO", autor, editorial, false, 1, 1);
        Libro activo = crearLibro("C3 ACTIVO", autor, editorial, true, 1, 1);

        Resultado r = correrCarrera(TransactionDefinition.ISOLATION_DEFAULT,
                () -> servicioLibro.bajaxEditorial(editorial.getId()),
                () -> servicioLibro.alta(inactivo.getId()));

        assertThat(r.retenedor).as("retenedor bajaxEditorial").isNull();
        assertMiExcepcion(r.esperador, "editorial", "baja");
        assertThat(editorialAlta(editorial.getId())).isFalse();
        assertThat(libroAlta(inactivo.getId())).isFalse();
        assertThat(libroAlta(activo.getId())).isFalse();
        assertNingunLibroActivoBajoPadreInactivo();
    }

    @Test
    void altaLibroRetiene_bajaxEditorialEspera_terminaConTodosLosLibrosDadosDeBaja() throws Exception {
        Autor autor = crearAutor("C4");
        Editorial editorial = crearEditorial("C4");
        Libro inactivo = crearLibro("C4 INACTIVO", autor, editorial, false, 1, 1);
        Libro activo = crearLibro("C4 ACTIVO", autor, editorial, true, 1, 1);

        Resultado r = correrCarrera(TransactionDefinition.ISOLATION_DEFAULT,
                () -> servicioLibro.alta(inactivo.getId()),
                () -> servicioLibro.bajaxEditorial(editorial.getId()));

        assertThat(r.retenedor).as("retenedor alta").isNull();
        assertThat(r.esperador).as("esperador bajaxEditorial").isNull();
        assertThat(editorialAlta(editorial.getId())).isFalse();
        assertThat(libroAlta(inactivo.getId())).isFalse();
        assertThat(libroAlta(activo.getId())).isFalse();
        assertNingunLibroActivoBajoPadreInactivo();
    }

    // ------------------------------------------------------------------
    // 5-8: baja de padre vs crearLibro
    // ------------------------------------------------------------------

    @Test
    void bajaxAutorRetiene_crearLibroEspera_creacionRechazadaYSinLibroNuevo() throws Exception {
        Autor autor = crearAutor("C5");
        Editorial editorial = crearEditorial("C5");
        Libro existente = crearLibro("C5 EXISTENTE", autor, editorial, true, 1, 1);
        String tituloNuevo = titulo("C5 NUEVO");

        Resultado r = correrCarrera(TransactionDefinition.ISOLATION_DEFAULT,
                () -> servicioLibro.bajaxAutor(autor.getId()),
                () -> servicioLibro.crearLibro(null, tituloNuevo, 2000, autor.getId(), editorial.getId(), 1));

        assertThat(r.retenedor).as("retenedor bajaxAutor").isNull();
        assertMiExcepcion(r.esperador, "autor", "baja");
        assertThat(autorAlta(autor.getId())).isFalse();
        assertThat(libroAlta(existente.getId())).isFalse();
        assertThat(contarLibrosPorTituloYAutor(tituloNuevo, autor.getId())).as("libro nuevo").isZero();
        assertNingunLibroActivoBajoPadreInactivo();
    }

    @Test
    void crearLibroRetiene_bajaxAutorEspera_libroNuevoTerminaDadoDeBaja() throws Exception {
        Autor autor = crearAutor("C6");
        Editorial editorial = crearEditorial("C6");
        Libro existente = crearLibro("C6 EXISTENTE", autor, editorial, true, 1, 1);
        String tituloNuevo = titulo("C6 NUEVO");

        Resultado r = correrCarrera(TransactionDefinition.ISOLATION_DEFAULT,
                () -> servicioLibro.crearLibro(null, tituloNuevo, 2000, autor.getId(), editorial.getId(), 1),
                () -> servicioLibro.bajaxAutor(autor.getId()));

        assertThat(r.retenedor).as("retenedor crearLibro").isNull();
        assertThat(r.esperador).as("esperador bajaxAutor").isNull();
        assertThat(autorAlta(autor.getId())).isFalse();
        assertThat(libroAlta(existente.getId())).isFalse();
        assertThat(contarLibrosPorTituloYAutor(tituloNuevo, autor.getId())).as("libro nuevo").isEqualTo(1);
        assertThat(libroAltaPorTituloYAutor(tituloNuevo, autor.getId())).as("libro nuevo activo").isFalse();
        assertNingunLibroActivoBajoPadreInactivo();
    }

    @Test
    void bajaxEditorialRetiene_crearLibroEspera_creacionRechazadaYSinLibroNuevo() throws Exception {
        Autor autor = crearAutor("C7");
        Editorial editorial = crearEditorial("C7");
        Libro existente = crearLibro("C7 EXISTENTE", autor, editorial, true, 1, 1);
        String tituloNuevo = titulo("C7 NUEVO");

        Resultado r = correrCarrera(TransactionDefinition.ISOLATION_DEFAULT,
                () -> servicioLibro.bajaxEditorial(editorial.getId()),
                () -> servicioLibro.crearLibro(null, tituloNuevo, 2000, autor.getId(), editorial.getId(), 1));

        assertThat(r.retenedor).as("retenedor bajaxEditorial").isNull();
        assertMiExcepcion(r.esperador, "editorial", "baja");
        assertThat(editorialAlta(editorial.getId())).isFalse();
        assertThat(libroAlta(existente.getId())).isFalse();
        assertThat(contarLibrosPorTituloYAutor(tituloNuevo, autor.getId())).as("libro nuevo").isZero();
        assertNingunLibroActivoBajoPadreInactivo();
    }

    @Test
    void crearLibroRetiene_bajaxEditorialEspera_libroNuevoTerminaDadoDeBaja() throws Exception {
        Autor autor = crearAutor("C8");
        Editorial editorial = crearEditorial("C8");
        Libro existente = crearLibro("C8 EXISTENTE", autor, editorial, true, 1, 1);
        String tituloNuevo = titulo("C8 NUEVO");

        Resultado r = correrCarrera(TransactionDefinition.ISOLATION_DEFAULT,
                () -> servicioLibro.crearLibro(null, tituloNuevo, 2000, autor.getId(), editorial.getId(), 1),
                () -> servicioLibro.bajaxEditorial(editorial.getId()));

        assertThat(r.retenedor).as("retenedor crearLibro").isNull();
        assertThat(r.esperador).as("esperador bajaxEditorial").isNull();
        assertThat(editorialAlta(editorial.getId())).isFalse();
        assertThat(libroAlta(existente.getId())).isFalse();
        assertThat(contarLibrosPorTituloYAutor(tituloNuevo, autor.getId())).as("libro nuevo").isEqualTo(1);
        assertThat(libroAltaPorTituloYAutor(tituloNuevo, autor.getId())).as("libro nuevo activo").isFalse();
        assertNingunLibroActivoBajoPadreInactivo();
    }

    // ------------------------------------------------------------------
    // 9: modificarLibro concurrente hacia el mismo titulo + autor
    // ------------------------------------------------------------------

    @Test
    void modificarLibroRetiene_otroModificarLibroHaciaMismoTituloYAutor_soloUnoGana() throws Exception {
        Autor autor = crearAutor("C9");
        Editorial editorial = crearEditorial("C9");
        String tituloOriginal1 = titulo("C9 ORIGINAL 1");
        String tituloOriginal2 = titulo("C9 ORIGINAL 2");
        String tituloObjetivo = titulo("C9 OBJETIVO");
        Libro libro1 = crearLibroConTitulo(tituloOriginal1, autor, editorial, true, 1, 1);
        Libro libro2 = crearLibroConTitulo(tituloOriginal2, autor, editorial, true, 1, 1);

        Resultado r = correrCarrera(TransactionDefinition.ISOLATION_READ_COMMITTED,
                () -> servicioLibro.modificarLibro(null, libro1.getId(), tituloObjetivo, 2001,
                        autor.getNombre(), editorial.getNombre()),
                () -> servicioLibro.modificarLibro(null, libro2.getId(), tituloObjetivo, 2001,
                        autor.getNombre(), editorial.getNombre()));

        assertThat(r.retenedor).as("retenedor modificarLibro L1").isNull();
        assertMiExcepcion(r.esperador, "ya existe un libro con ese t");
        assertThat(contarLibrosPorTituloYAutor(tituloObjetivo, autor.getId())).as("duplicados titulo+autor").isEqualTo(1);
        assertThat(tituloDeLibro(libro1.getId())).isEqualTo(tituloObjetivo);
        assertThat(tituloDeLibro(libro2.getId())).as("L2 sin escritura parcial").isEqualTo(tituloOriginal2);
        assertNingunLibroActivoBajoPadreInactivo();
    }

    // ------------------------------------------------------------------
    // 10-11: baja(libro) vs solicitar prestamo
    // ------------------------------------------------------------------

    @Test
    void bajaLibroRetiene_solicitarEspera_prestamoRechazadoYStockIntacto() throws Exception {
        Autor autor = crearAutor("C10");
        Editorial editorial = crearEditorial("C10");
        Usuario usuario = crearUsuario("C10");
        Libro libro = crearLibro("C10 LIBRO", autor, editorial, true, 1, 0);

        Resultado r = correrCarrera(TransactionDefinition.ISOLATION_DEFAULT,
                () -> servicioLibro.baja(libro.getId()),
                () -> servicePrestamo.solicitar(usuario.getMail(), libro.getId()));

        assertThat(r.retenedor).as("retenedor baja").isNull();
        assertMiExcepcion(r.esperador, "dado de baja");
        assertThat(libroAlta(libro.getId())).isFalse();
        assertStock(libro.getId(), 1, 0, 1);
        assertThat(contarPrestamosActivosDeLibro(libro.getId())).isZero();
    }

    @Test
    void solicitarRetiene_bajaLibroEspera_prestamoActivoYLibroDeBajaConStockConsistente() throws Exception {
        Autor autor = crearAutor("C11");
        Editorial editorial = crearEditorial("C11");
        Usuario usuario = crearUsuario("C11");
        Libro libro = crearLibro("C11 LIBRO", autor, editorial, true, 1, 0);

        Resultado r = correrCarrera(TransactionDefinition.ISOLATION_READ_COMMITTED,
                () -> servicePrestamo.solicitar(usuario.getMail(), libro.getId()),
                () -> servicioLibro.baja(libro.getId()));

        assertThat(r.retenedor).as("retenedor solicitar").isNull();
        assertThat(r.esperador).as("esperador baja").isNull();
        assertThat(libroAlta(libro.getId())).isFalse();
        assertStock(libro.getId(), 0, 1, 1);
        assertThat(contarPrestamosActivosDeLibro(libro.getId())).isEqualTo(1);
    }

    // ------------------------------------------------------------------
    // Harness
    // ------------------------------------------------------------------

    @FunctionalInterface
    private interface Operacion {
        void ejecutar() throws Exception;
    }

    private static final class Resultado {
        private final Throwable retenedor;
        private final Throwable esperador;

        private Resultado(Throwable retenedor, Throwable esperador) {
            this.retenedor = retenedor;
            this.esperador = esperador;
        }
    }

    /**
     * Corre una carrera determinista: el retenedor ejecuta su operacion y
     * conserva sus locks hasta el latch; el esperador arranca despues y debe
     * quedar bloqueado detras del retenedor. Ambos futuros se esperan con
     * timeout. Todo se libera y se cierra en finally, incluso si falla.
     */
    private Resultado correrCarrera(int aislamientoRetenedor, Operacion retenedora, Operacion esperadora)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<Throwable>> futuros = new ArrayList<>();
        CountDownLatch reteniendo = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        AtomicLong conexionRetenedora = new AtomicLong(-1);
        AtomicReference<Throwable> errorRetenedor = new AtomicReference<>();
        try {
            TransactionTemplate plantilla = new TransactionTemplate(txManager);
            plantilla.setIsolationLevel(aislamientoRetenedor);

            Future<Throwable> futuroRetenedor = executor.submit(() -> {
                try {
                    plantilla.<Object>execute(status -> {
                        try {
                            retenedora.ejecutar();
                            conexionRetenedora.set(jdbc.queryForObject("SELECT CONNECTION_ID()", Long.class));
                        } catch (Throwable t) {
                            errorRetenedor.compareAndSet(null, t);
                            status.setRollbackOnly();
                            reteniendo.countDown();
                            return null;
                        }
                        reteniendo.countDown();
                        try {
                            if (!liberar.await(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)) {
                                errorRetenedor.compareAndSet(null,
                                        new IllegalStateException("El latch de liberacion nunca se abrio"));
                                status.setRollbackOnly();
                            }
                        } catch (InterruptedException ex) {
                            errorRetenedor.compareAndSet(null, ex);
                            status.setRollbackOnly();
                        }
                        return null;
                    });
                } catch (Throwable t) {
                    errorRetenedor.compareAndSet(null, t);
                } finally {
                    reteniendo.countDown();
                }
                return errorRetenedor.get();
            });
            futuros.add(futuroRetenedor);

            assertThat(reteniendo.await(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS))
                    .as("el retenedor debia llegar a retener sus locks").isTrue();
            if (errorRetenedor.get() != null) {
                throw new AssertionError("El retenedor fallo antes de retener: " + errorRetenedor.get(),
                        errorRetenedor.get());
            }
            assertThat(conexionRetenedora.get()).as("CONNECTION_ID del retenedor").isPositive();

            Future<Throwable> futuroEsperador = executor.submit(() -> {
                try {
                    esperadora.ejecutar();
                    return (Throwable) null;
                } catch (Throwable t) {
                    return t;
                }
            });
            futuros.add(futuroEsperador);

            esperarBloqueoDetrasDe(conexionRetenedora.get(), futuroEsperador);

            liberar.countDown();
            Throwable resultadoRetenedor = futuroRetenedor.get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
            Throwable resultadoEsperador = futuroEsperador.get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
            return new Resultado(resultadoRetenedor, resultadoEsperador);
        } finally {
            liberar.countDown();
            for (Future<Throwable> futuro : futuros) {
                futuro.cancel(true);
            }
            executor.shutdownNow();
            boolean terminado = executor.awaitTermination(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
            assertThat(terminado).as("los hilos del caso deben terminar (sin bloqueos vivos)").isTrue();
        }
    }

    /**
     * Sondea hasta comprobar que existe exactamente una transaccion en LOCK
     * WAIT bloqueada por la transaccion del retenedor. El paso entre sondeos
     * es Future.get con timeout (no hay sleep): si el esperador termina antes
     * de bloquearse, el caso falla.
     */
    private void esperarBloqueoDetrasDe(long conexionRetenedora, Future<Throwable> esperador)
            throws InterruptedException {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SEGUNDOS);
        while (true) {
            try {
                Throwable resultado = esperador.get(PASO_SONDEO_MS, TimeUnit.MILLISECONDS);
                throw new AssertionError("El esperador termino sin bloquearse detras del retenedor: " + resultado);
            } catch (TimeoutException ex) {
                // sigue vivo: todavia no se sabe si esta bloqueado
            } catch (ExecutionException ex) {
                throw new AssertionError("El esperador fallo sin bloquearse", ex);
            }
            Long esperas = jdbc.queryForObject(SQL_ESPERAS_BLOQUEADAS_POR, Long.class, conexionRetenedora);
            if (esperas != null && esperas > 1) {
                throw new AssertionError("Espera ambigua: " + esperas + " transacciones bloqueadas por el retenedor");
            }
            if (esperas != null && esperas == 1) {
                return;
            }
            if (System.nanoTime() > limite) {
                throw new AssertionError("Timeout: no se detecto LOCK WAIT detras del retenedor");
            }
        }
    }

    // ------------------------------------------------------------------
    // Fixtures (cada saveAndFlush confirma en su propia transaccion)
    // ------------------------------------------------------------------

    private String titulo(String tag) {
        return PREFIJO + " " + tag + " " + sufijo;
    }

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

    private Usuario crearUsuario(String tag) {
        Usuario usuario = new Usuario();
        usuario.setNombre(PREFIJO);
        usuario.setApellido("Test");
        usuario.setClave("clave-no-usada");
        usuario.setMail(PREFIJO.toLowerCase() + "-" + tag.toLowerCase() + "-" + sufijo.toLowerCase() + "@test.com");
        usuario.setRol(Rol.USUARIO);
        usuario.setAlta(new Date());
        usuario.setBaja(null);
        usuario.setCantidadPrestamos(null);
        Usuario guardado = usuarioRepo.saveAndFlush(usuario);
        usuarioIds.add(guardado.getId());
        return guardado;
    }

    private Libro crearLibro(String tag, Autor autor, Editorial editorial, boolean alta, int restantes, int prestados) {
        return crearLibroConTitulo(titulo(tag), autor, editorial, alta, restantes, prestados);
    }

    private Libro crearLibroConTitulo(String titulo, Autor autor, Editorial editorial, boolean alta,
            int restantes, int prestados) {
        Libro libro = new Libro();
        libro.setTitulo(titulo);
        libro.setAnio(2000);
        libro.setAlta(alta);
        libro.setNroejemplares(restantes + prestados);
        libro.setEjemplaresRestantes(restantes);
        libro.setEjemplaresPrestados(prestados);
        libro.setAutor(autor);
        libro.setEditorial(editorial);
        return libroRepo.saveAndFlush(libro);
    }

    /**
     * Limpieza en transacciones independientes (cada sentencia JDBC hace
     * autocommit), solo sobre los ids creados por este test y con guarda de
     * prefijo P162B4C. Orden dependiente: prestamos, libros, usuarios,
     * autores, editoriales.
     */
    @AfterEach
    void limpiarFixtures() {
        for (String autorId : autorIds) {
            jdbc.update("DELETE FROM prestamo WHERE libro_id IN "
                    + "(SELECT id FROM libro WHERE autor_id = ? AND titulo LIKE ?)", autorId, PREFIJO + "%");
        }
        for (String usuarioId : usuarioIds) {
            jdbc.update("DELETE FROM prestamo WHERE usuario_id = ?", usuarioId);
        }
        for (String autorId : autorIds) {
            jdbc.update("DELETE FROM libro WHERE autor_id = ? AND titulo LIKE ?", autorId, PREFIJO + "%");
        }
        for (String usuarioId : usuarioIds) {
            jdbc.update("DELETE FROM usuario WHERE id = ? AND nombre = ?", usuarioId, PREFIJO);
        }
        for (String autorId : autorIds) {
            jdbc.update("DELETE FROM autor WHERE id = ? AND nombre LIKE ?", autorId, PREFIJO + "%");
        }
        for (String editorialId : editorialIds) {
            jdbc.update("DELETE FROM editorial WHERE id = ? AND nombre LIKE ?", editorialId, PREFIJO + "%");
        }
    }

    // ------------------------------------------------------------------
    // Lecturas de estado final: JDBC directo, fuera de Hibernate
    // ------------------------------------------------------------------

    private boolean autorAlta(String id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT alta FROM autor WHERE id = ?", Boolean.class, id));
    }

    private boolean editorialAlta(String id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT alta FROM editorial WHERE id = ?", Boolean.class, id));
    }

    private boolean libroAlta(String id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT alta FROM libro WHERE id = ?", Boolean.class, id));
    }

    private String tituloDeLibro(String id) {
        return jdbc.queryForObject("SELECT titulo FROM libro WHERE id = ?", String.class, id);
    }

    private long contarLibrosPorTituloYAutor(String titulo, String autorId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM libro WHERE UPPER(TRIM(titulo)) = UPPER(TRIM(?)) AND autor_id = ?",
                Long.class, titulo, autorId);
    }

    private boolean libroAltaPorTituloYAutor(String titulo, String autorId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT alta FROM libro WHERE UPPER(TRIM(titulo)) = UPPER(TRIM(?)) AND autor_id = ?",
                Boolean.class, titulo, autorId));
    }

    private long contarPrestamosActivosDeLibro(String libroId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM prestamo WHERE libro_id = ? AND devolucion IS NULL", Long.class, libroId);
    }

    private void assertStock(String libroId, int restantes, int prestados, int total) {
        assertThat(jdbc.queryForObject("SELECT ejemplares_restantes FROM libro WHERE id = ?", Integer.class, libroId))
                .as("ejemplares restantes").isEqualTo(restantes);
        assertThat(jdbc.queryForObject("SELECT ejemplares_prestados FROM libro WHERE id = ?", Integer.class, libroId))
                .as("ejemplares prestados").isEqualTo(prestados);
        assertThat(jdbc.queryForObject("SELECT nroejemplares FROM libro WHERE id = ?", Integer.class, libroId))
                .as("total de ejemplares").isEqualTo(total);
        assertThat(restantes + prestados).as("restantes + prestados = total").isEqualTo(total);
    }

    /** Invariante global sobre los fixtures de este test (por sufijo unico). */
    private void assertNingunLibroActivoBajoPadreInactivo() {
        Long violaciones = jdbc.queryForObject(
                "SELECT COUNT(*) FROM libro l "
                + "JOIN autor a ON a.id = l.autor_id "
                + "JOIN editorial e ON e.id = l.editorial_id "
                + "WHERE l.alta = 1 AND (a.alta = 0 OR e.alta = 0) AND a.nombre LIKE ?",
                Long.class, PREFIJO + "%" + sufijo);
        assertThat(violaciones).as("libros activos bajo autor/editorial inactivo").isZero();
    }

    /** Resultado controlado: MiExcepcion (nunca cruda) con los fragmentos esperados. */
    private void assertMiExcepcion(Throwable error, String... fragmentos) {
        assertThat(error).as("el esperador debia terminar con MiExcepcion controlada").isInstanceOf(MiExcepcion.class);
        String mensaje = error.getMessage() == null ? "" : error.getMessage().toLowerCase();
        for (String fragmento : fragmentos) {
            assertThat(mensaje).contains(fragmento.toLowerCase());
        }
    }
}
