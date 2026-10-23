package com.gartrix.carwash.controlador;

import com.gartrix.carwash.model.Empleado;
import com.gartrix.carwash.repositorios.UsuarioRepository;
import com.gartrix.carwash.services.EmpleadoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/empleados")
public class EmpleadoWebController {
    private final EmpleadoService empleados;
    private final UsuarioRepository usuarios;

    public EmpleadoWebController(EmpleadoService empleados, UsuarioRepository usuarios) {
        this.empleados = empleados;
        this.usuarios = usuarios;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("empleados", empleados.obtenerTodos());
        return "empleados/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("empleado", new Empleado());
        return "empleados/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        model.addAttribute("empleado", empleados.obtenerPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return "empleados/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("empleado") Empleado empleado,
                          BindingResult errores, RedirectAttributes flash) {
        if (errores.hasErrors()) return "empleados/formulario";
        if (empleado.getIdEmpleado() != null) {
            var actual = empleados.obtenerPorId(empleado.getIdEmpleado())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            actual.setNombres(empleado.getNombres());
            actual.setApellidos(empleado.getApellidos());
            actual.setTelefono(empleado.getTelefono());
            actual.setFechaContrato(empleado.getFechaContrato());
            actual.setSalarioBase(empleado.getSalarioBase());
            actual.setTipoContrato(empleado.getTipoContrato());
            // No desactivar accidentalmente al empleado que controla la cuenta principal.
            boolean admin = usuarios.findByUsername("admin")
                    .filter(u -> u.getEmpleado() != null && u.getEmpleado().getIdEmpleado().equals(actual.getIdEmpleado()))
                    .isPresent();
            actual.setActivo(admin ? true : empleado.getActivo());
            empleados.guardarEmpleado(actual);
        } else empleados.guardarEmpleado(empleado);
        flash.addFlashAttribute("mensaje", "Empleado guardado correctamente.");
        return "redirect:/empleados";
    }

    @PostMapping("/{id}/alternar")
    public String alternar(@PathVariable Integer id, RedirectAttributes flash) {
        var empleado = empleados.obtenerPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean admin = usuarios.findByUsername("admin")
                .filter(u -> u.getEmpleado() != null && u.getEmpleado().getIdEmpleado().equals(id))
                .isPresent();
        if (admin) {
            flash.addFlashAttribute("error", "No puedes desactivar al empleado administrador.");
        } else {
            empleado.setActivo(!Boolean.TRUE.equals(empleado.getActivo()));
            empleados.guardarEmpleado(empleado);
            flash.addFlashAttribute("mensaje", "Estado del empleado actualizado.");
        }
        return "redirect:/empleados";
    }
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            empleados.eliminarEmpleado(id);
            flash.addFlashAttribute("mensaje", "Empleado eliminado de PostgreSQL junto con su cuenta de acceso, si existía.");
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", "No se puede eliminar este empleado porque existen datos históricos relacionados. Puedes desactivarlo.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/empleados";
    }
}
