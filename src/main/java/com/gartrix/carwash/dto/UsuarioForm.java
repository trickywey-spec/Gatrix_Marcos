package com.gartrix.carwash.dto;

import jakarta.validation.constraints.*;

/** Datos de entrada de usuarios, separados de la entidad JPA y del hash cifrado. */
public class UsuarioForm {
    private Integer idUsuario;

    @NotNull(message = "Selecciona un empleado.")
    private Integer idEmpleado;

    @NotBlank(message = "Indica un nombre de usuario.")
    @Pattern(regexp = "[a-zA-Z0-9._-]{3,50}", message = "Usa entre 3 y 50 letras, dígitos, puntos, guiones o guion bajo.")
    private String username;

    private String password;

    @NotNull(message = "Selecciona un rol.")
    @Min(1) @Max(2)
    private Integer idRol = 1;

    private boolean activo = true;

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer value) { idUsuario = value; }
    public Integer getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(Integer value) { idEmpleado = value; }
    public String getUsername() { return username; }
    public void setUsername(String value) { username = value; }
    public String getPassword() { return password; }
    public void setPassword(String value) { password = value; }
    public Integer getIdRol() { return idRol; }
    public void setIdRol(Integer value) { idRol = value; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean value) { activo = value; }
}
