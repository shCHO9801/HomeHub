import type {
    DockerContainerResponse,
    DockerContainerState,
} from './api'

type DockerContainerPanelProps = {
    containers: DockerContainerResponse[]
    isLoading: boolean
    errorMessage: string | null
}

const STATE_LABELS: Record<DockerContainerState, string> = {
    RUNNING: '실행 중',
    STOPPED: '중지됨',
    UNHEALTHY: '비정상',
    UNKNOWN: '확인 불가',
}

const STATE_STYLES: Record<DockerContainerState, string> = {
    RUNNING: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
    STOPPED: 'bg-slate-100 text-slate-600 ring-slate-200',
    UNHEALTHY: 'bg-red-50 text-red-700 ring-red-200',
    UNKNOWN: 'bg-amber-50 text-amber-700 ring-amber-200',
}

function DockerContainerPanel({
                                  containers,
                                  isLoading,
                                  errorMessage,
                              }: DockerContainerPanelProps) {
    const runningCount = containers.filter(
        (container) => container.state === 'RUNNING',
    ).length

    return (
        <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
            <header className="flex flex-wrap items-start justify-between gap-4 border-b border-slate-200 px-6 py-5">
                <div>
                    <p className="font-mono text-xs tracking-[0.14em] text-slate-500 uppercase">
                        Docker workloads
                    </p>
                    <h2 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">
                        컨테이너
                    </h2>
                </div>

                {!isLoading && !errorMessage && (
                    <p className="rounded-full bg-slate-100 px-3 py-1.5 font-mono text-xs text-slate-600">
                        실행 {runningCount} / 전체 {containers.length}
                    </p>
                )}
            </header>

            {isLoading ? (
                <p
                    className="px-6 py-8 text-sm text-slate-600"
                    role="status"
                >
                    컨테이너 정보를 불러오는 중…
                </p>
            ) : errorMessage ? (
                <p
                    className="m-6 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
                    role="alert"
                >
                    {errorMessage}
                </p>
            ) : containers.length === 0 ? (
                <p className="px-6 py-8 text-sm text-slate-600">
                    확인된 Docker 컨테이너가 없습니다.
                </p>
            ) : (
                <div className="overflow-x-auto">
                    <table className="w-full min-w-3xl text-left">
                        <thead className="bg-slate-50 text-xs tracking-[0.08em] text-slate-500 uppercase">
                        <tr>
                            <th className="px-6 py-3 font-semibold">
                                컨테이너
                            </th>
                            <th className="px-4 py-3 font-semibold">
                                상태
                            </th>
                            <th className="px-4 py-3 font-semibold">
                                포트
                            </th>
                            <th className="px-6 py-3 font-semibold">
                                상세 상태
                            </th>
                        </tr>
                        </thead>

                        <tbody className="divide-y divide-slate-200">
                        {containers.map((container) => (
                            <tr
                                className="align-top"
                                key={container.id}
                            >
                                <td className="px-6 py-4">
                                    <p className="font-medium text-slate-950">
                                        {container.name}
                                    </p>
                                    <p className="mt-1 max-w-xs break-all font-mono text-xs text-slate-500">
                                        {container.image}
                                    </p>
                                    {container.composeProject && (
                                        <p className="mt-2 text-xs text-slate-500">
                                            프로젝트{' '}
                                            <span className="font-medium text-slate-700">
                                                    {container.composeProject}
                                                </span>
                                        </p>
                                    )}
                                </td>

                                <td className="px-4 py-4">
                                        <span
                                            className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ring-inset ${
                                                STATE_STYLES[
                                                    container.state
                                                    ]
                                            }`}
                                        >
                                            {
                                                STATE_LABELS[
                                                    container.state
                                                    ]
                                            }
                                        </span>
                                </td>

                                <td className="px-4 py-4">
                                    <p className="max-w-xs whitespace-pre-wrap font-mono text-xs leading-5 text-slate-600">
                                        {container.ports || '—'}
                                    </p>
                                </td>

                                <td className="px-6 py-4 text-sm text-slate-600">
                                    {container.statusDetail}
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

export default DockerContainerPanel
