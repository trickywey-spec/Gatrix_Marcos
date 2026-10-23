package com.gartrix.carwash.repositorios;
import com.gartrix.carwash.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PagoRepository extends JpaRepository<Pago,Long> {
 boolean existsByReserva_IdReserva(Long idReserva);
 List<Pago> findAllByOrderByRegistradoEnDesc();
}
