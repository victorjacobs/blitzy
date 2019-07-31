FROM gradle:jdk11 AS builder

COPY . /src
WORKDIR /src

RUN useradd -u 1107 blitzy
RUN gradle shadowJar


FROM openjdk:11-slim

COPY --from=builder /src/build/libs/blitzy-1.0-SNAPSHOT-all.jar /app/blitzy.jar
COPY --from=builder /etc/passwd /etc/passwd
COPY ./entrypoint.sh /app/entrypoint.sh

USER blitzy
EXPOSE 8080
WORKDIR /app

ENTRYPOINT ["./entrypoint.sh"]
