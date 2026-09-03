import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Sparkles,
  ShieldCheck,
  CheckCircle,
  AlertOctagon,
  Copy,
  ExternalLink,
  User,
  CreditCard,
  History,
  Clock,
  Send,
  XCircle,
  Zap,
} from 'lucide-react';
import api from '../services/api';
import { useToast } from '../components/Toast';

export const OpportunityDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToast } = useToast();

  const [opp, setOpp] = useState(null);
  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [analyzing, setAnalyzing] = useState(false);
  const [approving, setApproving] = useState(false);
  const [dismissing, setDismissing] = useState(false);

  const fetchOpportunity = async () => {
    try {
      setLoading(true);
      const data = await api.getOpportunity(id);
      setOpp(data);
      const logs = await api.getAuditEvents(id);
      setAuditLogs(logs);
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to load opportunity', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOpportunity();
  }, [id]);

  const handleGenerateAi = async () => {
    try {
      setAnalyzing(true);
      const updated = await api.analyzeOpportunity(id);
      setOpp(updated);
      const logs = await api.getAuditEvents(id);
      setAuditLogs(logs);
      addToast('AI Recovery Analysis generated successfully!', 'success');
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to generate AI analysis', 'error');
    } finally {
      setAnalyzing(false);
    }
  };

  const handleApprove = async () => {
    try {
      setApproving(true);
      const res = await api.approveOpportunity(id);
      addToast('Recovery action approved! Payment link generated.', 'success');
      fetchOpportunity();
    } catch (err) {
      const msg = err.response?.data?.message || 'Payment link generation failed';
      addToast(msg, 'error');
      // Refresh to reflect failed status in audit/state
      fetchOpportunity();
    } finally {
      setApproving(false);
    }
  };

  const handleDismiss = async () => {
    try {
      setDismissing(true);
      const updated = await api.dismissOpportunity(id);
      setOpp(updated);
      addToast('Opportunity dismissed.', 'info');
      fetchOpportunity();
    } catch (err) {
      addToast(err.response?.data?.message || 'Failed to dismiss opportunity', 'error');
    } finally {
      setDismissing(false);
    }
  };

  const copyToClipboard = (text, label = 'Text') => {
    navigator.clipboard.writeText(text);
    addToast(`${label} copied to clipboard!`, 'info');
  };

  if (loading && !opp) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="flex flex-col items-center gap-3">
          <div className="h-8 w-8 animate-spin rounded-full border-2 border-teal-500 border-t-transparent"></div>
          <p className="text-sm text-slate-400">Loading opportunity #{id}...</p>
        </div>
      </div>
    );
  }

  if (!opp) {
    return (
      <div className="p-8 text-center">
        <p className="text-slate-400">Opportunity not found.</p>
        <button
          onClick={() => navigate('/opportunities')}
          className="mt-4 px-4 py-2 rounded-xl text-xs font-semibold bg-slate-800 text-slate-200"
        >
          Back to Opportunities
        </button>
      </div>
    );
  }

  const typeLabel = {
    FAILED_PAYMENT_RECOVERY: 'Failed Payment Recovery',
    HIGH_VALUE_CHECKOUT_RECOVERY: 'High-Value Checkout Recovery',
    REPEAT_CUSTOMER_REENGAGEMENT: 'Repeat Customer Re-engagement',
  }[opp.type] || opp.type;

  const isApproved = opp.status === 'RECOVERY_CREATED' || opp.status === 'APPROVED';
  const isDismissed = opp.status === 'DISMISSED';
  const isFailed = opp.status === 'FAILED';

  return (
    <div className="space-y-6 p-8 max-w-5xl mx-auto">
      {/* Back Button & Header */}
      <div>
        <button
          onClick={() => navigate('/opportunities')}
          className="inline-flex items-center gap-1.5 text-xs text-slate-400 hover:text-slate-200 mb-4 transition-colors"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Back to Opportunities</span>
        </button>

        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2.5">
              <h1 className="text-2xl font-bold text-white">Opportunity #{opp.id}</h1>
              <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-teal-500/15 text-teal-300 border border-teal-500/30">
                {typeLabel}
              </span>
              <span className={`px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                isApproved
                  ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30'
                  : isFailed
                  ? 'bg-rose-500/15 text-rose-400 border border-rose-500/30'
                  : isDismissed
                  ? 'bg-slate-800 text-slate-500 border border-slate-700'
                  : 'bg-blue-500/15 text-blue-400 border border-blue-500/30'
              }`}>
                {opp.status}
              </span>
            </div>
            <p className="text-xs text-slate-400 mt-1">
              Flagged by deterministic revenue leak detection on {new Date(opp.createdAt).toLocaleString()}
            </p>
          </div>

          {/* Action Header Buttons */}
          <div className="flex items-center gap-2">
            {!isApproved && !isDismissed && (
              <button
                onClick={handleDismiss}
                disabled={dismissing}
                className="px-4 py-2 rounded-xl text-xs font-semibold bg-slate-900 border border-slate-800 text-slate-400 hover:text-rose-400 hover:border-rose-500/30 transition-all"
              >
                {dismissing ? 'Dismissing...' : 'Dismiss'}
              </button>
            )}

            {!isApproved && !isDismissed && (
              <button
                onClick={handleApprove}
                disabled={approving}
                className="flex items-center gap-2 px-5 py-2 rounded-xl text-xs font-bold bg-gradient-to-r from-teal-500 to-emerald-500 text-slate-950 hover:from-teal-400 hover:to-emerald-400 shadow-md shadow-teal-500/20 active:scale-95 transition-all"
              >
                <ShieldCheck className="w-4 h-4" />
                <span>{approving ? 'Approving...' : 'Approve Recovery'}</span>
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Failure Banner if Gateway Failed */}
      {isFailed && (
        <div className="p-4 rounded-2xl bg-rose-950/30 border border-rose-500/40 text-rose-200 flex items-start gap-3 shadow-lg shadow-rose-950/20">
          <AlertOctagon className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
          <div className="flex-1">
            <h3 className="font-semibold text-sm text-rose-300">Payment Link Generation Failed</h3>
            <p className="text-xs text-rose-300/80 mt-1">
              {opp.failureReason || 'Razorpay payment link creation failed. No customer charge was completed.'}
            </p>
            <div className="mt-3 flex items-center gap-2">
              <button
                onClick={handleApprove}
                disabled={approving}
                className="px-3 py-1.5 rounded-lg text-xs font-semibold bg-rose-500/20 hover:bg-rose-500/30 text-rose-200 border border-rose-500/40 transition-colors"
              >
                Retry Approval
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Successful Razorpay Payment Link Banner (Section 11, 12, 13) */}
      {isApproved && opp.paymentLink && (
        <div className="p-6 rounded-2xl bg-gradient-to-br from-emerald-950/40 via-slate-900 to-slate-900 border border-emerald-500/40 shadow-xl relative overflow-hidden">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <CheckCircle className="w-5 h-5 text-emerald-400" />
                <h3 className="text-base font-bold text-white">Razorpay Test Mode Payment Link Active</h3>
                <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                  READY
                </span>
              </div>
              <p className="text-xs text-slate-300">
                Payment link generated and verified by backend. Send directly to customer or embed in outreach.
              </p>
              <div className="flex items-center gap-2 pt-2">
                <span className="text-xs text-slate-400">Reference:</span>
                <code className="text-xs font-mono bg-slate-950/80 px-2 py-0.5 rounded text-emerald-300 border border-emerald-500/20">
                  {opp.razorpayReference}
                </code>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={() => copyToClipboard(opp.paymentLink, 'Payment Link')}
                className="flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 transition-colors border border-slate-700"
              >
                <Copy className="w-3.5 h-3.5" />
                <span>Copy Link</span>
              </button>
              <a
                href={opp.paymentLink}
                target="_blank"
                rel="noreferrer"
                className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-bold bg-emerald-500 hover:bg-emerald-400 text-slate-950 transition-colors shadow-md shadow-emerald-500/20"
              >
                <span>Test Link</span>
                <ExternalLink className="w-3.5 h-3.5" />
              </a>
            </div>
          </div>

          <div className="mt-4 p-2.5 rounded-xl bg-slate-950/60 border border-slate-800 flex items-center justify-between text-xs">
            <span className="text-slate-400 font-mono truncate mr-2">{opp.paymentLink}</span>
          </div>
        </div>
      )}

      {/* Grid: Customer & Transaction Details */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Customer Snapshot */}
        <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4">
          <div className="flex items-center gap-2 text-slate-300 font-semibold text-sm">
            <User className="w-4 h-4 text-teal-400" />
            <span>Customer Profile</span>
          </div>
          <div className="space-y-2 text-xs">
            <div className="flex justify-between py-1 border-b border-slate-800/60">
              <span className="text-slate-400">Name</span>
              <span className="font-semibold text-white">{opp.customerName}</span>
            </div>
            <div className="flex justify-between py-1 border-b border-slate-800/60">
              <span className="text-slate-400">Email</span>
              <span className="text-slate-200 font-mono">{opp.customerEmail}</span>
            </div>
            <div className="flex justify-between py-1 border-b border-slate-800/60">
              <span className="text-slate-400">Phone</span>
              <span className="text-slate-200 font-mono">{opp.customerPhone || 'N/A'}</span>
            </div>
            <div className="flex justify-between py-1 border-b border-slate-800/60">
              <span className="text-slate-400">Historical Orders</span>
              <span className="font-semibold text-white">{opp.customerTotalOrders || 0}</span>
            </div>
            <div className="flex justify-between py-1">
              <span className="text-slate-400">Lifetime Spent</span>
              <span className="font-bold text-teal-400">
                ₹{Number(opp.customerTotalSpent || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
              </span>
            </div>
          </div>
        </div>

        {/* Transaction Snapshot & Recovery Score */}
        <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-slate-300 font-semibold text-sm">
              <CreditCard className="w-4 h-4 text-teal-400" />
              <span>Financial Snapshot</span>
            </div>
            <span className="text-lg font-bold text-white">
              ₹{Number(opp.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
            </span>
          </div>

          {/* Recovery Score Bar */}
          <div className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800 space-y-2">
            <div className="flex items-center justify-between text-xs">
              <span className="font-semibold text-slate-300">Deterministic Recovery Score</span>
              <span className="font-bold text-sm text-teal-300">{opp.recoveryScore}/100</span>
            </div>
            <div className="w-full h-2.5 rounded-full bg-slate-800 overflow-hidden">
              <div
                className={`h-full rounded-full transition-all duration-500 ${
                  opp.recoveryScore >= 75
                    ? 'bg-emerald-400'
                    : opp.recoveryScore >= 50
                    ? 'bg-amber-400'
                    : 'bg-slate-400'
                }`}
                style={{ width: `${opp.recoveryScore}%` }}
              ></div>
            </div>
            <p className="text-[11px] text-slate-400">
              Calculated using transaction recency, historical order frequency, channel reliability, and basket value.
            </p>
          </div>

          <div className="space-y-1.5 text-xs text-slate-300">
            <div className="font-medium text-slate-400">Detection Reason:</div>
            <p className="text-slate-300 bg-slate-800/30 p-2 rounded-lg border border-slate-800">
              {opp.reason}
            </p>
          </div>
        </div>
      </div>

      {/* AI Recommendation Layer (Section 9) */}
      <div className="p-6 rounded-2xl bg-gradient-to-br from-slate-900 via-slate-900/90 to-purple-950/20 border border-purple-500/20 shadow-lg space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-purple-500/20 text-purple-400 border border-purple-500/30">
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-base font-bold text-white">Gemini AI Recovery Reasoning</h2>
                {opp.confidence && (
                  <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-purple-500/20 text-purple-300 border border-purple-500/30">
                    {opp.confidence} CONFIDENCE
                  </span>
                )}
              </div>
              <p className="text-xs text-slate-400">Contextual analysis generated strictly from supplied transaction records</p>
            </div>
          </div>

          <button
            onClick={handleGenerateAi}
            disabled={analyzing}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-semibold bg-purple-600/30 hover:bg-purple-600/40 text-purple-200 border border-purple-500/40 transition-all self-start sm:self-auto"
          >
            <Sparkles className={`w-3.5 h-3.5 ${analyzing ? 'animate-spin' : ''}`} />
            <span>{analyzing ? 'Analyzing with Gemini...' : opp.aiExplanation ? 'Re-generate AI Analysis' : 'Generate AI Analysis'}</span>
          </button>
        </div>

        {opp.aiExplanation ? (
          <div className="space-y-4 text-xs">
            {/* Why at risk */}
            <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800 space-y-1">
              <div className="font-semibold text-slate-300 text-xs">Why This Revenue is at Risk:</div>
              <p className="text-slate-300 leading-relaxed">{opp.aiExplanation}</p>
            </div>

            {/* Recommended Action */}
            <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800 space-y-1">
              <div className="font-semibold text-teal-400 text-xs">Recommended Merchant Action:</div>
              <p className="text-slate-300 leading-relaxed">{opp.aiRecommendation}</p>
            </div>

            {/* Suggested Outreach Message */}
            {opp.suggestedMessage && (
              <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="font-semibold text-slate-300">Suggested Customer Message:</span>
                  <button
                    onClick={() => {
                      const finalMsg = opp.suggestedMessage.replace(
                        '{{PAYMENT_LINK}}',
                        opp.paymentLink || '[Razorpay Link Generated on Approval]'
                      );
                      copyToClipboard(finalMsg, 'Customer outreach message');
                    }}
                    className="flex items-center gap-1 text-[11px] text-teal-400 hover:text-teal-300"
                  >
                    <Copy className="w-3 h-3" />
                    <span>Copy Outreach</span>
                  </button>
                </div>
                <div className="p-3 rounded-lg bg-slate-900 border border-slate-800/80 text-slate-300 font-sans italic">
                  "{opp.suggestedMessage.replace('{{PAYMENT_LINK}}', opp.paymentLink || '{{PAYMENT_LINK}}')}"
                </div>
              </div>
            )}
          </div>
        ) : (
          <div className="p-6 rounded-xl bg-slate-950/40 border border-slate-800/80 text-center space-y-2">
            <p className="text-xs text-slate-400">
              No AI analysis generated yet. Click "Generate AI Analysis" to synthesize customer behavior and generate outreach.
            </p>
          </div>
        )}
      </div>

      {/* Opportunity Audit Trail (Section 14) */}
      <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4">
        <div className="flex items-center gap-2 text-slate-300 font-semibold text-sm">
          <History className="w-4 h-4 text-teal-400" />
          <span>Opportunity Audit History</span>
        </div>

        <div className="space-y-3">
          {auditLogs.length > 0 ? (
            auditLogs.map((log) => {
              const actorBadge = {
                SYSTEM: 'text-slate-400 bg-slate-800 border-slate-700',
                MERCHANT: 'text-teal-300 bg-teal-950/40 border-teal-500/30',
                AI_AGENT: 'text-purple-300 bg-purple-950/40 border-purple-500/30',
              }[log.actor] || 'text-slate-400 bg-slate-800';

              return (
                <div
                  key={log.id}
                  className="flex items-start justify-between p-3 rounded-xl bg-slate-950/40 border border-slate-800/80 text-xs"
                >
                  <div className="flex items-start gap-3">
                    <div className="p-1.5 rounded-lg bg-slate-800 text-slate-300 mt-0.5">
                      <Clock className="w-3.5 h-3.5" />
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-white">{log.action}</span>
                        <span className={`px-1.5 py-0.5 rounded text-[10px] font-semibold border ${actorBadge}`}>
                          {log.actor}
                        </span>
                      </div>
                      <p className="text-slate-300 mt-0.5">{log.details}</p>
                    </div>
                  </div>
                  <span className="text-slate-500 font-mono text-[11px] whitespace-nowrap ml-4">
                    {new Date(log.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                  </span>
                </div>
              );
            })
          ) : (
            <p className="text-xs text-slate-500">No audit events recorded for this opportunity.</p>
          )}
        </div>
      </div>
    </div>
  );
};
