package com.gartrix.carwash;

import com.gartrix.carwash.model.*;
import com.gartrix.carwash.repositorios.*;
import com.gartrix.carwash.services.PagoService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PagoNegocioTest {
    @Test
    void rechazaCobroDuplicado() {
        var pagos = mock(PagoRepository.class);
        var reservas = mock(ReservaRepository.class);
        var reserva = new Reserva(); reserva.setEstado(EstadoReserva.CONFIRMADA);
        when(reservas.findById(1L)).thenReturn(Optional.of(reserva));
        when(pagos.existsByReserva_IdReserva(1L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class,
                () -> new PagoService(pagos, reservas).registrar(1L, "YAPE"));
        verify(pagos, never()).save(any());
    }

    @Test
    void guardaImporteDeLaReservaNoMontoManipulado() {
        var pagos = mock(PagoRepository.class);
        var reservas = mock(ReservaRepository.class);
        var reserva = new Reserva(); reserva.setEstado(EstadoReserva.COMPLETADA);
        reserva.setPrecioTotal(new BigDecimal("85.00"));
        when(reservas.findById(2L)).thenReturn(Optional.of(reserva));
        when(pagos.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        var pago = new PagoService(pagos, reservas).registrar(2L, "EFECTIVO");
        assertEquals(new BigDecimal("85.00"), pago.getMonto());
        assertEquals("EFECTIVO", pago.getMetodoPago());
    }

    @Test
    void noCobraReservasCanceladas() {
        var pagos = mock(PagoRepository.class);
        var reservas = mock(ReservaRepository.class);
        var reserva = new Reserva(); reserva.setEstado(EstadoReserva.CANCELADA);
        when(reservas.findById(3L)).thenReturn(Optional.of(reserva));
        assertThrows(IllegalArgumentException.class,
                () -> new PagoService(pagos, reservas).registrar(3L, "PLIN"));
    }
}
