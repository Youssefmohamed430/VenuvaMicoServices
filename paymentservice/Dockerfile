FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the local AOP library
COPY AOP ./AOP

# Build the AOP library first
RUN cd AOP/aop && mvn clean install -DskipTests

# Copy the specific microservice
COPY paymentservice/pom.xml ./paymentservice/
COPY paymentservice/src ./paymentservice/src

# Build the microservice
RUN cd paymentservice && mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/paymentservice/target/*.jar app.jar
EXPOSE 9099
ENTRYPOINT ["java", "-jar", "app.jar"]
