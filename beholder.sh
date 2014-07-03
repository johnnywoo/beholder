#!/bin/bash

set -e

./gradlew assemble

java -jar build/libs/beholder.jar 3822
