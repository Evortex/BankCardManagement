FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/bankcard-management.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
