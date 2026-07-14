package com.mza.Libreria.repositorios;

import com.mza.Libreria.entidades.Usuario;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Lautaro Pavez
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, String>{
    
    @Query("SELECT u FROM Usuario u WHERE u.mail = :mail")
    Usuario buscarPorMail(@Param("mail") String mail);

    @Query("SELECT u FROM Usuario u WHERE u.mail = :mail")
    Optional<Usuario> validaMail(@Param("mail") String mail);
    
    @Query("SELECT u FROM Usuario u WHERE u.id = :id")
    Usuario buscarPorId(@Param("id")String id);
    
    @Query("SELECT a FROM Usuario a WHERE a.cantPrestamos = :cantPrestamos ")
    List<Usuario> buscaPrestamos(@Param("cantPrestamos") Integer cantPrestamos);
    
    @Query("SELECT a FROM Usuario a WHERE a.baja = NULL")
    List<Usuario> listaActivos(@Param("baja") Date baja); 

    //@Query("SELECT a FROM Usuario a WHERE a.baja = null")
    //List<Usuario> buscaActivos();
}
