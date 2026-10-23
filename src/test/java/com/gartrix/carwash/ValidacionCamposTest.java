package com.gartrix.carwash;

import com.gartrix.carwash.dto.ReservaForm;
import com.gartrix.carwash.model.Empleado;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Comprueba que los datos no se validen solamente en el navegador. */
class ValidacionCamposTest {

    private static jakarta.validation.ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void iniciarValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        factory.close();
    }

    @Test
    void nombreDelClienteSoloAceptaLetrasYEspacios() {
        var reserva = new ReservaForm();
        reserva.setNombreCompleto("José María");
        assertTrue(validator.validateProperty(reserva, "nombreCompleto").isEmpty());
        reserva.setNombreCompleto("José123!");
        assertFalse(validator.validateProperty(reserva, "nombreCompleto").isEmpty());
    }

    @Test
    void telefonoDebeTenerNueveDigitos() {
        var reserva = new ReservaForm();
        reserva.setTelefono("987654321");
        assertTrue(validator.validateProperty(reserva, "telefono").isEmpty());
        reserva.setTelefono("987-654-321");
        assertFalse(validator.validateProperty(reserva, "telefono").isEmpty());
    }

    @Test
    void nombresDeEmpleadoNoAdmitenSimbolos() {
        var empleado = new Empleado();
        empleado.setNombres("María Elena");
        assertTrue(validator.validateProperty(empleado, "nombres").isEmpty());
        empleado.setNombres("María @ Elena");
        assertFalse(validator.validateProperty(empleado, "nombres").isEmpty());
    }

    @Test
    void placaAlfanumericaConGuionEsValida() {
        var reserva = new ReservaForm();
        reserva.setPlaca("ABC-123");
        assertTrue(validator.validateProperty(reserva, "placa").isEmpty());
        reserva.setPlaca("ABC@123");
        assertFalse(validator.validateProperty(reserva, "placa").isEmpty());
    }
}
