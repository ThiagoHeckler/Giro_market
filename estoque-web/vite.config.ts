/// <reference types="vitest/config" />
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: Number(process.env.ESTOQUE_WEB_PORTA ?? 15174),
    strictPort: true,
    // Em dev o Vite faz proxy para o estoque: mesma origem no navegador, sem CORS.
    proxy: {
      "/api": {
        target: process.env.ESTOQUE_URL ?? "http://localhost:18081",
        rewrite: (caminho) => caminho.replace(/^\/api/, ""),
      },
    },
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./src/testes/setup.ts"],
    css: false,
    globals: true,
  },
});
