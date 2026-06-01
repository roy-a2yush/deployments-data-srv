# Build Stage
FROM azul/zulu-openjdk:26 AS builder
WORKDIR /app

# Pull pre-installed Maven directly from the official Maven image
COPY --from=maven:3.9.9-eclipse-temurin-21 /usr/share/maven /usr/share/maven
ENV MAVEN_HOME=/usr/share/maven
ENV PATH=$MAVEN_HOME/bin:$PATH

# Cache dependencies
COPY pom.xml .
COPY api-spec ./api-spec
RUN mvn dependency:go-offline -B

# Copy source code and build the application targeting Java 26
COPY . .
RUN mvn clean package -DskipTests

# Runtime Stage
FROM azul/zulu-openjdk:26-jre
WORKDIR /app

# Create a non-privileged user for security
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copy built artifact from the builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
