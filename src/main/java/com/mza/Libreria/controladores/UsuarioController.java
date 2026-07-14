package com.mza.Libreria.controladores;

import com.mza.Libreria.entidades.Usuario;
import com.mza.Libreria.excepciones.MiExcepcion;
import com.mza.Libreria.servicios.ServiceUsuario;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
/**
 *
 * @author Lautaro Pavez
 */

@Controller 
@RequestMapping("/usuario") 
public class UsuarioController {
    
    @Autowired
    private ServiceUsuario servUsuario;
    
    @GetMapping("/lista") 
    public String lista(ModelMap modelo){
        List<Usuario> usuariosLista = servUsuario.listarTodos();
        modelo.addAttribute("usuarios",usuariosLista); 
        return "list-usuario";  
    }
    
    @GetMapping("/modificar/{id}") 
    public String modificar(@PathVariable String id,ModelMap modelo){  
        modelo.put("usuario",servUsuario.buscarPorId(id));
        return "modif-Usuario"; 
    }
    
    @PostMapping("/modificar/{id}") 
    public String modificar(ModelMap modelo,@PathVariable String id,@RequestParam String nombre,@RequestParam String apellido,@RequestParam String mail,@RequestParam String clave,@RequestParam String clave2)throws Exception{
        try{
            servUsuario.modificar(id,nombre,apellido,mail,clave,clave2);
            modelo.put("exito","Modificación exitosa"); 
            return lista(modelo);   
        }catch(Exception e){
            modelo.put("error","Falto algún dato"); 
            return "modif-Usuario"; 
        }
    }
    
    @GetMapping("/baja/{id}") 
    public String baja(@PathVariable String id,ModelMap modelo){
        try {
            servUsuario.deshabilitar(id);
            return "redirect:/usuario/lista";  
        } catch (MiExcepcion ex) {
            modelo.put("error", ex.getMessage()); 
            return "redirect:/usuario/lista"; 
        }
    }
    
    @GetMapping("/alta/{id}") 
    public String alta(@PathVariable String id,ModelMap modelo){ //Acá recibo un id que viene por URL --> /modificar/${id} y ese id es el que uso para buscar el usuario y mostrarlo, lo enviamos tambien por url 
        try {
            servUsuario.habilitar(id);
            return "redirect:/usuario/lista";  
        } catch (MiExcepcion ex) {
            return "redirect:/usuario/lista"; 
        }
    }
    
    @GetMapping("/eliminar/{id}") 
    public String eliminar(@PathVariable String id,ModelMap modelo) throws Exception{
        try {
            servUsuario.eliminar(id);
            return "redirect:/usuario/lista";  
        }catch(MiExcepcion ex) {
            modelo.put("error", ex.getMessage()); 
            return "redirect:/usuario/lista"; 
        }
    }   
}
