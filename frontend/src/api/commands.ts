import client from './client';
import type { DeviceCommand, CreateCommandRequest, Page } from '../types';

export const commandsApi = {
  list: (deviceId: number, page = 0, size = 20) =>
    client.get<Page<DeviceCommand>>(`/devices/${deviceId}/commands`, { params: { page, size } }),

  send: (deviceId: number, data: CreateCommandRequest) =>
    client.post<DeviceCommand>(`/devices/${deviceId}/commands`, data),
};
