# syntax=docker/dockerfile:1

########################################
# сборка приложения
########################################

FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/

RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY config/ config/
COPY src/ src/

# Собираем только толстый (executable) jar, без тестов и checkstyle
RUN ./gradlew --no-daemon bootJar

########################################
# distroless
########################################

FROM gcr.io/distroless/java25-debian13:nonroot

WORKDIR /app

COPY --from=build /app/build/libs/app-1.0-SNAPSHOT.jar app.jar

ENV SPRING_PROFILES_ACTIVE=production

EXPOSE 8080

CMD ["app.jar"]