import type {MonitoringHistoryResponse} from './api'

type MonitoringHistoryChartProps = {
    history: MonitoringHistoryResponse[]
}

type Series = {
    key:
        | 'cpuUsagePercent'
        | 'memoryUsagePercent'
        | 'diskUsagePercent'
    label: string
    color: string
}

const WIDTH = 800
const HEIGHT = 260
const PADDING = {
    top: 20,
    right: 20,
    bottom: 40,
    left: 48,
}

const series: Series[] = [
    {
        key: 'cpuUsagePercent',
        label: 'CPU',
        color: '#14b8a6',
    },
    {
        key: 'memoryUsagePercent',
        label: '메모리',
        color: '#6366f1',
    },
    {
        key: 'diskUsagePercent',
        label: '디스크',
        color: '#f59e0b',
    },
]

const yTicks = [0, 25, 50, 75, 100]

function MonitoringHistoryChart({
                                    history,
                                }: MonitoringHistoryChartProps) {
    if (history.length < 2) {
        return (
            <p className="rounded-xl border border-slate-200 bg-white px-5 py-8 text-center text-sm text-slate-500">
                그래프를 표시하려면 두 개 이상의 수집 데이터가 필요합니다.
            </p>
        )
    }

    const chartWidth =
        WIDTH - PADDING.left - PADDING.right
    const chartHeight =
        HEIGHT - PADDING.top - PADDING.bottom

    const timestamps = history.map((item) =>
        new Date(item.collectedAt).getTime(),
    )
    const startTime = Math.min(...timestamps)
    const endTime = Math.max(...timestamps)
    const timeRange = Math.max(endTime - startTime, 1)

    const toX = (collectedAt: string) =>
        PADDING.left +
        ((new Date(collectedAt).getTime() - startTime) /
            timeRange) *
        chartWidth

    const toY = (value: number) =>
        PADDING.top +
        chartHeight -
        (Math.min(Math.max(value, 0), 100) / 100) *
        chartHeight

    const formatTime = (value: number) =>
        new Intl.DateTimeFormat('ko-KR', {
            hour: '2-digit',
            minute: '2-digit',
        }).format(new Date(value))

    const xLabels = [
        startTime,
        startTime + timeRange / 2,
        endTime,
    ]

    return (
        <div>
            <div className="mb-4 flex flex-wrap gap-4">
                {series.map((item) => (
                    <div
                        className="flex items-center gap-2 text-xs text-slate-600"
                        key={item.key}
                    >
                        <span
                            aria-hidden="true"
                            className="size-2.5 rounded-full"
                            style={{
                                backgroundColor: item.color,
                            }}
                        />
                        {item.label}
                    </div>
                ))}
            </div>

            <div className="overflow-x-auto">
                <svg
                    aria-label="최근 24시간 서버 자원 사용률"
                    className="min-w-[640px]"
                    role="img"
                    viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
                >
                    {yTicks.map((tick) => {
                        const y = toY(tick)

                        return (
                            <g key={tick}>
                                <line
                                    stroke="#e2e8f0"
                                    strokeDasharray="4 4"
                                    x1={PADDING.left}
                                    x2={WIDTH - PADDING.right}
                                    y1={y}
                                    y2={y}
                                />
                                <text
                                    fill="#64748b"
                                    fontSize="11"
                                    textAnchor="end"
                                    x={PADDING.left - 10}
                                    y={y + 4}
                                >
                                    {tick}%
                                </text>
                            </g>
                        )
                    })}

                    {xLabels.map((timestamp, index) => {
                        const x =
                            PADDING.left +
                            (index / (xLabels.length - 1)) *
                            chartWidth

                        return (
                            <text
                                fill="#64748b"
                                fontSize="11"
                                key={timestamp}
                                textAnchor={
                                    index === 0
                                        ? 'start'
                                        : index ===
                                        xLabels.length - 1
                                            ? 'end'
                                            : 'middle'
                                }
                                x={x}
                                y={HEIGHT - 10}
                            >
                                {formatTime(timestamp)}
                            </text>
                        )
                    })}

                    {series.map((item) => {
                        const points = history
                            .map(
                                (historyItem) =>
                                    `${toX(
                                        historyItem.collectedAt,
                                    )},${toY(
                                        historyItem[item.key],
                                    )}`,
                            )
                            .join(' ')

                        return (
                            <g key={item.key}>
                                <polyline
                                    fill="none"
                                    points={points}
                                    stroke={item.color}
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    strokeWidth="2.5"
                                />

                                {history.map((historyItem) => (
                                    <circle
                                        cx={toX(
                                            historyItem.collectedAt,
                                        )}
                                        cy={toY(
                                            historyItem[item.key],
                                        )}
                                        fill={item.color}
                                        key={`${item.key}-${historyItem.collectedAt}`}
                                        r="3"
                                    >
                                        <title>
                                            {item.label}{' '}
                                            {historyItem[
                                                item.key
                                                ].toFixed(1)}
                                            % ·{' '}
                                            {formatTime(
                                                new Date(
                                                    historyItem.collectedAt,
                                                ).getTime(),
                                            )}
                                        </title>
                                    </circle>
                                ))}
                            </g>
                        )
                    })}
                </svg>
            </div>
        </div>
    )
}

export default MonitoringHistoryChart