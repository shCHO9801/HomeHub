import {
    useCallback,
    useEffect,
    useState,
} from 'react'
import type {MonitoringHistoryResponse} from './api'
import {getMonitoringHistory} from './api'
import MonitoringHistoryChart from './MonitoringHistoryChart'

function MonitoringHistoryPanel() {
    const [history, setHistory] =
        useState<MonitoringHistoryResponse[]>([])
    const [isLoading, setIsLoading] = useState(true)
    const [isRefreshing, setIsRefreshing] =
        useState(false)
    const [errorMessage, setErrorMessage] =
        useState<string | null>(null)

    const loadHistory = useCallback(
        async (refreshing = false) => {
            if (refreshing) {
                setIsRefreshing(true)
            } else {
                setIsLoading(true)
            }

            try {
                const response =
                    await getMonitoringHistory(24)

                setHistory(response)
                setErrorMessage(null)
            } catch (error) {
                setErrorMessage(
                    error instanceof Error
                        ? error.message
                        : '서버 모니터링 이력을 불러오지 못했습니다.',
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
            void loadHistory()
        }, 0)

        return () => {
            window.clearTimeout(initialLoadId)
        }
    }, [loadHistory])

    return (
        <section className="rounded-2xl border border-slate-200 bg-slate-50 p-6">
            <header className="flex flex-wrap items-start justify-between gap-4">
                <div>
                    <p className="font-mono text-xs tracking-[0.14em] text-slate-500 uppercase">
                        Last 24 hours
                    </p>

                    <h2 className="mt-2 text-2xl font-semibold tracking-tight">
                        서버 자원 사용 추이
                    </h2>

                    <p className="mt-2 text-sm text-slate-500">
                        5분마다 수집한 CPU, 메모리, 디스크 사용률입니다.
                    </p>
                </div>

                <button
                    className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100 disabled:opacity-60"
                    disabled={isLoading || isRefreshing}
                    onClick={() => void loadHistory(true)}
                    type="button"
                >
                    {isRefreshing ? '갱신 중…' : '새로고침'}
                </button>
            </header>

            <div className="mt-6">
                {isLoading ? (
                    <p
                        className="rounded-xl border border-slate-200 bg-white px-5 py-8 text-center text-sm text-slate-500"
                        role="status"
                    >
                        서버 모니터링 이력을 불러오는 중…
                    </p>
                ) : errorMessage ? (
                    <div className="rounded-xl border border-red-200 bg-red-50 px-5 py-5">
                        <p className="text-sm text-red-800">
                            {errorMessage}
                        </p>

                        <button
                            className="mt-4 rounded-lg bg-red-900 px-4 py-2 text-sm font-semibold text-white"
                            onClick={() => void loadHistory()}
                            type="button"
                        >
                            다시 시도
                        </button>
                    </div>
                ) : history.length === 0 ? (
                    <p className="rounded-xl border border-slate-200 bg-white px-5 py-8 text-center text-sm text-slate-500">
                        아직 수집된 서버 모니터링 이력이 없습니다.
                    </p>
                ) : (
                    <div className="rounded-xl border border-slate-200 bg-white p-5">
                        <MonitoringHistoryChart
                            history={history}
                        />
                    </div>
                )}
            </div>
        </section>
    )
}

export default MonitoringHistoryPanel