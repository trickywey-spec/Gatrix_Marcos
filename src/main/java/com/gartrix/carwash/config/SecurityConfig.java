package com.gartrix.carwash.config;

import com.gartrix.carwash.repositorios.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/** Los roles se leen desde la cuenta almacenada en PostgreSQL. */
@Configuration
public class SecurityConfig {
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository usuarios) {
        return nombre -> {
            var usuario = usuarios.findByUsernameIgnoreCase(nombre.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales incorrectas"));
            boolean activo = Boolean.TRUE.equals(usuario.getActivo())
                && usuario.getEmpleado() != null
                && Boolean.TRUE.equals(usuario.getEmpleado().getActivo());
            String rol = switch (usuario.getIdRol()) {
                case 1 -> "TRABAJADOR";
                case 2 -> "ADMIN";
                default -> throw new UsernameNotFoundException("Rol no reconocido");
            };
            return User.withUsername(usuario.getUsername())
                .password(usuario.getPasswordHash())
                .roles(rol)
                .disabled(!activo)
                .build();
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/*.html", "/css/**", "/img/**", "/js/**", "/vendor/**",
                        "/favicon.ico", "/error", "/reservas", "/reservas/**", "/api/horarios",
                        "/api/servicios", "/consulta", "/consulta/**").permitAll()
                .requestMatchers("/admin", "/admin/**", "/usuarios", "/usuarios/**",
                        "/empleados", "/empleados/**").hasRole("ADMIN")
                .requestMatchers("/trabajador", "/trabajador/**").hasRole("TRABAJADOR")
                .anyRequest().denyAll())
            .formLogin(login -> login.loginPage("/login.html")
                .loginProcessingUrl("/iniciar-sesion")
                .successHandler((request, response, authentication) -> {
                    boolean admin = authentication.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
                    response.sendRedirect(request.getContextPath() + (admin ? "/admin" : "/trabajador"));
                })
                .failureUrl("/login.html?error").permitAll())
            .logout(logout -> logout.logoutUrl("/salir")
                .logoutSuccessUrl("/login.html?logout").permitAll())
            .build();
    }
}
