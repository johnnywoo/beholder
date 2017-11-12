#!/bin/bash

###
## BUILD
#
# this script builds a docker container with the beholder jar
# which can then be executed with `docker run`
#

# changing to project directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"

# building the jar
./gradlew jar

# building the container
docker build -t beholder .

if [ "x$1" = "x-r" ]; then
    echo
    echo '=== RUN ============================================='
    docker run -ti beholder
fi
