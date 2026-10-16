package com.proyecto.servicios.config;

import com.proyecto.servicios.model.GenericResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ValidadorCampos {

    /**
     * Intercepta la excepcion MethodArgumentNotValidException lanzada cuando falla @Valid.
     * En Spring @ExceptionHandler debe recibir MethodArgumentNotValidException ex,
     * y extraer de ella el BindingResult/Errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> handleValidationException(MethodArgumentNotValidException ex) {
        return dtoValidation(ex.getBindingResult());
    }

    public ResponseEntity<GenericResponse> dtoValidation(Errors errors) {
        Map<String, String> listError = new HashMap<>();
        for (FieldError fieldError : errors.getFieldErrors()) {
            listError.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        GenericResponse<Map<String, String>> genericResponse = new GenericResponse<>();
        genericResponse.setCodigo(1);
        genericResponse.setMensaje("Datos no validos: " + listError);
        genericResponse.setData(listError);
        return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
    }
}
