package com.gartrix.carwash;

import com.gartrix.carwash.config.SecurityConfig;
import com.gartrix.carwash.model.Empleado;
import com.gartrix.carwash.model.Usuario;
import com.gartrix.carwash.repositorios.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityConfigTest {
    private Usuario cuenta(String nombre, Integer rol, boolean activo) {
        Empleado e = new Empleado("Juan", "Prueba", "987654321", true,
            LocalDate.now(), BigDecimal.ZERO, "Indefinido");
        return new Usuario(e, nombre, "$2a$10$prueba", rol, activo);
    }
    @Test void trabajadorRegistradoPuedeAutenticarseConSuRol() {
        UsuarioRepository repo=mock(UsuarioRepository.class);
        when(repo.findByUsernameIgnoreCase("test")).thenReturn(Optional.of(cuenta("test",1,true)));
        var user=new SecurityConfig().userDetailsService(repo).loadUserByUsername("test");
        assertTrue(user.isEnabled());
        assertTrue(user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TRABAJADOR")));
        assertFalse(user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }
    @Test void adminTieneRolAdmin() {
        UsuarioRepository repo=mock(UsuarioRepository.class);
        when(repo.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(cuenta("admin",2,true)));
        var user=new SecurityConfig().userDetailsService(repo).loadUserByUsername("admin");
        assertTrue(user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }
    @Test void cuentaInactivaNoPuedeEntrar() {
        UsuarioRepository repo=mock(UsuarioRepository.class);
        when(repo.findByUsernameIgnoreCase("test")).thenReturn(Optional.of(cuenta("test",1,false)));
        assertFalse(new SecurityConfig().userDetailsService(repo).loadUserByUsername("test").isEnabled());
    }
    @Test void cuentaInexistenteEsRechazada() {
        UsuarioRepository repo=mock(UsuarioRepository.class);
        assertThrows(UsernameNotFoundException.class,
            () -> new SecurityConfig().userDetailsService(repo).loadUserByUsername("desconocido"));
    }
}
