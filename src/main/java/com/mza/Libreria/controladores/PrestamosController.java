package com.mza.Libreria.controladores;

import com.mza.Libreria.entidades.Prestamo;
import com.mza.Libreria.excepciones.MiExcepcion;
import com.mza.Libreria.servicios.ServicePrestamo;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
/**
 *
 * @author Lautaro Pavez
 */
@Controller
@RequestMapping("/prestamo")
public class PrestamosController {

    @Autowired
    private ServicePrestamo servPrestamo;

    // La identidad se deriva unicamente de Authentication.getName() (el mail
    // de sesion); nunca se acepta un usuarioId de request/formulario.
    @PostMapping("/solicitar")
    public String solicitar(
            @RequestParam String libroId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            servPrestamo.solicitar(authentication.getName(), libroId);
            redirectAttributes.addFlashAttribute("exito", "¡Préstamo solicitado con éxito!");
            return "redirect:/prestamo/mis-prestamos";
        } catch (MiExcepcion ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/libro/biblioteca";
        }
    }

    @GetMapping("/mis-prestamos")
    public String misPrestamos(ModelMap modelo, Authentication authentication) {
        try {
            List<Prestamo> prestamos = servPrestamo.misPrestamos(authentication.getName());
            modelo.addAttribute("prestamos", prestamos);
        } catch (MiExcepcion ex) {
            modelo.put("error", ex.getMessage());
            modelo.addAttribute("prestamos", Collections.emptyList());
        }
        return "mis-prestamos";
    }
}
