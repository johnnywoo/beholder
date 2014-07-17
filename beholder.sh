#!/bin/bash

set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

jar_file="$DIR/build/libs/beholder.jar"

# only build the gradle project if jar is older than the sources
if [ -e "$jar_file" ]; then
    newest_source_file_mtime="$(find "$DIR" -type f -path '*/src/*' -printf '%T@ %p\n' | sort -n | tail -1 | cut -f1 -d.)"
    jar_file_mtime="$(find "$DIR" -path "$jar_file" -printf '%T@ %p\n' | sort -n | tail -1 | cut -f1 -d.)"

    if [ "$newest_source_file_mtime" -gt "$jar_file_mtime" ]; then
        ./gradlew
    fi
else
    # there is no jar yet
    ./gradlew
fi

java -jar "$jar_file" "$@"
