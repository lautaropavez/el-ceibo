package com.mza.Libreria.repositorios;

import com.mza.Libreria.entidades.Libro;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Lautaro Pavez
 */
@Repository
public interface LibroRepository  extends JpaRepository<Libro, String>{

    @Query("SELECT l FROM Libro l WHERE l.id = :id")
    Libro buscarPorId(@Param("id")String id);

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
