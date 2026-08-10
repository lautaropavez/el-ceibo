
package com.mza.Libreria.servicios;

import com.mza.Libreria.entidades.Usuario;
import com.mza.Libreria.enumeradores.Rol;
import com.mza.Libreria.excepciones.MiExcepcion;
import com.mza.Libreria.repositorios.PrestamoRepository;
import com.mza.Libreria.repositorios.UsuarioRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 *
 * @author Lautaro Pavez
 */
@Service
public class ServiceUsuario {
    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private PrestamoRepository prestamoRepo;

    @Autowired
    private ServiceNotificacion sNotific;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional(rollbackFor = MiExcepcion.class)
    public void registrar(String nombre,String apellido,String mail,String clave, String clave2) throws MiExcepcion{

        String mailTrim = mail == null ? null : mail.trim();

        validacion(nombre,apellido,mailTrim,clave,clave2);
        validarMailDisponible(mailTrim, null);

        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setMail(mailTrim);
        usuario.setRol(Rol.USUARIO);
        usuario.setClave(passwordEncoder.encode(clave));

        usuario.setAlta(new Date());
        usuario.setBaja(null);

        try {
            usuarioRepo.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException ex) {
            throw new MiExcepcion("No se pudo guardar el usuario porque el correo electrónico ingresado ya está en uso o entra en conflicto con un registro existente. Verificá los datos e intentá nuevamente.");
        }
    }
    
