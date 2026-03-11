import { useState } from 'react';
import { commandsApi } from '../api/commands';
import type { DeviceCommand } from '../types';

interface Props {
  deviceId: number;
  commands: DeviceCommand[];
  onCommandSent: () => void;
}

export default function CommandPanel({ deviceId, commands, onCommandSent }: Props) {
  const [commandName, setCommandName] = useState('');
  const [payload, setPayload] = useState('{}');
  const [sending, setSending] = useState(false);

  const handleSend = async () => {
    setSending(true);
    try {
      let parsed: Record<string, unknown> = {};
      try {
        parsed = JSON.parse(payload);
      } catch {
        /* keep empty */
      }
      await commandsApi.send(deviceId, { commandName, payload: parsed });
      setCommandName('');
      setPayload('{}');
      onCommandSent();
    } finally {
      setSending(false);
    }
  };

  const statusBadge: Record<string, string> = {
    PENDING: 'bg-yellow-100 text-yellow-700',
    SENT: 'bg-blue-100 text-blue-700',
    ACKNOWLEDGED: 'bg-green-100 text-green-700',
    FAILED: 'bg-red-100 text-red-700',
    EXPIRED: 'bg-gray-100 text-gray-500',
  };

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-5">
      <h4 className="text-sm font-semibold text-gray-700 mb-4">Send Command</h4>

      <div className="flex gap-3 mb-4">
        <input
          type="text"
          placeholder="Command name"
          value={commandName}
          onChange={(e) => setCommandName(e.target.value)}
          className="flex-1 rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
        />
        <input
          type="text"
          placeholder='{"key":"value"}'
          value={payload}
          onChange={(e) => setPayload(e.target.value)}
          className="flex-1 rounded-md border border-gray-300 px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-brand-500"
        />
        <button
          onClick={handleSend}
          disabled={!commandName || sending}
          className="px-4 py-2 rounded-md bg-brand-600 text-white text-sm font-medium hover:bg-brand-700 disabled:opacity-50"
        >
          {sending ? 'Sending…' : 'Send'}
        </button>
      </div>

      <h4 className="text-sm font-semibold text-gray-700 mb-2">Command History</h4>
      <ul className="divide-y divide-gray-100 max-h-64 overflow-y-auto">
        {commands.map((cmd) => (
          <li key={cmd.id} className="py-2 flex items-center justify-between text-sm">
            <span className="font-mono">{cmd.commandName}</span>
            <div className="flex items-center gap-2">
              <span className="text-xs text-gray-400">
                {new Date(cmd.createdAt).toLocaleString()}
              </span>
              <span
                className={`text-xs px-2 py-0.5 rounded-full ${statusBadge[cmd.status]}`}
              >
                {cmd.status}
              </span>
            </div>
          </li>
        ))}
        {commands.length === 0 && (
          <li className="py-4 text-center text-gray-400 text-sm">No commands yet</li>
        )}
      </ul>
    </div>
  );
}
