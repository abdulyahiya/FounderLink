import axios, { AxiosResponse } from 'axios';
import api from './axiosConfig';
import { LoginFormData, RegisterFormData, AuthResponse, ApiResponse, ForgotPasswordData, ResetPasswordData, ChangePasswordData } from '../../types';

const authApi = axios.create({
  baseURL:
    import.meta.env.VITE_AUTH_API_BASE_URL ||
    import.meta.env.VITE_API_BASE_URL ||
    'https://founderlink-c6qc.onrender.com',
  withCredentials: true,
});

authApi.interceptors.response.use((response) => {
  const payload = response.data as ApiResponse<unknown> | unknown;
  if (payload && typeof payload === 'object' && 'data' in (payload as Record<string, unknown>)) {
    response.data = (payload as ApiResponse<unknown>).data;
  }
  return response;
});

export const login = (data: LoginFormData): Promise<AxiosResponse<AuthResponse>> =>
  authApi.post('/auth/login', data);

export const register = (data: RegisterFormData): Promise<AxiosResponse<void>> =>
  authApi.post('/auth/register', data);

export const refreshToken = (data: { refreshToken: string }): Promise<AxiosResponse<AuthResponse>> =>
  authApi.post('/auth/refresh', data);

export const forgotPassword = (data: ForgotPasswordData): Promise<AxiosResponse<void>> =>
  authApi.post('/auth/forgot-password', data);

export const resetPassword = (data: ResetPasswordData): Promise<AxiosResponse<void>> =>
  authApi.post('/auth/reset-password', data);

export const changePassword = (data: ChangePasswordData): Promise<AxiosResponse<void>> =>
  api.post('/auth/change-password', data);
