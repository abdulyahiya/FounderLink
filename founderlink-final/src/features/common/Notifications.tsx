import React, { useCallback, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { Bell, CheckCheck, Mail } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Layout from '../../shared/components/Layout';
import useAuth from '../../shared/hooks/useAuth';
import { getNotifications, markAsRead } from '../../core/api/notificationApi';
import { setUnreadCount } from '../../store/slices/notificationSlice';
import { Notification } from '../../types';

const TYPE_LABELS: Record<string, string> = {
  USER_REGISTERED: 'Welcome', STARTUP_CREATED: 'Startup', STARTUP_REJECTED: 'Startup',
  INVESTMENT_CREATED: 'Investment', INVESTMENT_APPROVED: 'Investment', INVESTMENT_REJECTED: 'Investment',
  TEAM_INVITE_SENT: 'Team', PAYMENT_SUCCESS: 'Payment', PAYMENT_REJECTED: 'Payment',
};

const TYPE_COLOR: Record<string, string> = {
  USER_REGISTERED: 'var(--brand)', STARTUP_CREATED: 'var(--blue)', STARTUP_REJECTED: 'var(--red)',
  INVESTMENT_CREATED: 'var(--green)', INVESTMENT_APPROVED: 'var(--green)', INVESTMENT_REJECTED: 'var(--red)',
  TEAM_INVITE_SENT: 'var(--purple)', PAYMENT_SUCCESS: 'var(--green)', PAYMENT_REJECTED: 'var(--red)',
};

const shimmer = {
  background: 'linear-gradient(90deg, var(--surface-2) 25%, var(--surface-3) 50%, var(--surface-2) 75%)',
  backgroundSize: '200% 100%', animation: 'shimmer 1.4s infinite',
};

const Notifications: React.FC = () => {
  const { userId } = useAuth();
  const dispatch = useDispatch();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    if (!userId) { setLoading(false); return; }
    getNotifications(userId)
      .then((res) => {
        const payload = res.data as Notification[] | { data?: Notification[] };
        const data = Array.isArray(payload) ? payload : payload.data || [];
        setNotifications(data);
        dispatch(setUnreadCount(data.filter((n) => !n.isRead).length));
      })
      .catch(() => toast.error('Failed to load notifications'))
      .finally(() => setLoading(false));
  }, [dispatch, userId]);

  useEffect(() => { load(); }, [load]);

  const handleMarkRead = async (id: number): Promise<void> => {
    try { await markAsRead(id); load(); }
    catch { toast.error('Failed'); }
  };

  const handleMarkAll = async (): Promise<void> => {
    const unread = notifications.filter((n) => !n.isRead);
    await Promise.all(unread.map((n) => markAsRead(n.id).catch(() => {})));
    load();
  };

  const unread = notifications.filter((n) => !n.isRead);

  const relativeTime = (date: string): string => {
    const diff = Date.now() - new Date(date).getTime();
    if (diff < 60000) return 'just now';
    if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`;
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}h ago`;
    return `${Math.floor(diff / 86400000)}d ago`;
  };

  return (
    <Layout>
      <style>{`@keyframes shimmer { 0% { background-position: 200% 0 } 100% { background-position: -200% 0 } }`}</style>
      <div style={{ maxWidth: 720, margin: '0 auto' }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 24, flexWrap: 'wrap', gap: 12 }}>
          <div>
            <span style={{ fontSize: 11, fontWeight: 600, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--brand)', display: 'block', marginBottom: 4 }}>Notifications</span>
            <h1 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 28, fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>
              {loading ? 'Loading…' : unread.length > 0 ? `${unread.length} unread` : 'All caught up'}
            </h1>
          </div>
          {unread.length > 0 && (
            <button onClick={handleMarkAll} style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: 13, color: 'var(--brand)', fontWeight: 500, display: 'flex', alignItems: 'center', gap: 5 }}>
              <CheckCheck size={14} /> Mark all as read
            </button>
          )}
        </div>

        {loading ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {[1,2,3].map(i => <div key={i} style={{ height: 72, borderRadius: 14, ...shimmer }} />)}
          </div>
        ) : notifications.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '60px 24px', background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16 }}>
            <div style={{ width: 40, height: 40, borderRadius: 10, background: 'var(--brand-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 12px' }}>
              <Bell size={20} style={{ color: 'var(--brand)' }} />
            </div>
            <p style={{ fontSize: 14, fontWeight: 500, color: 'var(--text-primary)', marginBottom: 4 }}>All caught up!</p>
            <p style={{ fontSize: 12, color: 'var(--text-muted)' }}>No notifications yet.</p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }} role="list">
            {notifications.map((n) => {
              const color = TYPE_COLOR[n.type] || 'var(--brand)';
              return (
                <div
                  key={n.id}
                  role="listitem"
                  style={{
                    display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 14,
                    padding: '14px 18px',
                    borderRadius: 14,
                    border: '1px solid var(--border)',
                    borderLeft: `3px solid ${n.isRead ? 'var(--border)' : color}`,
                    background: 'var(--surface)',
                    boxShadow: 'var(--shadow-card)',
                    opacity: n.isRead ? 0.5 : 1,
                    transition: 'opacity 200ms',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12, flex: 1, minWidth: 0 }}>
                    <div style={{ width: 32, height: 32, borderRadius: 8, background: `${color}1a`, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, marginTop: 2 }}>
                      <Mail size={14} style={{ color }} />
                    </div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <p style={{ fontSize: 13, color: 'var(--text-primary)', lineHeight: 1.5, marginBottom: 4 }}>{n.message}</p>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                        <span style={{ fontSize: 11, color: 'var(--text-faint)' }}>{relativeTime(n.createdAt)}</span>
                        <span style={{ fontSize: 10, fontWeight: 600, color, background: `${color}1a`, padding: '2px 7px', borderRadius: 20 }}>
                          {TYPE_LABELS[n.type] || n.type}
                        </span>
                      </div>
                    </div>
                  </div>
                  {!n.isRead && (
                    <button
                      onClick={() => handleMarkRead(n.id)}
                      style={{ background: 'none', border: '1px solid var(--border)', borderRadius: 7, cursor: 'pointer', fontSize: 11, color: 'var(--text-muted)', padding: '4px 10px', flexShrink: 0, display: 'flex', alignItems: 'center', gap: 4 }}
                    >
                      <CheckCheck size={11} /> Read
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </Layout>
  );
};

export default Notifications;
