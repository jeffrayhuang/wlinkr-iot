import client from './client';
import type { Dashboard, User } from '../types';

export const dashboardApi = {
  get: () => client.get<Dashboard>('/dashboard'),
};

export const authApi = {
  me: () => client.get<User>('/auth/me'),
};
