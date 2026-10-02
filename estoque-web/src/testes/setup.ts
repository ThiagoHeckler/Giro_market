import "@testing-library/jest-dom/vitest";
import { COOKIE_CSRF } from "../api/estoque";

afterEach(() => {
  window.localStorage.clear();
  document.cookie = `${COOKIE_CSRF}=; max-age=0`;
  vi.unstubAllGlobals();
});
