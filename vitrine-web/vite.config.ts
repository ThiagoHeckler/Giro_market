/// <reference types="vitest/config" />
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Em dev o Vite faz proxy para o mercado: mesma origem no navegador, sem CORS.
    proxy: {
      "/api": {
        target: process.env.MERCADO_URL ?? "http://localhost:8080",
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
