#!/usr/bin/env bash
# Renders the Spigot page assets to ../*.png with headless Chrome.
# Usage: ./render.sh   (macOS, Google Chrome installed)
set -euo pipefail
cd "$(dirname "$0")"
CHROME="${CHROME:-/Applications/Google Chrome.app/Contents/MacOS/Google Chrome}"
OUT=..

shot() { # file width height output [query]
  "$CHROME" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1 \
    --default-background-color=00000000 --window-size="$2,$3" --virtual-time-budget=5000 \
    --screenshot="$PWD/$OUT/$4" "file://$PWD/$1${5:-}" >/dev/null 2>&1
  echo "rendered $4"
}

shot icon.html 256 256 icon.png
shot banner.html 1200 440 banner.png
shot how-it-works.html 1200 400 how-it-works.png
shot features.html 1200 580 features.png
for s in "features:Features:star" "how:How it works:link" "setup:Setup:gear" "commands:Commands:slash" \
         "placeholders:Placeholders:tag" "network:Networks & proxies:net" "config:Configuration:file" "support:Support:chat"; do
  IFS=: read -r name title icon <<<"$s"
  shot header.html 1200 96 "header-$name.png" "?t=$(python3 -c 'import urllib.parse,sys;print(urllib.parse.quote(sys.argv[1]))' "$title")&i=$icon"
done
