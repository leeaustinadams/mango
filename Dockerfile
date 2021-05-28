FROM openjdk:11-slim-bullseye
RUN mkdir -p /usr/src/mango
WORKDIR /usr/src/mango
COPY "$PWD/target/mango-standalone.jar" app-standalone.jar
COPY docker-entrypoint.sh /usr/local/bin/
ENTRYPOINT ["docker-entrypoint.sh"]

CMD ["java", "-jar", "app-standalone.jar"]

