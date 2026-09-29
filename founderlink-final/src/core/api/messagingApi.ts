import { AxiosResponse } from 'axios';
import api from './axiosConfig';
import { Message, Conversation } from '../../types';

export interface SendMessageRequest {
  receiverId: number;
  content: string;
  senderId?: number;
  conversationId?: number;
}

export const sendMessage = (data: SendMessageRequest): Promise<AxiosResponse<Message>> =>
  api.post('/messages', data);

export const getConversationMessages = (conversationId: number): Promise<AxiosResponse<Message[]>> =>
  api.get(`/messages/conversation/${conversationId}`);

export const getMyConversations = (): Promise<AxiosResponse<Conversation[]>> =>
  api.get('/messages/conversations');

export const startConversation = (otherUserId: number): Promise<AxiosResponse<Conversation>> =>
  api.post(`/messages/conversations?otherUserId=${otherUserId}`);
