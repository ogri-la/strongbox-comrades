#!/bin/bash
set -e

echo "generating 'comrades.csv' from 'comrades.raw'"
lein run

echo "compiling to javascript"
lein do clean, cljsbuild once min

echo "review changes"
vivaldi-stable resources/public/index.html

echo "syncing changes to website"
rsync -av resources/public/ ../wow-addon-managers.github.io/

echo

echo "now commit changes to '../wow-addon-managers.github.io' and push to github"
