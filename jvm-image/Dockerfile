FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl unzip \
    && rm -rf /var/lib/apt/lists/*

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw

COPY src src

RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /workspace/target/investment-0.0.1-SNAPSHOT.jar /app/investment.jar

RUN useradd --system --no-create-home --shell /usr/sbin/nologin appuser \
    && chown appuser:appuser /app/investment.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/investment.jar"]
