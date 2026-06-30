'use client';

import { useEffect, useState } from 'react';
import { Plus, Check, Trash2 } from 'lucide-react';

interface Task {
  id: string;
  title: string;
  description?: string;
  status: string;
  created_at: string;
}

export default function Dashboard() {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [newTask, setNewTask] = useState('');
  const [loading, setLoading] = useState(true);

  const API_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:3001';

  useEffect(() => {
    fetchTasks();
  }, []);

  const fetchTasks = async () => {
    try {
      const res = await fetch(`${API_URL}/api/tasks`);
      if (res.ok) {
        setTasks(await res.json());
      }
    } catch (err) {
      console.error('Failed to fetch tasks:', err);
    }
    setLoading(false);
  };

  const addTask = async () => {
    if (!newTask.trim()) return;

    try {
      const res = await fetch(`${API_URL}/api/tasks`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title: newTask, description: '' })
      });
      if (res.ok) {
        setNewTask('');
        fetchTasks();
      }
    } catch (err) {
      console.error('Failed to add task:', err);
    }
  };

  const updateTask = async (id: string, status: string) => {
    try {
      const task = tasks.find(t => t.id === id);
      if (!task) return;

      await fetch(`${API_URL}/api/tasks/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...task, status })
      });
      fetchTasks();
    } catch (err) {
      console.error('Failed to update task:', err);
    }
  };

  const deleteTask = async (id: string) => {
    try {
      await fetch(`${API_URL}/api/tasks/${id}`, { method: 'DELETE' });
      fetchTasks();
    } catch (err) {
      console.error('Failed to delete task:', err);
    }
  };

  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold mb-8">Task Manager</h1>

      <div className="bg-gray-900 p-6 rounded border border-gray-800 mb-8">
        <div className="flex gap-2">
          <input
            type="text"
            value={newTask}
            onChange={(e) => setNewTask(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && addTask()}
            placeholder="Add new task..."
            className="flex-1 px-4 py-2 bg-gray-800 border border-gray-700 rounded text-white"
          />
          <button
            onClick={addTask}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded flex items-center gap-2"
          >
            <Plus size={20} /> Add
          </button>
        </div>
      </div>

      {loading ? (
        <div className="text-center text-gray-400">Loading tasks...</div>
      ) : (
        <div className="space-y-3">
          {tasks.map((task) => (
            <div key={task.id} className="bg-gray-900 p-4 rounded border border-gray-800 flex items-center justify-between">
              <div className="flex-1">
                <h3 className="font-semibold">{task.title}</h3>
                <p className="text-sm text-gray-400">Status: {task.status}</p>
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => updateTask(task.id, 'COMPLETED')}
                  className="p-2 hover:bg-gray-800 rounded"
                  title="Mark complete"
                >
                  <Check size={20} />
                </button>
                <button
                  onClick={() => deleteTask(task.id)}
                  className="p-2 hover:bg-gray-800 rounded text-red-500"
                  title="Delete task"
                >
                  <Trash2 size={20} />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
