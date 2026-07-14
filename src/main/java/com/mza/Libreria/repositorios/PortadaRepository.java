package com.mza.Libreria.repositorios;

import com.mza.Libreria.entidades.Portada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Lautaro Pavez
 */
@Repository
public interface PortadaRepository extends JpaRepository<Portada, String> {
    
}
