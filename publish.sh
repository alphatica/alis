#!/usr/bin/env bash
set -e
export BRANCH
BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$BRANCH" = "private" ]; then
    echo "Private branch!"
    exit 1
fi
mvn clean package
git push
cp studio/target/*-jar-with-dependencies.jar alis-studio/studio.jar
rm -f alis.zip
zip -r alis.zip alis-studio/
cp alis.zip  ../alphatica.com/web/en/software/alis/docs/download/alis.zip
