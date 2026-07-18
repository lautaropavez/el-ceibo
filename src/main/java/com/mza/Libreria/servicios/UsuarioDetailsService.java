package com.mza.Libreria.servicios;

import com.mza.Libreria.entidades.Usuario;
import com.mza.Libreria.repositorios.UsuarioRepository;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Puente entre Spring Security y la tabla `usuario`.
 * Spring Security invoca loadUserByUsername() en cada intento de login
 * contra /logincheck (ver SecurityConfig), pasándole lo que llega en el
 * parámetro "username" del form (acá es el mail, no un username separado).
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Override
    public UserDetails loadUserByUsername(String mail) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepo.buscarPorMail(mail);
        if (usuario == null) {
            throw new UsernameNotFoundException("No existe un usuario con ese mail");
        }

        // El rol de nuestra entidad (ADMIN/USUARIO) se traduce a authority con
        // prefijo "ROLE_" porque es lo que espera hasRole("ADMIN") en SecurityConfig.
        // "enabled=false" cuando el usuario tiene baja cargada evita que pueda loguearse
        // aunque conozca su clave.
        return new User(
                usuario.getMail(),
                usuario.getClave(),
                usuario.getBaja() == null,
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()))
        );
    }
}
