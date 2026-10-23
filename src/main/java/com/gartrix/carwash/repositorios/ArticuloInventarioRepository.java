package com.gartrix.carwash.repositorios;
import com.gartrix.carwash.model.ArticuloInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ArticuloInventarioRepository extends JpaRepository<ArticuloInventario,Long> {
 List<ArticuloInventario> findAllByOrderByNombreAsc();
}
