#!/usr/bin/env bash
# Buduje alis.zip i kopiuje go w dwa miejsca:
#  1. do zrodel strony (../alphatica.com/web/src/public/downloads/), zeby deploy
#     strony zawieral plik i go nie usunal,
#  2. prosto na serwer (/var/www/html/downloads/), bez deployu strony.
set -e
export BRANCH
BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$BRANCH" = "private" ]; then
    echo "Private branch!"
    exit 1
fi

deploy_target="${DEPLOY_SSH_TARGET:-ubuntu@alphatica.com}"
deploy_port="${DEPLOY_SSH_PORT:-23}"
site_copy="../alphatica.com/web/src/public/downloads/alis.zip"

mvn clean package
git push
cp studio/target/*-jar-with-dependencies.jar alis-studio/studio.jar
rm -f alis.zip
zip -r alis.zip alis-studio/

mkdir -p -- "$(dirname -- "$site_copy")"
cp -- alis.zip "$site_copy"
echo "Skopiowano do strony: $site_copy"

scp -P "$deploy_port" alis.zip "$deploy_target:alis.zip.upload"
ssh -p "$deploy_port" "$deploy_target" \
    "sudo install -d -m 0755 /var/www/html/downloads \
     && sudo install -m 0644 alis.zip.upload /var/www/html/downloads/.alis.zip.new \
     && sudo mv -f /var/www/html/downloads/.alis.zip.new /var/www/html/downloads/alis.zip \
     && rm -f alis.zip.upload"
echo "Wgrano na serwer: /var/www/html/downloads/alis.zip"
