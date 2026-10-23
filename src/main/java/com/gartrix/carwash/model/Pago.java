package com.gartrix.carwash.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
@Entity @Table(name="pago")
public class Pago {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_pago") private Long idPago;
 @OneToOne(optional=false) @JoinColumn(name="id_reserva",nullable=false,unique=true) private Reserva reserva;
 @Column(nullable=false,precision=10,scale=2) private BigDecimal monto;
 @Column(name="metodo_pago",nullable=false,length=25) private String metodoPago;
 @Column(name="registrado_en",nullable=false) private LocalDateTime registradoEn;
 @PrePersist void init(){if(registradoEn==null)registradoEn=LocalDateTime.now(ZoneId.of("America/Lima"));}
 public Long getIdPago(){return idPago;} public void setIdPago(Long v){idPago=v;}
 public Reserva getReserva(){return reserva;} public void setReserva(Reserva v){reserva=v;}
 public BigDecimal getMonto(){return monto;} public void setMonto(BigDecimal v){monto=v;}
 public String getMetodoPago(){return metodoPago;} public void setMetodoPago(String v){metodoPago=v;}
 public LocalDateTime getRegistradoEn(){return registradoEn;}
}
