package com.gartrix.carwash.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
@Entity @Table(name="articulo_inventario")
public class ArticuloInventario {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_articulo") private Long idArticulo;
 @NotBlank @Size(max=120) @Column(nullable=false,length=120) private String nombre;
 @NotBlank @Size(max=70) @Column(nullable=false,length=70) private String categoria;
 @NotBlank @Size(max=30) @Column(nullable=false,length=30) private String unidad="unidad";
 @NotNull @Min(0) @Column(nullable=false) private Integer stock=0;
 @NotNull @Min(0) @Column(nullable=false) private Integer minimo=0;
 @NotNull @DecimalMin("0.00") @Column(nullable=false,precision=12,scale=2) private BigDecimal costoUnitario=BigDecimal.ZERO;
 @Column(nullable=false) private Boolean activo=true;
 public Long getIdArticulo(){return idArticulo;} public void setIdArticulo(Long v){idArticulo=v;}
 public String getNombre(){return nombre;} public void setNombre(String v){nombre=v;}
 public String getCategoria(){return categoria;} public void setCategoria(String v){categoria=v;}
 public String getUnidad(){return unidad;} public void setUnidad(String v){unidad=v;}
 public Integer getStock(){return stock;} public void setStock(Integer v){stock=v;}
 public Integer getMinimo(){return minimo;} public void setMinimo(Integer v){minimo=v;}
 public BigDecimal getCostoUnitario(){return costoUnitario;} public void setCostoUnitario(BigDecimal v){costoUnitario=v;}
 public Boolean getActivo(){return activo;} public void setActivo(Boolean v){activo=v;}
}
