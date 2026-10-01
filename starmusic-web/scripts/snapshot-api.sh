#!/usr/bin/env bash
# 把執行中的後端 API 快照成靜態檔，供 FTP 純靜態部署使用。
# 用法：先啟動後端（gateway 於 8080），再執行 scripts/snapshot-api.sh [base-url]
# 產出：public/assets/api/**/*.json 與 public/media/radio-ch*.wav
set -u
BASE="${1:-http://localhost:8080}"
cd "$(dirname "$0")/.."
OUT="public/assets/api"
mkdir -p "$OUT"

get() { # $1 = api path, $2 = dest file under $OUT
  mkdir -p "$(dirname "$OUT/$2")"
  curl -sf "$BASE/api$1" -o "$OUT/$2" || echo "  WARN failed: /api$1" >&2
}
urlenc() { python -c "import urllib.parse,sys;sys.stdout.write(urllib.parse.quote(sys.argv[1],safe=''))" "$1"; }
getq() { # $1 = api path, $2 = dest file, rest = raw k=v query pairs
  local path="$1" dest="$2"; shift 2
  local qs="" kv
  for kv in "$@"; do qs+="${qs:+&}${kv%%=*}=$(urlenc "${kv#*=}")"; done
  mkdir -p "$(dirname "$OUT/$dest")"
  curl -sf "$BASE/api$path?$qs" -o "$OUT/$dest" \
    || echo "  WARN failed: /api$path $*" >&2
}
jsonvals() { python -c "import json,sys;[print(x) for x in json.load(sys.stdin)]" | tr -d '\r'; }
jsonids() { python -c "import json,sys;[print(x['id']) for x in json.load(sys.stdin)]" | tr -d '\r'; }

echo "== news =="
get /news news.json
get /news/breaking news/breaking.json
get /news/categories news/categories.json
curl -sf "$BASE/api/news/categories" | jsonvals | while IFS= read -r c; do
  getq /news "news/category=$c.json" "category=$c"
done
curl -sf "$BASE/api/news" | jsonids | while IFS= read -r id; do
  get "/news/$id" "news/$id.json"
done

echo "== videos =="
get /videos videos.json
get /videos/home videos/home.json
get /videos/ranking videos/ranking.json
get /videos/categories videos/categories.json
curl -sf "$BASE/api/videos/categories" | jsonvals | while IFS= read -r c; do
  getq /videos "videos/category=$c.json" "category=$c"
done
getq /videos "videos/vip=true.json" "vip=true"
curl -sf "$BASE/api/videos" | jsonids | while IFS= read -r id; do
  get "/videos/$id" "videos/$id.json"
  get "/videos/$id/comments" "videos/$id/comments.json"
  get "/videos/$id/favorite" "videos/$id/favorite.json"
done

echo "== radio =="
get /radio/channels radio/channels.json
get /radio/programs radio/programs.json
python - "$OUT/radio/channels.json" <<'EOF'
import re, sys
p = sys.argv[1]
s = open(p, encoding='utf-8').read()
s = re.sub(r'/api/radio/streams/(\d+)', r'/media/radio-ch\1.wav', s)
open(p, 'w', encoding='utf-8').write(s)
EOF
curl -sf "$BASE/api/radio/channels" | jsonids | while IFS= read -r id; do
  curl -sf "$BASE/api/radio/streams/$id" -o "public/media/radio-ch$id.wav" \
    || echo "  WARN stream $id" >&2
  getq /radio/programs "radio/programs/channelId=$id.json" "channelId=$id"
done

echo "== magazines =="
get /magazines magazines.json
get /magazines/latest magazines/latest.json
get /magazines/categories magazines/categories.json
curl -sf "$BASE/api/magazines/categories" | jsonvals | while IFS= read -r c; do
  getq /magazines "magazines/category=$c.json" "category=$c"
done

echo "== products =="
get /products products.json
get /products/categories products/categories.json
curl -sf "$BASE/api/products/categories" | jsonvals | while IFS= read -r c; do
  getq /products "products/category=$c.json" "category=$c"
done

echo "== posts =="
getq /posts "posts/page=0&size=12.json" "page=0" "size=12"
for t in VIDEO AUDIO IMAGE ARTICLE; do
  getq /posts "posts/type=$t&page=0&size=12.json" "type=$t" "page=0" "size=12"
done
python -c "import json,sys;[print(p['id']) for p in json.load(sys.stdin)['content']]" \
  < "$OUT/posts/page=0&size=12.json" | tr -d '\r' | while IFS= read -r id; do
  get "/posts/$id" "posts/$id.json"
  get "/posts/$id/comments" "posts/$id/comments.json"
  get "/posts/$id/likes" "posts/$id/likes.json"
done

echo "== done =="
find "$OUT" -name "*.json" | wc -l
