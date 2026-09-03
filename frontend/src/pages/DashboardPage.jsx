import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  TrendingUp,
  AlertTriangle,
  Coins,
  CheckCircle,
  ArrowUpRight,
  Sparkles,
  Zap,
  Clock,
  ChevronRight,
  ExternalLink,
} from 'lucide-react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Cell,
} from 'recharts';
import api from '../services/api';
import { useToast } from '../components/Toast';

export const DashboardPage = () => {
  const navigate = useNavigate();
  const { addToast } = useToast();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [approvingId, setApprovingId] = useState(null);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      const res = await api.getDashboard();
      setData(res);
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to fetch dashboard metrics', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const handleQuickApprove = async (e, oppId) => {
    e.stopPropagation();
    try {
      setApprovingId(oppId);
      const res = await api.approveOpportunity(oppId);
      addToast(
        `Payment link generated! Ref: ${res.razorpayReference}`,
        'success'
      );
      fetchDashboardData();
    } catch (err) {
      addToast(
        err.response?.data?.message || 'Failed to approve recovery action',
        'error'
      );
    } finally {
      setApprovingId(null);
    }
  };

  if (loading && !data) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="flex flex-col items-center gap-3">
          <div className="h-8 w-8 animate-spin rounded-full border-2 border-teal-500 border-t-transparent"></div>
          <p className="text-sm text-slate-400">Loading merchant analytics...</p>
        </div>
      </div>
    );
  }

  // Chart data for revenue comparison
  const chartData = [
    { name: 'Successful', amount: Number(data?.successfulRevenue || 0), fill: '#10b981' },
    { name: 'Failed Payments', amount: Number(data?.failedRevenue || 0), fill: '#f43f5e' },
    { name: 'Abandoned Cart', amount: Number(data?.abandonedRevenue || 0), fill: '#f59e0b' },
    { name: 'Potential Recovery', amount: Number(data?.potentialRecoverableRevenue || 0), fill: '#14b8a6' },
  ];

  return (
    <div className="space-y-8 p-8 max-w-7xl mx-auto">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Merchant Revenue Overview</h1>
          <p className="text-sm text-slate-400 mt-1">
            Deterministic leak detection across payment gateways with autonomous AI recommendations.
          </p>
        </div>
      </div>

      {/* Top KPI Cards (Section 10) */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {/* 1. Total Revenue */}
        <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 shadow-sm relative overflow-hidden group">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Total Captured Revenue</span>
            <div className="p-2 rounded-xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              <TrendingUp className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-bold text-white">
            ₹{Number(data?.totalRevenue || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
          <p className="text-xs text-slate-500 mt-1.5 flex items-center gap-1">
            <span className="text-emerald-400 font-medium">100% verified</span> settlements
          </p>
        </div>

        {/* 2. Potential Recoverable Revenue */}
        <div className="p-5 rounded-2xl bg-gradient-to-br from-teal-950/40 via-slate-900/80 to-slate-900 border border-teal-500/30 shadow-lg shadow-teal-950/20 relative overflow-hidden">
          <div className="flex items-center justify-between text-teal-300 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Potential Recoverable</span>
            <div className="p-2 rounded-xl bg-teal-500/20 text-teal-300 border border-teal-500/30 animate-pulse">
              <Coins className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-bold text-teal-300">
            ₹{Number(data?.potentialRecoverableRevenue || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
          <p className="text-xs text-teal-400/80 mt-1.5 flex items-center gap-1">
            <Sparkles className="w-3.5 h-3.5" /> High likelihood of recovery
          </p>
        </div>

        {/* 3. Active Recovery Opportunities */}
        <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 shadow-sm relative overflow-hidden">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Active Opportunities</span>
            <div className="p-2 rounded-xl bg-amber-500/10 text-amber-400 border border-amber-500/20">
              <AlertTriangle className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-bold text-white">{data?.activeOpportunitiesCount || 0}</div>
          <p className="text-xs text-slate-500 mt-1.5">
            Awaiting merchant review & approval
          </p>
        </div>

        {/* 4. Recovered Test Revenue */}
        <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 shadow-sm relative overflow-hidden">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Recovered Revenue</span>
            <div className="p-2 rounded-xl bg-teal-500/10 text-teal-400 border border-teal-500/20">
              <CheckCircle className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-bold text-white">
            ₹{Number(data?.recoveredRevenue || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
          <p className="text-xs text-slate-500 mt-1.5">
            Through approved recovery links
          </p>
        </div>
      </div>

      {/* Revenue Leak Chart & Visual Breakdown */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 p-6 rounded-2xl bg-slate-900/60 border border-slate-800">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-base font-semibold text-white">Revenue Pipeline & Leak Analysis</h2>
              <p className="text-xs text-slate-400">Comparison of successful transactions vs intercepted leak categories</p>
            </div>
          </div>
          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData} margin={{ top: 10, right: 10, left: 10, bottom: 20 }}>
                <XAxis dataKey="name" stroke="#64748b" fontSize={12} tickLine={false} />
                <YAxis
                  stroke="#64748b"
                  fontSize={12}
                  tickLine={false}
                  tickFormatter={(val) => `₹${val >= 1000 ? (val / 1000).toFixed(0) + 'k' : val}`}
                />
                <Tooltip
                  cursor={{ fill: 'rgba(255, 255, 255, 0.05)' }}
                  content={({ active, payload }) => {
                    if (active && payload && payload.length) {
                      return (
                        <div className="rounded-xl border border-slate-700 bg-slate-900 p-3 shadow-xl">
                          <p className="text-xs font-semibold text-slate-400">{payload[0].payload.name}</p>
                          <p className="text-base font-bold text-white mt-1">
                            ₹{Number(payload[0].value).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                          </p>
                        </div>
                      );
                    }
                    return null;
                  }}
                />
                <Bar dataKey="amount" radius={[8, 8, 0, 0]}>
                  {chartData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.fill} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Leak Distribution Stats */}
        <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 flex flex-col justify-between">
          <div>
            <h2 className="text-base font-semibold text-white mb-1">Leak Breakdown</h2>
            <p className="text-xs text-slate-400 mb-6">Intercepted across active rules</p>
            <div className="space-y-4">
              <div className="p-3.5 rounded-xl bg-slate-800/40 border border-slate-800 flex items-center justify-between">
                <div>
                  <div className="text-xs text-slate-400">Failed Payments</div>
                  <div className="text-base font-semibold text-white">
                    ₹{Number(data?.failedRevenue || 0).toLocaleString('en-IN')}
                  </div>
                </div>
                <span className="px-2 py-1 rounded text-[11px] font-medium bg-rose-500/10 text-rose-400 border border-rose-500/20">
                  Critical
                </span>
              </div>

              <div className="p-3.5 rounded-xl bg-slate-800/40 border border-slate-800 flex items-center justify-between">
                <div>
                  <div className="text-xs text-slate-400">Abandoned Checkouts</div>
                  <div className="text-base font-semibold text-white">
                    ₹{Number(data?.abandonedRevenue || 0).toLocaleString('en-IN')}
                  </div>
                </div>
                <span className="px-2 py-1 rounded text-[11px] font-medium bg-amber-500/10 text-amber-400 border border-amber-500/20">
                  High Value
                </span>
              </div>

              <div className="p-3.5 rounded-xl bg-slate-800/40 border border-slate-800 flex items-center justify-between">
                <div>
                  <div className="text-xs text-slate-400">Repeat Customer Dormancy</div>
                  <div className="text-base font-semibold text-white">
                    {data?.recentOpportunities?.filter(o => o.type === 'REPEAT_CUSTOMER_REENGAGEMENT').length || 0} flagged
                  </div>
                </div>
                <span className="px-2 py-1 rounded text-[11px] font-medium bg-teal-500/10 text-teal-400 border border-teal-500/20">
                  Retention
                </span>
              </div>
            </div>
          </div>

          <button
            onClick={() => navigate('/opportunities')}
            className="w-full mt-6 py-2.5 px-4 rounded-xl text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 flex items-center justify-center gap-2 transition-all"
          >
            <span>View All Opportunities</span>
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Recovery Opportunities Table (Section 10) */}
      <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-base font-semibold text-white">High Priority Recovery Opportunities</h2>
            <p className="text-xs text-slate-400">Opportunities automatically prioritized by deterministic recovery score</p>
          </div>
          <button
            onClick={() => navigate('/opportunities')}
            className="text-xs text-teal-400 hover:text-teal-300 font-medium flex items-center gap-1"
          >
            <span>See all ({data?.activeOpportunitiesCount || 0})</span>
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-800 text-[11px] uppercase tracking-wider text-slate-400">
                <th className="py-3 px-4">Customer</th>
                <th className="py-3 px-4">Opportunity Type</th>
                <th className="py-3 px-4">Amount</th>
                <th className="py-3 px-4">Recovery Score</th>
                <th className="py-3 px-4">AI Recommendation</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 text-sm">
              {data?.recentOpportunities && data.recentOpportunities.length > 0 ? (
                data.recentOpportunities.map((opp) => {
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
                      {/* Customer */}
                      <td className="py-3.5 px-4 font-medium text-white">
                        <div>{opp.customerName}</div>
                        <div className="text-xs text-slate-400 font-normal">{opp.customerEmail}</div>
                      </td>

                      {/* Type */}
                      <td className="py-3.5 px-4">
                        <span className={`inline-flex px-2 py-0.5 rounded text-xs font-medium border ${typeBadgeClass}`}>
                          {typeLabel}
                        </span>
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

                      {/* Recommendation snippet */}
                      <td className="py-3.5 px-4 text-xs text-slate-300 max-w-xs truncate">
                        {opp.aiRecommendation || 'Click Review to generate AI recovery strategy.'}
                      </td>

                      {/* Status */}
                      <td className="py-3.5 px-4">
                        <span className={`inline-flex px-2 py-0.5 rounded text-xs font-medium border ${statusBadge}`}>
                          {opp.status}
                        </span>
                      </td>

                      {/* Action */}
                      <td className="py-3.5 px-4 text-right" onClick={(e) => e.stopPropagation()}>
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => navigate(`/opportunities/${opp.id}`)}
                            className="px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 transition-colors"
                          >
                            Review
                          </button>
                          {opp.status === 'NEW' || opp.status === 'REVIEWED' ? (
                            <button
                              onClick={(e) => handleQuickApprove(e, opp.id)}
                              disabled={approvingId === opp.id}
                              className="px-3 py-1.5 rounded-lg text-xs font-semibold bg-teal-500/20 hover:bg-teal-500/30 text-teal-300 border border-teal-500/40 transition-colors"
                            >
                              {approvingId === opp.id ? 'Approving...' : 'Approve'}
                            </button>
                          ) : opp.paymentLink ? (
                            <a
                              href={opp.paymentLink}
                              target="_blank"
                              rel="noreferrer"
                              className="px-2.5 py-1.5 rounded-lg text-xs font-medium bg-emerald-500/15 text-emerald-300 hover:bg-emerald-500/25 flex items-center gap-1"
                            >
                              <span>Link</span>
                              <ExternalLink className="w-3 h-3" />
                            </a>
                          ) : null}
                        </div>
                      </td>
                    </tr>
                  );
                })
              ) : (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-slate-400">
                    No active opportunities. Click "Simulate Revenue Leak" or "Load Demo Data" above.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Recent Audit Activity Stream (Section 14) */}
      <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-base font-semibold text-white">Recent Audit Activity</h2>
            <p className="text-xs text-slate-400">Immutable audit log of all system detections, merchant approvals, and gateway events</p>
          </div>
          <button
            onClick={() => navigate('/activity')}
            className="text-xs text-teal-400 hover:text-teal-300 font-medium flex items-center gap-1"
          >
            <span>Full Audit Trail</span>
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>

        <div className="space-y-3">
          {data?.recentActivities && data.recentActivities.length > 0 ? (
            data.recentActivities.map((act) => {
              const actorBadge = {
                SYSTEM: 'text-slate-400 bg-slate-800 border-slate-700',
                MERCHANT: 'text-teal-300 bg-teal-950/40 border-teal-500/30',
                AI_AGENT: 'text-purple-300 bg-purple-950/40 border-purple-500/30',
              }[act.actor] || 'text-slate-400 bg-slate-800';

              return (
                <div
                  key={act.id}
                  className="flex items-start justify-between p-3 rounded-xl bg-slate-900/40 border border-slate-800/80 text-xs"
                >
                  <div className="flex items-start gap-3">
                    <div className="p-1.5 rounded-lg bg-slate-800 text-slate-300 mt-0.5">
                      <Clock className="w-3.5 h-3.5" />
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-white">{act.action}</span>
                        <span className={`px-1.5 py-0.5 rounded text-[10px] font-semibold border ${actorBadge}`}>
                          {act.actor}
                        </span>
                      </div>
                      <p className="text-slate-300 mt-0.5">{act.details}</p>
                    </div>
                  </div>
                  <span className="text-slate-500 font-mono text-[11px] whitespace-nowrap ml-4">
                    {new Date(act.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                  </span>
                </div>
              );
            })
          ) : (
            <p className="text-xs text-slate-500 py-3">No activity recorded yet.</p>
          )}
        </div>
      </div>
    </div>
  );
};
