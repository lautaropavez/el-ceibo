package com.mza.Libreria.controladores;

import com.mza.Libreria.entidades.Libro;
import com.mza.Libreria.entidades.Portada;
import com.mza.Libreria.excepciones.MiExcepcion;
import com.mza.Libreria.servicios.ServiceLibro;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * @author Lautaro Pavez
 */
@Controller
@RequestMapping("/portada")
public class PortadaController {

    private static final String MIME_JPEG = "image/jpeg";
    private static final String MIME_PNG = "image/png";

    private static final byte[] FIRMA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] FIRMA_PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    @Autowired
    private ServiceLibro servLibro;

//  (1° Forma que aprendimos, solo cambian las 2 lineas que comentamos)
//   @GetMapping("/libro")
//   public ResponseEntity<byte[]> portadaLibro(@RequestParam String id) throws MiExcepcion{
//  (2° Forma que aprendimos) Para pasarle el id por la URL con PathVariable
    @GetMapping("/libro/{id}")
    public ResponseEntity<byte[]> portadaLibro(@PathVariable String id) {
        Libro libro;
        try {
            libro = servLibro.findbyId(id);
        } catch (MiExcepcion noExiste) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Portada portada = libro.getPortada();
        if (portada == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        byte[] contenido = portada.getContenido(); // Accedemos al contenido que es un arreglo de bytes, que es el que tenemos que hacer que descargue el navegador con una URL pq las fotos en HTML se consumen con una URL
        if (contenido == null || contenido.length == 0) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        MediaType mediaType = resolverMediaTypeConfiable(portada.getMime(), contenido);
        if (mediaType == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        HttpHeaders headers = new HttpHeaders(); //Las cabeceras le van a  decir al navegador que yo estoy devolviendo una imagen
        headers.setContentType(mediaType);
        return new ResponseEntity<>(contenido, headers, HttpStatus.OK); // El response entity puede recibir tres parámetros: 1° contenido, 2° cabecera 3° estado en el que se determina ese proceso, osea con que código vamos a devolver el pedido(HTTP STATUS 200,500,400,etc.)
    }

    // El MIME persistido nunca se usa directamente como Content-Type: solo se
    // acepta si además coincide con la firma real de los bytes.
    private MediaType resolverMediaTypeConfiable(String mime, byte[] contenido) {
        if (MIME_JPEG.equals(mime) && empiezaCon(contenido, FIRMA_JPEG)) {
            return MediaType.IMAGE_JPEG;
        }
        if (MIME_PNG.equals(mime) && empiezaCon(contenido, FIRMA_PNG)) {
            return MediaType.IMAGE_PNG;
        }
        return null;
    }

    private boolean empiezaCon(byte[] contenido, byte[] firma) {
        if (contenido.length < firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if (contenido[i] != firma[i]) {
                return false;
            }
        }
        return true;
    }
}
