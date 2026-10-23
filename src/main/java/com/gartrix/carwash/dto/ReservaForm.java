package com.gartrix.carwash.dto;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.*;
import java.util.*;
public class ReservaForm {
 @NotBlank @Size(min=2,max=120) @Pattern(regexp="[\\p{L}]+(?: [\\p{L}]+)*",message="Solo letras y espacios (mínimo 2 caracteres)") private String nombreCompleto;
 @NotBlank @Email @Size(max=120) private String email;
 @NotBlank @Pattern(regexp="9[0-9]{8}",message="El celular debe tener 9 dígitos y empezar con 9") private String telefono;
 @NotBlank @Pattern(regexp="Sedán / Hatchback|Camioneta / SUV|4x4 / Pick-up|Motocicleta", message="Selecciona un vehículo del listado") private String tipoVehiculo;
 @Size(max=20) @Pattern(regexp="|[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*",message="La placa solo permite letras, números y guiones") private String placa;
 @NotNull @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) private LocalDate fecha;
 @NotNull @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) private LocalTime hora;
 @Size(max=700) private String comentarios;
 @NotEmpty private List<Long> servicioIds=new ArrayList<>();
 @NotNull @Min(1) @Max(10) private Integer cantidadVehiculos=1;
 public String getNombreCompleto(){return nombreCompleto;} public void setNombreCompleto(String v){nombreCompleto=v;}
 public String getEmail(){return email;} public void setEmail(String v){email=v;}
 public String getTelefono(){return telefono;} public void setTelefono(String v){telefono=v;}
 public String getTipoVehiculo(){return tipoVehiculo;} public void setTipoVehiculo(String v){tipoVehiculo=v;}
 public String getPlaca(){return placa;} public void setPlaca(String v){placa=v;}
 public LocalDate getFecha(){return fecha;} public void setFecha(LocalDate v){fecha=v;}
 public LocalTime getHora(){return hora;} public void setHora(LocalTime v){hora=v;}
 public String getComentarios(){return comentarios;} public void setComentarios(String v){comentarios=v;}
 public List<Long> getServicioIds(){return servicioIds;} public void setServicioIds(List<Long> v){servicioIds=v;}
 public Integer getCantidadVehiculos(){return cantidadVehiculos;} public void setCantidadVehiculos(Integer v){cantidadVehiculos=v;}
}
