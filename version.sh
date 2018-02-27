#!/bin/bash

# base version (major and minor) is defined in gradle build script
baseVersion="$( grep version build.gradle | head -n 1 | cut -d"'" -f2 )"

# patch version is the number of commits
commitNumber="$( git log | wc -l )"
# removing whitespace
commitNumber="${commitNumber// /}"

version="$baseVersion.$commitNumber"
echo "$version"

if [ "x$1" = "x-t" ]; then
    git tag "$version"
    git push origin "$version"
fi
