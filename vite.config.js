import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host: true, // Exposes on 0.0.0.0 for LAN access from mobile phones
    port: 5173,
  },
})
