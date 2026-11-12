import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import {loadEnv} from 'vite'
import {defineConfig} from 'vitest/config'

export default defineConfig(({mode}) => {
    const env = loadEnv(mode, process.cwd(), '')

    return {
        plugins: [react(), tailwindcss()],
        server: {
            proxy: {
                '/actuator': {
                    target: env.BACKEND_URL || 'http://localhost:8080',
                    changeOrigin: true,
                },
                '/api': {
                    target: env.BACKEND_URL || 'http://localhost:8080',
                    changeOrigin: true,
                },
            },
        },
        test: {
            environment: 'jsdom',
            setupFiles: './src/test/setup.ts',
        },
    }
})
