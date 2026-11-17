# Host Monitoring Agent Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 미니 PC 호스트에서 OSHI로 상태를 수집하는 읽기 전용 Agent를 만들고, Docker의 HomeHub 백엔드가 인증된 HTTP 요청으로 그 상태를 조회하게 한다.

**Architecture:** `agent/`는 별도 Spring Boot 애플리케이션으로 호스트에서 systemd로 실행한다. 기존 백엔드 API와 프론트엔드 계약은 유지하고, 백엔드의 OSHI 직접 수집을 `HostAgentClient` 호출로 교체한다.

**Tech Stack:** Java 21, Spring Boot 4.1.2, Spring Web MVC, Spring Security, OSHI 7.7.3, Gradle 9.8.2, JUnit 6, Mockito

**Spec:** `docs/superpowers/specs/2026-11-17-host-monitoring-agent-design.md`

## Global Constraints

- `agent`와 `backend`는 독립된 Gradle 프로젝트로 유지하고 공통 Gradle 모듈을 만들지 않는다.
- Agent API는 `GET /api/agent/status` 하나만 제공한다.
- Agent 기본 포트는 `9101`, 개발 기본 바인딩은 `127.0.0.1`이다.
- 인증은 `Authorization: Bearer <AGENT_TOKEN>`을 사용하고 토큰을 코드나 로그에 남기지 않는다.
- Agent JVM 메모리는 systemd에서 `-Xms64m -Xmx256m`로 제한한다.
- Docker 제어, 재부팅, 명령 실행, 이력 저장, 알림은 구현하지 않는다.
- 프론트엔드와 `GET /api/monitoring/server` 응답 계약은 변경하지 않는다.
- 사용자가 정한 작업 방식에 따라 각 기능의 최소 구현을 먼저 작성하고 바로 테스트를 추가한다.

---

## File Map

### Agent 애플리케이션

- `agent/build.gradle`: Agent 전용 의존성과 Java 버전
- `agent/settings.gradle`: 독립 프로젝트 이름
- `agent/gradlew`, `agent/gradle/wrapper/*`: 독립 Gradle Wrapper
- `agent/src/main/java/io/github/shhome/homehub/agent/HomehubAgentApplication.java`: 실행 진입점
- `agent/src/main/resources/application.yaml`: 포트, 바인딩 주소, 토큰 설정
- `agent/src/main/java/io/github/shhome/homehub/agent/config/AgentProperties.java`: Agent 토큰 설정
- `agent/src/main/java/io/github/shhome/homehub/agent/config/AgentSecurityConfig.java`: Stateless 보안 체인
- `agent/src/main/java/io/github/shhome/homehub/agent/security/AgentTokenAuthenticationFilter.java`: Bearer 토큰 검증
- `agent/src/main/java/io/github/shhome/homehub/agent/monitoring/config/MonitoringConfig.java`: OSHI Provider Bean
- `agent/src/main/java/io/github/shhome/homehub/agent/monitoring/service/HostMonitoringService.java`: 호스트 상태 수집
- `agent/src/main/java/io/github/shhome/homehub/agent/monitoring/controller/AgentStatusController.java`: Agent 상태 API
- `agent/src/main/java/io/github/shhome/homehub/agent/monitoring/dto/*`: Agent 응답 계약

### HomeHub 백엔드

- `backend/src/main/java/io/github/shhome/homehub/monitoring/config/HostAgentProperties.java`: Agent 주소, 토큰, 시간 제한
- `backend/src/main/java/io/github/shhome/homehub/monitoring/config/MonitoringConfig.java`: 인증 헤더와 시간 제한이 적용된 RestClient
- `backend/src/main/java/io/github/shhome/homehub/monitoring/client/HostAgentClient.java`: Agent API 호출
- `backend/src/main/java/io/github/shhome/homehub/monitoring/service/MonitoringService.java`: Agent 실패를 `UNKNOWN`으로 변환
- `backend/src/main/resources/application.yaml`: Agent 환경변수 매핑
- `backend/build.gradle`: 백엔드 OSHI 의존성 제거

### 배포

