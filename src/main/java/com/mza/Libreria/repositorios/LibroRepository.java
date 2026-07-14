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
    @Query("SELECT p from Libro p WHERE p.titulo LIKE %?1% or p.autor.nombre LIKE %?1% or p.editorial.nombre LIKE %?1% AND p.alta = true")
    List<Libro> buscaTodoActivos(@Param("buscar") String buscar);

    @Query("SELECT p from Libro p WHERE p.titulo LIKE %?1% or p.autor.nombre LIKE %?1% or p.editorial.nombre LIKE %?1%")
    List<Libro> buscaTodo(@Param("buscar") String buscar);
    
    @Query("SELECT l from Libro l WHERE l.alta = true AND l.ejemplaresRestantes > 0")
    List<Libro> listaActivos(); 
    
    @Query("SELECT l FROM Libro l WHERE l.editorial.nombre = :nombre")
    List<Libro> listarPorEditorial(@Param("nombre")String editorial);
    
    @Query("SELECT l FROM Libro l WHERE l.autor.nombre = :nombre")
    List<Libro> listarPorAutor(@Param("nombre")String autor);
    
    @Query("SELECT l FROM Libro l WHERE l.titulo = :titulo")
    Libro buscarPorTitulo(@Param("titulo")String titulo);      
}
