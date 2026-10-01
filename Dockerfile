# Dockerfile for viora-platform
# Summary:
# Multi-stage Dockerfile using Maven and Eclipse Temurin JDK/JRE 21 for production deployment in Render.

# ==========================================
# Step 1: Build Stage
# ==========================================
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiar archivos del wrapper y dependencias para cachear capas
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Descargar dependencias de forma offline
RUN ./mvnw dependency:go-offline -B

# Copiar código fuente y compilar artefacto omitiendo tests
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ==========================================
# Step 2: Runtime Stage
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Crear usuario sin privilegios por seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copiar el jar compilado desde la etapa de build
COPY --from=build /app/target/*.jar app.jar

# Render expone la variable de entorno PORT en tiempo de ejecución
ENV PORT=8080
EXPOSE 8080

# Ejecutar la aplicación mapeando el puerto asignado por Render
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT}"]
