package com.mza.Libreria.repositorios;

import com.mza.Libreria.entidades.Libro;
import java.util.List;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Lautaro Pavez
 */
@Repository
public interface LibroRepository  extends JpaRepository<Libro, String>{

    // Proyección no entity-typed: al no devolver un Libro administrado,
    // no entra al Persistence Context. Sirve para lecturas preliminares
    // no autoritativas (descubrir ids de padres) sin dejar cacheada una
    // instancia stale que interfiera con un buscarYBloquearPorId posterior
    // sobre el mismo id (Hibernate reutilizaría esa instancia cacheada en
    // vez de reflejar el estado recién bloqueado).
    interface IdsPadresLibro {
        String getLibroId();
        String getAutorId();
        String getEditorialId();
    }

    @Query("SELECT l.id AS libroId, a.id AS autorId, e.id AS editorialId "
            + "FROM Libro l LEFT JOIN l.autor a LEFT JOIN l.editorial e "
            + "WHERE l.id = :id")
    IdsPadresLibro buscarIdsPadresPorId(@Param("id") String id);

    // Ids únicamente, ordenados ascendente por la propia consulta: no
    // devuelven entidades Libro, así que tampoco entran al Persistence
    // Context antes del lock correspondiente en el loop que las consume.
    @Query("SELECT l.id FROM Libro l WHERE l.autor.id = :idAutor ORDER BY l.id ASC")
    List<String> buscarIdsPorAutorId(@Param("idAutor") String idAutor);

    @Query("SELECT l.id FROM Libro l WHERE l.editorial.id = :idEditorial ORDER BY l.id ASC")
    List<String> buscarIdsPorEditorialId(@Param("idEditorial") String idEditorial);

    // Proyección no entity-typed del estado actual de un Libro (título,
    // año, nombre de autor/editorial vía LEFT JOIN). Sirve únicamente
    // para el 404 preliminar no autoritativo de existencia en
    // modificarLibro(), sin cargar el Libro como entidad administrada
    // antes de su buscarYBloquearPorId. No decide "sin cambios": esa
    // verificación es exclusivamente autoritativa, bajo los tres locks
    // (ver el comentario en ServiceLibro.modificarLibro()).
    interface EstadoLibroPreliminar {
        String getTitulo();
        Integer getAnio();
        String getNombreAutor();
        String getNombreEditorial();
    }

    @Query("SELECT l.titulo AS titulo, l.anio AS anio, "
            + "a.nombre AS nombreAutor, e.nombre AS nombreEditorial "
            + "FROM Libro l LEFT JOIN l.autor a LEFT JOIN l.editorial e "
            + "WHERE l.id = :id")
    EstadoLibroPreliminar buscarEstadoPreliminarPorId(@Param("id") String id);

    @Query("SELECT l FROM Libro l WHERE l.id = :id")
    Libro buscarPorId(@Param("id")String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Libro l WHERE l.id = :id")
    Libro buscarYBloquearPorId(@Param("id") String id);

    // con esta query se obtiene contenido parecido a, LIKE %?1% remplaza a LIKE :variable
    @Query("SELECT p FROM Libro p "
            + "WHERE p.alta = true "
            + "AND ("
            + "    LOWER(p.titulo) LIKE LOWER(CONCAT('%', :buscar, '%')) "
            + "    OR LOWER(p.autor.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) "
            + "    OR LOWER(p.editorial.nombre) LIKE LOWER(CONCAT('%', :buscar, '%'))"
            + ")")
    List<Libro> buscaTodoActivos(@Param("buscar") String buscar);

    @Query("SELECT p from Libro p WHERE p.titulo LIKE %?1% or p.autor.nombre LIKE %?1% or p.editorial.nombre LIKE %?1%")
    List<Libro> buscaTodo(@Param("buscar") String buscar);
    
    @Query("SELECT l from Libro l WHERE l.alta = true")
    List<Libro> listaActivos();
    
    @Query("SELECT l FROM Libro l WHERE l.editorial.nombre = :nombre")
    List<Libro> listarPorEditorial(@Param("nombre")String editorial);
    
    @Query("SELECT l FROM Libro l WHERE l.autor.nombre = :nombre")
    List<Libro> listarPorAutor(@Param("nombre")String autor);
    
    @Query("SELECT l FROM Libro l WHERE UPPER(TRIM(l.titulo)) = UPPER(TRIM(:titulo)) AND l.autor.id = :autorId")
    List<Libro> buscarPorTituloNormalizadoYAutor(@Param("titulo") String titulo, @Param("autorId") String autorId);

    long countByPortada_Id(String portadaId);
}
