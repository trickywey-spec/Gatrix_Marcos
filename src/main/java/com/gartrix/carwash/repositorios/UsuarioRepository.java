package com.gartrix.carwash.repositorios;

import com.gartrix.carwash.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByUsernameIgnoreCase(String username);
    Optional<Usuario> findByEmpleado_IdEmpleado(Integer empleadoId);

    List<Usuario> findByActivo(Boolean activo);

    List<Usuario> findByUsernameContainingIgnoreCase(String texto);
}