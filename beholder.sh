#!/bin/bash

set -e

./gradlew

java -jar build/libs/beholder.jar "$@"
