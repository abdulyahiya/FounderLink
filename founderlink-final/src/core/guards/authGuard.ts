import tokenService from '../tokenService';

// Auth guard utility — used by ProtectedRoute
export const isTokenValid = (token: string | null): boolean => {
  if (!token) return false;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.exp * 1000 > Date.now();
  } catch {
    return false;
  }
};

export const clearAuth = (): void => {
  tokenService.clearAll();
};
