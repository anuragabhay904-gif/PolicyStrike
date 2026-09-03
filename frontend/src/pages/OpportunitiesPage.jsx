import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, Filter, Coins, ChevronRight, ExternalLink, RefreshCw } from 'lucide-react';
import api from '../services/api';
import { useToast } from '../components/Toast';

export const OpportunitiesPage = () => {
  const navigate = useNavigate();
  const { addToast } = useToast();
  const [opportunities, setOpportunities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [searchTerm, setSearchTerm] = useState('');

  const fetchOpportunities = async () => {
    try {
      setLoading(true);
      const data = await api.getOpportunities(statusFilter);
      setOpportunities(data);
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to fetch opportunities', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOpportunities();
  }, [statusFilter]);

  const filtered = opportunities.filter((opp) => {
    if (!searchTerm) return true;
    const term = searchTerm.toLowerCase();
    return (
      opp.customerName?.toLowerCase().includes(term) ||
      opp.customerEmail?.toLowerCase().includes(term) ||
      opp.type?.toLowerCase().includes(term) ||
      opp.reason?.toLowerCase().includes(term)
    );
  });

  const statuses = ['ALL', 'NEW', 'REVIEWED', 'RECOVERY_CREATED', 'DISMISSED', 'FAILED'];

  return (
    <div className="space-y-6 p-8 max-w-7xl mx-auto">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Revenue Recovery Opportunities</h1>
          <p className="text-sm text-slate-400 mt-1">
            Review intercepted revenue leaks, run AI analysis, and issue Razorpay recovery links.
          </p>
        </div>
        <button
          onClick={fetchOpportunities}
          className="flex items-center gap-2 px-3 py-2 rounded-xl text-xs font-medium bg-slate-900 border border-slate-800 text-slate-300 hover:text-white transition-all self-start md:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Filter Tabs & Search */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        {/* Status Tabs */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-2 sm:pb-0">
          {statuses.map((s) => (
            <button
              key={s}
              onClick={() => setStatusFilter(s)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-all ${
                statusFilter === s
                  ? 'bg-teal-500/20 text-teal-300 border border-teal-500/40'
                  : 'bg-slate-900/60 text-slate-400 hover:text-slate-200 border border-slate-800'
              }`}
            >
              {s.replace('_', ' ')}
            </button>
          ))}
        </div>

        {/* Search */}
        <div className="relative w-full sm:w-64">
          <Search className="w-4 h-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search customer, type..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-3 py-1.5 rounded-xl bg-slate-900/80 border border-slate-800 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-teal-500"
          />
        </div>
      </div>

      {/* Table */}
      <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-800 text-[11px] uppercase tracking-wider text-slate-400">
                <th className="py-3 px-4">Opportunity</th>
                <th className="py-3 px-4">Customer</th>
                <th className="py-3 px-4">Amount</th>
                <th className="py-3 px-4">Score</th>
                <th className="py-3 px-4">AI Recommendation</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 text-sm">
              {filtered.length > 0 ? (
                filtered.map((opp) => {
                  const typeBadgeClass = {
                    FAILED_PAYMENT_RECOVERY: 'bg-rose-500/10 text-rose-400 border-rose-500/20',
                    HIGH_VALUE_CHECKOUT_RECOVERY: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
                    REPEAT_CUSTOMER_REENGAGEMENT: 'bg-teal-500/10 text-teal-400 border-teal-500/20',
                  }[opp.type] || 'bg-slate-800 text-slate-300';

                  const typeLabel = {
                    FAILED_PAYMENT_RECOVERY: 'Failed Payment',
                    HIGH_VALUE_CHECKOUT_RECOVERY: 'High Value Checkout',
                    REPEAT_CUSTOMER_REENGAGEMENT: 'Repeat Re-engagement',
                  }[opp.type] || opp.type;

                  const statusBadge = {
                    NEW: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
                    REVIEWED: 'bg-purple-500/10 text-purple-400 border-purple-500/20',
                    APPROVED: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
                    RECOVERY_CREATED: 'bg-teal-500/15 text-teal-300 border-teal-500/30',
                    DISMISSED: 'bg-slate-800 text-slate-500 border-slate-700',
                    FAILED: 'bg-rose-900/30 text-rose-400 border-rose-500/40',
                  }[opp.status] || 'bg-slate-800 text-slate-400';

                  return (
                    <tr
                      key={opp.id}
                      onClick={() => navigate(`/opportunities/${opp.id}`)}
                      className="hover:bg-slate-800/40 cursor-pointer transition-colors"
                    >
                      {/* ID & Type */}
                      <td className="py-3.5 px-4">
                        <div className="font-semibold text-white">#{opp.id}</div>
                        <span className={`inline-flex px-2 py-0.5 rounded text-[11px] font-medium border mt-1 ${typeBadgeClass}`}>
                          {typeLabel}
                        </span>
                      </td>

                      {/* Customer */}
                      <td className="py-3.5 px-4">
                        <div className="font-medium text-white">{opp.customerName}</div>
                        <div className="text-xs text-slate-400">{opp.customerEmail}</div>
                      </td>

                      {/* Amount */}
                      <td className="py-3.5 px-4 font-bold text-white">
                        ₹{Number(opp.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                      </td>

                      {/* Score */}
                      <td className="py-3.5 px-4">
                        <div className="flex items-center gap-2">
                          <div className="w-16 h-2 rounded-full bg-slate-800 overflow-hidden">
                            <div
                              className={`h-full rounded-full ${
                                opp.recoveryScore >= 75
                                  ? 'bg-emerald-400'
                                  : opp.recoveryScore >= 50
                                  ? 'bg-amber-400'
                                  : 'bg-slate-400'
                              }`}
                              style={{ width: `${opp.recoveryScore}%` }}
                            ></div>
                          </div>
                          <span className="font-semibold text-xs text-slate-300">{opp.recoveryScore}/100</span>
                        </div>
                      </td>

                      {/* Recommendation */}
                      <td className="py-3.5 px-4 text-xs text-slate-300 max-w-xs truncate">
                        {opp.aiRecommendation || (
                          <span className="text-slate-500 italic">Analysis pending</span>
                        )}
                      </td>

                      {/* Status */}
                      <td className="py-3.5 px-4">
                        <span className={`inline-flex px-2 py-0.5 rounded text-xs font-medium border ${statusBadge}`}>
                          {opp.status}
                        </span>
                      </td>

                      {/* Action */}
                      <td className="py-3.5 px-4 text-right" onClick={(e) => e.stopPropagation()}>
                        <button
                          onClick={() => navigate(`/opportunities/${opp.id}`)}
                          className="px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 transition-colors"
                        >
                          Review
                        </button>
                      </td>
                    </tr>
                  );
                })
              ) : (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-slate-400">
                    {loading ? 'Loading opportunities...' : 'No opportunities match your filter.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
