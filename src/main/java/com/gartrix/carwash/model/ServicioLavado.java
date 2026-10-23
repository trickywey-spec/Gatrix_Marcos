package com.gartrix.carwash.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
@Entity @Table(name="servicio_lavado")
public class ServicioLavado {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @NotBlank @Size(max=140) @Column(nullable=false,length=140) private String nombre;
 @Size(max=500) @Column(length=500) private String descripcion;
 @NotNull @DecimalMin("0.0") @Digits(integer=8,fraction=2) @Column(nullable=false,precision=10,scale=2) private BigDecimal precio;
 @Column(nullable=false) private Boolean activo=true;
 public Long getId(){return id;} public void setId(Long id){this.id=id;}
 public String getNombre(){return nombre;} public void setNombre(String nombre){this.nombre=nombre;}
 public String getDescripcion(){return descripcion;} public void setDescripcion(String descripcion){this.descripcion=descripcion;}
 public BigDecimal getPrecio(){return precio;} public void setPrecio(BigDecimal precio){this.precio=precio;}
 public Boolean getActivo(){return activo;} public void setActivo(Boolean activo){this.activo=activo;}
}
