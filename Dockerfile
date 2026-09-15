FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 application
WORKDIR /application
COPY --from=build /workspace/target/prices-0.0.1-SNAPSHOT.jar application.jar
USER application
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/application/application.jar"]
