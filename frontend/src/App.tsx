import { useEffect, useState } from 'react'

type ApiStatus = 'loading' | 'online' | 'offline'

interface HealthResponse {
  status: string
}

const statusContent: Record<
  ApiStatus,
  { label: string; description: string; containerClass: string; dotClass: string }
> = {
  loading: {
    label: '서버 상태 확인 중',
    description: '백엔드 응답을 기다리고 있습니다.',
    containerClass: 'border-slate-200 bg-slate-50 text-slate-700',
    dotClass: 'bg-slate-400',
  },
  online: {
    label: 'API 온라인',
    description: '백엔드와 데이터베이스가 응답하고 있습니다.',
    containerClass: 'border-emerald-200 bg-emerald-50 text-emerald-900',
    dotClass: 'bg-emerald-500',
  },
  offline: {
    label: 'API 연결 실패',
    description: '백엔드 실행 상태와 연결 주소를 확인하세요.',
    containerClass: 'border-amber-200 bg-amber-50 text-amber-950',
    dotClass: 'bg-amber-500',
  },
}

function App() {
  const [apiStatus, setApiStatus] = useState<ApiStatus>('loading')

  useEffect(() => {
    let active = true
    const controller = new AbortController()
    const timeoutId = window.setTimeout(() => controller.abort(), 5_000)

    const checkHealth = async () => {
      try {
        const response = await fetch('/actuator/health', {
          signal: controller.signal,
        })

        if (!response.ok) {
          if (active) {
            setApiStatus('offline')
          }
          return
        }

        const health = (await response.json()) as HealthResponse

        if (active) {
          setApiStatus(health.status === 'UP' ? 'online' : 'offline')
        }
      } catch {
        if (active) {
          setApiStatus('offline')
        }
      } finally {
        window.clearTimeout(timeoutId)
      }
    }

    void checkHealth()

    return () => {
      active = false
      window.clearTimeout(timeoutId)
      controller.abort()
    }
  }, [])

  const status = statusContent[apiStatus]

  return (
    <main className="grid min-h-screen place-items-center bg-slate-100 px-6 py-12 text-slate-950">
      <section className="w-full max-w-xl overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-200 px-8 py-7">
          <p className="font-mono text-xs tracking-[0.16em] text-slate-500 uppercase">
            Local network · System check
          </p>
          <h1 className="mt-3 text-3xl font-semibold tracking-tight">
            My Home Hub
          </h1>
          <p className="mt-2 max-w-md text-sm leading-6 text-slate-600">
            홈 인프라 서비스의 연결 상태를 확인합니다.
          </p>
        </div>

        <div className="px-8 py-7">
          <p className="text-xs font-semibold tracking-[0.14em] text-slate-500 uppercase">
            Backend
          </p>
          <div
            aria-live="polite"
            className={`mt-3 flex items-start gap-3 rounded-xl border px-4 py-4 ${status.containerClass}`}
            role="status"
          >
            <span
              aria-hidden="true"
              className={`mt-1.5 size-2.5 shrink-0 rounded-full ${status.dotClass}`}
            />
            <span>
              <span className="block text-sm font-semibold">{status.label}</span>
              <span className="mt-1 block text-sm opacity-75">
                {status.description}
              </span>
            </span>
          </div>
        </div>
      </section>
    </main>
  )
}

export default App
