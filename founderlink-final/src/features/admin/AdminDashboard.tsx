import React, { useEffect, useState } from 'react';
import { AxiosError } from 'axios';
import { toast } from 'react-hot-toast';
import { CheckCircle, XCircle, Clock, ShieldCheck, Building2, ChevronDown, ChevronUp } from 'lucide-react';
import Layout from '../../shared/components/Layout';
import Button from '../../shared/components/Button';
import { getAllStartupsAdmin, approveStartup, rejectStartup } from '../../core/api/startupApi';
import { Startup } from '../../types';

const shimmer = {
  background: 'linear-gradient(90deg, var(--surface-2) 25%, var(--surface-3) 50%, var(--surface-2) 75%)',
  backgroundSize: '200% 100%', animation: 'shimmer 1.4s infinite',
};

type Tab = 'pending' | 'approved' | 'rejected';

const AdminDashboard: React.FC = () => {
  const [startups, setStartups] = useState<Startup[]>([]);
  const [loading, setLoading] = useState(true);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<Tab>('pending');

  const load = (): void => {
    setLoading(true);
    getAllStartupsAdmin()
      .then((res) => setStartups(res.data?.content || []))
      .catch(() => toast.error('Failed to load'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const handleApprove = async (id: number): Promise<void> => {
    setActionLoading(`${id}_approve`);
    try {
      await approveStartup(id);
      toast.success('Startup approved!');
      setExpandedId(null);
      load();
    } catch (error) {
      const err = error as AxiosError<{ message?: string }>;
      toast.error(err.response?.data?.message || 'Failed to approve');
    } finally { setActionLoading(null); }
  };

  const handleReject = async (id: number): Promise<void> => {
    setActionLoading(`${id}_reject`);
    try {
      await rejectStartup(id);
      toast.success('Startup rejected.');
      setExpandedId(null);
      load();
    } catch (error) {
      const err = error as AxiosError<{ message?: string }>;
      toast.error(err.response?.data?.message || 'Failed to reject');
    } finally { setActionLoading(null); }
  };

  const pending = startups.filter((s) => !s.isApproved && !s.isRejected);
  const approved = startups.filter((s) => s.isApproved);
  const rejected = startups.filter((s) => s.isRejected);
  const tabMap: Record<Tab, Startup[]> = { pending, approved, rejected };
  const currentList = tabMap[activeTab];

  const tabStyle = (tab: Tab): React.CSSProperties => ({
    padding: '8px 20px',
    fontSize: 13,
    fontWeight: 600,
    borderRadius: 8,
    border: 'none',
    cursor: 'pointer',
    fontFamily: "'DM Sans', system-ui, sans-serif",
    transition: 'all 150ms',
    background: activeTab === tab ? 'var(--brand)' : 'transparent',
    color: activeTab === tab ? '#fff' : 'var(--text-muted)',
  });

  return (
    <Layout>
      <style>{`@keyframes shimmer { 0% { background-position: 200% 0 } 100% { background-position: -200% 0 } }`}</style>
      <div style={{ maxWidth: 960, margin: '0 auto' }}>
        {/* Header */}
        <div style={{ marginBottom: 28 }}>
          <span style={{ fontSize: 11, fontWeight: 600, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--amber)', display: 'block', marginBottom: 4 }}>Admin</span>
          <h1 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 28, fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>Startup approvals</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: 14, marginTop: 4 }}>Review and approve or reject startup submissions</p>
        </div>

        {/* Tab switcher */}
        <div style={{ display: 'flex', gap: 4, background: 'var(--surface-2)', padding: 4, borderRadius: 10, marginBottom: 24, width: 'fit-content' }}>
          {(['pending', 'approved', 'rejected'] as Tab[]).map((tab) => (
            <button key={tab} onClick={() => setActiveTab(tab)} style={tabStyle(tab)}>
              {tab.charAt(0).toUpperCase() + tab.slice(1)}
              <span style={{
                marginLeft: 6, fontSize: 11,
                background: activeTab === tab ? 'rgba(255,255,255,0.2)' : 'var(--surface-3)',
                color: activeTab === tab ? '#fff' : 'var(--text-muted)',
                padding: '1px 7px', borderRadius: 20,
              }}>
                {tabMap[tab].length}
              </span>
            </button>
          ))}
        </div>

        {/* List */}
        <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, overflow: 'hidden', boxShadow: 'var(--shadow-card)' }}>
          {loading ? (
            <div style={{ padding: 24 }}>
              {[1,2,3].map(i => <div key={i} style={{ height: 64, borderRadius: 12, marginBottom: 8, ...shimmer }} />)}
            </div>
          ) : currentList.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '60px 24px' }}>
              <div style={{ width: 40, height: 40, borderRadius: 10, background: 'var(--surface-2)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 12px' }}>
                <Building2 size={20} style={{ color: 'var(--text-muted)' }} />
              </div>
              <p style={{ fontSize: 14, fontWeight: 500, color: 'var(--text-primary)', marginBottom: 4 }}>No {activeTab} startups</p>
              <p style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                {activeTab === 'pending' ? 'All submissions have been reviewed.' : `No startups have been ${activeTab} yet.`}
              </p>
            </div>
          ) : (
            currentList.map((startup, idx) => (
              <div
                key={startup.id}
                style={{ borderBottom: idx < currentList.length - 1 ? '1px solid var(--border)' : 'none' }}
              >
                <div
                  style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '16px 24px', gap: 16, flexWrap: 'wrap' }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 14, flex: 1, minWidth: 0 }}>
                    <div style={{ width: 38, height: 38, borderRadius: 10, background: 'var(--surface-2)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, fontSize: 14, fontWeight: 700, color: 'var(--text-secondary)' }}>
                      {startup.name[0].toUpperCase()}
                    </div>
                    <div style={{ minWidth: 0 }}>
                      <p style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-primary)', marginBottom: 3 }}>{startup.name}</p>
                      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                        <span className="badge-blue" style={{ fontSize: 10 }}>{startup.industry}</span>
                        <span className="badge-yellow" style={{ fontSize: 10 }}>{startup.stage === 'EARLY_TRACTION' ? 'Early Traction' : startup.stage}</span>
                        {startup.createdAt && (
                          <span style={{ fontSize: 11, color: 'var(--text-faint)' }}>
                            {new Date(startup.createdAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    {activeTab === 'pending' ? (
                      <>
                        <Button
                          variant="success"
                          size="sm"
                          isLoading={actionLoading === `${startup.id}_approve`}
                          leftIcon={<CheckCircle size={13} />}
                          onClick={() => handleApprove(startup.id)}
                        >Approve</Button>
                        <Button
                          variant="danger"
                          size="sm"
                          isLoading={actionLoading === `${startup.id}_reject`}
                          leftIcon={<XCircle size={13} />}
                          onClick={() => handleReject(startup.id)}
                        >Reject</Button>
                      </>
                    ) : activeTab === 'approved' ? (
                      <span className="badge-green">Approved</span>
                    ) : (
                      <span className="badge-red">Rejected</span>
                    )}
                    <button
                      onClick={() => setExpandedId(expandedId === startup.id ? null : startup.id)}
                      style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted)', padding: 4 }}
                    >
                      {expandedId === startup.id ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                    </button>
                  </div>
                </div>
                {expandedId === startup.id && (
                  <div style={{ padding: '0 24px 20px', borderTop: '1px solid var(--border)', paddingTop: 16 }}>
                    <p style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.6, marginBottom: 12 }}>{startup.description || 'No description provided.'}</p>
                    <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
                      {startup.location && <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>📍 {startup.location}</span>}
                      {startup.fundingGoal && <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>💰 Goal: ₹{Number(startup.fundingGoal).toLocaleString('en-IN')}</span>}
                    </div>
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      </div>
    </Layout>
  );
};

export default AdminDashboard;
