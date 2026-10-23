package com.gartrix.carwash.controlador;

import com.gartrix.carwash.model.Empleado;
import com.gartrix.carwash.model.EstadoReserva;
import com.gartrix.carwash.model.Reserva;
import com.gartrix.carwash.repositorios.ReservaRepository;
import com.gartrix.carwash.repositorios.UsuarioRepository;
import com.gartrix.carwash.services.ReservaService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

@Controller
@RequestMapping("/trabajador")
public class TrabajadorController {
    private final com.gartrix.carwash.repositorios.ServicioLavadoRepository servicios;
    private final com.gartrix.carwash.repositorios.ArticuloInventarioRepository inventario;
    private final ReservaRepository reservas;
    private final UsuarioRepository usuarios;
    private final ReservaService reservaService;
    public TrabajadorController(ReservaRepository reservas, UsuarioRepository usuarios, ReservaService reservaService,
          com.gartrix.carwash.repositorios.ServicioLavadoRepository servicios,
          com.gartrix.carwash.repositorios.ArticuloInventarioRepository inventario) {
        this.reservas=reservas; this.usuarios=usuarios; this.reservaService=reservaService;
        this.servicios=servicios; this.inventario=inventario;
    }
    private Empleado empleado(Authentication auth) {
        return usuarios.findByUsernameIgnoreCase(auth.getName())
            .filter(u -> u.getIdRol()==1 && Boolean.TRUE.equals(u.getActivo()))
            .map(u -> u.getEmpleado())
            .filter(e -> Boolean.TRUE.equals(e.getActivo()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Cuenta de trabajador inválida."));
    }
    @GetMapping
    public String panel(Authentication auth, Model model) {
        Empleado e=empleado(auth);
        model.addAttribute("empleado", e);
        model.addAttribute("pendientes", reservas.countByEstado(EstadoReserva.PENDIENTE));
        model.addAttribute("misTareas", reservas.findByEmpleadoAsignado_IdEmpleadoOrderByFechaAscHoraAsc(e.getIdEmpleado()));
        model.addAttribute("disponibles", reservas.findByEmpleadoAsignadoIsNullAndEstadoInOrderByFechaAscHoraAsc(
            java.util.List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA)));
        return "trabajador/panel";
    }

    @GetMapping("/servicios")
    public String verServicios(Authentication auth, Model model) {
        empleado(auth);
        model.addAttribute("servicios", servicios.findAll());
        return "trabajador/servicios";
    }
    @GetMapping("/inventario")
    public String verInventario(Authentication auth, Model model) {
        empleado(auth);
        model.addAttribute("articulos", inventario.findAll());
        return "trabajador/inventario";
    }

    @Transactional
    @PostMapping("/reservas/{id}/tomar")
    public String tomar(@PathVariable Long id, Authentication auth, RedirectAttributes flash) {
        Empleado e=empleado(auth);
        // Actualización condicionada en SQL: sólo UNA persona podrá tomar la tarea.
        int actualizadas=reservas.tomarSiLibre(id, e.getIdEmpleado());
        flash.addFlashAttribute(actualizadas==1 ? "mensaje" : "error",
            actualizadas==1 ? "Reserva asignada a tu cuenta." : "La reserva ya fue asignada o ya no está disponible.");
        return "redirect:/trabajador";
    }

    @PostMapping("/reservas/{id}/estado")
    @Transactional
    public String avanzar(@PathVariable Long id, @RequestParam EstadoReserva estado,
                          Authentication auth, RedirectAttributes flash) {
        Empleado e=empleado(auth);
        Reserva reserva=reservas.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (reserva.getEmpleadoAsignado()==null || !e.getIdEmpleado().equals(reserva.getEmpleadoAsignado().getIdEmpleado())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta reserva no está asignada a tu cuenta.");
        }
        var actual=reserva.getEstado();
        boolean valido=(actual==EstadoReserva.PENDIENTE && estado==EstadoReserva.CONFIRMADA)
            || (actual==EstadoReserva.CONFIRMADA && estado==EstadoReserva.EN_PROCESO)
            || (actual==EstadoReserva.EN_PROCESO && estado==EstadoReserva.COMPLETADA);
        if (!valido) {
            flash.addFlashAttribute("error", "Cambio de estado no permitido. Sigue el orden de trabajo.");
        } else {
            try {
                reservaService.cambiarEstado(id, estado);
                flash.addFlashAttribute("mensaje", "Atención actualizada.");
            } catch (IllegalArgumentException ex) { flash.addFlashAttribute("error", ex.getMessage()); }
        }
        return "redirect:/trabajador";
    }
}
