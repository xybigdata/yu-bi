FROM eclipse-temurin:21-jre

LABEL org.opencontainers.image.title="yu-bi" \
      org.opencontainers.image.description="yu-bi open source BI server" \
      org.opencontainers.image.source="https://github.com/xybigdata/yu-bi" \
      org.opencontainers.image.licenses="Apache-2.0"

WORKDIR /yu-bi

COPY yu-bi-server-*-install.zip /tmp/yu-bi-install.zip

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl unzip \
    && unzip -q /tmp/yu-bi-install.zip -d /yu-bi \
    && rm -f /tmp/yu-bi-install.zip \
    && apt-get purge -y --auto-remove unzip \
    && rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Shanghai

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --fail --silent http://127.0.0.1:8080/api/v1/sys/info | grep -q '"success":true' || exit 1

ENTRYPOINT ["java", "-server", "-Xms2G", "-Xmx2G", "--add-opens=java.base/java.lang=ALL-UNNAMED", "-Dspring.profiles.active=config", "-Dfile.encoding=UTF-8", "-cp", "lib/*", "datart.DatartServerApplication"]
