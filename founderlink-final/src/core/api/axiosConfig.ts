import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import tokenService from '../tokenService';
import type { ApiResponse, AuthResponse } from '../../types';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'https://founderlink-c6qc.onrender.com',
});

let isRefreshing = false;
let failedQueue: Array<{ resolve: (_token: string) => void; reject: (_error: unknown) => void }> =
  [];

const isApiEnvelope = <T>(payload: unknown): payload is ApiResponse<T> => {
  return !!payload && typeof payload === 'object' && 'data' in payload && 'message' in payload;
};

const redirectToLogin = () => {
  tokenService.clearAll();
  window.location.href = '/login';
};

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) prom.reject(error);
    else prom.resolve(token!);
  });
  failedQueue = [];
};

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenService.getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (response) => {
    if (isApiEnvelope(response.data)) {
      response.data = response.data.data;
    }
    return response;
  },
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
    const url = originalRequest?.url || '';
    const isAuthEndpoint = ['/auth/login', '/auth/register', '/auth/refresh'].some((ep) =>
      url.includes(ep)
    );

    if (error.response?.status === 401 && !isAuthEndpoint && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers['Authorization'] = `Bearer ${token}`;
            return api(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const currentToken = tokenService.getToken();
      const refreshToken = tokenService.getRefreshToken();
      if (!currentToken || !refreshToken) {
        redirectToLogin();
        return Promise.reject(error);
      }

      try {
        const authBase =
          import.meta.env.VITE_AUTH_API_BASE_URL ||
          import.meta.env.VITE_API_BASE_URL ||
          'https://founderlink-c6qc.onrender.com';
        const response = await axios.post<AuthResponse>(
          `${authBase}/auth/refresh`,
          { refreshToken }
        );
        const newToken = response.data.token;
        tokenService.setToken(newToken);
        tokenService.setRefreshToken(response.data.refreshToken || refreshToken);
        api.defaults.headers.common['Authorization'] = `Bearer ${newToken}`;
        processQueue(null, newToken);
        originalRequest.headers = originalRequest.headers || {};
        originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        redirectToLogin();
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default api;
