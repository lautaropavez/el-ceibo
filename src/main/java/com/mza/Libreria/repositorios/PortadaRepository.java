package com.mza.Libreria.repositorios;

import com.mza.Libreria.entidades.Portada;
import java.util.Optional;
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
public interface PortadaRepository extends JpaRepository<Portada, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Portada p WHERE p.id = :id")
    Optional<Portada> buscarYBloquearPorId(@Param("id") String id);

}
