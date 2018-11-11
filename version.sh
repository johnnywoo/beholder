#!/bin/bash

set -e

# base version (major and minor) is defined in gradle build script
baseVersion="$( grep version build.gradle.kts | head -n 1 | cut -d'"' -f2 )"

# patch version is the number of commits
if [ "x$1" = "x--next" ]; then
    commitNumber="$( (git rev-list master ; echo) | wc -l )"
else
    commitNumber="$( git rev-list master | wc -l )"
fi
# removing whitespace
commitNumber="${commitNumber// /}"

version="$baseVersion.$commitNumber"
echo "$version"

if [ "x$1" = "x-f" ]; then
    echo "$version" > "${2:-./src/main/resources/version.txt}"
    sed -i '' "s/johnnywoo\/beholder:[0-9][0-9.]*/johnnywoo\/beholder:$version/" ./README.md
fi

if [ "x$1" = "x-t" ]; then
    git tag "$version"
    git push origin "$version"
fi
