package com.gartrix.carwash.services;

import com.gartrix.carwash.model.Empleado;
import com.gartrix.carwash.repositorios.EmpleadoRepository;
import com.gartrix.carwash.repositorios.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class EmpleadoService {
    private final EmpleadoRepository empleados;
    private final UsuarioRepository usuarios;
    public EmpleadoService(EmpleadoRepository empleados, UsuarioRepository usuarios) {
        this.empleados=empleados; this.usuarios=usuarios;
    }
    public Empleado guardarEmpleado(Empleado e) { return empleados.save(e); }
    public List<Empleado> obtenerTodos() { return empleados.findAll(); }
    public Optional<Empleado> obtenerPorId(Integer id) { return empleados.findById(id); }

    @Transactional
    public void eliminarEmpleado(Integer id) {
        Empleado empleado=empleados.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado."));
        var cuenta = usuarios.findByEmpleado_IdEmpleado(id);
        if (cuenta.isPresent() && "admin".equalsIgnoreCase(cuenta.get().getUsername()))
            throw new IllegalArgumentException("No puedes eliminar al empleado administrador principal.");
        // Eliminar primero la cuenta asociada, después el empleado, dentro de la misma transacción.
        cuenta.ifPresent(u -> { usuarios.delete(u); usuarios.flush(); });
        empleados.delete(empleado);
        empleados.flush();
    }
}
