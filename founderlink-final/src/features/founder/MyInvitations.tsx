import React, { useEffect, useState } from 'react';
import { Users, CheckCircle, XCircle, Rocket, Mail } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Layout from '../../shared/components/Layout';
import { getMyInvitations, acceptInvitation, rejectInvitation } from '../../core/api/teamApi';
import { getStartupById } from '../../core/api/startupApi';
import { Invitation, Startup } from '../../types';

const ROLE_COLORS: Record<string, string> = {
  FOUNDER: 'badge-blue', CO_FOUNDER: 'badge-purple',
  CTO: 'badge-blue', CPO: 'badge-blue',
  MARKETING_HEAD: 'badge-green', ENGINEERING_LEAD: 'badge-yellow',
};
const ROLE_LABEL: Record<string, string> = {
  CO_FOUNDER: 'Co-Founder', CTO: 'CTO', CPO: 'CPO',
  MARKETING_HEAD: 'Marketing Head', ENGINEERING_LEAD: 'Engineering Lead',
};

const shimmer = {
  background: 'linear-gradient(90deg, var(--surface-2) 25%, var(--surface-3) 50%, var(--surface-2) 75%)',
  backgroundSize: '200% 100%', animation: 'shimmer 1.4s infinite',
};

const MyInvitations: React.FC = () => {
  const [invitations, setInvitations] = useState<Invitation[]>([]);
  const [startupMap, setStartupMap] = useState<Record<number, Startup | null>>({});
  const [loading, setLoading] = useState<boolean>(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  const load = (): void => {
    setLoading(true);
    getMyInvitations()
      .then((res) => {
        const list: Invitation[] = res.data || [];
        const uniqueIds = Array.from(new Set(list.map((i) => i.startupId)));
        Promise.all(uniqueIds.map((id) => getStartupById(id).then((r) => [id, r.data] as [number, Startup]).catch(() => [id, null] as [number, null])))
          .then((entries) => {
            const map = Object.fromEntries(entries);
            setStartupMap(map);
            // Filter out invitations whose startup has been deleted (API returned 404 → null)
            const validList = list.filter((inv) => map[inv.startupId] !== null);
            setInvitations(validList);
          });
      })
      .catch(() => toast.error('Failed to load invitations'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const handleAccept = async (id: number): Promise<void> => {
    setActionLoading(id + '_accept');
    try { await acceptInvitation(id); toast.success('Invitation accepted! You are now part of the team.'); load(); }
    catch (err: any) { toast.error(err.response?.data?.message || 'Failed to accept invitation'); }
    finally { setActionLoading(null); }
  };

  const handleReject = async (id: number): Promise<void> => {
    setActionLoading(id + '_reject');
    try { await rejectInvitation(id); toast.success('Invitation rejected.'); load(); }
    catch (err: any) { toast.error(err.response?.data?.message || 'Failed to reject invitation'); }
    finally { setActionLoading(null); }
  };

  const pending  = invitations.filter((i) => i.status === 'PENDING');
  const resolved = invitations.filter((i) => i.status !== 'PENDING');

  const metrics = [
    { label: 'Pending', value: pending.length, accent: 'var(--amber)' },
    { label: 'Accepted', value: invitations.filter((i) => i.status === 'ACCEPTED').length, accent: 'var(--green)' },
    { label: 'Total', value: invitations.length, accent: 'var(--text-primary)' },
  ];

  return (
    <Layout>
      <style>{`@keyframes shimmer { 0% { background-position: 200% 0 } 100% { background-position: -200% 0 } }`}</style>
      <div style={{ maxWidth: 800, margin: '0 auto' }}>
        <div style={{ marginBottom: 28 }}>
          <span style={{ fontSize: 11, fontWeight: 600, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--purple)', display: 'block', marginBottom: 4 }}>Co-Founder</span>
          <h1 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 28, fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>My invitations</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: 14, marginTop: 4 }}>Accept or reject startup team invitations</p>
        </div>

        {/* Metrics */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, overflow: 'hidden', boxShadow: 'var(--shadow-card)', marginBottom: 24 }}>
          {metrics.map((m, i) => (
            <div key={i} style={{ padding: '20px 24px', borderRight: i < metrics.length - 1 ? '1px solid var(--border)' : 'none' }}>
              <p style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 6 }}>{m.label}</p>
              <p style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 24, fontWeight: 700, color: m.accent, lineHeight: 1 }}>{m.value}</p>
            </div>
          ))}
        </div>

        {loading ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {[1,2].map(i => <div key={i} style={{ height: 72, borderRadius: 14, ...shimmer }} />)}
          </div>
        ) : invitations.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '60px 24px', background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16 }}>
            <div style={{ width: 40, height: 40, borderRadius: 10, background: 'var(--purple-bg)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 12px' }}>
              <Mail size={20} style={{ color: 'var(--purple)' }} />
            </div>
            <p style={{ fontSize: 14, fontWeight: 500, color: 'var(--text-primary)', marginBottom: 4 }}>No invitations yet</p>
            <p style={{ fontSize: 12, color: 'var(--text-muted)' }}>Founders will invite you to join their teams here</p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {/* Pending invitations */}
            {pending.length > 0 && (
              <>
                <p style={{ fontSize: 13, fontWeight: 600, color: 'var(--amber)', marginBottom: 4, display: 'flex', alignItems: 'center', gap: 6 }}>
                  <Users size={13} /> Awaiting response ({pending.length})
                </p>
                {pending.map((inv) => (
                  <div key={inv.id} style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '14px 18px', borderRadius: 14, border: '1px solid var(--border)', background: 'var(--surface)', boxShadow: 'var(--shadow-card)', flexWrap: 'wrap' }}>
                    <div style={{ width: 40, height: 40, borderRadius: 10, background: 'var(--brand-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, fontSize: 15, fontWeight: 700, color: 'var(--brand)' }}>
                      {(startupMap[inv.startupId]?.name || 'S')[0].toUpperCase()}
                    </div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <p style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-primary)', marginBottom: 4 }}>
                        {startupMap[inv.startupId]?.name || `Startup #${inv.startupId}`}
                      </p>
                      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', alignItems: 'center' }}>
                        <span className={ROLE_COLORS[inv.role] || 'badge-blue'} style={{ fontSize: 10 }}>
                          {ROLE_LABEL[inv.role] || inv.role}
                        </span>
                        <span style={{ fontSize: 11, color: 'var(--text-faint)' }}>
                          {new Date(inv.createdAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}
                        </span>
                      </div>
                    </div>
                    <div style={{ display: 'flex', gap: 8 }}>
                      <button
                        onClick={() => handleAccept(inv.id)}
                        disabled={!!actionLoading}
                        style={{ padding: '6px 14px', borderRadius: 8, background: 'var(--green-bg)', border: '1px solid rgba(22,163,74,0.2)', color: 'var(--green)', fontSize: 12, fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 5 }}
                      >
                        <CheckCircle size={12} /> Accept
                      </button>
                      <button
                        onClick={() => handleReject(inv.id)}
                        disabled={!!actionLoading}
                        style={{ padding: '6px 14px', borderRadius: 8, background: 'var(--red-bg)', border: '1px solid rgba(220,38,38,0.2)', color: 'var(--red)', fontSize: 12, fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 5 }}
                      >
                        <XCircle size={12} /> Decline
                      </button>
                    </div>
                  </div>
                ))}
              </>
            )}

            {/* Past invitations */}
            {resolved.length > 0 && (
              <>
                <p style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-muted)', marginTop: 16, marginBottom: 4 }}>Past invitations ({resolved.length})</p>
                {resolved.map((inv) => (
                  <div key={inv.id} style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '12px 18px', borderRadius: 14, border: '1px solid var(--border)', background: 'var(--surface)', opacity: 0.7, flexWrap: 'wrap' }}>
                    <div style={{ width: 36, height: 36, borderRadius: 10, background: 'var(--surface-2)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, fontSize: 13, fontWeight: 700, color: 'var(--text-muted)' }}>
                      {(startupMap[inv.startupId]?.name || 'S')[0].toUpperCase()}
                    </div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <p style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-primary)', marginBottom: 2 }}>
                        {startupMap[inv.startupId]?.name || `Startup #${inv.startupId}`}
                      </p>
                      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', alignItems: 'center' }}>
                        <span className={ROLE_COLORS[inv.role] || 'badge-blue'} style={{ fontSize: 10 }}>{ROLE_LABEL[inv.role] || inv.role}</span>
                        <span className={inv.status === 'ACCEPTED' ? 'badge-green' : 'badge-red'} style={{ fontSize: 10 }}>{inv.status}</span>
                      </div>
                    </div>
                  </div>
                ))}
              </>
            )}
          </div>
        )}
      </div>
    </Layout>
  );
};

export default MyInvitations;
