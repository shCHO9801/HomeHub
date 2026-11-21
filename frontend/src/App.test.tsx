import { fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'

const currentUser = {
  id: 59,
  username: 'sunghyun-admin',
  role: 'ADMIN',
}

const csrfToken = {
  headerName: 'X-CSRF-TOKEN',
  parameterName: '_csrf',
  token: 'csrf-token',
}

const serverStatus = {
  status: 'ONLINE',
  cpuUsagePercent: 12.5,
  logicalProcessorCount: 8,
  memory: {
    totalBytes: 16_000,
    usedBytes: 8_000,
    availableBytes: 8_000,
    usagePercent: 50,
  },
  disk: {
    totalBytes: 100_000,
    usedBytes: 40_000,
    availableBytes: 60_000,
    usagePercent: 40,
  },
  uptimeSeconds: 90_061,
  collectedAt: '2026-11-12T17:00:00+09:00',
}

const dockerContainers = [
  {
    id: 'abc123',
    name: 'homehub-backend',
    image: 'homehub-backend:latest',
    state: 'RUNNING',
    statusDetail: 'Up 2 hours (healthy)',
    ports: '127.0.0.1:8080->8080/tcp',
    composeProject: 'homehub',
  },
]

const ports = [
  {
    protocol: 'TCP',
    port: 22,
    bindAddress: '0.0.0.0',
    exposure: 'ALL_INTERFACES',
    processName: 'sshd',
  },
  {
    protocol: 'TCP',
    port: 8100,
    bindAddress: '127.0.0.1',
    exposure: 'LOOPBACK',
    processName: 'docker-proxy',
  },
]

function jsonResponse(body: unknown, status = 200): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  } as Response
}

function emptyResponse(status = 204): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
  } as Response
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('App 인증 흐름', () => {
  it('로그인 상태를 확인하는 동안 로딩 상태를 표시한다', () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(
      () => new Promise<Response>(() => {}),
    )

    render(<App />)

    expect(
      screen.getByText('로그인 상태 확인 중…'),
    ).toBeInTheDocument()
  })

  it('미인증 사용자에게 로그인 화면을 표시한다', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(jsonResponse({}, 401))

    render(<App />)

    expect(
      await screen.findByRole('heading', {
        name: 'My Home Hub',
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByLabelText('사용자명'),
    ).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/auth/me',
      { credentials: 'include' },
    )
  })

  it('로그인에 성공하면 현재 사용자를 표시한다', async () => {
    let currentUserRequestCount = 0

    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockImplementation(async (input) => {
        const url = String(input)

        if (url === '/api/auth/me') {
          currentUserRequestCount++

          return currentUserRequestCount === 1
            ? jsonResponse({}, 401)
            : jsonResponse(currentUser)
        }

        if (url === '/api/auth/csrf') {
          return jsonResponse(csrfToken)
        }

        if (url === '/api/auth/login') {
          return emptyResponse()
        }

        if (url === '/api/monitoring/docker/containers') {
          return jsonResponse(dockerContainers)
        }

        if (url === '/api/monitoring/ports') {
          return jsonResponse(ports)
        }

        return jsonResponse(serverStatus)
      })

    render(<App />)

    fireEvent.change(
      await screen.findByLabelText('사용자명'),
      { target: { value: 'sunghyun-admin' } },
    )
    fireEvent.change(screen.getByLabelText('비밀번호'), {
      target: { value: 'strong-password' },
    })
    fireEvent.click(
      screen.getByRole('button', { name: '로그인' }),
    )

    expect(
      await screen.findByText('sunghyun-admin'),
    ).toBeInTheDocument()
    expect(screen.getByText('ADMIN')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/auth/login',
      expect.objectContaining({
        method: 'POST',
        credentials: 'include',
      }),
    )
  })

  it('로그인 실패 메시지를 표시한다', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(
      async (input) => {
        const url = String(input)

        if (url === '/api/auth/me') {
          return jsonResponse({}, 401)
        }

        if (url === '/api/auth/csrf') {
          return jsonResponse(csrfToken)
        }

        return jsonResponse(
          {
            message: '아이디 또는 비밀번호가 올바르지 않습니다.',
          },
          401,
        )
      },
    )

    render(<App />)

    fireEvent.change(
      await screen.findByLabelText('사용자명'),
      { target: { value: 'sunghyun-admin' } },
    )
    fireEvent.change(screen.getByLabelText('비밀번호'), {
      target: { value: 'wrong-password' },
    })
    fireEvent.click(
      screen.getByRole('button', { name: '로그인' }),
    )

    expect(
      await screen.findByRole('alert'),
    ).toHaveTextContent(
      '아이디 또는 비밀번호가 올바르지 않습니다.',
    )
  })

  it('로그아웃하면 로그인 화면으로 돌아간다', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockImplementation(async (input) => {
        const url = String(input)

        if (url === '/api/auth/me') {
          return jsonResponse(currentUser)
        }

        if (url === '/api/auth/csrf') {
          return jsonResponse(csrfToken)
        }

        if (url === '/api/auth/logout') {
          return emptyResponse()
        }

        if (url === '/api/monitoring/docker/containers') {
          return jsonResponse(dockerContainers)
        }

        if (url === '/api/monitoring/ports') {
          return jsonResponse(ports)
        }

        return jsonResponse(serverStatus)
      })

    render(<App />)

    fireEvent.click(
      await screen.findByRole('button', {
        name: '로그아웃',
      }),
    )

    expect(
      await screen.findByLabelText('사용자명'),
    ).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/auth/logout',
      expect.objectContaining({
        method: 'POST',
        credentials: 'include',
      }),
    )
  })
})

