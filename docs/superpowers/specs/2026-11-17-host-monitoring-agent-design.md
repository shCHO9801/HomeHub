# Host Monitoring Agent 설계

## 목적

Docker 컨테이너에서 실행되는 HomeHub 백엔드가 아니라 미니 PC 호스트에서 직접 CPU, 메모리, 디스크, 업타임을 수집한다. 동일한 Agent를 이후 데스크톱에도 설치할 수 있도록 장비별 수집과 중앙 통합 책임을 분리한다.

## 범위

이번 작업은 다음 기능만 포함한다.

- OSHI를 이용한 호스트 CPU 사용률과 논리 코어 수 수집
- 메모리 전체, 사용, 가용 용량 수집
- 루트 파일시스템 전체, 사용, 가용 용량 수집
- 시스템 업타임과 수집 시각 반환
- Bearer 토큰을 사용한 Agent API 인증
- HomeHub 백엔드에서 Agent 상태 조회
- Agent 장애 또는 시간 초과 시 `UNKNOWN` 상태 반환
- 미니 PC에서 Agent를 systemd 서비스로 실행하기 위한 설정

다음 기능은 포함하지 않는다.

- Docker 컨테이너 제어
- 서버 재부팅 또는 명령 실행
- 상태 이력 저장과 그래프
- 알림
- NAS 상태 수집
- 데스크톱 배포

## 저장소 구조

HomeHub 저장소 안에 독립 애플리케이션을 추가한다.

```text
HomeHub/
├── frontend/
├── backend/
├── agent/
└── compose.yaml
```

`backend`와 `agent`는 각각 독립된 Gradle 프로젝트다. 루트 Gradle 멀티 프로젝트나 공통 모듈은 만들지 않는다. 두 애플리케이션은 Java 클래스를 직접 공유하지 않고 HTTP API 계약으로 통신한다.

## 책임 분리

### Agent

- 설치된 장비의 OSHI 정보를 수집한다.
- 현재 상태를 읽기 전용 HTTP API로 제공한다.
- DB나 상태 이력을 보유하지 않는다.
- 장비를 변경하는 기능을 제공하지 않는다.

### HomeHub 백엔드

- Agent API를 호출한다.
- 응답을 기존 HomeHub 모니터링 응답으로 전달한다.
- 연결 실패와 시간 초과를 `UNKNOWN`으로 변환한다.
- 사용자 로그인과 외부 접근 제어를 담당한다.

### 프론트엔드

- 기존 `GET /api/monitoring/server`를 계속 사용한다.
- Agent 주소나 인증 토큰을 알지 못한다.

## API 계약

Agent는 다음 API를 제공한다.

```http
GET /api/agent/status
Authorization: Bearer <AGENT_TOKEN>
```

응답 필드는 기존 `ServerStatusResponse` 계약을 유지한다.

- `status`: `ONLINE` 또는 `UNKNOWN`
- `cpuUsagePercent`
- `logicalProcessorCount`
- `memory`
- `disk`
- `uptimeSeconds`
- `collectedAt`

`memory`와 `disk`는 전체 바이트, 사용 바이트, 가용 바이트, 사용률을 포함한다.

## 데이터 흐름

1. 프론트엔드가 HomeHub 백엔드의 `GET /api/monitoring/server`를 호출한다.
2. 백엔드가 설정된 Agent 주소로 `GET /api/agent/status`를 호출한다.
3. 백엔드는 `Authorization` 헤더에 Agent 토큰을 전달한다.
4. Agent가 OSHI로 현재 호스트 상태를 수집한다.
5. Agent 응답을 백엔드가 기존 응답 계약으로 반환한다.
6. Agent에 연결할 수 없으면 백엔드가 모든 자원 필드를 `null`로 둔 `UNKNOWN` 응답을 반환한다.

## 설정과 보안

Agent 기본 포트는 `9101`이다. 개발 환경의 기본 바인딩 주소는 `127.0.0.1`로 두고, 미니 PC에서는 환경변수로 내부망 IP를 지정한다.

Agent 포트는 Caddy에 연결하지 않고 공유기 포트 포워딩도 하지 않는다. Agent와 백엔드는 같은 Bearer 토큰을 각 실행 환경의 비밀 환경변수로 보관한다. 토큰 값은 코드, Git, 응답 및 로그에 기록하지 않는다.

백엔드는 Agent 연결 제한 시간과 응답 제한 시간을 설정한다. 초기 기본값은 각각 2초와 3초로 한다.

Agent JVM은 미니 PC에서 다음 상한으로 실행한다.

```text
-Xms64m
-Xmx256m
```

## 오류 처리

- 토큰이 없거나 다르면 Agent는 `401 Unauthorized`를 반환한다.
- OSHI 수집 중 오류가 발생하면 Agent는 `UNKNOWN` 응답을 반환하고 서버 내부 오류 메시지는 노출하지 않는다.
- Agent 연결 실패, 시간 초과, 잘못된 응답은 백엔드에서 `UNKNOWN`으로 변환한다.
- Agent 장애 때문에 HomeHub 백엔드 요청이 500으로 실패하지 않도록 한다.
- 토큰, 전체 인증 헤더 및 민감한 설정은 오류 로그에 기록하지 않는다.

## 기존 코드 변경

- 백엔드 `MonitoringService`의 OSHI 수집 로직을 Agent로 이동한다.
- 백엔드 `MonitoringConfig`의 `SystemInfoProvider` 구성을 Agent로 이동한다.
- 백엔드에는 Agent HTTP 클라이언트와 연결 설정을 추가한다.
- 백엔드 `MonitoringController`와 프론트엔드 API 경로는 유지한다.
- 백엔드에서 더 이상 사용하지 않는 OSHI 의존성을 제거한다.

## 테스트

### Agent

- 정상 OSHI 값으로 `ONLINE` 응답을 생성한다.
- 잘못된 자원 값 또는 수집 오류를 `UNKNOWN`으로 처리한다.
- 올바른 토큰 요청은 허용한다.
- 토큰 누락과 잘못된 토큰 요청은 `401`을 반환한다.

### 백엔드

- Agent 정상 응답을 기존 API 응답으로 전달한다.
- Agent 연결 실패와 시간 초과를 `UNKNOWN`으로 처리한다.
- 기존 인증이 적용된 `/api/monitoring/server` 계약을 유지한다.

### 배포

- Agent JAR 빌드와 테스트를 통과한다.
- 미니 PC에서 systemd 서비스가 비루트 사용자로 실행된다.
- HomeHub 백엔드 컨테이너가 내부망 Agent API를 조회한다.
- Agent 포트가 외부 인터넷을 통해 접근되지 않는다.
