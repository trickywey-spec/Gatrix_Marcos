package com.gartrix.carwash;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class CarwashApplication {
 public static void main(String[] args) { SpringApplication.run(CarwashApplication.class,args); }
 // El menú de consola original permanece en consola/ pero NO bloquea el servidor al arrancar.
}
