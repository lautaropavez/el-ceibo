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
    // Envuelto para rechazar entradas de más de 72 bytes UTF-8 (CVE-2025-22228):
    // BCrypt ignora silenciosamente lo que exceda ese límite.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCrypt72BytePasswordEncoder(new BCryptPasswordEncoder());
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
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

                // Las reglas de rol de abajo con rutas exactas usan mvcMatchers: resuelven la
                // ruta igual que Spring MVC (incluida la barra final, p. ej. "/libro/lista/"),
                // de modo que no queden fuera de la regla y caigan en anyRequest().authenticated().
                // Las reglas públicas siguen con antMatchers: ante una variante no listada fallan
                // cerradas (exigen sesión).

                // --- Solo ADMIN: paneles de gestión completos (listas) ---
                .mvcMatchers(
                    "/libro/lista",
                    "/autor/lista",
                    "/editorial/lista"
                ).hasRole("ADMIN")

                // --- Solo ADMIN: mutaciones de catálogo (alta/baja/modif/eliminar/registro) ---
                .mvcMatchers(
                    "/libro/registro", "/libro/registrar",
                    "/libro/modificar/**", "/libro/baja/**", "/libro/alta/**", "/libro/eliminar/**"
                ).hasRole("ADMIN")
                .mvcMatchers(
                    "/autor/registroAutor", "/autor/registrarAutor",
                    "/autor/alta/**", "/autor/baja/**", "/autor/eliminar/**"
                ).hasRole("ADMIN")
                .mvcMatchers(
                    "/editorial/registroEditorial", "/editorial/registrarEditorial",
                    "/editorial/alta/**", "/editorial/baja/**", "/editorial/eliminar/**"
                ).hasRole("ADMIN")

                // --- Solo ADMIN: administracion de prestamos (P13.2D) ---
                // Debe evaluarse antes que la regla generica de abajo: Spring
                // Security usa la primera coincidencia, no la mas especifica.
                .mvcMatchers("/prestamo/lista", "/prestamo/devolver").hasRole("ADMIN")

                // --- Solo USUARIO: solicitud y consulta propia de prestamos (P13.2E) ---
                .mvcMatchers("/prestamo/solicitar", "/prestamo/mis-prestamos").hasRole("USUARIO")

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
