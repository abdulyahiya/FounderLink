import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { ArrowLeft, Edit, Users, TrendingUp, MapPin, CheckCircle, DollarSign, Target, BarChart2 } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Layout from '../../shared/components/Layout';
import { getStartupById } from '../../core/api/startupApi';
import { getPaymentsByStartup } from '../../core/api/paymentApi';
import { Startup, Payment } from '../../types';

const shimmer = {
  background: 'linear-gradient(90deg, var(--surface-2) 25%, var(--surface-3) 50%, var(--surface-2) 75%)',
  backgroundSize: '200% 100%', animation: 'shimmer 1.4s infinite',
};

const FounderStartupDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [startup, setStartup] = useState<Startup | null>(null);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    if (!id) return;
    const startupId = Number(id);
    Promise.all([
      getStartupById(startupId),
      getPaymentsByStartup(startupId).catch(() => ({ data: [] })),
    ])
      .then(([startupRes, paymentsRes]) => {
        setStartup(startupRes.data);
        setPayments((paymentsRes as any).data || []);
      })
      .catch(() => toast.error('Failed to load startup details'))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return (
      <Layout>
        <style>{`@keyframes shimmer { 0% { background-position: 200% 0 } 100% { background-position: -200% 0 } }`}</style>
        <div style={{ maxWidth: 860, margin: '0 auto' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {[80, 200, 160].map((h, i) => <div key={i} style={{ height: h, borderRadius: 16, ...shimmer }} />)}
          </div>
        </div>
      </Layout>
    );
  }

  if (!startup) {
    return (
      <Layout>
        <div style={{ maxWidth: 860, margin: '0 auto', textAlign: 'center', padding: '60px 24px' }}>
          <p style={{ color: 'var(--text-muted)' }}>Startup not found.</p>
        </div>
      </Layout>
    );
  }

  // ── funding maths ────────────────────────────────────────────────────────
  const successPayments = payments.filter((p) => p.status === 'SUCCESS');
  const pendingPayments = payments.filter((p) => p.status === 'AWAITING_APPROVAL');
  const totalReceived   = successPayments.reduce((sum, p) => sum + Number(p.amount), 0);
  const totalPending    = pendingPayments.reduce((sum, p) => sum + Number(p.amount), 0);
  const fundingGoal     = Number(startup.fundingGoal) || 0;
  const progress        = fundingGoal > 0 ? Math.min((totalReceived / fundingGoal) * 100, 100) : 0;
  const remaining       = Math.max(fundingGoal - totalReceived, 0);

  // group SUCCESS payments by investor for breakdown
  const investorMap: Record<string, { name: string; total: number }> = {};
  successPayments.forEach((p) => {
    const key = String(p.investorId);
    if (!investorMap[key]) investorMap[key] = { name: p.investorName || `Investor #${p.investorId}`, total: 0 };
    investorMap[key].total += Number(p.amount);
  });
  const investorBreakdown = Object.values(investorMap).sort((a, b) => b.total - a.total);

  const statusBadge = () => {
    if (startup.isApproved) return <span className="badge-green">Approved</span>;
    if (startup.isRejected) return <span className="badge-red">Rejected</span>;
    return <span className="badge-yellow">Pending review</span>;
  };

  return (
    <Layout>
      <style>{`@keyframes shimmer { 0% { background-position: 200% 0 } 100% { background-position: -200% 0 } }`}</style>
      <div style={{ maxWidth: 860, margin: '0 auto' }}>
        {/* Back */}
        <button
          onClick={() => navigate(-1)}
          style={{ display: 'flex', alignItems: 'center', gap: 6, background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted)', fontSize: 13, marginBottom: 20, padding: 0 }}
        >
          <ArrowLeft size={15} /> Back to my startups
        </button>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>

          {/* ── Header card ─────────────────────────────────────────────────── */}
          <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, padding: 24, boxShadow: 'var(--shadow-card)' }}>
            <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap', marginBottom: 12 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                <div style={{ width: 48, height: 48, borderRadius: 12, background: 'var(--brand-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 18, fontWeight: 700, color: 'var(--brand)', flexShrink: 0 }}>
                  {startup.name[0].toUpperCase()}
                </div>
                <div>
                  <h1 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 22, fontWeight: 700, color: 'var(--text-primary)', marginBottom: 6 }}>
                    {startup.name}
                  </h1>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, alignItems: 'center' }}>
                    {statusBadge()}
                    <span className="badge-blue" style={{ fontSize: 10 }}>{startup.industry}</span>
                    <span className="badge-purple" style={{ fontSize: 10 }}>{startup.stage === 'EARLY_TRACTION' ? 'Early Traction' : startup.stage}</span>
                    {startup.location && (
                      <span style={{ fontSize: 12, color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: 4 }}>
                        <MapPin size={11} />{startup.location}
                      </span>
                    )}
                    {startup.industry && (
                      <span style={{ fontSize: 12, color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: 4 }}>
                        <TrendingUp size={11} />{startup.industry}
                      </span>
                    )}
                  </div>
                </div>
              </div>

              {/* Action buttons */}
              <div style={{ display: 'flex', gap: 8, flexShrink: 0 }}>
                <Link
                  to={`/founder/team/${startup.id}`}
                  title="Manage team"
                  className="btn-secondary"
                  style={{ textDecoration: 'none', gap: 6, fontSize: 12 }}
                >
                  <Users size={13} /> Team
                </Link>
                <Link
                  to={`/founder/startups/${startup.id}/edit`}
                  title="Edit startup"
                  className="btn-secondary"
                  style={{ textDecoration: 'none', gap: 6, fontSize: 12 }}
                >
                  <Edit size={13} /> Edit
                </Link>
              </div>
            </div>

            {startup.description && (
              <p style={{ color: 'var(--text-secondary)', fontSize: 14, lineHeight: 1.7 }}>{startup.description}</p>
            )}
          </div>

          {/* ── Funding Progress Card ─────────────────────────────────────── */}
          <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, padding: 24, boxShadow: 'var(--shadow-card)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 20 }}>
              <div style={{ width: 32, height: 32, borderRadius: 8, background: 'var(--brand-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <BarChart2 size={16} style={{ color: 'var(--brand)' }} />
              </div>
              <h2 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 16, fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>
                Funding overview
              </h2>
            </div>

            {/* Top stats row */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 12, marginBottom: 20 }}>
              {[
                { label: 'Funding goal', value: `₹${fundingGoal.toLocaleString('en-IN')}`, icon: <Target size={14} />, color: 'var(--brand)' },
                { label: 'Total received', value: `₹${totalReceived.toLocaleString('en-IN')}`, icon: <CheckCircle size={14} />, color: 'var(--green)' },
                { label: 'Remaining', value: `₹${remaining.toLocaleString('en-IN')}`, icon: <DollarSign size={14} />, color: remaining === 0 ? 'var(--green)' : 'var(--amber)' },
              ].map((stat, i) => (
                <div key={i} style={{ background: 'var(--surface-2)', borderRadius: 12, padding: '14px 16px', border: '1px solid var(--border)' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8, color: stat.color }}>
                    {stat.icon}
                    <span style={{ fontSize: 11, fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--text-muted)' }}>{stat.label}</span>
                  </div>
                  <p style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 18, fontWeight: 700, color: stat.color, lineHeight: 1 }}>{stat.value}</p>
                </div>
              ))}
            </div>

            {/* Progress bar */}
            {fundingGoal > 0 ? (
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 8 }}>
                  <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)' }}>
                    ₹{totalReceived.toLocaleString('en-IN')}
                    <span style={{ fontSize: 11, fontWeight: 400, color: 'var(--text-muted)', marginLeft: 4 }}>
                      raised of ₹{fundingGoal.toLocaleString('en-IN')} goal
                    </span>
                  </span>
                  <span style={{ fontSize: 14, fontWeight: 700, color: progress >= 100 ? 'var(--green)' : 'var(--brand)' }}>
                    {progress.toFixed(1)}%
                  </span>
                </div>
                <div style={{ height: 10, background: 'var(--surface-3)', borderRadius: 99, overflow: 'hidden' }}>
                  <div
                    style={{
                      height: '100%',
                      width: `${progress}%`,
                      background: progress >= 100
                        ? 'linear-gradient(90deg, var(--green), #34d399)'
                        : 'linear-gradient(90deg, var(--brand), #34d399)',
                      borderRadius: 99,
                      transition: 'width 0.8s cubic-bezier(0.4,0,0.2,1)',
                    }}
                  />
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 6 }}>
                  <span style={{ fontSize: 11, color: 'var(--text-faint)' }}>
                    {successPayments.length} confirmed payment{successPayments.length !== 1 ? 's' : ''}
                  </span>
                  {totalPending > 0 && (
                    <span style={{ fontSize: 11, color: 'var(--amber)' }}>
                      + ₹{totalPending.toLocaleString('en-IN')} pending approval
                    </span>
                  )}
                </div>
              </div>
            ) : (
              <p style={{ fontSize: 13, color: 'var(--text-muted)', textAlign: 'center', padding: '12px 0' }}>
                No funding goal set for this startup.
              </p>
            )}
          </div>

          {/* ── Investor Breakdown ────────────────────────────────────────── */}
          {investorBreakdown.length > 0 && (
            <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, overflow: 'hidden', boxShadow: 'var(--shadow-card)' }}>
              <div style={{ padding: '16px 24px', borderBottom: '1px solid var(--border)' }}>
                <h2 style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-secondary)', margin: 0 }}>
                  Investor contributions ({investorBreakdown.length})
                </h2>
              </div>
              {investorBreakdown.map((inv, idx) => {
                const pct = fundingGoal > 0 ? Math.min((inv.total / fundingGoal) * 100, 100) : 0;
                return (
                  <div
                    key={idx}
                    style={{
                      padding: '14px 24px',
                      borderBottom: idx < investorBreakdown.length - 1 ? '1px solid var(--border)' : 'none',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                        <div style={{ width: 32, height: 32, borderRadius: 8, background: 'var(--green-bg)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 12, fontWeight: 700, color: 'var(--green)' }}>
                          {inv.name[0].toUpperCase()}
                        </div>
                        <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)' }}>{inv.name}</span>
                      </div>
                      <div style={{ textAlign: 'right' }}>
                        <span style={{ fontSize: 14, fontWeight: 700, color: 'var(--green)' }}>
                          ₹{inv.total.toLocaleString('en-IN')}
                        </span>
                        {fundingGoal > 0 && (
                          <span style={{ fontSize: 11, color: 'var(--text-muted)', marginLeft: 6 }}>
                            ({pct.toFixed(1)}% of goal)
                          </span>
                        )}
                      </div>
                    </div>
                    {fundingGoal > 0 && (
                      <div style={{ height: 4, background: 'var(--surface-3)', borderRadius: 99, overflow: 'hidden' }}>
                        <div style={{ height: '100%', width: `${pct}%`, background: 'var(--green)', borderRadius: 99, transition: 'width 0.6s ease' }} />
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}

          {/* Empty state when no payments yet */}
          {successPayments.length === 0 && (
            <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, textAlign: 'center', padding: '40px 24px' }}>
              <div style={{ width: 40, height: 40, borderRadius: 10, background: 'var(--brand-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 12px' }}>
                <DollarSign size={20} style={{ color: 'var(--brand)' }} />
              </div>
              <p style={{ fontSize: 14, fontWeight: 500, color: 'var(--text-primary)', marginBottom: 4 }}>No funds received yet</p>
              <p style={{ fontSize: 12, color: 'var(--text-muted)' }}>Confirmed investments from investors will appear here</p>
            </div>
          )}

        </div>
      </div>
    </Layout>
  );
};

export default FounderStartupDetail;
