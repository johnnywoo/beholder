#!/bin/bash

# base version (major and minor) is defined in gradle build script
baseVersion="$( grep version build.gradle | head -n 1 | cut -d"'" -f2 )"

# patch version is the number of commits
commitNumber="$( git log | wc -l )"
# removing whitespace
commitNumber="${commitNumber// /}"

echo "$baseVersion.$commitNumber"
