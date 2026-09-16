FROM ghcr.io/graalvm/native-image-community:25 AS build

WORKDIR /workspace

RUN microdnf install -y curl unzip \
    && microdnf clean all

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw

COPY src src

# Native Image compilation is memory-intensive. Limiting parallelism reduces
# peak memory usage at the cost of a slower build.
ENV NATIVE_IMAGE_OPTIONS="--parallelism=1 --gc=serial -Os"

RUN ./mvnw -Pnative clean native:compile -DskipTests

FROM ubuntu:24.04

WORKDIR /app

COPY --from=build /workspace/target/investment /app/investment

RUN useradd --system --no-create-home --shell /usr/sbin/nologin appuser \
    && mkdir -p /app/logs \
    && chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["/app/investment"]

CMD ["-Xms16m", "-Xmx64m"]
