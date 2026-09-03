import React, { useState, useEffect } from 'react';
import { Routes, Route } from 'react-router-dom';
import { Navbar } from './components/Navbar';
import { Sidebar } from './components/Sidebar';
import { ToastProvider } from './components/Toast';
import { DashboardPage } from './pages/DashboardPage';
import { OpportunitiesPage } from './pages/OpportunitiesPage';
import { OpportunityDetailPage } from './pages/OpportunityDetailPage';
import { TransactionsPage } from './pages/TransactionsPage';
import { ActivityPage } from './pages/ActivityPage';
import api from './services/api';

export function AppContent() {
  const [demoStatus, setDemoStatus] = useState({
    paymentMode: 'DEMO',
    failureSimulationActive: false,
  });
  const [activeCount, setActiveCount] = useState(0);

  const refreshGlobalState = async () => {
    try {
      const status = await api.getDemoStatus();
      setDemoStatus(status);
      const dash = await api.getDashboard();
      setActiveCount(dash.activeOpportunitiesCount);
    } catch (err) {
      console.warn('Failed to load global state:', err);
    }
  };

  useEffect(() => {
    refreshGlobalState();
  }, []);

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col selection:bg-teal-500/30">
      <Navbar
        demoStatus={demoStatus}
        setDemoStatus={setDemoStatus}
        onDataRefresh={refreshGlobalState}
      />
      <div className="flex flex-1">
        <Sidebar activeOpportunitiesCount={activeCount} />
        <main className="flex-1 bg-slate-950/70 overflow-y-auto">
          <Routes>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/opportunities" element={<OpportunitiesPage />} />
            <Route path="/opportunities/:id" element={<OpportunityDetailPage />} />
            <Route path="/transactions" element={<TransactionsPage />} />
            <Route path="/activity" element={<ActivityPage />} />
          </Routes>
        </main>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <ToastProvider>
      <AppContent />
    </ToastProvider>
  );
}
