package com.gartrix.carwash.services;

import com.gartrix.carwash.dto.ReservaForm;
import com.gartrix.carwash.model.EstadoReserva;
import com.gartrix.carwash.model.Reserva;
import com.gartrix.carwash.model.ServicioLavado;
import com.gartrix.carwash.repositorios.ReservaRepository;
import com.gartrix.carwash.repositorios.PagoRepository;
import com.gartrix.carwash.repositorios.ServicioLavadoRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class ReservaService {
    public static final ZoneId ZONA = ZoneId.of("America/Lima");

    private final ReservaRepository reservas;
    private final ServicioLavadoRepository servicios;
    private final EntityManager entityManager;
    private final PagoRepository pagos;

    public ReservaService(ReservaRepository reservas, ServicioLavadoRepository servicios,
                          EntityManager entityManager, PagoRepository pagos) {
        this.reservas = reservas;
        this.servicios = servicios;
        this.entityManager = entityManager;
        this.pagos = pagos;
    }

    public static List<LocalTime> horarios(LocalDate dia) {
        if (dia == null) return List.of();
        return (switch (dia.getDayOfWeek()) {
            case SATURDAY -> List.of(8, 10, 12, 14, 16);
            case SUNDAY -> List.of(9, 11, 13, 15);
            default -> List.of(8, 10, 12, 14, 16, 18);
        }).stream().map(hora -> LocalTime.of(hora, 0)).toList();
    }

    @Transactional(readOnly = true)
    public List<LocalTime> horariosDisponibles(LocalDate fecha) {
        if (fecha == null || fecha.isBefore(LocalDate.now(ZONA))) return List.of();
        var ocupados = reservas.findByFechaAndEstadoNot(fecha, EstadoReserva.CANCELADA)
                .stream().map(Reserva::getHora).toList();
        return horarios(fecha).stream()
                .filter(hora -> !ocupados.contains(hora))
                .filter(hora -> !fecha.equals(LocalDate.now(ZONA)) || hora.isAfter(LocalTime.now(ZONA)))
                .toList();
    }

    @Transactional
    public Reserva crear(ReservaForm form) {
        validarTurno(form);
        bloquearTurno(form);
        if (reservas.existsByFechaAndHoraAndEstadoNot(form.getFecha(), form.getHora(), EstadoReserva.CANCELADA)) {
            throw new IllegalArgumentException("Ese turno ya está ocupado. Elige otra hora.");
        }
        var reserva = new Reserva();
        aplicarDatos(reserva, form);
        return reservas.save(reserva);
    }

    /** Permite corregir datos y reagendar reservas pendientes o confirmadas.
     * Los cobros y las atenciones terminadas quedan protegidos de cambios retroactivos. */
    @Transactional
    public Reserva editar(Long id, ReservaForm form) {
        var reserva = reservas.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe esa reserva."));
        if (reserva.getEstado() != EstadoReserva.PENDIENTE
                && reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new IllegalArgumentException("Solo se pueden editar reservas pendientes o confirmadas.");
        }
        if (pagos.existsByReserva_IdReserva(id)) {
            throw new IllegalArgumentException("No se puede modificar una reserva que ya tiene pago registrado.");
        }
        validarTurno(form);
        bloquearTurno(form);
        boolean ocupadoPorOtro = reservas.findByFechaAndEstadoNot(form.getFecha(), EstadoReserva.CANCELADA)
                .stream().anyMatch(otra -> !otra.getIdReserva().equals(id)
                        && otra.getHora().equals(form.getHora()));
        if (ocupadoPorOtro) {
            throw new IllegalArgumentException("Ese turno ya está ocupado por otra reserva.");
        }
        aplicarDatos(reserva, form);
        return reservas.save(reserva);
    }

    private void validarTurno(ReservaForm form) {
        LocalDate hoy = LocalDate.now(ZONA);
        if (form.getFecha() == null || form.getFecha().isBefore(hoy)) {
            throw new IllegalArgumentException("Selecciona una fecha actual o futura.");
        }
        if (form.getHora() == null || !horarios(form.getFecha()).contains(form.getHora())) {
            throw new IllegalArgumentException("Selecciona un turno válido.");
        }
        if (form.getFecha().equals(hoy) && !form.getHora().isAfter(LocalTime.now(ZONA))) {
            throw new IllegalArgumentException("El horario seleccionado ya pasó.");
        }
        if (form.getServicioIds() == null || form.getServicioIds().isEmpty()) {
            throw new IllegalArgumentException("Elige al menos un servicio.");
        }
        if (form.getCantidadVehiculos() == null || form.getCantidadVehiculos() < 1
                || form.getCantidadVehiculos() > 10) {
            throw new IllegalArgumentException("Puedes reservar entre 1 y 10 vehículos.");
        }
    }

    private void bloquearTurno(ReservaForm form) {
        // Bloqueo transaccional PostgreSQL: dos solicitudes simultáneas no ocupan el mismo turno.
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(:turno))")
                .setParameter("turno", form.getFecha() + "_" + form.getHora())
                .getSingleResult();
    }

    private void aplicarDatos(Reserva reserva, ReservaForm form) {
        var ids = form.getServicioIds().stream().distinct().toList();
        List<ServicioLavado> elegidos = servicios.findAllById(ids);
        if (elegidos.size() != ids.size()
                || elegidos.stream().anyMatch(s -> !Boolean.TRUE.equals(s.getActivo()))) {
            throw new IllegalArgumentException("Uno o más servicios ya no están disponibles.");
        }
        reserva.setNombreCompleto(form.getNombreCompleto().trim());
        reserva.setEmail(form.getEmail().trim());
        reserva.setTelefono(form.getTelefono().trim());
        reserva.setTipoVehiculo(form.getTipoVehiculo().trim());
        reserva.setPlaca(form.getPlaca() == null ? null : form.getPlaca().trim().toUpperCase());
        reserva.setFecha(form.getFecha());
        reserva.setHora(form.getHora());
        reserva.setComentarios(form.getComentarios());
        reserva.setServicios(elegidos);
        reserva.setCantidadVehiculos(form.getCantidadVehiculos());
        var precio = elegidos.stream().map(ServicioLavado::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        reserva.setPrecioTotal(precio.multiply(BigDecimal.valueOf(form.getCantidadVehiculos())));
    }

    @Transactional
    public void cambiarEstado(Long id, EstadoReserva nuevo) {
        Reserva reserva = reservas.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe esa reserva."));
        EstadoReserva actual = reserva.getEstado();
        if (nuevo == EstadoReserva.CANCELADA && pagos.existsByReserva_IdReserva(id)) {
            throw new IllegalArgumentException("Una reserva cobrada no se puede cancelar; registra la devolución antes.");
        }
        boolean permitido = switch (actual) {
            case PENDIENTE -> nuevo == EstadoReserva.CONFIRMADA || nuevo == EstadoReserva.CANCELADA;
            case CONFIRMADA -> nuevo == EstadoReserva.EN_PROCESO || nuevo == EstadoReserva.CANCELADA;
            case EN_PROCESO -> nuevo == EstadoReserva.COMPLETADA;
            case COMPLETADA, CANCELADA -> false;
        };
        if (!permitido) throw new IllegalArgumentException("Cambio de estado no permitido: " + actual + " → " + nuevo);
        reserva.setEstado(nuevo);
    }
}
