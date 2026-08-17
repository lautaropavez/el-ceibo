/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mza.Libreria.repositorios;

import com.mza.Libreria.entidades.Prestamo;
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
public interface PrestamoRepository extends JpaRepository<Prestamo, String>{

    boolean existsByLibro_Id(String libroId);

    boolean existsByUsuario_Id(String usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Prestamo p WHERE p.id = :id")
    Prestamo buscarYBloquearPorId(@Param("id") String id);

    long countByUsuario_IdAndFechaDevolucionIsNull(String usuarioId);

    boolean existsByUsuario_IdAndLibro_IdAndFechaDevolucionIsNull(String usuarioId, String libroId);

    List<Prestamo> findByUsuario_Id(String usuarioId);

}
