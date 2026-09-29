import React from 'react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Bell, MessageSquare, LogOut, User, Sun, Moon } from 'lucide-react';
import { logout } from '../../store/slices/authSlice';
import { setUnreadCount } from '../../store/slices/notificationSlice';
import { toggleTheme, selectTheme } from '../../store/slices/themeSlice';
import { getUnreadNotifications } from '../../core/api/notificationApi';
import useAuth from '../hooks/useAuth';
import useNotificationSocket from '../hooks/useNotificationSocket';
import type { RootState } from '../../types';
import type { AppDispatch } from '../../store/store';

const Navbar: React.FC = () => {
  const dispatch = useDispatch<AppDispatch>();
  const navigate = useNavigate();
  const { user, userId, isFounder, isInvestor, isCoFounder } = useAuth();
  const unreadCount = useSelector((s: RootState) => s.notifications.unreadCount);
  const theme = useSelector(selectTheme);
  const [showLogoutModal, setShowLogoutModal] = useState(false);
  const cancelRef = useRef<HTMLButtonElement>(null);
  const modalRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!userId) return;
    getUnreadNotifications(userId)
      .then((res) => {
        const payload = res.data as { data?: Array<unknown> } | Array<unknown>;
        const list = Array.isArray(payload) ? payload : payload.data || [];
        dispatch(setUnreadCount(list.length));
      })
      .catch(() => {});
  }, [userId, dispatch]);

  const handleNewNotification = useCallback(() => {
    dispatch(setUnreadCount(unreadCount + 1));
  }, [dispatch, unreadCount]);

  useNotificationSocket(userId, handleNewNotification);

  useEffect(() => {
    if (!showLogoutModal) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') { setShowLogoutModal(false); return; }
      if (e.key === 'Tab') {
        const focusable = modalRef.current?.querySelectorAll<HTMLElement>(
          'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
        );
        if (!focusable || focusable.length === 0) return;
        const first = focusable[0];
        const last = focusable[focusable.length - 1];
        if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
        else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    setTimeout(() => cancelRef.current?.focus(), 10);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [showLogoutModal]);

  const confirmLogout = () => {
    setShowLogoutModal(false);
    dispatch(logout());
    navigate('/login');
  };

  let dashboardLink = '/admin/dashboard';
  if (isFounder) dashboardLink = '/founder/dashboard';
  else if (isInvestor) dashboardLink = '/investor/dashboard';
  else if (isCoFounder) dashboardLink = '/cofounder/dashboard';

  const iconCls = 'p-2 rounded-[8px] transition-all duration-150 hover:[background:var(--surface-2)]';

  return (
    <>
      <nav
        className="sticky top-0 z-50 px-6 py-3 flex items-center justify-between"
        style={{ background: 'var(--surface)', borderBottom: '1px solid var(--border)' }}
      >
        <Link to={dashboardLink} className="flex items-center gap-2.5">
          <div
            className="w-8 h-8 rounded-[10px] flex items-center justify-center text-white text-xs font-bold"
            style={{ background: 'var(--brand)', boxShadow: '0 1px 4px rgba(5,150,105,0.4)' }}
          >
            FL
          </div>
          <span
            className="text-base font-bold tracking-tight"
            style={{ fontFamily: "'Syne', system-ui, sans-serif", color: 'var(--text-primary)' }}
          >
            FounderLink
          </span>
        </Link>

        <div className="flex items-center gap-0.5">
          {/* Theme toggle — FIRST item, before notification bell */}
          <button
            onClick={() => dispatch(toggleTheme())}
            aria-label="Toggle theme"
            style={{
              width: 32,
              height: 32,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              borderRadius: 8,
              background: 'var(--surface-2)',
              color: 'var(--text-muted)',
              border: 'none',
              cursor: 'pointer',
              transition: 'background 150ms',
              marginRight: 2,
            }}
            onMouseEnter={e => (e.currentTarget.style.background = 'var(--surface-3)')}
            onMouseLeave={e => (e.currentTarget.style.background = 'var(--surface-2)')}
          >
            {theme === 'dark' ? <Sun size={17} /> : <Moon size={17} />}
          </button>

          {(isFounder || isInvestor || isCoFounder) && (
            <>
              <Link
                to="/notifications"
                aria-label="Notifications"
                className={`relative ${iconCls}`}
                style={{ color: 'var(--text-muted)' }}
              >
                <Bell size={17} />
                {unreadCount > 0 && (
                  <span
                    className="absolute top-1.5 right-1.5 w-3.5 h-3.5 text-white text-[9px] rounded-full flex items-center justify-center font-semibold"
                    style={{ background: 'var(--red)' }}
                  >
                    {unreadCount > 9 ? '9+' : unreadCount}
                  </span>
                )}
              </Link>
              <Link
                to="/messages"
                aria-label="Messages"
                className={iconCls}
                style={{ color: 'var(--text-muted)' }}
              >
                <MessageSquare size={17} />
              </Link>
            </>
          )}

          <Link to="/profile" aria-label="Profile" className={iconCls} style={{ color: 'var(--text-muted)' }}>
            <User size={17} />
          </Link>

          {user?.email && (
            <span
              className="text-xs hidden md:block mx-2 pl-3 font-medium"
              style={{ color: 'var(--text-muted)', borderLeft: '1px solid var(--border-medium)' }}
            >
              {user.email}
            </span>
          )}

          <button
            onClick={() => setShowLogoutModal(true)}
            aria-label="Sign out"
            title="Sign out"
            className={`${iconCls} hover:![color:var(--red)] hover:![background:var(--red-bg)]`}
            style={{ color: 'var(--text-muted)' }}
          >
            <LogOut size={17} />
          </button>
        </div>
      </nav>

      {showLogoutModal && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="logout-title"
          onClick={(e) => { if (e.target === e.currentTarget) setShowLogoutModal(false); }}
          style={{
            position: 'fixed', inset: 0, zIndex: 9999,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            background: 'rgba(0,0,0,0.4)',
            backdropFilter: 'blur(4px)', WebkitBackdropFilter: 'blur(4px)',
            padding: '1rem',
          }}
        >
          <div
            ref={modalRef}
            style={{
              background: 'var(--surface)',
              border: '1px solid var(--border)',
              borderRadius: 16,
              padding: '28px 32px',
              boxShadow: '0 16px 48px rgba(0,0,0,0.18)',
              width: '100%', maxWidth: 400,
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 12 }}>
              <div style={{
                width: 38, height: 38, borderRadius: 10,
                background: 'var(--red-bg)',
                display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
              }}>
                <LogOut size={17} style={{ color: 'var(--red)' }} />
              </div>
              <h2
                id="logout-title"
                style={{
                  fontFamily: "'Syne', system-ui, sans-serif",
                  fontSize: 18, fontWeight: 600,
                  color: 'var(--text-primary)', margin: 0,
                }}
              >
                Sign out
              </h2>
            </div>
            <p style={{
              color: 'var(--text-secondary)', fontSize: 14, lineHeight: 1.6,
              marginBottom: 24, fontFamily: "'DM Sans', system-ui, sans-serif",
            }}>
              You'll be signed out of your account. Any unsaved changes will be lost.
            </p>
            <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
              <button ref={cancelRef} onClick={() => setShowLogoutModal(false)} className="btn-secondary">
                Cancel
              </button>
              <button onClick={confirmLogout} className="btn-danger">
                Sign out
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default Navbar;
