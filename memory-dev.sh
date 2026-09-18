#!/usr/bin/env bash
set -euo pipefail

# 本地一键启动 / 停止 memory-service + memory-client
# 用法: ./memory-dev.sh start | stop | restart | status | wait

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
SERVICE_PORT="${MEMORY_SERVICE_PORT:-9090}"
HEALTH_URL="${MEMORY_HEALTH_URL:-http://127.0.0.1:${SERVICE_PORT}/api/health}"
WAIT_SECONDS="${MEMORY_WAIT_SECONDS:-3}"
RUN_DIR="${TMPDIR:-/tmp}/memory-dev"
SERVICE_MAIN="com.murong.ecp.tools.fx.MemoryServiceApplication"
CLIENT_MAIN="com.murong.ecp.tools.fx.MemoryApplication"

mkdir -p "${RUN_DIR}"

log() {
  echo "[memory-dev] $*"
}

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "缺少命令: $1" >&2
    exit 1
  fi
}

pids_by_pattern() {
  pgrep -f "$1" 2>/dev/null || true
}

is_service_up() {
  curl -sf "${HEALTH_URL}" >/dev/null 2>&1
}

print_pids() {
  local label="$1"
  local pattern="$2"
  local pids
  pids="$(pids_by_pattern "${pattern}")"
  if [[ -n "${pids}" ]]; then
    log "${label}: ${pids//$'\n'/, }"
  else
    log "${label}: 未运行"
  fi
}

stop_by_pattern() {
  local pattern="$1"
  local pids
  pids="$(pids_by_pattern "${pattern}")"
  if [[ -z "${pids}" ]]; then
    return 0
  fi
  log "停止进程 ${pattern} -> ${pids//$'\n'/, }"
  # shellcheck disable=SC2086
  kill ${pids} 2>/dev/null || true
  sleep 1
  pids="$(pids_by_pattern "${pattern}")"
  if [[ -n "${pids}" ]]; then
    # shellcheck disable=SC2086
    kill -9 ${pids} 2>/dev/null || true
  fi
}

cmd_wait() {
  local timeout="${1:-${WAIT_SECONDS}}"
  log "等待 ${timeout}s 后启动 memory-client"
  sleep "${timeout}"
}

cmd_status() {
  if command -v curl >/dev/null 2>&1 && is_service_up; then
    log "memory-service: 运行中 (${HEALTH_URL})"
  else
    log "memory-service: 未探测到健康检查"
  fi
  print_pids "MemoryServiceApplication" "${SERVICE_MAIN}"
  print_pids "MemoryApplication" "${CLIENT_MAIN}"
}

cmd_stop() {
  stop_by_pattern "${CLIENT_MAIN}"
  stop_by_pattern "${SERVICE_MAIN}"
  stop_by_pattern "spring-boot:run .*-pl memory-client"
  stop_by_pattern "spring-boot:run .*-pl memory-service"
  stop_by_pattern "-pl memory-client .*spring-boot:run"
  stop_by_pattern "-pl memory-service .*spring-boot:run"
  stop_by_pattern "memory-client/pom.xml .*spring-boot:run"
  stop_by_pattern "memory-service/pom.xml .*spring-boot:run"
  if command -v lsof >/dev/null 2>&1; then
    local port_pids
    port_pids="$(lsof -ti tcp:"${SERVICE_PORT}" 2>/dev/null || true)"
    if [[ -n "${port_pids}" ]]; then
      log "释放端口 ${SERVICE_PORT}: ${port_pids//$'\n'/, }"
      # shellcheck disable=SC2086
      kill ${port_pids} 2>/dev/null || true
      sleep 1
      port_pids="$(lsof -ti tcp:"${SERVICE_PORT}" 2>/dev/null || true)"
      if [[ -n "${port_pids}" ]]; then
        # shellcheck disable=SC2086
        kill -9 ${port_pids} 2>/dev/null || true
      fi
    fi
  fi
  log "已停止"
}

start_service() {
  if [[ -n "$(pids_by_pattern "${SERVICE_MAIN}")" ]]; then
    log "memory-service 已在运行，跳过启动"
    return 0
  fi
  need_cmd mvn
  log "安装依赖模块到本地仓库 ..."
  mvn -f "${ROOT_DIR}/pom.xml" -pl memory-lib,memory-common -am -DskipTests install
  log "启动 memory-service ..."
  nohup mvn -f "${ROOT_DIR}/memory-service/pom.xml" -DskipTests spring-boot:run \
    > "${RUN_DIR}/service.log" 2>&1 &
  echo $! > "${RUN_DIR}/service.mvn.pid"
  log "memory-service Maven PID=$(cat "${RUN_DIR}/service.mvn.pid")，日志: ${RUN_DIR}/service.log"
  cmd_wait
}

start_client_fg() {
  need_cmd mvn
  if [[ -n "$(pids_by_pattern "${CLIENT_MAIN}")" ]]; then
    trap - EXIT INT TERM
    log "memory-client 已在运行，不再重复启动"
    return 0
  fi
  log "启动 memory-client ..."
  mvn -f "${ROOT_DIR}/memory-client/pom.xml" -DskipTests spring-boot:run
}

cmd_start() {
  start_service
  start_client_fg
}

usage() {
  cat <<EOF
本地一键启动 / 停止 Memory

用法:
  ./memory-dev.sh start      启动 service，等待 3 秒后启动 client（前台，Ctrl+C 或 IDEA 停止会一起关掉）
  ./memory-dev.sh stop       停止 service 和 client
  ./memory-dev.sh restart    先停再启
  ./memory-dev.sh status     查看运行状态
  ./memory-dev.sh wait       仅等待 3 秒（给 IDEA 启动 client 前使用）

环境变量:
  MEMORY_SERVICE_PORT   默认 9090
  MEMORY_WAIT_SECONDS   默认 3
EOF
}

COMMAND="${1:-}"
case "${COMMAND}" in
  start)
    trap 'cmd_stop' EXIT INT TERM
    cmd_start
    ;;
  stop)
    cmd_stop
    ;;
  restart)
    cmd_stop
    trap 'cmd_stop' EXIT INT TERM
    cmd_start
    ;;
  status)
    cmd_status
    ;;
  wait)
    cmd_wait
    ;;
  *)
    usage
    exit 1
    ;;
esac
