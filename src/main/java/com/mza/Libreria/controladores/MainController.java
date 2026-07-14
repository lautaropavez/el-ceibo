package com.mza.Libreria.controladores;

import com.mza.Libreria.excepciones.MiExcepcion;
import com.mza.Libreria.servicios.ServiceUsuario;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
/**
 *
 * @author Lautaro Pavez
 */
@Controller
@RequestMapping("/") 
public class MainController {

    @Autowired
    private ServiceUsuario servUsuario;
    
    @GetMapping("/")
    public String index(){
        return "index.html";
    }
    
    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, ModelMap modelo){
        if(error != null){
            modelo.put("error","Nombre de usuario o clave incorrectos");
        }
        return "login.html";
    }
    @GetMapping("/registro")
    public String registro(){
        return "registro.html";
    }
    
    @PostMapping("/registrar")
    public String registrar(ModelMap modelo,@RequestParam String nombre,@RequestParam String apellido,@RequestParam String mail,@RequestParam String clave1,@RequestParam String clave2){
        try {
            servUsuario.registrar(nombre,apellido,mail,clave1,clave2);
            
        } catch (MiExcepcion ex) {
            modelo.put("error", ex.getMessage()); 
            
            modelo.put("nombre",nombre);
            modelo.put("apellido",apellido);
            modelo.put("mail",mail);
            modelo.put("clave1",clave1);
            modelo.put("clave2",clave2);
            return "registro.html"; 
            
        }
        modelo.put("exito","¡¡Registro exitoso!!");
        return "registro.html"; 

    }
    
    
}
