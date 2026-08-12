-- V2: convierte libro.isbn del modelo legacy (BIGINT con valores
-- aleatorios falsos) al modelo bibliografico acordado (VARCHAR(13)
-- NULL UNIQUE). Los 17 valores legacy no representan ningun ISBN
-- real, se limpian a NULL antes de cambiar el tipo de columna.

-- 1. Limpiar valores legacy (todos son aleatorios, ninguno es un ISBN real).
UPDATE libro
SET isbn = NULL
WHERE isbn IS NOT NULL;

-- 2. Eliminar el UNIQUE legacy sobre la columna BIGINT.
ALTER TABLE libro DROP INDEX UK_ehuya6b4bxgkc4ru5wcf5njgr;

-- 3. Cambiar el tipo de columna (ya vacia tras el paso 1).
ALTER TABLE libro MODIFY COLUMN isbn VARCHAR(13) NULL;

-- 4. Crear el UNIQUE estable nuevo sobre la columna ya VARCHAR.
ALTER TABLE libro ADD CONSTRAINT UK_libro_isbn UNIQUE (isbn);
