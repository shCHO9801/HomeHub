import type {SubmitEvent} from 'react'
import {useState} from 'react'

type LoginPageProps = {
    onLogin: (
        username: string,
        password: string,
    ) => Promise<void>
}

function LoginPage({onLogin}: LoginPageProps) {
    const [username, setUsername] = useState('')
    const [password, setPassword] = useState('')
    const [errorMessage, setErrorMessage] = useState<string | null>(null)
    const [isSubmitting, setIsSubmitting] = useState(false)

    const handleSubmit = async (
        event: SubmitEvent<HTMLFormElement>,
    ) => {
        event.preventDefault()
        setErrorMessage(null)
        setIsSubmitting(true)

        try {
            await onLogin(username, password)
        } catch (error) {
            setPassword('')
            setErrorMessage(
                error instanceof Error
                    ? error.message
                    : '로그인에 실패했습니다.',
            )
        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <main className="grid min-h-screen place-items-center bg-slate-100 px-6 py-12 text-slate-950">
            <section className="w-full max-w-md overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
                <header className="border-b border-slate-200 px-8 py-7">
                    <div
                        className="flex items-center gap-2 font-mono text-xs tracking-[0.16em] text-slate-500 uppercase">
            <span
                aria-hidden="true"
                className="size-2 rounded-full bg-teal-500"
            />
                        Home node · Authentication
                    </div>

                    <h1 className="mt-4 text-3xl font-semibold tracking-tight">
                        My Home Hub
                    </h1>

                    <p className="mt-2 text-sm leading-6 text-slate-600">
                        홈 인프라 관리 화면에 접속합니다.
                    </p>
                </header>

                <form
                    className="space-y-5 px-8 py-7"
                    onSubmit={handleSubmit}
                >
                    <div>
                        <label
                            className="block text-sm font-medium text-slate-700"
                            htmlFor="username"
                        >
                            사용자명
                        </label>
                        <input
                            autoComplete="username"
                            autoFocus
                            className="mt-2 w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm outline-none transition focus:border-teal-600 focus:ring-3 focus:ring-teal-100"
                            id="username"
                            name="username"
                            onChange={(event) => setUsername(event.target.value)}
                            required
                            type="text"
                            value={username}
                        />
                    </div>

                    <div>
                        <label
                            className="block text-sm font-medium text-slate-700"
                            htmlFor="password"
                        >
                            비밀번호
                        </label>
                        <input
                            autoComplete="current-password"
                            className="mt-2 w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm outline-none transition focus:border-teal-600 focus:ring-3 focus:ring-teal-100"
                            id="password"
                            name="password"
                            onChange={(event) => setPassword(event.target.value)}
                            required
                            type="password"
                            value={password}
                        />
                    </div>

                    {errorMessage && (
                        <p
                            className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
                            role="alert"
                        >
                            {errorMessage}
                        </p>
                    )}

                    <button
                        className="w-full rounded-xl bg-slate-950 px-4 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-600 disabled:cursor-not-allowed disabled:opacity-60"
                        disabled={isSubmitting}
                        type="submit"
                    >
                        {isSubmitting ? '로그인 중…' : '로그인'}
                    </button>
                </form>
            </section>
        </main>
    )
}

export default LoginPage