'use client';

import { useEffect, useState } from 'react';
import { AlertCircle, Clock, User } from 'lucide-react';

interface Incident {
  id: number;
  title: string;
  severity: string;
  service: string;
  status: string;
  createdAt: string;
  onCallMember?: string;
}

export default function Incidents() {
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Mock data
    setIncidents([
      {
        id: 1,
        title: 'Database connection pool exhausted',
        severity: 'CRITICAL',
        service: 'payment-service',
        status: 'OPEN',
        createdAt: new Date().toISOString(),
        onCallMember: 'John Doe',
      },
      {
        id: 2,
        title: 'High memory usage detected',
        severity: 'HIGH',
        service: 'api-gateway',
        status: 'ACKNOWLEDGED',
        createdAt: new Date(Date.now() - 3600000).toISOString(),
        onCallMember: 'Jane Smith',
      },
    ]);
    setLoading(false);
  }, []);

  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case 'CRITICAL':
        return 'badge-critical';
      case 'HIGH':
        return 'badge-high';
      case 'MEDIUM':
        return 'badge-medium';
      default:
        return 'badge-low';
    }
  };

  const getStatusColor = (status: string) => {
    return status === 'OPEN' ? 'text-red-500' : status === 'ACKNOWLEDGED' ? 'text-yellow-500' : 'text-green-500';
  };

  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold mb-8">Incidents</h1>

      {loading ? (
        <div className="text-center text-gray-400">Loading...</div>
      ) : (
        <div className="space-y-4">
          {incidents.map((incident) => (
            <div key={incident.id} className="bg-gray-900 p-6 rounded border border-gray-800 hover:border-gray-700">
              <div className="flex justify-between items-start mb-4">
                <div className="flex items-start gap-3">
                  <AlertCircle className="text-red-500 mt-1" size={20} />
                  <div>
                    <h3 className="text-lg font-bold">{incident.title}</h3>
                    <p className="text-gray-400 text-sm">{incident.service}</p>
                  </div>
                </div>
                <span className={getSeverityColor(incident.severity)}>{incident.severity}</span>
              </div>

              <div className="flex gap-6 text-sm text-gray-400">
                <div className="flex items-center gap-2">
                  <Clock size={16} />
                  <span>{new Date(incident.createdAt).toLocaleString()}</span>
                </div>
                <div className="flex items-center gap-2">
                  <User size={16} />
                  <span>{incident.onCallMember}</span>
                </div>
                <div className={`flex items-center gap-2 ${getStatusColor(incident.status)}`}>
                  <span>{incident.status}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
