package com.gartrix.carwash.services;
import com.gartrix.carwash.model.*;
import com.gartrix.carwash.repositorios.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Set;
@Service
public class PagoService {
 private final PagoRepository pagos; private final ReservaRepository reservas;
 public PagoService(PagoRepository pagos,ReservaRepository reservas){this.pagos=pagos;this.reservas=reservas;}
 @Transactional public Pago registrar(Long idReserva,String metodo){
  if(!Set.of("EFECTIVO","YAPE","PLIN","TARJETA","TRANSFERENCIA").contains(metodo))throw new IllegalArgumentException("Método de pago no válido.");
  var reserva=reservas.findById(idReserva).orElseThrow(()->new IllegalArgumentException("Reserva no encontrada."));
  if(reserva.getEstado()==EstadoReserva.CANCELADA)throw new IllegalArgumentException("No se pueden cobrar reservas canceladas.");
  if(pagos.existsByReserva_IdReserva(idReserva))throw new IllegalArgumentException("La reserva ya tiene un pago registrado.");
  var pago=new Pago();pago.setReserva(reserva);pago.setMonto(reserva.getPrecioTotal());pago.setMetodoPago(metodo);
  return pagos.save(pago);
 }
 public BigDecimal totalCobrado(){return pagos.findAll().stream().map(Pago::getMonto).reduce(BigDecimal.ZERO,BigDecimal::add);}
}
