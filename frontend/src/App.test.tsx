import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
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

const synologyStorage = {
  status: 'AVAILABLE',
  stale: false,
  volumes: [
    {
      name: 'Volume 1',
      path: '/volume1',
      totalBytes: 3_829_365_997_568,
      usedBytes: 784_113_881_088,
      availableBytes: 3_045_252_116_480,
      usagePercent: 20.48,
    },
  ],
  storageItems: [
    {
      name: 'Volume 1',
      type: 'VOLUME',
      health: 'HEALTHY',
    },
    {
      name: 'Storage Pool 1',
      type: 'STORAGE_POOL',
      health: 'HEALTHY',
    },
  ],
  collectedAt: '2026-11-23T14:47:03Z',
}

const monitoringHistory = [
  {
    cpuUsagePercent: 12.5,
    memoryUsagePercent: 45.5,
    diskUsagePercent: 20.4,
    collectedAt: '2026-11-24T00:00:00Z',
  },
  {
    cpuUsagePercent: 18.2,
    memoryUsagePercent: 47.1,
    diskUsagePercent: 20.5,
    collectedAt: '2026-11-24T00:05:00Z',
  },
]

const normalAlertSummary = {
  status: 'NORMAL',
  alerts: [],
  checkedAt: '2026-11-24T08:00:00Z',
}

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

function renderApp(path = '/') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}

function mockAuthenticatedDashboard(
  alertSummary: unknown,
) {
  return vi
    .spyOn(globalThis, 'fetch')
    .mockImplementation(async (input) => {
      const url = String(input)

      if (url === '/api/auth/me') {
        return jsonResponse(currentUser)
      }

      if (url === '/api/monitoring/alerts') {
        return jsonResponse(alertSummary)
      }

      if (url === '/api/monitoring/history?hours=24') {
        return jsonResponse([])
      }

      if (url === '/api/monitoring/docker/containers') {
        return jsonResponse(dockerContainers)
      }

      if (url === '/api/monitoring/ports') {
        return jsonResponse(ports)
      }

      return jsonResponse(serverStatus)
    })
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('App 인증 흐름', () => {
  it('로그인 상태를 확인하는 동안 로딩 상태를 표시한다', () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(
      () => new Promise<Response>(() => {}),
    )

    renderApp()

    expect(
      screen.getByText('로그인 상태 확인 중…'),
    ).toBeInTheDocument()
  })

  it('미인증 사용자에게 로그인 화면을 표시한다', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(jsonResponse({}, 401))

    renderApp()

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

    renderApp()

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
      await screen.findAllByText('sunghyun-admin'),
    ).toHaveLength(2)
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

    renderApp()

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

    renderApp()

    const logoutButtons = await screen.findAllByRole(
      'button',
      { name: '로그아웃' },
    )

    expect(logoutButtons).toHaveLength(2)
    fireEvent.click(logoutButtons[0])

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

    renderApp()

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

    renderApp()

    expect(
      await screen.findByText(
        '서버 모니터링 정보를 불러오지 못했습니다.',
      ),
    ).toBeInTheDocument()
  })
})