    @Transactional(rollbackFor = MiExcepcion.class)
    public void modificar(String id, String nombre, String apellido, String mail, String clave, String clave2) throws MiExcepcion {

        String mailTrim = mail == null ? null : mail.trim();

        validacionDatosBasicos(nombre, apellido, mailTrim);
        validarMailDisponible(mailTrim, id);

        boolean claveVacia = (clave == null || clave.trim().isEmpty());
        boolean clave2Vacia = (clave2 == null || clave2.trim().isEmpty());

        if (!claveVacia && clave2Vacia) {
            throw new MiExcepcion("Debe repetir la nueva clave");
        }
        if (claveVacia && !clave2Vacia) {
            throw new MiExcepcion("Debe ingresar la nueva clave");
        }

        Optional<Usuario> respuesta = usuarioRepo.findById(id);
        if (respuesta.isPresent()) {
            Usuario usuario = respuesta.get();
            usuario.setNombre(nombre);
            usuario.setApellido(apellido);
            usuario.setMail(mailTrim);

            if (!claveVacia) {
                if (clave.length() < 8) {
                    throw new MiExcepcion("La clave no puede tener menos de 8 caracteres");
                }
                if (!clave.equals(clave2)) {
                    throw new MiExcepcion("Las claves deben ser iguales");
                }
                usuario.setClave(passwordEncoder.encode(clave));
            }

            try {
                usuarioRepo.saveAndFlush(usuario);
            } catch (DataIntegrityViolationException ex) {
                throw new MiExcepcion("No se pudo guardar el usuario porque el correo electrónico ingresado ya está en uso o entra en conflicto con un registro existente. Verificá los datos e intentá nuevamente.");
            }
        } else {
            throw new MiExcepcion("No se encontró el usuario ingresado");
        }
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorId(String id){
        Usuario u = usuarioRepo.buscarPorId(id);
        return usuarioRepo.buscarPorId(id);
    }
    
    
    @Transactional(rollbackFor = MiExcepcion.class)
    public void deshabilitar(String id)throws MiExcepcion{

        Optional<Usuario> respuesta = usuarioRepo.findById(id);
        if(!respuesta.isPresent()){
            throw new MiExcepcion("No se encontró el usuario ingresado");
        }
        Usuario usuario = respuesta.get();
        if (usuario.getBaja() != null) {
            return; // ya estaba de baja: no pisamos la fecha original
        }
        if (Rol.ADMIN.equals(usuario.getRol()) && usuarioRepo.countByRolAndBajaIsNull(Rol.ADMIN) <= 1) {
            throw new MiExcepcion("No se puede dar de baja al último administrador activo del sistema.");
        }
        usuario.setBaja(new Date());
        usuarioRepo.save(usuario);
    }
    
    @Transactional(rollbackFor = MiExcepcion.class)
    public void habilitar(String id)throws MiExcepcion{
        
        Optional<Usuario> respuesta = usuarioRepo.findById(id);
        if(respuesta.isPresent()){
            Usuario usuario = respuesta.get();
            usuario.setBaja(null);          
            usuarioRepo.save(usuario);    
        }else{
         throw new MiExcepcion("No se encontró el usuario ingresado");   
        }
    }
    
    @Transactional(rollbackFor = MiExcepcion.class)
    public void cambiarRol(String id)throws MiExcepcion{
        Optional<Usuario> respuesta = usuarioRepo.findById(id);

        if (respuesta.isPresent()) {
            Usuario usuario = respuesta.get();

            if (usuario.getRol().equals(Rol.USUARIO)) {
                usuario.setRol(Rol.ADMIN);
            } else if (usuario.getRol().equals(Rol.ADMIN)) {
                usuario.setRol(Rol.USUARIO);
            }
        }
    }
    
    @Transactional(rollbackFor = MiExcepcion.class)
    public void eliminar(String id)throws MiExcepcion{
        Usuario u = usuarioRepo.buscarPorId(id);
        if (u == null) {
            throw new MiExcepcion("No se encontró a este usuario en la base de datos");
        }
        if (prestamoRepo.existsByUsuario_Id(id)) {
            throw new MiExcepcion("Este usuario tiene préstamos registrados y no puede eliminarse físicamente. Use la baja.");
        }
        if (Rol.ADMIN.equals(u.getRol()) && u.getBaja() == null
                && usuarioRepo.countByRolAndBajaIsNull(Rol.ADMIN) <= 1) {
            throw new MiExcepcion("No se puede eliminar al último administrador activo del sistema.");
        }
        try {
            usuarioRepo.delete(u);
            usuarioRepo.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new MiExcepcion("No se puede eliminar este usuario porque tiene registros asociados.");
        }
    }
    
    @Transactional(readOnly = true) //Busca todos los usuarios activos e inactivos (para el administrador)
    public List<Usuario> listarTodos() {
        return usuarioRepo.findAll();
    }
    
    @Transactional(readOnly = true) //Busca todos los usuarios que esten activos, es distinto a listaActivos de libro porque en libro solo pusimos un boolean activo, acá dos fechas de activo e inactivo
    public List<Usuario> listaActivos(Date baja) {
        return usuarioRepo.listaActivos(baja);
    }
    public void validacion(String nombre,String apellido,String mail,String clave1,String clave2) throws MiExcepcion {

        if (nombre == null || nombre.isEmpty()) {
            throw new MiExcepcion("Debe indicar el nombre");
        }
        if (apellido == null || apellido.isEmpty()) {
            throw new MiExcepcion("Debe indicar el apellido");
        }
        if (mail == null || mail.isEmpty()) {
            throw new MiExcepcion("Debe indicar el correo electrónico");
        }
        if (mail.contains("@") == false) {
                throw new MiExcepcion("El correo electrónico es incorrecto");
        }
        if (clave1 == null || clave1.trim().isEmpty()) {
            throw new MiExcepcion("Debe indicar la clave");
        }
        if (clave1.length() < 8) {
            throw new MiExcepcion("La clave no puede tener menos de 8 caracteres");
        }
        
        if(!clave1.equals(clave2)) {
            throw new MiExcepcion("Las claves deben ser iguales");
        }
    }

    private void validarMailDisponible(String mail, String idActual) throws MiExcepcion {
        Usuario existente = usuarioRepo.buscarPorMail(mail);
        if (existente != null && !Objects.equals(existente.getId(), idActual)) {
            throw new MiExcepcion("Ese correo electrónico ya está en uso.");
        }
    }

    private void validacionDatosBasicos(String nombre, String apellido, String mail) throws MiExcepcion {
        if (nombre == null || nombre.isEmpty()) {
            throw new MiExcepcion("Debe indicar el nombre");
        }
        if (apellido == null || apellido.isEmpty()) {
            throw new MiExcepcion("Debe indicar el apellido");
        }
        if (mail == null || mail.isEmpty()) {
            throw new MiExcepcion("Debe indicar el correo electrónico");
        }
        if (mail.contains("@") == false) {
            throw new MiExcepcion("El correo electrónico es incorrecto");
        }
    }
}
