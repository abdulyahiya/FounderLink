 import { User } from '../types';

// ─── Token Storage Mode ───────────────────────────────────────────────────────
//
//  COOKIE_MODE = false  →  current mode: token stored in localStorage
//                          (works with your existing backend as-is)
//
//  COOKIE_MODE = true   →  secure mode: token lives in an httpOnly cookie
//                          set by the server — JS never touches it
//
//  TO SWITCH: set COOKIE_MODE = true  AND  update your backend to:
//    1. On POST /auth/login  → Set-Cookie: token=<jwt>; HttpOnly; Secure; SameSite=Strict
//    2. On POST /auth/refresh → Set-Cookie: token=<new_jwt>; HttpOnly; Secure; SameSite=Strict
//    3. On POST /auth/logout  → Set-Cookie: token=; Max-Age=0; HttpOnly
//    axios withCredentials: true (already set in axiosConfig) handles the rest.
//
const COOKIE_MODE = false;

const TOKEN_KEY = 'token';
const REFRESH_TOKEN_KEY = 'refreshToken';
const USER_KEY = 'user';

const tokenService = {
  // ── Token ──────────────────────────────────────────────────────────────────

  getToken(): string | null {
    if (COOKIE_MODE) return null; // browser sends httpOnly cookie automatically
    return localStorage.getItem(TOKEN_KEY);
  },

  setToken(token: string): void {
    if (COOKIE_MODE) return; // backend sets the httpOnly cookie in Set-Cookie header
    localStorage.setItem(TOKEN_KEY, token);
  },

  removeToken(): void {
    if (COOKIE_MODE) return; // backend clears cookie via logout endpoint
    localStorage.removeItem(TOKEN_KEY);
  },

  getRefreshToken(): string | null {
    if (COOKIE_MODE) return null;
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  },

  setRefreshToken(token: string): void {
    if (COOKIE_MODE) return;
    localStorage.setItem(REFRESH_TOKEN_KEY, token);
  },

  removeRefreshToken(): void {
    if (COOKIE_MODE) return;
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  },

  // ── User metadata (role, email — not sensitive, localStorage in both modes) ─

  getUser(): User | null {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null');
  },

  setUser(user: User): void {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  },

  removeUser(): void {
    localStorage.removeItem(USER_KEY);
  },

  // ── Clear session ──────────────────────────────────────────────────────────

  clearAll(): void {
    if (!COOKIE_MODE) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(REFRESH_TOKEN_KEY);
    }
    localStorage.removeItem(USER_KEY);
    // In COOKIE_MODE the backend must expose POST /auth/logout to clear the httpOnly cookie
  },
};

export default tokenService;
