import { useState } from 'react';
import Layout from '../components/Layout';
import DeviceCard from '../components/DeviceCard';
import { useDevices } from '../hooks/useDevices';
import { devicesApi } from '../api/devices';
import type { CreateDeviceRequest, DeviceType } from '../types';

export default function DevicesPage() {
  const [page, setPage] = useState(0);
  const { data, loading, refetch } = useDevices(page);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState<CreateDeviceRequest>({
    name: '',
    deviceType: 'SENSOR',
    serialNumber: '',
  });

  const handleCreate = async () => {
    await devicesApi.create(form);
    setShowForm(false);
    setForm({ name: '', deviceType: 'SENSOR', serialNumber: '' });
    refetch();
  };

  return (
    <Layout>
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-bold">Devices</h2>
        <button
          onClick={() => setShowForm(!showForm)}
          className="px-4 py-2 rounded-md bg-brand-600 text-white text-sm font-medium hover:bg-brand-700"
        >
          {showForm ? 'Cancel' : '+ Add Device'}
        </button>
      </div>

      {showForm && (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-5 mb-6 space-y-3">
          <input
            placeholder="Device name"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
          />
          <div className="flex gap-3">
            <select
              value={form.deviceType}
              onChange={(e) => setForm({ ...form, deviceType: e.target.value as DeviceType })}
              className="flex-1 rounded-md border border-gray-300 px-3 py-2 text-sm"
            >
              <option value="SENSOR">Sensor</option>
              <option value="ACTUATOR">Actuator</option>
              <option value="GATEWAY">Gateway</option>
              <option value="CONTROLLER">Controller</option>
            </select>
            <input
              placeholder="Serial number"
              value={form.serialNumber}
              onChange={(e) => setForm({ ...form, serialNumber: e.target.value })}
              className="flex-1 rounded-md border border-gray-300 px-3 py-2 text-sm font-mono"
            />
          </div>
          <input
            placeholder="Location (optional)"
            value={form.location || ''}
            onChange={(e) => setForm({ ...form, location: e.target.value })}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
          />
          <button
            onClick={handleCreate}
            disabled={!form.name || !form.serialNumber}
            className="px-4 py-2 rounded-md bg-green-600 text-white text-sm font-medium hover:bg-green-700 disabled:opacity-50"
          >
            Create Device
          </button>
        </div>
      )}

      {loading ? (
        <div className="flex justify-center py-20">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-500 border-t-transparent" />
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {data?.content.map((device) => (
              <DeviceCard key={device.id} device={device} />
            ))}
          </div>

          {data && data.totalPages > 1 && (
            <div className="flex justify-center gap-2 mt-8">
              <button
                disabled={data.first}
                onClick={() => setPage((p) => p - 1)}
                className="px-3 py-1 rounded border text-sm disabled:opacity-30"
              >
                Previous
              </button>
              <span className="px-3 py-1 text-sm text-gray-600">
                Page {data.number + 1} of {data.totalPages}
              </span>
              <button
                disabled={data.last}
                onClick={() => setPage((p) => p + 1)}
                className="px-3 py-1 rounded border text-sm disabled:opacity-30"
              >
                Next
              </button>
            </div>
          )}

          {data?.content.length === 0 && (
            <p className="text-center text-gray-400 py-20">
              No devices yet. Click "+ Add Device" to register one.
            </p>
          )}
        </>
      )}
    </Layout>
  );
}
