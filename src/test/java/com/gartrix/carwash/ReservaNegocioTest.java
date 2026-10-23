package com.gartrix.carwash;

import com.gartrix.carwash.dto.ReservaForm;
import com.gartrix.carwash.model.*;
import com.gartrix.carwash.repositorios.*;
import com.gartrix.carwash.services.ReservaService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Pruebas del flujo de negocio con repositorios simulados, sin depender de PostgreSQL. */
class ReservaNegocioTest {
    private ReservaRepository reservas;
    private ServicioLavadoRepository servicios;
    private PagoRepository pagos;
    private ReservaService negocio;
    private LocalDate fecha;

    @BeforeEach
    void preparar() {
        reservas = mock(ReservaRepository.class);
        servicios = mock(ServicioLavadoRepository.class);
        pagos = mock(PagoRepository.class);
        EntityManager em = mock(EntityManager.class);
        Query q = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(q);
        when(q.setParameter(eq("turno"), any())).thenReturn(q);
        when(q.getSingleResult()).thenReturn(1);
        negocio = new ReservaService(reservas, servicios, em, pagos);
        fecha = LocalDate.now(ReservaService.ZONA).plusWeeks(2)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    }

    private ReservaForm form() {
        var form = new ReservaForm();
        form.setNombreCompleto("Ana María");
        form.setEmail("ana@example.com");
        form.setTelefono("987654321");
        form.setTipoVehiculo("Sedán / Hatchback");
        form.setCantidadVehiculos(2);
        form.setFecha(fecha);
        form.setHora(LocalTime.of(10, 0));
        form.setServicioIds(List.of(2L, 3L));
        return form;
    }

    @Test
    void creaReservaYCalculaPrecioSinConfiarEnElCliente() {
        var a = new ServicioLavado(); a.setId(2L); a.setNombre("Interior");
        a.setPrecio(new BigDecimal("20.00"));
        var b = new ServicioLavado(); b.setId(3L); b.setNombre("Exterior");
        b.setPrecio(new BigDecimal("25.00"));
        when(servicios.findAllById(anyIterable())).thenReturn(List.of(a,b));
        when(reservas.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        var resultado = negocio.crear(form());
        assertEquals(new BigDecimal("90.00"), resultado.getPrecioTotal());
        assertEquals(2, resultado.getServicios().size());
        assertEquals("Ana María", resultado.getNombreCompleto());
    }

    @Test
    void rechazaReservaDeTurnoYaOcupado() {
        when(reservas.existsByFechaAndHoraAndEstadoNot(eq(fecha), eq(LocalTime.of(10, 0)), any()))
                .thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> negocio.crear(form()));
        verify(reservas, never()).save(any());
    }

    @Test
    void rechazaFechaPasadaYServicioNoSeleccionado() {
        var datos = form();
        datos.setFecha(LocalDate.now(ReservaService.ZONA).minusDays(1));
        assertThrows(IllegalArgumentException.class, () -> negocio.crear(datos));
        datos.setFecha(fecha);
        datos.setServicioIds(List.of());
        assertThrows(IllegalArgumentException.class, () -> negocio.crear(datos));
    }

    @Test
    void edicionNoPermiteUsarTurnoDeOtroCliente() {
        var existente = new Reserva(); existente.setIdReserva(4L);
        existente.setEstado(EstadoReserva.PENDIENTE);
        when(reservas.findById(4L)).thenReturn(Optional.of(existente));
        var otra = new Reserva(); otra.setIdReserva(5L);
        otra.setHora(LocalTime.of(10, 0));
        when(reservas.findByFechaAndEstadoNot(eq(fecha), eq(EstadoReserva.CANCELADA)))
                .thenReturn(List.of(otra));
        assertThrows(IllegalArgumentException.class, () -> negocio.editar(4L, form()));
    }

    @Test
    void noModificaReservasCobradasOCerradas() {
        var existente = new Reserva(); existente.setIdReserva(4L);
        existente.setEstado(EstadoReserva.PENDIENTE);
        when(reservas.findById(4L)).thenReturn(Optional.of(existente));
        when(pagos.existsByReserva_IdReserva(4L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> negocio.editar(4L, form()));
        existente.setEstado(EstadoReserva.COMPLETADA);
        assertThrows(IllegalArgumentException.class, () -> negocio.editar(4L, form()));
    }

    @Test
    void noCancelaReservaQueYaSeHaCobrado() {
        var reserva = new Reserva(); reserva.setIdReserva(6L);
        reserva.setEstado(EstadoReserva.PENDIENTE);
        when(reservas.findById(6L)).thenReturn(Optional.of(reserva));
        when(pagos.existsByReserva_IdReserva(6L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class,
                () -> negocio.cambiarEstado(6L, EstadoReserva.CANCELADA));
        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
    }

    @Test
    void sigueFlujoValidoDeEstados() {
        var reserva = new Reserva(); reserva.setIdReserva(6L);
        reserva.setEstado(EstadoReserva.PENDIENTE);
        when(reservas.findById(6L)).thenReturn(Optional.of(reserva));
        negocio.cambiarEstado(6L, EstadoReserva.CONFIRMADA);
        negocio.cambiarEstado(6L, EstadoReserva.EN_PROCESO);
        negocio.cambiarEstado(6L, EstadoReserva.COMPLETADA);
        assertEquals(EstadoReserva.COMPLETADA, reserva.getEstado());
        assertThrows(IllegalArgumentException.class,
                () -> negocio.cambiarEstado(6L, EstadoReserva.PENDIENTE));
    }
}
