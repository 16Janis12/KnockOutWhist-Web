# === Stage 1: Build the Play application ===
FROM --platform=$BUILDPLATFORM sbtscala/scala-sbt:eclipse-temurin-alpine-22_36_1.10.3_3.5.1 AS builder

WORKDIR /app

# Install Node.js and Less CSS preprocessor
USER root
RUN apk add --no-cache nodejs npm && \
    npm install -g less

# Cache dependencies first
COPY project ./project
COPY build.sbt ./
RUN sbt -Dscoverage.skip=true update

# Copy the rest of the code
COPY . .


# Build the app and stage it
RUN sbt -Dscoverage.skip=true clean stage

# === Stage 2: Runtime image ===
FROM --platform=$TARGETPLATFORM eclipse-temurin:22-jre-alpine

# Install Argon2 CLI and libraries
RUN apk add --no-cache bash argon2 argon2-libs

WORKDIR /opt/playapp

# Copy staged Play build
COPY --from=builder /app/knockoutwhistweb/target/universal/stage /opt/playapp

# Expose the default Play port
EXPOSE 9000

# Set environment variables
ENV PLAY_HTTP_PORT=9000

# Run the Play app
ENTRYPOINT ["./bin/knockoutwhistweb"]
CMD ["-Dplay.server.pidfile.path=/dev/null"]