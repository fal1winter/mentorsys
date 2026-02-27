FROM maven:3.8.6-openjdk-8 AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

FROM openjdk:8-jre-slim
WORKDIR /app
COPY --from=builder /app/target/mentor-system.jar app.jar

EXPOSE 7020

ENTRYPOINT ["java", "-jar", "app.jar"]
