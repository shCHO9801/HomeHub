import ResourceUsageCard from './ResourceUsageCard'
import {formatCollectedAt, formatUptime,} from './format'
import type {ServerStatusResponse} from './api'

type ServerMonitoringPanelProps = {
    serverStatus: ServerStatusResponse | null
    isLoading: boolean
    isRefreshing: boolean
    errorMessage: string | null
    onRefresh: () => void
}

function ServerMonitoringPanel({
                                   serverStatus,
                                   isLoading,
                                   isRefreshing,
                                   errorMessage,
                                   onRefresh,
                               }: ServerMonitoringPanelProps) {
    if (isLoading) {
        return (
            <section
                className="rounded-2xl border border-slate-200 bg-white p-6"
                role="status"
            >
                서버 상태를 불러오는 중…
            </section>
        )
    }

    if (!serverStatus) {
        return (
            <section className="rounded-2xl border border-red-200 bg-red-50 p-6">
                <p className="text-sm text-red-800">
                    {errorMessage ??
                        '서버 상태를 불러오지 못했습니다.'}
                </p>
                <button
                    className="mt-4 rounded-lg bg-red-900 px-4 py-2 text-sm font-semibold text-white"
                    onClick={onRefresh}
                    type="button"
                >
                    다시 시도
                </button>
            </section>
        )
    }

    const isOnline = serverStatus.status === 'ONLINE'

    return (
        <section className="rounded-2xl border border-slate-200 bg-slate-50 p-6">
            <header className="flex flex-wrap items-start justify-between gap-4">
                <div>
                    <div className="flex items-center gap-2">
            <span
                aria-hidden="true"
                className={`size-2.5 rounded-full ${
                    isOnline
                        ? 'bg-emerald-500'
                        : 'bg-amber-500'
                }`}
            />
                        <p className="font-mono text-xs tracking-[0.14em] text-slate-500 uppercase">
                            Mini PC Server
                        </p>
                    </div>

                    <h2 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">
                        {isOnline
                            ? '서버 온라인'
                            : '상태 확인 불가'}
                    </h2>

                    {errorMessage && (
                        <p className="mt-2 text-sm text-amber-700">
                            {errorMessage}
                        </p>
                    )}
                </div>

                <button
                    className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100 disabled:opacity-60"
                    disabled={isRefreshing}
                    onClick={onRefresh}
                    type="button"
                >
                    {isRefreshing ? '갱신 중…' : '새로고침'}
                </button>
            </header>

            {isOnline ? (
                <>
                    <div className="mt-6 grid gap-4 md:grid-cols-3">
                        <article className="rounded-xl border border-slate-200 bg-white p-5">
                            <p className="text-xs font-semibold tracking-[0.12em] text-slate-500 uppercase">
                                CPU
                            </p>
                            <p className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">
                                {serverStatus.cpuUsagePercent?.toFixed(1) ??
                                    '—'}
                                %
                            </p>
                            <p className="mt-4 text-xs text-slate-500">
                                논리 프로세서{' '}
                                {serverStatus.logicalProcessorCount ?? '—'}개
                            </p>
                        </article>

                        {serverStatus.memory && (
                            <ResourceUsageCard
                                label="Memory"
                                usage={serverStatus.memory}
                            />
                        )}

                        {serverStatus.disk && (
                            <ResourceUsageCard
                                label="Disk"
                                usage={serverStatus.disk}
                            />
                        )}
                    </div>

                    <div
                        className="mt-4 flex flex-wrap justify-between gap-3 rounded-xl border border-slate-200 bg-white px-5 py-4 text-sm">
                        <p className="text-slate-600">
                            가동 시간{' '}
                            <span className="font-medium text-slate-950">
                {serverStatus.uptimeSeconds === null
                    ? '—'
                    : formatUptime(
                        serverStatus.uptimeSeconds,
                    )}
              </span>
                        </p>

                        <p className="text-slate-500">
                            {formatCollectedAt(serverStatus.collectedAt)}
                        </p>
                    </div>
                </>
            ) : (
                <p className="mt-6 rounded-xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm text-amber-900">
                    서버 자원 정보를 수집하지 못했습니다.
                </p>
            )}
        </section>
    )
}

export default ServerMonitoringPanel