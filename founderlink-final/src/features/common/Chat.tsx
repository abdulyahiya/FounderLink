import React, { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Send, ArrowLeft, Wifi, WifiOff } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Layout from '../../shared/components/Layout';
import useAuth from '../../shared/hooks/useAuth';
import { getConversationMessages, sendMessage } from '../../core/api/messagingApi';
import { getAuthUserById } from '../../core/api/userApi';
import { AuthUser, Message, RouteLocationState } from '../../types';

const MESSAGING_WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8086/ws';

const Chat: React.FC = () => {
  const { conversationId } = useParams<{ conversationId: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const state = (location.state || {}) as RouteLocationState;
  const { userId } = useAuth();

  const [messages, setMessages] = useState<Message[]>([]);
  const [otherUser, setOtherUser] = useState<AuthUser | null>(null);
  const [resolvedOtherUserId, setResolvedOtherUserId] = useState<number | null>(state.otherUserId || null);
  const [text, setText] = useState('');
  const [sending, setSending] = useState(false);
  const [connected, setConnected] = useState(false);

  const bottomRef = useRef<HTMLDivElement | null>(null);
  const stompClientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!conversationId) return;
    getConversationMessages(Number(conversationId))
      .then((res) => {
        const payload = res.data as Message[] | { data?: Message[] };
        const loaded = Array.isArray(payload) ? payload : payload.data || [];
        setMessages(loaded);
        if (!state.otherUserId && loaded.length > 0 && userId) {
          const derivedId = loaded.find((m) => m.senderId !== userId)?.senderId;
          if (derivedId) setResolvedOtherUserId(derivedId);
        }
      })
      .catch(() => toast.error('Failed to load messages'));
  }, [conversationId, state.otherUserId, userId]);

  useEffect(() => {
    if (!resolvedOtherUserId) return;
    getAuthUserById(resolvedOtherUserId).then((res) => setOtherUser(res.data)).catch(() => undefined);
  }, [resolvedOtherUserId]);

  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: 'smooth' }); }, [messages]);

  useEffect(() => {
    if (!conversationId) return;
    const token = localStorage.getItem('token');
    if (!token) return;
    const client = new Client({
      webSocketFactory: () => new SockJS(MESSAGING_WS_URL),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/conversation/${conversationId}`, (frame: IMessage) => {
          const newMsg = JSON.parse(frame.body) as Message;
          setMessages((prev) => prev.some((m) => m.id === newMsg.id) ? prev : [...prev, newMsg]);
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setConnected(false),
      onWebSocketClose: () => setConnected(false),
    });
    client.activate();
    stompClientRef.current = client;
    return () => { client.deactivate(); stompClientRef.current = null; setConnected(false); };
  }, [conversationId]);

  const handleSend = async (): Promise<void> => {
    if (!text.trim() || !resolvedOtherUserId) return;
    setSending(true);
    const content = text;
    setText('');
    try { await sendMessage({ receiverId: resolvedOtherUserId, content }); }
    catch { toast.error('Failed to send'); setText(content); }
    finally { setSending(false); }
  };

  const displayName = otherUser?.name || otherUser?.email || (resolvedOtherUserId ? `User #${resolvedOtherUserId}` : `Conversation #${conversationId}`);
  const initials = (name?: string, email?: string) => (name?.[0] || email?.[0] || '?').toUpperCase();

  return (
    <Layout>
      <div style={{ maxWidth: 720, margin: '0 auto', display: 'flex', flexDirection: 'column', height: 'calc(100vh - 140px)' }}>
        {/* Header */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16, gap: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <button
              onClick={() => navigate('/messages')}
              style={{ width: 34, height: 34, borderRadius: 8, background: 'var(--surface-2)', border: '1px solid var(--border)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)', cursor: 'pointer' }}
            >
              <ArrowLeft size={15} />
            </button>
            <div style={{ width: 36, height: 36, borderRadius: '50%', background: 'var(--brand-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 14, fontWeight: 700, color: 'var(--brand)' }}>
              {initials(otherUser?.name, otherUser?.email)}
            </div>
            <h1 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 17, fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>{displayName}</h1>
          </div>
          <div style={{
            display: 'flex', alignItems: 'center', gap: 5,
            fontSize: 11, fontWeight: 500, padding: '4px 10px', borderRadius: 20,
            background: connected ? 'var(--green-bg)' : 'var(--surface-2)',
            color: connected ? 'var(--green)' : 'var(--text-muted)',
          }}>
            {connected ? <><Wifi size={11} /> Live</> : <><WifiOff size={11} /> Connecting…</>}
          </div>
        </div>

        {/* Message area */}
        <div style={{
          flex: 1, overflowY: 'auto', padding: 20,
          background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16,
          boxShadow: 'var(--shadow-card)', marginBottom: 12,
          display: 'flex', flexDirection: 'column', gap: 8,
        }}>
          {messages.length === 0 ? (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
              <p style={{ fontSize: 13, color: 'var(--text-faint)' }}>No messages yet. Start the conversation!</p>
            </div>
          ) : (
            messages.map((m) => {
              const isMine = m.senderId === userId;
              return (
                <div key={m.id} style={{ display: 'flex', justifyContent: isMine ? 'flex-end' : 'flex-start' }}>
                  <div style={{
                    maxWidth: '72%', padding: '9px 14px',
                    borderRadius: isMine ? '14px 14px 3px 14px' : '14px 14px 14px 3px',
                    background: isMine ? 'var(--brand)' : 'var(--surface-2)',
                    color: isMine ? '#fff' : 'var(--text-primary)',
                    fontSize: 14, lineHeight: 1.5,
                    border: isMine ? 'none' : '1px solid var(--border)',
                  }}>
                    {m.content}
                  </div>
                </div>
              );
            })
          )}
          <div ref={bottomRef} />
        </div>

        {/* Input bar */}
        <div style={{ display: 'flex', gap: 10 }}>
          <input
            className="input-field"
            style={{ flex: 1 }}
            placeholder="Type a message…"
            value={text}
            onChange={(e) => setText(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); void handleSend(); } }}
          />
          <button
            onClick={() => void handleSend()}
            disabled={sending || !text.trim() || !resolvedOtherUserId}
            className="btn-primary"
            style={{ padding: '0 18px', gap: 6 }}
          >
            <Send size={15} />
          </button>
        </div>
      </div>
    </Layout>
  );
};

export default Chat;
