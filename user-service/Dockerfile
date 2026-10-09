# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy service source code
COPY . .
RUN if [ -d "user-service" ]; then cp -r user-service/* . ; fi

# Package the application
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy compiled JAR to app.jar
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8086

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
