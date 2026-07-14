package com.mza.Libreria.entidades;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import org.hibernate.annotations.GenericGenerator;


/**
 *
 * @author Lautaro Pavez
 */

@Entity
public class Libro {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;
    
    @Column(unique = true)
    private Long isbn;
    
    @Column(nullable = false)
    private String titulo;
    
    private Integer anio;
    private Integer nroejemplares;
    private Integer ejemplaresPrestados;
    private Integer ejemplaresRestantes;
    private Boolean alta;
    
//    @OneToOne(cascade = CascadeType.REMOVE)// Haciendo referencia a que si modifico un "usuario" se elimine en cascada 
    @ManyToOne(cascade = CascadeType.ALL)
    private Autor autor; //2° Clase min 5
    
    @ManyToOne(cascade = CascadeType.ALL)// Esto indicado para que si modifico al PADRE,modifico el vinculado
    private Editorial editorial;
    
    @OneToOne
    private Portada portada;
    
//  GETTERS & SETTERS

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getIsbn() {
        return isbn;
    }

    public void setIsbn(Long isbn) {
        this.isbn = isbn;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Integer getNroejemplares() {
        return nroejemplares;
    }

    public void setNroejemplares(Integer nroejemplares) {
        this.nroejemplares = nroejemplares;
    }

    public Integer getEjemplaresPrestados() {
        return ejemplaresPrestados;
    }

    public void setEjemplaresPrestados(Integer ejemplaresPrestados) {
        this.ejemplaresPrestados = ejemplaresPrestados;
    }

    public Integer getEjemplaresRestantes() {
        return ejemplaresRestantes;
    }

    public void setEjemplaresRestantes(Integer ejemplaresRestantes) {
        this.ejemplaresRestantes = ejemplaresRestantes;
    }

    public Boolean getAlta() {
        return alta;
    }

    public void setAlta(Boolean alta) {
        this.alta = alta;
    }

    public Autor getAutor() {
        return autor;
    }

    public void setAutor(Autor autor) {
        this.autor = autor;
    }

    public Editorial getEditorial() {
        return editorial;
    }

    public void setEditorial(Editorial editorial) {
        this.editorial = editorial;
    }   
    
    public Portada getPortada() {
        return portada;
    }
        
    public void setPortada(Portada portada) {
        this.portada = portada;
    }
    
}

