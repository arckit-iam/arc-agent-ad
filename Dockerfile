FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /opt/arc-agent-ad
COPY --from=builder /app/target/arc-agent-ad-*.jar app.jar

# Import CA cert into JVM truststore
COPY truststore/iap-ca.pem /tmp/iap-ca.pem
RUN keytool -importcert \
      -noprompt \
      -alias iap-ca \
      -file /tmp/iap-ca.pem \
      -keystore $JAVA_HOME/lib/security/cacerts \
      -storepass changeit && \
    rm /tmp/iap-ca.pem

ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /opt/arc-agent-ad/app.jar"]
