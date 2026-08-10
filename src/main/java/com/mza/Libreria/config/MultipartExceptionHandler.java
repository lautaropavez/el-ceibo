package com.mza.Libreria.config;

import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * MaxUploadSizeExceededException ocurre durante la resolución del multipart,
 * antes de que el método del controller se ejecute, por lo que ningún
 * try/catch dentro de LibroController puede atraparla.
 *
 * @author Lautaro Pavez
 */
@ControllerAdvice
public class MultipartExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String archivoDemasiadoGrande(HttpServletRequest request) {
        String uri = request.getRequestURI();

        if ("/libro/registrar".equals(uri)) {
            return "redirect:/libro/registro";
        }
        if (uri != null && uri.startsWith("/libro/modificar/")) {
            String resto = uri.substring("/libro/modificar/".length());
            if (!resto.isEmpty() && !resto.contains("/")) {
                return "redirect:" + uri;
            }
        }
        return "redirect:/libro/lista";
    }
}
