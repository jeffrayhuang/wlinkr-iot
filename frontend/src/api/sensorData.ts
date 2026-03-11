import client from './client';
import type { SensorData, CreateSensorDataRequest, Page } from '../types';

export const sensorDataApi = {
  list: (deviceId: number, page = 0, size = 50) =>
    client.get<Page<SensorData>>(`/devices/${deviceId}/data`, { params: { page, size } }),

  range: (deviceId: number, from: string, to: string) =>
    client.get<SensorData[]>(`/devices/${deviceId}/data/range`, { params: { from, to } }),

  latest: (deviceId: number, metric: string, limit = 10) =>
    client.get<SensorData[]>(`/devices/${deviceId}/data/latest`, { params: { metric, limit } }),

  ingest: (deviceId: number, data: CreateSensorDataRequest) =>
    client.post<SensorData>(`/devices/${deviceId}/data`, data),
};
