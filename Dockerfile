FROM eclipse-temurin:21-jre

LABEL "author"="tl"

WORKDIR /datart

COPY datart-server-*-install.zip /tmp/datart-install.zip

RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip \
    && unzip -q /tmp/datart-install.zip -d /datart \
    && rm -f /tmp/datart-install.zip \
    && apt-get purge -y --auto-remove unzip \
    && rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Shanghai

EXPOSE 8080

ENTRYPOINT ["java", "-server", "-Xms2G", "-Xmx2G", "--add-opens=java.base/java.lang=ALL-UNNAMED", "-Dspring.profiles.active=config", "-Dfile.encoding=UTF-8", "-cp", "lib/*", "datart.DatartServerApplication"]
