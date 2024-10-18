FROM maven:3.9.7-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package

# Use an official OpenJDK runtime as a parent image
FROM openjdk:21-jdk-slim

# Copy the project’s jar file into the container at /app
COPY --from=build /app/target/central-hub.jar app.jar

# Make port 8080 available to the world outside this container
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]

# to build image after building jar post any changes
# docker build -t profile-service .
# docker-compose up --build
# docker file and docker-compose port should be same
# docker-compose down : shutdown the container
# till we shutdown the postgres image , db remains intact
#docker file has container port
#app.properties has