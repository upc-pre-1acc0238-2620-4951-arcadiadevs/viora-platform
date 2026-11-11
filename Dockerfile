# Dockerfile for viora-platform
# Summary:
# Multi-stage Dockerfile using Maven and Eclipse Temurin JDK/JRE 21 for production deployment in Render.

# ==========================================
# Step 1: Build Stage
# ==========================================
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiar el descriptor del proyecto y descargar dependencias para cachear capas
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copiar código fuente y compilar artefacto omitiendo tests
COPY src ./src
RUN mvn clean package -DskipTests -B

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

# Ejecutar la aplicación mapeando el puerto asignado por Render. Fuera del heap la JVM ocupa unos 250 MB fijos
# (metaspace, símbolos, código compilado), así que el heap queda en 192 MB y devuelve al sistema lo que no usa, y solo
# compila con C1, que ocupa mucha menos caché de código y memoria del compilador. Medido en 512 MB y 0,5 CPU: en reposo
# usa unos 350 MB y aguanta diez peticiones pesadas a la vez, donde la configuración anterior mataba el contenedor.
ENTRYPOINT ["sh", "-c", "java -Xmx192m -XX:MaxHeapFreeRatio=40 -XX:MinHeapFreeRatio=15 -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=32m -XX:MaxMetaspaceSize=170m -Xss512k -jar app.jar --server.port=${PORT}"]
