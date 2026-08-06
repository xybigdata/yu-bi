#!/usr/bin/env bash

set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "用法: $0 <安装包.zip> <预期版本>" >&2
  exit 2
fi

INSTALL_ZIP="$1"
EXPECTED_VERSION="${2#v}"
EXPECTED_NAME="yu-bi-server-v${EXPECTED_VERSION}-install.zip"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CANONICAL_DEMO_DB="${ROOT_DIR}/server/src/main/release/bin/h2/yubi.demo.mv.db"

failures=0

fail() {
  echo "发布包契约失败: $*" >&2
  failures=$((failures + 1))
}

if [[ ! -f "${INSTALL_ZIP}" ]]; then
  echo "安装包不存在: ${INSTALL_ZIP}" >&2
  exit 2
fi

if [[ "$(basename "${INSTALL_ZIP}")" != "${EXPECTED_NAME}" ]]; then
  fail "文件名应为 ${EXPECTED_NAME}"
fi

entries="$(unzip -Z1 "${INSTALL_ZIP}")"

require_entry() {
  local entry="$1"
  if ! grep -Fxq "${entry}" <<<"${entries}"; then
    fail "缺少 ${entry}"
  fi
}

require_entry "LICENSE"
require_entry "NOTICE"
require_entry "bin/yu-bi-server.sh"
require_entry "bin/h2/yubi.demo.mv.db"

unexpected_h2="$({ grep '^bin/h2/' <<<"${entries}" || true; } \
  | grep -Ev '^bin/h2/$|^bin/h2/yubi\.demo\.mv\.db$' || true)"
if [[ -n "${unexpected_h2}" ]]; then
  fail "bin/h2 含非受控文件: $(tr '\n' ' ' <<<"${unexpected_h2}")"
fi

if [[ ! -f "${CANONICAL_DEMO_DB}" ]]; then
  fail "缺少受控 demo 数据库源文件 ${CANONICAL_DEMO_DB#"${ROOT_DIR}/"}"
elif ! cmp -s "${CANONICAL_DEMO_DB}" <(unzip -p "${INSTALL_ZIP}" bin/h2/yubi.demo.mv.db); then
  fail "安装包 demo 数据库与受控源文件不一致"
fi

runtime_dir="$(mktemp -d "${TMPDIR:-/tmp}/yu-bi-release-contract.XXXXXX")"
cleanup() {
  rm -rf "${runtime_dir}"
}
trap cleanup EXIT
unzip -q "${INSTALL_ZIP}" bin/yu-bi-server.sh -d "${runtime_dir}"

if stat -c '%a' "${runtime_dir}/bin/yu-bi-server.sh" >/dev/null 2>&1; then
  script_mode="$(stat -c '%a' "${runtime_dir}/bin/yu-bi-server.sh")"
else
  script_mode="$(stat -f '%Lp' "${runtime_dir}/bin/yu-bi-server.sh")"
fi
if [[ "${script_mode}" != "755" ]]; then
  fail "bin/yu-bi-server.sh 权限应为 0755，实际为 ${script_mode}"
fi

maven_version="$(
  cd "${ROOT_DIR}"
  mvn -q -N help:evaluate -Dexpression=project.version -DforceStdout \
    | tr -d '\r' \
    | sed $'s/\033\\[[0-9;]*m//g'
)"
frontend_version="$(node -p "require('${ROOT_DIR}/frontend/package.json').version")"
frontend_lock_version="$(node -p "require('${ROOT_DIR}/frontend/package-lock.json').version")"

for version_source in \
  "Maven:${maven_version}" \
  "frontend/package.json:${frontend_version}" \
  "frontend/package-lock.json:${frontend_lock_version}"; do
  source_name="${version_source%%:*}"
  actual_version="${version_source#*:}"
  if [[ "${actual_version}" != "${EXPECTED_VERSION}" ]]; then
    fail "${source_name} 版本应为 ${EXPECTED_VERSION}，实际为 ${actual_version}"
  fi
done

project_jars="$({ grep '^lib/yu-bi-.*\.jar$' <<<"${entries}" || true; })"
if [[ -z "${project_jars}" ]]; then
  fail "未找到 yu-bi 项目 JAR"
else
  escaped_version="${EXPECTED_VERSION//./\\.}"
  mismatched_jars="$(grep -Ev "^lib/yu-bi-.+-${escaped_version}\\.jar$" <<<"${project_jars}" || true)"
  if [[ -n "${mismatched_jars}" ]]; then
    fail "项目 JAR 版本不一致: $(tr '\n' ' ' <<<"${mismatched_jars}")"
  fi
fi

test_dependency_pattern='^lib/(spring-boot-starter-test|spring-test|assertj-core|awaitility|byte-buddy-agent|hamcrest|jsonassert|junit-|mockito-|opentest4j|org\.jacoco\.agent|xmlunit-core)-.*\.jar$'
test_dependencies="$(grep -E "${test_dependency_pattern}" <<<"${entries}" || true)"
if [[ -n "${test_dependencies}" ]]; then
  fail "安装包含测试依赖: $(tr '\n' ' ' <<<"${test_dependencies}")"
fi

if (( failures > 0 )); then
  echo "发布包契约检查失败，共 ${failures} 项" >&2
  exit 1
fi

echo "发布包契约检查通过: ${INSTALL_ZIP} (${EXPECTED_VERSION})"
