-- Diagnóstico de la nueva BD tras arrancar Spring Boot y crear una cuenta de trabajador.
SELECT r.nombre AS rol, count(u.id_usuario) AS cuentas
FROM rol r LEFT JOIN usuario u ON u.id_rol=r.id_rol GROUP BY r.nombre ORDER BY r.nombre;
SELECT u.id_usuario, u.username, r.nombre AS rol, u.activo,
 e.nombres || ' ' || e.apellidos AS empleado, e.activo AS empleado_activo
FROM usuario u JOIN rol r ON r.id_rol=u.id_rol JOIN empleado e ON e.id_empleado=u.id_empleado;
SELECT COUNT(*) AS total_reservas FROM reserva;
SELECT COUNT(*) AS tablas FROM information_schema.tables
WHERE table_schema='public' AND table_type='BASE TABLE';
