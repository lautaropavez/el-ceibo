package com.mza.Libreria.controladores;
import com.mza.Libreria.entidades.Autor;
import com.mza.Libreria.excepciones.MiExcepcion;
import com.mza.Libreria.servicios.ServiceAutor;
import com.mza.Libreria.servicios.ServiceLibro;
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
@RequestMapping("/autor") 
public class AutorController {

    @Autowired
    private ServiceAutor servAutor;
    
    @Autowired
    private ServiceLibro servLibro;
    
    @GetMapping("/registroAutor")
    public String formulario() {
        return "nuevoAutor";
    }

    @PostMapping("/registrarAutor")
    public String crear(ModelMap modelo, @RequestParam String nombreAutor) {
        try {
            servAutor.crearAutor(nombreAutor);
            modelo.put("exito", "¡Has creado un nuevo Autor!"); 
            return "nuevoAutor";
        } catch (MiExcepcion ex) {
            modelo.put("error", ex.getMessage());
            modelo.put("nombreAutor", nombreAutor);
            return "nuevoAutor";
        }
    }
    
    @GetMapping("/lista")
    public String lista(ModelMap modelo){
        List<Autor> autoresLista = servAutor.buscaTodos();
        modelo.addAttribute("autores",autoresLista);
        return "list-autor"; 
    }
     
    
    @GetMapping("/alta/{id}") 
    public String alta(@PathVariable String id,ModelMap modelo){
        try {
            servLibro.altaxAutor(id);
            return "redirect:/autor/lista";  
        } catch (MiExcepcion ex) {
            modelo.put("error", ex.getMessage()); 
            return "redirect:/autor/lista"; 
        }
    }
    
    @GetMapping("/baja/{id}")
    public String baja(@PathVariable String id,ModelMap modelo){
        try {
            servLibro.bajaxAutor(id);
            return "redirect:/autor/lista";  
        } catch (MiExcepcion ex) {
            modelo.put("error", ex.getMessage());
            return "redirect:/autor/lista"; 
        }
    }

    @GetMapping("/eliminar/{id}") 
    public String eliminar(@PathVariable String id,ModelMap modelo) throws Exception{
        try {
            servLibro.eliminarAutor(id);
            return "redirect:/autor/lista";  
        }catch(MiExcepcion ex) {
            modelo.put("error", ex.getMessage()); 
            return "redirect:/autor/lista"; 
        }
    }     
}
