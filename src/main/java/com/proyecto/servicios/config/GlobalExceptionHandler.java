package com.proyecto.servicios.config;

import com.proyecto.servicios.exception.OnboardingException;
import com.proyecto.servicios.model.GenericResponse;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(OnboardingException.class)
    public ResponseEntity<Map<String, Object>> handleOnboardingException(OnboardingException ex) {
        log.warn("Excepcion de negocio (Onboarding): {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("codigo", ex.getStatus().value());
        response.put("mensaje", ex.getMessage());
        return new ResponseEntity<>(response, ex.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error("Error de validacion (400 Bad Request): {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("codigo", HttpStatus.BAD_REQUEST.value());
        response.put("mensaje", "Error de validacion en los datos enviados.");
        response.put("errores", errors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.error("Error al leer el JSON de la peticion: {}", ex.getMessage());
        GenericResponse response = new GenericResponse();
        response.setCodigo(HttpStatus.BAD_REQUEST.value());
        response.setMensaje("El cuerpo de la peticion esta vacio o el JSON tiene un formato incorrecto (revisa los tipos de dato y el formato de fecha AAAA-MM-DD).");
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

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

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<GenericResponse> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException ex) {
        log.error("Acceso denegado (403 Forbidden): {}", ex.getMessage());
        GenericResponse response = new GenericResponse();
        response.setCodigo(HttpStatus.FORBIDDEN.value());
        response.setMensaje("Acceso denegado: No tienes permisos para acceder a este recurso o no te pertenece.");
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(org.springframework.security.authorization.AuthorizationDeniedException.class)
    public ResponseEntity<GenericResponse> handleAuthorizationDeniedException(org.springframework.security.authorization.AuthorizationDeniedException ex) {
        log.error("Autorizacion denegada (403 Forbidden): {}", ex.getMessage());
        GenericResponse response = new GenericResponse();
        response.setCodigo(HttpStatus.FORBIDDEN.value());
        response.setMensaje("Acceso denegado: No tienes permisos para acceder a este recurso o no te pertenece.");
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleGeneralException(Exception ex) {
        log.error("Error inesperado en la aplicacion: {}", ex.getMessage(), ex);
        GenericResponse response = new GenericResponse();
        response.setCodigo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        String debugMsg = "Error interno: " + ex.getClass().getName() + " - " + ex.getMessage();
        if (ex.getCause() != null) {
            debugMsg += " | Causa: " + ex.getCause().getClass().getName() + " - " + ex.getCause().getMessage();
        }
        response.setMensaje(debugMsg);
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
