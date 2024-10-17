# Stage 1: Build the application using Maven
FROM maven:3.9.7-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package

# Stage 2: Set up the runtime environment
# Use an official OpenJDK runtime as a parent image
FROM openjdk:21-jdk-slim

# Create the log directory and set proper permissions
RUN mkdir -p /var/log/central-hub && \
    chmod -R 777 /var/log/central-hub  # Ensure the app can write to the log directory

# Copy the project’s jar file into the container at /app
COPY --from=build /app/target/central-hub.jar central-repo-app.jar

# Make port 8082 available to the world outside this container
EXPOSE 8082

# Run the jar file
ENTRYPOINT ["java", "-jar", "central-repo-app.jar"]

# to build image after building jar post any changes
# docker build -t central-hub:latest .
# docker-compose up --build
# docker push simranarora264/central-hub:latest
# docker file and docker-compose port should be same
# docker-compose down : shutdown the container
# till we shutdown the postgres image , db remains intact
#docker file has container port
#app.properties has