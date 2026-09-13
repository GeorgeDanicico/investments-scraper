FROM ghcr.io/graalvm/native-image-community:25 AS build

WORKDIR /workspace

RUN microdnf install -y curl unzip \
    && microdnf clean all

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw

COPY src src

RUN ./mvnw -Pnative clean native:compile -DskipTests

FROM ubuntu:24.04

WORKDIR /app

COPY --from=build /workspace/target/investment /app/investment

RUN useradd --system --no-create-home --shell /usr/sbin/nologin appuser \
    && chown appuser:appuser /app/investment

USER appuser

EXPOSE 8080

ENTRYPOINT ["/app/investment"]
