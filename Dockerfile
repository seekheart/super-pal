FROM gradle:5.6.4-jdk11 AS build

RUN mkdir /app
WORKDIR /app

COPY --chown=gradle:gradle . /app

RUN gradle clean build --no-daemon

FROM openjdk:11-jre-slim

RUN mkdir /app
WORKDIR /app

COPY --from=build /app/build/libs/superpal.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]