-- V3: formaliza en la DB tres invariantes de negocio ya vigentes en la
-- aplicacion, verificadas contra los datos reales antes de esta
-- migracion (0 libros sin autor, 0 portadas compartidas, 0 duplicados
-- titulo+autor exactos ni normalizados, 0 titulos con espacios extremos).
-- Ninguna requiere limpieza de datos previa.

-- 1. autor_id pasa a obligatorio (la aplicacion ya garantiza esto en
-- crearLibro()/modificarLibro(); no hay ninguna fila legacy con
-- autor_id NULL).
ALTER TABLE libro MODIFY COLUMN autor_id VARCHAR(255) NOT NULL;

-- 2. Una Portada no puede quedar referenciada por mas de un Libro.
-- MySQL/MariaDB permite multiples NULL bajo UNIQUE de forma nativa,
-- por lo que los libros sin portada no se ven afectados.
ALTER TABLE libro ADD CONSTRAINT UK_libro_portada_id UNIQUE (portada_id);

-- 3. Mismo titulo + mismo autor queda bloqueado a nivel de DB, como
-- segunda linea de defensa detras de validarDuplicado() (que ademas
-- normaliza con TRIM+UPPER, algo que este UNIQUE simple no repite;
-- ver razonamiento aceptado explicitamente en la posta).
ALTER TABLE libro ADD CONSTRAINT UK_libro_titulo_autor UNIQUE (titulo, autor_id);
