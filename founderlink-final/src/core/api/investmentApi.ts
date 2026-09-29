import { AxiosResponse } from 'axios';
import api from './axiosConfig';
import { Investment, InvestmentFormData } from '../../types';

export const createInvestment = (data: InvestmentFormData & { investorId: number; startupId: number }): Promise<AxiosResponse<Investment>> =>
  api.post('/investments', data);

export const getInvestmentsByStartup = (startupId: number): Promise<AxiosResponse<Investment[]>> =>
  api.get(`/investments/startup/${startupId}`);

export const getMyInvestments = (investorId: number): Promise<AxiosResponse<Investment[]>> =>
  api.get(`/investments/investor/${investorId}`);

export const approveInvestment = (id: number): Promise<AxiosResponse<Investment>> =>
  api.put(`/investments/${id}/approve`);

export const rejectInvestment = (id: number): Promise<AxiosResponse<Investment>> =>
  api.put(`/investments/${id}/reject`);
