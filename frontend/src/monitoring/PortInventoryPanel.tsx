import {useMemo, useState} from 'react'
import type {
    PortExposure,
    PortStatusResponse,
} from './api'

type PortInventoryPanelProps = {
    ports: PortStatusResponse[]
    isLoading: boolean
    errorMessage: string | null
}

type ExposureFilter = 'ALL' | PortExposure

const EXPOSURE_LABELS: Record<PortExposure, string> = {
    LOOPBACK: '로컬 전용',
    ALL_INTERFACES: '전체 인터페이스',
    SPECIFIC_INTERFACE: '특정 주소',
}

const EXPOSURE_STYLES: Record<PortExposure, string> = {
    LOOPBACK:
        'bg-emerald-50 text-emerald-700 ring-emerald-200',
    ALL_INTERFACES:
        'bg-amber-50 text-amber-700 ring-amber-200',
    SPECIFIC_INTERFACE:
        'bg-sky-50 text-sky-700 ring-sky-200',
}

const FILTERS: {
    value: ExposureFilter
    label: string
}[] = [
    {value: 'ALL', label: '전체'},
    {value: 'ALL_INTERFACES', label: '전체 인터페이스'},
    {value: 'LOOPBACK', label: '로컬 전용'},
    {value: 'SPECIFIC_INTERFACE', label: '특정 주소'},
]

function PortInventoryPanel({
                                ports,
                                isLoading,
                                errorMessage,
                            }: PortInventoryPanelProps) {
    const [query, setQuery] = useState('')
    const [exposureFilter, setExposureFilter] =
        useState<ExposureFilter>('ALL')

    const filteredPorts = useMemo(() => {
        const normalizedQuery = query
            .trim()
            .toLowerCase()

        return ports.filter((port) => {
            const matchesExposure =
                exposureFilter === 'ALL'
                || port.exposure === exposureFilter

            const matchesQuery =
                normalizedQuery.length === 0
                || String(port.port).includes(normalizedQuery)
                || port.bindAddress
                    .toLowerCase()
                    .includes(normalizedQuery)
                || (port.processName ?? '')
                    .toLowerCase()
                    .includes(normalizedQuery)

            return matchesExposure && matchesQuery
        })
    }, [exposureFilter, ports, query])

    const allInterfacesCount = ports.filter(
        (port) => port.exposure === 'ALL_INTERFACES',
    ).length

    return (
        <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
            <header className="border-b border-slate-200 px-6 py-5">
                <div className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                        <p className="font-mono text-xs tracking-[0.14em] text-slate-500 uppercase">
                            Network listeners
                        </p>
                        <h2 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">
                            사용 중인 포트
                        </h2>
                    </div>

                    {!isLoading && !errorMessage && (
                        <div className="flex gap-2 font-mono text-xs">
                            <span className="rounded-full bg-slate-100 px-3 py-1.5 text-slate-600">
                                전체 {ports.length}
                            </span>
                            <span className="rounded-full bg-amber-50 px-3 py-1.5 text-amber-700">
                                전체 인터페이스 {allInterfacesCount}
                            </span>
                        </div>
                    )}
                </div>

                {!isLoading && !errorMessage && ports.length > 0 && (
                    <div className="mt-5 flex flex-col gap-3">
                        <label>
                            <span className="sr-only">포트 검색</span>
                            <input
                                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm outline-none transition placeholder:text-slate-400 focus:border-teal-600 focus:ring-3 focus:ring-teal-100"
                                onChange={(event) =>
                                    setQuery(event.target.value)
                                }
                                placeholder="포트, 주소 또는 프로세스 검색"
                                type="search"
                                value={query}
                            />
                        </label>

                        <div
                            aria-label="포트 공개 범위"
                            className="flex flex-wrap gap-2"
                            role="group"
                        >
                            {FILTERS.map((filter) => {
                                const selected =
                                    exposureFilter === filter.value

                                return (
                                    <button
                                        aria-pressed={selected}
                                        className={`rounded-lg px-3 py-1.5 text-xs font-medium transition ${
                                            selected
                                                ? 'bg-slate-950 text-white'
                                                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                                        }`}
                                        key={filter.value}
                                        onClick={() =>
                                            setExposureFilter(
                                                filter.value,
                                            )
                                        }
                                        type="button"
                                    >
                                        {filter.label}
                                    </button>
                                )
                            })}
                        </div>
                    </div>
                )}
            </header>

            {isLoading ? (
                <p
                    className="px-6 py-8 text-sm text-slate-600"
                    role="status"
                >
                    포트 정보를 불러오는 중…
                </p>
            ) : errorMessage ? (
                <p
                    className="m-6 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
                    role="alert"
                >
                    {errorMessage}
                </p>
            ) : ports.length === 0 ? (
                <p className="px-6 py-8 text-sm text-slate-600">
                    확인된 대기 포트가 없습니다.
                </p>
            ) : filteredPorts.length === 0 ? (
                <p className="px-6 py-8 text-sm text-slate-600">
                    검색 조건에 맞는 포트가 없습니다.
                </p>
            ) : (
                <div className="overflow-x-auto">
                    <table className="w-full min-w-[720px] text-left">
                        <thead className="bg-slate-50 text-xs tracking-[0.08em] text-slate-500 uppercase">
                        <tr>
                            <th className="px-6 py-3 font-semibold">
                                포트
                            </th>
                            <th className="px-4 py-3 font-semibold">
                                바인딩 주소
                            </th>
                            <th className="px-4 py-3 font-semibold">
                                공개 범위
                            </th>
                            <th className="px-6 py-3 font-semibold">
                                프로세스
                            </th>
                        </tr>
                        </thead>

                        <tbody className="divide-y divide-slate-200">
                        {filteredPorts.map((port, index) => (
                            <tr
                                className="align-middle"
                                key={[
                                    port.protocol,
                                    port.bindAddress,
                                    port.port,
                                    index,
                                ].join('-')}
                            >
                                <td className="px-6 py-4">
                                        <span className="font-mono text-base font-semibold text-slate-950">
                                            {port.port}
                                        </span>
                                    <span className="ml-2 font-mono text-xs text-slate-500">
                                            {port.protocol}
                                        </span>
                                </td>

                                <td className="px-4 py-4 font-mono text-xs text-slate-600">
                                    {port.bindAddress}
                                </td>

                                <td className="px-4 py-4">
                                        <span
                                            className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ring-inset ${
                                                EXPOSURE_STYLES[
                                                    port.exposure
                                                    ]
                                            }`}
                                        >
                                            {
                                                EXPOSURE_LABELS[
                                                    port.exposure
                                                    ]
                                            }
                                        </span>
                                </td>

                                <td className="px-6 py-4 text-sm text-slate-600">
                                    {port.processName
                                        ?? '확인 불가'}
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}
        </section>
    )
}

export default PortInventoryPanel