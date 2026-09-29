import { AxiosResponse } from 'axios';
import api from './axiosConfig';
import { TeamMember, Invitation, InviteCoFounderData } from '../../types';

export const inviteCoFounder = (data: InviteCoFounderData): Promise<AxiosResponse<Invitation>> =>
  api.post('/teams/invite', data);

export const acceptInvitation = (invitationId: number): Promise<AxiosResponse<TeamMember>> =>
  api.post(`/teams/join/${invitationId}`);

export const rejectInvitation = (invitationId: number): Promise<AxiosResponse<void>> =>
  api.put(`/teams/reject/${invitationId}`);

export const getTeamByStartup = (startupId: number): Promise<AxiosResponse<TeamMember[]>> =>
  api.get(`/teams/startup/${startupId}`);

export const getMyInvitations = (): Promise<AxiosResponse<Invitation[]>> =>
  api.get('/teams/invitations/my');

export const updateMemberRole = (memberId: number, data: { role: string }): Promise<AxiosResponse<TeamMember>> =>
  api.put(`/teams/${memberId}/role`, data);
