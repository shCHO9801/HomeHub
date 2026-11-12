const BYTE_UNITS = ['B', 'KB', 'MB', 'GB', 'TB']

export function formatBytes(bytes: number): string {
    if (bytes <= 0) {
        return '0 B'
    }

    const unitIndex = Math.min(
        Math.floor(Math.log(bytes) / Math.log(1024)),
        BYTE_UNITS.length - 1,
    )

    const value = bytes / 1024 ** unitIndex

    return `${value.toFixed(unitIndex === 0 ? 0 : 1)} ${BYTE_UNITS[unitIndex]}`
}

export function formatUptime(seconds: number): string {
    const days = Math.floor(seconds / 86_400)
    const hours = Math.floor((seconds % 86_400) / 3_600)
    const minutes = Math.floor((seconds % 3_600) / 60)

    const parts = []

    if (days > 0) {
        parts.push(`${days}일`)
    }

    if (hours > 0) {
        parts.push(`${hours}시간`)
    }

    parts.push(`${minutes}분`)

    return parts.join(' ')
}

export function formatCollectedAt(value: string): string {
    return new Intl.DateTimeFormat('ko-KR', {
        dateStyle: 'medium',
        timeStyle: 'medium',
    }).format(new Date(value))
}