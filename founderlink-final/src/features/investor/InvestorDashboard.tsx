import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  CheckCircle, Clock, XCircle, TrendingUp, ArrowRight,
} from 'lucide-react';
import Layout from '../../shared/components/Layout';
import useAuth from '../../shared/hooks/useAuth';
import { getAllStartups } from '../../core/api/startupApi';
import { getPaymentsByInvestor } from '../../core/api/paymentApi';
import { Startup, Payment } from '../../types';

const shimmer = {
  background: 'linear-gradient(90deg, var(--surface-2) 25%, var(--surface-3) 50%, var(--surface-2) 75%)',
  backgroundSize: '200% 100%',
  animation: 'shimmer 1.4s infinite',
};

const statusLabel = (status: string): string => {
  if (status === 'SUCCESS') return 'Confirmed';
  if (status === 'AWAITING_APPROVAL') return 'Pending';
  if (status === 'FAILED') return 'Failed';
  if (status === 'REJECTED') return 'Rejected';
  return status;
};

const InvestorDashboard: React.FC = () => {
  const { user } = useAuth();
  const [startups, setStartups] = useState<Startup[]>([]);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [paymentsLoading, setPaymentsLoading] = useState<boolean>(true);

  useEffect(() => {
    getAllStartups(0, 100).then((res) => setStartups(res.data?.content || [])).catch(() => {});
    if (user?.userId) {
      getPaymentsByInvestor(user.userId)
        .then((res) => setPayments(res.data || []))
        .catch(() => {})
        .finally(() => setPaymentsLoading(false));
    }
  }, [user]);

  const successPayments = payments.filter((p: Payment) => p.status === 'SUCCESS');
  const pendingPayments = payments.filter((p: Payment) => p.status === 'AWAITING_APPROVAL');
  const totalInvested = successPayments.reduce((sum: number, p: Payment) => sum + Number(p.amount), 0);
  const uniqueStartups = new Set(successPayments.map((p) => p.startupId)).size;
  const userDisplayName = user?.name || user?.email?.split('@')[0];

  const paymentBadge = (status: string) => {
    if (status === 'SUCCESS') return <span className="badge-green">{statusLabel(status)}</span>;
    if (status === 'FAILED' || status === 'REJECTED') return <span className="badge-red">{statusLabel(status)}</span>;
    return <span className="badge-yellow">{statusLabel(status)}</span>;
  };

  const metrics = [
    { label: 'Total invested', value: paymentsLoading ? null : `₹${totalInvested.toLocaleString('en-IN')}`, accent: 'var(--green)' },
    { label: 'Active investments', value: paymentsLoading ? null : successPayments.length, accent: 'var(--brand)' },
    { label: 'Startups funded', value: paymentsLoading ? null : uniqueStartups, accent: 'var(--brand)' },
    { label: 'Pending', value: paymentsLoading ? null : pendingPayments.length, accent: 'var(--amber)' },
  ];

  const suggestedStartups = startups
    .filter((s) => s.isApproved)
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    .slice(0, 6);

  return (
    <Layout>
      <style>{`@keyframes shimmer { 0% { background-position: 200% 0 } 100% { background-position: -200% 0 } }`}</style>
      <div style={{ maxWidth: 1000, margin: '0 auto' }}>
        {/* Header */}
        <div style={{ marginBottom: 28 }}>
          <span style={{ fontSize: 11, fontWeight: 600, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--green)', display: 'block', marginBottom: 4 }}>Investor</span>
          <h1 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 28, fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>
            Welcome back{userDisplayName ? `, ${userDisplayName}` : ''}
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: 14, marginTop: 4 }}>Overview of your investment activity</p>
        </div>

        {/* Metrics strip */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, overflow: 'hidden', boxShadow: 'var(--shadow-card)', marginBottom: 28 }}>
          {metrics.map((m, i) => (
            <div key={i} style={{ padding: '20px 24px', borderRight: i < metrics.length - 1 ? '1px solid var(--border)' : 'none' }}>
              <p style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 6 }}>{m.label}</p>
              {m.value === null ? (
                <div style={{ height: 28, width: 60, borderRadius: 6, ...shimmer }} />
              ) : (
                <p style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 24, fontWeight: 700, color: m.accent, lineHeight: 1 }}>{m.value}</p>
              )}
            </div>
          ))}
        </div>

        {/* Two columns */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: 20 }}>
          {/* Recent investments */}
          <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, padding: 24, boxShadow: 'var(--shadow-card)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
              <h2 style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-secondary)' }}>Recent investments</h2>
              <Link to="/investor/investments" style={{ fontSize: 12, color: 'var(--brand)', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: 3 }}>
                View all <ArrowRight size={12} />
              </Link>
            </div>
            {paymentsLoading ? (
              [1,2,3].map(i => <div key={i} style={{ height: 56, borderRadius: 12, marginBottom: 8, ...shimmer }} />)
            ) : payments.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '32px 0' }}>
                <div style={{ width: 40, height: 40, borderRadius: 10, background: 'var(--green-bg)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 12px' }}>
                  <TrendingUp size={20} style={{ color: 'var(--green)' }} />
                </div>
                <p style={{ fontSize: 14, fontWeight: 500, color: 'var(--text-primary)', marginBottom: 4 }}>No investments yet</p>
                <Link to="/investor/startups" className="btn-primary" style={{ textDecoration: 'none', fontSize: 13, marginTop: 12 }}>Browse startups</Link>
              </div>
            ) : (
              payments.slice(0, 5).map((p: Payment) => (
                <div key={p.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 14px', borderRadius: 12, border: '1px solid var(--border)', marginBottom: 8 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                    <div style={{ width: 36, height: 36, borderRadius: 10, background: 'var(--green-bg)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, fontSize: 13, fontWeight: 600, color: 'var(--green)' }}>
                      {(p.startupName || 'S')[0].toUpperCase()}
                    </div>
                    <div>
                      <p style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-primary)', marginBottom: 2 }}>{p.startupName}</p>
                      <p style={{ fontSize: 11, color: 'var(--text-muted)' }}>{new Date(p.createdAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}</p>
                    </div>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)' }}>₹{Number(p.amount).toLocaleString('en-IN')}</span>
                    {paymentBadge(p.status)}
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Suggested startups */}
          <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, padding: 24, boxShadow: 'var(--shadow-card)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
              <h2 style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-secondary)' }}>Suggested startups</h2>
              <Link to="/investor/startups" style={{ fontSize: 12, color: 'var(--brand)', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: 3 }}>
                View all <ArrowRight size={12} />
              </Link>
            </div>
            {suggestedStartups.length === 0 ? (
              <p style={{ fontSize: 13, color: 'var(--text-muted)', padding: '24px 0', textAlign: 'center' }}>No startups available</p>
            ) : (
              suggestedStartups.map((s) => (
                <Link
                  key={s.id}
                  to={`/investor/startups/${s.id}`}
                  style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 14px', borderRadius: 12, border: '1px solid var(--border)', marginBottom: 8, textDecoration: 'none', transition: 'border-color 150ms', cursor: 'pointer' }}
                  onMouseEnter={e => ((e.currentTarget as HTMLElement).style.borderColor = 'var(--brand-border)')}
                  onMouseLeave={e => ((e.currentTarget as HTMLElement).style.borderColor = 'var(--border)')}
                >
                  <div>
                    <p style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-primary)', marginBottom: 2 }}>{s.name}</p>
                    <p style={{ fontSize: 11, color: 'var(--text-muted)' }}>{s.industry} · {s.location}</p>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <span className="badge-green" style={{ fontSize: 10 }}>{s.stage === 'EARLY_TRACTION' ? 'Early Traction' : s.stage}</span>
                    <ArrowRight size={13} style={{ color: 'var(--text-faint)' }} />
                  </div>
                </Link>
              ))
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default InvestorDashboard;
