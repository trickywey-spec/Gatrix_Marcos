package com.gartrix.carwash;

import com.gartrix.carwash.model.*;
import com.gartrix.carwash.repositorios.*;
import com.gartrix.carwash.services.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EliminacionRegistrosTest {
    @Test void eliminarUsuarioBorraSuCuentaPeroNoElEmpleado() {
        var usuarios=mock(UsuarioRepository.class);
        var e=new Empleado(); e.setIdEmpleado(8);
        var u=new Usuario(e,"trabajador","hash",1,true);u.setIdUsuario(25);
        when(usuarios.findById(25)).thenReturn(Optional.of(u));
        new UsuarioService(usuarios,new BCryptPasswordEncoder()).eliminarUsuario(25);
        verify(usuarios).delete(u);
        verify(usuarios).flush();
    }
    @Test void eliminarEmpleadoBorraPrimeroSuUsuarioVinculado() {
        var empleados=mock(EmpleadoRepository.class);
        var usuarios=mock(UsuarioRepository.class);
        var e=new Empleado();e.setIdEmpleado(8);
        var u=new Usuario(e,"trabajador","hash",1,true);
        when(empleados.findById(8)).thenReturn(Optional.of(e));
        when(usuarios.findByEmpleado_IdEmpleado(8)).thenReturn(Optional.of(u));
        new EmpleadoService(empleados,usuarios).eliminarEmpleado(8);
        var orden=inOrder(usuarios,empleados);
        orden.verify(usuarios).delete(u);
        orden.verify(usuarios).flush();
        orden.verify(empleados).delete(e);
        orden.verify(empleados).flush();
    }
    @Test void protegeCuentaPrincipal() {
        var empleados=mock(EmpleadoRepository.class);
        var usuarios=mock(UsuarioRepository.class);
        var e=new Empleado();e.setIdEmpleado(1);
        var u=new Usuario(e,"admin","hash",2,true);
        when(empleados.findById(1)).thenReturn(Optional.of(e));
        when(usuarios.findByEmpleado_IdEmpleado(1)).thenReturn(Optional.of(u));
        assertThrows(IllegalArgumentException.class,
            () -> new EmpleadoService(empleados,usuarios).eliminarEmpleado(1));
        verify(empleados,never()).delete(any());
    }
}
