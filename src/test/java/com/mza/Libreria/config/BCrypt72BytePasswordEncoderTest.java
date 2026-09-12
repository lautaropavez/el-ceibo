package com.mza.Libreria.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class BCrypt72BytePasswordEncoderTest {

    private final BCrypt72BytePasswordEncoder encoder =
            new BCrypt72BytePasswordEncoder(new BCryptPasswordEncoder());

    @Test
    void encode_conExactamente72BytesUtf8_aceptaYPermiteAutenticar() {
        String clave72Bytes = repetir('a', 72);

        String hash = encoder.encode(clave72Bytes);

        assertThat(encoder.matches(clave72Bytes, hash)).isTrue();
    }

    @Test
    void encode_con73BytesUtf8_rechaza() {
        String clave73Bytes = repetir('a', 73);

        assertThatThrownBy(() -> encoder.encode(clave73Bytes))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void encode_unicodeConMenosDe72CaracteresPeroMasDe72Bytes_rechaza() {
        // 'ñ' ocupa 2 bytes en UTF-8: 40 caracteres -> 80 bytes, pero
        // clave.length() (chars Java) da 40, muy por debajo de 72.
        String claveUnicode = repetir('ñ', 40);
        assertThat(claveUnicode.length()).isLessThan(72);

        assertThatThrownBy(() -> encoder.encode(claveUnicode))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void matches_conEntradaSobredimensionada_noDelegaEnElEncoderReal() {
        PasswordEncoder delegateMock = mock(PasswordEncoder.class);
        when(delegateMock.matches(any(), any())).thenReturn(true);
        BCrypt72BytePasswordEncoder encoderConMock = new BCrypt72BytePasswordEncoder(delegateMock);

        boolean resultado = encoderConMock.matches(repetir('a', 73), "cualquier-hash");

        assertThat(resultado).isFalse();
        verify(delegateMock, never()).matches(any(), any());
    }

    @Test
    void matches_conCandidatoMayorA72BytesQueComparteElPrefijoValido_esRechazadoPorElWrapper() {
        String clave72Bytes = repetir('a', 72);
        String hash = encoder.encode(clave72Bytes);

        String claveConSufijo = clave72Bytes + "X";

        assertThat(encoder.matches(claveConSufijo, hash)).isFalse();
    }

    @Test
    void encode_conCaracterUtf8De4BytesCercaDelLimite_respetaElLimiteExactoDeBytes() {
        // U+1F600 ("😀") es un carácter fuera del BMP: 2 chars Java
        // (surrogate pair) y 4 bytes en UTF-8.
        String emoji = "😀";

        String clave72Bytes = repetir('a', 68) + emoji; // 68 + 4 = 72 bytes
        String clave73Bytes = repetir('a', 69) + emoji; // 69 + 4 = 73 bytes

        String hash = encoder.encode(clave72Bytes);
        assertThat(encoder.matches(clave72Bytes, hash)).isTrue();

        assertThatThrownBy(() -> encoder.encode(clave73Bytes))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static String repetir(char c, int veces) {
        StringBuilder sb = new StringBuilder(veces);
        for (int i = 0; i < veces; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
