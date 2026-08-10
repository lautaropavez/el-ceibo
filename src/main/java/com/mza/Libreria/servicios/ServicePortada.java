package com.mza.Libreria.servicios;

import com.mza.Libreria.entidades.Portada;
import com.mza.Libreria.excepciones.MiExcepcion;
import com.mza.Libreria.repositorios.PortadaRepository;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Lautaro Pavez
 */
@Service
public class ServicePortada {

    private static final long MAX_FILE_BYTES = 5L * 1024L * 1024L;
    private static final int MAX_WIDTH = 4000;
    private static final int MAX_HEIGHT = 4000;
    private static final long MAX_PIXELS = 12_000_000L;

    private static final String FORMATO_JPEG = "JPEG";
    private static final String FORMATO_PNG = "PNG";
    private static final byte[] FIRMA_PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    @Autowired
    private PortadaRepository portadaRepo;

    @Transactional(rollbackFor = MiExcepcion.class)
    public Portada guardar(MultipartFile archivo) throws MiExcepcion {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }
        ImagenValidada validada = validarYRecodificar(archivo);

        Portada portada = new Portada();
        portada.setMime(validada.mime);
        portada.setNombre(null);
        portada.setContenido(validada.bytes);

        return portadaRepo.save(portada);
    }

    @Transactional(rollbackFor = MiExcepcion.class)
    public Portada actualizar(String idPortada, MultipartFile archivo) throws MiExcepcion {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }
        ImagenValidada validada = validarYRecodificar(archivo);

        if (idPortada == null) {
            throw new MiExcepcion("La portada que se intenta actualizar ya no existe.");
        }
        Portada portada = portadaRepo.findById(idPortada)
                .orElseThrow(() -> new MiExcepcion("La portada que se intenta actualizar ya no existe."));
        portada.setMime(validada.mime);
        portada.setNombre(null);
        portada.setContenido(validada.bytes);

        return portadaRepo.save(portada);
    }

    private ImagenValidada validarYRecodificar(MultipartFile archivo) throws MiExcepcion {
        if (archivo.getSize() > MAX_FILE_BYTES) {
            throw new MiExcepcion("El archivo supera el tamaño máximo permitido (5 MB).");
        }

        byte[] bytesOriginales;
        try {
            bytesOriginales = archivo.getBytes();
        } catch (IOException e) {
            throw new MiExcepcion("No se pudo leer el archivo enviado.");
        }

        if (bytesOriginales.length == 0) {
            throw new MiExcepcion("El archivo enviado está vacío.");
        }

        String formato = detectarFormatoPorFirma(bytesOriginales);
        if (formato == null) {
            throw new MiExcepcion("Formato de imagen no soportado. Solo se aceptan JPEG y PNG.");
        }

        BufferedImage imagen = decodificar(bytesOriginales, formato);

        if (FORMATO_JPEG.equals(formato)) {
            imagen = normalizarARgb(imagen);
        }

        byte[] recodificado = recodificar(imagen, formato);
        if (recodificado.length > MAX_FILE_BYTES) {
            throw new MiExcepcion("La imagen procesada supera el tamaño máximo permitido (5 MB).");
        }

        String mime = FORMATO_JPEG.equals(formato) ? "image/jpeg" : "image/png";
        return new ImagenValidada(recodificado, mime);
    }

    private String detectarFormatoPorFirma(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return FORMATO_JPEG;
        }
        if (bytes.length >= FIRMA_PNG.length) {
            boolean coincide = true;
            for (int i = 0; i < FIRMA_PNG.length; i++) {
                if (bytes[i] != FIRMA_PNG[i]) {
                    coincide = false;
                    break;
                }
            }
            if (coincide) {
                return FORMATO_PNG;
            }
        }
        return null;
    }

    private String normalizarFormatoReader(String formatoReader) {
        if (formatoReader == null) {
            return null;
        }
        String f = formatoReader.trim().toUpperCase();
        if (f.equals("JPEG") || f.equals("JPG")) {
            return FORMATO_JPEG;
        }
        if (f.equals("PNG")) {
            return FORMATO_PNG;
        }
        return null;
    }

    private BufferedImage decodificar(byte[] bytesOriginales, String formatoEsperado) throws MiExcepcion {
        ImageReader reader = null;
        try {
            ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytesOriginales));
            if (iis == null) {
                throw new MiExcepcion("No se pudo interpretar el archivo como imagen.");
            }
            try {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
                if (!readers.hasNext()) {
                    throw new MiExcepcion("No se encontró un decodificador para este archivo.");
                }
                reader = readers.next();
                reader.setInput(iis, true, true);

                String formatoReal = normalizarFormatoReader(reader.getFormatName());
                if (!formatoEsperado.equals(formatoReal)) {
                    throw new MiExcepcion("El contenido real del archivo no coincide con su firma declarada.");
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0) {
                    throw new MiExcepcion("La imagen no tiene dimensiones válidas.");
                }
                if (width > MAX_WIDTH || height > MAX_HEIGHT) {
                    throw new MiExcepcion("La imagen supera las dimensiones máximas permitidas (4000x4000).");
                }
                if ((long) width * (long) height > MAX_PIXELS) {
                    throw new MiExcepcion("La imagen supera la cantidad máxima de píxeles permitida.");
                }

                BufferedImage imagen;
                try {
                    imagen = reader.read(0);
                } catch (RuntimeException decodeInesperado) {
                    throw new MiExcepcion("No se pudo decodificar el archivo como imagen.");
                }
                if (imagen == null) {
                    throw new MiExcepcion("No se pudo decodificar el archivo como imagen.");
                }
                return imagen;
            } finally {
                iis.close();
            }
        } catch (IOException e) {
            throw new MiExcepcion("No se pudo decodificar el archivo como imagen.");
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }
    }

    // JPEG no admite canal alfa; se normaliza a un modelo de color fijo
    // para evitar depender del BufferedImage.getType() que devuelva el decoder.
    private BufferedImage normalizarARgb(BufferedImage origen) {
        if (origen.getType() == BufferedImage.TYPE_INT_RGB) {
            return origen;
        }
        BufferedImage normalizada = new BufferedImage(origen.getWidth(), origen.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = normalizada.createGraphics();
        try {
            g.drawImage(origen, 0, 0, Color.WHITE, null);
        } finally {
            g.dispose();
        }
        return normalizada;
    }

    private byte[] recodificar(BufferedImage imagen, String formato) throws MiExcepcion {
        String formatoWriter = FORMATO_JPEG.equals(formato) ? "jpg" : "png";
        // ByteArrayOutputStream.write() no declara "throws IOException" (nunca lo hace
        // por diseño), por lo que un límite acotado solo puede cortar el crecimiento
        // lanzando una excepción unchecked propia; se captura explícitamente más abajo,
        // nunca con un catch(Exception) genérico.
        SalidaAcotada salida = new SalidaAcotada((int) MAX_FILE_BYTES);
        boolean escrito;
        try {
            escrito = ImageIO.write(imagen, formatoWriter, salida);
        } catch (LimiteSuperadoException limiteSuperado) {
            throw new MiExcepcion("La imagen procesada supera el tamaño máximo permitido (5 MB).");
        } catch (RuntimeException codificacionInesperada) {
            throw new MiExcepcion("No se pudo generar la imagen procesada.");
        } catch (IOException e) {
            throw new MiExcepcion("No se pudo generar la imagen procesada.");
        }
        if (!escrito) {
            throw new MiExcepcion("No se encontró un codificador para este formato de imagen.");
        }
        return salida.toByteArray();
    }

    private static final class ImagenValidada {
        private final byte[] bytes;
        private final String mime;

        private ImagenValidada(byte[] bytes, String mime) {
            this.bytes = bytes;
            this.mime = mime;
        }
    }

    private static final class LimiteSuperadoException extends RuntimeException {
    }

    private static final class SalidaAcotada extends ByteArrayOutputStream {
        private final int limiteBytes;

        private SalidaAcotada(int limiteBytes) {
            this.limiteBytes = limiteBytes;
        }

        @Override
        public synchronized void write(int b) {
            if (count >= limiteBytes) {
                throw new LimiteSuperadoException();
            }
            super.write(b);
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) {
            // "len > limiteBytes - count" en vez de "count + len > limiteBytes":
            // count y limiteBytes se mantienen siempre acotados (~5MB), por lo que
            // la resta nunca desborda, a diferencia de la suma si len fuera enorme.
            if (len > limiteBytes - count) {
                throw new LimiteSuperadoException();
            }
            super.write(b, off, len);
        }
    }
}
