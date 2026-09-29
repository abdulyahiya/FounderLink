import React, { useCallback, useEffect, useState } from 'react';
import { DollarSign, CheckCircle, XCircle, Clock, Copy } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Layout from '../../shared/components/Layout';
import useAuth from '../../shared/hooks/useAuth';
import { getPaymentsByFounder, acceptPayment, rejectPayment } from '../../core/api/paymentApi';
import { Payment } from '../../types';

const shimmer = {
  background: 'linear-gradient(90deg, var(--surface-2) 25%, var(--surface-3) 50%, var(--surface-2) 75%)',
  backgroundSize: '200% 100%', animation: 'shimmer 1.4s infinite',
};

const FounderInvestments: React.FC = () => {
  const { user } = useAuth();
  const [payments, setPayments] = useState<Payment[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  const load = useCallback(() => {
    if (!user?.userId) return;
    setLoading(true);
    getPaymentsByFounder(user.userId)
      .then((res) => setPayments(res.data || []))
      .catch(() => toast.error('Failed to load investment requests'))
      .finally(() => setLoading(false));
  }, [user]);

  useEffect(() => { load(); }, [load]);

  const handleAccept = async (paymentId: number): Promise<void> => {
    setActionLoading(paymentId + '_accept');
    try { await acceptPayment(paymentId); toast.success('Investment accepted!'); load(); }
    catch (err: any) { toast.error(err.response?.data?.error || 'Failed to accept'); }
    finally { setActionLoading(null); }
  };

  const handleReject = async (paymentId: number): Promise<void> => {
    setActionLoading(paymentId + '_reject');
    try { await rejectPayment(paymentId); toast.success('Investment rejected. Refund initiated.'); load(); }
    catch (err: any) { toast.error(err.response?.data?.error || 'Failed to reject'); }
    finally { setActionLoading(null); }
  };

  const pending  = payments.filter((p) => p.status === 'AWAITING_APPROVAL');
  const confirmed = payments.filter((p) => p.status === 'SUCCESS');
  const rejected = payments.filter((p) => p.status === 'REJECTED');
  const totalReceived = confirmed.reduce((sum, p) => sum + Number(p.amount), 0);
  const uniqueInvestors = new Set(confirmed.map((p) => p.investorId)).size;

  const statusBadge = (status: string) => {
    if (status === 'SUCCESS') return <span className="badge-green">Confirmed</span>;
    if (status === 'REJECTED') return <span className="badge-red">Rejected</span>;
    return <span className="badge-yellow">Pending</span>;
  };

  const metrics = [
    { label: 'Total received', value: `₹${totalReceived.toLocaleString('en-IN')}`, accent: 'var(--green)' },
    { label: 'Unique investors', value: uniqueInvestors, accent: 'var(--brand)' },
    { label: 'Pending review', value: pending.length, accent: 'var(--amber)' },
    { label: 'Rejected', value: rejected.length, accent: 'var(--red)' },
  ];

  return (
    <Layout>
      <style>{`@keyframes shimmer { 0% { background-position: 200% 0 } 100% { background-position: -200% 0 } }`}</style>
      <div style={{ maxWidth: 960, margin: '0 auto' }}>
        <div style={{ marginBottom: 28 }}>
          <span style={{ fontSize: 11, fontWeight: 600, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--brand)', display: 'block', marginBottom: 4 }}>Founder</span>
          <h1 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 28, fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>Investment requests</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: 14, marginTop: 4 }}>Review and accept or reject incoming investments</p>
        </div>

        {/* Metrics */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, overflow: 'hidden', boxShadow: 'var(--shadow-card)', marginBottom: 28 }}>
          {metrics.map((m, i) => (
            <div key={i} style={{ padding: '20px 24px', borderRight: i < metrics.length - 1 ? '1px solid var(--border)' : 'none' }}>
              <p style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 6 }}>{m.label}</p>
              <p style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 24, fontWeight: 700, color: m.accent, lineHeight: 1 }}>{m.value}</p>
            </div>
          ))}
        </div>

        {/* List */}
        <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, overflow: 'hidden', boxShadow: 'var(--shadow-card)' }}>
          <div style={{ padding: '16px 24px', borderBottom: '1px solid var(--border)' }}>
            <h2 style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-secondary)', margin: 0 }}>All requests ({payments.length})</h2>
          </div>
          {loading ? (
            <div style={{ padding: 24 }}>
              {[1,2,3].map(i => <div key={i} style={{ height: 64, borderRadius: 12, marginBottom: 8, ...shimmer }} />)}
            </div>
          ) : payments.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '60px 24px' }}>
              <div style={{ width: 40, height: 40, borderRadius: 10, background: 'var(--brand-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 12px' }}>
                <DollarSign size={20} style={{ color: 'var(--brand)' }} />
              </div>
              <p style={{ fontSize: 14, fontWeight: 500, color: 'var(--text-primary)', marginBottom: 4 }}>No investment requests yet</p>
              <p style={{ fontSize: 12, color: 'var(--text-muted)' }}>Investor requests will appear here once your startup is approved</p>
            </div>
          ) : (
            payments.map((p, idx) => (
              <div key={p.id} style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '14px 24px', borderBottom: idx < payments.length - 1 ? '1px solid var(--border)' : 'none', flexWrap: 'wrap' }}>
                <div style={{ width: 38, height: 38, borderRadius: 10, background: 'var(--brand-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, fontSize: 13, fontWeight: 700, color: 'var(--brand)' }}>
                  {(p.investorName || 'I')[0].toUpperCase()}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <p style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)', marginBottom: 2 }}>{p.investorName}</p>
                  <p style={{ fontSize: 11, color: 'var(--text-muted)' }}>{p.startupName} · {new Date(p.createdAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}</p>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
                  <span style={{ fontSize: 14, fontWeight: 700, color: 'var(--text-primary)' }}>₹{Number(p.amount).toLocaleString('en-IN')}</span>
                  {statusBadge(p.status)}
                  {p.status === 'AWAITING_APPROVAL' && (
                    <>
                      <button
                        onClick={() => handleAccept(p.id)}
                        disabled={!!actionLoading}
                        style={{ padding: '5px 12px', borderRadius: 7, background: 'var(--green-bg)', border: '1px solid rgba(22,163,74,0.2)', color: 'var(--green)', fontSize: 12, fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 4 }}
                      >
                        <CheckCircle size={12} /> Accept
                      </button>
                      <button
                        onClick={() => handleReject(p.id)}
                        disabled={!!actionLoading}
                        style={{ padding: '5px 12px', borderRadius: 7, background: 'var(--red-bg)', border: '1px solid rgba(220,38,38,0.2)', color: 'var(--red)', fontSize: 12, fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 4 }}
                      >
                        <XCircle size={12} /> Reject
                      </button>
                    </>
                  )}
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </Layout>
  );
};

export default FounderInvestments;
