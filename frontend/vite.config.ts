import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const rootDir = fileURLToPath(new URL('.', import.meta.url));

export default defineConfig({
  plugins: [react()],
  build: {
    rollupOptions: {
      input: {
        h5: path.resolve(rootDir, 'h5.html'),
        buyer: path.resolve(rootDir, 'buyer.html'),
        admin: path.resolve(rootDir, 'admin.html'),
        data: path.resolve(rootDir, 'data.html')
      }
    }
  }
});
