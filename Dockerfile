# Etapa de construcción (Build)
FROM gradle:8.4-jdk17 AS build
WORKDIR /app

# Copiamos primero los archivos de configuracion
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Copiamos el codigo fuente
COPY src ./src

# Damos permisos de ejecucion a gradlew y construimos el jar
RUN chmod +x ./gradlew
RUN ./gradlew build -x test

# Etapa de produccion (Run)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiamos el jar generado en la etapa anterior (aseguramos el nombre correcto usando comodin)
COPY --from=build /app/build/libs/Fintech-*.jar app.jar

# Spring Boot usa este puerto por defecto
EXPOSE 8080

# Comando para ejecutar la app
ENTRYPOINT ["java", "-jar", "app.jar"]
