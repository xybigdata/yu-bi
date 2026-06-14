FROM eclipse-temurin:21-jre

LABEL "author"="tl"

WORKDIR /yu-bi

COPY yu-bi-server-*-install.zip /tmp/yu-bi-install.zip

RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip \
    && unzip -q /tmp/yu-bi-install.zip -d /yu-bi \
    && rm -f /tmp/yu-bi-install.zip \
    && apt-get purge -y --auto-remove unzip \
    && rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Shanghai

EXPOSE 8080

ENTRYPOINT ["java", "-server", "-Xms2G", "-Xmx2G", "--add-opens=java.base/java.lang=ALL-UNNAMED", "-Dspring.profiles.active=config", "-Dfile.encoding=UTF-8", "-cp", "lib/*", "datart.DatartServerApplication"]
