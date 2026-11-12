import {formatBytes} from './format'
import type {ResourceUsage} from './api'

type ResourceUsageCardProps = {
    label: string
    usage: ResourceUsage
}

function ResourceUsageCard({
                               label,
                               usage,
                           }: ResourceUsageCardProps) {
    const normalizedPercent = Math.min(
        Math.max(usage.usagePercent, 0),
        100,
    )

    return (
        <article className="rounded-xl border border-slate-200 bg-white p-5">
            <div className="flex items-end justify-between gap-4">
                <div>
                    <p className="text-xs font-semibold tracking-[0.12em] text-slate-500 uppercase">
                        {label}
                    </p>
                    <p className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">
                        {usage.usagePercent.toFixed(1)}%
                    </p>
                </div>

                <p className="text-right text-xs leading-5 text-slate-500">
                    {formatBytes(usage.usedBytes)}
                    <br/>
                    / {formatBytes(usage.totalBytes)}
                </p>
            </div>

            <div
                aria-label={`${label} 사용률`}
                aria-valuemax={100}
                aria-valuemin={0}
                aria-valuenow={normalizedPercent}
                className="mt-4 h-2 overflow-hidden rounded-full bg-slate-100"
                role="progressbar"
            >
                <div
                    className="h-full rounded-full bg-teal-500 transition-[width]"
                    style={{width: `${normalizedPercent}%`}}
                />
            </div>
        </article>
    )
}

export default ResourceUsageCard