package com.mza.Libreria.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mza.Libreria.entidades.Usuario;
import com.mza.Libreria.excepciones.MiExcepcion;
import com.mza.Libreria.repositorios.UsuarioRepository;
import com.mza.Libreria.support.LibreriaIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@LibreriaIntegrationTest
class ServiceUsuarioIntegrationTest {

    @Autowired
    private ServiceUsuario servicioUsuario;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Test
    void modificar_sinPasswordNuevaYManteniendoMail_conservaHash() throws MiExcepcion {
        String mail = "p143-hash-conservado@test.com";
        servicioUsuario.registrar("P143", "HashTest", mail, "password123", "password123");

        Usuario creado = usuarioRepo.buscarPorMail(mail);
        String hashOriginal = creado.getClave();

        servicioUsuario.modificar(creado.getId(), "P143 Modificado", "HashTestModificado", mail, "", "");

        Usuario modificado = usuarioRepo.findById(creado.getId())
                .orElseThrow(() -> new IllegalStateException("Usuario de fixture no encontrado"));
        assertThat(modificado.getNombre()).isEqualTo("P143 Modificado");
        assertThat(modificado.getApellido()).isEqualTo("HashTestModificado");
        assertThat(modificado.getClave()).isEqualTo(hashOriginal);
    }

    @Test
    void registrar_conMailDuplicado_rechazaYNoCreaOtroUsuario() throws MiExcepcion {
        String mail = "p143-mail-duplicado@test.com";
        servicioUsuario.registrar("P143", "Original", mail, "password123", "password123");

        assertThatThrownBy(() -> servicioUsuario.registrar("P143", "Intruso", mail, "password456", "password456"))
                .isInstanceOf(MiExcepcion.class);

        Usuario tras = usuarioRepo.buscarPorMail(mail);
        assertThat(tras.getNombre()).isEqualTo("P143");
        assertThat(tras.getApellido()).isEqualTo("Original");
    }

    @Test
    void modificar_conMailDeOtroUsuario_rechazaYSinModificarElUsuario() throws MiExcepcion {
        String mailA = "p143-mail-propio-a@test.com";
        String mailB = "p143-mail-propio-b@test.com";
        servicioUsuario.registrar("P143", "UsuarioA", mailA, "password123", "password123");
        servicioUsuario.registrar("P143", "UsuarioB", mailB, "password456", "password456");

        Usuario usuarioA = usuarioRepo.buscarPorMail(mailA);
        String hashOriginalA = usuarioA.getClave();

        assertThatThrownBy(() -> servicioUsuario.modificar(usuarioA.getId(), "P143 Intento", "Cambiado", mailB, "", ""))
                .isInstanceOf(MiExcepcion.class);

        Usuario usuarioARecargado = usuarioRepo.findById(usuarioA.getId())
                .orElseThrow(() -> new IllegalStateException("Usuario de fixture no encontrado"));
        assertThat(usuarioARecargado.getNombre()).isEqualTo("P143");
        assertThat(usuarioARecargado.getApellido()).isEqualTo("UsuarioA");
        assertThat(usuarioARecargado.getMail()).isEqualTo(mailA);
        assertThat(usuarioARecargado.getClave()).isEqualTo(hashOriginalA);
    }
}
