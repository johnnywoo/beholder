#!/bin/bash

export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get install -qqy --no-install-recommends \
    less \
    tree \
    nano \
    procps \
    dnsutils \
    net-tools \
    mtr \
    telnet \
    netcat
