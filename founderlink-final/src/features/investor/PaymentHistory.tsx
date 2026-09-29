import React, { useEffect, useState } from 'react';
import { CreditCard, Copy, CheckCircle, XCircle, Clock } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Layout from '../../shared/components/Layout';
import useAuth from '../../shared/hooks/useAuth';
import { getPaymentsByInvestor } from '../../core/api/paymentApi';
import { Payment } from '../../types';

const shimmer = {
  background: 'linear-gradient(90deg, var(--surface-2) 25%, var(--surface-3) 50%, var(--surface-2) 75%)',
  backgroundSize: '200% 100%', animation: 'shimmer 1.4s infinite',
};

const PaymentHistory: React.FC = () => {
  const { user } = useAuth();
  const [payments, setPayments] = useState<Payment[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    if (user?.userId) {
      getPaymentsByInvestor(user.userId)
        .then(res => setPayments(res.data || []))
        .catch(() => toast.error('Failed to load payment history'))
        .finally(() => setLoading(false));
    }
  }, [user]);

  const copyId = (id: string) => { navigator.clipboard.writeText(id); toast.success('Copied!'); };

  const successCount = payments.filter((p: Payment) => p.status === 'SUCCESS').length;
  const failedCount  = payments.filter((p: Payment) => p.status === 'FAILED' || p.status === 'REJECTED').length;
  const totalPaid    = payments.filter((p: Payment) => p.status === 'SUCCESS').reduce((s: number, p: Payment) => s + Number(p.amount), 0);

  const metrics = [
    { label: 'Amount paid', value: `₹${totalPaid.toLocaleString('en-IN')}`, accent: 'var(--green)', sub: 'successful only' },
    { label: 'Total transactions', value: payments.length, accent: 'var(--text-primary)', sub: 'all statuses' },
    { label: 'Success / Failed', value: `${successCount} / ${failedCount}`, accent: 'var(--brand)', sub: 'transaction outcome' },
  ];

  const statusConfig: Record<string, { icon: React.ReactNode; label: string; cls: string }> = {
    SUCCESS:  { icon: <CheckCircle size={12} />, label: 'Success',  cls: 'badge-green' },
    FAILED:   { icon: <XCircle size={12} />,    label: 'Failed',   cls: 'badge-red'   },
    REJECTED: { icon: <XCircle size={12} />,    label: 'Rejected', cls: 'badge-red'   },
  };
  const getStatus = (s: string) => statusConfig[s] ?? { icon: <Clock size={12} />, label: 'Pending', cls: 'badge-yellow' };

  return (
    <Layout>
      <style>{`
        @keyframes shimmer { 0%{background-position:200% 0} 100%{background-position:-200% 0} }
        @keyframes ph-fade-up { from{opacity:0;transform:translateY(14px)} to{opacity:1;transform:translateY(0)} }
        .ph-page { animation: ph-fade-up 0.38s cubic-bezier(0.22,1,0.36,1) both; }
        .ph-row {
          display: grid;
          grid-template-columns: 36px 1fr auto;
          align-items: center; gap: 14px;
          padding: 14px 24px;
          transition: background 0.15s;
        }
        .ph-row:hover { background: var(--surface-2); }
        .ph-copy-btn {
          background: var(--surface-3); border: 1px solid var(--border);
          border-radius: 6px; padding: 3px 7px; cursor: pointer;
          color: var(--text-faint); display: inline-flex; align-items: center; gap: 4px;
          font-size: 10px; transition: all 0.15s;
        }
        .ph-copy-btn:hover { color: var(--text-primary); border-color: var(--text-muted); }
      `}</style>

      <div className="ph-page" style={{ maxWidth: 960, margin: '0 auto' }}>
        <div style={{ marginBottom: 28 }}>
          <span style={{ fontSize: 11, fontWeight: 600, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--green)', display: 'block', marginBottom: 4 }}>Investor</span>
          <h1 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 28, fontWeight: 700, color: 'var(--text-primary)', margin: 0, letterSpacing: '-0.4px' }}>Payment History</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: 14, marginTop: 4 }}>Razorpay transaction ledger</p>
        </div>

        {/* ── Metrics strip ── */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 20, overflow: 'hidden', boxShadow: 'var(--shadow-card)', marginBottom: 28 }}>
          {metrics.map((m, i) => (
            <div key={i} style={{ padding: '22px 24px', borderRight: i < metrics.length - 1 ? '1px solid var(--border)' : 'none' }}>
              <p style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 6, fontWeight: 500 }}>{m.label}</p>
              <p style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 26, fontWeight: 800, color: m.accent, lineHeight: 1, marginBottom: 4 }}>{m.value}</p>
              <p style={{ fontSize: 10, color: 'var(--text-faint)', margin: 0 }}>{m.sub}</p>
            </div>
          ))}
        </div>

        {/* ── Transaction ledger ── */}
        <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 20, overflow: 'hidden', boxShadow: 'var(--shadow-card)' }}>

          {/* Column header */}
          <div style={{ display: 'grid', gridTemplateColumns: '36px 1fr auto', gap: 14, padding: '10px 24px', borderBottom: '1px solid var(--border)', background: 'var(--surface-2)' }}>
            <div />
            <span style={{ fontSize: 10, fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase', color: 'var(--text-faint)' }}>Transaction</span>
            <span style={{ fontSize: 10, fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase', color: 'var(--text-faint)', textAlign: 'right' }}>Amount</span>
          </div>

          {loading ? (
            <div style={{ padding: 24 }}>
              {[1, 2, 3].map(i => <div key={i} style={{ height: 64, borderRadius: 12, marginBottom: 10, ...shimmer }} />)}
            </div>
          ) : payments.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '64px 24px' }}>
              <div style={{ width: 44, height: 44, borderRadius: 12, background: 'var(--green-bg)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 14px' }}>
                <CreditCard size={20} style={{ color: 'var(--green)' }} />
              </div>
              <p style={{ fontSize: 15, fontWeight: 600, color: 'var(--text-primary)', marginBottom: 6 }}>No transactions yet</p>
              <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>Your Razorpay payment records will appear here</p>
            </div>
          ) : (
            payments.map((p: Payment, idx) => {
              const s = getStatus(p.status);
              const dt = new Date(p.createdAt);
              return (
                <div key={p.id} className="ph-row" style={{ borderBottom: idx < payments.length - 1 ? '1px solid var(--border)' : 'none' }}>
                  {/* Icon */}
                  <div style={{
                    width: 36, height: 36, borderRadius: 10, flexShrink: 0,
                    background: p.status === 'SUCCESS' ? 'var(--green-bg)' : p.status === 'FAILED' || p.status === 'REJECTED' ? 'rgba(239,68,68,0.1)' : 'var(--surface-3)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                  }}>
                    <CreditCard size={15} style={{ color: p.status === 'SUCCESS' ? 'var(--green)' : p.status === 'FAILED' || p.status === 'REJECTED' ? '#ef4444' : 'var(--text-muted)' }} />
                  </div>

                  {/* Details */}
                  <div style={{ minWidth: 0 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4, flexWrap: 'wrap' }}>
                      <p style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>{p.startupName}</p>
                      <span className={s.cls} style={{ display: 'inline-flex', alignItems: 'center', gap: 3, borderRadius: 99 }}>
                        {s.icon} {s.label}
                      </span>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                      <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>
                        {dt.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}{' '}
                        <span style={{ color: 'var(--text-faint)' }}>{dt.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })}</span>
                      </span>
                      {p.razorpayPaymentId && (
                        <button className="ph-copy-btn" onClick={() => copyId(p.razorpayPaymentId!)}>
                          <code style={{ fontFamily: 'monospace', fontSize: 10 }}>
                            {p.razorpayPaymentId.slice(0, 18)}…
                          </code>
                          <Copy size={9} />
                        </button>
                      )}
                    </div>
                  </div>

                  {/* Amount */}
                  <div style={{ textAlign: 'right', flexShrink: 0 }}>
                    <p style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 15, fontWeight: 800, color: p.status === 'SUCCESS' ? 'var(--green)' : 'var(--text-muted)', margin: 0 }}>
                      ₹{Number(p.amount).toLocaleString('en-IN')}
                    </p>
                    <p style={{ fontSize: 10, color: 'var(--text-faint)', margin: '2px 0 0' }}>INR</p>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>
    </Layout>
  );
};

export default PaymentHistory;
