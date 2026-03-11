import type { Device } from '../types';
import { Link } from 'react-router-dom';

const statusColor: Record<string, string> = {
  ONLINE: 'bg-green-100 text-green-800',
  OFFLINE: 'bg-gray-100 text-gray-800',
  MAINTENANCE: 'bg-yellow-100 text-yellow-800',
  ERROR: 'bg-red-100 text-red-800',
};

export default function DeviceCard({ device }: { device: Device }) {
  return (
    <Link
      to={`/devices/${device.id}`}
      className="block bg-white rounded-lg shadow-sm border border-gray-200 p-5 hover:shadow-md transition-shadow"
    >
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-lg font-semibold text-gray-900">{device.name}</h3>
        <span
          className={`text-xs font-medium px-2.5 py-0.5 rounded-full ${statusColor[device.status]}`}
        >
          {device.status}
        </span>
      </div>
      <dl className="grid grid-cols-2 gap-2 text-sm text-gray-600">
        <div>
          <dt className="font-medium text-gray-500">Type</dt>
          <dd>{device.deviceType}</dd>
        </div>
        <div>
          <dt className="font-medium text-gray-500">Serial</dt>
          <dd className="font-mono text-xs">{device.serialNumber}</dd>
        </div>
        {device.location && (
          <div className="col-span-2">
            <dt className="font-medium text-gray-500">Location</dt>
            <dd>{device.location}</dd>
          </div>
        )}
      </dl>
    </Link>
  );
}