describe('App 서버 상태', () => {
  it('인증된 사용자에게 백엔드 온라인 상태를 표시한다', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(
      async (input) => {
        const url = String(input)

        if (url === '/api/auth/me') {
          return jsonResponse(currentUser)
        }

        if (url === '/api/monitoring/docker/containers') {
          return jsonResponse(dockerContainers)
        }

        if (url === '/api/monitoring/ports') {
          return jsonResponse(ports)
        }

        return jsonResponse(serverStatus)
      },
    )

    render(<App />)

    expect(
      await screen.findByText('서버 온라인'),
    ).toBeInTheDocument()
  })

  it('백엔드 상태 요청이 실패하면 연결 실패를 표시한다', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(
      async (input) => {
        if (String(input) === '/api/auth/me') {
          return jsonResponse(currentUser)
        }

        throw new TypeError('Network error')
      },
    )

    render(<App />)

    expect(
      await screen.findByText(
        '서버 모니터링 정보를 불러오지 못했습니다.',
      ),
    ).toBeInTheDocument()
  })
})

describe('App Docker 모니터링', () => {
  it('인증된 사용자에게 Docker 컨테이너 목록을 표시한다', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockImplementation(async (input) => {
        const url = String(input)

        if (url === '/api/auth/me') {
          return jsonResponse(currentUser)
        }

        if (url === '/api/monitoring/docker/containers') {
          return jsonResponse(dockerContainers)
        }

        if (url === '/api/monitoring/ports') {
          return jsonResponse(ports)
        }

        return jsonResponse(serverStatus)
      })

    render(<App />)

    expect(
      await screen.findByText('homehub-backend'),
    ).toBeInTheDocument()
    expect(
      screen.getByText('127.0.0.1:8080->8080/tcp'),
    ).toBeInTheDocument()
    expect(screen.getByText('실행 중')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/monitoring/docker/containers',
      { credentials: 'include' },
    )
  })

  it('Docker 컨테이너 요청 실패를 서버 상태와 구분해 표시한다', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(
      async (input) => {
        const url = String(input)

        if (url === '/api/auth/me') {
          return jsonResponse(currentUser)
        }

        if (url === '/api/monitoring/docker/containers') {
          return jsonResponse({}, 500)
        }

        if (url === '/api/monitoring/ports') {
          return jsonResponse(ports)
        }

        return jsonResponse(serverStatus)
      },
    )

    render(<App />)

    expect(
      await screen.findByText(
        'Docker 컨테이너 정보를 불러오지 못했습니다.',
      ),
    ).toBeInTheDocument()
    expect(screen.getByText('서버 온라인')).toBeInTheDocument()
  })
})

describe('App 포트 현황', () => {
  it('인증된 사용자에게 사용 중인 포트를 표시한다', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockImplementation(async (input) => {
        const url = String(input)

        if (url === '/api/auth/me') {
          return jsonResponse(currentUser)
        }

        if (url === '/api/monitoring/docker/containers') {
          return jsonResponse(dockerContainers)
        }

        if (url === '/api/monitoring/ports') {
          return jsonResponse(ports)
        }

        return jsonResponse(serverStatus)
      })

    render(<App />)

    expect(
      await screen.findByRole('heading', {
        name: '사용 중인 포트',
      }),
    ).toBeInTheDocument()
    expect(
      await screen.findByText('sshd'),
    ).toBeInTheDocument()
    expect(screen.getByText('docker-proxy')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/monitoring/ports',
      { credentials: 'include' },
    )
  })

  it('공개 범위 필터로 포트 목록을 좁힌다', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(
      async (input) => {
        const url = String(input)

        if (url === '/api/auth/me') {
          return jsonResponse(currentUser)
        }

        if (url === '/api/monitoring/docker/containers') {
          return jsonResponse(dockerContainers)
        }

        if (url === '/api/monitoring/ports') {
          return jsonResponse(ports)
        }

        return jsonResponse(serverStatus)
      },
    )

    render(<App />)

    await screen.findByText('sshd')

    fireEvent.click(
      screen.getByRole('button', { name: '로컬 전용' }),
    )

    expect(screen.queryByText('sshd')).not.toBeInTheDocument()
    expect(screen.getByText('docker-proxy')).toBeInTheDocument()
  })

  it('포트 요청 실패를 다른 모니터링 정보와 구분해 표시한다', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(
      async (input) => {
        const url = String(input)

        if (url === '/api/auth/me') {
          return jsonResponse(currentUser)
        }

        if (url === '/api/monitoring/docker/containers') {
          return jsonResponse(dockerContainers)
        }

        if (url === '/api/monitoring/ports') {
          return jsonResponse({}, 500)
        }

        return jsonResponse(serverStatus)
      },
    )

    render(<App />)

    expect(
      await screen.findByText(
        '사용 중인 포트 정보를 불러오지 못했습니다.',
      ),
    ).toBeInTheDocument()
    expect(screen.getByText('서버 온라인')).toBeInTheDocument()
  })
})
