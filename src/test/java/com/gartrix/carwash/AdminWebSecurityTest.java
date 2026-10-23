package com.gartrix.carwash;

import com.gartrix.carwash.config.SecurityConfig;
import com.gartrix.carwash.controlador.AdminController;
import com.gartrix.carwash.repositorios.*;
import com.gartrix.carwash.services.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/** Comprobación del firewall MVC / CSRF sin conectarse a PostgreSQL. */
@WebMvcTest(controllers = AdminController.class)
@Import(SecurityConfig.class)
class AdminWebSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean UsuarioRepository usuarios;
    @MockitoBean ReservaRepository reservas;
    @MockitoBean ReservaService reservaService;
    @MockitoBean ServicioLavadoRepository servicios;
    @MockitoBean ArticuloInventarioRepository inventario;
    @MockitoBean PagoRepository pagos;
    @MockitoBean PagoService pagoService;
    @MockitoBean EmpleadoRepository empleados;

    @Test void visitanteNoPuedeAbrirElPanel() throws Exception {
        mvc.perform(get("/admin/reservas")).andExpect(status().is3xxRedirection());
    }

    @Test @WithMockUser(roles = "TRABAJADOR")
    void colaboradorNoPuedeAbrirAdministracion() throws Exception {
        mvc.perform(get("/admin/reservas")).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void postSinTokenCsrfEsRechazado() throws Exception {
        mvc.perform(post("/admin/servicios/guardar").param("nombre", "Test"))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void administradorAutorizadoPuedeSolicitarCatalogo() throws Exception {
        mvc.perform(get("/admin/servicios")).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void administradorPuedeGuardarServicioConCsrf() throws Exception {
        mvc.perform(post("/admin/servicios/guardar").with(csrf())
                .param("nombre", "Lavado de prueba").param("precio", "25.00")
                .param("activo", "true"))
                .andExpect(status().is3xxRedirection());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void retirarServicioLoDesactivaSinBorrarReservas() throws Exception {
        var servicio = new com.gartrix.carwash.model.ServicioLavado();
        servicio.setId(12L);
        servicio.setNombre("Lavado");
        servicio.setPrecio(new java.math.BigDecimal("15.00"));
        org.mockito.Mockito.when(servicios.findById(12L)).thenReturn(java.util.Optional.of(servicio));
        mvc.perform(post("/admin/servicios/12/eliminar").with(csrf()))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertFalse(servicio.getActivo());
        org.mockito.Mockito.verify(servicios).save(servicio);
    }

    @Test @WithMockUser(roles = "ADMIN")
    void cancelarReservaUsaServicioDeNegocio() throws Exception {
        mvc.perform(post("/admin/reservas/44/cancelar").with(csrf()))
                .andExpect(status().is3xxRedirection());
        org.mockito.Mockito.verify(reservaService).cambiarEstado(
                44L, com.gartrix.carwash.model.EstadoReserva.CANCELADA);
    }

}
