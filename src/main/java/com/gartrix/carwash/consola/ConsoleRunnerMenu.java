package com.gartrix.carwash.consola;

import com.gartrix.carwash.model.Empleado;
import com.gartrix.carwash.model.Usuario;
import com.gartrix.carwash.services.EmpleadoService;
import com.gartrix.carwash.services.UsuarioService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

@Component
public class ConsoleRunnerMenu {

    private final UsuarioService usuarioService;
    private final EmpleadoService empleadoService;

    public ConsoleRunnerMenu(UsuarioService usuarioService, EmpleadoService empleadoService) {
        this.usuarioService = usuarioService;
        this.empleadoService = empleadoService;
    }

    public void iniciarMenu() {
        Scanner scanner = new Scanner(System.in);
        boolean salir = false;

        while (!salir) {
            System.out.println("\n=========================================");
            System.out.println("    SISTEMA DE GESTIÓN DE USUARIOS       ");
            System.out.println("=========================================");
            System.out.println("1. Insertar Usuario");
            System.out.println("2. Listar Todos los Usuarios");
            System.out.println("3. Actualizar Usuario");
            System.out.println("4. Eliminar Usuario");
            System.out.println("5. Búsquedas Específicas");
            System.out.println("6. Salir de la Consola");
            System.out.print("Seleccione una opción: ");

            int opcion = leerEntero(scanner);

            switch (opcion) {
                case 1 -> insertar(scanner);
                case 2 -> listar();
                case 3 -> actualizar(scanner);
                case 4 -> eliminar(scanner);
                case 5 -> menuBusquedas(scanner);
                case 6 -> {
                    salir = true;
                    System.out.println("Cerrando menú interactivo de consola...");
                }
                default -> System.out.println("Opción no válida. Intente de nuevo.");
            }
        }
    }

    private void insertar(Scanner scanner) {
        System.out.println("\n--- INSERTAR USUARIO ---");

        System.out.print("Ingrese el ID del Empleado existente: ");
        int idEmpleado = leerEntero(scanner);

        Optional<Empleado> empOpt = empleadoService.obtenerPorId(idEmpleado);
        if (empOpt.isEmpty()) {
            System.out.println("Error: No existe un Empleado con el ID " + idEmpleado + ". Primero créalo mediante la web o base de datos.");
            return;
        }

        System.out.print("Username: ");
        String username = scanner.nextLine();

        System.out.print("Contraseña: ");
        String password = scanner.nextLine();

        System.out.print("ID Rol (ej. 1): ");
        int idRol = leerEntero(scanner);
        if (idRol == -1) idRol = 1;

        Usuario nuevo = new Usuario(empOpt.get(), username, password, idRol, true);
        Usuario guardado = usuarioService.guardarUsuario(nuevo);

        System.out.println("Usuario creado con éxito. ID Usuario: " + guardado.getIdUsuario() +
                " | Asignado a: " + guardado.getEmpleado().getNombres() + " " + guardado.getEmpleado().getApellidos());
    }

    private void listar() {
        System.out.println("\n--- LISTA DE USUARIOS ---");
        List<Usuario> usuarios = usuarioService.obtenerTodos();
        if (usuarios.isEmpty()) {
            System.out.println("No hay usuarios registrados.");
        } else {
            usuarios.forEach(u -> System.out.printf(
                    "ID: %d | Username: %-15s | Empleado: %-25s | Rol: %d | Activo: %b%n",
                    u.getIdUsuario(),
                    u.getUsername(),
                    (u.getEmpleado() != null ? u.getEmpleado().getNombres() + " " + u.getEmpleado().getApellidos() : "Sin asignar"),
                    u.getIdRol(),
                    u.getActivo()
            ));
        }
    }

    private void actualizar(Scanner scanner) {
        System.out.println("\n--- ACTUALIZAR USUARIO ---");
        System.out.print("Ingrese el ID del usuario a modificar: ");
        int id = leerEntero(scanner);

        Optional<Usuario> opt = usuarioService.obtenerPorId(id);
        if (opt.isPresent()) {
            System.out.print("Nuevo Username: ");
            String nuevoUsername = scanner.nextLine();

            System.out.print("Nuevo ID Rol: ");
            int nuevoRol = leerEntero(scanner);

            System.out.print("¿Está activo? (true/false): ");
            boolean activo = Boolean.parseBoolean(scanner.nextLine());

            Usuario usuario = opt.get();
            usuario.setUsername(nuevoUsername);
            usuario.setActivo(activo);
            usuario.setIdRol(nuevoRol);
            usuarioService.guardarUsuario(usuario);
            System.out.println("Usuario actualizado correctamente.");
        } else {
            System.out.println("Usuario no encontrado con el ID: " + id);
        }
    }

    private void eliminar(Scanner scanner) {
        System.out.println("\n--- ELIMINAR USUARIO ---");
        System.out.print("Ingrese el ID del usuario a eliminar: ");
        int id = leerEntero(scanner);

        if (usuarioService.obtenerPorId(id).isPresent()) {
            usuarioService.eliminarUsuario(id);
            System.out.println("Usuario eliminado exitosamente.");
        } else {
            System.out.println("No se encontró ningún usuario con el ID: " + id);
        }
    }

    private void menuBusquedas(Scanner scanner) {
        System.out.println("\n--- SUBMENÚ DE BÚSQUEDAS ---");
        System.out.println("1. Por ID");
        System.out.println("2. Por coincidencia parcial en Username");
        System.out.println("3. Por estado (Activo / Inactivo)");
        System.out.print("Seleccione una opción: ");

        int subOpcion = leerEntero(scanner);

        switch (subOpcion) {
            case 1 -> {
                System.out.print("Ingrese ID a buscar: ");
                int id = leerEntero(scanner);
                usuarioService.obtenerPorId(id).ifPresentOrElse(
                        u -> System.out.println("Encontrado -> ID: " + u.getIdUsuario() + " | Username: " + u.getUsername()),
                        () -> System.out.println("No se encontró usuario con ese ID.")
                );
            }
            case 2 -> {
                System.out.print("Ingrese texto a buscar en username: ");
                String texto = scanner.nextLine();
                List<Usuario> resultados = usuarioService.buscarPorCoincidenciaUsername(texto);
                System.out.println("Resultados encontrados (" + resultados.size() + "):");
                resultados.forEach(u -> System.out.println("ID: " + u.getIdUsuario() + " | Username: " + u.getUsername()));
            }
            case 3 -> {
                System.out.print("¿Buscar activos (t) o inactivos (f)?: ");
                String texto = scanner.nextLine();
                boolean estadoBoolean;
                if (texto.equalsIgnoreCase("t")) {
                    estadoBoolean = true;
                } else if (texto.equalsIgnoreCase("f")) {
                    estadoBoolean = false;
                } else {
                    System.out.println("Carácter inválido, se esperaba 't' o 'f'.");
                    return;
                }
                List<Usuario> resultados = usuarioService.buscarPorEstado(estadoBoolean);
                String estadoTexto = estadoBoolean ? "Activos" : "Inactivos";
                System.out.println("Usuarios con estado [" + estadoTexto + "] (" + resultados.size() + "):");
                resultados.forEach(u ->
                        System.out.println("ID: " + u.getIdUsuario() + " | Username: " + u.getUsername())
                );
            }
            default -> System.out.println("Opción de búsqueda no válida.");
        }
    }

    private int leerEntero(Scanner scanner) {
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}