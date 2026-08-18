-- V4: alinea la tabla prestamo con el modelo de ciclo de vida de P13
-- (docs/specs/P13_PRESTAMOS.md). El campo alta se elimina: el estado del
-- prestamo pasa a derivarse unicamente de devolucion (NULL = activo).
-- Las columnas fisicas prestamo/devolucion no se renombran (solo cambian
-- de nombre los campos Java, ver entidad Prestamo).

-- 1. Eliminar alta: ya no es fuente de verdad de ningun estado.
ALTER TABLE prestamo DROP COLUMN alta;

-- 2. Agregar fecha_vencimiento como nullable primero, para poder
-- completarla con backfill antes de exigir NOT NULL.
ALTER TABLE prestamo ADD COLUMN fecha_vencimiento datetime NULL;

-- 3. Backfill unicamente de filas legacy validas (prestamo NOT NULL):
-- fecha_vencimiento = prestamo + 14 dias, igual que calculara el
-- servicio para los prestamos nuevos. No se inventa ningun dato para
-- filas con prestamo NULL; esas quedan con fecha_vencimiento NULL y
-- haran fallar visiblemente el paso 4 si existen.
UPDATE prestamo
SET fecha_vencimiento = DATE_ADD(prestamo, INTERVAL 14 DAY)
WHERE prestamo IS NOT NULL;

-- 4. A partir de aca, todo prestamo (legacy o nuevo) debe cumplir el
-- modelo obligatorio de P13. Si alguna fila legacy no lo cumple
-- (prestamo/usuario_id/libro_id NULL, o por lo tanto fecha_vencimiento
-- sin poder completarse en el paso 3), esta migracion falla aqui de
-- forma visible en vez de fabricar datos.
ALTER TABLE prestamo MODIFY COLUMN fecha_vencimiento datetime NOT NULL;
ALTER TABLE prestamo MODIFY COLUMN usuario_id varchar(255) NOT NULL;
ALTER TABLE prestamo MODIFY COLUMN libro_id varchar(255) NOT NULL;
ALTER TABLE prestamo MODIFY COLUMN prestamo datetime NOT NULL;

-- 5. Indices de soporte para las consultas de negocio de P13.5.4:
-- prestamos activos por usuario (cupo maximo) y prestamo activo por
-- usuario+libro (no duplicado). devolucion queda al final de cada
-- indice porque las consultas filtran por "devolucion IS NULL".
CREATE INDEX idx_prestamo_usuario_activo ON prestamo (usuario_id, devolucion);
CREATE INDEX idx_prestamo_usuario_libro_activo ON prestamo (usuario_id, libro_id, devolucion);
