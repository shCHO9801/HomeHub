import {useCallback, useEffect, useState} from 'react'
import {getSynologyStorageStatus, type SynologyStorageHealth, type SynologyStorageResponse,} from './api'
import {formatBytes, formatCollectedAt,} from '../monitoring/format'

const healthLabels: Record<SynologyStorageHealth, string> = {
    HEALTHY: '정상',
    WARNING: '주의',
    CRITICAL: '위험',
    UNKNOWN: '확인 불가',
}

const healthClassNames: Record<SynologyStorageHealth, string> = {
    HEALTHY: 'bg-emerald-50 text-emerald-700',
    WARNING: 'bg-amber-50 text-amber-700',
    CRITICAL: 'bg-red-50 text-red-700',
    UNKNOWN: 'bg-slate-100 text-slate-600',
}

function NasStoragePage() {
    const [storage, setStorage] =
        useState<SynologyStorageResponse | null>(null)
    const [isLoading, setIsLoading] = useState(true)
    const [isRefreshing, setIsRefreshing] = useState(false)
    const [errorMessage, setErrorMessage] =
        useState<string | null>(null)

    const loadStorage = useCallback(
        async (refreshing = false) => {
            if (refreshing) {
                setIsRefreshing(true)
            } else {
                setIsLoading(true)
            }

            try {
                const response =
                    await getSynologyStorageStatus()

                setStorage(response)
                setErrorMessage(null)
            } catch (error) {
                setErrorMessage(
                    error instanceof Error
                        ? error.message
                        : 'NAS 저장공간 정보를 불러오지 못했습니다.',
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
            void loadStorage()
        }, 0)

        return () => {
            window.clearTimeout(initialLoadId)
        }
    }, [loadStorage])

    if (isLoading) {
        return (
            <section
                className="rounded-2xl border border-slate-200 bg-white p-6"
                role="status"
            >
                NAS 저장공간 정보를 불러오는 중…
            </section>
        )
    }

    if (!storage) {
        return (
            <section className="rounded-2xl border border-red-200 bg-red-50 p-6">
                <p className="text-sm text-red-800">
                    {errorMessage ??
                        'NAS 저장공간 정보를 불러오지 못했습니다.'}
                </p>

                <button
                    className="mt-4 rounded-lg bg-red-900 px-4 py-2 text-sm font-semibold text-white"
                    onClick={() => void loadStorage()}
                    type="button"
                >
                    다시 시도
                </button>
            </section>
        )
    }

    const isAvailable = storage.status === 'AVAILABLE'

    return (
        <section className="rounded-2xl border border-slate-200 bg-slate-50 p-6">
            <header className="flex flex-wrap items-start justify-between gap-4">
                <div>
                    <div className="flex items-center gap-2">
                        <span
                            aria-hidden="true"
                            className={`size-2.5 rounded-full ${
                                isAvailable
                                    ? 'bg-emerald-500'
                                    : 'bg-amber-500'
                            }`}
                        />

                        <p className="font-mono text-xs tracking-[0.14em] text-slate-500 uppercase">
                            Synology NAS
                        </p>
                    </div>

                    <h2 className="mt-2 text-2xl font-semibold tracking-tight">
                        {isAvailable
                            ? '저장공간 정상'
                            : '상태 확인 불가'}
                    </h2>

                    {storage.stale && (
                        <p className="mt-2 text-sm text-amber-700">
                            현재 수집에 실패해 마지막 정상 정보를 표시합니다.
                        </p>
                    )}
                </div>

                <button
                    className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100 disabled:opacity-60"
                    disabled={isRefreshing}
                    onClick={() => void loadStorage(true)}
                    type="button"
                >
                    {isRefreshing ? '갱신 중…' : '새로고침'}
                </button>
            </header>

            {storage.volumes.length > 0 ? (
                <div className="mt-6 grid gap-4 lg:grid-cols-2">
                    {storage.volumes.map((volume) => {
                        const percent = Math.min(
                            Math.max(volume.usagePercent, 0),
                            100,
                        )

                        return (
                            <article
                                className="rounded-xl border border-slate-200 bg-white p-5"
                                key={volume.path}
                            >
                                <div className="flex items-end justify-between gap-4">
                                    <div>
                                        <p className="text-xs font-semibold tracking-[0.12em] text-slate-500 uppercase">
                                            {volume.name}
                                        </p>

                                        <p className="mt-2 text-3xl font-semibold tracking-tight">
                                            {volume.usagePercent.toFixed(1)}%
                                        </p>
                                    </div>

                                    <p className="text-right text-xs leading-5 text-slate-500">
                                        {formatBytes(volume.usedBytes)}
                                        <br/>
                                        / {formatBytes(volume.totalBytes)}
                                    </p>
                                </div>

                                <div
                                    aria-label={`${volume.name} 사용률`}
                                    aria-valuemax={100}
                                    aria-valuemin={0}
                                    aria-valuenow={percent}
                                    className="mt-5 h-2 overflow-hidden rounded-full bg-slate-100"
                                    role="progressbar"
                                >
                                    <div
                                        className="h-full rounded-full bg-teal-500"
                                        style={{
                                            width: `${percent}%`,
                                        }}
                                    />
                                </div>

                                <div className="mt-4 flex justify-between gap-4 text-xs text-slate-500">
                                    <span>{volume.path}</span>
                                    <span>
                                        여유 공간{' '}
                                        {formatBytes(
                                            volume.availableBytes,
                                        )}
                                    </span>
                                </div>
                            </article>
                        )
                    })}
                </div>
            ) : (
                <p className="mt-6 rounded-xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm text-amber-900">
                    NAS 저장공간 정보를 수집하지 못했습니다.
                </p>
            )}

            {storage.storageItems.length > 0 && (
                <div className="mt-4 rounded-xl border border-slate-200 bg-white">
                    <div className="border-b border-slate-200 px-5 py-4">
                        <h3 className="font-semibold">
                            저장소 건강 상태
                        </h3>
                    </div>

                    <ul className="divide-y divide-slate-100">
                        {storage.storageItems.map((item) => (
                            <li
                                className="flex items-center justify-between gap-4 px-5 py-4"
                                key={`${item.type}-${item.name}`}
                            >
                                <div>
                                    <p className="text-sm font-medium">
                                        {item.name}
                                    </p>
                                    <p className="mt-1 font-mono text-xs text-slate-500">
                                        {item.type}
                                    </p>
                                </div>

                                <span
                                    className={`rounded-full px-3 py-1 text-xs font-semibold ${
                                        healthClassNames[item.health]
                                    }`}
                                >
                                    {healthLabels[item.health]}
                                </span>
                            </li>
                        ))}
                    </ul>
                </div>
            )}

            {storage.collectedAt && (
                <p className="mt-4 text-right text-xs text-slate-500">
                    수집 시각{' '}
                    {formatCollectedAt(storage.collectedAt)}
                </p>
            )}
        </section>
    )
}

export default NasStoragePage
