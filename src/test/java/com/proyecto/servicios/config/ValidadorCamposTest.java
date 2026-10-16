package com.proyecto.servicios.config;

import com.proyecto.servicios.model.GenericResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Modulo Validaciones - ValidadorCampos")
class ValidadorCamposTest {

    private final ValidadorCampos validador = new ValidadorCampos();

    @Test
    @DisplayName("TC-VAL-01: Procesa errores de campos y retorna 400 Bad Request con GenericResponse")
    void testDtoValidationConErrores() {
        BindingResult errors = mock(BindingResult.class);
        FieldError error1 = new FieldError("clienteRegistroDTO", "correoElectronico", "Formato de correo no valido");
        FieldError error2 = new FieldError("clienteRegistroDTO", "nombre", "El nombre es obligatorio");
        when(errors.getFieldErrors()).thenReturn(List.of(error1, error2));

        ResponseEntity<GenericResponse> response = validador.dtoValidation(errors);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getCodigo());
        assertTrue(response.getBody().getMensaje().contains("Datos no validos"));
        assertTrue(response.getBody().getMensaje().contains("correoElectronico=Formato de correo no valido"));
        assertTrue(response.getBody().getMensaje().contains("nombre=El nombre es obligatorio"));
    }
}