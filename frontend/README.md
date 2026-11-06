# My Home Hub Frontend

React, TypeScript, Vite, Tailwind CSS로 구성된 My Home Hub 프런트엔드입니다.

## 요구 환경

- Node.js 24.15 이상
- pnpm 12

## 로컬 실행

```bash
pnpm install
pnpm dev
```

개발 서버는 기본적으로 `http://localhost:5173`에서 실행됩니다. 백엔드 주소는 `.env.local`의 `BACKEND_URL`로 설정하며 기본 예시는 `http://localhost:8080`입니다.

Vite 프록시는 개발 환경에서만 동작합니다. 배포 환경에서 `dist/`를 제공할 때는 같은 출처의 `/actuator/health` 요청을 Spring Boot로 전달해야 합니다.

## 검사

```bash
pnpm test
pnpm lint
pnpm build
```
