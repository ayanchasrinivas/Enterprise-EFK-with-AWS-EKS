'use client';

import { useEffect, useState } from 'react';
import { Users, Calendar, Phone, Mail } from 'lucide-react';

interface OnCallMember {
  name: string;
  email: string;
  phone?: string;
  service: string;
  startDate: string;
  endDate: string;
}

export default function OnCall() {
  const [schedule, setSchedule] = useState<OnCallMember[]>([]);

  useEffect(() => {
    setSchedule([
      {
        name: 'Alice Johnson',
        email: 'alice@example.com',
        phone: '+1-555-0101',
        service: 'payment-service',
        startDate: new Date().toISOString(),
        endDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
      },
      {
        name: 'Bob Smith',
        email: 'bob@example.com',
        phone: '+1-555-0102',
        service: 'api-gateway',
        startDate: new Date().toISOString(),
        endDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
      },
    ]);
  }, []);

  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold mb-8">On-Call Schedule</h1>

      <div className="space-y-4">
        {schedule.map((member, idx) => (
          <div key={idx} className="bg-gray-900 p-6 rounded border border-gray-800">
            <div className="flex justify-between items-start mb-4">
              <div className="flex items-center gap-3">
                <Users className="text-blue-500" size={24} />
                <div>
                  <h3 className="text-lg font-bold">{member.name}</h3>
                  <p className="text-gray-400 text-sm">{member.service}</p>
                </div>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4 text-sm">
              <div className="flex items-center gap-2 text-gray-400">
                <Mail size={16} />
                <span>{member.email}</span>
              </div>
              <div className="flex items-center gap-2 text-gray-400">
                <Phone size={16} />
                <span>{member.phone}</span>
              </div>
              <div className="flex items-center gap-2 text-gray-400 col-span-2">
                <Calendar size={16} />
                <span>{new Date(member.startDate).toLocaleDateString()} - {new Date(member.endDate).toLocaleDateString()}</span>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
