import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Users, Mail, Search, Rocket, CheckCircle, XCircle } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Layout from '../../shared/components/Layout';
import { getMyInvitations } from '../../core/api/teamApi';
import { getStartupById } from '../../core/api/startupApi';
import { Invitation, Startup } from '../../types';

const ROLE_LABEL: Record<string, string> = {
  CTO: 'CTO', CPO: 'CPO', MARKETING_HEAD: 'Marketing Head',
  ENGINEERING_LEAD: 'Engineering Lead', CO_FOUNDER: 'Co-Founder',
};
const ROLE_BADGE: Record<string, string> = {
  CTO: 'badge-blue', CPO: 'badge-blue', MARKETING_HEAD: 'badge-green',
  ENGINEERING_LEAD: 'badge-yellow', CO_FOUNDER: 'badge-purple',
};

const shimmer = {
  background: 'linear-gradient(90deg, var(--surface-2) 25%, var(--surface-3) 50%, var(--surface-2) 75%)',
  backgroundSize: '200% 100%', animation: 'shimmer 1.4s infinite',
};

const CoFounderDashboard: React.FC = () => {
  const navigate = useNavigate();
  const [invitations, setInvitations] = useState<Invitation[]>([]);
  const [teamsJoined, setTeamsJoined] = useState<Invitation[]>([]);
  const [startupMap, setStartupMap] = useState<Record<number, Startup | null>>({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getMyInvitations()
      .then((res) => {
        const allInvites = res.data || [];
        setInvitations(allInvites);
        const accepted = allInvites.filter((invite) => invite.status === 'ACCEPTED');
        setTeamsJoined(accepted);
        const uniqueIds = Array.from(new Set(allInvites.map((invite) => invite.startupId)));
        return Promise.all(
          uniqueIds.map((id) =>
            getStartupById(id)
              .then((response) => [id, response.data] as [number, Startup])
              .catch(() => [id, null] as [number, null])
          )
        ).then((entries) => {
          const map = Object.fromEntries(entries);
          setStartupMap(map);
          // Remove invitations whose startup has been deleted (null = 404 from API)
          const validInvites = allInvites.filter((invite) => map[invite.startupId] !== null);
          setInvitations(validInvites);
          setTeamsJoined(validInvites.filter((invite) => invite.status === 'ACCEPTED'));
        });
      })
      .catch(() => toast.error('Failed to load dashboard'))
      .finally(() => setLoading(false));
  }, []);

  const pending = invitations.filter((invite) => invite.status === 'PENDING');

  const metrics = [
    { label: 'Invitations received', value: loading ? null : invitations.length, accent: 'var(--purple)' },
    { label: 'Active teams', value: loading ? null : teamsJoined.length, accent: 'var(--brand)' },
    { label: 'Pending invitations', value: loading ? null : pending.length, accent: 'var(--amber)' },
  ];

  return (
    <Layout>
      <style>{`@keyframes shimmer { 0% { background-position: 200% 0 } 100% { background-position: -200% 0 } }`}</style>
      <div style={{ maxWidth: 900, margin: '0 auto' }}>
        {/* Header */}
        <div style={{ marginBottom: 28 }}>
          <span style={{ fontSize: 11, fontWeight: 600, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--purple)', display: 'block', marginBottom: 4 }}>Co-Founder</span>
          <h1 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 28, fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>Dashboard</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: 14, marginTop: 4 }}>Your team invitations and activity</p>
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

        {/* My invitations */}
        <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, padding: 24, boxShadow: 'var(--shadow-card)' }}>
          <h2 style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 20, paddingBottom: 12, borderBottom: '1px solid var(--border)' }}>My invitations</h2>
          {loading ? (
            [1,2,3].map(i => <div key={i} style={{ height: 64, borderRadius: 12, marginBottom: 8, ...shimmer }} />)
          ) : invitations.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '40px 0' }}>
              <div style={{ width: 40, height: 40, borderRadius: 10, background: 'var(--purple-bg)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 12px' }}>
                <Mail size={20} style={{ color: 'var(--purple)' }} />
              </div>
              <p style={{ fontSize: 14, fontWeight: 500, color: 'var(--text-primary)', marginBottom: 4 }}>No invitations yet</p>
              <p style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 16 }}>Browse startups to get noticed by founders</p>
              <button onClick={() => navigate('/cofounder/startups')} className="btn-primary" style={{ fontSize: 13 }}>Browse startups</button>
            </div>
          ) : (
            invitations.map((invite) => {
              const startup = startupMap[invite.startupId];
              return (
                <div key={invite.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '14px 18px', borderRadius: 12, border: '1px solid var(--border)', marginBottom: 8, flexWrap: 'wrap', gap: 12 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                    <div style={{ width: 38, height: 38, borderRadius: 10, background: 'var(--purple-bg)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, fontSize: 14, fontWeight: 700, color: 'var(--purple)' }}>
                      {(startup?.name || 'S')[0].toUpperCase()}
                    </div>
                    <div>
                      <p style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-primary)', marginBottom: 3 }}>
                        {startup ? startup.name : `Startup #${invite.startupId}`}
                      </p>
                      <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                        <span className={ROLE_BADGE[invite.role] || 'badge-blue'} style={{ fontSize: 10 }}>
                          {ROLE_LABEL[invite.role] || invite.role}
                        </span>
                        {invite.status === 'ACCEPTED' && <span className="badge-green" style={{ fontSize: 10 }}>Accepted</span>}
                        {invite.status === 'REJECTED' && <span className="badge-red" style={{ fontSize: 10 }}>Declined</span>}
                        {invite.status === 'PENDING' && <span className="badge-yellow" style={{ fontSize: 10 }}>Pending</span>}
                      </div>
                    </div>
                  </div>
                  {invite.status === 'PENDING' && (
                    <div style={{ display: 'flex', gap: 8 }}>
                      <button
                        onClick={() => navigate('/founder/invitations')}
                        className="btn-primary"
                        style={{ fontSize: 12, padding: '6px 14px', gap: 5 }}
                      >
                        <CheckCircle size={13} /> Review
                      </button>
                    </div>
                  )}
                </div>
              );
            })
          )}
        </div>
      </div>
    </Layout>
  );
};

export default CoFounderDashboard;
