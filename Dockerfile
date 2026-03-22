FROM eclipse-temurin:21
RUN apt update
RUN apt install -y git
ADD ./allwrite-cli/build/distributions/allwrite.tar /app
RUN ln -s /app/allwrite/bin/allwrite-cli /allwrite
ENTRYPOINT []
