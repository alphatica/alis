import { defineConfig } from 'vite';
import solid from 'vite-plugin-solid';

// Backend started with: java -jar studio.jar --no-browser
const backend = 'http://127.0.0.1:7070';

export default defineConfig({
  plugins: [solid()],
  build: {
    target: 'es2022',
    outDir: 'dist',
    emptyOutDir: true,
    // Minification drops the libraries' license headers; their licenses (MIT etc.) must still ship with the JAR.
    license: { fileName: 'third-party-licenses.md' },
  },
  // `npm run dev`: GUI with hot reload on :5173, API and WebSocket forwarded to the running backend.
  // Origin is rewritten because the backend accepts requests only from its own pages.
  server: {
    proxy: {
      '/api': { target: backend, changeOrigin: true, headers: { origin: backend } },
      '/ws': { target: backend, ws: true, changeOrigin: true, headers: { origin: backend } },
    },
  },
});
