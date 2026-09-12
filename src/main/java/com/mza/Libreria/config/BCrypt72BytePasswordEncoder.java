package com.mza.Libreria.config;

import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Envuelve un PasswordEncoder delegado (BCrypt) para cerrar
 * CVE-2025-22228: BCrypt solo usa los primeros 72 bytes UTF-8 de la
 * entrada, por lo que dos claves distintas que compartan ese prefijo
 * autentican contra el mismo hash. Este wrapper es un control
 * compensatorio mientras el proyecto siga en Spring Security 5.7.11:
 * no trunca, no prehashea, solo rechaza.
 */
public class BCrypt72BytePasswordEncoder implements PasswordEncoder {

    private static final int MAX_BYTES_UTF8 = 72;

    private final PasswordEncoder delegate;

    public BCrypt72BytePasswordEncoder(PasswordEncoder delegate) {
        this.delegate = delegate;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null || excedeLimite(rawPassword)) {
            throw new IllegalArgumentException(
                "La contraseña supera el límite de " + MAX_BYTES_UTF8 + " bytes UTF-8.");
        }
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || excedeLimite(rawPassword)) {
            return false;
        }
        return delegate.matches(rawPassword, encodedPassword);
    }

    private boolean excedeLimite(CharSequence rawPassword) {
        return rawPassword.toString().getBytes(StandardCharsets.UTF_8).length > MAX_BYTES_UTF8;
    }
}
