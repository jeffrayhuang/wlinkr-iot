import client from './client';
import type { Device, CreateDeviceRequest, UpdateDeviceRequest, Page } from '../types';

export const devicesApi = {
  list: (page = 0, size = 20) =>
    client.get<Page<Device>>('/devices', { params: { page, size } }),

  get: (id: number) =>
    client.get<Device>(`/devices/${id}`),

  create: (data: CreateDeviceRequest) =>
    client.post<Device>('/devices', data),

  update: (id: number, data: UpdateDeviceRequest) =>
    client.put<Device>(`/devices/${id}`, data),

  delete: (id: number) =>
    client.delete(`/devices/${id}`),
};
