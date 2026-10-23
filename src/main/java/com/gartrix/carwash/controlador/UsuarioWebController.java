package com.gartrix.carwash.controlador;

import com.gartrix.carwash.dto.UsuarioForm;
import com.gartrix.carwash.model.Usuario;
import com.gartrix.carwash.repositorios.EmpleadoRepository;
import com.gartrix.carwash.repositorios.UsuarioRepository;
import com.gartrix.carwash.services.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
public class UsuarioWebController {
    private final UsuarioService servicio;
    private final UsuarioRepository usuarios;
    private final EmpleadoRepository empleados;

    public UsuarioWebController(UsuarioService servicio, UsuarioRepository usuarios,
                                EmpleadoRepository empleados) {
        this.servicio = servicio;
        this.usuarios = usuarios;
        this.empleados = empleados;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", servicio.obtenerTodos());
        return "usuarios";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("usuarioForm", new UsuarioForm());
        return preparar(model);
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        Usuario usuario = servicio.obtenerPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if ("admin".equals(usuario.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta principal está protegida.");
        }
        var form = new UsuarioForm();
        form.setIdUsuario(id);
        form.setIdEmpleado(usuario.getEmpleado().getIdEmpleado());
        form.setUsername(usuario.getUsername());
        form.setIdRol(usuario.getIdRol());
        form.setActivo(Boolean.TRUE.equals(usuario.getActivo()));
        model.addAttribute("usuarioForm", form);
        return preparar(model);
    }

    private String preparar(Model model) {
        model.addAttribute("empleados", empleados.findAll());
        return "formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("usuarioForm") UsuarioForm form,
                          BindingResult errores, Model model, RedirectAttributes flash) {
        var usuarioActual = form.getIdUsuario() == null ? new Usuario()
                : servicio.obtenerPorId(form.getIdUsuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if ("admin".equals(usuarioActual.getUsername())) {
            errores.reject("cuenta.protegida", "La cuenta principal no se puede modificar aquí.");
        }
        var clave = form.getPassword() == null ? "" : form.getPassword();
        if ((form.getIdUsuario() == null || !clave.isBlank()) && clave.length() < 8) {
            errores.rejectValue("password", "clave.corta", "La contraseña debe tener al menos 8 caracteres.");
        }
        if ("admin".equalsIgnoreCase(form.getUsername())) {
            errores.rejectValue("username", "reservado", "El nombre admin está reservado.");
        }
        if (form.getUsername() != null) {
            usuarios.findByUsernameIgnoreCase(form.getUsername()).ifPresent(otro -> {
                if (!otro.getIdUsuario().equals(form.getIdUsuario())) {
                    errores.rejectValue("username", "duplicado", "Ya existe ese nombre de usuario.");
                }
            });
        }
        var empleado = form.getIdEmpleado() == null ? null : empleados.findById(form.getIdEmpleado()).orElse(null);
        if (empleado == null) {
            errores.rejectValue("idEmpleado", "invalido", "Selecciona un empleado existente.");
        } else {
            usuarios.findAll().stream()
                    .filter(otro -> otro.getEmpleado() != null
                            && otro.getEmpleado().getIdEmpleado().equals(empleado.getIdEmpleado())
                            && !otro.getIdUsuario().equals(form.getIdUsuario()))
                    .findFirst().ifPresent(otro -> errores.rejectValue("idEmpleado", "ocupado",
                            "Ese empleado ya tiene una cuenta."));
        }
        if (errores.hasErrors()) return preparar(model);

        usuarioActual.setEmpleado(empleado);
        usuarioActual.setUsername(form.getUsername().trim());
        usuarioActual.setActivo(form.isActivo());
        usuarioActual.setIdRol(form.getIdRol());
        if (!clave.isBlank()) usuarioActual.setPasswordHash(clave);
        servicio.guardarUsuario(usuarioActual);
        flash.addFlashAttribute("mensaje", "Usuario guardado correctamente.");
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/alternar")
    public String alternar(@PathVariable Integer id, RedirectAttributes flash) {
        var usuario = servicio.obtenerPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if ("admin".equals(usuario.getUsername())) {
            flash.addFlashAttribute("errorUsuario", "La cuenta admin no puede desactivarse.");
        } else {
            usuario.setActivo(!Boolean.TRUE.equals(usuario.getActivo()));
            usuarios.save(usuario);
            flash.addFlashAttribute("mensaje", "Estado de cuenta actualizado.");
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes flash) {
        var usuario = servicio.obtenerPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if ("admin".equals(usuario.getUsername())) {
            flash.addFlashAttribute("errorUsuario", "La cuenta principal no puede eliminarse.");
        } else {
            try {
                servicio.eliminarUsuario(id);
                flash.addFlashAttribute("mensaje", "Usuario eliminado definitivamente de PostgreSQL. El empleado permanece registrado.");
            } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                flash.addFlashAttribute("errorUsuario", "No se puede eliminar: existen registros relacionados. Desactiva la cuenta si necesitas conservar el historial.");
            } catch (IllegalArgumentException ex) {
                flash.addFlashAttribute("errorUsuario", ex.getMessage());
            }
        }
        return "redirect:/usuarios";
    }
}
