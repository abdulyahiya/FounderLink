import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  // sockjs-client (and some STOMP polyfills) reference Node's `global`.
  // Vite runs in a browser context where `global` doesn't exist — this shim
  // maps it to the standard `globalThis` so the bundle doesn't crash.
  define: {
    global: 'globalThis',
  },
  server: {
    port: 3002,
  },
});
