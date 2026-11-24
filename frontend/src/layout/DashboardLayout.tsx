import { NavLink, Outlet } from 'react-router'
import type { CurrentUser } from '../auth/api'

type DashboardLayoutProps = {
    currentUser: CurrentUser
    onLogout: () => Promise<void>
}

const navigationItems = [
    { path: '/', label: '홈', end: true },
    { path: '/docker', label: 'Docker', end: false },
    { path: '/ports', label: '포트', end: false },
    { path: '/nas', label: 'NAS', end: false },
]

function DashboardLayout({
                             currentUser,
                             onLogout,
                         }: DashboardLayoutProps) {
    const navigationClassName = ({
                                     isActive,
                                 }: {
        isActive: boolean
    }) =>
        [
            'rounded-lg px-4 py-3 text-sm font-medium transition',
            isActive
                ? 'bg-slate-950 text-white'
                : 'text-slate-600 hover:bg-slate-100 hover:text-slate-950',
        ].join(' ')

    return (
        <div className="min-h-screen bg-slate-100 text-slate-950">
            <header className="sticky top-0 z-10 border-b border-slate-200 bg-white md:hidden">
                <div className="flex items-center justify-between gap-4 px-6 py-4">
                    <div>
                        <p className="text-sm font-semibold">
                            My Home Hub
                        </p>
                        <p className="mt-1 font-mono text-xs text-slate-500">
                            {currentUser.username}
                        </p>
                    </div>

                    <button
                        className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700"
                        onClick={() => void onLogout()}
                        type="button"
                    >
                        로그아웃
                    </button>
                </div>
            </header>

            <div className="mx-auto flex w-full max-w-7xl md:min-h-screen">
                <aside className="hidden w-64 shrink-0 border-r border-slate-200 bg-white p-6 md:block">
                    <p className="font-mono text-xs tracking-[0.16em] text-slate-500 uppercase">
                        Local network
                    </p>

                    <h1 className="mt-3 text-2xl font-semibold tracking-tight">
                        My Home Hub
                    </h1>

                    <nav className="mt-8 flex flex-col gap-2">
                        {navigationItems.map((item) => (
                            <NavLink
                                className={navigationClassName}
                                end={item.end}
                                key={item.path}
                                to={item.path}
                            >
                                {item.label}
                            </NavLink>
                        ))}
                    </nav>

                    <div className="mt-8 border-t border-slate-200 pt-5">
                        <p className="text-sm font-medium">
                            {currentUser.username}
                        </p>
                        <p className="mt-1 font-mono text-xs text-slate-500">
                            {currentUser.role}
                        </p>

                        <button
                            className="mt-4 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
                            onClick={() => void onLogout()}
                            type="button"
                        >
                            로그아웃
                        </button>
                    </div>
                </aside>

                <main className="min-w-0 flex-1 px-6 py-8 pb-24 md:px-10 md:py-10">
                    <Outlet />
                </main>
            </div>

            <nav className="fixed inset-x-0 bottom-0 grid grid-cols-4 border-t border-slate-200 bg-white p-2 md:hidden">
                {navigationItems.map((item) => (
                    <NavLink
                        className={navigationClassName}
                        end={item.end}
                        key={item.path}
                        to={item.path}
                    >
            <span className="block text-center">
              {item.label}
            </span>
                    </NavLink>
                ))}
            </nav>
        </div>
    )
}

export default DashboardLayout