
#
#
# Build stage -- Account Management Jar
#
FROM eclipse-temurin:17-jdk-jammy AS build2
ENV HOME=/usr/app/account-management
RUN mkdir -p $HOME
WORKDIR $HOME
ADD . $HOME
#RUN --mount=type=cache,target=/root/.m2 ./mvnw -f $HOME/pom.xml clean install -DskipTests
RUN ./mvnw clean package -DskipTests

#
# Package stage
#
FROM eclipse-temurin:17-jre-jammy
COPY --from=build2 /usr/app/account-management/target/*.jar account.jar
ENV SPRINGPROFILES=prod
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.32.1/opentelemetry-javaagent.jar opentelemetry-javaagent.jar
ENV JAVA_TOOL_OPTIONS "-javaagent:opentelemetry-javaagent.jar -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=${SPRINGPROFILES}  -Ddd.profiling.enabled=true -XX:FlightRecorderOptions=stackdepth=256 -Ddd.logs.injection=true -Ddd.service=AccountManagementApplication -Ddd.version=1.0"
EXPOSE 8180
ENTRYPOINT ["sh","-cx","java -jar account.jar"]




