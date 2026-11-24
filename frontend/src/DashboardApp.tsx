import {useCallback, useEffect, useState} from 'react'
import {Navigate, Route, Routes} from 'react-router'
import type {CurrentUser} from './auth/api'
import DashboardLayout from './layout/DashboardLayout'
import DockerContainerPanel from './monitoring/DockerContainerPanel'
import MonitoringAlertPanel from './monitoring/MonitoringAlertPanel'
import MonitoringHistoryPanel from './monitoring/MonitoringHistoryPanel'
import PortInventoryPanel from './monitoring/PortInventoryPanel'
import ServerMonitoringPanel from './monitoring/ServerMonitoringPanel'
import type {
    DockerContainerResponse,
    PortStatusResponse,
    ServerStatusResponse,
} from './monitoring/api'
import {
    getDockerContainers,
    getPorts,
    getServerStatus,
} from './monitoring/api'
import NasStoragePage from './synology/NasStoragePage'

const MONITORING_REFRESH_INTERVAL = 10_000

type DashboardAppProps = {
    currentUser: CurrentUser
    onLogout: () => Promise<void>
}

function DashboardApp({
                          currentUser,
                          onLogout,
                      }: DashboardAppProps) {
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

    const loadServerStatus = useCallback(
        async (refreshing = false) => {
            if (refreshing) {
                setIsMonitoringRefreshing(true)
            } else {
                setIsMonitoringLoading(true)
            }

            try {
                setServerStatus(await getServerStatus())
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
                setDockerContainers(
                    await getDockerContainers(),
                )
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
                setPorts(await getPorts())
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
        loadDockerContainers,
        loadPorts,
        loadServerStatus,
    ])

    return (
        <Routes>
            <Route
                element={
                    <DashboardLayout
                        currentUser={currentUser}
                        onLogout={onLogout}
                    />
                }
            >
                <Route
                    index
                    element={
                        <div className="space-y-6">
                            <MonitoringAlertPanel/>

                            <ServerMonitoringPanel
                                errorMessage={monitoringError}
                                isLoading={isMonitoringLoading}
                                isRefreshing={isMonitoringRefreshing}
                                onRefresh={() => {
                                    void loadServerStatus(true)
                                }}
                                serverStatus={serverStatus}
                            />

                            <MonitoringHistoryPanel/>
                        </div>
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

export default DashboardApp
