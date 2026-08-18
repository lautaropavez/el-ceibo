package com.mza.Libreria.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración central de Spring Security.
 * Reemplaza al login/autorización "de mentira" que no existía antes:
 * ahora cada request pasa por esta cadena de filtros antes de llegar
 * a los controllers.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // BCrypt hashea con salt random incluido en el propio hash, por eso
    // dos usuarios con la misma clave nunca generan el mismo valor guardado.
    // Se usa tanto acá (para validar login) como en ServiceUsuario (para guardar).
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // TODO revisar comentario antes de publicar:
            // CSRF deshabilitado a propósito. Los forms actuales (login.html,
            // registro.html, nuevoLibro.html, etc.) usan action="..." plano,
            // sin th:action ni token _csrf oculto. Con CSRF activo, todo POST
            // (incluido /logincheck y /registrar) devolvería 403 Forbidden.
            // Reactivar cuando los templates usen th:action + input _csrf.
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // --- Público: home, auth, registro, estáticos, consulta de catálogo ---
                .antMatchers(
                    "/",
                    "/login",
                    "/logincheck",
                    "/registro",
                    "/registrar",
                    "/css/**",
                    "/js/**",
                    "/img/**",
                    "/portada/libro/**",
                    "/libro/biblioteca",
                    "/error"
                ).permitAll()

                // --- Solo ADMIN: gestión de usuarios completa ---
                .antMatchers("/usuario/**").hasRole("ADMIN")

                // --- Solo ADMIN: paneles de gestión completos (listas) ---
                .antMatchers(
                    "/libro/lista",
                    "/autor/lista",
                    "/editorial/lista"
                ).hasRole("ADMIN")

                // --- Solo ADMIN: mutaciones de catálogo (alta/baja/modif/eliminar/registro) ---
                .antMatchers(
                    "/libro/registro", "/libro/registrar",
                    "/libro/modificar/**", "/libro/baja/**", "/libro/alta/**", "/libro/eliminar/**"
                ).hasRole("ADMIN")
                .antMatchers(
                    "/autor/registroAutor", "/autor/registrarAutor",
                    "/autor/alta/**", "/autor/baja/**", "/autor/eliminar/**"
                ).hasRole("ADMIN")
                .antMatchers(
                    "/editorial/registroEditorial", "/editorial/registrarEditorial",
                    "/editorial/alta/**", "/editorial/baja/**", "/editorial/eliminar/**"
                ).hasRole("ADMIN")

                // --- Solo ADMIN: administracion de prestamos (P13.2D) ---
                // Debe evaluarse antes que la regla generica de abajo: Spring
                // Security usa la primera coincidencia, no la mas especifica.
                .antMatchers("/prestamo/lista", "/prestamo/devolver").hasRole("ADMIN")

                // --- Cualquier usuario logueado (USUARIO o ADMIN) ---
                .antMatchers("/prestamo/**").authenticated()

                // --- Fallback: todo lo que no matcheó arriba requiere estar logueado ---
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/logincheck")
                .usernameParameter("username")
                .passwordParameter("password")
                .failureUrl("/login?error")
                .defaultSuccessUrl("/libro/biblioteca", false)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }
}
