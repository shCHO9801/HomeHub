import { act, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'

afterEach(() => {
  vi.useRealTimers()
  vi.restoreAllMocks()
})

describe('App', () => {
  it('백엔드 상태를 확인하는 동안 로딩 상태를 표시한다', () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(
      () => new Promise<Response>(() => {}),
    )

    render(<App />)

    expect(
      screen.getByRole('heading', { name: 'My Home Hub' }),
    ).toBeInTheDocument()
    expect(screen.getByText('서버 상태 확인 중')).toBeInTheDocument()
  })

  it('백엔드가 정상일 때 온라인 상태를 표시한다', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      json: async () => ({ status: 'UP' }),
    } as Response)

    render(<App />)

    expect(await screen.findByText('API 온라인')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith(
      '/actuator/health',
      expect.objectContaining({ signal: expect.anything() }),
    )
  })

  it('백엔드가 비정상일 때 연결 실패를 표시한다', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: false,
      json: async () => ({ status: 'DOWN' }),
    } as Response)

    render(<App />)

    expect(await screen.findByText('API 연결 실패')).toBeInTheDocument()
  })

  it('백엔드 요청이 실패할 때 연결 실패를 표시한다', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('Network error'))

    render(<App />)

    expect(await screen.findByText('API 연결 실패')).toBeInTheDocument()
  })

  it('백엔드가 제한 시간 안에 응답하지 않으면 연결 실패를 표시한다', async () => {
    vi.useFakeTimers()
    vi.spyOn(globalThis, 'fetch').mockImplementation((_input, init) => {
      return new Promise<Response>((_resolve, reject) => {
        init?.signal?.addEventListener('abort', () => {
          reject(new DOMException('Aborted', 'AbortError'))
        })
      })
    })

    render(<App />)
    await act(async () => {
      await vi.advanceTimersByTimeAsync(5_000)
    })

    expect(screen.getByText('API 연결 실패')).toBeInTheDocument()
  })
})