- `.env.example`: 백엔드의 Agent 연결 환경변수 예시
- `compose.yaml`: 백엔드 컨테이너의 Agent 설정 전달
- `deploy/systemd/homehub-agent.service`: 비루트 Agent systemd 서비스
- `docs/deployment/host-agent.md`: 빌드, 설치, 환경변수, 검증 절차

---

### Task 1: 독립 Agent 프로젝트와 호스트 상태 수집

**Files:**
- Create: `agent/build.gradle`
- Create: `agent/settings.gradle`
- Create: `agent/gradlew`
- Create: `agent/gradlew.bat`
- Create: `agent/gradle/wrapper/gradle-wrapper.jar`
- Create: `agent/gradle/wrapper/gradle-wrapper.properties`
- Create: `agent/src/main/java/io/github/shhome/homehub/agent/HomehubAgentApplication.java`
- Create: `agent/src/main/resources/application.yaml`
- Create: `agent/src/main/java/io/github/shhome/homehub/agent/monitoring/config/MonitoringConfig.java`
- Create: `agent/src/main/java/io/github/shhome/homehub/agent/monitoring/dto/AgentStatus.java`
- Create: `agent/src/main/java/io/github/shhome/homehub/agent/monitoring/dto/ResourceUsageResponse.java`
- Create: `agent/src/main/java/io/github/shhome/homehub/agent/monitoring/dto/AgentStatusResponse.java`
- Create: `agent/src/main/java/io/github/shhome/homehub/agent/monitoring/service/HostMonitoringService.java`
- Test: `agent/src/test/java/io/github/shhome/homehub/agent/monitoring/service/HostMonitoringServiceTest.java`

**Interfaces:**
- Produces: `HostMonitoringService#getStatus(): AgentStatusResponse`
- Produces: `AgentStatusResponse(AgentStatus status, Double cpuUsagePercent, Integer logicalProcessorCount, ResourceUsageResponse memory, ResourceUsageResponse disk, Long uptimeSeconds, OffsetDateTime collectedAt)`

- [ ] **Step 1: Create the independent Gradle project**

Copy the wrapper files from `backend` and create `agent/build.gradle` with only the required runtime dependencies:

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.1.2'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'io.github.shhome'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'com.github.oshi:oshi-core:7.7.3'

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testCompileOnly 'org.projectlombok:lombok'
    testAnnotationProcessor 'org.projectlombok:lombok'

    testImplementation 'org.springframework.boot:spring-boot-starter-security-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

Create `agent/settings.gradle`:

```groovy
rootProject.name = 'homehub-agent'
```

- [ ] **Step 2: Add the application entry point and default configuration**

Create `HomehubAgentApplication` with `@SpringBootApplication`. Configure `application.yaml` exactly as follows:

```yaml
spring:
  application:
    name: homehub-agent

server:
  address: ${AGENT_BIND_ADDRESS:127.0.0.1}
  port: ${AGENT_PORT:9101}

homehub:
  agent:
    token: ${AGENT_TOKEN}
```

- [ ] **Step 3: Move the OSHI collection behavior into Agent classes**

Create Agent-local DTOs with the same JSON field names as the backend DTOs. Move the existing calculation behavior from backend `MonitoringService` into `HostMonitoringService#getStatus()`. Keep these rules unchanged:

```java
double cpuPercent = Math.clamp(ratio * 100, 0, 100);
long available = Math.clamp(availableBytes, 0, totalBytes);
long used = totalBytes - available;
```

On `RuntimeException`, return `AgentStatus.UNKNOWN` with resource fields set to `null` and `collectedAt` set to the current time.

- [ ] **Step 4: Add collection tests after the implementation**

Write tests that mock `SystemInfoProvider` and verify:

```java
assertEquals(UNKNOWN, response.status());
assertNull(response.cpuUsagePercent());
assertNull(response.memory());
assertNotNull(response.collectedAt());
```

Also reproduce the existing normal-value mocks to verify CPU rounding, logical processor count, memory, disk, and uptime.

- [ ] **Step 5: Run Agent tests and compile**

Run:

```bash
cd agent
./gradlew test
./gradlew bootJar
```

Expected: both commands exit with code `0` and produce `agent/build/libs/homehub-agent-0.0.1-SNAPSHOT.jar`.

