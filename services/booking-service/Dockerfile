# Build from the repository root: docker build -f services/booking-service/Dockerfile .
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY infrastructure infrastructure
COPY services services
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -q -pl services/booking-service -am package -DskipTests

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN useradd --system --uid 1001 cinema
WORKDIR /app
COPY --from=build /src/services/booking-service/target/*.jar app.jar
USER cinema
EXPOSE 8083
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
