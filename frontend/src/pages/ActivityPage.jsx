import React, { useState, useEffect } from 'react';
import { History, Clock, RefreshCw, Filter } from 'lucide-react';
import api from '../services/api';
import { useToast } from '../components/Toast';

export const ActivityPage = () => {
  const { addToast } = useToast();
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actorFilter, setActorFilter] = useState('ALL');

  const fetchAuditEvents = async () => {
    try {
      setLoading(true);
      const data = await api.getAuditEvents();
      setEvents(data);
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to fetch audit trail', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAuditEvents();
  }, []);

  const filtered = events.filter((e) => {
    if (actorFilter === 'ALL') return true;
    return e.actor === actorFilter;
  });

  const actors = ['ALL', 'SYSTEM', 'MERCHANT', 'AI_AGENT'];

  return (
    <div className="space-y-6 p-8 max-w-5xl mx-auto">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">System & Merchant Audit Trail</h1>
          <p className="text-sm text-slate-400 mt-1">
            Immutable log of all autonomous detections, AI outputs, and merchant approval decisions.
          </p>
        </div>
        <button
          onClick={fetchAuditEvents}
          className="flex items-center gap-2 px-3 py-2 rounded-xl text-xs font-medium bg-slate-900 border border-slate-800 text-slate-300 hover:text-white transition-all self-start md:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Actor Filters */}
      <div className="flex items-center gap-2">
        {actors.map((actor) => (
          <button
            key={actor}
            onClick={() => setActorFilter(actor)}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              actorFilter === actor
                ? 'bg-teal-500/20 text-teal-300 border border-teal-500/40'
                : 'bg-slate-900/60 text-slate-400 hover:text-slate-200 border border-slate-800'
            }`}
          >
            {actor}
          </button>
        ))}
      </div>

      {/* Audit Log Timeline */}
      <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4">
        {filtered.length > 0 ? (
          <div className="space-y-3">
            {filtered.map((log) => {
              const actorBadge = {
                SYSTEM: 'text-slate-400 bg-slate-800 border-slate-700',
                MERCHANT: 'text-teal-300 bg-teal-950/40 border-teal-500/30',
                AI_AGENT: 'text-purple-300 bg-purple-950/40 border-purple-500/30',
              }[log.actor] || 'text-slate-400 bg-slate-800';

              const actionColor = {
                OPPORTUNITY_DETECTED: 'text-amber-400',
                AI_ANALYSIS_GENERATED: 'text-purple-400',
                RECOVERY_APPROVED: 'text-teal-400',
                PAYMENT_LINK_CREATED: 'text-emerald-400',
                PAYMENT_LINK_FAILED: 'text-rose-400',
                RECOVERY_DISMISSED: 'text-slate-500',
              }[log.action] || 'text-white';

              return (
                <div
                  key={log.id}
                  className="flex items-start justify-between p-4 rounded-xl bg-slate-950/40 border border-slate-800/80 hover:border-slate-700/80 transition-all text-xs"
                >
                  <div className="flex items-start gap-3">
                    <div className="p-2 rounded-lg bg-slate-800 text-slate-300 mt-0.5">
                      <Clock className="w-4 h-4" />
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className={`font-bold ${actionColor}`}>{log.action}</span>
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold border ${actorBadge}`}>
                          {log.actor}
                        </span>
                        {log.opportunityId && (
                          <span className="text-slate-500 font-mono">
                            Opp #{log.opportunityId}
                          </span>
                        )}
                      </div>
                      <p className="text-slate-300 mt-1 leading-relaxed">{log.details}</p>
                    </div>
                  </div>
                  <div className="text-right text-slate-500 font-mono text-[11px] whitespace-nowrap ml-4">
                    <div>{new Date(log.timestamp).toLocaleDateString()}</div>
                    <div>{new Date(log.timestamp).toLocaleTimeString()}</div>
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="py-12 text-center text-slate-400 text-xs">
            {loading ? 'Loading audit records...' : 'No audit events found.'}
          </div>
        )}
      </div>
    </div>
  );
};