- [ ] **Step 6: Commit Task 1**

```bash
git add agent
git commit -m "feat: 호스트 상태 수집 Agent 추가"
```

---

### Task 2: Agent Bearer 토큰 인증과 상태 API

**Files:**
- Create: `agent/src/main/java/io/github/shhome/homehub/agent/config/AgentProperties.java`
- Create: `agent/src/main/java/io/github/shhome/homehub/agent/config/AgentSecurityConfig.java`
- Create: `agent/src/main/java/io/github/shhome/homehub/agent/security/AgentTokenAuthenticationFilter.java`
- Create: `agent/src/main/java/io/github/shhome/homehub/agent/monitoring/controller/AgentStatusController.java`
- Test: `agent/src/test/java/io/github/shhome/homehub/agent/monitoring/AgentStatusIntegrationTest.java`

**Interfaces:**
- Consumes: `HostMonitoringService#getStatus()` from Task 1
- Produces: authenticated `GET /api/agent/status`
- Produces: `AgentProperties(String token)` bound from `homehub.agent`

- [ ] **Step 1: Add validated token configuration**

Create a `@ConfigurationProperties(prefix = "homehub.agent")` record with `@NotBlank String token`, enable it in `AgentSecurityConfig`, and fail Agent startup when `AGENT_TOKEN` is missing or blank.

- [ ] **Step 2: Implement constant-time Bearer token authentication**

Create `AgentTokenAuthenticationFilter extends OncePerRequestFilter`. For `/api/agent/**`, compare the expected and received header bytes using:

```java
MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8),
        received.getBytes(StandardCharsets.UTF_8)
)
```

On mismatch, set status `401`, content type `application/json`, and return this fixed body without logging the token:

```json
{"status":401,"message":"Agent 인증에 실패했습니다."}
```

On success, place an authenticated `UsernamePasswordAuthenticationToken` for principal `homehub-backend` into `SecurityContextHolder` and continue the filter chain.

- [ ] **Step 3: Configure stateless Spring Security**

Create a `SecurityFilterChain` that disables CSRF for the Agent, uses `SessionCreationPolicy.STATELESS`, requires authentication for `/api/agent/**`, and adds `AgentTokenAuthenticationFilter` before `AnonymousAuthenticationFilter`.

- [ ] **Step 4: Add the status controller**

Implement:

```java
@GetMapping("/status")
public ResponseEntity<AgentStatusResponse> getStatus() {
    return ResponseEntity.ok(hostMonitoringService.getStatus());
}
```

under `@RequestMapping("/api/agent")`.

- [ ] **Step 5: Add API authentication tests**

Use `@SpringBootTest(properties = "homehub.agent.token=test-agent-token")` and `@AutoConfigureMockMvc`. Mock `HostMonitoringService` and verify:

- correct `Authorization: Bearer test-agent-token` returns `200` and the expected JSON;
- no header returns `401`;
- `Bearer wrong-token` returns `401`;
- the response never contains the configured token.

- [ ] **Step 6: Run and commit Task 2**

```bash
cd agent
./gradlew test --rerun-tasks
cd ..
git add agent
git commit -m "feat: Agent 상태 API 인증 추가"
```

---

### Task 3: HomeHub 백엔드의 Agent Client 전환

**Files:**
- Create: `backend/src/main/java/io/github/shhome/homehub/monitoring/config/HostAgentProperties.java`
- Modify: `backend/src/main/java/io/github/shhome/homehub/monitoring/config/MonitoringConfig.java`
- Create: `backend/src/main/java/io/github/shhome/homehub/monitoring/client/HostAgentClient.java`
- Modify: `backend/src/main/java/io/github/shhome/homehub/monitoring/service/MonitoringService.java`
- Modify: `backend/src/main/resources/application.yaml`
- Modify: `backend/build.gradle`
- Modify: `backend/src/test/java/io/github/shhome/homehub/monitoring/service/MonitoringServiceTest.java`
- Modify: `backend/src/test/java/io/github/shhome/homehub/monitoring/MonitoringIntegrationTest.java`
- Test: `backend/src/test/java/io/github/shhome/homehub/monitoring/client/HostAgentClientTest.java`

