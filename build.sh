#!/usr/bin/env sh
set -e
mvn clean package
cp studio/target/*-jar-with-dependencies.jar alis-studio/studio.jar
rm -f alis.zip
zip -r alis.zip alis-studio/