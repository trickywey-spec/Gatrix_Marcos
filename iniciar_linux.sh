#!/bin/sh
set -eu
cd "$(dirname "$0")"
echo "Gatrix_Marcos — Java 21 + Spring Boot + PostgreSQL"
echo "Verifica la base de datos Gatrix_Marcos y DB_USER/DB_PASSWORD antes de iniciar."
java -version
exec ./mvnw spring-boot:run
