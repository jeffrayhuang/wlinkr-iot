import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts';
import type { SensorData } from '../types';

interface Props {
  data: SensorData[];
  metricName: string;
}

export default function SensorChart({ data, metricName }: Props) {
  const formatted = data.map((d) => ({
    time: new Date(d.recordedAt).toLocaleTimeString(),
    value: d.metricValue,
  }));

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-5">
      <h4 className="text-sm font-semibold text-gray-700 mb-4">{metricName}</h4>
      <ResponsiveContainer width="100%" height={250}>
        <LineChart data={formatted}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="time" tick={{ fontSize: 11 }} />
          <YAxis tick={{ fontSize: 11 }} />
          <Tooltip />
          <Line
            type="monotone"
            dataKey="value"
            stroke="#3b82f6"
            strokeWidth={2}
            dot={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
