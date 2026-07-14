package com.mza.Libreria.repositorios;

import com.mza.Libreria.entidades.Autor;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Lautaro Pavez
 */
@Repository
public interface AutorRepository extends JpaRepository<Autor, String>{
 
    @Query("SELECT a FROM Autor a WHERE a.id = :id")
    Autor buscarPorId(@Param("id")String id);
    
    @Query("SELECT a FROM Autor a WHERE a.nombre = :nombre")
    Autor buscarPorNombre(@Param("nombre")String nombre);

    @Query("SELECT a FROM Autor a WHERE a.alta = true")
    List<Autor> buscaActivos();
    
    //-------------------------------NO USADOS--------------------------------------   

    /*
        @Query("SELECT a FROM Autor a WHERE a.nombre = :nombre")
        List<Autor> listarPorNombre(@Param("nombre")String nombre);

        @Query("SELECT p FROM Persona p WHERE p.dni = :dni")
        Persona buscarPorDNI(@Param("dni")Long dni);

        @Query("SELECT a FROM Autor a WHERE a.nombre LIKE %?1%")
        List<Autor> buscaPorNombre(@Param("nombre") String nombre);

        @Query ("SELECT a FROM Autor a")
        List<Autor> buscaAutor();
    */
}