FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the local AOP library
COPY AOP ./AOP

# Build the AOP library first
RUN cd AOP/aop && mvn clean install -DskipTests

# Copy the specific microservice
COPY registration-service/pom.xml ./registration-service/
COPY registration-service/src ./registration-service/src

# Build the microservice
RUN cd registration-service && mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/registration-service/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
