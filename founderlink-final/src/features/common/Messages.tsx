import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { MessageSquare, Plus, ChevronRight, Search, Building2 } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Layout from '../../shared/components/Layout';
import useAuth from '../../shared/hooks/useAuth';
import { getMyConversations, startConversation } from '../../core/api/messagingApi';
import { getAuthUserById, getUsersByRole } from '../../core/api/userApi';
import { getStartupsByFounder } from '../../core/api/startupApi';
import { AuthUser, Conversation, RouteLocationState } from '../../types';

const shimmer = {
  background: 'linear-gradient(90deg, var(--surface-2) 25%, var(--surface-3) 50%, var(--surface-2) 75%)',
  backgroundSize: '200% 100%', animation: 'shimmer 1.4s infinite',
};

/* map role string → readable label */
const roleLabel = (role?: string): string => {
  if (!role) return '';
  if (role === 'ROLE_FOUNDER')   return 'Founder';
  if (role === 'ROLE_INVESTOR')  return 'Investor';
  if (role === 'ROLE_COFOUNDER') return 'Co-Founder';
  if (role === 'ROLE_ADMIN')     return 'Admin';
  return role.replace('ROLE_', '');
};

/* role accent color */
const roleColor = (role?: string): string => {
  if (role === 'ROLE_FOUNDER')   return 'var(--brand)';
  if (role === 'ROLE_INVESTOR')  return 'var(--green)';
  if (role === 'ROLE_COFOUNDER') return '#8b5cf6';
  return 'var(--text-muted)';
};

