package com.gartrix.carwash.controlador;

import com.gartrix.carwash.dto.ReservaForm;
import com.gartrix.carwash.repositorios.ReservaRepository;
import com.gartrix.carwash.repositorios.ServicioLavadoRepository;
import com.gartrix.carwash.services.ReservaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Controller
public class PublicoController {
    private static final Logger log = LoggerFactory.getLogger(PublicoController.class);
    private final ReservaService reservaService;
    private final ServicioLavadoRepository servicios;
    private final ReservaRepository reservas;

    public PublicoController(ReservaService reservaService, ServicioLavadoRepository servicios,
                             ReservaRepository reservas) {
        this.reservaService = reservaService;
        this.servicios = servicios;
        this.reservas = reservas;
    }

    @GetMapping({"/", "/index.html"})
    public String home() { return "index"; }

    @GetMapping("/inicio.html")
    public String nosotros() { return "inicio"; }

    @GetMapping("/servicios.html")
    public String servicios(Model model) {
        model.addAttribute("serviciosActivos", servicios.findByActivoTrueOrderByIdAsc());
        return "servicios";
    }

    @GetMapping("/adicionales.html")
    public String adicionales() { return "adicionales"; }

    @GetMapping("/precios.html")
    public String precios(Model model) {
        model.addAttribute("serviciosActivos", servicios.findByActivoTrueOrderByIdAsc());
        return "precios";
    }

    @GetMapping("/ubicacion.html")
    public String ubicacion() { return "ubicacion"; }

    @GetMapping("/login.html")
    public String login() { return "login"; }

    /** Catálogo público separado del renderizado HTML para evitar errores 500 en la vista. */
    @GetMapping("/api/servicios")
    @ResponseBody
    public ResponseEntity<?> catalogoServicios() {
        try {
            List<Map<String, Object>> catalogo = servicios.findByActivoTrueOrderByIdAsc()
                    .stream().map(item -> Map.<String, Object>of(
                            "id", item.getId(),
                            "nombre", item.getNombre(),
                            "descripcion", item.getDescripcion() == null ? "" : item.getDescripcion(),
                            "precio", item.getPrecio()))
                    .toList();
            return ResponseEntity.ok(catalogo);
        } catch (DataAccessException ex) {
            log.error("No fue posible consultar los servicios en PostgreSQL", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "El catálogo no está disponible. Verifica PostgreSQL y vuelve a intentar."));
        }
    }

    @GetMapping({"/reservas.html", "/reservas"})
    public String formulario(@RequestParam(required = false) String servicios,
                             @RequestParam(defaultValue = "1") int cantidad,
                             Model model) {
        if (!model.containsAttribute("reservaForm")) {
            ReservaForm form = new ReservaForm();
            form.setCantidadVehiculos(Math.max(1, Math.min(10, cantidad)));
            if (servicios != null && servicios.matches("[0-9,]+")) {
                try {
                    List<Long> ids = Arrays.stream(servicios.split(","))
                            .filter(texto -> !texto.isEmpty() && texto.length() <= 18)
                            .limit(20).map(Long::parseLong).distinct().toList();
                    form.setServicioIds(ids);
                } catch (NumberFormatException ignored) {
                    // La validación final ocurre al guardar en el servidor.
                }
            }
            model.addAttribute("reservaForm", form);
        }
        return mostrarFormulario(model);
    }

    private String mostrarFormulario(Model model) {
        model.addAttribute("hoy", LocalDate.now(ReservaService.ZONA));
        return "reservas";
    }

    @PostMapping("/reservas")
    public String reservar(@Valid @ModelAttribute("reservaForm") ReservaForm form,
                           BindingResult errores, Model model) {
        if (errores.hasErrors()) {
            model.addAttribute("erroresReserva", true);
            return mostrarFormulario(model);
        }
        try {
            var reserva = reservaService.crear(form);
            return "redirect:/reservas/confirmacion/" + reserva.getCodigo();
        } catch (IllegalArgumentException error) {
            model.addAttribute("errorReserva", error.getMessage());
            return mostrarFormulario(model);
        } catch (DataAccessException error) {
            log.error("No se pudo guardar la reserva en PostgreSQL", error);
            model.addAttribute("errorReserva", "No fue posible guardar la reserva. Verifica que PostgreSQL esté disponible e inténtalo de nuevo.");
            return mostrarFormulario(model);
        }
    }

    @GetMapping("/reservas/confirmacion/{codigo}")
    public String confirmacion(@PathVariable String codigo, Model model) {
        var reserva = reservas.findByCodigo(codigo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("reserva", reserva);
        return "reservas/confirmacion";
    }

    /** Endpoint utilizado por el selector de turnos; lee las reservas actuales en PostgreSQL. */
    @GetMapping("/api/horarios")
    @ResponseBody
    public List<Map<String, Object>> turnos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        List<LocalTime> disponibles = reservaService.horariosDisponibles(fecha);
        return ReservaService.horarios(fecha).stream().map(hora -> Map.<String, Object>of(
                "hora", hora.toString(), "disponible", disponibles.contains(hora)))
                .toList();
    }

    @GetMapping("/consulta")
    public String consulta() { return "consulta"; }

    @PostMapping("/consulta")
    public String consultarEstado(@RequestParam String codigo, @RequestParam String email,
                                 Model model) {
        var reserva = reservas.findByCodigoAndEmailIgnoreCase(codigo.trim().toUpperCase(), email.trim());
        if (reserva.isEmpty()) {
            model.addAttribute("errorConsulta", "No encontramos una reserva con ese código y correo.");
        } else {
            model.addAttribute("reserva", reserva.get());
        }
        return "consulta";
    }
}
