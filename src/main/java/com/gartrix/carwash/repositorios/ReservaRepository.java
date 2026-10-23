package com.gartrix.carwash.repositorios;

import com.gartrix.carwash.model.EstadoReserva;
import com.gartrix.carwash.model.Reserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long>, JpaSpecificationExecutor<Reserva> {
    List<Reserva> findByEmpleadoAsignado_IdEmpleadoOrderByFechaAscHoraAsc(Integer idEmpleado);
    List<Reserva> findByEmpleadoAsignadoIsNullAndEstadoInOrderByFechaAscHoraAsc(List<EstadoReserva> estados);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE reserva SET id_empleado_asignado = :empleado " +
        "WHERE id_reserva = :id AND id_empleado_asignado IS NULL " +
        "AND estado IN ('PENDIENTE','CONFIRMADA')", nativeQuery = true)
    int tomarSiLibre(@Param("id") Long id, @Param("empleado") Integer empleado);

    Optional<Reserva> findByCodigo(String codigo);

    Optional<Reserva> findByCodigoAndEmailIgnoreCase(String codigo, String email);

    boolean existsByFechaAndHoraAndEstadoNot(LocalDate fecha, LocalTime hora, EstadoReserva estado);

    List<Reserva> findByFechaAndEstadoNot(LocalDate fecha, EstadoReserva estado);

    List<Reserva> findTop6ByOrderByCreadaEnDesc();

    List<Reserva> findByFechaBetween(LocalDate inicio, LocalDate fin);

    Page<Reserva> findByEstado(EstadoReserva estado, Pageable pageable);

    long countByEstado(EstadoReserva estado);
    long countByFecha(LocalDate fecha);
}
