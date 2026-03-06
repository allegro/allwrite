FROM eclipse-temurin:21
RUN apt update
RUN apt install -y git
ADD ./allwrite-runner/build/distributions/allwrite.tar /app
RUN ln -s /app/allwrite/bin/allwrite-runner /allwrite
ENTRYPOINT []
