
package com.mza.Libreria.servicios;

import com.mza.Libreria.entidades.Libro;
import com.mza.Libreria.entidades.Prestamo;
import com.mza.Libreria.entidades.Usuario;
import com.mza.Libreria.excepciones.MiExcepcion;
import com.mza.Libreria.repositorios.LibroRepository;
import com.mza.Libreria.repositorios.PrestamoRepository;
import com.mza.Libreria.repositorios.UsuarioRepository;
import java.util.Calendar;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Lautaro Pavez
 */
@Service
public class ServicePrestamo {

    private static final int MAX_PRESTAMOS_ACTIVOS = 3;
    private static final int DIAS_VENCIMIENTO = 14;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private LibroRepository libroRepo;

    @Autowired
    private PrestamoRepository prestamoRepo;

    // mailUsuarioAutenticado debe venir siempre de Authentication (resuelto
    // por el controller), nunca de un usuarioId de request/formulario.
    // Orden de locks obligatorio: USUARIO -> LIBRO (ver SPEC P13 secciones
    // 6 y 10), para evitar deadlocks entre solicitudes concurrentes.
    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = MiExcepcion.class
    )
    public Prestamo solicitar(String mailUsuarioAutenticado, String libroId) throws MiExcepcion {
        try {
            Usuario usuarioSesion = usuarioRepo.buscarPorMail(mailUsuarioAutenticado);
            if (usuarioSesion == null) {
                throw new MiExcepcion("No se encontró al usuario autenticado.");
            }

            Usuario usuario = usuarioRepo.buscarYBloquearPorId(usuarioSesion.getId());
            if (usuario == null) {
                throw new MiExcepcion("No se encontró al usuario autenticado.");
            }
            if (usuario.getBaja() != null) {
                throw new MiExcepcion("Un usuario dado de baja no puede solicitar préstamos.");
            }

            long prestamosActivos = prestamoRepo.countByUsuario_IdAndFechaDevolucionIsNull(usuario.getId());
            if (prestamosActivos >= MAX_PRESTAMOS_ACTIVOS) {
                throw new MiExcepcion("Ya tenés " + MAX_PRESTAMOS_ACTIVOS + " préstamos activos, no podés solicitar otro.");
            }

            Libro libro = libroRepo.buscarYBloquearPorId(libroId);
            if (libro == null) {
                throw new MiExcepcion("El libro solicitado no existe.");
            }
            if (!Boolean.TRUE.equals(libro.getAlta())) {
                throw new MiExcepcion("El libro solicitado está dado de baja.");
            }
            if (libro.getEjemplaresRestantes() == null || libro.getEjemplaresRestantes() <= 0) {
                throw new MiExcepcion("No hay ejemplares disponibles de este libro.");
            }
            if (libro.getEjemplaresPrestados() == null) {
                throw new MiExcepcion("El stock de este libro es inconsistente y no puede prestarse. Contactá a un administrador.");
            }
            if (prestamoRepo.existsByUsuario_IdAndLibro_IdAndFechaDevolucionIsNull(usuario.getId(), libro.getId())) {
                throw new MiExcepcion("Ya tenés un préstamo activo de este libro.");
            }

            Date fechaPrestamo = new Date();
            Date fechaVencimiento = sumarDias(fechaPrestamo, DIAS_VENCIMIENTO);

            Prestamo prestamo = new Prestamo();
            prestamo.setUsuario(usuario);
            prestamo.setLibro(libro);
            prestamo.setFechaPrestamo(fechaPrestamo);
            prestamo.setFechaVencimiento(fechaVencimiento);
            prestamo.setFechaDevolucion(null);

            libro.setEjemplaresRestantes(libro.getEjemplaresRestantes() - 1);
            libro.setEjemplaresPrestados(libro.getEjemplaresPrestados() + 1);
            libroRepo.save(libro);

            return prestamoRepo.save(prestamo);
        } catch (PessimisticLockingFailureException ex) {
            throw new MiExcepcion("El usuario o el libro están siendo modificados por otra operación. Intente nuevamente.");
        }
    }

    private Date sumarDias(Date fecha, int dias) {
        Calendar calendario = Calendar.getInstance();
        calendario.setTime(fecha);
        calendario.add(Calendar.DAY_OF_MONTH, dias);
        return calendario.getTime();
    }

}
