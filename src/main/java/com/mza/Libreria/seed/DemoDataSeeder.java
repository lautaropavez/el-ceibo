package com.mza.Libreria.seed;

import com.mza.Libreria.entidades.Autor;
import com.mza.Libreria.entidades.Editorial;
import com.mza.Libreria.entidades.Libro;
import com.mza.Libreria.entidades.Portada;
import com.mza.Libreria.repositorios.AutorRepository;
import com.mza.Libreria.repositorios.EditorialRepository;
import com.mza.Libreria.repositorios.LibroRepository;
import com.mza.Libreria.repositorios.PortadaRepository;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

/**
 * Carga el catalogo demo (autores, editoriales, libros y portadas) usado
 * para la presentacion del proyecto. Solo corre con el profile "demo"
 * (SPRING_PROFILES_ACTIVE=local,demo) y es idempotente: si un autor,
 * editorial o libro ya existe por nombre/titulo, no lo vuelve a crear.
 */
@Component
@Profile("demo")
public class DemoDataSeeder implements CommandLineRunner {

    @Autowired
    private AutorRepository autorRepo;

    @Autowired
    private EditorialRepository editorialRepo;

    @Autowired
    private LibroRepository libroRepo;

    @Autowired
    private PortadaRepository portadaRepo;

    @Override
    @Transactional
    public void run(String... args) throws IOException {
        System.out.println("=== DemoDataSeeder: iniciando carga de datos demo ===");

        Autor garciaMarquez = obtenerOCrearAutor("Gabriel García Márquez");
        Autor tolkien = obtenerOCrearAutor("J. R. R. Tolkien");
        Autor borges = obtenerOCrearAutor("Jorge Luis Borges");
        Autor cortazar = obtenerOCrearAutor("Julio Cortázar");
        Autor allende = obtenerOCrearAutor("Isabel Allende");
        Autor orwell = obtenerOCrearAutor("George Orwell");
        Autor cervantes = obtenerOCrearAutor("Miguel de Cervantes");
        Autor saintExupery = obtenerOCrearAutor("Antoine de Saint-Exupéry");
        Autor kafka = obtenerOCrearAutor("Franz Kafka");
        Autor rowling = obtenerOCrearAutor("J. K. Rowling");
        Autor neruda = obtenerOCrearAutor("Pablo Neruda");
        Autor verne = obtenerOCrearAutor("Julio Verne");
        Autor homero = obtenerOCrearAutor("Homero");
        Autor dispenza = obtenerOCrearAutor("Joe Dispenza");

        Editorial sudamericana = obtenerOCrearEditorial("Sudamericana");
        Editorial minotauro = obtenerOCrearEditorial("Minotauro");
        Editorial emece = obtenerOCrearEditorial("Emecé");
        Editorial alfaguara = obtenerOCrearEditorial("Alfaguara");
        Editorial debolsillo = obtenerOCrearEditorial("Debolsillo");
        Editorial catedra = obtenerOCrearEditorial("Cátedra");
        Editorial salamandra = obtenerOCrearEditorial("Salamandra");
        Editorial alianza = obtenerOCrearEditorial("Alianza Editorial");
        Editorial seixBarral = obtenerOCrearEditorial("Seix Barral");
        Editorial anaya = obtenerOCrearEditorial("Anaya");
        Editorial gredos = obtenerOCrearEditorial("Gredos");
        Editorial urano = obtenerOCrearEditorial("Urano");

        crearLibroSiNoExiste("Cien años de soledad", 1967, garciaMarquez, sudamericana, 5, "portada_cien_anios.jpg");
        crearLibroSiNoExiste("El Hobbit", 1937, tolkien, minotauro, 3, null);
        crearLibroConStockCero("El Señor de los Anillos", 1954, tolkien, minotauro, 1, null);
        crearLibroSiNoExiste("El amor en los tiempos del cólera", 1985, garciaMarquez, sudamericana, 4, null);
        crearLibroSiNoExiste("Ficciones", 1944, borges, emece, 6, null);
        crearLibroSiNoExiste("Rayuela", 1963, cortazar, alfaguara, 3, "portada_rayuela.jpg");
        crearLibroSiNoExiste("La casa de los espíritus", 1982, allende, alfaguara, 5, null);
        crearLibroSiNoExiste("1984", 1949, orwell, debolsillo, 8, "portada_1984.jpg");
        crearLibroSiNoExiste("Don Quijote de la Mancha", 1605, cervantes, catedra, 2, null);
        crearLibroSiNoExiste("El Principito", 1943, saintExupery, salamandra, 6, "portada_principito.jpg");
        crearLibroSiNoExiste("La metamorfosis", 1915, kafka, alianza, 4, null);
        crearLibroSiNoExiste("Harry Potter y la piedra filosofal", 1997, rowling, salamandra, 10, "portada_harry_potter.jpg");
        crearLibroSiNoExiste("Confieso que he vivido", 1974, neruda, seixBarral, 3, null);
        crearLibroSiNoExiste("Veinte mil leguas de viaje submarino", 1870, verne, anaya, 5, null);
        crearLibroSiNoExiste("La Ilíada", 1991, homero, gredos, 4, "portada_iliada.jpg");
        crearLibroSiNoExiste("La Odisea", 1982, homero, gredos, 4, "portada_odisea.jpg");
        crearLibroSiNoExiste("Deja de ser tú", 2012, dispenza, urano, 7, "portada_deja_de_ser_tu.jpg");

        System.out.println("=== DemoDataSeeder: finalizado ===");
    }

