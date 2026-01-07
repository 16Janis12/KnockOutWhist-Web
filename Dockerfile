FROM --platform=$TARGETPLATFORM eclipse-temurin:22-jre-alpine

# Install Argon2 CLI and libraries
RUN apk add --no-cache bash argon2 argon2-libs

WORKDIR /opt/playapp

# Copy staged Play build
COPY ./knockoutwhistweb/target/universal/stage /opt/playapp

# Expose the default Play port
EXPOSE 9000

# Set environment variables
ENV PLAY_HTTP_PORT=9000

# Run the Play app
ENTRYPOINT ["./bin/knockoutwhistweb"]
CMD ["-Dplay.server.pidfile.path=/dev/null"]