const Messages: React.FC = () => {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [userMap, setUserMap]             = useState<Record<number, AuthUser>>({});
  const [startupMap, setStartupMap]       = useState<Record<number, string>>({});
  const [loading, setLoading]             = useState(true);
  const [messageable, setMessageable]     = useState<AuthUser[]>([]);
  const [search, setSearch]               = useState('');
  const [showDropdown, setShowDropdown]   = useState(false);
  const navigate = useNavigate();
  const { userId, isFounder, isInvestor, isCoFounder } = useAuth();

  useEffect(() => {
    if (!userId) { setLoading(false); return; }
    let rolesToFetch: string[] = [];
    if (isFounder) rolesToFetch = ['ROLE_INVESTOR', 'ROLE_COFOUNDER'];
    else if (isInvestor || isCoFounder) rolesToFetch = ['ROLE_FOUNDER'];

    Promise.all([
      getMyConversations()
        .then((res) => { const p = res.data as Conversation[] | { data?: Conversation[] }; return Array.isArray(p) ? p : p.data || []; })
        .catch(() => [] as Conversation[]),
      ...rolesToFetch.map((role) => getUsersByRole(role).then((res) => res.data || []).catch(() => [] as AuthUser[])),
    ])
      .then(([convos, ...userArrays]) => {
        setConversations(convos);
        const allUsers = userArrays.flat().filter((u) => u.userId !== userId);
        setMessageable(allUsers);
        const nextMap: Record<number, AuthUser> = {};
        allUsers.forEach((u) => { nextMap[u.userId] = u; });
        const coveredIds = new Set(allUsers.map((u) => u.userId));
        const extraIds = Array.from(new Set(convos.map((c) => c.participant1Id === userId ? c.participant2Id : c.participant1Id))).filter((id) => !coveredIds.has(id));
        if (extraIds.length > 0) {
          Promise.all(extraIds.map((id) => getAuthUserById(id).then((r) => [id, r.data] as [number, AuthUser]).catch(() => [id, null] as [number, null])))
            .then((entries) => { entries.forEach(([id, data]) => { if (data) nextMap[id] = data; }); setUserMap({ ...nextMap }); });
        } else { setUserMap(nextMap); }

        /* ── fetch startup names for all ROLE_FOUNDER users ── */
        const founderUsers = [...allUsers, ...extraIds.map(id => nextMap[id]).filter(Boolean)].filter(u => u?.role === 'ROLE_FOUNDER');
        const founderIds = [...new Set(founderUsers.map(u => u.userId))];
        if (founderIds.length > 0) {
          Promise.all(
            founderIds.map(fid =>
              getStartupsByFounder(fid)
                .then(r => ({ fid, name: r.data?.[0]?.name || '' }))
                .catch(() => ({ fid, name: '' }))
            )
          ).then(results => {
            const sm: Record<number, string> = {};
            results.forEach(({ fid, name }) => { if (name) sm[fid] = name; });
            setStartupMap(sm);
          });
        }
      })
      .catch(() => toast.error('Failed to load conversations'))
      .finally(() => setLoading(false));
  }, [isCoFounder, isFounder, isInvestor, userId]);

  const filtered = search.trim().length > 0
    ? messageable.filter((u) => u.name?.toLowerCase().includes(search.toLowerCase()) || u.email?.toLowerCase().includes(search.toLowerCase()))
    : [];

  const handleSelectUser = async (user: AuthUser): Promise<void> => {
    setSearch(''); setShowDropdown(false);
    try {
      const res = await startConversation(user.userId);
      const conversation = res.data as Conversation | { data?: Conversation };
      const value = ('data' in conversation ? conversation.data : conversation) as Conversation | undefined;
      if (!value) throw new Error('Conversation not created');
      navigate(`/messages/${value.id}`, { state: { otherUserId: user.userId } as RouteLocationState });
    } catch { toast.error('Failed to start conversation'); }
  };

  const initials = (name?: string, email?: string) => (name?.[0] || email?.[0] || '?').toUpperCase();

  /* ── sub-line shown under a user's name ── */
  const UserMeta: React.FC<{ user: AuthUser; startupName?: string; size?: 'sm' | 'xs' }> = ({ user, startupName, size = 'sm' }) => {
    const fs = size === 'xs' ? 10 : 11;
    return (
      <div style={{ display: 'flex', alignItems: 'center', gap: 5, flexWrap: 'wrap' }}>
        <span style={{ fontSize: fs, fontWeight: 600, color: roleColor(user.role), letterSpacing: '0.01em' }}>
          {roleLabel(user.role)}
        </span>
        {startupName && (
          <>
            <span style={{ fontSize: fs, color: 'var(--text-faint)' }}>·</span>
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: 3, fontSize: fs, color: 'var(--text-muted)' }}>
              <Building2 size={fs - 1} style={{ flexShrink: 0 }} />
              {startupName}
            </span>
          </>
        )}
      </div>
    );
  };

  return (
    <Layout>
      <style>{`@keyframes shimmer { 0% { background-position: 200% 0 } 100% { background-position: -200% 0 } }`}</style>
      <div style={{ maxWidth: 720, margin: '0 auto' }}>
        <div style={{ marginBottom: 24 }}>
          <span style={{ fontSize: 11, fontWeight: 600, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--brand)', display: 'block', marginBottom: 4 }}>Messages</span>
          <h1 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 28, fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>Direct messages</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: 14, marginTop: 4 }}>Conversations with other users</p>
        </div>

        {/* New conversation search */}
        <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, padding: 20, marginBottom: 16, boxShadow: 'var(--shadow-card)' }}>
          <p style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 12, display: 'flex', alignItems: 'center', gap: 6 }}>
            <Plus size={14} /> New conversation
          </p>
          <div style={{ position: 'relative' }}>
            <Search size={14} style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-faint)', pointerEvents: 'none' }} />
            <input
              className="input-field"
              style={{ paddingLeft: 34 }}
              placeholder="Search by name or email…"
              value={search}
              onChange={(e) => { setSearch(e.target.value); setShowDropdown(true); }}
              onFocus={() => setShowDropdown(true)}
              onBlur={() => setTimeout(() => setShowDropdown(false), 150)}
            />
            {showDropdown && filtered.length > 0 && (
              <div style={{ position: 'absolute', zIndex: 20, width: '100%', marginTop: 4, background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 12, boxShadow: 'var(--shadow-hover)', overflow: 'hidden' }}>
                {filtered.slice(0, 8).map((u) => (
                  <button
                    key={u.userId}
                    onMouseDown={() => handleSelectUser(u)}
                    style={{ width: '100%', textAlign: 'left', padding: '10px 16px', background: 'none', border: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 12, transition: 'background 150ms', fontFamily: "'DM Sans', system-ui, sans-serif" }}
                    onMouseEnter={e => (e.currentTarget.style.background = 'var(--surface-2)')}
                    onMouseLeave={e => (e.currentTarget.style.background = 'none')}
                  >
                    {/* Avatar with role-coloured ring */}
                    <div style={{ position: 'relative', flexShrink: 0 }}>
                      <div style={{ width: 36, height: 36, borderRadius: '50%', background: 'var(--surface-2)', border: `2px solid ${roleColor(u.role)}`, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 13, fontWeight: 700, color: roleColor(u.role) }}>
                        {initials(u.name, u.email)}
                      </div>
                    </div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <p style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)', margin: '0 0 3px' }}>{u.name || u.email}</p>
                      <UserMeta user={u} startupName={startupMap[u.userId]} size="xs" />
                    </div>
                  </button>
                ))}
              </div>
            )}
            {showDropdown && search.trim().length > 0 && filtered.length === 0 && (
              <div style={{ position: 'absolute', zIndex: 20, width: '100%', marginTop: 4, background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 12, padding: '12px 16px' }}>
                <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>No users found for "{search}"</p>
              </div>
            )}
          </div>
        </div>

        {/* Conversations list */}
        <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, overflow: 'hidden', boxShadow: 'var(--shadow-card)' }}>
          <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--border)' }}>
            <p style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-secondary)', margin: 0 }}>Conversations</p>
          </div>
          {loading ? (
            <div style={{ padding: 16 }}>
              {[1,2,3].map(i => <div key={i} style={{ height: 60, borderRadius: 12, marginBottom: 8, ...shimmer }} />)}
            </div>
          ) : conversations.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '40px 24px' }}>
              <div style={{ width: 40, height: 40, borderRadius: 10, background: 'var(--brand-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 12px' }}>
                <MessageSquare size={20} style={{ color: 'var(--brand)' }} />
              </div>
              <p style={{ fontSize: 14, fontWeight: 500, color: 'var(--text-primary)', marginBottom: 4 }}>No conversations yet</p>
              <p style={{ fontSize: 12, color: 'var(--text-muted)' }}>Search above to start a new chat</p>
            </div>
          ) : (
            conversations.map((c, idx) => {
              const otherId = c.participant1Id === userId ? c.participant2Id : c.participant1Id;
              const other   = userMap[otherId];
              return (
                <div
                  key={c.id}
                  onClick={() => navigate(`/messages/${c.id}`, { state: { otherUserId: otherId } as RouteLocationState })}
                  style={{
                    display: 'flex', alignItems: 'center', gap: 14, padding: '14px 20px',
                    borderBottom: idx < conversations.length - 1 ? '1px solid var(--border)' : 'none',
                    cursor: 'pointer', transition: 'background 150ms',
                  }}
                  onMouseEnter={e => (e.currentTarget.style.background = 'var(--surface-2)')}
                  onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
                >
                  {/* Avatar with role-coloured border */}
                  <div style={{ width: 42, height: 42, borderRadius: '50%', background: 'var(--surface-2)', border: `2px solid ${roleColor(other?.role)}`, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, fontSize: 15, fontWeight: 700, color: roleColor(other?.role) }}>
                    {initials(other?.name, other?.email)}
                  </div>

                  <div style={{ flex: 1, minWidth: 0 }}>
                    <p style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-primary)', marginBottom: 4 }}>
                      {other?.name || other?.email || `User #${otherId}`}
                    </p>
                    {other
                      ? <UserMeta user={other} startupName={startupMap[otherId]} />
                      : <p style={{ fontSize: 11, color: 'var(--text-muted)' }}>{new Date(c.createdAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}</p>
                    }
                  </div>

                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 4, flexShrink: 0 }}>
                    <ChevronRight size={15} style={{ color: 'var(--text-faint)' }} />
                    <span style={{ fontSize: 10, color: 'var(--text-faint)' }}>
                      {new Date(c.createdAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short' })}
                    </span>
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

export default Messages;
