#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="${ROOT_DIR}/.tmp"
LOG_FILE="${LOG_DIR}/demo-health.log"
INSTALL_ZIP_GLOB="${DATART_DEMO_INSTALL_ZIP_GLOB:-yu-bi-server-*-install.zip}"
PORT="${DATART_DEMO_PORT:-8080}"
HEALTH_URL="${DATART_DEMO_HEALTH_URL:-http://127.0.0.1:${PORT}/api/v1/sys/info}"
STARTUP_TIMEOUT_SECONDS="${DATART_DEMO_TIMEOUT_SECONDS:-180}"
SPRING_PROFILE="${DATART_DEMO_PROFILE:-demo}"
START_CLASS="${DATART_DEMO_START_CLASS:-datart.DatartServerApplication}"
JAVA_OPENS_ARGS=(
  "--add-opens=java.base/java.lang=ALL-UNNAMED"
)

mkdir -p "${LOG_DIR}"
rm -f "${LOG_FILE}"

SERVER_PID=""
RUNTIME_DIR=""

cleanup() {
  if [[ -n "${SERVER_PID}" ]] && kill -0 "${SERVER_PID}" 2>/dev/null; then
    kill "${SERVER_PID}" 2>/dev/null || true
    wait "${SERVER_PID}" 2>/dev/null || true
  fi
  if [[ -n "${RUNTIME_DIR}" ]] && [[ -d "${RUNTIME_DIR}" ]]; then
    rm -rf "${RUNTIME_DIR}"
  fi
}

trap cleanup EXIT

cd "${ROOT_DIR}"

shopt -s nullglob
zip_candidates=(${INSTALL_ZIP_GLOB})
shopt -u nullglob

if [[ ${#zip_candidates[@]} -ne 1 ]]; then
  echo "无法唯一定位安装包，匹配模式: ${INSTALL_ZIP_GLOB}" >&2
  printf '匹配结果:\n' >&2
  printf '  %s\n' "${zip_candidates[@]:-<none>}" >&2
  exit 1
fi

INSTALL_ZIP="${zip_candidates[0]}"
RUNTIME_DIR="$(mktemp -d "${LOG_DIR}/demo-runtime.XXXXXX")"
unzip -q "${INSTALL_ZIP}" -d "${RUNTIME_DIR}"

cd "${RUNTIME_DIR}"

java \
  "${JAVA_OPENS_ARGS[@]}" \
  -Dspring.profiles.active="${SPRING_PROFILE}" \
  -Dserver.port="${PORT}" \
  -Dfile.encoding=UTF-8 \
  -cp "lib/*" \
  "${START_CLASS}" \
  >"${LOG_FILE}" 2>&1 &
SERVER_PID=$!

deadline=$((SECONDS + STARTUP_TIMEOUT_SECONDS))

while (( SECONDS < deadline )); do
  if curl --silent --show-error --fail "${HEALTH_URL}" | grep -q '"success":true'; then
    echo "健康检查通过: ${HEALTH_URL}"
    exit 0
  fi
  sleep 2
done

echo "健康检查失败: ${HEALTH_URL}" >&2
echo "最近启动日志:" >&2
tail -n 120 "${LOG_FILE}" >&2 || true
exit 1
