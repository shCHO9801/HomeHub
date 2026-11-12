import { useCallback, useEffect, useState } from 'react'
import LoginPage from './auth/LoginPage'
import {
  getCurrentUser,
  login,
  logout,
} from './auth/api'
import type { CurrentUser } from './auth/api'
import ServerMonitoringPanel from './monitoring/ServerMonitoringPanel'
import { getServerStatus } from './monitoring/api'
import type { ServerStatusResponse } from './monitoring/api'

const MONITORING_REFRESH_INTERVAL = 10_000

function App() {
  const [currentUser, setCurrentUser] =
    useState<CurrentUser | null | undefined>(undefined)
  const [serverStatus, setServerStatus] =
    useState<ServerStatusResponse | null>(null)
  const [isMonitoringLoading, setIsMonitoringLoading] =
    useState(true)
  const [isMonitoringRefreshing, setIsMonitoringRefreshing] =
    useState(false)
  const [monitoringError, setMonitoringError] =
    useState<string | null>(null)

  useEffect(() => {
    let active = true

    const checkAuthentication = async () => {
      try {
        const user = await getCurrentUser()

        if (active) {
          setCurrentUser(user)
        }
      } catch {
        if (active) {
          setCurrentUser(null)
        }
      }
    }

    void checkAuthentication()

    return () => {
      active = false
    }
  }, [])

  const loadServerStatus = useCallback(
    async (refreshing = false) => {
      if (refreshing) {
        setIsMonitoringRefreshing(true)
      } else {
        setIsMonitoringLoading(true)
      }

      try {
        const status = await getServerStatus()

        setServerStatus(status)
        setMonitoringError(null)
      } catch (error) {
        setMonitoringError(
          error instanceof Error
            ? error.message
            : '서버 상태를 불러오지 못했습니다.',
        )
      } finally {
        setIsMonitoringLoading(false)
        setIsMonitoringRefreshing(false)
      }
    },
    [],
  )

  useEffect(() => {
    if (!currentUser) {
      return
    }

    const initialLoadId = window.setTimeout(() => {
      void loadServerStatus()
    }, 0)

    const intervalId = window.setInterval(() => {
      void loadServerStatus(true)
    }, MONITORING_REFRESH_INTERVAL)

    return () => {
      window.clearTimeout(initialLoadId)
      window.clearInterval(intervalId)
    }
  }, [currentUser, loadServerStatus])

  const handleLogin = async (
    username: string,
    password: string,
  ) => {
    await login(username, password)

    const user = await getCurrentUser()

    if (!user) {
      throw new Error(
        '로그인 사용자 정보를 확인하지 못했습니다.',
      )
    }

    setCurrentUser(user)
  }

  const handleLogout = async () => {
    await logout()
    setCurrentUser(null)
    setServerStatus(null)
    setMonitoringError(null)
    setIsMonitoringLoading(true)
  }

  if (currentUser === undefined) {
    return (
      <main className="grid min-h-screen place-items-center bg-slate-100 text-slate-700">
        <p role="status">로그인 상태 확인 중…</p>
      </main>
    )
  }

  if (currentUser === null) {
    return <LoginPage onLogin={handleLogin} />
  }

  return (
    <main className="min-h-screen bg-slate-100 px-6 py-12 text-slate-950">
      <div className="mx-auto w-full max-w-5xl space-y-6">
        <section className="rounded-2xl border border-slate-200 bg-white px-8 py-7 shadow-sm">
          <p className="font-mono text-xs tracking-[0.16em] text-slate-500 uppercase">
            Local network · System check
          </p>
          <h1 className="mt-3 text-3xl font-semibold tracking-tight">
            My Home Hub
          </h1>
          <p className="mt-2 max-w-md text-sm leading-6 text-slate-600">
            홈 인프라 서비스의 연결 상태를 확인합니다.
          </p>

          <div className="mt-5 flex items-center justify-between gap-4 border-t border-slate-200 pt-5">
            <div>
              <p className="text-sm font-medium text-slate-900">
                {currentUser.username}
              </p>
              <p className="mt-1 font-mono text-xs text-slate-500">
                {currentUser.role}
              </p>
            </div>

            <button
              className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
              onClick={() => void handleLogout()}
              type="button"
            >
              로그아웃
            </button>
          </div>
        </section>

        <ServerMonitoringPanel
          errorMessage={monitoringError}
          isLoading={isMonitoringLoading}
          isRefreshing={isMonitoringRefreshing}
          onRefresh={() => void loadServerStatus(true)}
          serverStatus={serverStatus}
        />
      </div>
    </main>
  )
}

export default App
