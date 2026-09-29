import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Rocket, Plus, ArrowRight, IndianRupee } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Layout from '../../shared/components/Layout';
import useAuth from '../../shared/hooks/useAuth';
import { getStartupsByFounder } from '../../core/api/startupApi';
import { getPaymentsByFounder } from '../../core/api/paymentApi';
import { Startup, Payment } from '../../types';

/* ── helpers ── */
const greeting = () => {
  const h = new Date().getHours();
  if (h < 12) return 'Good morning';
  if (h < 18) return 'Good afternoon';
  return 'Good evening';
};

const shimmer = {
  background: 'linear-gradient(90deg, var(--surface-2) 25%, var(--surface-3) 50%, var(--surface-2) 75%)',
  backgroundSize: '200% 100%',
  animation: 'shimmer 1.4s infinite',
};

const FounderDashboard: React.FC = () => {
  const { userId, user } = useAuth();
  const [startups, setStartups] = useState<Startup[]>([]);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [paymentsLoading, setPaymentsLoading] = useState<boolean>(true);

  useEffect(() => {
    if (!userId) return;
    getStartupsByFounder(userId)
      .then((res) => setStartups(res.data || []))
      .catch(() => toast.error('Failed to load startups'))
      .finally(() => setLoading(false));

    getPaymentsByFounder(userId)
      .then((res) => setPayments(res.data || []))
      .catch(() => {})
      .finally(() => setPaymentsLoading(false));
  }, [userId]);

  const approved = startups.filter((s) => s.isApproved);
  const pending = startups.filter((s) => !s.isApproved && !s.isRejected);
  const successPayments = payments.filter((p) => p.status === 'SUCCESS');
  const totalReceived = successPayments.reduce((sum, p) => sum + Number(p.amount), 0);
  const userDisplayName = user?.name || user?.email?.split('@')[0];

  const statusBadge = (startup: Startup) => {
    if (startup.isApproved) return <span className="badge-green">Approved</span>;
    if (startup.isRejected) return <span className="badge-red">Rejected</span>;
    return <span className="badge-yellow">Pending</span>;
  };

  const paymentBadge = (status: string) => {
    if (status === 'SUCCESS') return <span className="badge-green">Success</span>;
    if (status === 'REJECTED') return <span className="badge-red">Rejected</span>;
    return <span className="badge-yellow">Pending</span>;
  };

  const metrics = [
    { label: 'Total startups', value: loading ? null : startups.length, accent: 'var(--brand)' },
    { label: 'Approved', value: loading ? null : approved.length, accent: 'var(--green)' },
    { label: 'Pending', value: loading ? null : pending.length, accent: 'var(--amber)' },
    { label: 'Total raised', value: paymentsLoading ? null : `₹${totalReceived.toLocaleString('en-IN')}`, accent: 'var(--green)' },
  ];

  return (
    <Layout>
      <style>{`
        @keyframes shimmer { 0% { background-position: 200% 0 } 100% { background-position: -200% 0 } }
      `}</style>
      <div style={{ maxWidth: 1000, margin: '0 auto' }}>
        {/* Page header */}
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 28, gap: 16, flexWrap: 'wrap' }}>
          <div>
            <span style={{ fontSize: 11, fontWeight: 600, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--brand)', display: 'block', marginBottom: 4 }}>
              Founder
            </span>
            <h1 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 28, fontWeight: 600, color: 'var(--text-primary)', margin: 0, lineHeight: 1.2 }}>
              {greeting()}{userDisplayName ? `, ${userDisplayName}` : ''}
            </h1>
            <p style={{ color: 'var(--text-muted)', fontSize: 14, marginTop: 4 }}>
              {loading ? 'Loading…' : `${startups.length} startup${startups.length !== 1 ? 's' : ''} in your account`}
            </p>
          </div>
          <Link to="/founder/startups/create" className="btn-primary" style={{ textDecoration: 'none', gap: 6 }}>
            <Plus size={15} /> New startup
          </Link>
        </div>

        {/* Metrics strip */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: 16,
            overflow: 'hidden',
            boxShadow: 'var(--shadow-card)',
            marginBottom: 28,
          }}
        >
          {metrics.map((m, i) => (
            <div
              key={i}
              style={{
                padding: '20px 24px',
                borderRight: i < metrics.length - 1 ? '1px solid var(--border)' : 'none',
              }}
            >
              <p style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 6, fontFamily: "'DM Sans', system-ui, sans-serif" }}>
                {m.label}
              </p>
              {m.value === null ? (
                <div style={{ height: 28, width: 60, borderRadius: 6, ...shimmer }} />
              ) : (
                <p style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 24, fontWeight: 700, color: m.accent, lineHeight: 1 }}>
                  {m.value}
                </p>
              )}
            </div>
          ))}
        </div>

        {/* Two-column grid */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: 20 }}>
          {/* Recent payments */}
          <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, padding: 24, boxShadow: 'var(--shadow-card)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
              <h2 style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-secondary)' }}>Recent payments</h2>
              <Link to="/founder/payments" style={{ fontSize: 12, color: 'var(--brand)', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: 3 }}>
                View all <ArrowRight size={12} />
              </Link>
            </div>
            {paymentsLoading ? (
              [1,2,3].map(i => <div key={i} style={{ height: 56, borderRadius: 12, marginBottom: 8, ...shimmer }} />)
            ) : payments.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '32px 0' }}>
                <div style={{ width: 40, height: 40, borderRadius: 10, background: 'var(--brand-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 12px' }}>
                  <IndianRupee size={20} style={{ color: 'var(--brand)' }} />
                </div>
                <p style={{ fontSize: 14, fontWeight: 500, color: 'var(--text-primary)', marginBottom: 4 }}>No payments yet</p>
                <p style={{ fontSize: 12, color: 'var(--text-muted)' }}>Investor payments will appear here</p>
              </div>
            ) : (
              payments.slice(0, 5).map((p) => (
                <div key={p.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 14px', borderRadius: 12, border: '1px solid var(--border)', marginBottom: 8, background: 'var(--surface)' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                    <div style={{ width: 36, height: 36, borderRadius: 10, background: 'var(--brand-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, fontSize: 13, fontWeight: 600, color: 'var(--brand)' }}>
                      {(p.investorName || 'I')[0].toUpperCase()}
                    </div>
                    <div>
                      <p style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-primary)', marginBottom: 2 }}>{p.investorName}</p>
                      <p style={{ fontSize: 11, color: 'var(--text-muted)' }}>{p.startupName}</p>
                    </div>
                  </div>
                  <div style={{ textAlign: 'right', display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)' }}>₹{Number(p.amount).toLocaleString('en-IN')}</span>
                    {paymentBadge(p.status)}
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Your startups */}
          <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, padding: 24, boxShadow: 'var(--shadow-card)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
              <h2 style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-secondary)' }}>Your startups</h2>
              <Link to="/founder/startups" style={{ fontSize: 12, color: 'var(--brand)', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: 3 }}>
                View all <ArrowRight size={12} />
              </Link>
            </div>
            {loading ? (
              [1,2].map(i => <div key={i} style={{ height: 56, borderRadius: 12, marginBottom: 8, ...shimmer }} />)
            ) : startups.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '32px 0' }}>
                <div style={{ width: 40, height: 40, borderRadius: 10, background: 'var(--brand-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 12px' }}>
                  <Rocket size={20} style={{ color: 'var(--brand)' }} />
                </div>
                <p style={{ fontSize: 14, fontWeight: 500, color: 'var(--text-primary)', marginBottom: 4 }}>No startups yet</p>
                <p style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 16 }}>Create your first startup to get started</p>
                <Link to="/founder/startups/create" className="btn-primary" style={{ textDecoration: 'none', fontSize: 13 }}>
                  Create startup
                </Link>
              </div>
            ) : (
              startups.slice(0, 5).map((s) => (
                <div key={s.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 14px', borderRadius: 12, border: '1px solid var(--border)', marginBottom: 8 }}>
                  <div>
                    <p style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-primary)', marginBottom: 2 }}>{s.name}</p>
                    <p style={{ fontSize: 11, color: 'var(--text-muted)' }}>{s.industry} · {s.location}</p>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <span className="badge-blue" style={{ fontSize: 10 }}>{s.stage === 'EARLY_TRACTION' ? 'Early Traction' : s.stage}</span>
                    {statusBadge(s)}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default FounderDashboard;
