package com.mza.Libreria.servicios;

import com.mza.Libreria.entidades.Editorial;
import com.mza.Libreria.excepciones.MiExcepcion;
import com.mza.Libreria.repositorios.EditorialRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Lautaro Pavez
 */
//No tengo metodo modificar porque se modifica solo en el libro

@Service
public class ServiceEditorial {

    @Autowired
    private EditorialRepository editorialRepo;
    
    @Transactional(rollbackFor = MiExcepcion.class)
    public void crearEditorial(String nombre) throws MiExcepcion {
        validacion(nombre);
        Editorial editorial = new Editorial();
        editorial.setNombre(nombre.trim().toUpperCase());
        editorial.setAlta(Boolean.TRUE);

        try {
            editorialRepo.saveAndFlush(editorial);
        } catch (DataIntegrityViolationException ex) {
            throw new MiExcepcion("La editorial ya existe");
        }
    }
    
    @Transactional(readOnly = true)
    public Editorial buscarporId(String id){
        return editorialRepo.buscarPorId(id);
    }
    
    @Transactional(readOnly = true)
    public Editorial buscarporNombre(String nombre){
        return editorialRepo.buscarPorNombre(nombre);
    }
    
    //Busca todas las editoriales(activas e inactivas)
    @Transactional(readOnly = true)
    public List<Editorial> buscaTodas() {
        return editorialRepo.findAll(); 
    }
    
    //Busca todas las editoriales activas
    @Transactional(readOnly = true)
    public List<Editorial> buscaActivas() {
        return editorialRepo.buscaActivas(); 
    }
    
    public void validacion(String nombreEdit) throws MiExcepcion{
        if (nombreEdit == null || nombreEdit.trim().isEmpty()){
            throw new MiExcepcion("Debe indicar el nombre de la Editorial");
        }
        
        Editorial editorial = editorialRepo.buscarPorNombre(nombreEdit.trim().toUpperCase());
        if(editorial != null){
            throw new MiExcepcion("La editorial ya existe");
        }
    } 
    
//    ---------------------------------- NO USADOS --------------------------------
    
//    @Transactional
//    public Editorial modificarEditorial(String nombre) throws MiExcepcion {
//        Editorial editorialeditado = editorialRepo.buscarPorNombre(nombre);
//        if (editorialeditado != null) {      
//            editorialeditado.setNombre(nombre);
//            return editorialRepo.save(editorialeditado);
//        } else {
//            throw new MiExcepcion("No se encontró a este Autor en la base de datos.");
//        }
//    }
    
}
