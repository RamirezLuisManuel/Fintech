package com.proyecto.servicios.config;

import com.proyecto.servicios.exception.*;
import com.proyecto.servicios.model.GenericResponse;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(OnboardingException.class)
    public ResponseEntity<GenericResponse<Object>> handleOnboardingException(OnboardingException ex) {
        log.warn("Excepcion de negocio (Onboarding): {}", ex.getMessage());
        return new ResponseEntity<>(GenericResponse.errorNegocio(ex.getMessage()), ex.getStatus());
    }

    // Excepciones de negocio personalizadas dedicadas (Codigo 2)
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<GenericResponse<Object>> handleRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return new ResponseEntity<>(GenericResponse.errorNegocio(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DatoDuplicadoException.class)
    public ResponseEntity<GenericResponse<Object>> handleDatoDuplicado(DatoDuplicadoException ex) {
        log.warn("Dato duplicado o en conflicto: {}", ex.getMessage());
        return new ResponseEntity<>(GenericResponse.errorNegocio(ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(RecursoInactivoException.class)
    public ResponseEntity<GenericResponse<Object>> handleRecursoInactivo(RecursoInactivoException ex) {
        log.warn("Recurso inactivo / dado de baja: {}", ex.getMessage());
        return new ResponseEntity<>(GenericResponse.errorNegocio(ex.getMessage()), HttpStatus.GONE);
    }

    @ExceptionHandler(OperacionNoPermitidaException.class)
    public ResponseEntity<GenericResponse<Object>> handleOperacionNoPermitida(OperacionNoPermitidaException ex) {
        log.warn("Operacion no permitida: {}", ex.getMessage());
        return new ResponseEntity<>(GenericResponse.errorNegocio(ex.getMessage()), HttpStatus.FORBIDDEN);
    }

    // Manejo de validaciones de DTOs (@Valid) con codigo 1 y HTTP 400
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse<java.util.Map<String, String>>> handleValidationException(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        java.util.Map<String, String> listError = new java.util.HashMap<>();
        for (org.springframework.validation.FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            listError.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        GenericResponse<java.util.Map<String, String>> genericResponse = new GenericResponse<>();
        genericResponse.setCodigo(1);
        genericResponse.setMensaje("Datos no validos: " + listError);
        genericResponse.setData(listError);
        return new ResponseEntity<>(genericResponse, org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse<Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.error("Error al leer el JSON de la peticion: {}", ex.getMessage());
        return new ResponseEntity<>(
            GenericResponse.errorValidacion("El cuerpo de la peticion esta vacio o el JSON tiene un formato incorrecto (revisa los tipos de dato y el formato de fecha AAAA-MM-DD)."),
            HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<GenericResponse<Object>> handleConstraintViolation(
            jakarta.validation.ConstraintViolationException ex) {
        return new ResponseEntity<>(
                GenericResponse.errorValidacion("Datos no validos: " + ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<GenericResponse<Object>> handleFeignException(FeignException ex) {
        log.error("Error de integracion (FeignException) al llamar al servicio externo: status={}, body={}", ex.status(), ex.contentUTF8());
        if (ex.status() == 401 || ex.status() == 403) {
            return new ResponseEntity<>(
                GenericResponse.errorNegocio("Error de autenticacion al consumir servicio externo (token invalido o expirado)."),
                HttpStatus.UNAUTHORIZED
            );
        }
        HttpStatus status = ex.status() != 0 ? HttpStatus.resolve(ex.status()) : HttpStatus.INTERNAL_SERVER_ERROR;
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        return new ResponseEntity<>(GenericResponse.errorNegocio("Respuesta no exitosa del servicio externo integrado."), status);
    }

    @ExceptionHandler(RetryableException.class)
    public ResponseEntity<GenericResponse<Object>> handleRetryableException(RetryableException ex) {
        log.error("Error de timeout o conexion (RetryableException) con el servicio externo: {}", ex.getMessage());
        return new ResponseEntity<>(
            GenericResponse.errorNegocio("El servicio externo no responde a tiempo (Timeout) o esta inaccesible."),
            HttpStatus.SERVICE_UNAVAILABLE
        );
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<GenericResponse<Object>> handleResponseStatusException(org.springframework.web.server.ResponseStatusException ex) {
        log.error("Error ResponseStatusException: {}", ex.getReason(), ex);
        String msg = ex.getReason() != null ? ex.getReason() : "Ocurrio un error.";
        return new ResponseEntity<>(GenericResponse.errorNegocio(msg), ex.getStatusCode());
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<GenericResponse<Object>> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException ex) {
        log.error("Acceso denegado (403 Forbidden): {}", ex.getMessage());
        return new ResponseEntity<>(
            GenericResponse.errorNegocio("Acceso denegado: No tienes permisos para acceder a este recurso o no te pertenece."),
            HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(org.springframework.security.authorization.AuthorizationDeniedException.class)
    public ResponseEntity<GenericResponse<Object>> handleAuthorizationDeniedException(org.springframework.security.authorization.AuthorizationDeniedException ex) {
        log.error("Autorizacion denegada (403 Forbidden): {}", ex.getMessage());
        return new ResponseEntity<>(
            GenericResponse.errorNegocio("Acceso denegado: No tienes permisos para acceder a este recurso o no te pertenece."),
            HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<GenericResponse<Object>> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        log.warn("Ruta no encontrada (404): {}", ex.getResourcePath());
        return new ResponseEntity<>(
            GenericResponse.errorNegocio("Ruta o endpoint no encontrado: " + ex.getResourcePath()),
            HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<GenericResponse<Object>> handleMethodNotSupported(org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        log.warn("Metodo HTTP no soportado: {}", ex.getMessage());
        return new ResponseEntity<>(
            GenericResponse.errorNegocio(ex.getMessage()),
            HttpStatus.METHOD_NOT_ALLOWED
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse<Object>> handleGeneralException(Exception ex) {
        log.error("Error inesperado en la aplicacion: ", ex);
        return new ResponseEntity<>(
            GenericResponse.errorNegocio("Ha ocurrido un error interno en el servidor. Por favor, contacte al soporte tecnico."),
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