    private Autor obtenerOCrearAutor(String nombre) {
        Autor existente = autorRepo.buscarPorNombre(nombre.toUpperCase());
        if (existente != null) {
            return existente;
        }
        Autor nuevo = new Autor();
        nuevo.setNombre(nombre.toUpperCase());
        nuevo.setAlta(Boolean.TRUE);
        return autorRepo.save(nuevo);
    }

    private Editorial obtenerOCrearEditorial(String nombre) {
        Editorial existente = editorialRepo.buscarPorNombre(nombre.toUpperCase());
        if (existente != null) {
            return existente;
        }
        Editorial nueva = new Editorial();
        nueva.setNombre(nombre.toUpperCase());
        nueva.setAlta(Boolean.TRUE);
        return editorialRepo.save(nueva);
    }

    private void crearLibroSiNoExiste(String titulo, int anio, Autor autor, Editorial editorial,
            int nroEjemplares, String archivoPortada) throws IOException {
        if (!libroRepo.buscarPorTituloNormalizadoYAutor(titulo, autor.getId()).isEmpty()) {
            return;
        }
        Libro libro = construirLibroBase(titulo, anio, autor, editorial, nroEjemplares, archivoPortada);
        libro.setEjemplaresPrestados(0);
        libro.setEjemplaresRestantes(nroEjemplares);
        libroRepo.save(libro);
    }

    private void crearLibroConStockCero(String titulo, int anio, Autor autor, Editorial editorial,
            int nroEjemplares, String archivoPortada) throws IOException {
        if (!libroRepo.buscarPorTituloNormalizadoYAutor(titulo, autor.getId()).isEmpty()) {
            return;
        }
        Libro libro = construirLibroBase(titulo, anio, autor, editorial, nroEjemplares, archivoPortada);
        libro.setEjemplaresPrestados(nroEjemplares);
        libro.setEjemplaresRestantes(0);
        libroRepo.save(libro);
    }

    private Libro construirLibroBase(String titulo, int anio, Autor autor, Editorial editorial,
            int nroEjemplares, String archivoPortada) throws IOException {
        Libro libro = new Libro();
        libro.setTitulo(titulo);
        libro.setAnio(anio);
        libro.setAlta(Boolean.TRUE);
        libro.setNroejemplares(nroEjemplares);
        libro.setAutor(autor);
        libro.setEditorial(editorial);
        if (archivoPortada != null) {
            libro.setPortada(cargarPortada(archivoPortada));
        }
        return libro;
    }

    private Portada cargarPortada(String archivo) throws IOException {
        ClassPathResource recurso = new ClassPathResource("static/img/portadas-demo/" + archivo);
        byte[] contenido = StreamUtils.copyToByteArray(recurso.getInputStream());
        Portada portada = new Portada();
        portada.setNombre(archivo);
        portada.setMime("image/jpeg");
        portada.setContenido(contenido);
        return portadaRepo.save(portada);
    }
}
