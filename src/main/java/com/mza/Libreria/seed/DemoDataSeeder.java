package com.mza.Libreria.seed;

import com.mza.Libreria.entidades.Autor;
import com.mza.Libreria.entidades.Editorial;
import com.mza.Libreria.entidades.Libro;
import com.mza.Libreria.entidades.Portada;
import com.mza.Libreria.entidades.Usuario;
import com.mza.Libreria.excepciones.MiExcepcion;
import com.mza.Libreria.repositorios.AutorRepository;
import com.mza.Libreria.repositorios.EditorialRepository;
import com.mza.Libreria.repositorios.LibroRepository;
import com.mza.Libreria.repositorios.PortadaRepository;
import com.mza.Libreria.repositorios.PrestamoRepository;
import com.mza.Libreria.repositorios.UsuarioRepository;
import com.mza.Libreria.servicios.ServicePrestamo;
import com.mza.Libreria.servicios.ServiceUsuario;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

/**
 * Carga los datos demo (catalogo, usuarios demo y un prestamo real) usados
 * para la presentacion del proyecto. Solo corre con el profile "demo"
 * (SPRING_PROFILES_ACTIVE=demo; no se combina con "local", ver README) y es
 * idempotente: si un autor,
 * editorial, libro, usuario o prestamo (en cualquier estado) ya existe, no
 * lo vuelve a crear.
 *
 * Lo primero que hace run() es una guarda autoritativa: lee el catalogo
 * real de la conexion y aborta, antes de tocar cualquier repositorio o
 * service, si no es la base demo que corresponde a los perfiles activos.
 * Las credenciales de los usuarios demo vienen del entorno (sin default).
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

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private PrestamoRepository prestamoRepo;

    @Autowired
    private ServiceUsuario serviceUsuario;

    @Autowired
    private ServicePrestamo servicePrestamo;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    @Value("${app.demo.admin.mail}")
    private String adminMail;

    @Value("${app.demo.admin.password}")
    private String adminPassword;

    @Value("${app.demo.usuario.mail}")
    private String usuarioMail;

    @Value("${app.demo.usuario.password}")
    private String usuarioPassword;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(String... args) throws IOException {
        validarBaseDemo();
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
        Libro senorDeLosAnillos = crearLibroSiNoExiste("El Señor de los Anillos", 1954, tolkien, minotauro, 1, null);
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

        crearUsuarioDemo(adminMail, adminPassword, "Admin", "Demo", true);
        Usuario usuarioDemo = crearUsuarioDemo(usuarioMail, usuarioPassword, "Usuario", "Demo", false);

        crearPrestamoSiNoExiste(usuarioDemo, senorDeLosAnillos);

        System.out.println("=== DemoDataSeeder: finalizado ===");
    }

    // Guarda autoritativa: independiente de los initializers, verifica el
    // catalogo realmente conectado antes de cualquier repo/service.
    private void validarBaseDemo() {
        String catalogo;
        try (Connection conexion = dataSource.getConnection()) {
            catalogo = conexion.getCatalog();
        } catch (SQLException ex) {
            throw new IllegalStateException(
                "No se pudo verificar la base conectada. Abortando el seeder demo.", ex);
        }
        DemoDatabaseEarlyGuardInitializer.validarCatalogo(
                catalogo, Arrays.asList(environment.getActiveProfiles()));
    }

    // cambiarRol() es un toggle: solo se invoca en la creacion inicial del
    // admin, nunca si el usuario ya existia.
    private Usuario crearUsuarioDemo(String mail, String clave, String nombre, String apellido,
            boolean admin) {
        if (mail == null || mail.trim().isEmpty() || clave == null || clave.trim().isEmpty()) {
            throw new IllegalStateException(
                "Faltan credenciales del usuario demo '" + nombre + "'. Abortando.");
        }
        String mailTrim = mail.trim();
        Usuario existente = usuarioRepo.buscarPorMail(mailTrim);
        if (existente != null) {
            return existente;
        }
        try {
            serviceUsuario.registrar(nombre, apellido, mailTrim, clave, clave);
            Usuario creado = usuarioRepo.buscarPorMail(mailTrim);
            if (creado == null) {
                throw new IllegalStateException(
                    "No se encontro el usuario demo '" + nombre + "' tras registrarlo.");
            }
            if (admin) {
                serviceUsuario.cambiarRol(creado.getId());
            }
            return creado;
        } catch (MiExcepcion ex) {
            throw new IllegalStateException(
                "No se pudo crear el usuario demo '" + nombre + "': " + ex.getMessage(), ex);
        }
    }

    // Idempotencia any-state: si ya hubo un prestamo (activo o devuelto)
    // de este usuario sobre este libro, no se solicita otro.
    private void crearPrestamoSiNoExiste(Usuario usuario, Libro libro) {
        if (prestamoRepo.existsByUsuario_IdAndLibro_Id(usuario.getId(), libro.getId())) {
            return;
        }
        try {
            servicePrestamo.solicitar(usuario.getMail(), libro.getId());
        } catch (MiExcepcion ex) {
            throw new IllegalStateException(
                "No se pudo crear el prestamo demo: " + ex.getMessage(), ex);
        }
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

    private Libro crearLibroSiNoExiste(String titulo, int anio, Autor autor, Editorial editorial,
            int nroEjemplares, String archivoPortada) throws IOException {
        List<Libro> existentes = libroRepo.buscarPorTituloNormalizadoYAutor(titulo, autor.getId());
        if (!existentes.isEmpty()) {
            return existentes.get(0);
        }
        Libro libro = construirLibroBase(titulo, anio, autor, editorial, nroEjemplares, archivoPortada);
        libro.setEjemplaresPrestados(0);
        libro.setEjemplaresRestantes(nroEjemplares);
        return libroRepo.save(libro);
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
