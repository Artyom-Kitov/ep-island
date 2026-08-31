import {defineConfig} from 'vite';
import react from '@vitejs/plugin-react';
import {resolve} from 'node:path';

export default defineConfig({
    plugins: [react()],
    base: '/react/',
    build: {
        outDir: resolve(import.meta.dirname, '../src/main/resources/static/react'),
        emptyOutDir: true,
        rollupOptions: {
            output: {
                entryFileNames: 'app.js',
                chunkFileNames: 'chunks/[name]-[hash].js',
                assetFileNames: 'assets/[name]-[hash][extname]'
            }
        }
    },
    server: {
        port: 5173,
        proxy: {
            '/api': 'http://127.0.0.1:25074',
            '/styles.css': 'http://127.0.0.1:25074',
            '/favicon.svg': 'http://127.0.0.1:25074'
        }
    }
});