**Interfaces:**
- Consumes: Agent `GET /api/agent/status`
- Produces: `HostAgentClient#getStatus(): ServerStatusResponse`
- Produces: unchanged backend `MonitoringService#getServerStatus(): ServerStatusResponse`
- Produces: `HostAgentProperties(URI baseUrl, String token, Duration connectTimeout, Duration readTimeout)`

- [ ] **Step 1: Add backend Agent configuration**

Bind these settings in `application.yaml`:

```yaml
homehub:
  host-agent:
    base-url: ${HOST_AGENT_BASE_URL:http://localhost:9101}
    token: ${HOST_AGENT_TOKEN:}
    connect-timeout: ${HOST_AGENT_CONNECT_TIMEOUT:2s}
    read-timeout: ${HOST_AGENT_READ_TIMEOUT:3s}
```

Use `@NotNull` for URI and durations. Keep the backend token as a plain `String` so isolated backend tests can start without an Agent secret; the Agent itself still refuses to start without `AGENT_TOKEN`, and the production deployment guide requires both sides to use the same non-empty token.

- [ ] **Step 2: Replace OSHI configuration with an authenticated RestClient**

Change `MonitoringConfig` to enable `HostAgentProperties` and expose `@Qualifier("hostAgentRestClient") RestClient`. Use `JdkClientHttpRequestFactory` with both configured timeouts and set this default header:

```java
HttpHeaders.AUTHORIZATION,
"Bearer " + properties.token()
```

- [ ] **Step 3: Implement `HostAgentClient`**

Implement `getStatus()` by calling `/api/agent/status`, deserializing `ServerStatusResponse`, and rejecting a null response body with `Objects.requireNonNull`.

- [ ] **Step 4: Replace backend direct collection**

Reduce `MonitoringService` to one dependency, `HostAgentClient`. Return the client response on success. Catch `RuntimeException`, log only the exception class, and return:

```java
new ServerStatusResponse(
        UNKNOWN,
        null,
        null,
        null,
        null,
        null,
        OffsetDateTime.now()
)
```

Remove `com.github.oshi:oshi-core:7.7.3` from `backend/build.gradle`.

- [ ] **Step 5: Update backend tests**

Change `MonitoringServiceTest` to mock `HostAgentClient` and verify normal pass-through plus failure-to-`UNKNOWN`. In `MonitoringIntegrationTest`, use `@MockitoBean MonitoringService` and return a fixed `ONLINE` response so the test does not require a running Agent.

Create `HostAgentClientTest` with a JDK `HttpServer` bound to port `0`. Capture the request header and verify the client sends the expected Bearer token and deserializes all response fields.

- [ ] **Step 6: Run and commit Task 3**

```bash
cd backend
./gradlew test --rerun-tasks
cd ..
git add backend
git commit -m "feat: 백엔드 호스트 Agent 연동"
```

---

### Task 4: 로컬 환경과 미니 PC 배포 설정

**Files:**
- Create: `agent/.env.example`
- Modify: `.env.example`
- Modify: `compose.yaml`
- Create: `deploy/systemd/homehub-agent.service`
- Create: `docs/deployment/host-agent.md`

**Interfaces:**
- Consumes: Agent JAR from Task 1
- Consumes: backend environment names from Task 3
- Produces: systemd service `homehub-agent.service`

- [ ] **Step 1: Add environment examples**

Create `agent/.env.example`:

```dotenv
AGENT_BIND_ADDRESS=127.0.0.1
AGENT_PORT=9101
AGENT_TOKEN=replace-with-a-long-random-token
```

Add to root `.env.example`:

```dotenv
HOST_AGENT_BASE_URL=http://host.docker.internal:9101
HOST_AGENT_TOKEN=replace-with-the-same-agent-token
HOST_AGENT_CONNECT_TIMEOUT=2s
HOST_AGENT_READ_TIMEOUT=3s
```

- [ ] **Step 2: Pass Agent configuration to the backend container**

Add the four `HOST_AGENT_*` values to the backend environment in `compose.yaml`. Add:

```yaml
extra_hosts:
  - "host.docker.internal:host-gateway"
```

