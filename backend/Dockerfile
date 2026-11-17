# Build em duas etapas: a imagem final leva só o JRE e o .jar (menor e mais rápida de subir).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B -DskipTests package && mv target/*.jar app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home app
COPY --from=build /app/app.jar app.jar
USER app

# Memória curta de propósito (plano grátis tem ~512 MB): heap pequeno, GC simples, só o compilador rápido.
ENV JAVA_TOOL_OPTIONS="-Xmx300m -Xms128m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -Xss512k -XX:TieredStopAtLevel=1"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
