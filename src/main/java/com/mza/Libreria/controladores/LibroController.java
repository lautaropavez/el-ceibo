package com.mza.Libreria.controladores;

import com.mza.Libreria.entidades.Autor;
import com.mza.Libreria.entidades.Editorial;
import com.mza.Libreria.entidades.Libro;
import com.mza.Libreria.excepciones.MiExcepcion;
import com.mza.Libreria.servicios.ServiceAutor;
import com.mza.Libreria.servicios.ServiceEditorial;
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
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Lautaro Pavez
 */

@Controller 
@RequestMapping("/libro")
public class LibroController {

    @Autowired
    private ServiceLibro servLibro; 
    
    @Autowired
    private ServiceAutor servAutor; 
    
    @Autowired
    private ServiceEditorial servEditorial; 
    
    @GetMapping("/registro") 
    public String formulario(ModelMap modelo){
        
        List<Autor> autores = servAutor.buscaActivos();
        modelo.addAttribute("autores", autores);
        
        List<Editorial> editoriales = servEditorial.buscaActivas();
        modelo.addAttribute("editoriales", editoriales);
        
        return "nuevoLibro";
    }	
      
    @PostMapping("/registrar") 
    public String crear(ModelMap modelo,@RequestParam MultipartFile archivo,@RequestParam String titulo,@RequestParam Integer anio, @RequestParam String idAutor,@RequestParam String idEditorial){ 
        try{
           servLibro.crearLibro(archivo,titulo,anio,idAutor,idEditorial);
           modelo.put("exito","¡Registro exitoso!");    
           List<Autor> autores = servAutor.buscaActivos();
           modelo.addAttribute("autores", autores);
        
           List<Editorial> editoriales = servEditorial.buscaActivas();
           modelo.addAttribute("editoriales", editoriales);
           return "nuevoLibro"; 
          
        }catch(MiExcepcion ex){
            modelo.put("error", ex.getMessage());
            modelo.put("titulo",titulo);
            modelo.put("anio",anio);
            modelo.put("autor",idAutor);
            modelo.put("editorial",idEditorial);
            return "nuevoLibro"; 
        }  
    }

    
    @GetMapping("/lista") 
    public String lista(ModelMap modelo){
        List<Libro> librosLista = servLibro.listarTodos();
        modelo.addAttribute("libros",librosLista); 
         return "list-libro";
    }
        
    //IN PROCESS
    @GetMapping("/biblioteca") 
    public String biblioteca(ModelMap modelo){
       // List<Libro> librosLista = servLibro.listarTodos();
      //  modelo.addAttribute("libros",librosLista); 
       return "/fragments/nada"; 
    }
    
    @GetMapping("/modificar/{id}") 
    public String modificar(@PathVariable String id,ModelMap modelo){
        modelo.put("libro",servLibro.buscarPorId(id));
        return "modif-Libro"; 
    }
    
    @PostMapping("/modificar/{id}") //@RequestParam MultipartFile archivo
    public String modificar(ModelMap modelo,@PathVariable String id, @RequestParam String titulo,@RequestParam Integer anio, @RequestParam String nombreAut,@RequestParam String nombreEdit)throws Exception{
        try{
            servLibro.modificarLibro(null,id,titulo,anio,nombreAut,nombreEdit);
            return "list-libro";
        }catch(MiExcepcion ex){
            modelo.put("error",ex.getMessage());
            modelo.put("id",id);
           // modelo.put("archivo",archivo);
            modelo.put("titulo",titulo);
            modelo.put("anio",anio);
            modelo.put("nombreAut",nombreAut);
            modelo.put("nombreEdit",nombreEdit);
            return "modif-Libro";
        }
    }
    
    @GetMapping("/baja/{id}")
    public String baja(@PathVariable String id,ModelMap modelo){
        try {
            servLibro.baja(id);
            return "redirect:/libro/lista";  
        } catch (Exception ex) {
            modelo.put("error", ex.getMessage());
            return "redirect:/libro/lista"; 
        }
    }
    
    @GetMapping("/alta/{id}") 
    public String alta(@PathVariable String id,ModelMap modelo){
        try {
            servLibro.alta(id);
            return "redirect:/libro/lista";  
        } catch (Exception ex) {
            modelo.put("error", ex.getMessage()); 
            return "redirect:/libro/lista"; 
        }
    }
    
    @GetMapping("/eliminar/{id}") 
    public String eliminar(@PathVariable String id,ModelMap modelo) throws Exception{
        try {
            servLibro.eliminarLibro(id);
            return "redirect:/libro/lista";  
        }catch(MiExcepcion ex) {
            modelo.put("error", ex.getMessage()); 
            return "redirect:/libro/lista"; 
        }
    }   
}
