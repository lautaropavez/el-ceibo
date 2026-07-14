package com.mza.Libreria.servicios;

import com.mza.Libreria.entidades.Portada;
import com.mza.Libreria.excepciones.MiExcepcion;
import com.mza.Libreria.repositorios.PortadaRepository;
import java.io.IOException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Lautaro Pavez
 */
@Service
public class ServicePortada {
    
    @Autowired
    private PortadaRepository portadaRepo;

    @Transactional
    public Portada guardar(MultipartFile archivo) throws MiExcepcion{

        if(archivo != null && !archivo.isEmpty()){
            try{
                Portada portada = new Portada();
                portada.setMime(archivo.getContentType());
                portada.setNombre(archivo.getName());
                portada.setContenido(archivo.getBytes());

                return portadaRepo.save(portada);
            }catch(IOException e){
                System.err.println(e.getMessage());
            }
        }
        return null;
    }
    
    @Transactional
    public Portada actualizar(String idPortada,MultipartFile archivo)throws MiExcepcion{ 
        if (archivo != null && !archivo.isEmpty()) {
            try {
                Portada portada = new Portada();
                
                if (idPortada != null) {
                    Optional<Portada> respuesta = portadaRepo.findById(idPortada);
                    if (respuesta.isPresent()) {
                        portada = respuesta.get();
                    }
                }
                portada.setMime(archivo.getContentType());
                portada.setNombre(archivo.getName());
                portada.setContenido(archivo.getBytes());

                return portadaRepo.save(portada);
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    return null;
    }
}
