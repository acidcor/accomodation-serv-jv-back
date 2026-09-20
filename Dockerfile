# Builder stage
FROM eclipse-temurin:26-alpine AS builder

WORKDIR /application

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
COPY /config/checkstyle/checkstyle.xml .

RUN chmod +x mvnw

COPY src src

RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:26-jre-alpine

WORKDIR /application

COPY --from=builder /application/target/*.jar application.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "application.jar"]
