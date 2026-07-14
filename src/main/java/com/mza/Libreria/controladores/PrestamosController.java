package com.mza.Libreria.controladores;

import com.mza.Libreria.servicios.ServicePrestamo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
/**
 *
 * @author Lautaro Pavez
 */
@Controller 
@RequestMapping("/prestamo") 
public class PrestamosController {
    
    @Autowired
    private ServicePrestamo servPrestamo; 
}
