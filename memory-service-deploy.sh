#!/usr/bin/env bash
set -euo pipefail

# 将 memory-service 编译打包并部署到远程服务器 /home/appadmin/memory

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
REMOTE_HOST="${REMOTE_HOST:-10.1.145.113}"
REMOTE_USER="${REMOTE_USER:-appadmin}"
if [[ -z "${REMOTE_PASS:-}" ]]; then
  REMOTE_PASS=']5[Y}P6A+!V40v'
fi
REMOTE_DIR="${REMOTE_DIR:-/home/appadmin/memory}"
REMOTE_PORT="${REMOTE_PORT:-22}"
REMOTE_JAVA_HOME="${REMOTE_JAVA_HOME:-/opt/jdk}"
APP_PORT="${APP_PORT:-8080}"
APP_NAME="memory-service"
JAR_NAME="${APP_NAME}.jar"

export SSHPASS="${REMOTE_PASS}"

SSH_OPTS=(-o StrictHostKeyChecking=accept-new -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR -o ConnectTimeout=15 -o ServerAliveInterval=15 -o ServerAliveCountMax=3 -p "${REMOTE_PORT}")
SCP_OPTS=(-o StrictHostKeyChecking=accept-new -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR -o ConnectTimeout=15 -o ServerAliveInterval=15 -o ServerAliveCountMax=3 -C -P "${REMOTE_PORT}")

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "缺少命令: $1" >&2
    exit 1
  fi
}

ssh_run() {
  sshpass -e ssh "${SSH_OPTS[@]}" "${REMOTE_USER}@${REMOTE_HOST}" "$@"
}

scp_to() {
  sshpass -e scp "${SCP_OPTS[@]}" "$1" "${REMOTE_USER}@${REMOTE_HOST}:$2"
}

need_cmd mvn
need_cmd ssh
need_cmd scp
need_cmd sshpass

cd "${ROOT_DIR}"
if [[ "${SKIP_BUILD:-0}" == "1" ]]; then
  echo "==> 跳过编译（SKIP_BUILD=1）"
else
  echo "==> 本地编译打包 memory-service"
  mvn -pl memory-service -am clean package -DskipTests
fi

JAR_FILE="$(find "${ROOT_DIR}/memory-service/target" -maxdepth 1 -type f -name "${APP_NAME}-*.jar" ! -name "*.original" | head -n 1)"
if [[ -z "${JAR_FILE}" || ! -f "${JAR_FILE}" ]]; then
  echo "未找到可执行 jar: memory-service/target/${APP_NAME}-*.jar" >&2
  exit 1
fi
echo "==> 产物: ${JAR_FILE}"

START_SCRIPT="$(mktemp)"
STOP_SCRIPT="$(mktemp)"
trap 'rm -f "${START_SCRIPT}" "${STOP_SCRIPT}"' EXIT

cat > "${START_SCRIPT}" <<EOF
#!/usr/bin/env bash
set -euo pipefail
APP_HOME="${REMOTE_DIR}"
APP_PORT="${APP_PORT}"
JAR_NAME="${JAR_NAME}"
APP_NAME="${APP_NAME}"
JAVA_HOME="\${JAVA_HOME:-${REMOTE_JAVA_HOME}}"
JAVA_BIN="\${JAVA_HOME}/bin/java"
if [[ ! -x "\${JAVA_BIN}" ]]; then
  echo "找不到 Java: \${JAVA_BIN}（可设置 JAVA_HOME 或 REMOTE_JAVA_HOME）" >&2
  exit 1
fi
cd "\${APP_HOME}"
mkdir -p logs
if [[ -f "\${APP_NAME}.pid" ]]; then
  OLD_PID="\$(cat "\${APP_NAME}.pid" || true)"
  if [[ -n "\${OLD_PID}" ]] && kill -0 "\${OLD_PID}" 2>/dev/null; then
    echo "已在运行 pid=\${OLD_PID}"
    exit 0
  fi
fi
nohup "\${JAVA_BIN}" -jar "\${JAR_NAME}" --server.port="\${APP_PORT}" > "logs/\${APP_NAME}.out" 2>&1 &
echo \$! > "\${APP_NAME}.pid"
echo "已启动 pid=\$(cat "\${APP_NAME}.pid") JAVA=\${JAVA_BIN} 端口=\${APP_PORT}"
EOF

cat > "${STOP_SCRIPT}" <<EOF
#!/usr/bin/env bash
set -euo pipefail
APP_HOME="${REMOTE_DIR}"
APP_NAME="${APP_NAME}"
cd "\${APP_HOME}"
if [[ -f "\${APP_NAME}.pid" ]]; then
  PID="\$(cat "\${APP_NAME}.pid" || true)"
  if [[ -n "\${PID}" ]] && kill -0 "\${PID}" 2>/dev/null; then
    kill "\${PID}" || true
    for _ in 1 2 3 4 5 6 7 8 9 10; do
      kill -0 "\${PID}" 2>/dev/null || break
      sleep 1
    done
    if kill -0 "\${PID}" 2>/dev/null; then
      kill -9 "\${PID}" || true
    fi
    echo "已停止 pid=\${PID}"
  fi
  rm -f "\${APP_NAME}.pid"
fi
pkill -f "${JAR_NAME}" 2>/dev/null || true
EOF

echo "==> 上传到 ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_DIR}"
ssh_run "mkdir -p '${REMOTE_DIR}/logs' && '${REMOTE_JAVA_HOME}/bin/java' -version"
scp_to "${STOP_SCRIPT}" "${REMOTE_DIR}/stop.sh"
ssh_run "chmod +x '${REMOTE_DIR}/stop.sh' && '${REMOTE_DIR}/stop.sh' || true"
scp_to "${JAR_FILE}" "${REMOTE_DIR}/${JAR_NAME}"
scp_to "${START_SCRIPT}" "${REMOTE_DIR}/start.sh"
ssh_run "chmod +x '${REMOTE_DIR}/start.sh' '${REMOTE_DIR}/stop.sh' && '${REMOTE_DIR}/start.sh'"

echo "==> 等待健康检查 /api/health"
ok=0
for i in $(seq 1 30); do
  if ssh_run "curl -fsS --max-time 3 http://127.0.0.1:${APP_PORT}/api/health >/dev/null"; then
    ok=1
    break
  fi
  sleep 2
done

if [[ "${ok}" -ne 1 ]]; then
  echo "启动后健康检查失败，最近日志：" >&2
  ssh_run "tail -n 80 '${REMOTE_DIR}/logs/${APP_NAME}.out' || true"
  exit 1
fi

echo "==> 部署成功"
echo "    目录: ${REMOTE_DIR}"
echo "    进程: ssh ${REMOTE_USER}@${REMOTE_HOST} 'cat ${REMOTE_DIR}/${APP_NAME}.pid'"
echo "    健康: http://${REMOTE_HOST}:${APP_PORT}/api/health"
echo "    启停: ${REMOTE_DIR}/start.sh | ${REMOTE_DIR}/stop.sh"
