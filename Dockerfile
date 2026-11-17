FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY librerias ./librerias
COPY src ./src
RUN mkdir classes \
    && javac -encoding UTF-8 -cp "librerias/*" -d classes $(find src -name "*.java")

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/classes ./classes
COPY librerias ./librerias
COPY web ./web
COPY misPersistencias ./misPersistencias

ENV PORT=8080
EXPOSE 8080

CMD ["java", "-cp", "classes:librerias/*", "edu.unimag.AppWeb"]
