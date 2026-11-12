export type UserRole = 'ADMIN' | 'MEMBER'

export type CurrentUser = {
    id: number
    username: string
    role: UserRole
}

type CsrfTokenResponse = {
    headerName: string
    parameterName: string
    token: string
}

type ApiErrorResponse = {
    status: number
    message: string
    code: string
    field: string | null
}

const requestOptions: RequestInit = {
    credentials: 'include',
}

export async function getCurrentUser(): Promise<CurrentUser | null> {
    const response = await fetch('/api/auth/me', requestOptions)

    if (response.status === 401) {
        return null
    }

    if (!response.ok) {
        throw new Error(
            await getErrorMessage(response, '사용자 정보를 불러오지 못했습니다.'),
        )
    }

    return (await response.json()) as CurrentUser
}

export async function login(
    username: string,
    password: string,
): Promise<void> {
    const csrf = await getCsrfToken()

    const response = await fetch('/api/auth/login', {
        ...requestOptions,
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            [csrf.headerName]: csrf.token,
        },
        body: new URLSearchParams({
            username,
            password,
        }),
    })

    if (!response.ok) {
        throw new Error(
            await getErrorMessage(
                response,
                '로그인에 실패했습니다.',
            ),
        )
    }
}

export async function logout(): Promise<void> {
    const csrf = await getCsrfToken()

    const response = await fetch('/api/auth/logout', {
        ...requestOptions,
        method: 'POST',
        headers: {
            [csrf.headerName]: csrf.token,
        },
    })

    if (!response.ok) {
        throw new Error(
            await getErrorMessage(
                response,
                '로그아웃에 실패했습니다.',
            ),
        )
    }
}

async function getCsrfToken(): Promise<CsrfTokenResponse> {
    const response = await fetch('/api/auth/csrf', requestOptions)

    if (!response.ok) {
        throw new Error(
            await getErrorMessage(
                response,
                '보안 토큰을 불러오지 못했습니다.',
            ),
        )
    }

    return (await response.json()) as CsrfTokenResponse
}

async function getErrorMessage(
    response: Response,
    fallbackMessage: string,
): Promise<string> {
    try {
        const error = (await response.json()) as ApiErrorResponse

        return error.message || fallbackMessage
    } catch {
        return fallbackMessage
    }
}