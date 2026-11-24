import {useEffect, useState} from 'react'
import LoginPage from './auth/LoginPage'
import type {CurrentUser} from './auth/api'
import {getCurrentUser, login, logout} from './auth/api'
import DashboardApp from './DashboardApp'

function App() {
    const [currentUser, setCurrentUser] =
        useState<CurrentUser | null | undefined>(undefined)
    useEffect(() => {
        let active = true

        const checkAuthentication = async () => {
            try {
                const user = await getCurrentUser()

                if (active) {
                    setCurrentUser(user)
                }
            } catch {
                if (active) {
                    setCurrentUser(null)
                }
            }
        }

        void checkAuthentication()

        return () => {
            active = false
        }
    }, [])

    const handleLogin = async (
        username: string,
        password: string,
    ) => {
        await login(username, password)

        const user = await getCurrentUser()

        if (!user) {
            throw new Error(
                '로그인 사용자 정보를 확인하지 못했습니다.',
            )
        }

        setCurrentUser(user)
    }

    const handleLogout = async () => {
        await logout()
        setCurrentUser(null)
    }

    if (currentUser === undefined) {
        return (
            <main className="grid min-h-screen place-items-center bg-slate-100 text-slate-700">
                <p role="status">로그인 상태 확인 중…</p>
            </main>
        )
    }

    if (currentUser === null) {
        return <LoginPage onLogin={handleLogin}/>
    }

    return (
        <DashboardApp
            currentUser={currentUser}
            onLogout={handleLogout}
        />
    )
}

export default App
