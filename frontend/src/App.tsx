import {useCallback, useEffect, useState} from 'react'
import LoginPage from './auth/LoginPage'
import type {CurrentUser} from './auth/api'
import {getCurrentUser, login, logout,} from './auth/api'
import DockerContainerPanel from './monitoring/DockerContainerPanel'
import PortInventoryPanel from './monitoring/PortInventoryPanel'
import ServerMonitoringPanel from './monitoring/ServerMonitoringPanel'
import type {DockerContainerResponse, PortStatusResponse, ServerStatusResponse,} from './monitoring/api'
import {getDockerContainers, getPorts, getServerStatus,} from './monitoring/api'

import {Navigate, Route, Routes} from 'react-router'
import DashboardLayout from './layout/DashboardLayout'
import NasStoragePage from './synology/NasStoragePage'

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
    const [dockerContainers, setDockerContainers] =
        useState<DockerContainerResponse[]>([])
    const [isDockerLoading, setIsDockerLoading] =
        useState(true)
    const [dockerError, setDockerError] =
        useState<string | null>(null)
    const [ports, setPorts] =
        useState<PortStatusResponse[]>([])
    const [isPortLoading, setIsPortLoading] =
        useState(true)
    const [portError, setPortError] =
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

    const loadDockerContainers = useCallback(
        async (showLoading = false) => {
            if (showLoading) {
                setIsDockerLoading(true)
            }

            try {
                const containers = await getDockerContainers()

                setDockerContainers(containers)
                setDockerError(null)
            } catch (error) {
                setDockerError(
                    error instanceof Error
                        ? error.message
                        : 'Docker 컨테이너 정보를 불러오지 못했습니다.',
                )
            } finally {
                setIsDockerLoading(false)
            }
        },
        [],
    )

    const loadPorts = useCallback(
        async (showLoading = false) => {
            if (showLoading) {
                setIsPortLoading(true)
            }

            try {
                const portStatuses = await getPorts()

                setPorts(portStatuses)
                setPortError(null)
            } catch (error) {
                setPortError(
                    error instanceof Error
                        ? error.message
                        : '사용 중인 포트 정보를 불러오지 못했습니다.',
                )
            } finally {
                setIsPortLoading(false)
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
            void loadDockerContainers(true)
            void loadPorts(true)
        }, 0)

        const intervalId = window.setInterval(() => {
            void loadServerStatus(true)
            void loadDockerContainers()
            void loadPorts()
        }, MONITORING_REFRESH_INTERVAL)

        return () => {
            window.clearTimeout(initialLoadId)
            window.clearInterval(intervalId)
        }
    }, [
        currentUser,
        loadDockerContainers,
        loadPorts,
        loadServerStatus,
    ])

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
        setDockerContainers([])
        setDockerError(null)
        setIsDockerLoading(true)
        setPorts([])
        setPortError(null)
        setIsPortLoading(true)
    }

    if (currentUser === undefined) {
        return (
            <main className="grid min-h-screen place-items-center bg-slate-100 text-slate-700">
                <p role="status">로그인 상태 확인 중…</p>
            </main>
        )
    }

    if (currentUser === null) {
        return <LoginPage onLogin={handleLogin}/>
    }

    return (
        <Routes>
            <Route
                element={
                    <DashboardLayout
                        currentUser={currentUser}
                        onLogout={handleLogout}
                    />
                }
            >
                <Route
                    index
                    element={
                        <ServerMonitoringPanel
                            errorMessage={monitoringError}
                            isLoading={isMonitoringLoading}
                            isRefreshing={isMonitoringRefreshing}
                            onRefresh={() => {
                                void loadServerStatus(true)
                            }}
                            serverStatus={serverStatus}
                        />
                    }
                />

                <Route
                    path="docker"
                    element={
                        <DockerContainerPanel
                            containers={dockerContainers}
                            errorMessage={dockerError}
                            isLoading={isDockerLoading}
                        />
                    }
                />

                <Route
                    path="ports"
                    element={
                        <PortInventoryPanel
                            errorMessage={portError}
                            isLoading={isPortLoading}
                            ports={ports}
                        />
                    }
                />

                <Route
                    path="nas"
                    element={<NasStoragePage/>}
                />

                <Route
                    path="*"
                    element={<Navigate replace to="/"/>}
                />
            </Route>
        </Routes>
    )
}

export default App
