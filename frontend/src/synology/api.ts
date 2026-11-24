export type SynologyConnectionStatus =
    | 'AVAILABLE'
    | 'UNAVAILABLE'

export type SynologyStorageHealth =
    | 'HEALTHY'
    | 'WARNING'
    | 'CRITICAL'
    | 'UNKNOWN'

export type SynologyStorageItemType =
    | 'VOLUME'
    | 'STORAGE_POOL'
    | 'UNKNOWN'

export type SynologyVolumeResponse = {
    name: string
    path: string
    totalBytes: number
    usedBytes: number
    availableBytes: number
    usagePercent: number
}

export type SynologyStorageItemResponse = {
    name: string
    type: SynologyStorageItemType
    health: SynologyStorageHealth
}

export type SynologyStorageResponse = {
    status: SynologyConnectionStatus
    stale: boolean
    volumes: SynologyVolumeResponse[]
    storageItems: SynologyStorageItemResponse[]
    collectedAt: string | null
}

export async function getSynologyStorageStatus():
    Promise<SynologyStorageResponse> {
    try {
        const response = await fetch(
            '/api/synology/storage',
            {
                credentials: 'include',
            },
        )

        if (!response.ok) {
            throw new Error()
        }

        return (await response.json()) as SynologyStorageResponse
    } catch {
        throw new Error(
            'NAS 저장공간 정보를 불러오지 못했습니다.',
        )
    }
}