describe('App 모니터링 경고', () => {
  it('경고가 없으면 현재 상태가 정상임을 표시한다', async () => {
    const fetchMock = mockAuthenticatedDashboard(
      normalAlertSummary,
    )

    renderApp()

    expect(
      await screen.findByRole('heading', {
        name: '현재 경고 없음',
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByText(
        '서버, Docker, NAS 상태가 정상입니다.',
      ),
    ).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/monitoring/alerts',
      { credentials: 'include' },
    )
  })

  it('주의 경고가 있으면 확인이 필요한 항목을 표시한다', async () => {
    mockAuthenticatedDashboard({
      status: 'WARNING',
      alerts: [
        {
          source: 'SERVER',
          status: 'WARNING',
          title: '디스크 사용률 경고',
          message: '디스크 사용률이 90.0%입니다.',
        },
      ],
      checkedAt: '2026-11-24T08:00:00Z',
    })

    renderApp()

    expect(
      await screen.findByRole('heading', {
        name: '확인이 필요한 항목이 있습니다',
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByText('디스크 사용률 경고'),
    ).toBeInTheDocument()
    expect(screen.getByText('서버')).toBeInTheDocument()
  })

  it('위험 경고가 있으면 즉시 확인이 필요함을 표시한다', async () => {
    mockAuthenticatedDashboard({
      status: 'CRITICAL',
      alerts: [
        {
          source: 'DOCKER',
          status: 'CRITICAL',
          title: 'Docker 컨테이너 비정상',
          message: 'backend 컨테이너가 비정상 상태입니다.',
        },
      ],
      checkedAt: '2026-11-24T08:00:00Z',
    })

    renderApp()

    expect(
      await screen.findByRole('heading', {
        name: '즉시 확인이 필요합니다',
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByText('Docker 컨테이너 비정상'),
    ).toBeInTheDocument()
    expect(screen.getByText('CRITICAL')).toBeInTheDocument()
  })
})

describe('App 서버 모니터링 이력', () => {
  it('최근 24시간 서버 자원 사용률 그래프를 표시한다', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockImplementation(async (input) => {
        const url = String(input)

        if (url === '/api/auth/me') {
          return jsonResponse(currentUser)
        }

        if (url === '/api/monitoring/history?hours=24') {
          return jsonResponse(monitoringHistory)
        }

        if (url === '/api/monitoring/docker/containers') {
          return jsonResponse(dockerContainers)
        }

        if (url === '/api/monitoring/ports') {
          return jsonResponse(ports)
        }

        return jsonResponse(serverStatus)
      })

    renderApp()

    expect(
      await screen.findByRole('img', {
        name: '최근 24시간 서버 자원 사용률',
      }),
    ).toBeInTheDocument()
    expect(screen.getAllByText('CPU')).toHaveLength(2)
    expect(screen.getByText('메모리')).toBeInTheDocument()
    expect(screen.getByText('디스크')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/monitoring/history?hours=24',
      { credentials: 'include' },
    )
  })

  it('수집된 이력이 없으면 빈 상태를 표시한다', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(
      async (input) => {
        const url = String(input)

        if (url === '/api/auth/me') {
          return jsonResponse(currentUser)
        }

        if (url === '/api/monitoring/history?hours=24') {
          return jsonResponse([])
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

    renderApp()

    expect(
      await screen.findByText(
        '아직 수집된 서버 모니터링 이력이 없습니다.',
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

    renderApp('/docker')

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

    renderApp('/docker')

    expect(
      await screen.findByText(
        'Docker 컨테이너 정보를 불러오지 못했습니다.',
      ),
    ).toBeInTheDocument()
    expect(
      screen.queryByText('서버 온라인'),
    ).not.toBeInTheDocument()
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

    renderApp('/ports')

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

    renderApp('/ports')

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

    renderApp('/ports')

    expect(
      await screen.findByText(
        '사용 중인 포트 정보를 불러오지 못했습니다.',
      ),
    ).toBeInTheDocument()
    expect(
      screen.queryByText('서버 온라인'),
    ).not.toBeInTheDocument()
  })
})

describe('App Synology NAS 저장공간', () => {
  it('인증된 사용자에게 NAS 저장공간과 건강 상태를 표시한다', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockImplementation(async (input) => {
        const url = String(input)

        if (url === '/api/auth/me') {
          return jsonResponse(currentUser)
        }

        if (url === '/api/synology/storage') {
          return jsonResponse(synologyStorage)
        }

        if (url === '/api/monitoring/docker/containers') {
          return jsonResponse(dockerContainers)
        }

        if (url === '/api/monitoring/ports') {
          return jsonResponse(ports)
        }

        return jsonResponse(serverStatus)
      })

    renderApp('/nas')

    expect(
      await screen.findByRole('heading', {
        name: '저장공간 정상',
      }),
    ).toBeInTheDocument()
    expect(screen.getAllByText('Volume 1')).toHaveLength(2)
    expect(screen.getByText('20.5%')).toBeInTheDocument()
    expect(screen.getByText('Storage Pool 1')).toBeInTheDocument()
    expect(screen.getAllByText('정상')).toHaveLength(2)
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/synology/storage',
      { credentials: 'include' },
    )
  })

  it('NAS 수집에 실패하면 상태 확인 불가를 표시한다', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(
      async (input) => {
        const url = String(input)

        if (url === '/api/auth/me') {
          return jsonResponse(currentUser)
        }

        if (url === '/api/synology/storage') {
          return jsonResponse({
            status: 'UNAVAILABLE',
            stale: false,
            volumes: [],
            storageItems: [],
            collectedAt: null,
          })
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

    renderApp('/nas')

    expect(
      await screen.findByRole('heading', {
        name: '상태 확인 불가',
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByText(
        'NAS 저장공간 정보를 수집하지 못했습니다.',
      ),
    ).toBeInTheDocument()
  })

  it('이전 수집 데이터를 표시할 때 stale 상태를 안내한다', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(
      async (input) => {
        const url = String(input)

        if (url === '/api/auth/me') {
          return jsonResponse(currentUser)
        }

        if (url === '/api/synology/storage') {
          return jsonResponse({
            ...synologyStorage,
            status: 'UNAVAILABLE',
            stale: true,
          })
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

    renderApp('/nas')

    expect(
      await screen.findByText(
        '현재 수집에 실패해 마지막 정상 정보를 표시합니다.',
      ),
    ).toBeInTheDocument()
    expect(screen.getByText('20.5%')).toBeInTheDocument()
  })
})
