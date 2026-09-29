import React, { useEffect, useMemo, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Link } from 'react-router-dom';
import { Search, ChevronLeft, ChevronRight, MapPin, TrendingUp, Rocket, IndianRupee } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Layout from '../../shared/components/Layout';
import {
  fetchStartups, setCurrentPage,
  selectStartups, selectStartupLoading, selectStartupError,
  selectTotalPages, selectTotalElements, selectCurrentPage,
} from '../../store/slices/startupSlice';
import useDebounce from '../../shared/hooks/useDebounce';
import useAuth from '../../shared/hooks/useAuth';
import { Startup } from '../../types';
import { AppDispatch } from '../../store/store';

const STAGES: string[] = ['All', 'IDEA', 'MVP', 'EARLY_TRACTION', 'SCALING'];
const PAGE_SIZE = 10;

const shimmer = {
  background: 'linear-gradient(90deg, var(--surface-2) 25%, var(--surface-3) 50%, var(--surface-2) 75%)',
  backgroundSize: '200% 100%', animation: 'shimmer 1.4s infinite',
};

/* Deterministic accent colour per startup initial */
const ACCENTS = [
  { bg: 'rgba(5,150,105,0.12)',  color: 'var(--brand)' },
  { bg: 'rgba(37,99,235,0.12)',  color: 'var(--blue)' },
  { bg: 'rgba(124,58,237,0.12)', color: 'var(--purple)' },
  { bg: 'rgba(217,119,6,0.12)',  color: 'var(--amber)' },
  { bg: 'rgba(22,163,74,0.12)',  color: 'var(--green)' },
];
const accent = (name: string) => ACCENTS[name.charCodeAt(0) % ACCENTS.length];

const stageLabel = (stage: string) =>
  stage === 'EARLY_TRACTION' ? 'Early Traction' : stage;

