FROM debian:stretch

# Installing base packages
RUN set -xe \
    && export DEBIAN_FRONTEND=noninteractive \
    && apt-get update -qq \
    && apt-get dist-upgrade -qq \
    && apt-get install -qqy --no-install-recommends \
        openjdk-8-jre-headless \
    && mkdir -p /etc/beholder

COPY build/libs /root
COPY docker/beholder /sbin/
COPY docker/beholder.conf /etc/beholder/

CMD java -jar /root/beholder*.jar --config-file=/etc/beholder/beholder.conf