Do not publish port `9101` from Compose because the Agent runs on the host, not in Docker.

- [ ] **Step 3: Add the hardened systemd unit**

Create `deploy/systemd/homehub-agent.service`:

```ini
[Unit]
Description=HomeHub Host Monitoring Agent
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=homehub-agent
Group=homehub-agent
EnvironmentFile=/etc/homehub-agent/agent.env
ExecStart=/usr/bin/java -Xms64m -Xmx256m -jar /opt/homehub-agent/homehub-agent.jar
Restart=on-failure
RestartSec=5
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true

[Install]
WantedBy=multi-user.target
```

- [ ] **Step 4: Document exact mini PC installation**

Document commands that:

1. build `agent/build/libs/homehub-agent-0.0.1-SNAPSHOT.jar`;
2. create system user `homehub-agent`;
3. install the JAR under `/opt/homehub-agent`;
4. create `/etc/homehub-agent/agent.env` with mode `600`;
5. determine the mini PC LAN address with `hostname -I | awk '{print $1}'` and use it as `AGENT_BIND_ADDRESS`;
6. generate the shared token with `openssl rand -base64 48`;
7. copy the same token into `/srv/homehub/.env` as `HOST_AGENT_TOKEN`;
8. assign `MINI_PC_IP=$(hostname -I | awk '{print $1}')` and set `HOST_AGENT_BASE_URL=http://${MINI_PC_IP}:9101` in `/srv/homehub/.env`;
9. enable and start `homehub-agent.service`;
10. rebuild only the backend Compose service.

The guide must explicitly state that the token must not be pasted into chat or committed.

- [ ] **Step 5: Validate deployment files and commit**

```bash
docker compose config --quiet
systemd-analyze verify deploy/systemd/homehub-agent.service
git diff --check
git add agent/.env.example .env.example compose.yaml deploy/systemd/homehub-agent.service docs/deployment/host-agent.md
git commit -m "feat: 호스트 Agent 배포 설정 추가"
```

---

### Task 5: 전체 회귀 검증과 실제 미니 PC 연결

**Files:**
- Modify only files required to fix failures found by the commands below.

**Interfaces:**
- Consumes: completed Agent API, backend client, Compose configuration, and systemd unit
- Produces: externally unchanged HomeHub monitoring endpoint backed by host data

- [ ] **Step 1: Run the complete local verification suite**

```bash
cd agent
./gradlew test --rerun-tasks
./gradlew bootJar

cd ../backend
./gradlew test --rerun-tasks

cd ../frontend
pnpm test
pnpm lint
pnpm build

cd ..
docker compose config --quiet
git diff --check
```

Expected: every command exits with code `0`.

- [ ] **Step 2: Verify the Agent locally without exposing it**

Start Agent with a temporary token and verify:

```bash
AGENT_TOKEN=local-agent-test ./agent/gradlew -p agent bootRun
curl -i http://127.0.0.1:9101/api/agent/status
curl -i -H 'Authorization: Bearer local-agent-test' http://127.0.0.1:9101/api/agent/status
```

Expected: the first request returns `401`; the second returns `200` and `ONLINE`.

- [ ] **Step 3: Deploy to the mini PC using the tracked guide**

Follow `docs/deployment/host-agent.md`, then verify on the mini PC:

```bash
systemctl is-active homehub-agent
set -a
. /srv/homehub/.env
set +a
curl --fail --silent --show-error \
  -H "Authorization: Bearer ${HOST_AGENT_TOKEN}" \
  "${HOST_AGENT_BASE_URL}/api/agent/status"
docker compose ps
```

Expected: systemd reports `active`, Agent returns `ONLINE`, and all HomeHub containers remain running.

- [ ] **Step 4: Verify through HomeHub**

Log in at `https://homehub.shhome.synology.me` and confirm the monitoring values are the mini PC host values, including the host root disk capacity rather than the backend container filesystem capacity.

- [ ] **Step 5: Final commit if verification required fixes**

If verification changed tracked files, inspect them and stage modifications to already tracked files:

```bash
git status --short
git add -u
git commit -m "fix: 호스트 Agent 통합 검증 오류 수정"
```

If no tracked files changed, do not create an empty commit.
