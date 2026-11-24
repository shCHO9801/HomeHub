export type ServerStatus = 'ONLINE' | 'UNKNOWN'

export type ResourceUsage = {
    totalBytes: number
    usedBytes: number
    availableBytes: number
    usagePercent: number
}

export type ServerStatusResponse = {
    status: ServerStatus
    cpuUsagePercent: number | null
    logicalProcessorCount: number | null
    memory: ResourceUsage | null
    disk: ResourceUsage | null
    uptimeSeconds: number | null
    collectedAt: string
}

export type DockerContainerState =
    | 'RUNNING'
    | 'STOPPED'
    | 'UNHEALTHY'
    | 'UNKNOWN'

export type DockerContainerResponse = {
    id: string
    name: string
    image: string
    state: DockerContainerState
    statusDetail: string
    ports: string
    composeProject: string
}

export type PortProtocol = 'TCP' | 'UDP'

export type PortExposure =
    | 'LOOPBACK'
    | 'ALL_INTERFACES'
    | 'SPECIFIC_INTERFACE'

export type PortStatusResponse = {
    protocol: PortProtocol
    port: number
    bindAddress: string
    exposure: PortExposure
    processName: string | null
}

export type MonitoringHistoryResponse = {
    cpuUsagePercent: number
    memoryUsagePercent: number
    diskUsagePercent: number
    collectedAt: string
}

export type MonitoringAlertStatus =
    | 'NORMAL'
    | 'WARNING'
    | 'CRITICAL'

export type MonitoringAlertSource =
    | 'SERVER'
    | 'DOCKER'
    | 'NAS'

export type MonitoringAlertResponse = {
    source: MonitoringAlertSource
    status: MonitoringAlertStatus
    title: string
    message: string
}

export type MonitoringAlertSummaryResponse = {
    status: MonitoringAlertStatus
    alerts: MonitoringAlertResponse[]
    checkedAt: string
}

export async function getMonitoringAlerts():
    Promise<MonitoringAlertSummaryResponse> {
    try {
        const response = await fetch(
            '/api/monitoring/alerts',
            {
                credentials: 'include',
            },
        )

        if (!response.ok) {
            throw new Error()
        }

        const summary = await response.json()

        if (
            typeof summary !== 'object'
            || summary === null
            || !Array.isArray(summary.alerts)
        ) {
            throw new Error()
        }

        return summary as MonitoringAlertSummaryResponse
    } catch {
        throw new Error(
            '모니터링 경고를 불러오지 못했습니다.',
        )
    }
}

export async function getMonitoringHistory(
    hours = 24,
): Promise<MonitoringHistoryResponse[]> {
    try {
        const response = await fetch(
            `/api/monitoring/history?hours=${hours}`,
            {
                credentials: 'include',
            },
        )

        if (!response.ok) {
            throw new Error()
        }

        const history = await response.json()

        if (!Array.isArray(history)) {
            throw new Error()
        }

        return history as MonitoringHistoryResponse[]
    } catch {
        throw new Error(
            '서버 모니터링 이력을 불러오지 못했습니다.',
        )
    }
}

export async function getPorts(): Promise<PortStatusResponse[]> {
    try {
        const response = await fetch('/api/monitoring/ports', {
            credentials: 'include',
        })

        if (!response.ok) {
            throw new Error()
        }

        return (await response.json()) as PortStatusResponse[]
    } catch {
        throw new Error(
            '사용 중인 포트 정보를 불러오지 못했습니다.',
        )
    }
}

export async function getDockerContainers(): Promise<
    DockerContainerResponse[]
> {
    try {
        const response = await fetch(
            '/api/monitoring/docker/containers',
            {
                credentials: 'include',
            },
        )

        if (!response.ok) {
            throw new Error()
        }

        return (await response.json()) as DockerContainerResponse[]
    } catch {
        throw new Error(
            'Docker 컨테이너 정보를 불러오지 못했습니다.',
        )
    }
}

export async function getServerStatus(): Promise<ServerStatusResponse> {
    try {
        const response = await fetch('/api/monitoring/server', {
            credentials: 'include',
        })

        if (!response.ok) {
            throw new Error()
        }

        return (await response.json()) as ServerStatusResponse
    } catch {
        throw new Error(
            '서버 모니터링 정보를 불러오지 못했습니다.',
        )
    }
}
