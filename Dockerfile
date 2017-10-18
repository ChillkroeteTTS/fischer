FROM openjdk

RUN adduser --disabled-password --gecos "" --no-create-home conan
RUN mkdir /conan/ && chown -R conan:conan /conan
USER conan
WORKDIR "/conan"

COPY "target/conan-0.1.0-SNAPSHOT-standalone.jar" "/conan/conan.jar"

CMD ["java", "-jar", "conan.jar"]