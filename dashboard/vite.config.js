import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The services listen on 127.0.0.1 only and have no CORS setup, so the dev server forwards to them.
// shortcut: dev server only; serve the built files behind the same kind of proxy before deploying anywhere.
const to = (port, prefix) => ({
  target: `http://127.0.0.1:${port}`,
  rewrite: (path) => path.replace(prefix, ''),
})

export default defineConfig({
  plugins: [react()],
  server: {
    host: '127.0.0.1',
    port: 5173,
    proxy: {
      '/bank': to(8080, /^\/bank/),
      '/rules': to(8082, /^\/rules/),
      '/graph': to(8083, /^\/graph/),
    },
  },
})
