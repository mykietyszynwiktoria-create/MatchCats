FROM eclipse-temurin:21-jdk AS build
WORKDIR /source
COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x gradlew
COPY src src
COPY frontend frontend
RUN ./gradlew bootJar --no-daemon --console=plain

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build --chown=10001:10001 /source/build/libs/*-SNAPSHOT.jar /app/matchcats.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/matchcats.jar"]
