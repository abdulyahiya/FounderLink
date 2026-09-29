import React, { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import {
  Search, UserPlus, Users, User, CheckCircle,
  Briefcase, X, ChevronDown, Pencil, Check, Plus,
} from 'lucide-react';
import Layout from '../../shared/components/Layout';
import { getCoFounderIds, getProfilesBatch, getAuthUserById, getUsersByRole } from '../../core/api/userApi';
import { getTeamByStartup, inviteCoFounder, updateMemberRole } from '../../core/api/teamApi';
import useDebounce from '../../shared/hooks/useDebounce';
import { TeamMember, UserProfile, AuthUser } from '../../types';

interface RoleOption { value: string; label: string; }
interface InviteRoleOption { value: string; label: string; }
interface MergedUser { userId: number; name: string; email: string; bio?: string; skills?: string; experience?: string; hasProfile: boolean; }
interface MemberProfile { userId: number; name: string; email: string; bio?: string; skills?: string; experience?: string; }
interface TeamManagementParams { startupId: string; [key: string]: string | undefined; }

const UPDATE_ROLES: RoleOption[] = [
  { value: 'CO_FOUNDER', label: 'Co-Founder' }, { value: 'CTO', label: 'CTO' },
  { value: 'CPO', label: 'CPO' }, { value: 'MARKETING_HEAD', label: 'Marketing Head' },
  { value: 'ENGINEERING_LEAD', label: 'Engineering Lead' },
];
const INVITE_ROLES: InviteRoleOption[] = [
  { value: 'CTO', label: 'CTO – Chief Technology Officer' },
  { value: 'CPO', label: 'CPO – Chief Product Officer' },
  { value: 'MARKETING_HEAD', label: 'Marketing Head' },
  { value: 'ENGINEERING_LEAD', label: 'Engineering Lead' },
];
const ROLE_BADGE: Record<string, string> = {
  FOUNDER: 'badge-purple', CO_FOUNDER: 'badge-purple',
  CTO: 'badge-blue', CPO: 'badge-blue',
  MARKETING_HEAD: 'badge-green', ENGINEERING_LEAD: 'badge-yellow',
};
const ROLE_LABEL: Record<string, string> = {
  FOUNDER: 'Founder', CO_FOUNDER: 'Co-Founder', CTO: 'CTO', CPO: 'CPO',
  MARKETING_HEAD: 'Marketing Head', ENGINEERING_LEAD: 'Engineering Lead',
};

function mergeUserData(authSummaries: AuthUser[], profiles: UserProfile[]): MergedUser[] {
  const profileMap: Record<number, UserProfile> = {};
  profiles.forEach((p) => { profileMap[p.userId] = p; });
  return authSummaries.map((auth) => {
    const profile = profileMap[auth.userId] || ({} as Partial<UserProfile>);
    return { userId: auth.userId, name: profile.name || auth.name, email: profile.email || auth.email, bio: profile.bio, skills: profile.skills, experience: profile.experience, hasProfile: !!profile.userId };
  });
}

const shimmer = {
  background: 'linear-gradient(90deg, var(--surface-2) 25%, var(--surface-3) 50%, var(--surface-2) 75%)',
  backgroundSize: '200% 100%', animation: 'shimmer 1.4s infinite',
};

export default function TeamManagement(): React.ReactElement {
  const { startupId } = useParams<TeamManagementParams>();
  const parsedStartupId = Number(startupId);

  const [skillInput, setSkillInput] = useState<string>('');
  const debouncedSkillInput = useDebounce(skillInput, 400);
  const [searchMode, setSearchMode] = useState<'role' | 'role+skill'>('role');
  const [searchResults, setSearchResults] = useState<MergedUser[]>([]);
  const [searching, setSearching] = useState<boolean>(false);
  const [searched, setSearched] = useState<boolean>(false);

  const [selected, setSelected] = useState<MergedUser | null>(null);
  const [selectedRole, setSelectedRole] = useState<string>('CTO');
  const [inviting, setInviting] = useState<boolean>(false);
  const [showInviteForm, setShowInviteForm] = useState<boolean>(false);

  const [members, setMembers] = useState<TeamMember[]>([]);
  const [memberProfiles, setMemberProfiles] = useState<Record<number, MemberProfile>>({});
  const [loadingTeam, setLoadingTeam] = useState<boolean>(true);

  const [editingMemberId, setEditingMemberId] = useState<number | null>(null);
  const [editingRole, setEditingRole] = useState<string>('');
  const [updatingRole, setUpdatingRole] = useState<boolean>(false);

  const loadTeam = useCallback(async () => {
    if (!startupId || Number.isNaN(parsedStartupId)) { toast.error('Invalid startup'); setLoadingTeam(false); return; }
    setLoadingTeam(true);
    try {
      const res = await getTeamByStartup(parsedStartupId);
      const teamMembers: TeamMember[] = res.data || [];
      setMembers(teamMembers);
      if (teamMembers.length > 0) {
        const ids = teamMembers.map((m) => m.userId);
        const profileMap: Record<number, MemberProfile> = {};
        const authResults = await Promise.allSettled(ids.map((id) => getAuthUserById(id)));
        authResults.forEach((result, i) => { if (result.status === 'fulfilled') { const u = result.value.data; profileMap[ids[i]] = { userId: u.userId, name: u.name, email: u.email }; } });
        try { const profileRes = await getProfilesBatch(ids, ''); (profileRes.data || []).forEach((p: UserProfile) => { profileMap[p.userId] = { ...profileMap[p.userId], ...p }; }); } catch { /* optional */ }
        setMemberProfiles(profileMap);
      }
    } catch { toast.error('Failed to load team'); }
    finally { setLoadingTeam(false); }
  }, [parsedStartupId, startupId]);

  useEffect(() => { loadTeam(); }, [loadTeam]);

  const handleSearch = async (): Promise<void> => {
    if (searchMode === 'role+skill' && !debouncedSkillInput.trim()) { toast.error('Enter a skill to search with'); return; }
    setSearching(true); setSearched(false); setSelected(null);
    try {
      let coFounders: AuthUser[] = [];
      try {
        const authRes = await getCoFounderIds();
        coFounders = authRes.data || [];
      } catch {
        const fallbackRes = await getUsersByRole('ROLE_COFOUNDER');
        coFounders = fallbackRes.data || [];
      }

      if (coFounders.length === 0) { setSearchResults([]); setSearched(true); return; }
      const allIds = coFounders.map((u) => u.userId);
      const skill = searchMode === 'role+skill' ? debouncedSkillInput.trim() : '';
      let profiles: UserProfile[] = [];
      try {
        const profileRes = await getProfilesBatch(allIds, skill);
        profiles = profileRes.data || [];
      } catch {
        // Profiles are optional for this screen; still show auth users so founders can invite.
        profiles = [];
      }

      let merged: MergedUser[];
      if (searchMode === 'role') { merged = mergeUserData(coFounders, profiles); }
      else { const matchedIds = new Set(profiles.map((p) => p.userId)); merged = mergeUserData(coFounders.filter((u) => matchedIds.has(u.userId)), profiles); }
      setSearchResults(merged); setSearched(true);
    } catch { toast.error('Search failed. Please try again.'); }
    finally { setSearching(false); }
  };

  useEffect(() => {
    if (!showInviteForm || searching) return;
    if (searchMode === 'role') {
      handleSearch().catch(() => undefined);
    }
  }, [showInviteForm, searchMode]);

  const handleSelect = (user: MergedUser): void => { if (alreadyOnTeam(user.userId)) return; setSelected((prev) => (prev?.userId === user.userId ? null : user)); setSelectedRole('CTO'); };
  const handleInvite = async (): Promise<void> => {
    if (!selected) return;
    if (alreadyOnTeam(selected.userId)) { toast.error(`${selected.name} is already on the team.`); return; }
    if (!startupId || Number.isNaN(parsedStartupId)) { toast.error('Invalid startup'); return; }
    setInviting(true);
    try { await inviteCoFounder({ startupId: parsedStartupId, invitedUserId: selected.userId, role: selectedRole }); toast.success(`Invite sent to ${selected.name}!`); setSelected(null); setSearchResults([]); setSearched(false); setSkillInput(''); setShowInviteForm(false); loadTeam(); }
    catch (err: any) { toast.error(err.response?.data?.message || 'Failed to send invite.'); }
    finally { setInviting(false); }
  };

  const parseSkills = (skills: string): string[] => skills ? skills.split(',').map((s) => s.trim()).filter(Boolean) : [];
  const alreadyOnTeam = (userId: number): boolean => members.some((m) => m.userId === userId);
  const handleRoleUpdate = async (memberId: number): Promise<void> => {
    setUpdatingRole(true);
    try { await updateMemberRole(memberId, { role: editingRole }); toast.success('Role updated'); setEditingMemberId(null); setEditingRole(''); loadTeam(); }
    catch (err: any) { toast.error(err.response?.data?.message || 'Failed to update role'); }
    finally { setUpdatingRole(false); }
  };

  return (
    <Layout>
      <style>{`@keyframes shimmer { 0% { background-position: 200% 0 } 100% { background-position: -200% 0 } }`}</style>
      <div style={{ maxWidth: 800, margin: '0 auto' }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 28, flexWrap: 'wrap', gap: 12 }}>
          <div>
            <span style={{ fontSize: 11, fontWeight: 600, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--brand)', display: 'block', marginBottom: 4 }}>Founder</span>
            <h1 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 28, fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>Team management</h1>
            <p style={{ color: 'var(--text-muted)', fontSize: 14, marginTop: 4 }}>Startup #{startupId} — invite and manage your co-founders</p>
          </div>
          <button
            onClick={() => setShowInviteForm(v => !v)}
            className="btn-primary"
            style={{ gap: 6 }}
          >
            <Plus size={14} /> Invite co-founder
          </button>
        </div>

        {/* Inline invite form — slide-down */}
        {showInviteForm && (
          <div style={{ background: 'var(--surface)', border: '1px solid var(--brand-border)', borderRadius: 16, padding: 24, marginBottom: 20, boxShadow: 'var(--shadow-card)' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 18 }}>
              <p style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: 8 }}>
                <UserPlus size={15} style={{ color: 'var(--brand)' }} /> Search and invite a co-founder
              </p>
              <button onClick={() => { setShowInviteForm(false); setSelected(null); setSearchResults([]); setSearched(false); setSkillInput(''); }} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted)', padding: 4 }}>
                <X size={16} />
              </button>
            </div>

            {/* Search controls */}
            <div style={{ display: 'flex', gap: 8, marginBottom: 14, flexWrap: 'wrap' }}>
              <div style={{ display: 'flex', gap: 4 }}>
                {(['role', 'role+skill'] as const).map((m) => (
                  <button key={m} onClick={() => setSearchMode(m)} style={{ padding: '6px 12px', borderRadius: 7, fontSize: 12, fontWeight: 500, border: 'none', cursor: 'pointer', background: searchMode === m ? 'var(--brand)' : 'var(--surface-2)', color: searchMode === m ? '#fff' : 'var(--text-muted)', fontFamily: "'DM Sans', system-ui, sans-serif" }}>
                    {m === 'role' ? 'All co-founders' : 'Filter by skill'}
                  </button>
                ))}
              </div>
              {searchMode === 'role+skill' && (
                <input
                  className="input-field"
                  style={{ flex: 1, minWidth: 160 }}
                  placeholder="Enter skill (e.g. React, Java)…"
                  value={skillInput}
                  onChange={(e) => setSkillInput(e.target.value)}
                />
              )}
              <button onClick={handleSearch} disabled={searching} className="btn-primary" style={{ gap: 6 }}>
                <Search size={13} /> {searching ? 'Searching…' : 'Search'}
              </button>
            </div>

            {/* Results */}
            {searching ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {[1,2].map(i => <div key={i} style={{ height: 56, borderRadius: 12, ...shimmer }} />)}
              </div>
            ) : searched && searchResults.length === 0 ? (
              <p style={{ fontSize: 13, color: 'var(--text-muted)', padding: '16px 0', textAlign: 'center' }}>No co-founders found matching your criteria.</p>
            ) : searched && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8, maxHeight: 280, overflowY: 'auto', marginBottom: 14 }}>
                {searchResults.map((u) => {
                  const isMember = alreadyOnTeam(u.userId);
                  const isSelected = selected?.userId === u.userId;
                  return (
                    <div
                      key={u.userId}
                      onClick={() => handleSelect(u)}
                      style={{
                        padding: '12px 16px', borderRadius: 12,
                        border: `1px solid ${isSelected ? 'var(--brand)' : 'var(--border)'}`,
                        background: isSelected ? 'var(--brand-subtle)' : 'var(--surface-2)',
                        cursor: isMember ? 'not-allowed' : 'pointer',
                        opacity: isMember ? 0.5 : 1,
                        display: 'flex', alignItems: 'center', gap: 12,
                        transition: 'border-color 150ms',
                      }}
                    >
                      <div style={{ width: 36, height: 36, borderRadius: '50%', background: isSelected ? 'var(--brand-subtle)' : 'var(--surface-3)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, fontSize: 14, fontWeight: 700, color: isSelected ? 'var(--brand)' : 'var(--text-muted)' }}>
                        {isSelected ? <CheckCircle size={18} style={{ color: 'var(--brand)' }} /> : (u.name?.[0] || '?').toUpperCase()}
                      </div>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap', marginBottom: 2 }}>
                          <p style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)' }}>{u.name}</p>
                          {isMember && <span className="badge-green" style={{ fontSize: 10 }}>In team</span>}
                        </div>
                        <p style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: u.skills ? 4 : 0 }}>{u.email}</p>
                        {u.skills && (
                          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
                            {parseSkills(u.skills).slice(0, 4).map((s) => (
                              <span key={s} style={{ fontSize: 10, padding: '2px 6px', borderRadius: 5, background: 'var(--surface-3)', color: 'var(--text-muted)', border: '1px solid var(--border)' }}>{s}</span>
                            ))}
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}

            {/* Send invite */}
            {selected && (
              <div style={{ display: 'flex', gap: 10, alignItems: 'flex-end', paddingTop: 14, borderTop: '1px solid var(--border)' }}>
                <div style={{ flex: 1 }}>
                  <label style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', display: 'block', marginBottom: 6 }}>
                    Role for {selected.name}
                  </label>
                  <select className="input-field" value={selectedRole} onChange={(e) => setSelectedRole(e.target.value)}>
                    {INVITE_ROLES.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
                  </select>
                </div>
                <button onClick={handleInvite} disabled={inviting} className="btn-primary" style={{ gap: 6 }}>
                  <UserPlus size={14} /> {inviting ? 'Sending…' : 'Send invite'}
                </button>
              </div>
            )}
          </div>
        )}

        {/* Current team */}
        <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, overflow: 'hidden', boxShadow: 'var(--shadow-card)' }}>
          <div style={{ padding: '16px 24px', borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', gap: 8 }}>
            <Users size={15} style={{ color: 'var(--brand)' }} />
            <h2 style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-secondary)', margin: 0 }}>Current team</h2>
            {members.length > 0 && <span className="badge-blue" style={{ fontSize: 10, marginLeft: 4 }}>{members.length}</span>}
          </div>

          {loadingTeam ? (
            <div style={{ padding: 20 }}>
              {[1,2].map(i => <div key={i} style={{ height: 56, borderRadius: 12, marginBottom: 8, ...shimmer }} />)}
            </div>
          ) : members.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '48px 24px' }}>
              <div style={{ width: 40, height: 40, borderRadius: 10, background: 'var(--brand-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 12px' }}>
                <Users size={20} style={{ color: 'var(--brand)' }} />
              </div>
              <p style={{ fontSize: 14, fontWeight: 500, color: 'var(--text-primary)', marginBottom: 4 }}>No team members yet</p>
              <p style={{ fontSize: 12, color: 'var(--text-muted)' }}>Invite co-founders using the button above</p>
            </div>
          ) : (
            members.map((m, idx) => {
              const profile = memberProfiles[m.userId];
              const displayName = profile?.name || `User #${m.userId}`;
              const displayEmail = profile?.email;
              const isEditing = editingMemberId === m.id;
              return (
                <div key={m.id} style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '14px 24px', borderBottom: idx < members.length - 1 ? '1px solid var(--border)' : 'none', flexWrap: 'wrap' }}>
                  <div style={{ width: 38, height: 38, borderRadius: 10, background: m.role === 'FOUNDER' ? 'var(--brand-subtle)' : 'var(--purple-bg)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, fontSize: 14, fontWeight: 700, color: m.role === 'FOUNDER' ? 'var(--brand)' : 'var(--purple)' }}>
                    {(displayName[0] || '?').toUpperCase()}
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <p style={{ fontSize: 14, fontWeight: 500, color: 'var(--text-primary)', marginBottom: 3 }}>{displayName}</p>
                    <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', alignItems: 'center' }}>
                      <span className={ROLE_BADGE[m.role] || 'badge-blue'} style={{ fontSize: 10 }}>{ROLE_LABEL[m.role] || m.role}</span>
                      {displayEmail && <span style={{ fontSize: 11, color: 'var(--text-faint)' }}>{displayEmail}</span>}
                    </div>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    {isEditing ? (
                      <>
                        <select className="input-field" style={{ height: 34, padding: '0 28px 0 10px', fontSize: 12 }} value={editingRole} onChange={(e) => setEditingRole(e.target.value)} disabled={updatingRole}>
                          {UPDATE_ROLES.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
                        </select>
                        <button onClick={() => handleRoleUpdate(m.id)} disabled={updatingRole} style={{ width: 30, height: 30, borderRadius: 7, background: 'var(--green-bg)', border: '1px solid rgba(22,163,74,0.2)', color: 'var(--green)', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                          <Check size={13} />
                        </button>
                        <button onClick={() => { setEditingMemberId(null); setEditingRole(''); }} style={{ width: 30, height: 30, borderRadius: 7, background: 'var(--surface-2)', border: '1px solid var(--border)', color: 'var(--text-muted)', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                          <X size={13} />
                        </button>
                      </>
                    ) : (
                      m.role !== 'FOUNDER' && (
                        <button onClick={() => { setEditingMemberId(m.id); setEditingRole(m.role); }} style={{ width: 30, height: 30, borderRadius: 7, background: 'var(--surface-2)', border: '1px solid var(--border)', color: 'var(--text-muted)', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                          <Pencil size={12} />
                        </button>
                      )
                    )}
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>
    </Layout>
  );
}
