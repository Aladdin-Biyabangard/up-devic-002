# 1. Build mərhələsi — Gradle və Java 21 istifadə edir
FROM gradle:8.13-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle build -x test

# 2. Run mərhələsi — yalnız OpenJDK 21 lazımdır
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
