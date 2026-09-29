import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { TrendingUp, ArrowRight, BarChart2 } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Layout from '../../shared/components/Layout';
import useAuth from '../../shared/hooks/useAuth';
import { getPaymentsByInvestor } from '../../core/api/paymentApi';
import { Payment } from '../../types';

const shimmer = {
  background: 'linear-gradient(90deg, var(--surface-2) 25%, var(--surface-3) 50%, var(--surface-2) 75%)',
  backgroundSize: '200% 100%', animation: 'shimmer 1.4s infinite',
};

const MyInvestments: React.FC = () => {
  const { user } = useAuth();
  const [payments, setPayments] = useState<Payment[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    if (!user?.userId) return;
    getPaymentsByInvestor(user.userId)
      .then(res => setPayments(res.data || []))
      .catch(() => toast.error('Failed to load investments'))
      .finally(() => setLoading(false));
  }, [user]);

  const successful = payments.filter((p: Payment) => p.status === 'SUCCESS');
  const totalInvested = successful.reduce((sum: number, p: Payment) => sum + Number(p.amount), 0);

  /* ── group confirmed payments by startup ── */
  const portfolioMap: Record<number, { name: string; total: number; count: number; latest: string }> = {};
  successful.forEach((p: Payment) => {
    if (!portfolioMap[p.startupId]) {
      portfolioMap[p.startupId] = { name: p.startupName, total: 0, count: 0, latest: p.createdAt };
    }
    portfolioMap[p.startupId].total += Number(p.amount);
    portfolioMap[p.startupId].count += 1;
    if (new Date(p.createdAt) > new Date(portfolioMap[p.startupId].latest)) {
      portfolioMap[p.startupId].latest = p.createdAt;
    }
  });
  const portfolio = Object.entries(portfolioMap).map(([id, v]) => ({ startupId: Number(id), ...v }));
  portfolio.sort((a, b) => b.total - a.total);

  const uniqueStartups = portfolio.length;
  const pendingCount = payments.filter((p: Payment) => p.status !== 'SUCCESS' && p.status !== 'FAILED' && p.status !== 'REJECTED').length;

  const metrics = [
    { label: 'Total invested', value: `₹${totalInvested.toLocaleString('en-IN')}`, accent: 'var(--green)', sub: 'confirmed only' },
    { label: 'Startups funded', value: uniqueStartups, accent: 'var(--brand)', sub: 'unique companies' },
    { label: 'Pending review', value: pendingCount, accent: 'var(--amber, #d97706)', sub: 'awaiting approval' },
  ];

  return (
    <Layout>
      <style>{`
        @keyframes shimmer { 0%{background-position:200% 0} 100%{background-position:-200% 0} }
        @keyframes mi-fade-up { from{opacity:0;transform:translateY(14px)} to{opacity:1;transform:translateY(0)} }
        .mi-page { animation: mi-fade-up 0.38s cubic-bezier(0.22,1,0.36,1) both; }
        .mi-portfolio-row {
          display: flex; align-items: center; gap: 16px;
          padding: 16px 24px;
          transition: background 0.15s;
          cursor: default;
        }
        .mi-portfolio-row:hover { background: var(--surface-2); }
        .mi-bar-track {
          height: 5px; border-radius: 99px; overflow: hidden;
          background: var(--surface-3); margin-top: 6px;
        }
        .mi-bar-fill {
          height: 100%; border-radius: 99px;
          background: linear-gradient(90deg, var(--green), #34d399);
          transition: width 0.8s cubic-bezier(0.4,0,0.2,1);
        }
      `}</style>

      <div className="mi-page" style={{ maxWidth: 960, margin: '0 auto' }}>
        <div style={{ marginBottom: 28 }}>
          <span style={{ fontSize: 11, fontWeight: 600, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--green)', display: 'block', marginBottom: 4 }}>Investor</span>
          <h1 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 28, fontWeight: 700, color: 'var(--text-primary)', margin: 0, letterSpacing: '-0.4px' }}>My Investments</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: 14, marginTop: 4 }}>Your portfolio of funded startups</p>
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

        {/* ── Portfolio table ── */}
        <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 20, overflow: 'hidden', boxShadow: 'var(--shadow-card)' }}>
          <div style={{ padding: '16px 24px', borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', gap: 8 }}>
            <BarChart2 size={14} style={{ color: 'var(--text-muted)' }} />
            <h2 style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-secondary)', margin: 0 }}>
              Portfolio  {portfolio.length > 0 && <span style={{ fontSize: 11, color: 'var(--text-faint)', fontWeight: 400 }}>({portfolio.length} startup{portfolio.length !== 1 ? 's' : ''})</span>}
            </h2>
          </div>

          {loading ? (
            <div style={{ padding: 24 }}>
              {[1, 2, 3].map(i => <div key={i} style={{ height: 72, borderRadius: 12, marginBottom: 10, ...shimmer }} />)}
            </div>
          ) : portfolio.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '64px 24px' }}>
              <div style={{ width: 44, height: 44, borderRadius: 12, background: 'var(--green-bg)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 14px' }}>
                <TrendingUp size={20} style={{ color: 'var(--green)' }} />
              </div>
              <p style={{ fontSize: 15, fontWeight: 600, color: 'var(--text-primary)', marginBottom: 6 }}>No confirmed investments yet</p>
              <p style={{ fontSize: 13, color: 'var(--text-muted)', marginBottom: 16 }}>Browse startups and make your first investment</p>
              <Link to="/investor/startups" className="btn-primary" style={{ textDecoration: 'none', fontSize: 13, display: 'inline-flex' }}>
                Browse startups
              </Link>
            </div>
          ) : (
            <>
              {portfolio.map((item, idx) => {
                const sharePct = totalInvested > 0 ? (item.total / totalInvested) * 100 : 0;
                const hue = item.name.split('').reduce((a, c) => a + c.charCodeAt(0), 0) % 360;
                return (
                  <div key={item.startupId} className="mi-portfolio-row" style={{ borderBottom: idx < portfolio.length - 1 ? '1px solid var(--border)' : 'none' }}>
                    {/* Avatar */}
                    <div style={{
                      width: 44, height: 44, borderRadius: 12, flexShrink: 0,
                      background: `hsla(${hue},65%,52%,0.12)`,
                      color: `hsl(${hue},60%,48%)`,
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontFamily: "'Syne', system-ui, sans-serif", fontSize: 17, fontWeight: 800,
                    }}>
                      {item.name[0].toUpperCase()}
                    </div>

                    {/* Name + bar */}
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', gap: 8 }}>
                        <p style={{ fontSize: 14, fontWeight: 700, color: 'var(--text-primary)', margin: 0, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                          {item.name}
                        </p>
                        <span style={{ fontSize: 10, color: 'var(--text-faint)', flexShrink: 0 }}>
                          {item.count} payment{item.count !== 1 ? 's' : ''}
                        </span>
                      </div>
                      <div className="mi-bar-track">
                        <div className="mi-bar-fill" style={{ width: `${sharePct}%` }} />
                      </div>
                      <p style={{ fontSize: 10, color: 'var(--text-faint)', marginTop: 4 }}>
                        {sharePct.toFixed(1)}% of total portfolio · last {new Date(item.latest).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}
                      </p>
                    </div>

                    {/* Amount + link */}
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 6, flexShrink: 0 }}>
                      <span style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 16, fontWeight: 800, color: 'var(--green)' }}>
                        ₹{item.total.toLocaleString('en-IN')}
                      </span>
                      <Link to={`/investor/startups/${item.startupId}`} style={{ fontSize: 11, color: 'var(--brand)', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: 3, fontWeight: 500 }}>
                        View <ArrowRight size={10} />
                      </Link>
                    </div>
                  </div>
                );
              })}

              {/* Footer total */}
              <div style={{ padding: '14px 24px', borderTop: '1px solid var(--border)', background: 'var(--surface-2)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: 12, color: 'var(--text-muted)', fontWeight: 500 }}>Total portfolio value</span>
                <span style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 16, fontWeight: 800, color: 'var(--green)' }}>
                  ₹{totalInvested.toLocaleString('en-IN')}
                </span>
              </div>
            </>
          )}
        </div>
      </div>
    </Layout>
  );
};

export default MyInvestments;
