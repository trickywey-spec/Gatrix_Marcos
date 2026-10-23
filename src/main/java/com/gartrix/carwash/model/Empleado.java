package com.gartrix.carwash.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "empleado")
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empleado")
    private Integer idEmpleado;

    @NotBlank @Size(min=2,max=100) @Pattern(regexp="[\\p{L}]+(?: [\\p{L}]+)*", message="Solo letras y espacios")
    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @NotBlank @Size(min=2,max=100) @Pattern(regexp="[\\p{L}]+(?: [\\p{L}]+)*", message="Solo letras y espacios")
    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Size(max=15) @Pattern(regexp="|9[0-9]{8}", message="El celular debe tener 9 dígitos y comenzar por 9")
    @Column(name = "telefono", length = 15)
    private String telefono;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_contrato")
    private LocalDate fechaContrato;

    @DecimalMin("0.00")
    @Column(name = "salario_base", precision = 12, scale = 2)
    private BigDecimal salarioBase;

    @Column(name = "tipo_contrato", length = 50)
    private String tipoContrato;

    @OneToOne(mappedBy = "empleado")
    private Usuario usuario;

    public Empleado() {}

    public Empleado(String nombres, String apellidos, String telefono, Boolean activo,
                    LocalDate fechaContrato, BigDecimal salarioBase, String tipoContrato) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.telefono = telefono;
        this.activo = activo;
        this.fechaContrato = fechaContrato;
        this.salarioBase = salarioBase;
        this.tipoContrato = tipoContrato;
    }

    public Integer getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(Integer idEmpleado) { this.idEmpleado = idEmpleado; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public LocalDate getFechaContrato() { return fechaContrato; }
    public void setFechaContrato(LocalDate fechaContrato) { this.fechaContrato = fechaContrato; }

    public BigDecimal getSalarioBase() { return salarioBase; }
    public void setSalarioBase(BigDecimal salarioBase) { this.salarioBase = salarioBase; }

    public String getTipoContrato() { return tipoContrato; }
    public void setTipoContrato(String tipoContrato) { this.tipoContrato = tipoContrato; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}