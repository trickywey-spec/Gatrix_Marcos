package com.gartrix.carwash.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
@Entity @Table(name="reserva",indexes={@Index(name="idx_reserva_fecha",columnList="fecha"),@Index(name="idx_reserva_estado",columnList="estado")})
public class Reserva {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_reserva") private Long idReserva;
 @Column(name="codigo",nullable=false,unique=true,length=40) private String codigo;
 @Column(name="nombre_completo",nullable=false,length=120) private String nombreCompleto;
 @Column(nullable=false,length=120) private String email;
 @Column(nullable=false,length=20) private String telefono;
 @Column(name="tipo_vehiculo",nullable=false,length=90) private String tipoVehiculo;
 @Column(length=20) private String placa;
 @Column(nullable=false) private LocalDate fecha;
 @Column(nullable=false) private LocalTime hora;
 @Column(length=700) private String comentarios;
 @ManyToMany(fetch=FetchType.EAGER)
 @JoinTable(name="reserva_servicio",joinColumns=@JoinColumn(name="id_reserva"),inverseJoinColumns=@JoinColumn(name="id_servicio"))
 private List<ServicioLavado> servicios=new ArrayList<>();
 @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="id_empleado_asignado") private Empleado empleadoAsignado;
 @Column(name="cantidad_vehiculos",nullable=false) private Integer cantidadVehiculos=1;
 @Column(name="precio_total",nullable=false,precision=10,scale=2) private BigDecimal precioTotal;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private EstadoReserva estado=EstadoReserva.PENDIENTE;
 @Column(name="creada_en",nullable=false,updatable=false) private LocalDateTime creadaEn;
 @PrePersist void antesDeCrear(){if(codigo==null)codigo="GRX-"+UUID.randomUUID().toString().replace("-","").toUpperCase(); if(creadaEn==null)creadaEn=LocalDateTime.now(ZoneId.of("America/Lima"));}
 public Long getIdReserva(){return idReserva;} public void setIdReserva(Long id){this.idReserva=id;}
 public String getCodigo(){return codigo;} public void setCodigo(String codigo){this.codigo=codigo;}
 public String getNombreCompleto(){return nombreCompleto;} public void setNombreCompleto(String v){nombreCompleto=v;}
 public String getEmail(){return email;} public void setEmail(String v){email=v;}
 public String getTelefono(){return telefono;} public void setTelefono(String v){telefono=v;}
 public String getTipoVehiculo(){return tipoVehiculo;} public void setTipoVehiculo(String v){tipoVehiculo=v;}
 public String getPlaca(){return placa;} public void setPlaca(String v){placa=v;}
 public LocalDate getFecha(){return fecha;} public void setFecha(LocalDate v){fecha=v;}
 public LocalTime getHora(){return hora;} public void setHora(LocalTime v){hora=v;}
 public String getComentarios(){return comentarios;} public void setComentarios(String v){comentarios=v;}
 public List<ServicioLavado> getServicios(){return servicios;} public void setServicios(List<ServicioLavado> v){servicios=v;}
 public String getResumenServicios(){return servicios.stream().map(ServicioLavado::getNombre).collect(Collectors.joining(", "));}
 public Empleado getEmpleadoAsignado(){return empleadoAsignado;} public void setEmpleadoAsignado(Empleado e){empleadoAsignado=e;}
 public Integer getCantidadVehiculos(){return cantidadVehiculos;} public void setCantidadVehiculos(Integer v){cantidadVehiculos=v;}
 public BigDecimal getPrecioTotal(){return precioTotal;} public void setPrecioTotal(BigDecimal v){precioTotal=v;}
 public EstadoReserva getEstado(){return estado;} public void setEstado(EstadoReserva v){estado=v;}
 public LocalDateTime getCreadaEn(){return creadaEn;}
}
