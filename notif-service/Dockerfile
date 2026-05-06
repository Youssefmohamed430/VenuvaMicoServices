FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the local AOP library
COPY AOP ./AOP

# Build the AOP library first
RUN cd AOP/aop && mvn clean install -DskipTests

# Copy the specific microservice
COPY notif-service/pom.xml ./notif-service/
COPY notif-service/src ./notif-service/src

# Build the microservice
RUN cd notif-service && mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/notif-service/target/*.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
