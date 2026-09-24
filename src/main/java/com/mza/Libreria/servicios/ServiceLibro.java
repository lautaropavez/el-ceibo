package com.mza.Libreria.servicios;

import com.mza.Libreria.entidades.Autor;
import com.mza.Libreria.entidades.Editorial;
import com.mza.Libreria.entidades.Libro;
import com.mza.Libreria.entidades.Portada;
import com.mza.Libreria.excepciones.MiExcepcion;
import com.mza.Libreria.repositorios.AutorRepository;
import com.mza.Libreria.repositorios.EditorialRepository;
import com.mza.Libreria.repositorios.LibroRepository;
import com.mza.Libreria.repositorios.PortadaRepository;
import com.mza.Libreria.repositorios.PrestamoRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Lautaro Pavez
 */
@Service
public class ServiceLibro {

    @Autowired
    private LibroRepository libroRepo;

    @Autowired
    private AutorRepository autorRepo;

    @Autowired
    private EditorialRepository editorialRepo;

    @Autowired
    private PrestamoRepository prestamoRepo;

    @Autowired
    private ServiceAutor sAutor;

    @Autowired
    private ServiceEditorial sEditorial;
    
    @Autowired
    private ServicePortada sPortada;

    @Autowired
    private PortadaRepository portadaRepo;

