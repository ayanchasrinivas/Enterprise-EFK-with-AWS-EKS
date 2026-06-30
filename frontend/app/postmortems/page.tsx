'use client';

import { useEffect, useState } from 'react';
import { FileText, Download } from 'lucide-react';

interface Postmortem {
  id: string;
  title: string;
  incidentId: string;
  status: string;
  generatedAt: string;
  generatedBy: string;
}

export default function Postmortems() {
  const [postmortems, setPostmortems] = useState<Postmortem[]>([]);

  useEffect(() => {
    setPostmortems([
      {
        id: 'pm-001',
        title: 'Database Connection Exhaustion - Root Cause & Resolution',
        incidentId: 'incident-1',
        status: 'PUBLISHED',
        generatedAt: new Date(Date.now() - 86400000).toISOString(),
        generatedBy: 'system',
      },
      {
        id: 'pm-002',
        title: 'API Gateway Memory Leak Investigation',
        incidentId: 'incident-2',
        status: 'GENERATED',
        generatedAt: new Date(Date.now() - 172800000).toISOString(),
        generatedBy: 'system',
      },
    ]);
  }, []);

  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold mb-8">Postmortems</h1>

      <div className="space-y-4">
        {postmortems.map((pm) => (
          <div key={pm.id} className="bg-gray-900 p-6 rounded border border-gray-800 hover:border-gray-700">
            <div className="flex justify-between items-start">
              <div className="flex items-start gap-3">
                <FileText className="text-green-500 mt-1" size={20} />
                <div>
                  <h3 className="text-lg font-bold">{pm.title}</h3>
                  <p className="text-gray-400 text-sm">Incident: {pm.incidentId}</p>
                  <p className="text-gray-500 text-xs mt-2">{new Date(pm.generatedAt).toLocaleString()}</p>
                </div>
              </div>
              <div className="flex items-center gap-4">
                <span className="px-3 py-1 bg-green-900 text-green-200 rounded text-xs">{pm.status}</span>
                <button className="p-2 hover:bg-gray-800 rounded">
                  <Download size={20} />
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
