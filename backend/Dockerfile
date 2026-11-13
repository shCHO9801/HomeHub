FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

COPY src src

RUN ./gradlew bootJar --no-daemon


FROM eclipse-temurin:21-jre

RUN groupadd --system homehub \
    && useradd --system --gid homehub homehub

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

USER homehub

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