    @Transactional(rollbackFor = MiExcepcion.class)
    public void crearLibro(
            MultipartFile archivo,
            String titulo,
            Integer anio,
            String idAutor,
            String idEditorial,
            Integer nroEjemplares) throws MiExcepcion {

        String tituloNorm = normalizar(titulo);
        validarFormatoBasico(tituloNorm, anio);

        String idAutorNorm = normalizar(idAutor);
        if (idAutorNorm == null) {
            throw new MiExcepcion("Debe indicar el Autor");
        }
        String idEditorialNorm = normalizar(idEditorial);
        if (idEditorialNorm == null) {
            throw new MiExcepcion("Debe indicar la Editorial");
        }
        if (nroEjemplares == null || nroEjemplares <= 0) {
            throw new MiExcepcion("Debe indicar una cantidad de ejemplares mayor a 0");
        }
        if (nroEjemplares > 999) {
            throw new MiExcepcion("La cantidad de ejemplares no puede superar 999");
        }

        Autor autor = resolverAutorActivoPorId(idAutorNorm);
        Editorial editorial = resolverEditorialActivaPorId(idEditorialNorm);

        validarDuplicado(tituloNorm, autor.getId(), null);

        Libro lib = new Libro();
        lib.setTitulo(tituloNorm);
        lib.setAnio(anio);
        lib.setAlta(Boolean.TRUE);
        lib.setNroejemplares(nroEjemplares);
        lib.setEjemplaresPrestados(0);
        lib.setEjemplaresRestantes(nroEjemplares);
        lib.setAutor(autor);
        lib.setEditorial(editorial);

        Portada portada = sPortada.guardar(archivo); //En el caso de que yo no mande un archivo adjunto, no hay problema pq está contemplado y el método guardar me va a devolver null entonces el usuario va a quedar sin foto.
        lib.setPortada(portada);

        try {
            libroRepo.saveAndFlush(lib);
        } catch (DataIntegrityViolationException ex) {
            throw new MiExcepcion("Ya existe un libro con ese título y autor.");
        }
    }

    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = MiExcepcion.class
    )
    public void modificarLibro(MultipartFile archivo, String id, String titulo, Integer anio, String newNombreAutor, String newNombreEditorial) throws MiExcepcion {

        String tituloNorm = normalizar(titulo);
        validarFormatoBasico(tituloNorm, anio);

        String nombreAutorNorm = normalizar(newNombreAutor);
        if (nombreAutorNorm == null) {
            throw new MiExcepcion("Debe indicar el Autor");
        }
        String nombreEditorialNorm = normalizar(newNombreEditorial);
        if (nombreEditorialNorm == null) {
            throw new MiExcepcion("Debe indicar la Editorial");
        }

        // Lectura preliminar NO autoritativa: proyección escalar (no
        // entity-typed), solo para el 404 temprano. No carga ningún Libro
        // administrado en el Persistence Context. No decide "sin cambios":
        // esa decisión es exclusivamente autoritativa, bajo los tres locks.
        LibroRepository.EstadoLibroPreliminar estadoPreliminar = libroRepo.buscarEstadoPreliminarPorId(id);
        if (estadoPreliminar == null) {
            throw new MiExcepcion("No se encontró a este libro en la base de datos");
        }
        boolean hayArchivoNuevo = archivo != null && !archivo.isEmpty();

        try {
            // Orden canónico de locks: AUTOR -> EDITORIAL -> LIBRO.
            String nombreAutorMayus = nombreAutorNorm.toUpperCase();
            Autor autorExistente;
            String idAutorPreliminar = autorRepo.buscarIdPorNombre(nombreAutorMayus);
            if (idAutorPreliminar != null) {
                autorExistente = autorRepo.buscarYBloquearPorId(idAutorPreliminar);
                if (autorExistente == null) {
                    throw new MiExcepcion("El autor cambió durante la operación. Intente nuevamente.");
                }
                if (!Boolean.TRUE.equals(autorExistente.isAlta())) {
                    throw new MiExcepcion("El autor '" + autorExistente.getNombre() + "' existe pero está dado de baja. Debe reactivarlo antes de asignarlo a este libro.");
                }
                validarDuplicado(tituloNorm, autorExistente.getId(), id);
            } else {
                autorExistente = null;
            }

            String nombreEditorialMayus = nombreEditorialNorm.toUpperCase();
            Editorial editorialExistente;
            String idEditorialPreliminar = editorialRepo.buscarIdPorNombre(nombreEditorialMayus);
            if (idEditorialPreliminar != null) {
                editorialExistente = editorialRepo.buscarYBloquearPorId(idEditorialPreliminar);
                if (editorialExistente == null) {
                    throw new MiExcepcion("La editorial cambió durante la operación. Intente nuevamente.");
                }
                if (!Boolean.TRUE.equals(editorialExistente.isAlta())) {
                    throw new MiExcepcion("La editorial '" + editorialExistente.getNombre() + "' existe pero está dada de baja. Debe reactivarla antes de asignarla a este libro.");
                }
            } else {
                editorialExistente = null;
            }

            if (autorExistente == null) {
                sAutor.crearAutor(nombreAutorNorm);
                autorExistente = sAutor.buscarPorNombre(nombreAutorMayus);
            }
            if (editorialExistente == null) {
                sEditorial.crearEditorial(nombreEditorialNorm);
                editorialExistente = sEditorial.buscarporNombre(nombreEditorialMayus);
            }

            // Re-lock del Libro, al final, ya con Autor/Editorial bloqueados.
            Libro libro = libroRepo.buscarYBloquearPorId(id);
            if (libro == null) {
                throw new MiExcepcion("No se encontró a este libro en la base de datos");
            }

            // Única decisión de "sin cambios": autoritativa, contra el
            // Libro ya bloqueado.
            String autorActualNombre = libro.getAutor() != null ? libro.getAutor().getNombre() : null;
            String editorialActualNombre = libro.getEditorial() != null ? libro.getEditorial().getNombre() : null;
            boolean sinCambios = equalsIgnoreCaseNullSafe(libro.getTitulo(), tituloNorm)
                    && Objects.equals(libro.getAnio(), anio)
                    && equalsIgnoreCaseNullSafe(autorActualNombre, nombreAutorNorm)
                    && equalsIgnoreCaseNullSafe(editorialActualNombre, nombreEditorialNorm);
            if (sinCambios && !hayArchivoNuevo) {
                throw new MiExcepcion("No existen cambios para editar");
            }

            if (archivo != null && !archivo.isEmpty()) {
                Portada portadaFinal;
                if (libro.getPortada() == null) {
                    portadaFinal = sPortada.guardar(archivo);
                } else {
                    String idPortada = libro.getPortada().getId();
                    if (idPortada == null) {
                        throw new MiExcepcion("La portada actual del libro tiene un dato inconsistente y no puede reemplazarse.");
                    }
                    portadaRepo.buscarYBloquearPorId(idPortada)
                            .orElseThrow(() -> new MiExcepcion("La portada actual del libro tiene un dato inconsistente y no puede reemplazarse."));
                    long referencias = libroRepo.countByPortada_Id(idPortada);
                    if (referencias != 1) {
                        throw new MiExcepcion("La portada actual del libro tiene un dato inconsistente y no puede reemplazarse.");
                    }
                    portadaFinal = sPortada.actualizar(idPortada, archivo);
                }
                libro.setPortada(portadaFinal);
            }

            libro.setAutor(autorExistente);
            libro.setEditorial(editorialExistente);
            libro.setTitulo(tituloNorm);
            libro.setAnio(anio);

            try {
                libroRepo.saveAndFlush(libro);
            } catch (DataIntegrityViolationException ex) {
                throw new MiExcepcion("Ya existe un libro con ese título y autor.");
            }
        } catch (PessimisticLockingFailureException ex) {
            throw new MiExcepcion("El libro está siendo modificado por otra operación. Intente nuevamente.");
        }
    }
    
    //2 Formas de buscar por Id
    @Transactional(readOnly = true)
    public Libro buscarPorId(String id) {
        Optional<Libro> l = libroRepo.findById(id);
        if (l.isPresent()) {
            Libro lib = l.get();
            return lib;
        } else {
            return null; //throw new MiExcepcion("Este libro no existe."); esto debo ponerlo en el controlador
        }
    }
    @Transactional(readOnly = true)
    public Libro findbyId(String id) throws MiExcepcion{
        Libro l = libroRepo.buscarPorId(id);
        if (l != null) {
            return l;
        } else {
            throw new MiExcepcion("Este libro no existe.");

        }
    }

    @Transactional(readOnly = true) //Busca todos los libros activos e inactivos (para el administrador)
    public List<Libro> listarTodos() {
        return libroRepo.findAll();
    }
    
    @Transactional(readOnly = true) //Busca todos los libros que esten activos y tengan ejemplares > 0
    public List<Libro> listaActivos() {
        return libroRepo.listaActivos();
    }
    
    @Transactional(readOnly = true) //Busca todo, la variable buscar nos va a buscar ya sea libros editoriales o autores
    public List<Libro> listaBuscada(String buscar) {
        return libroRepo.buscaTodo(buscar);
    }

    @Transactional(readOnly = true) //Busca todo, la variable buscar nos va a buscar ya sea libros editoriales o autores que esten activos
    public List<Libro> listaBuscadaActivos(String buscar) {
        return libroRepo.buscaTodoActivos(buscar);
    }

    @Transactional(readOnly = true) 
    public List<Libro> listaporEditorial(String nombre) {
        return libroRepo.listarPorEditorial(nombre);
    }
    
    @Transactional(readOnly = true)
    public List<Libro> listaporAutor(String nombre) {
        return libroRepo.listarPorAutor(nombre);
    }
    
    @Transactional(rollbackFor = MiExcepcion.class)
    public Libro alta(String id) throws MiExcepcion {
        // Lectura preliminar NO autoritativa: proyección escalar (no
        // entity-typed) que solo descubre los ids de autor/editorial
        // actualmente asociados, sin cargar ningún Libro administrado en el
        // Persistence Context. Así, el buscarYBloquearPorId de más abajo es
        // la primera carga real de la entidad Libro en este método, y su
        // estado refleja genuinamente lo que el lock acaba de bloquear.
        LibroRepository.IdsPadresLibro idsPreliminares = libroRepo.buscarIdsPadresPorId(id);
        if (idsPreliminares == null) {
            throw new MiExcepcion("Este libro no existe.");
        }
        String idAutorPreliminar = idsPreliminares.getAutorId();
        String idEditorialPreliminar = idsPreliminares.getEditorialId();
        if (idAutorPreliminar == null) {
            throw new MiExcepcion("No se puede reactivar: el libro no tiene autor asignado.");
        }
        if (idEditorialPreliminar == null) {
            throw new MiExcepcion("No se puede reactivar: el libro no tiene editorial asignada.");
        }

        try {
            Autor autor = autorRepo.buscarYBloquearPorId(idAutorPreliminar);
            if (autor == null || !Boolean.TRUE.equals(autor.isAlta())) {
                throw new MiExcepcion("No se puede reactivar: el autor de este libro está dado de baja.");
            }
            Editorial editorial = editorialRepo.buscarYBloquearPorId(idEditorialPreliminar);
            if (editorial == null || !Boolean.TRUE.equals(editorial.isAlta())) {
                throw new MiExcepcion("No se puede reactivar: la editorial de este libro está dada de baja.");
            }

            Libro entidad = libroRepo.buscarYBloquearPorId(id);
            if (entidad == null) {
                throw new MiExcepcion("Este libro no existe.");
            }
            String idAutorActual = entidad.getAutor() != null ? entidad.getAutor().getId() : null;
            String idEditorialActual = entidad.getEditorial() != null ? entidad.getEditorial().getId() : null;
            if (!idAutorPreliminar.equals(idAutorActual) || !idEditorialPreliminar.equals(idEditorialActual)) {
                throw new MiExcepcion("El libro fue modificado durante la operación. Intente nuevamente.");
            }

            entidad.setAlta(true);
            return libroRepo.saveAndFlush(entidad);
        } catch (PessimisticLockingFailureException ex) {
            throw new MiExcepcion("El libro está siendo modificado por otra operación. Intente nuevamente.");
        }
    }

    @Transactional(rollbackFor = MiExcepcion.class)
    public Libro baja(String id) throws MiExcepcion {
        try {
            Libro entidad = libroRepo.buscarYBloquearPorId(id);
            if (entidad == null) {
                throw new MiExcepcion("Este libro no existe.");
            }
            entidad.setAlta(false);
            return libroRepo.saveAndFlush(entidad);
        } catch (PessimisticLockingFailureException ex) {
            throw new MiExcepcion("El libro está siendo modificado por otra operación. Intente nuevamente.");
        }
    }

    @Transactional(rollbackFor = MiExcepcion.class) //Damos de alta unicamente al autor; sus libros deben reactivarse por separado
    public Autor altaxAutor(String idAutor) throws MiExcepcion {
        try {
            Autor autor = autorRepo.buscarYBloquearPorId(idAutor);
            if (autor == null) {
                throw new MiExcepcion("No se encontró a este Autor en la base de datos");
            }
            autor.setAlta(true);
            return autorRepo.saveAndFlush(autor);
        } catch (PessimisticLockingFailureException ex) {
            throw new MiExcepcion("El autor está siendo modificado por otra operación. Intente nuevamente.");
        }
    }

    @Transactional(rollbackFor = MiExcepcion.class) //Damos de baja al autor y todos sus libros
    public Autor bajaxAutor(String idAutor) throws MiExcepcion {
        try {
            Autor autor = autorRepo.buscarYBloquearPorId(idAutor);
            if (autor == null) {
                throw new MiExcepcion("No se encontró a este Autor en la base de datos");
            }
            // Ids únicamente (proyección no entity-typed), ordenados ascendente
            // por la propia consulta: no cargan ningún Libro en el Persistence
            // Context, así que cada buscarYBloquearPorId de más abajo es la
            // primera carga real de esa entidad. Orden canónico de locks:
            // AUTOR -> LIBROS, para no invertir el orden usado por
            // crearLibro()/modificarLibro() y evitar deadlocks.
            List<String> idsLibrosOrdenados = libroRepo.buscarIdsPorAutorId(autor.getId());

            for (String idLibro : idsLibrosOrdenados) {
                Libro libroBloqueado = libroRepo.buscarYBloquearPorId(idLibro);
                if (libroBloqueado == null) {
                    throw new MiExcepcion("Este libro no existe.");
                }
                boolean sigueSiendoDelAutor = libroBloqueado.getAutor() != null
                        && autor.getId().equals(libroBloqueado.getAutor().getId());
                if (!sigueSiendoDelAutor) {
                    continue;
                }
                libroBloqueado.setAlta(false);
                libroRepo.save(libroBloqueado);
            }

            autor.setAlta(false);
            // saveAndFlush() descarga el Persistence Context completo (el
            // autor y los libros ya marcados arriba), no solo esta entidad:
            // un único flush alcanza para toda la unidad de persistencia de
            // este método. Prohibido un flush por cada libro del loop.
            return autorRepo.saveAndFlush(autor);
        } catch (PessimisticLockingFailureException ex) {
            throw new MiExcepcion("El autor está siendo modificado por otra operación. Intente nuevamente.");
        }
    }

    @Transactional(rollbackFor = MiExcepcion.class) //Damos de alta unicamente la editorial; sus libros deben reactivarse por separado
    public Editorial altaxEditorial(String idEditorial) throws MiExcepcion {
        try {
            Editorial editorial = editorialRepo.buscarYBloquearPorId(idEditorial);
            if (editorial == null) {
                throw new MiExcepcion("No se encontró a esta Editorial en la base de datos");
            }
            editorial.setAlta(true);
            return editorialRepo.saveAndFlush(editorial);
        } catch (PessimisticLockingFailureException ex) {
            throw new MiExcepcion("La editorial está siendo modificada por otra operación. Intente nuevamente.");
        }
    }

    @Transactional(rollbackFor = MiExcepcion.class) //Damos de baja la editorial y todos los libros
    public Editorial bajaxEditorial(String idAutor) throws MiExcepcion {
        try {
            Editorial editorial = editorialRepo.buscarYBloquearPorId(idAutor);
            if (editorial == null) {
                throw new MiExcepcion("No se encontró a esta Editorial en la base de datos");
            }
            // Ids únicamente (proyección no entity-typed), ordenados ascendente
            // por la propia consulta: mismo motivo y orden canónico que
            // bajaxAutor() (EDITORIAL -> LIBROS).
            List<String> idsLibrosOrdenados = libroRepo.buscarIdsPorEditorialId(editorial.getId());

            for (String idLibro : idsLibrosOrdenados) {
                Libro libroBloqueado = libroRepo.buscarYBloquearPorId(idLibro);
                if (libroBloqueado == null) {
                    throw new MiExcepcion("Este libro no existe.");
                }
                boolean sigueSiendoDeLaEditorial = libroBloqueado.getEditorial() != null
                        && editorial.getId().equals(libroBloqueado.getEditorial().getId());
                if (!sigueSiendoDeLaEditorial) {
                    continue;
                }
                libroBloqueado.setAlta(false);
                libroRepo.save(libroBloqueado);
            }

            editorial.setAlta(false);
            // saveAndFlush() descarga el Persistence Context completo (la
            // editorial y los libros ya marcados arriba), no solo esta
            // entidad: un único flush alcanza para toda la unidad de
            // persistencia de este método. Prohibido un flush por cada
            // libro del loop.
            return editorialRepo.saveAndFlush(editorial);
        } catch (PessimisticLockingFailureException ex) {
            throw new MiExcepcion("La editorial está siendo modificada por otra operación. Intente nuevamente.");
        }
    }
    
    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = MiExcepcion.class
    )
    public void eliminarLibro(String id) throws MiExcepcion {
        try {
            Libro l = libroRepo.buscarYBloquearPorId(id);
            if (l == null) {
                throw new MiExcepcion("No se encontró a este libro en la base de datos");
            }
            Portada portadaActual = l.getPortada();
            String portadaId = portadaActual != null ? portadaActual.getId() : null;
            Portada portadaBloqueada = null;
            if (portadaId != null) {
                portadaBloqueada = portadaRepo.buscarYBloquearPorId(portadaId).orElse(null);
            }
            if (prestamoRepo.existsByLibro_Id(id)) {
                throw new MiExcepcion("Este libro tiene préstamos registrados y no puede eliminarse físicamente. Use la baja.");
            }
            libroRepo.delete(l);
            libroRepo.flush();
            if (portadaBloqueada != null) {
                long referencias = libroRepo.countByPortada_Id(portadaId);
                if (referencias == 0) {
                    portadaRepo.delete(portadaBloqueada);
                    portadaRepo.flush();
                }
            }
        } catch (DataIntegrityViolationException ex) {
            throw new MiExcepcion("No se puede eliminar este libro porque tiene registros asociados.");
        } catch (PessimisticLockingFailureException ex) {
            throw new MiExcepcion("El libro está siendo modificado por otra operación. Intente nuevamente.");
        }
    }
    
    @Transactional(rollbackFor = MiExcepcion.class)
    public void eliminarAutor(String id) throws MiExcepcion {
        Autor a = autorRepo.buscarYBloquearPorId(id);
        if (a == null) {
            throw new MiExcepcion("No se encontró a este Autor en la base de datos");
        }
        List<Libro> librosxAutor = listaporAutor(a.getNombre());
        if (!librosxAutor.isEmpty()) {
            throw new MiExcepcion("No se puede eliminar este Autor porque tiene libros a su nombre, primero deberás eliminar los libros");
        }
        try {
            autorRepo.delete(a);
            autorRepo.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new MiExcepcion("No se puede eliminar este Autor porque tiene libros a su nombre, primero deberás eliminar los libros");
        }
    }
    @Transactional(rollbackFor = MiExcepcion.class)
    public void eliminarEditorial(String id) throws MiExcepcion {
        Editorial e = editorialRepo.buscarYBloquearPorId(id);
        if (e == null) {
            throw new MiExcepcion("No se encontró a esta Editorial en la base de datos");
        }
        List<Libro> librosxEdit = listaporEditorial(e.getNombre());
        if (!librosxEdit.isEmpty()) {
            throw new MiExcepcion("No se puede eliminar esta Editorial porque tiene libros a su nombre, primero deberas eliminar los libros");
        }
        try {
            editorialRepo.delete(e);
            editorialRepo.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new MiExcepcion("No se puede eliminar esta Editorial porque tiene libros a su nombre, primero deberas eliminar los libros");
        }
    }
    
    private String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private boolean equalsIgnoreCaseNullSafe(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.equalsIgnoreCase(b);
    }

    private void validarFormatoBasico(String tituloNorm, Integer anio) throws MiExcepcion {
        if (tituloNorm == null) {
            throw new MiExcepcion("Debe indicar el título");
        }
        if (anio == null || anio < 0 || anio > 2080) {
            throw new MiExcepcion("Debe indicar el año");
        }
    }

    private void validarDuplicado(String tituloNorm, String autorId, String idLibroActual) throws MiExcepcion {
        List<Libro> coincidencias = libroRepo.buscarPorTituloNormalizadoYAutor(tituloNorm, autorId);
        boolean hayBajaCoincidente = false;
        for (Libro coincidente : coincidencias) {
            if (idLibroActual != null && idLibroActual.equals(coincidente.getId())) {
                continue;
            }
            if (Boolean.TRUE.equals(coincidente.getAlta())) {
                throw new MiExcepcion("Ya existe un libro con ese título y autor.");
            }
            hayBajaCoincidente = true;
        }
        if (hayBajaCoincidente) {
            throw new MiExcepcion("Ya existe un libro con ese título y autor, pero está dado de baja. Podés reactivarlo en vez de crear uno nuevo.");
        }
    }

    private Autor resolverAutorActivoPorId(String idAutor) throws MiExcepcion {
        Autor autor = autorRepo.buscarYBloquearPorId(idAutor);
        if (autor == null) {
            throw new MiExcepcion("El autor seleccionado no existe");
        }
        if (!Boolean.TRUE.equals(autor.isAlta())) {
            throw new MiExcepcion("El autor seleccionado está dado de baja. Debe reactivarlo antes de asignarlo a un libro.");
        }
        return autor;
    }

    private Editorial resolverEditorialActivaPorId(String idEditorial) throws MiExcepcion {
        Editorial editorial = editorialRepo.buscarYBloquearPorId(idEditorial);
        if (editorial == null) {
            throw new MiExcepcion("La editorial seleccionada no existe");
        }
        if (!Boolean.TRUE.equals(editorial.isAlta())) {
            throw new MiExcepcion("La editorial seleccionada está dada de baja. Debe reactivarla antes de asignarla a un libro.");
        }
        return editorial;
    }

}
