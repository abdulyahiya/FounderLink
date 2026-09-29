import { AxiosResponse } from 'axios';
import api from './axiosConfig';
import { Startup, StartupFormData, PaginatedStartups } from '../../types';

export const createStartup = (data: StartupFormData): Promise<AxiosResponse<Startup>> =>
  api.post('/startups', data);

export const getAllStartups = (page: number = 0, size: number = 10): Promise<AxiosResponse<PaginatedStartups>> =>
  api.get(`/startups?page=${page}&size=${size}`);

export const getStartupById = (id: number): Promise<AxiosResponse<Startup>> =>
  api.get(`/startups/${id}`);

export const updateStartup = (id: number, data: StartupFormData): Promise<AxiosResponse<Startup>> =>
  api.put(`/startups/${id}`, data);

export const deleteStartup = (id: number): Promise<AxiosResponse<void>> =>
  api.delete(`/startups/${id}`);

export const approveStartup = (id: number): Promise<AxiosResponse<Startup>> =>
  api.put(`/startups/${id}/approve`);

export const rejectStartup = (id: number): Promise<AxiosResponse<Startup>> =>
  api.put(`/startups/${id}/reject`);

export const followStartup = (id: number): Promise<AxiosResponse<void>> =>
  api.post(`/startups/${id}/follow`);

export const getStartupsByFounder = (founderId: number): Promise<AxiosResponse<Startup[]>> =>
  api.get(`/startups/founder/${founderId}`);

export const getAllStartupsAdmin = (): Promise<AxiosResponse<PaginatedStartups>> =>
  api.get('/startups/admin/all?page=0&size=100');
