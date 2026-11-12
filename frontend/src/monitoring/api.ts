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
