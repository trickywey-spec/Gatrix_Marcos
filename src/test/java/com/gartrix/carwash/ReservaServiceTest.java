package com.gartrix.carwash;
import com.gartrix.carwash.services.ReservaService;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
class ReservaServiceTest {
 @Test void lunesTieneSeisHorarios(){assertEquals(6,ReservaService.horarios(LocalDate.of(2026,10,26)).size());}
 @Test void sabadoTieneCincoHorarios(){assertEquals(5,ReservaService.horarios(LocalDate.of(2026,10,24)).size());}
 @Test void domingoIniciaNueve(){assertEquals(LocalTime.of(9,0),ReservaService.horarios(LocalDate.of(2026,10,25)).get(0));}
 @Test void horarioFueraDeTurnoNoAparece(){assertFalse(ReservaService.horarios(LocalDate.of(2026,10,26)).contains(LocalTime.of(3,0)));}
}
