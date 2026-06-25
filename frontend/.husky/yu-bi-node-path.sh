#!/usr/bin/env sh

if command -v node >/dev/null 2>&1 && command -v npm >/dev/null 2>&1; then
  return 0
fi

for node_bin in \
  /opt/homebrew/opt/node@24/bin \
  /usr/local/opt/node@24/bin \
  /opt/homebrew/Cellar/node@24/*/bin \
  /usr/local/Cellar/node@24/*/bin \
  /Applications/Codex.app/Contents/Resources/cua_node/bin
do
  if [ -x "$node_bin/node" ] && [ -x "$node_bin/npm" ]; then
    PATH="$node_bin:$PATH"
    export PATH
    return 0
  fi
done

echo "未找到可用的 Node/npm，请确认已安装 Node 24 并加入 PATH。" >&2
return 1
