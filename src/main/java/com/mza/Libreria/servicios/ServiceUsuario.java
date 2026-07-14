
package com.mza.Libreria.servicios;

import com.mza.Libreria.entidades.Usuario;
import com.mza.Libreria.enumeradores.Rol;
import com.mza.Libreria.excepciones.MiExcepcion;
import com.mza.Libreria.repositorios.UsuarioRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
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
    private ServiceNotificacion sNotific;
    
    @Transactional
    public void registrar(String nombre,String apellido,String mail,String clave, String clave2) throws MiExcepcion{
   
        validacion(nombre,apellido,mail,clave,clave2);
        
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setMail(mail);
        usuario.setRol(Rol.USUARIO);
        usuario.setClave(clave);
        
        usuario.setAlta(new Date());
        usuario.setBaja(null);
        
        usuarioRepo.save(usuario);
    }
    
    @Transactional
    public void modificar(String id,String nombre,String apellido,String mail,String clave,String clave2) throws MiExcepcion{
        
        validacion(nombre,apellido,mail,clave,clave2);
        
        Optional<Usuario> respuesta = usuarioRepo.findById(id); //Este método nos devuelve una clase Optional como respuesta 
        if(respuesta.isPresent()){ //Y nos dice si el resultado está presente, osea si encontró un resultado con este id
            Usuario usuario = respuesta.get();
            usuario.setNombre(nombre);
            usuario.setApellido(apellido);
            usuario.setMail(mail);
            usuario.setClave(clave);

            
            usuarioRepo.save(usuario);
        }else{
         throw new MiExcepcion("No se encontró el usuario ingresado");   
        }  
    }
    
    @Transactional(readOnly = true)
    public Usuario buscarPorId(String id){
        Usuario u = usuarioRepo.buscarPorId(id);
        return usuarioRepo.buscarPorId(id);
    }
    
    
    @Transactional
    public void deshabilitar(String id)throws MiExcepcion{
        
        Optional<Usuario> respuesta = usuarioRepo.findById(id);
        if(respuesta.isPresent()){ 
            Usuario usuario = respuesta.get();
            usuario.setBaja(new Date());
            usuarioRepo.save(usuario);
        }else{
         throw new MiExcepcion("No se encontró el usuario ingresado");   
        }  
    }
    
    @Transactional
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
    
    @Transactional
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
    
    @Transactional
    public void eliminar(String id)throws Exception{
        Usuario u = usuarioRepo.buscarPorId(id);
        if (u != null) {
            usuarioRepo.deleteById(id);
        } else {
            throw new Exception("No se encontró a este usuario en la base de datos");
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
}
