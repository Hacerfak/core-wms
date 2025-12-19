# --- Stage 1: Builder (Compilação) ---
# Usamos uma imagem Debian (não Alpine) para evitar erros de DNS/Rede no Maven
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# 1. Copia APENAS o pom.xml primeiro (Cache de dependências)
COPY pom.xml .

# 2. Baixa as dependências e plugins (modo offline)
RUN mvn dependency:go-offline -B

# 3. Agora sim copia o código fonte
COPY src ./src

# 4. Compila e Gera o JAR
RUN mvn clean package -DskipTests

# --- Stage 2: Runtime (Execução Leve) ---
FROM eclipse-temurin:21-jre

WORKDIR /app

# ==============================================================================
# CORREÇÃO PARA ERRO PKIX (SEFAZ / ICP-BRASIL)
# ==============================================================================
# 1. Garante que estamos como root para instalar pacotes
USER root

# 2. Instala wget para baixar os certificados
RUN apt-get update && apt-get install -y wget ca-certificates

# 3. Baixa a cadeia Raiz da ICP-Brasil (v5 e v10)
# ADICIONADO: --no-check-certificate para evitar erro de handshake SSL no build
RUN wget --no-check-certificate https://acraiz.icpbrasil.gov.br/credenciadas/RAIZ/ICP-Brasilv5.crt -O /tmp/icp-brasil-v5.crt && \
    wget --no-check-certificate https://acraiz.icpbrasil.gov.br/credenciadas/RAIZ/ICP-Brasilv10.crt -O /tmp/icp-brasil-v10.crt

# 4. Importa para o TrustStore do Java (cacerts)
RUN keytool -import -trustcacerts -keystore "$JAVA_HOME/lib/security/cacerts" \
    -storepass changeit -noprompt -alias icp-brasil-v5 -file /tmp/icp-brasil-v5.crt && \
    keytool -import -trustcacerts -keystore "$JAVA_HOME/lib/security/cacerts" \
    -storepass changeit -noprompt -alias icp-brasil-v10 -file /tmp/icp-brasil-v10.crt

# 5. Limpeza para manter a imagem leve
RUN rm /tmp/icp-brasil-v5.crt /tmp/icp-brasil-v10.crt && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*
# ==============================================================================

# Cria usuário de segurança (boa prática)
RUN groupadd -r wmsgroup && useradd -r -g wmsgroup wmsuser
USER wmsuser

# Copia apenas o JAR final do estágio anterior
COPY --from=builder /app/target/*.jar app.jar

# Configurações de Memória para Containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]