import { AxiosResponse } from 'axios';
import api from './axiosConfig';
import { Notification } from '../../types';

export const getNotifications = (userId: number): Promise<AxiosResponse<Notification[]>> =>
  api.get(`/notifications/${userId}`);

export const getUnreadNotifications = (userId: number): Promise<AxiosResponse<Notification[]>> =>
  api.get(`/notifications/${userId}/unread`);

export const markAsRead = (notificationId: number): Promise<AxiosResponse<void>> =>
  api.put(`/notifications/${notificationId}/read`);
