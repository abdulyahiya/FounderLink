import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Plus, Edit, Trash2, Users, Rocket, MapPin, TrendingUp, ChevronRight } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Layout from '../../shared/components/Layout';
import useAuth from '../../shared/hooks/useAuth';
import { getStartupsByFounder, deleteStartup } from '../../core/api/startupApi';
import { Startup } from '../../types';

const shimmer = {
  background: 'linear-gradient(90deg, var(--surface-2) 25%, var(--surface-3) 50%, var(--surface-2) 75%)',
  backgroundSize: '200% 100%', animation: 'shimmer 1.4s infinite',
};

/* Deterministic accent per startup initial */
const ACCENTS = [
  { bg: 'rgba(5,150,105,0.12)',  color: 'var(--brand)' },
  { bg: 'rgba(37,99,235,0.12)',  color: 'var(--blue)' },
  { bg: 'rgba(124,58,237,0.12)', color: 'var(--purple)' },
  { bg: 'rgba(217,119,6,0.12)',  color: 'var(--amber)' },
  { bg: 'rgba(22,163,74,0.12)',  color: 'var(--green)' },
];
const accent = (name: string) => ACCENTS[name.charCodeAt(0) % ACCENTS.length];

const MyStartups: React.FC = () => {
  const { userId } = useAuth();
  const [startups, setStartups] = useState<Startup[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  const load = useCallback(() => {
    if (!userId) return;
    getStartupsByFounder(userId)
      .then((res) => setStartups(res.data || []))
      .catch(() => toast.error('Failed to load'))
      .finally(() => setLoading(false));
  }, [userId]);

  useEffect(() => { load(); }, [load]);

  const handleDelete = async (id: number): Promise<void> => {
    if (!window.confirm('Delete this startup?')) return;
    setStartups((prev) => prev.filter((s) => s.id !== id));
    try {
      await deleteStartup(id);
      toast.success('Startup deleted');
    } catch {
      toast.error('Failed to delete');
      load();
    }
  };

  const statusConfig = (startup: Startup) => {
    if (startup.isApproved)  return { label: 'Approved',  cls: 'badge-green',  dot: 'var(--green)' };
    if (startup.isRejected)  return { label: 'Rejected',  cls: 'badge-red',    dot: 'var(--red)' };
    return                          { label: 'Pending',   cls: 'badge-yellow', dot: 'var(--amber)' };
  };

  const stageLabel = (stage: string) =>
    stage === 'EARLY_TRACTION' ? 'Early Traction' : stage;

  return (
    <Layout>
      <style>{`
        @keyframes shimmer { 0% { background-position: 200% 0 } 100% { background-position: -200% 0 } }
        @keyframes fadeSlideUp {
          from { opacity: 0; transform: translateY(12px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        .startup-card {
          background: var(--surface);
          border: 1px solid var(--border);
          border-radius: 16px;
          padding: 20px 22px;
          box-shadow: var(--shadow-card);
          transition: transform 200ms ease, box-shadow 200ms ease, border-color 200ms ease;
          animation: fadeSlideUp 0.35s ease both;
          display: flex;
          flex-direction: column;
          gap: 14px;
          position: relative;
          overflow: hidden;
        }
        .startup-card::before {
          content: '';
          position: absolute;
          top: 0; left: 0; right: 0;
          height: 3px;
          background: linear-gradient(90deg, var(--brand), var(--brand-light));
          opacity: 0;
          transition: opacity 200ms ease;
        }
        .startup-card:hover {
          transform: translateY(-3px);
          box-shadow: var(--shadow-hover);
          border-color: var(--brand-border);
        }
        .startup-card:hover::before { opacity: 1; }

        .action-btn {
          width: 34px; height: 34px;
          border-radius: 9px;
          display: flex; align-items: center; justify-content: center;
          transition: background 150ms ease, color 150ms ease, transform 150ms ease;
          text-decoration: none;
          border: 1px solid var(--border);
          cursor: pointer;
          flex-shrink: 0;
        }
        .action-btn:hover { transform: scale(1.08); }
        .action-btn-ghost {
          background: var(--surface-2);
          color: var(--text-muted);
        }
        .action-btn-ghost:hover { background: var(--surface-3); color: var(--text-primary); }
        .action-btn-danger {
          background: var(--red-bg);
          color: var(--red);
          border-color: rgba(220,38,38,0.15);
        }
        .action-btn-danger:hover { background: rgba(220,38,38,0.16); }

        .view-link {
          display: flex; align-items: center; gap: 4px;
          font-size: 12px; font-weight: 500;
          color: var(--brand); text-decoration: none;
          opacity: 0;
          transition: opacity 180ms ease;
        }
        .startup-card:hover .view-link { opacity: 1; }

        .stat-pill {
          display: flex; align-items: center; gap: 5px;
          font-size: 11px; color: var(--text-muted);
          background: var(--surface-2);
          border: 1px solid var(--border);
          border-radius: 8px;
          padding: 4px 9px;
          white-space: nowrap;
        }

        .status-dot {
          width: 6px; height: 6px;
          border-radius: 50%;
          display: inline-block;
          margin-right: 4px;
          flex-shrink: 0;
        }
      `}</style>

      <div style={{ maxWidth: 980, margin: '0 auto' }}>

        {/* ── Page header ─────────────────────────────────────────────── */}
        <div style={{
          display: 'flex', alignItems: 'flex-start',
          justifyContent: 'space-between',
          marginBottom: 28, gap: 16, flexWrap: 'wrap',
        }}>
          <div>
            <span style={{
              fontSize: 11, fontWeight: 600, letterSpacing: '0.1em',
              textTransform: 'uppercase', color: 'var(--brand)',
              display: 'block', marginBottom: 4,
            }}>Founder</span>
            <h1 style={{
              fontFamily: "'Syne', system-ui, sans-serif",
              fontSize: 28, fontWeight: 700,
              color: 'var(--text-primary)', margin: 0,
            }}>My startups</h1>
            <p style={{ color: 'var(--text-muted)', fontSize: 13, marginTop: 5 }}>
              {loading
                ? 'Loading…'
                : `${startups.length} startup${startups.length !== 1 ? 's' : ''} in your portfolio`}
            </p>
          </div>

          <Link
            to="/founder/startups/create"
            className="btn-primary"
            style={{ textDecoration: 'none', gap: 6 }}
          >
            <Plus size={15} /> New startup
          </Link>
        </div>

        {/* ── Summary strip ───────────────────────────────────────────── */}
        {!loading && startups.length > 0 && (() => {
          const approved = startups.filter(s => s.isApproved).length;
          const pending  = startups.filter(s => !s.isApproved && !s.isRejected).length;
          const rejected = startups.filter(s => s.isRejected).length;
          return (
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
              background: 'var(--surface)',
              border: '1px solid var(--border)',
              borderRadius: 14,
              overflow: 'hidden',
              boxShadow: 'var(--shadow-card)',
              marginBottom: 24,
            }}>
              {[
                { label: 'Approved',  value: approved, color: 'var(--green)' },
                { label: 'Pending',   value: pending,  color: 'var(--amber)' },
                { label: 'Rejected',  value: rejected, color: 'var(--red)' },
              ].map((m, i, arr) => (
                <div key={m.label} style={{
                  padding: '16px 20px',
                  borderRight: i < arr.length - 1 ? '1px solid var(--border)' : 'none',
                  display: 'flex', flexDirection: 'column', gap: 4,
                }}>
                  <span style={{ fontSize: 11, color: 'var(--text-muted)', fontWeight: 500 }}>{m.label}</span>
                  <span style={{
                    fontFamily: "'Syne', system-ui, sans-serif",
                    fontSize: 22, fontWeight: 700,
                    color: m.color, lineHeight: 1,
                  }}>{m.value}</span>
                </div>
              ))}
            </div>
          );
        })()}

        {/* ── Content ─────────────────────────────────────────────────── */}
        {loading ? (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 14 }}>
            {[1, 2, 3, 4].map(i => (
              <div key={i} style={{ height: 180, borderRadius: 16, ...shimmer }} />
            ))}
          </div>

        ) : startups.length === 0 ? (

          /* ── Empty state ──────────────────────────────────────────── */
          <div style={{
            textAlign: 'center',
            padding: '72px 24px',
            background: 'var(--surface)',
            border: '1px dashed var(--border-medium)',
            borderRadius: 20,
          }}>
            <div style={{
              width: 56, height: 56, borderRadius: 16,
              background: 'var(--brand-subtle)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              margin: '0 auto 18px',
              boxShadow: '0 0 0 8px rgba(5,150,105,0.06)',
            }}>
              <Rocket size={24} style={{ color: 'var(--brand)' }} />
            </div>
            <p style={{
              fontSize: 17, fontWeight: 700,
              fontFamily: "'Syne', system-ui, sans-serif",
              color: 'var(--text-primary)', marginBottom: 8,
            }}>No startups yet</p>
            <p style={{ fontSize: 13, color: 'var(--text-muted)', marginBottom: 24, maxWidth: 300, margin: '0 auto 24px' }}>
              Create your first startup and start raising investment from investors.
            </p>
            <Link to="/founder/startups/create" className="btn-primary" style={{ textDecoration: 'none' }}>
              <Plus size={14} /> Create startup
            </Link>
          </div>

        ) : (

          /* ── Card grid ────────────────────────────────────────────── */
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(290px, 1fr))',
            gap: 14,
          }}>
            {startups.map((startup, idx) => {
              const ac     = accent(startup.name);
              const status = statusConfig(startup);
              return (
                <div
                  key={startup.id}
                  className="startup-card"
                  style={{ animationDelay: `${idx * 50}ms` }}
                >
                  {/* Top row: avatar + name + status */}
                  <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
                    <div style={{
                      width: 46, height: 46, borderRadius: 12,
                      background: ac.bg,
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontSize: 18, fontWeight: 700, color: ac.color,
                      flexShrink: 0,
                    }}>
                      {startup.name[0].toUpperCase()}
                    </div>

                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8, marginBottom: 4 }}>
                        <Link
                          to={`/founder/startups/${startup.id}`}
                          style={{
                            fontSize: 14, fontWeight: 700,
                            color: 'var(--text-primary)',
                            textDecoration: 'none',
                            fontFamily: "'Syne', system-ui, sans-serif",
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                            flex: 1,
                          }}
                          onMouseEnter={e => (e.currentTarget.style.color = 'var(--brand)')}
                          onMouseLeave={e => (e.currentTarget.style.color = 'var(--text-primary)')}
                        >
                          {startup.name}
                        </Link>
                        <span className={status.cls} style={{ fontSize: 10, flexShrink: 0, display: 'flex', alignItems: 'center' }}>
                          <span className="status-dot" style={{ background: status.dot }} />
                          {status.label}
                        </span>
                      </div>
                      <p style={{
                        fontSize: 12, color: 'var(--text-muted)', lineHeight: 1.5,
                        overflow: 'hidden',
                        display: '-webkit-box',
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: 'vertical' as const,
                        margin: 0,
                      }}>
                        {startup.description || `${startup.industry} startup based in ${startup.location}`}
                      </p>
                    </div>
                  </div>

                  {/* Meta pills */}
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                    <span className="stat-pill">
                      <TrendingUp size={10} style={{ color: 'var(--blue)' }} />
                      {startup.industry}
                    </span>
                    <span className="stat-pill">
                      <Rocket size={10} style={{ color: 'var(--purple)' }} />
                      {stageLabel(startup.stage)}
                    </span>
                    {startup.location && (
                      <span className="stat-pill">
                        <MapPin size={10} />
                        {startup.location}
                      </span>
                    )}
                  </div>

                  {/* Divider */}
                  <div style={{ height: 1, background: 'var(--border)' }} />

                  {/* Bottom: actions + view link */}
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}>
                    <div style={{ display: 'flex', gap: 7 }}>
                      <Link
                        to={`/founder/team/${startup.id}`}
                        title="Manage team"
                        className="action-btn action-btn-ghost"
                      >
                        <Users size={14} />
                      </Link>
                      <Link
                        to={`/founder/startups/${startup.id}/edit`}
                        title="Edit startup"
                        className="action-btn action-btn-ghost"
                      >
                        <Edit size={14} />
                      </Link>
                      <button
                        onClick={() => handleDelete(startup.id)}
                        title="Delete startup"
                        className="action-btn action-btn-danger"
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>

                    <Link to={`/founder/startups/${startup.id}`} className="view-link">
                      View details <ChevronRight size={12} />
                    </Link>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </Layout>
  );
};

export default MyStartups;
