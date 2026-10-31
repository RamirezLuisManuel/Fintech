# Etapa de construccion (Build)
FROM gradle:8.8-jdk17 AS build
WORKDIR /app

# Copiamos primero los archivos de configuracion
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Copiamos el codigo fuente
COPY src ./src

# Damos permisos de ejecucion a gradlew y construimos solo el bootJar excluyendo tests
RUN chmod +x ./gradlew
RUN ./gradlew bootJar -x test

# Etapa de produccion (Run)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiamos exclusivamente el artefacto ejecutable Spring Boot (evitando el plain.jar)
COPY --from=build /app/build/libs/Fintech-1.0.jar app.jar

# El puerto configurado en application.properties es 9090
EXPOSE 9090

# Comando para ejecutar la app
ENTRYPOINT ["java", "-jar", "app.jar"]
