'use client';

import { useEffect, useState } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

export default function Dashboard() {
  const [stats, setStats] = useState({
    openIncidents: 0,
    resolvedToday: 0,
    avgResolutionTime: 0,
    criticalAlerts: 0,
  });

  useEffect(() => {
    // Mock data - replace with real API calls
    setStats({
      openIncidents: 5,
      resolvedToday: 12,
      avgResolutionTime: 45,
      criticalAlerts: 2,
    });
  }, []);

  const chartData = [
    { name: 'Mon', incidents: 4, resolved: 3 },
    { name: 'Tue', incidents: 3, resolved: 4 },
    { name: 'Wed', incidents: 5, resolved: 2 },
    { name: 'Thu', incidents: 2, resolved: 6 },
    { name: 'Fri', incidents: 7, resolved: 5 },
  ];

  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold mb-8">Dashboard</h1>

      <div className="grid grid-cols-4 gap-4 mb-8">
        <div className="bg-gray-900 p-6 rounded border border-gray-800">
          <h3 className="text-gray-400 text-sm mb-2">Open Incidents</h3>
          <p className="text-3xl font-bold text-blue-400">{stats.openIncidents}</p>
        </div>
        <div className="bg-gray-900 p-6 rounded border border-gray-800">
          <h3 className="text-gray-400 text-sm mb-2">Resolved Today</h3>
          <p className="text-3xl font-bold text-green-400">{stats.resolvedToday}</p>
        </div>
        <div className="bg-gray-900 p-6 rounded border border-gray-800">
          <h3 className="text-gray-400 text-sm mb-2">Avg Resolution Time</h3>
          <p className="text-3xl font-bold text-yellow-400">{stats.avgResolutionTime}m</p>
        </div>
        <div className="bg-gray-900 p-6 rounded border border-gray-800">
          <h3 className="text-gray-400 text-sm mb-2">Critical Alerts</h3>
          <p className="text-3xl font-bold text-red-400">{stats.criticalAlerts}</p>
        </div>
      </div>

      <div className="bg-gray-900 p-6 rounded border border-gray-800">
        <h2 className="text-xl font-bold mb-4">Weekly Activity</h2>
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="name" />
            <YAxis />
            <Tooltip />
            <Legend />
            <Bar dataKey="incidents" fill="#ff4444" />
            <Bar dataKey="resolved" fill="#44ff44" />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
