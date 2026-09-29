import axios, { AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import tokenService from '../tokenService';
import {
  Payment,
  CreateOrderRequest,
  CreateOrderResponse,
  VerifyPaymentRequest,
  VerifyPaymentResponse,
  PaymentSaga,
} from '../../types';

const paymentApi = axios.create({
  baseURL:
    import.meta.env.VITE_PAYMENT_API_BASE_URL ||
    import.meta.env.VITE_API_BASE_URL ||
    'https://founderlink-c6qc.onrender.com',
});

paymentApi.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenService.getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export const createOrder = (data: CreateOrderRequest): Promise<AxiosResponse<CreateOrderResponse>> =>
  paymentApi.post('/api/payments/create-order', data);

export const verifyPayment = (data: VerifyPaymentRequest): Promise<AxiosResponse<VerifyPaymentResponse>> =>
  paymentApi.post('/api/payments/verify', data);

export const acceptPayment = (paymentId: number): Promise<AxiosResponse<Payment>> =>
  paymentApi.put(`/api/payments/${paymentId}/accept`);

export const rejectPayment = (paymentId: number): Promise<AxiosResponse<Payment>> =>
  paymentApi.put(`/api/payments/${paymentId}/reject`);

export const getPaymentsByInvestor = (investorId: number): Promise<AxiosResponse<Payment[]>> =>
  paymentApi.get(`/api/payments/investor/${investorId}`);

export const getPaymentsByFounder = (founderId: number): Promise<AxiosResponse<Payment[]>> =>
  paymentApi.get(`/api/payments/founder/${founderId}`);

export const getPaymentsByStartup = (startupId: number): Promise<AxiosResponse<Payment[]>> =>
  paymentApi.get(`/api/payments/startup/${startupId}`);

export const getSagaStatus = (paymentId: number): Promise<AxiosResponse<PaymentSaga>> =>
  paymentApi.get(`/api/payments/${paymentId}/saga`);
