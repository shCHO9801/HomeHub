# Host Monitoring Agent 배포

## 전제 조건

- HomeHub 저장소가 `/srv/homehub`에 있다.
- 미니 PC에 Java 21과 Docker Compose가 설치되어 있다.
- Agent의 `9101` 포트는 Caddy나 공유기 포트 포워딩에 연결하지 않는다.
- Agent 토큰은 채팅에 붙이거나 Git에 커밋하지 않는다.

## 1. Agent JAR 빌드

```bash
cd /srv/homehub
git pull --ff-only

cd agent
./gradlew test --rerun-tasks
./gradlew bootJar
```

생성 파일:

```text
/srv/homehub/agent/build/libs/homehub-agent-0.0.1-SNAPSHOT.jar
```

## 2. 전용 사용자와 설치 경로 생성

```bash
id homehub-agent >/dev/null 2>&1 \
  || sudo useradd \
    --system \
    --home /nonexistent \
    --shell /usr/sbin/nologin \
    homehub-agent

sudo install \
  -d \
  -o homehub-agent \
  -g homehub-agent \
  -m 0755 \
  /opt/homehub-agent

sudo install \
  -o homehub-agent \
  -g homehub-agent \
  -m 0644 \
  build/libs/homehub-agent-0.0.1-SNAPSHOT.jar \
  /opt/homehub-agent/homehub-agent.jar
```

## 3. Agent 환경변수 생성

미니 PC의 첫 번째 내부망 IPv4 주소를 확인한다.

```bash
MINI_PC_IP=$(hostname -I | awk '{print $1}')
printf '%s\n' "$MINI_PC_IP"
```

충분히 긴 공유 토큰과 임시 환경 파일을 생성한다.

```bash
umask 077
AGENT_TOKEN=$(openssl rand -base64 48)

printf '%s\n' \
  "AGENT_BIND_ADDRESS=${MINI_PC_IP}" \
  "AGENT_PORT=9101" \
  "AGENT_TOKEN=${AGENT_TOKEN}" \
  > /tmp/homehub-agent.env
```

Agent 전용 경로에 권한 `600`으로 설치한다.

```bash
sudo install \
  -d \
  -o homehub-agent \
  -g homehub-agent \
  -m 0700 \
  /etc/homehub-agent

sudo install \
  -o homehub-agent \
  -g homehub-agent \
  -m 0600 \
  /tmp/homehub-agent.env \
  /etc/homehub-agent/agent.env

shred -u /tmp/homehub-agent.env
```

## 4. systemd 서비스 설치

```bash
cd /srv/homehub

sudo install \
  -o root \
  -g root \
  -m 0644 \
  deploy/systemd/homehub-agent.service \
  /etc/systemd/system/homehub-agent.service

sudo systemctl daemon-reload
sudo systemctl enable --now homehub-agent
sudo systemctl is-active homehub-agent
```

정상이면 마지막 명령이 `active`를 출력한다.

## 5. Agent 직접 확인

현재 셸에만 토큰을 보관한 상태에서 Agent API를 확인한다.

```bash
curl --fail --silent --show-error \
  -H "Authorization: Bearer ${AGENT_TOKEN}" \
  "http://${MINI_PC_IP}:9101/api/agent/status"
```

응답의 `status`가 `ONLINE`인지 확인한다. 확인 후 현재 셸의 토큰 변수를 지운다.

```bash
unset AGENT_TOKEN
```

## 6. HomeHub 백엔드 연결

Agent 환경 파일에서 토큰을 읽어 현재 셸 변수로만 보관한다.

```bash
AGENT_TOKEN=$(sudo sed -n 's/^AGENT_TOKEN=//p' \
  /etc/homehub-agent/agent.env)
MINI_PC_IP=$(hostname -I | awk '{print $1}')
```

`/srv/homehub/.env`를 열어 다음 네 항목을 추가하거나 수정한다.

```bash
cd /srv/homehub
nano .env
```

```dotenv
HOST_AGENT_BASE_URL=http://미니PC의_내부망_IP:9101
HOST_AGENT_TOKEN=Agent와_동일한_토큰
HOST_AGENT_CONNECT_TIMEOUT=2s
HOST_AGENT_READ_TIMEOUT=3s
```

입력할 내부망 주소와 토큰은 다음 명령으로 미니 PC 화면에서만 확인한다.

```bash
printf 'HOST_AGENT_BASE_URL=http://%s:9101\n' "$MINI_PC_IP"
printf 'HOST_AGENT_TOKEN=%s\n' "$AGENT_TOKEN"
```

편집을 마치면 셸 변수를 제거하고 백엔드만 다시 빌드한다.

```bash
unset AGENT_TOKEN

docker compose config --quiet
docker compose up -d --build backend
docker compose ps
```

## 7. HomeHub 화면 확인

브라우저에서 다음 주소로 로그인한다.

```text
https://homehub.shhome.synology.me
```

모니터링 화면의 루트 디스크 전체 용량이 백엔드 컨테이너가 아니라 미니 PC의 약 466GB 디스크를 나타내는지 확인한다.

## 8. 운영 명령

상태 확인:

```bash
sudo systemctl status homehub-agent --no-pager
```

최근 로그 확인:

```bash
sudo journalctl -u homehub-agent -n 100 --no-pager
```

Agent JAR 업데이트 후 재시작:

```bash
cd /srv/homehub/agent
./gradlew bootJar

sudo install \
  -o homehub-agent \
  -g homehub-agent \
  -m 0644 \
  build/libs/homehub-agent-0.0.1-SNAPSHOT.jar \
  /opt/homehub-agent/homehub-agent.jar

sudo systemctl restart homehub-agent
sudo systemctl is-active homehub-agent
```
