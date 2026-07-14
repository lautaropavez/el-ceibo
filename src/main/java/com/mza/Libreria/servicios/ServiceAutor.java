
package com.mza.Libreria.servicios;

import com.mza.Libreria.entidades.Autor;
import com.mza.Libreria.excepciones.MiExcepcion;
import com.mza.Libreria.repositorios.AutorRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Lautaro Pavez
 */
//No tengo metodo modificar porque se modifica solo en el libro
@Service
public class ServiceAutor {

    @Autowired
    private AutorRepository autorRepo;
        
    @Transactional
    public void crearAutor(String nombre) throws MiExcepcion {
        validacion(nombre);
        Autor autor = new Autor();
        autor.setNombre(nombre.toUpperCase());
        autor.setAlta(Boolean.TRUE);

        autorRepo.save(autor); 
    }
       
    @Transactional(readOnly = true)
    public Autor buscarporId(String id){
        return autorRepo.buscarPorId(id); 
    }
   
    @Transactional(readOnly = true)
    public Autor buscarPorNombre(String nombre) {
        return autorRepo.buscarPorNombre(nombre); 
    }
    
    //Busca todos los autores(activos e inactivos)
    @Transactional(readOnly = true)
    public List<Autor> buscaTodos() {
        return autorRepo.findAll(); 
    }
    
    //Busca todos los autores activos
    @Transactional(readOnly = true)
    public List<Autor> buscaActivos() {
        return autorRepo.buscaActivos(); 
    }
    
    public void validacion(String nombreAut) throws MiExcepcion {
        if (nombreAut == null || nombreAut.trim().isEmpty()) {
            throw new MiExcepcion("Debe indicar el nombre del Autor");
        }
        
        Autor autor = autorRepo.buscarPorNombre(nombreAut.toUpperCase());
        if(autor != null){
            throw new MiExcepcion("El autor ya existe");
        }
    }
    
}
