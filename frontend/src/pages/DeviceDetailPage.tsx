import { useEffect, useState, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import Layout from '../components/Layout';
import SensorChart from '../components/SensorChart';
import CommandPanel from '../components/CommandPanel';
import { devicesApi } from '../api/devices';
import { sensorDataApi } from '../api/sensorData';
import { commandsApi } from '../api/commands';
import type { Device, SensorData, DeviceCommand } from '../types';

export default function DeviceDetailPage() {
  const { id } = useParams<{ id: string }>();
  const deviceId = Number(id);

  const [device, setDevice] = useState<Device | null>(null);
  const [sensorData, setSensorData] = useState<SensorData[]>([]);
  const [commands, setCommands] = useState<DeviceCommand[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchAll = useCallback(async () => {
    setLoading(true);
    try {
      const [devRes, dataRes, cmdRes] = await Promise.all([
        devicesApi.get(deviceId),
        sensorDataApi.list(deviceId, 0, 100),
        commandsApi.list(deviceId),
      ]);
      setDevice(devRes.data);
      setSensorData(dataRes.data.content);
      setCommands(cmdRes.data.content);
    } finally {
      setLoading(false);
    }
  }, [deviceId]);

  useEffect(() => {
    fetchAll();
  }, [fetchAll]);

  if (loading) {
    return (
      <Layout>
        <div className="flex justify-center py-20">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-500 border-t-transparent" />
        </div>
      </Layout>
    );
  }

  if (!device) return <Layout><p>Device not found.</p></Layout>;

  // Group sensor data by metric name for charts
  const byMetric = sensorData.reduce<Record<string, SensorData[]>>((acc, sd) => {
    (acc[sd.metricName] ??= []).push(sd);
    return acc;
  }, {});

  const statusColor: Record<string, string> = {
    ONLINE: 'bg-green-100 text-green-800',
    OFFLINE: 'bg-gray-100 text-gray-800',
    MAINTENANCE: 'bg-yellow-100 text-yellow-800',
    ERROR: 'bg-red-100 text-red-800',
  };

  return (
    <Layout>
      {/* Device header */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 mb-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-2xl font-bold">{device.name}</h2>
          <span className={`text-sm font-medium px-3 py-1 rounded-full ${statusColor[device.status]}`}>
            {device.status}
          </span>
        </div>
        <dl className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm text-gray-600">
          <div>
            <dt className="font-medium text-gray-500">Type</dt>
            <dd>{device.deviceType}</dd>
          </div>
          <div>
            <dt className="font-medium text-gray-500">Serial</dt>
            <dd className="font-mono text-xs">{device.serialNumber}</dd>
          </div>
          <div>
            <dt className="font-medium text-gray-500">Firmware</dt>
            <dd>{device.firmwareVersion ?? '—'}</dd>
          </div>
          <div>
            <dt className="font-medium text-gray-500">Location</dt>
            <dd>{device.location ?? '—'}</dd>
          </div>
        </dl>
        {device.description && (
          <p className="mt-3 text-sm text-gray-500">{device.description}</p>
        )}
      </div>

      {/* Sensor charts */}
      <div className="space-y-4 mb-6">
        {Object.entries(byMetric).map(([metric, readings]) => (
          <SensorChart key={metric} data={readings} metricName={metric} />
        ))}
        {sensorData.length === 0 && (
          <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-8 text-center text-gray-400">
            No sensor data received yet
          </div>
        )}
      </div>

      {/* Commands */}
      <CommandPanel
        deviceId={deviceId}
        commands={commands}
        onCommandSent={fetchAll}
      />
    </Layout>
  );
}
