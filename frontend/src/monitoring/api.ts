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