const BrowseStartups: React.FC = () => {
  const dispatch = useDispatch<AppDispatch>();
  const items          = useSelector(selectStartups);
  const loading        = useSelector(selectStartupLoading);
  const error          = useSelector(selectStartupError);
  const totalPages     = useSelector(selectTotalPages);
  const totalElements  = useSelector(selectTotalElements);
  const currentPage    = useSelector(selectCurrentPage);
  const { isInvestor } = useAuth();

  const [search,   setSearch]   = useState<string>('');
  const [stage,    setStage]    = useState<string>('All');
  const [location, setLocation] = useState<string>('');
  const debouncedSearch   = useDebounce<string>(search,   400);
  const debouncedLocation = useDebounce<string>(location, 400);

  useEffect(() => { dispatch(fetchStartups({ page: currentPage, size: PAGE_SIZE })); }, [dispatch, currentPage]);
  useEffect(() => { if (error) toast.error(error); }, [error]);

  const filtered = useMemo(() => {
    let result = items;
    if (debouncedSearch) {
      const q = debouncedSearch.toLowerCase();
      result = result.filter((s: Startup) => s.name.toLowerCase().includes(q) || s.industry.toLowerCase().includes(q));
    }
    if (stage !== 'All') result = result.filter((s: Startup) => s.stage === stage);
    if (debouncedLocation) {
      const loc = debouncedLocation.toLowerCase();
      result = result.filter((s: Startup) => s.location?.toLowerCase().includes(loc));
    }
    return result;
  }, [items, debouncedSearch, stage, debouncedLocation]);

  const handlePageChange = (page: number): void => {
    dispatch(setCurrentPage(page));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const getPageNumbers = (): (number | string)[] => {
    if (totalPages <= 7) return Array.from({ length: totalPages }, (_, i) => i);
    if (currentPage < 4) return [0, 1, 2, 3, 4, '...', totalPages - 1];
    if (currentPage > totalPages - 5) return [0, '...', totalPages - 5, totalPages - 4, totalPages - 3, totalPages - 2, totalPages - 1];
    return [0, '...', currentPage - 1, currentPage, currentPage + 1, '...', totalPages - 1];
  };

  const basePath = isInvestor ? '/investor' : '/cofounder';
  const roleLabel = isInvestor ? 'Investor' : 'Co-Founder';
  const roleColor = isInvestor ? 'var(--green)' : 'var(--purple)';

  return (
    <Layout>
      <style>{`
        @keyframes shimmer { 0% { background-position: 200% 0 } 100% { background-position: -200% 0 } }
        @keyframes fadeSlideUp {
          from { opacity: 0; transform: translateY(12px); }
          to   { opacity: 1; transform: translateY(0); }
        }

        .bs-card {
          background: var(--surface);
          border: 1px solid var(--border);
          border-radius: 16px;
          padding: 20px 22px;
          box-shadow: var(--shadow-card);
          transition: transform 200ms ease, box-shadow 200ms ease, border-color 200ms ease;
          animation: fadeSlideUp 0.35s ease both;
          display: flex;
          flex-direction: column;
          gap: 13px;
          position: relative;
          overflow: hidden;
          text-decoration: none;
        }
        .bs-card::before {
          content: '';
          position: absolute;
          top: 0; left: 0; right: 0;
          height: 3px;
          background: linear-gradient(90deg, var(--brand), var(--brand-light));
          opacity: 0;
          transition: opacity 200ms ease;
        }
        .bs-card:hover {
          transform: translateY(-3px);
          box-shadow: var(--shadow-hover);
          border-color: var(--brand-border);
        }
        .bs-card:hover::before { opacity: 1; }
        .bs-card:hover .bs-view-link { opacity: 1; }

        .bs-view-link {
          display: inline-flex; align-items: center; gap: 4px;
          font-size: 12px; font-weight: 600;
          color: var(--brand);
          opacity: 0;
          transition: opacity 180ms ease;
        }

        .bs-pill {
          display: inline-flex; align-items: center; gap: 5px;
          font-size: 11px; color: var(--text-muted);
          background: var(--surface-2);
          border: 1px solid var(--border);
          border-radius: 8px;
          padding: 4px 9px;
          white-space: nowrap;
        }

        .bs-stage-btn {
          padding: 6px 14px; border-radius: 8px;
          font-size: 12px; font-weight: 500;
          border: 1px solid var(--border);
          cursor: pointer;
          transition: all 150ms ease;
          font-family: 'DM Sans', system-ui, sans-serif;
          white-space: nowrap;
        }
        .bs-stage-btn-active {
          background: var(--brand); color: #fff;
          border-color: var(--brand);
          box-shadow: 0 1px 4px rgba(5,150,105,0.3);
        }
        .bs-stage-btn-inactive {
          background: var(--surface-2); color: var(--text-muted);
        }
        .bs-stage-btn-inactive:hover {
          background: var(--surface-3); color: var(--text-primary);
        }

        .bs-search-input {
          width: 100%; height: 38px;
          border: 1px solid var(--border-medium);
          border-radius: 9px;
          background: var(--surface-2);
          color: var(--text-primary);
          font-size: 13px;
          font-family: 'DM Sans', system-ui, sans-serif;
          outline: none;
          transition: border-color 150ms, box-shadow 150ms;
        }
        .bs-search-input:focus {
          border-color: var(--brand);
          box-shadow: 0 0 0 3px rgba(5,150,105,0.12);
        }

        .bs-page-btn {
          width: 34px; height: 34px;
          border-radius: 8px;
          border: 1px solid var(--border);
          font-size: 13px; font-weight: 500;
          cursor: pointer;
          display: flex; align-items: center; justify-content: center;
          transition: background 150ms, color 150ms, border-color 150ms;
          font-family: 'DM Sans', system-ui, sans-serif;
        }
        .bs-page-btn-active  { background: var(--brand); color: #fff; border-color: var(--brand); }
        .bs-page-btn-inactive { background: var(--surface); color: var(--text-muted); }
        .bs-page-btn-inactive:hover { background: var(--surface-2); color: var(--text-primary); }
        .bs-page-btn-nav { background: var(--surface); color: var(--text-muted); }
        .bs-page-btn-nav:hover:not(:disabled) { background: var(--surface-2); }
      `}</style>

      <div style={{ maxWidth: 980, margin: '0 auto' }}>

        {/* ── Page header ─────────────────────────────────────────────── */}
        <div style={{ marginBottom: 24 }}>
          <span style={{
            fontSize: 11, fontWeight: 600, letterSpacing: '0.1em',
            textTransform: 'uppercase', color: roleColor,
            display: 'block', marginBottom: 4,
          }}>
            {roleLabel}
          </span>
          <h1 style={{
            fontFamily: "'Syne', system-ui, sans-serif",
            fontSize: 28, fontWeight: 700,
            color: 'var(--text-primary)', margin: 0,
          }}>Browse startups</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: 13, marginTop: 5 }}>
            {totalElements} startup{totalElements !== 1 ? 's' : ''} available
          </p>
        </div>

        {/* ── Filter bar ──────────────────────────────────────────────── */}
        <div style={{
          background: 'var(--surface)',
          border: '1px solid var(--border)',
          borderRadius: 14,
          padding: '14px 18px',
          marginBottom: 22,
          boxShadow: 'var(--shadow-card)',
          display: 'flex',
          flexDirection: 'column',
          gap: 12,
        }}>
          {/* Search + location row */}
          <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
            <div style={{ position: 'relative', flex: '1 1 200px' }}>
              <Search size={14} style={{ position: 'absolute', left: 11, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-faint)', pointerEvents: 'none' }} />
              <input
                className="bs-search-input"
                style={{ paddingLeft: 34, paddingRight: 12 }}
                placeholder="Search by name or industry…"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
            <div style={{ position: 'relative', flex: '0 1 180px' }}>
              <MapPin size={13} style={{ position: 'absolute', left: 11, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-faint)', pointerEvents: 'none' }} />
              <input
                className="bs-search-input"
                style={{ paddingLeft: 30, paddingRight: 12 }}
                placeholder="Location…"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
              />
            </div>
          </div>

          {/* Stage filter pills */}
          <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', alignItems: 'center' }}>
            <span style={{ fontSize: 11, color: 'var(--text-faint)', fontWeight: 500, marginRight: 2 }}>Stage</span>
            {STAGES.map((s) => (
              <button
                key={s}
                onClick={() => setStage(s)}
                className={`bs-stage-btn ${stage === s ? 'bs-stage-btn-active' : 'bs-stage-btn-inactive'}`}
              >
                {s === 'EARLY_TRACTION' ? 'Early Traction' : s}
              </button>
            ))}
          </div>
        </div>

        {/* ── Results ─────────────────────────────────────────────────── */}
        {loading ? (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(290px, 1fr))', gap: 14 }}>
            {[1, 2, 3, 4, 5, 6].map(i => (
              <div key={i} style={{ height: 200, borderRadius: 16, ...shimmer }} />
            ))}
          </div>

        ) : filtered.length === 0 ? (

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
              background: 'var(--surface-2)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              margin: '0 auto 18px',
              boxShadow: '0 0 0 8px rgba(0,0,0,0.03)',
            }}>
              <Search size={24} style={{ color: 'var(--text-muted)' }} />
            </div>
            <p style={{
              fontSize: 16, fontWeight: 700,
              fontFamily: "'Syne', system-ui, sans-serif",
              color: 'var(--text-primary)', marginBottom: 6,
            }}>No startups found</p>
            <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>
              Try adjusting your search or filters
            </p>
          </div>

        ) : (

          /* ── Card grid ────────────────────────────────────────────── */
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(290px, 1fr))',
            gap: 14,
            marginBottom: 28,
          }}>
            {filtered.map((s: Startup, idx: number) => {
              const ac = accent(s.name);
              return (
                <Link
                  key={s.id}
                  to={`${basePath}/startups/${s.id}`}
                  className="bs-card"
                  style={{ animationDelay: `${idx * 40}ms` }}
                >
                  {/* Top: avatar + name + stage badge */}
                  <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
                    <div style={{
                      width: 46, height: 46, borderRadius: 12,
                      background: ac.bg,
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontSize: 18, fontWeight: 700, color: ac.color,
                      flexShrink: 0,
                    }}>
                      {s.name[0].toUpperCase()}
                    </div>

                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 6, marginBottom: 4 }}>
                        <span style={{
                          fontSize: 14, fontWeight: 700,
                          color: 'var(--text-primary)',
                          fontFamily: "'Syne', system-ui, sans-serif",
                          overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                          flex: 1,
                        }}>
                          {s.name}
                        </span>
                        <span className="badge-green" style={{ fontSize: 10, flexShrink: 0 }}>
                          {stageLabel(s.stage)}
                        </span>
                      </div>
                      <p style={{
                        fontSize: 12, color: 'var(--text-muted)', lineHeight: 1.55,
                        overflow: 'hidden',
                        display: '-webkit-box',
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: 'vertical' as const,
                        margin: 0,
                      }}>
                        {s.description || `A ${s.stage?.toLowerCase()} stage ${s.industry?.toLowerCase()} startup.`}
                      </p>
                    </div>
                  </div>

                  {/* Meta pills */}
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                    <span className="bs-pill">
                      <TrendingUp size={10} style={{ color: 'var(--blue)' }} />
                      {s.industry}
                    </span>
                    {s.location && (
                      <span className="bs-pill">
                        <MapPin size={10} />
                        {s.location}
                      </span>
                    )}
                    {s.fundingGoal && (
                      <span className="bs-pill" style={{ color: 'var(--green)', borderColor: 'rgba(22,163,74,0.2)', background: 'rgba(22,163,74,0.06)' }}>
                        <IndianRupee size={9} style={{ color: 'var(--green)' }} />
                        {Number(s.fundingGoal).toLocaleString('en-IN')}
                      </span>
                    )}
                  </div>

                  {/* Divider */}
                  <div style={{ height: 1, background: 'var(--border)' }} />

                  {/* Bottom row */}
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
                      {s.isApproved && (
                        <span className="badge-green" style={{ fontSize: 10 }}>✓ Approved</span>
                      )}
                    </div>
                    <span className="bs-view-link">
                      View details →
                    </span>
                  </div>
                </Link>
              );
            })}
          </div>
        )}

        {/* ── Pagination ──────────────────────────────────────────────── */}
        {totalPages > 1 && (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 5, paddingBottom: 8 }}>
            <button
              onClick={() => handlePageChange(currentPage - 1)}
              disabled={currentPage === 0}
              className="bs-page-btn bs-page-btn-nav"
              style={{ opacity: currentPage === 0 ? 0.35 : 1 }}
            >
              <ChevronLeft size={15} />
            </button>

            {getPageNumbers().map((p, idx) =>
              p === '...' ? (
                <span key={`el-${idx}`} style={{ padding: '0 4px', color: 'var(--text-faint)', fontSize: 13 }}>…</span>
              ) : (
                <button
                  key={p}
                  onClick={() => handlePageChange(p as number)}
                  className={`bs-page-btn ${p === currentPage ? 'bs-page-btn-active' : 'bs-page-btn-inactive'}`}
                >
                  {(p as number) + 1}
                </button>
              )
            )}

            <button
              onClick={() => handlePageChange(currentPage + 1)}
              disabled={currentPage >= totalPages - 1}
              className="bs-page-btn bs-page-btn-nav"
              style={{ opacity: currentPage >= totalPages - 1 ? 0.35 : 1 }}
            >
              <ChevronRight size={15} />
            </button>
          </div>
        )}
      </div>
    </Layout>
  );
};

export default BrowseStartups;
