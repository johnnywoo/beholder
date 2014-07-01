#!/bin/bash

set -e

./gradlew assemble

java -jar build/libs/beholder-0.1.jar
