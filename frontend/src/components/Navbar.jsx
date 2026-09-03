import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldCheck, Zap, Database, AlertOctagon, RefreshCw, CheckCircle2 } from 'lucide-react';
import api from '../services/api';
import { useToast } from './Toast';

export const Navbar = ({ onDataRefresh, demoStatus, setDemoStatus }) => {
  const navigate = useNavigate();
  const { addToast } = useToast();
  const [loadingAction, setLoadingAction] = useState(null);

  const handleSimulateLeak = async () => {
    try {
      setLoadingAction('simulate');
      const opp = await api.simulateLeak();
      addToast(`Revenue leak detected! Opportunity #${opp.id} created for ${opp.customerName} (₹${opp.amount})`, 'warning');
      if (onDataRefresh) onDataRefresh();
      // Navigate to opportunity details
      navigate(`/opportunities/${opp.id}`);
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to simulate revenue leak', 'error');
    } finally {
      setLoadingAction(null);
    }
  };

  const handleSeedData = async () => {
    try {
      setLoadingAction('seed');
      const res = await api.seedDemoData();
      addToast(res.message || 'Demo dataset successfully initialized with 10 customers & 26 transactions!', 'success');
      if (onDataRefresh) onDataRefresh();
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to initialize demo data', 'error');
    } finally {
      setLoadingAction(null);
    }
  };

  const handleToggleFailure = async () => {
    try {
      setLoadingAction('toggle-failure');
      const res = await api.toggleFailureSimulation();
      if (setDemoStatus) {
        setDemoStatus((prev) => ({ ...prev, failureSimulationActive: res.failureSimulationActive }));
      }
      addToast(res.message, res.failureSimulationActive ? 'warning' : 'info');
      if (onDataRefresh) onDataRefresh();
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to toggle failure scenario', 'error');
    } finally {
      setLoadingAction(null);
    }
  };

  return (
    <header className="sticky top-0 z-40 w-full border-b border-slate-800 bg-slate-950/80 backdrop-blur-md">
      <div className="flex h-16 items-center justify-between px-6">
        {/* Brand */}
        <div className="flex items-center gap-3 cursor-pointer" onClick={() => navigate('/')}>
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-tr from-teal-600 to-emerald-400 text-white shadow-lg shadow-teal-500/20">
            <ShieldCheck className="h-6 w-6" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-lg font-bold tracking-tight text-white">RefundShield</span>
              <span className="rounded bg-teal-500/10 px-1.5 py-0.5 text-[10px] font-semibold text-teal-400 border border-teal-500/20">
                RECOVERY AGENT
              </span>
            </div>
            <p className="text-xs text-slate-400 hidden sm:block">AI-Powered Revenue Leak Prevention</p>
          </div>
        </div>

        {/* Status Indicators & Demo Controls */}
        <div className="flex items-center gap-3">
          {/* Payment Gateway Mode Badge */}
          <div className="hidden lg:flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-slate-800/80 border border-slate-700 text-slate-300">
            <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse"></span>
            <span>Razorpay {demoStatus?.paymentMode === 'RAZORPAY' ? 'TEST MODE' : 'DEMO MODE'}</span>
          </div>

          {/* Failure Scenario Toggle Pill */}
          <button
            onClick={handleToggleFailure}
            disabled={loadingAction === 'toggle-failure'}
            title="Toggle Razorpay API failure simulation for testing edge cases"
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all border ${
              demoStatus?.failureSimulationActive
                ? 'bg-rose-500/15 border-rose-500/40 text-rose-400 shadow-lg shadow-rose-950/40'
                : 'bg-slate-900 border-slate-800 text-slate-400 hover:text-slate-200 hover:border-slate-700'
            }`}
          >
            <AlertOctagon className={`h-3.5 w-3.5 ${demoStatus?.failureSimulationActive ? 'text-rose-400' : 'text-slate-400'}`} />
            <span className="hidden sm:inline">Failure Simulation:</span>
            <span>{demoStatus?.failureSimulationActive ? 'ON' : 'OFF'}</span>
          </button>

          {/* Seed Demo Data Button */}
          <button
            onClick={handleSeedData}
            disabled={loadingAction === 'seed'}
            title="Reset and reload 10 customers & 26 transactions"
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-slate-900 border border-slate-800 text-slate-300 hover:text-white hover:bg-slate-800 transition-all"
          >
            <Database className={`h-3.5 w-3.5 ${loadingAction === 'seed' ? 'animate-spin' : 'text-teal-400'}`} />
            <span className="hidden sm:inline">Load Demo Data</span>
          </button>

          {/* Simulate Revenue Leak Button */}
          <button
            onClick={handleSimulateLeak}
            disabled={loadingAction === 'simulate'}
            className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-semibold bg-gradient-to-r from-teal-500 to-emerald-500 text-slate-950 hover:from-teal-400 hover:to-emerald-400 shadow-md shadow-teal-500/20 active:scale-95 transition-all"
          >
            <Zap className={`h-3.5 w-3.5 fill-current ${loadingAction === 'simulate' ? 'animate-bounce' : ''}`} />
            <span>Simulate Revenue Leak</span>
          </button>
        </div>
      </div>
    </header>
  );
};
