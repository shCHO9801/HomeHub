import {
    useCallback,
    useEffect,
    useState,
} from 'react'
import type {
    MonitoringAlertSource,
    MonitoringAlertSummaryResponse,
} from './api'
import {getMonitoringAlerts} from './api'
import {formatCollectedAt} from './format'

const ALERT_REFRESH_INTERVAL = 60_000

const sourceLabels: Record<MonitoringAlertSource, string> = {
    SERVER: '서버',
    DOCKER: 'Docker',
    NAS: 'NAS',
}

function MonitoringAlertPanel() {
    const [summary, setSummary] =
        useState<MonitoringAlertSummaryResponse | null>(null)
    const [isLoading, setIsLoading] = useState(true)
    const [isRefreshing, setIsRefreshing] =
        useState(false)
    const [errorMessage, setErrorMessage] =
        useState<string | null>(null)

    const loadAlerts = useCallback(
        async (refreshing = false) => {
            if (refreshing) {
                setIsRefreshing(true)
            } else {
                setIsLoading(true)
            }

            try {
                setSummary(await getMonitoringAlerts())
                setErrorMessage(null)
            } catch (error) {
                setErrorMessage(
                    error instanceof Error
                        ? error.message
                        : '모니터링 경고를 불러오지 못했습니다.',
                )
            } finally {
                setIsLoading(false)
                setIsRefreshing(false)
            }
        },
        [],
    )

    useEffect(() => {
        const initialLoadId = window.setTimeout(() => {
            void loadAlerts()
        }, 0)

        const intervalId = window.setInterval(() => {
            void loadAlerts(true)
        }, ALERT_REFRESH_INTERVAL)

        return () => {
            window.clearTimeout(initialLoadId)
            window.clearInterval(intervalId)
        }
    }, [loadAlerts])

    if (isLoading) {
        return (
            <section
                className="rounded-2xl border border-slate-200 bg-white p-6"
                role="status"
            >
                모니터링 경고를 확인하는 중…
            </section>
        )
    }

    if (!summary) {
        return (
            <section className="rounded-2xl border border-amber-200 bg-amber-50 p-6">
                <p className="text-sm text-amber-900">
                    {errorMessage ??
                        '모니터링 경고를 불러오지 못했습니다.'}
                </p>

                <button
                    className="mt-4 rounded-lg bg-amber-900 px-4 py-2 text-sm font-semibold text-white"
                    onClick={() => void loadAlerts()}
                    type="button"
                >
                    다시 시도
                </button>
            </section>
        )
    }

    const isNormal = summary.status === 'NORMAL'
    const isCritical = summary.status === 'CRITICAL'

    return (
        <section
            className={`rounded-2xl border p-6 ${
                isNormal
                    ? 'border-emerald-200 bg-emerald-50'
                    : isCritical
                        ? 'border-red-200 bg-red-50'
                        : 'border-amber-200 bg-amber-50'
            }`}
        >
            <header className="flex flex-wrap items-start justify-between gap-4">
                <div>
                    <div className="flex items-center gap-2">
                        <span
                            aria-hidden="true"
                            className={`size-2.5 rounded-full ${
                                isNormal
                                    ? 'bg-emerald-500'
                                    : isCritical
                                        ? 'bg-red-500'
                                        : 'bg-amber-500'
                            }`}
                        />

                        <p className="font-mono text-xs tracking-[0.14em] text-slate-600 uppercase">
                            Monitoring alerts
                        </p>
                    </div>

                    <h2 className="mt-2 text-xl font-semibold tracking-tight">
                        {isNormal
                            ? '현재 경고 없음'
                            : isCritical
                                ? '즉시 확인이 필요합니다'
                                : '확인이 필요한 항목이 있습니다'}
                    </h2>

                    <p className="mt-2 text-sm text-slate-600">
                        {isNormal
                            ? '서버, Docker, NAS 상태가 정상입니다.'
                            : `경고 ${summary.alerts.length}건이 감지됐습니다.`}
                    </p>
                </div>

                <button
                    className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100 disabled:opacity-60"
                    disabled={isRefreshing}
                    onClick={() => void loadAlerts(true)}
                    type="button"
                >
                    {isRefreshing ? '갱신 중…' : '새로고침'}
                </button>
            </header>

            {summary.alerts.length > 0 && (
                <ul className="mt-5 space-y-3">
                    {summary.alerts.map((alert, index) => (
                        <li
                            className="rounded-xl border border-white/80 bg-white px-4 py-4"
                            key={`${alert.source}-${alert.title}-${index}`}
                        >
                            <div className="flex flex-wrap items-center gap-2">
                                <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600">
                                    {sourceLabels[alert.source]}
                                </span>

                                <span
                                    className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
                                        alert.status === 'CRITICAL'
                                            ? 'bg-red-100 text-red-700'
                                            : 'bg-amber-100 text-amber-700'
                                    }`}
                                >
                                    {alert.status}
                                </span>
                            </div>

                            <p className="mt-3 text-sm font-semibold">
                                {alert.title}
                            </p>
                            <p className="mt-1 text-sm text-slate-600">
                                {alert.message}
                            </p>
                        </li>
                    ))}
                </ul>
            )}

            <p className="mt-4 text-right text-xs text-slate-500">
                확인 시각 {formatCollectedAt(summary.checkedAt)}
            </p>
        </section>
    )
}

export default MonitoringAlertPanel