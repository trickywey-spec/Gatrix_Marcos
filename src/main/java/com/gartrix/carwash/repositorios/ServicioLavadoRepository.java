package com.gartrix.carwash.repositorios;
import com.gartrix.carwash.model.ServicioLavado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ServicioLavadoRepository extends JpaRepository<ServicioLavado,Long> { List<ServicioLavado> findByActivoTrueOrderByIdAsc(); List<ServicioLavado> findByActivoTrueOrderByNombreAsc(); }
