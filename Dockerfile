FROM debian:stretch

# Installing base packages
RUN set -xe \
    && export DEBIAN_FRONTEND=noninteractive \
    && apt-get update -qq \
    && apt-get dist-upgrade -qq \
    && apt-get install -qqy --no-install-recommends \
        openjdk-8-jre-headless

COPY build/libs /root

CMD java -jar /root/beholder*.jar
