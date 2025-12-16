# --- Stage 1: Builder (Compilação) ---
# Usamos uma imagem Debian (não Alpine) para evitar erros de DNS/Rede no Maven
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# 1. O PULO DO GATO: Copia APENAS o pom.xml primeiro
COPY pom.xml .

# 2. Baixa as dependências e plugins (modo offline)
# O Docker vai CACHEAR essa linha. Se você não mudar o pom.xml,
# ele nunca mais vai baixar a internet inteira de novo nas próximas builds.
RUN mvn dependency:go-offline -B

# 3. Agora sim copia o código fonte
COPY src ./src

# 4. Compila e Gera o JAR (usando as dependências que já baixou no passo 2)
RUN mvn clean package -DskipTests

# --- Stage 2: Runtime (Execução Leve) ---
FROM eclipse-temurin:21-jre

WORKDIR /app

# Cria usuário de segurança (boa prática)
RUN groupadd -r wmsgroup && useradd -r -g wmsgroup wmsuser
USER wmsuser

# Copia apenas o JAR final do estágio anterior
COPY --from=builder /app/target/*.jar app.jar

# Configurações de Memória para Containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]