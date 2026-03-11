import { useEffect, useState } from 'react';
import type { Dashboard } from '../types';
import { dashboardApi } from '../api/dashboard';
import Layout from '../components/Layout';

export default function DashboardPage() {
  const [dash, setDash] = useState<Dashboard | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    dashboardApi
      .get()
      .then((res) => setDash(res.data))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <Layout>
        <div className="flex justify-center py-20">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-500 border-t-transparent" />
        </div>
      </Layout>
    );
  }

  if (!dash) return <Layout><p>Failed to load dashboard.</p></Layout>;

  const stats = [
    { label: 'Total Devices', value: dash.totalDevices, color: 'text-gray-900' },
    { label: 'Online', value: dash.onlineDevices, color: 'text-green-600' },
    { label: 'Offline', value: dash.offlineDevices, color: 'text-gray-500' },
    { label: 'Errors', value: dash.errorDevices, color: 'text-red-600' },
    { label: 'Sensor Readings', value: dash.totalSensorReadings, color: 'text-blue-600' },
    { label: 'Pending Commands', value: dash.pendingCommands, color: 'text-yellow-600' },
  ];

  return (
    <Layout>
      <h2 className="text-2xl font-bold mb-6">Dashboard</h2>

      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4 mb-8">
        {stats.map((s) => (
          <div
            key={s.label}
            className="bg-white rounded-lg shadow-sm border border-gray-200 p-4 text-center"
          >
            <p className={`text-3xl font-bold ${s.color}`}>{s.value}</p>
            <p className="text-xs text-gray-500 mt-1">{s.label}</p>
          </div>
        ))}
      </div>

      {Object.keys(dash.devicesByType).length > 0 && (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-5">
          <h3 className="text-sm font-semibold text-gray-700 mb-3">Devices by Type</h3>
          <div className="flex gap-6">
            {Object.entries(dash.devicesByType).map(([type, count]) => (
              <div key={type} className="text-center">
                <p className="text-2xl font-bold text-brand-600">{count}</p>
                <p className="text-xs text-gray-500">{type}</p>
              </div>
            ))}
          </div>
        </div>
      )}
    </Layout>
  );
}
