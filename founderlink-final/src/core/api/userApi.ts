import axios, { AxiosResponse } from 'axios';
import api from './axiosConfig';
import { UserProfile, ProfileFormData, AuthUser } from '../../types';

const authLookupApi = axios.create({
  baseURL:
    import.meta.env.VITE_AUTH_API_BASE_URL ||
    import.meta.env.VITE_API_BASE_URL ||
    'https://founderlink-c6qc.onrender.com',
  withCredentials: true,
});

export const getMyProfile = (userId: number): Promise<AxiosResponse<UserProfile>> =>
  api.get(`/users/${userId}`);

export const updateProfile = (userId: number, data: ProfileFormData): Promise<AxiosResponse<UserProfile>> =>
  api.put(`/users/${userId}`, data);

export const getUserById = (id: number): Promise<AxiosResponse<UserProfile>> =>
  api.get(`/users/${id}`);

export const searchUsersBySkill = (skill: string): Promise<AxiosResponse<UserProfile[]>> =>
  api.get(`/users/search?skill=${encodeURIComponent(skill)}`);

export const getCoFounderIds = (): Promise<AxiosResponse<AuthUser[]>> =>
  authLookupApi.get('/auth/users/by-role?role=ROLE_COFOUNDER');

export const getUsersByRole = (role: string): Promise<AxiosResponse<AuthUser[]>> =>
  authLookupApi.get(`/auth/users/by-role?role=${role}`);

export const getAuthUserById = (id: number): Promise<AxiosResponse<AuthUser>> =>
  authLookupApi.get(`/auth/users/${id}`);

export const getProfilesBatch = (userIds: number[], skill?: string): Promise<AxiosResponse<UserProfile[]>> => {
  const params = new URLSearchParams({ userIds: userIds.join(',') });
  if (skill) params.append('skill', skill);
  return api.get(`/users/profiles/batch?${params}`);
};
