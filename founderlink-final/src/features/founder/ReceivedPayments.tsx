import React, { useEffect, useState } from 'react';
import { IndianRupee, Copy } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Layout from '../../shared/components/Layout';
import useAuth from '../../shared/hooks/useAuth';
import { getPaymentsByFounder } from '../../core/api/paymentApi';
import { Payment } from '../../types';

const shimmer = {
  background: 'linear-gradient(90deg, var(--surface-2) 25%, var(--surface-3) 50%, var(--surface-2) 75%)',
  backgroundSize: '200% 100%', animation: 'shimmer 1.4s infinite',
};

const ReceivedPayments: React.FC = () => {
  const { user } = useAuth();
  const [payments, setPayments] = useState<Payment[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    if (user?.userId) {
      getPaymentsByFounder(user.userId)
        .then((res) => setPayments(res.data || []))
        .catch(() => toast.error('Failed to load payments'))
        .finally(() => setLoading(false));
    }
  }, [user]);

  const totalReceived = payments.filter((p) => p.status === 'SUCCESS').reduce((sum, p) => sum + Number(p.amount), 0);
  const uniqueInvestors = new Set(payments.filter((p) => p.status === 'SUCCESS').map((p) => p.investorId)).size;

  const statusBadge = (status: string) => {
    if (status === 'SUCCESS') return <span className="badge-green">Success</span>;
    if (status === 'REJECTED') return <span className="badge-red">Rejected</span>;
    return <span className="badge-yellow">Pending</span>;
  };

  const copyId = (id: string) => { navigator.clipboard.writeText(id); toast.success('Copied!'); };

  const metrics = [
    { label: 'Total received', value: `₹${totalReceived.toLocaleString('en-IN')}`, accent: 'var(--green)' },
    { label: 'Unique investors', value: uniqueInvestors, accent: 'var(--brand)' },
    { label: 'Total transactions', value: payments.length, accent: 'var(--text-primary)' },
  ];

  return (
    <Layout>
      <style>{`@keyframes shimmer { 0% { background-position: 200% 0 } 100% { background-position: -200% 0 } }`}</style>
      <div style={{ maxWidth: 960, margin: '0 auto' }}>
        <div style={{ marginBottom: 28 }}>
          <span style={{ fontSize: 11, fontWeight: 600, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--brand)', display: 'block', marginBottom: 4 }}>Founder</span>
          <h1 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 28, fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>Received payments</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: 14, marginTop: 4 }}>All incoming Razorpay transactions</p>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, overflow: 'hidden', boxShadow: 'var(--shadow-card)', marginBottom: 28 }}>
          {metrics.map((m, i) => (
            <div key={i} style={{ padding: '20px 24px', borderRight: i < metrics.length - 1 ? '1px solid var(--border)' : 'none' }}>
              <p style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 6 }}>{m.label}</p>
              <p style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 24, fontWeight: 700, color: m.accent, lineHeight: 1 }}>{m.value}</p>
            </div>
          ))}
        </div>

        <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, overflow: 'hidden', boxShadow: 'var(--shadow-card)' }}>
          <div style={{ padding: '16px 24px', borderBottom: '1px solid var(--border)' }}>
            <h2 style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-secondary)', margin: 0 }}>Transaction history</h2>
          </div>
          {loading ? (
            <div style={{ padding: 24 }}>
              {[1,2,3].map(i => <div key={i} style={{ height: 64, borderRadius: 12, marginBottom: 8, ...shimmer }} />)}
            </div>
          ) : payments.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '60px 24px' }}>
              <div style={{ width: 40, height: 40, borderRadius: 10, background: 'var(--brand-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 12px' }}>
                <IndianRupee size={20} style={{ color: 'var(--brand)' }} />
              </div>
              <p style={{ fontSize: 14, fontWeight: 500, color: 'var(--text-primary)', marginBottom: 4 }}>No payments received yet</p>
              <p style={{ fontSize: 12, color: 'var(--text-muted)' }}>Payments from investors will appear here</p>
            </div>
          ) : (
            payments.map((p, idx) => (
              <div key={p.id} style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '14px 24px', borderBottom: idx < payments.length - 1 ? '1px solid var(--border)' : 'none', flexWrap: 'wrap' }}>
                <div style={{ width: 38, height: 38, borderRadius: 10, background: 'var(--brand-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, fontSize: 13, fontWeight: 700, color: 'var(--brand)' }}>
                  {(p.investorName || 'I')[0].toUpperCase()}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <p style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)', marginBottom: 2 }}>{p.investorName}</p>
                  <p style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: p.razorpayPaymentId ? 2 : 0 }}>
                    {p.startupName} · {new Date(p.createdAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}
                  </p>
                  {p.razorpayPaymentId && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <code style={{ fontSize: 10, color: 'var(--text-faint)', fontFamily: 'monospace', background: 'var(--surface-2)', padding: '2px 6px', borderRadius: 4 }}>
                        {p.razorpayPaymentId.length > 20 ? p.razorpayPaymentId.slice(0, 20) + '…' : p.razorpayPaymentId}
                      </code>
                      <button onClick={() => copyId(p.razorpayPaymentId!)} style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 2, color: 'var(--text-faint)' }}>
                        <Copy size={11} />
                      </button>
                    </div>
                  )}
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <span style={{ fontSize: 14, fontWeight: 700, color: 'var(--text-primary)' }}>₹{Number(p.amount).toLocaleString('en-IN')}</span>
                  {statusBadge(p.status)}
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </Layout>
  );
};

export default ReceivedPayments;
