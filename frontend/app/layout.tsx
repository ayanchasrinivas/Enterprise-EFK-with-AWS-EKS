import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'OpsBrain - Incident Management',
  description: 'DevOps Incident Management & Response Platform',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body className="bg-gray-950 text-white">
        <div className="flex h-screen">
          <nav className="w-64 bg-gray-900 border-r border-gray-800 p-4">
            <div className="mb-8">
              <h1 className="text-2xl font-bold text-blue-400">OpsBrain</h1>
              <p className="text-xs text-gray-400">Incident Management</p>
            </div>
            <ul className="space-y-2">
              <li><a href="/" className="block p-2 hover:bg-gray-800 rounded">Dashboard</a></li>
              <li><a href="/incidents" className="block p-2 hover:bg-gray-800 rounded">Incidents</a></li>
              <li><a href="/on-call" className="block p-2 hover:bg-gray-800 rounded">On-Call</a></li>
              <li><a href="/notifications" className="block p-2 hover:bg-gray-800 rounded">Notifications</a></li>
              <li><a href="/postmortems" className="block p-2 hover:bg-gray-800 rounded">Postmortems</a></li>
            </ul>
          </nav>
          <main className="flex-1 overflow-auto">
            {children}
          </main>
        </div>
      </body>
    </html>
  );
}
