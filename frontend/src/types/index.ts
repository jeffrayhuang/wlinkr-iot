/* ============================================
   WLinkr IoT Platform – Shared TypeScript Types
   ============================================ */

export interface User {
  id: number;
  email: string;
  name: string;
  avatarUrl: string | null;
  provider: 'GOOGLE' | 'FACEBOOK';
  createdAt: string;
}

export type DeviceStatus = 'ONLINE' | 'OFFLINE' | 'MAINTENANCE' | 'ERROR';
export type DeviceType = 'SENSOR' | 'ACTUATOR' | 'GATEWAY' | 'CONTROLLER';
export type CommandStatus = 'PENDING' | 'SENT' | 'ACKNOWLEDGED' | 'FAILED' | 'EXPIRED';

export interface Device {
  id: number;
  name: string;
  deviceType: DeviceType;
  status: DeviceStatus;
  serialNumber: string;
  firmwareVersion: string | null;
  location: string | null;
  description: string | null;
  ownerId: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDeviceRequest {
  name: string;
  deviceType: DeviceType;
  serialNumber: string;
  firmwareVersion?: string;
  location?: string;
  description?: string;
}

export interface UpdateDeviceRequest {
  name?: string;
  status?: DeviceStatus;
  firmwareVersion?: string;
  location?: string;
  description?: string;
}

export interface SensorData {
  id: number;
  deviceId: number;
  metricName: string;
  metricValue: number;
  unit: string | null;
  recordedAt: string;
}

export interface CreateSensorDataRequest {
  metricName: string;
  metricValue: number;
  unit?: string;
}

export interface DeviceCommand {
  id: number;
  deviceId: number;
  issuedById: number;
  commandName: string;
  payload: Record<string, unknown> | null;
  status: CommandStatus;
  response: Record<string, unknown> | null;
  createdAt: string;
  executedAt: string | null;
}

export interface CreateCommandRequest {
  commandName: string;
  payload?: Record<string, unknown>;
}

export interface Dashboard {
  totalDevices: number;
  onlineDevices: number;
  offlineDevices: number;
  errorDevices: number;
  totalSensorReadings: number;
  pendingCommands: number;
  devicesByType: Record<string, number>;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
