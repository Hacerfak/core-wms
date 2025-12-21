# ==============================================================================
# ESTÁGIO 1: BUILD (Compilação)
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# 1. Cache de Dependências (Otimização)
# Copia apenas o pom.xml primeiro para baixar as dependências
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. Compilação do Código Fonte
# Copia a pasta src inteira
COPY src ./src
# Compila o JAR ignorando testes para ser mais rápido
RUN mvn clean package -DskipTests

# ==============================================================================
# ESTÁGIO 2: RUNTIME (Execução)
# ==============================================================================
# Usamos a imagem JRE (apenas execução) baseada em Linux Debian
FROM eclipse-temurin:21-jre
WORKDIR /app

# ------------------------------------------------------------------------------
# CONFIGURAÇÃO DE CERTIFICADOS (SEFAZ / ICP-BRASIL)
# ------------------------------------------------------------------------------
USER root

# Instala utilitários necessários
RUN apt-get update && apt-get install -y wget ca-certificates

# Baixa e Instala Cadeia de Certificados V5 e V10
RUN wget --no-check-certificate https://acraiz.icpbrasil.gov.br/credenciadas/RAIZ/ICP-Brasilv5.crt -O /tmp/icp-brasil-v5.crt && \
    wget --no-check-certificate https://acraiz.icpbrasil.gov.br/credenciadas/RAIZ/ICP-Brasilv10.crt -O /tmp/icp-brasil-v10.crt

# Importa para o Keystore do Java
RUN keytool -import -trustcacerts -keystore "$JAVA_HOME/lib/security/cacerts" \
    -storepass changeit -noprompt -alias icp-brasil-v5 -file /tmp/icp-brasil-v5.crt && \
    keytool -import -trustcacerts -keystore "$JAVA_HOME/lib/security/cacerts" \
    -storepass changeit -noprompt -alias icp-brasil-v10 -file /tmp/icp-brasil-v10.crt

# Limpeza para reduzir tamanho da imagem
RUN rm /tmp/icp-brasil-v5.crt /tmp/icp-brasil-v10.crt && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# ------------------------------------------------------------------------------
# CONFIGURAÇÃO DA APLICAÇÃO
# ------------------------------------------------------------------------------

# Cria usuário não-root por segurança
RUN groupadd -r wmsgroup && useradd -r -g wmsgroup wmsuser
USER wmsuser

# Copia o JAR gerado no Estágio 1
COPY --from=builder /app/target/*.jar app.jar

# Configurações de Memória Java para Containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

# Inicia a aplicação
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]