package com.proyecto.servicios.config;

import com.proyecto.servicios.model.GenericResponse;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<GenericResponse> handleFeignException(FeignException ex) {
        log.error("Error de integracion (FeignException) al llamar al servicio externo: status={}, body={}", ex.status(), ex.contentUTF8());
        GenericResponse response = new GenericResponse();
        
        if (ex.status() == 401 || ex.status() == 403) {
            response.setCodigo(ex.status());
            response.setMensaje("Error de autenticacion al consumir servicio externo (token invalido o expirado).");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        response.setCodigo(ex.status() != 0 ? ex.status() : HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setMensaje("Respuesta no exitosa del servicio externo integrado.");
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getCodigo()));
    }

    @ExceptionHandler(RetryableException.class)
    public ResponseEntity<GenericResponse> handleRetryableException(RetryableException ex) {
        log.error("Error de timeout o conexion (RetryableException) con el servicio externo: {}", ex.getMessage());
        GenericResponse response = new GenericResponse();
        response.setCodigo(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setMensaje("El servicio externo no responde a tiempo (Timeout) o esta inaccesible.");
        return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<GenericResponse> handleResponseStatusException(org.springframework.web.server.ResponseStatusException ex) {
        log.error("Error ResponseStatusException: {}", ex.getReason(), ex);
        GenericResponse response = new GenericResponse();
        response.setCodigo(ex.getStatusCode().value());
        response.setMensaje(ex.getReason() != null ? ex.getReason() : "Ocurrio un error.");
        return new ResponseEntity<>(response, ex.getStatusCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleGeneralException(Exception ex) {
        log.error("Error inesperado en la aplicacion: {}", ex.getMessage(), ex);
        GenericResponse response = new GenericResponse();
        response.setCodigo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        // Agregar el nombre de la excepcion y el mensaje para debuggear
        String debugMsg = "Error interno: " + ex.getClass().getName() + " - " + ex.getMessage();
        if (ex.getCause() != null) {
            debugMsg += " | Causa: " + ex.getCause().getClass().getName() + " - " + ex.getCause().getMessage();
        }
        response.setMensaje(debugMsg);
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
