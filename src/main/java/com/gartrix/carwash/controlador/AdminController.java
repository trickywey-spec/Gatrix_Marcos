package com.gartrix.carwash.controlador;

import com.gartrix.carwash.model.ArticuloInventario;
import com.gartrix.carwash.dto.ReservaForm;
import com.gartrix.carwash.model.EstadoReserva;
import com.gartrix.carwash.model.Reserva;
import com.gartrix.carwash.model.ServicioLavado;
import com.gartrix.carwash.repositorios.ArticuloInventarioRepository;
import com.gartrix.carwash.repositorios.PagoRepository;
import com.gartrix.carwash.repositorios.ReservaRepository;
import com.gartrix.carwash.repositorios.ServicioLavadoRepository;
import com.gartrix.carwash.services.PagoService;
import com.gartrix.carwash.services.ReservaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final com.gartrix.carwash.repositorios.EmpleadoRepository empleados;
    private final com.gartrix.carwash.repositorios.UsuarioRepository cuentas;
    private final ReservaRepository reservas;
    private final ReservaService reservaService;
    private final ServicioLavadoRepository servicios;
    private final ArticuloInventarioRepository inventario;
    private final PagoRepository pagos;
    private final PagoService pagoService;

    public AdminController(ReservaRepository reservas, ReservaService reservaService,
                           ServicioLavadoRepository servicios, ArticuloInventarioRepository inventario,
                           PagoRepository pagos, PagoService pagoService,
                           com.gartrix.carwash.repositorios.EmpleadoRepository empleados,
                           com.gartrix.carwash.repositorios.UsuarioRepository cuentas) {
        this.reservas = reservas;
        this.reservaService = reservaService;
        this.servicios = servicios;
        this.inventario = inventario;
        this.cuentas = cuentas;
        this.empleados = empleados;
        this.pagos = pagos;
        this.pagoService = pagoService;
    }

    public record PuntoGrafico(String dia, long cantidad, int porcentaje) {}

    @GetMapping
    public String panel(Model model) {
        var hoy = LocalDate.now(ReservaService.ZONA);
        long pendientes = reservas.countByEstado(EstadoReserva.PENDIENTE);
        long confirmadas = reservas.countByEstado(EstadoReserva.CONFIRMADA);
        long enProceso = reservas.countByEstado(EstadoReserva.EN_PROCESO);
        long completadas = reservas.countByEstado(EstadoReserva.COMPLETADA);
        var articulos = inventario.findAll();
        model.addAttribute("pendientes", pendientes);
        model.addAttribute("confirmadas", confirmadas);
        model.addAttribute("enProceso", enProceso);
        model.addAttribute("completadas", completadas);
        model.addAttribute("totalReservas", reservas.count());
        model.addAttribute("reservasHoy", reservas.countByFecha(hoy));
        model.addAttribute("totalCobrado", pagoService.totalCobrado());
        model.addAttribute("bajoStock", articulos.stream()
                .filter(a -> a.getStock() <= a.getMinimo() && Boolean.TRUE.equals(a.getActivo())).count());
        model.addAttribute("ultimas", reservas.findTop6ByOrderByCreadaEnDesc());
        model.addAttribute("inventarioValorizado", articulos.stream()
                .map(a -> a.getCostoUnitario().multiply(BigDecimal.valueOf(a.getStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        var puntos = new ArrayList<PuntoGrafico>();
        long max = 1;
        long[] totales = new long[7];
        for (int i = 0; i < 7; i++) {
            totales[i] = reservas.countByFecha(hoy.minusDays(6L - i));
            max = Math.max(max, totales[i]);
        }
        for (int i = 0; i < 7; i++) {
            var dia = hoy.minusDays(6L - i).getDayOfWeek()
                    .getDisplayName(TextStyle.SHORT, new Locale("es", "PE"));
            puntos.add(new PuntoGrafico(dia, totales[i], (int) Math.round(totales[i] * 100d / max)));
        }
        model.addAttribute("grafico", puntos);
        return "admin/panel";
    }

    @GetMapping("/reservas")
    public String listarReservas(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(required = false) EstadoReserva estado,
                                 @RequestParam(defaultValue = "") String q, Model model) {
        var pagina = PageRequest.of(Math.max(0, page), 10, Sort.by(Sort.Direction.DESC, "creadaEn"));
        Specification<Reserva> filtros = (root, query, criteria) -> {
            var condiciones = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (estado != null) condiciones.add(criteria.equal(root.get("estado"), estado));
            if (!q.isBlank()) {
                String texto = "%" + q.strip().toLowerCase(Locale.ROOT) + "%";
                condiciones.add(criteria.or(
                        criteria.like(criteria.lower(root.get("nombreCompleto")), texto),
                        criteria.like(criteria.lower(root.get("codigo")), texto),
                        criteria.like(criteria.lower(root.get("placa")), texto)));
            }
            return criteria.and(condiciones.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        model.addAttribute("reservas", reservas.findAll(filtros, pagina));
        model.addAttribute("trabajadores", cuentas.findAll().stream()
            .filter(u -> u.getIdRol()==1 && Boolean.TRUE.equals(u.getActivo()) && u.getEmpleado()!=null
                  && Boolean.TRUE.equals(u.getEmpleado().getActivo()))
            .map(u -> u.getEmpleado()).toList());
        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("q", q);
        model.addAttribute("estados", EstadoReserva.values());
        model.addAttribute("pagadas", pagos.findAll().stream().map(p -> p.getReserva().getIdReserva()).collect(java.util.stream.Collectors.toSet()));
        return "admin/reservas";
    }

    @PostMapping("/reservas/{id}/asignar")
    public String asignar(@PathVariable Long id, @RequestParam(required=false) Integer empleadoId,
                          RedirectAttributes flash) {
        var reserva=reservas.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (reserva.getEstado()==EstadoReserva.CANCELADA || reserva.getEstado()==EstadoReserva.COMPLETADA) {
            flash.addFlashAttribute("error", "No se puede asignar una reserva cerrada.");
            return "redirect:/admin/reservas";
        }
        var empleado = empleadoId==null ? null : cuentas.findByEmpleado_IdEmpleado(empleadoId)
            .filter(u -> u.getIdRol()==1 && Boolean.TRUE.equals(u.getActivo())
                    && Boolean.TRUE.equals(u.getEmpleado().getActivo()))
            .map(u -> u.getEmpleado())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige un trabajador con cuenta activa."));
        reserva.setEmpleadoAsignado(empleado);
        reservas.save(reserva);
        flash.addFlashAttribute("mensaje", "Responsable de atención actualizado.");
        return "redirect:/admin/reservas";
    }

    @PostMapping("/reservas/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, @RequestParam EstadoReserva estado,
                                RedirectAttributes flash) {
        try {
            reservaService.cambiarEstado(id, estado);
            flash.addFlashAttribute("mensaje", "Estado actualizado correctamente.");
        } catch (IllegalArgumentException error) {
            flash.addFlashAttribute("error", error.getMessage());
        }
        return "redirect:/admin/reservas";
    }

    @GetMapping("/reservas/{id}/editar")
    public String editarReserva(@PathVariable Long id, Model model) {
        var reserva = reservas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (reserva.getEstado() != EstadoReserva.PENDIENTE
                && reserva.getEstado() != EstadoReserva.CONFIRMADA
                || pagos.existsByReserva_IdReserva(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta reserva ya no admite modificaciones.");
        }
        var form = new ReservaForm();
        form.setNombreCompleto(reserva.getNombreCompleto());
        form.setEmail(reserva.getEmail());
        form.setTelefono(reserva.getTelefono());
        form.setTipoVehiculo(reserva.getTipoVehiculo());
        form.setPlaca(reserva.getPlaca());
        form.setFecha(reserva.getFecha());
        form.setHora(reserva.getHora());
        form.setComentarios(reserva.getComentarios());
        form.setCantidadVehiculos(reserva.getCantidadVehiculos());
        form.setServicioIds(reserva.getServicios().stream().map(ServicioLavado::getId).toList());
        model.addAttribute("reservaForm", form);
        prepararEdicionReserva(id, model);
        return "admin/reserva-form";
    }

    @PostMapping("/reservas/{id}/editar")
    public String guardarEdicionReserva(@PathVariable Long id,
            @Valid @ModelAttribute("reservaForm") ReservaForm form,
            BindingResult errores, Model model, RedirectAttributes flash) {
        if (errores.hasErrors()) {
            prepararEdicionReserva(id, model);
            return "admin/reserva-form";
        }
        try {
            reservaService.editar(id, form);
            flash.addFlashAttribute("mensaje", "Reserva actualizada correctamente.");
            return "redirect:/admin/reservas";
        } catch (IllegalArgumentException | org.springframework.dao.DataIntegrityViolationException ex) {
            model.addAttribute("error", ex instanceof IllegalArgumentException
                    ? ex.getMessage() : "Ese turno ya fue tomado. Selecciona otro.");
            prepararEdicionReserva(id, model);
            return "admin/reserva-form";
        }
    }

    private void prepararEdicionReserva(Long id, Model model) {
        model.addAttribute("reservaId", id);
        model.addAttribute("servicios", servicios.findAll(Sort.by("nombre")));
        // Oferta completa en el desplegable; ReservaService valida los turnos según el día.
        model.addAttribute("horas", java.util.List.of("08:00", "09:00", "10:00", "11:00",
                "12:00", "13:00", "14:00", "15:00", "16:00", "18:00"));
    }

    @PostMapping("/reservas/{id}/cancelar")
    public String cancelarReserva(@PathVariable Long id, RedirectAttributes flash) {
        try {
            reservaService.cambiarEstado(id, EstadoReserva.CANCELADA);
            flash.addFlashAttribute("mensaje", "Reserva cancelada. El turno vuelve a estar disponible.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/reservas";
    }

    @GetMapping("/servicios")
    public String listarServicios(Model model) {
        model.addAttribute("servicios", servicios.findAll(Sort.by("nombre")));
        return "admin/servicios";
    }

    @GetMapping("/servicios/nuevo")
    public String nuevoServicio(Model model) {
        model.addAttribute("servicio", new ServicioLavado());
        return "admin/servicios-form";
    }

    @GetMapping("/servicios/editar/{id}")
    public String editarServicio(@PathVariable Long id, Model model) {
        model.addAttribute("servicio", servicios.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return "admin/servicios-form";
    }

    @PostMapping("/servicios/guardar")
    public String guardarServicio(@Valid @ModelAttribute("servicio") ServicioLavado form,
                                  BindingResult errores) {
        if (errores.hasErrors()) return "admin/servicios-form";
        if (form.getId() != null) {
            var existente = servicios.findById(form.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            existente.setNombre(form.getNombre());
            existente.setDescripcion(form.getDescripcion());
            existente.setPrecio(form.getPrecio());
            existente.setActivo(form.getActivo());
            servicios.save(existente);
        } else {
            servicios.save(form);
        }
        return "redirect:/admin/servicios";
    }

    @PostMapping("/servicios/{id}/alternar")
    public String alternarServicio(@PathVariable Long id) {
        var servicio = servicios.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        servicio.setActivo(!Boolean.TRUE.equals(servicio.getActivo()));
        servicios.save(servicio);
        return "redirect:/admin/servicios";
    }

    /** Baja lógica: conserva el nombre del servicio en reservas históricas. */
    @PostMapping("/servicios/{id}/eliminar")
    public String eliminarServicio(@PathVariable Long id, RedirectAttributes flash) {
        var servicio = servicios.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        servicio.setActivo(false);
        servicios.save(servicio);
        flash.addFlashAttribute("mensaje", "Servicio retirado del catálogo sin borrar su historial.");
        return "redirect:/admin/servicios";
    }

    @GetMapping("/inventario")
    public String inventario(Model model) {
        model.addAttribute("articulos", inventario.findAllByOrderByNombreAsc());
        return "admin/inventario";
    }

    @GetMapping("/inventario/nuevo")
    public String nuevoArticulo(Model model) {
        model.addAttribute("articulo", new ArticuloInventario());
        return "admin/inventario-form";
    }

    @GetMapping("/inventario/editar/{id}")
    public String editarArticulo(@PathVariable Long id, Model model) {
        model.addAttribute("articulo", inventario.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return "admin/inventario-form";
    }

    @PostMapping("/inventario/guardar")
    public String guardarArticulo(@Valid @ModelAttribute("articulo") ArticuloInventario form,
                                  BindingResult errores) {
        if (errores.hasErrors()) return "admin/inventario-form";
        if (form.getIdArticulo() != null) {
            var actual = inventario.findById(form.getIdArticulo())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            actual.setNombre(form.getNombre());
            actual.setCategoria(form.getCategoria());
            actual.setUnidad(form.getUnidad());
            actual.setStock(form.getStock());
            actual.setMinimo(form.getMinimo());
            actual.setCostoUnitario(form.getCostoUnitario());
            actual.setActivo(form.getActivo());
            inventario.save(actual);
        } else {
            inventario.save(form);
        }
        return "redirect:/admin/inventario";
    }

    @PostMapping("/inventario/{id}/movimiento")
    public String cambiarStock(@PathVariable Long id, @RequestParam int cantidad,
                               RedirectAttributes flash) {
        var articulo = inventario.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!Boolean.TRUE.equals(articulo.getActivo()) || (cantidad != 1 && cantidad != -1) || articulo.getStock() + cantidad < 0) {
            flash.addFlashAttribute("error", "Movimiento de inventario no permitido.");
        } else {
            articulo.setStock(articulo.getStock() + cantidad);
            inventario.save(articulo);
            flash.addFlashAttribute("mensaje", "Stock actualizado.");
        }
        return "redirect:/admin/inventario";
    }

    @PostMapping("/inventario/eliminar/{id}")
    public String eliminarArticulo(@PathVariable Long id, RedirectAttributes flash) {
        var articulo = inventario.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        articulo.setActivo(false);
        inventario.save(articulo);
        flash.addFlashAttribute("mensaje", "Artículo desactivado sin perder su registro.");
        return "redirect:/admin/inventario";
    }

    @PostMapping("/inventario/activar/{id}")
    public String activarArticulo(@PathVariable Long id, RedirectAttributes flash) {
        var articulo = inventario.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        articulo.setActivo(true);
        inventario.save(articulo);
        flash.addFlashAttribute("mensaje", "Artículo reactivado.");
        return "redirect:/admin/inventario";
    }

    @GetMapping("/finanzas")
    public String finanzas(Model model) {
        model.addAttribute("pagos", pagos.findAllByOrderByRegistradoEnDesc());
        model.addAttribute("totalCobrado", pagoService.totalCobrado());
        model.addAttribute("cantidadPagos", pagos.count());
        model.addAttribute("reservas", reservas.findAll(Sort.by(Sort.Direction.DESC, "creadaEn"))
                .stream()
                .filter(r -> r.getEstado() != EstadoReserva.CANCELADA
                        && !pagos.existsByReserva_IdReserva(r.getIdReserva()))
                .toList());
        return "admin/finanzas";
    }

    @PostMapping("/finanzas/registrar")
    public String registrarPago(@RequestParam Long idReserva, @RequestParam String metodo,
                                RedirectAttributes flash) {
        try {
            pagoService.registrar(idReserva, metodo);
            flash.addFlashAttribute("mensaje", "Pago registrado correctamente.");
        } catch (IllegalArgumentException error) {
            flash.addFlashAttribute("error", error.getMessage());
        }
        return "redirect:/admin/finanzas";
    }
}
