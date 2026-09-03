import React, { useState, useEffect } from 'react';
import { Receipt, Search, RefreshCw } from 'lucide-react';
import api from '../services/api';
import { useToast } from '../components/Toast';

export const TransactionsPage = () => {
  const { addToast } = useToast();
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [searchTerm, setSearchTerm] = useState('');

  const fetchTransactions = async () => {
    try {
      setLoading(true);
      const data = await api.getTransactions(
        statusFilter !== 'ALL' ? { status: statusFilter } : {}
      );
      setTransactions(data);
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to fetch transactions', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTransactions();
  }, [statusFilter]);

  const filtered = transactions.filter((tx) => {
    if (!searchTerm) return true;
    const term = searchTerm.toLowerCase();
    return (
      tx.orderId?.toLowerCase().includes(term) ||
      tx.transactionType?.toLowerCase().includes(term) ||
      tx.status?.toLowerCase().includes(term)
    );
  });

  const statuses = ['ALL', 'SUCCESS', 'FAILED', 'ABANDONED'];

  return (
    <div className="space-y-6 p-8 max-w-7xl mx-auto">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Merchant Transactions Ledger</h1>
          <p className="text-sm text-slate-400 mt-1">
            Complete feed of gateway transactions used by the recovery detection engine.
          </p>
        </div>
        <button
          onClick={fetchTransactions}
          className="flex items-center gap-2 px-3 py-2 rounded-xl text-xs font-medium bg-slate-900 border border-slate-800 text-slate-300 hover:text-white transition-all self-start md:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Filter Tabs & Search */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-1.5 overflow-x-auto">
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
              {s}
            </button>
          ))}
        </div>

        <div className="relative w-full sm:w-64">
          <Search className="w-4 h-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search order ID, channel..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-3 py-1.5 rounded-xl bg-slate-900/80 border border-slate-800 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-teal-500"
          />
        </div>
      </div>

      {/* Ledger Table */}
      <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-800 text-[11px] uppercase tracking-wider text-slate-400">
                <th className="py-3 px-4">Transaction ID</th>
                <th className="py-3 px-4">Order ID</th>
                <th className="py-3 px-4">Customer ID</th>
                <th className="py-3 px-4">Channel</th>
                <th className="py-3 px-4">Amount</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4 text-right">Timestamp</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 text-sm">
              {filtered.length > 0 ? (
                filtered.map((tx) => {
                  const statusBadge = {
                    SUCCESS: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
                    FAILED: 'bg-rose-500/10 text-rose-400 border-rose-500/20',
                    ABANDONED: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
                    PENDING: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
                  }[tx.status] || 'bg-slate-800 text-slate-400';

                  return (
                    <tr key={tx.id} className="hover:bg-slate-800/40 transition-colors">
                      <td className="py-3.5 px-4 font-mono text-xs text-slate-300">#{tx.id}</td>
                      <td className="py-3.5 px-4 font-mono text-xs text-teal-400">{tx.orderId || 'N/A'}</td>
                      <td className="py-3.5 px-4 text-slate-300">Customer #{tx.customerId}</td>
                      <td className="py-3.5 px-4 text-slate-300 font-mono text-xs">{tx.transactionType}</td>
                      <td className="py-3.5 px-4 font-bold text-white">
                        ₹{Number(tx.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                      </td>
                      <td className="py-3.5 px-4">
                        <span className={`inline-flex px-2 py-0.5 rounded text-xs font-semibold border ${statusBadge}`}>
                          {tx.status}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-right text-xs text-slate-400 font-mono">
                        {tx.transactionDate ? new Date(tx.transactionDate).toLocaleString() : 'N/A'}
                      </td>
                    </tr>
                  );
                })
              ) : (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-slate-400">
                    {loading ? 'Loading ledger...' : 'No transactions found.'}
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
