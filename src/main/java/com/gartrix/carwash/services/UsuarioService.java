package com.gartrix.carwash.services;

import com.gartrix.carwash.model.Usuario;
import com.gartrix.carwash.repositorios.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {
    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    public UsuarioService(UsuarioRepository repo, PasswordEncoder encoder) { this.repo=repo; this.encoder=encoder; }

    @Transactional
    public Usuario guardarUsuario(Usuario usuario) {
        if (usuario.getPasswordHash() == null || usuario.getPasswordHash().isBlank())
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        if (!usuario.getPasswordHash().matches("\\$2[aby]\\$.*"))
            usuario.setPasswordHash(encoder.encode(usuario.getPasswordHash()));
        usuario.setUsername(usuario.getUsername().trim().toLowerCase(java.util.Locale.ROOT));
        return repo.save(usuario);
    }
    public List<Usuario> obtenerTodos() { return repo.findAll(); }
    public Optional<Usuario> obtenerPorId(Integer id) { return repo.findById(id); }
    public Optional<Usuario> buscarPorUsername(String user) { return repo.findByUsernameIgnoreCase(user); }
    public List<Usuario> buscarPorEstado(Boolean activo) { return repo.findByActivo(activo); }
    public List<Usuario> buscarPorCoincidenciaUsername(String texto) { return repo.findByUsernameContainingIgnoreCase(texto); }

    @Transactional
    public void eliminarUsuario(Integer id) {
        Usuario usuario=repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        if ("admin".equalsIgnoreCase(usuario.getUsername()))
            throw new IllegalArgumentException("No se puede eliminar el administrador principal.");
        // Conserva el empleado. La eliminación es física y se fuerza a PostgreSQL aquí.
        repo.delete(usuario);
        repo.flush();
    }
}
