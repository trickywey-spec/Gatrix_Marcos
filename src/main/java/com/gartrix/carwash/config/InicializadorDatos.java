package com.gartrix.carwash.config;

import com.gartrix.carwash.model.ArticuloInventario;
import com.gartrix.carwash.model.Empleado;
import com.gartrix.carwash.model.ServicioLavado;
import com.gartrix.carwash.model.Usuario;
import com.gartrix.carwash.repositorios.ArticuloInventarioRepository;
import com.gartrix.carwash.repositorios.EmpleadoRepository;
import com.gartrix.carwash.repositorios.ServicioLavadoRepository;
import com.gartrix.carwash.repositorios.UsuarioRepository;
import com.gartrix.carwash.services.UsuarioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Precarga para la demostración local del proyecto académico. */
@Configuration
public class InicializadorDatos {

    @Bean
    public CommandLineRunner iniciar(
            EmpleadoRepository empleados,
            UsuarioRepository usuarios,
            UsuarioService usuarioService,
            PasswordEncoder encoder,
            ServicioLavadoRepository servicios,
            ArticuloInventarioRepository articulos,
            @Value("${app.admin.password:1234}") String claveInicial) {
        return args -> {
            var existente = usuarios.findByUsername("admin");
            if (existente.isEmpty()) {
                var empleado = new Empleado("Administrador", "GATRIX", "987654321",
                        true, LocalDate.now(), BigDecimal.ZERO, "Administrativo");
                empleado = empleados.save(empleado);
                usuarioService.guardarUsuario(new Usuario(empleado, "admin", claveInicial, 2, true));
            } else {
                // Adecuar también bases que provienen de versiones anteriores.
                var administrador = existente.get();
                administrador.setIdRol(2);
                administrador.setActivo(true);
                if (administrador.getEmpleado() != null) {
                    var empleado = administrador.getEmpleado();
                    empleado.setActivo(true);
                    empleados.save(empleado);
                }
                // No sobrescribir contraseñas elegidas en el panel en cada arranque.
                if (administrador.getPasswordHash() == null || !administrador.getPasswordHash().startsWith("$2")) {
                    administrador.setPasswordHash(encoder.encode(claveInicial));
                }
                usuarios.save(administrador);
            }
            if (servicios.count() == 0) {
                servicio(servicios, "Lavado Exterior (Carrocería + Llantas)", "Lavado exterior y llantas", 25);
                servicio(servicios, "Lavado Interior (Aspirado + Tablero)", "Limpieza interior", 20);
                servicio(servicios, "Combo Clásico (Exterior + Interior)", "Lavado integral", 40);
                servicio(servicios, "Limpieza de Motor (Exterior)", "Limpieza de motor", 35);
                servicio(servicios, "Pulido de Faros (Par)", "Restauración de faros", 45);
                servicio(servicios, "Tratamiento Acrílico (Cera en pasta)", "Protección y brillo", 60);
                servicio(servicios, "Lavado de Tapas y Alfombras", "Alfombras y tapicería", 50);
                servicio(servicios, "Recubrimiento Cerámico", "Protección cerámica", 250);
                servicio(servicios, "Promo Gatrix Full (Completo)", "Servicio completo", 120);
            }
            if (articulos.count() == 0) {
                articulo(articulos, "Shampoo automotriz", "Limpieza", "litro", 15, 4, 22);
                articulo(articulos, "Cera acrílica", "Detallado", "unidad", 8, 3, 45);
                articulo(articulos, "Microfibra", "Accesorios", "unidad", 25, 8, 8);
            }
        };
    }

    private void servicio(ServicioLavadoRepository repo, String nombre, String descripcion, int precio) {
        var servicio = new ServicioLavado();
        servicio.setNombre(nombre);
        servicio.setDescripcion(descripcion);
        servicio.setPrecio(BigDecimal.valueOf(precio));
        repo.save(servicio);
    }

    private void articulo(ArticuloInventarioRepository repo, String nombre, String categoria,
                          String unidad, int stock, int minimo, int costo) {
        var articulo = new ArticuloInventario();
        articulo.setNombre(nombre);
        articulo.setCategoria(categoria);
        articulo.setUnidad(unidad);
        articulo.setStock(stock);
        articulo.setMinimo(minimo);
        articulo.setCostoUnitario(BigDecimal.valueOf(costo));
        repo.save(articulo);
    }
}
