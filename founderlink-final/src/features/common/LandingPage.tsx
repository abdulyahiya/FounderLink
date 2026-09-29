import React, { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, Menu, X, Check } from 'lucide-react';

/* ─── Scroll fade-in hook ─────────────────────────────────────────────────── */
const useFadeIn = (threshold = 0.15) => {
  const ref = useRef<HTMLDivElement>(null);
  const [visible, setVisible] = useState(false);
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const obs = new IntersectionObserver(
      ([entry]) => { if (entry.isIntersecting) { setVisible(true); obs.disconnect(); } },
      { threshold }
    );
    obs.observe(el);
    return () => obs.disconnect();
  }, [threshold]);
  return { ref, visible };
};

const FadeSection: React.FC<{ children: React.ReactNode; className?: string; style?: React.CSSProperties }> = ({ children, className = '', style }) => {
  const { ref, visible } = useFadeIn();
  return (
    <div
      ref={ref}
      className={className}
      style={{
        opacity: visible ? 1 : 0,
        transform: visible ? 'none' : 'translateY(20px)',
        transition: 'opacity 0.6s ease, transform 0.6s ease',
        ...style,
      }}
    >
      {children}
    </div>
  );
};

const Logo: React.FC<{ size?: number }> = ({ size = 32 }) => (
  <div
    style={{
      width: size, height: size,
      borderRadius: Math.round(size * 0.3),
      background: 'var(--brand)',
      boxShadow: '0 2px 8px rgba(5,150,105,0.35)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      color: '#fff', fontWeight: 700, flexShrink: 0,
      fontFamily: "'Syne', system-ui, sans-serif",
      fontSize: size * 0.34,
    }}
  >
    FL
  </div>
);

const LandingPage: React.FC = () => {
  const [scrolled, setScrolled] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 30);
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  const navLinks = [
    { href: '#features', label: 'Features' },
    { href: '#how-it-works', label: 'How it works' },
    { href: '#roles', label: 'Roles' },
  ];

  const stats = [
    { num: '2,400+', label: 'founders on the platform' },
    { num: '₹12Cr+', label: 'deployed through Razorpay' },
    { num: '340+', label: 'successful co-founder matches' },
  ];

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg)', color: 'var(--text-primary)', overflowX: 'hidden' }}>

      {/* ── NAVBAR ─────────────────────────────────────────────────────────── */}
      <nav
        style={{
          position: 'fixed', top: 0, left: 0, right: 0, zIndex: 50,
          transition: 'all 0.3s ease',
          backdropFilter: scrolled ? 'blur(14px)' : 'none',
          WebkitBackdropFilter: scrolled ? 'blur(14px)' : 'none',
          background: scrolled ? 'rgba(248,247,244,0.92)' : 'transparent',
          borderBottom: scrolled ? '1px solid var(--border)' : '1px solid transparent',
        }}
      >
        <div style={{ maxWidth: 1100, margin: '0 auto', padding: '14px 24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: 10, textDecoration: 'none' }}>
            <Logo size={34} />
            <span style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 18, fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
              FounderLink
            </span>
          </Link>

          <div style={{ display: 'none' }} className="md:flex items-center gap-8">
            {navLinks.map(({ href, label }) => (
              <a key={href} href={href}
                style={{ fontSize: 14, fontWeight: 500, color: 'var(--text-muted)', textDecoration: 'none', transition: 'color 150ms' }}
                onMouseEnter={e => ((e.target as HTMLElement).style.color = 'var(--text-primary)')}
                onMouseLeave={e => ((e.target as HTMLElement).style.color = 'var(--text-muted)')}
              >{label}</a>
            ))}
          </div>

          <div style={{ display: 'none' }} className="md:flex items-center gap-3">
            <Link to="/login" className="btn-secondary" style={{ textDecoration: 'none', padding: '8px 18px' }}>Sign in</Link>
            <Link to="/register" className="btn-primary" style={{ textDecoration: 'none' }}>
              Join now <ArrowRight size={14} />
            </Link>
          </div>

          <button
            className="md:hidden"
            style={{ padding: 8, borderRadius: 8, background: 'var(--surface-2)', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)' }}
            onClick={() => setMobileOpen(v => !v)}
            aria-label="Toggle menu"
          >
            {mobileOpen ? <X size={20} /> : <Menu size={20} />}
          </button>
        </div>

        {mobileOpen && (
          <div style={{ background: 'var(--surface)', borderTop: '1px solid var(--border)', padding: '16px 24px 24px', display: 'flex', flexDirection: 'column', gap: 12 }}>
            {navLinks.map(({ href, label }) => (
              <a key={href} href={href} onClick={() => setMobileOpen(false)}
                style={{ fontSize: 15, fontWeight: 500, color: 'var(--text-secondary)', textDecoration: 'none', padding: '8px 0' }}
              >{label}</a>
            ))}
            <div style={{ height: 1, background: 'var(--border)', margin: '4px 0' }} />
            <Link to="/login" style={{ textDecoration: 'none', color: 'var(--text-secondary)', fontWeight: 500, fontSize: 15 }}>Sign in</Link>
            <Link to="/register" className="btn-primary" style={{ textDecoration: 'none', justifyContent: 'center' }}>Join now</Link>
          </div>
        )}
      </nav>

      {/* ── HERO ───────────────────────────────────────────────────────────── */}
      <section
        style={{
          paddingTop: 'clamp(110px, 14vw, 160px)',
          paddingBottom: 'clamp(60px, 8vw, 100px)',
          textAlign: 'center',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        {/* Dot-grid background */}
        <div
          aria-hidden="true"
          style={{
            position: 'absolute', inset: 0, zIndex: 0,
            backgroundImage: 'radial-gradient(circle, var(--border-medium) 1px, transparent 1px)',
            backgroundSize: '28px 28px',
            maskImage: 'radial-gradient(ellipse 80% 60% at 50% 50%, black 40%, transparent 100%)',
            WebkitMaskImage: 'radial-gradient(ellipse 80% 60% at 50% 50%, black 40%, transparent 100%)',
          }}
        />

        <div style={{ maxWidth: 780, margin: '0 auto', padding: '0 24px', position: 'relative', zIndex: 1 }}>
          <div
            style={{
              display: 'inline-flex', alignItems: 'center', gap: 8,
              background: 'var(--brand-subtle)', border: '1px solid var(--brand-border)',
              borderRadius: 100, padding: '5px 14px', marginBottom: 28,
              fontSize: 12, fontWeight: 600, color: 'var(--brand)', letterSpacing: '0.04em',
              textTransform: 'uppercase',
            }}
          >
            <span style={{ width: 6, height: 6, borderRadius: '50%', background: 'var(--brand)', flexShrink: 0 }} />
            India's startup investment platform
          </div>

          <h1
            style={{
              fontFamily: "'Syne', system-ui, sans-serif",
              fontSize: 'clamp(38px, 6vw, 68px)',
              fontWeight: 700,
              lineHeight: 1.05,
              letterSpacing: '-0.03em',
              color: 'var(--text-primary)',
              marginBottom: 22,
            }}
          >
            Where founders meet<br />
            <span style={{ color: 'var(--brand)' }}>capital and co-builders.</span>
          </h1>

          <p
            style={{
              fontSize: 'clamp(15px, 2vw, 18px)',
              color: 'var(--text-secondary)',
              lineHeight: 1.6,
              marginBottom: 36,
              maxWidth: 520,
              margin: '0 auto 36px',
              fontFamily: "'DM Sans', system-ui, sans-serif",
            }}
          >
            A single platform for founders to raise funding, find co-founders, and close investments through Razorpay.
          </p>

          <div style={{ display: 'flex', gap: 12, justifyContent: 'center', flexWrap: 'wrap', marginBottom: 48 }}>
            <Link to="/register" className="btn-primary" style={{ textDecoration: 'none', padding: '12px 28px', fontSize: 15 }}>
              Start for free <ArrowRight size={15} />
            </Link>
            <Link to="/login" className="btn-secondary" style={{ textDecoration: 'none', padding: '12px 28px', fontSize: 15 }}>
              Sign in
            </Link>
          </div>

          {/* Trust strip — avatars + text */}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 12, flexWrap: 'wrap' }}>
            <div style={{ display: 'flex' }}>
              {['A', 'R', 'S', 'M'].map((initial, i) => (
                <div
                  key={i}
                  style={{
                    width: 32, height: 32, borderRadius: '50%',
                    background: ['#059669', '#7c3aed', '#2563eb', '#d97706'][i],
                    border: '2px solid var(--bg)',
                    marginLeft: i > 0 ? -10 : 0,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: 12, fontWeight: 600, color: '#fff',
                    fontFamily: "'DM Sans', system-ui, sans-serif",
                  }}
                >{initial}</div>
              ))}
            </div>
            <span style={{ fontSize: 13, color: 'var(--text-muted)' }}>
              Trusted by 2,400+ founders across India
            </span>
          </div>
        </div>
      </section>

      {/* ── 3-COLUMN PROOF STRIP ──────────────────────────────────────────── */}
      <FadeSection>
        <div style={{ maxWidth: 1100, margin: '0 auto', padding: '0 24px' }}>
          <div style={{ height: 1, background: 'var(--border)' }} />
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(3, 1fr)',
              gap: 0,
            }}
          >
            {stats.map((stat, i) => (
              <div
                key={i}
                style={{
                  padding: '32px 24px',
                  textAlign: 'center',
                  borderRight: i < stats.length - 1 ? '1px solid var(--border)' : 'none',
                }}
              >
                <div
                  style={{
                    fontFamily: "'Syne', system-ui, sans-serif",
                    fontSize: 'clamp(28px, 3.5vw, 40px)',
                    fontWeight: 700,
                    color: 'var(--text-primary)',
                    marginBottom: 4,
                  }}
                >
                  {stat.num}
                </div>
                <div style={{ fontSize: 13, color: 'var(--text-muted)' }}>{stat.label}</div>
              </div>
            ))}
          </div>
          <div style={{ height: 1, background: 'var(--border)' }} />
        </div>
      </FadeSection>

      {/* ── FEATURE SPOTLIGHT (alternating) ──────────────────────────────── */}
      <section id="features" style={{ padding: 'clamp(60px, 8vw, 100px) 0' }}>
        <div style={{ maxWidth: 1100, margin: '0 auto', padding: '0 24px' }}>
          <FadeSection style={{ marginBottom: 60 }}>
            <div style={{ textAlign: 'center', marginBottom: 48 }}>
              <span style={{ fontSize: 11, fontWeight: 600, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--brand)' }}>
                Platform features
              </span>
              <h2 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 'clamp(26px, 3.5vw, 36px)', fontWeight: 700, marginTop: 8, color: 'var(--text-primary)' }}>
                Built for how startups actually work
              </h2>
            </div>
          </FadeSection>

          {/* Feature 1 — text left, code block right */}
          <FadeSection>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
                gap: 48,
                alignItems: 'center',
                marginBottom: 80,
              }}
            >
              <div>
                <span style={{ fontSize: 11, fontWeight: 600, letterSpacing: '0.08em', textTransform: 'uppercase', color: 'var(--brand)', marginBottom: 12, display: 'block' }}>
                  Real-time messaging
                </span>
                <h3 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 'clamp(22px, 2.5vw, 28px)', fontWeight: 700, color: 'var(--text-primary)', marginBottom: 16, lineHeight: 1.2 }}>
                  Conversations that close deals faster
                </h3>
                <p style={{ color: 'var(--text-secondary)', fontSize: 15, lineHeight: 1.7, marginBottom: 20 }}>
                  Founders and investors connect through a persistent WebSocket chat — no email threads, no delays. Conversations are grouped by user so context is never lost between funding rounds.
                </p>
                <p style={{ color: 'var(--text-muted)', fontSize: 14, lineHeight: 1.7 }}>
                  Notifications arrive in real time via STOMP over SockJS, so every party stays updated without refreshing the page.
                </p>
              </div>
              <div
                style={{
                  background: 'var(--surface)',
                  border: '1px solid var(--border)',
                  borderRadius: 16,
                  overflow: 'hidden',
                  boxShadow: 'var(--shadow-card)',
                }}
              >
                <div style={{ padding: '12px 16px', background: 'var(--surface-2)', borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', gap: 8 }}>
                  <div style={{ display: 'flex', gap: 6 }}>
                    {['#f87171','#fbbf24','#34d399'].map((c, i) => <div key={i} style={{ width: 10, height: 10, borderRadius: '50%', background: c }} />)}
                  </div>
                  <span style={{ fontSize: 12, color: 'var(--text-faint)' }}>live messages</span>
                </div>
                <div style={{ padding: 20, display: 'flex', flexDirection: 'column', gap: 10 }}>
                  {[
                    { from: 'Arjun (Founder)', msg: 'We just closed our MVP. 800 DAUs this week.', align: 'left' },
                    { from: "Riya (Investor)", msg: "That traction is exactly what we look for. Let's schedule a call.", align: "right" },
                    { from: 'Arjun (Founder)', msg: 'Thursday works. Sending calendar invite now.', align: 'left' },
                  ].map((item, i) => (
                    <div key={i} style={{ display: 'flex', flexDirection: 'column', alignItems: item.align === 'right' ? 'flex-end' : 'flex-start' }}>
                      <div style={{ fontSize: 10, color: 'var(--text-faint)', marginBottom: 3 }}>{item.from}</div>
                      <div style={{
                        padding: '8px 12px', borderRadius: item.align === 'right' ? '12px 12px 2px 12px' : '12px 12px 12px 2px',
                        background: item.align === 'right' ? 'var(--brand)' : 'var(--surface-2)',
                        color: item.align === 'right' ? '#fff' : 'var(--text-primary)',
                        fontSize: 13, maxWidth: '80%', lineHeight: 1.4,
                      }}>
                        {item.msg}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </FadeSection>

          {/* Feature 2 — diagram left, text right */}
          <FadeSection>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
                gap: 48,
                alignItems: 'center',
              }}
            >
              <div
                style={{
                  background: 'var(--surface)',
                  border: '1px solid var(--border)',
                  borderRadius: 16,
                  padding: 24,
                  boxShadow: 'var(--shadow-card)',
                }}
              >
                <div style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-muted)', marginBottom: 20, textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                  Razorpay payment flow
                </div>
                {[
                  { step: '01', title: 'Investor submits amount', color: 'var(--brand)' },
                  { step: '02', title: 'Order created via Razorpay API', color: 'var(--blue)' },
                  { step: '03', title: 'Founder receives & approves', color: 'var(--amber)' },
                  { step: '04', title: 'Funds confirmed, records updated', color: 'var(--green)' },
                ].map((item, i) => (
                  <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 14, marginBottom: i < 3 ? 0 : 0 }}>
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                      <div style={{
                        width: 32, height: 32, borderRadius: 8,
                        background: item.color + '1a',
                        border: `1px solid ${item.color}33`,
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        fontSize: 11, fontWeight: 700, color: item.color,
                        flexShrink: 0,
                      }}>
                        {item.step}
                      </div>
                      {i < 3 && <div style={{ width: 1, height: 20, background: 'var(--border)' }} />}
                    </div>
                    <div style={{ fontSize: 14, color: 'var(--text-primary)', paddingBottom: i < 3 ? 20 : 0, lineHeight: 1.4 }}>
                      {item.title}
                    </div>
                  </div>
                ))}
              </div>
              <div>
                <span style={{ fontSize: 11, fontWeight: 600, letterSpacing: '0.08em', textTransform: 'uppercase', color: 'var(--brand)', marginBottom: 12, display: 'block' }}>
                  Razorpay payments
                </span>
                <h3 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 'clamp(22px, 2.5vw, 28px)', fontWeight: 700, color: 'var(--text-primary)', marginBottom: 16, lineHeight: 1.2 }}>
                  Investments settled in four steps
                </h3>
                <p style={{ color: 'var(--text-secondary)', fontSize: 15, lineHeight: 1.7, marginBottom: 20 }}>
                  Every investment goes through a verified Razorpay order, giving both parties a transparent payment trail and instant email receipts. No wire transfers, no manual reconciliation.
                </p>
                <p style={{ color: 'var(--text-muted)', fontSize: 14, lineHeight: 1.7 }}>
                  Founders review each payment before acceptance. Refunds on rejection are handled automatically through the Razorpay refund API.
                </p>
              </div>
            </div>
          </FadeSection>
        </div>
      </section>

      {/* ── ROLE CARDS ────────────────────────────────────────────────────── */}
      <section id="roles" style={{ padding: 'clamp(60px, 8vw, 100px) 0', background: 'var(--surface-2)' }}>
        <FadeSection>
          <div style={{ maxWidth: 1100, margin: '0 auto', padding: '0 24px' }}>
            <div style={{ marginBottom: 48 }}>
              <span style={{ fontSize: 11, fontWeight: 600, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--brand)', marginBottom: 8, display: 'block' }}>
                Built for every role
              </span>
              <h2 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 'clamp(26px, 3.5vw, 36px)', fontWeight: 700, color: 'var(--text-primary)' }}>
                One platform, three paths
              </h2>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              {[
                {
                  role: 'Founder',
                  tagline: 'Build your startup, raise capital, and grow your team — all in one dashboard.',
                  color: 'var(--brand)',
                  bg: 'var(--brand-subtle)',
                  border: 'var(--brand-border)',
                  points: ['Create and manage multiple startup profiles', 'Accept or reject investment requests', 'Invite co-founders and assign roles'],
                  cta: 'Register as a founder',
                },
                {
                  role: 'Investor',
                  tagline: 'Discover vetted startups, invest securely via Razorpay, and track every rupee.',
                  color: 'var(--green)',
                  bg: 'var(--green-bg)',
                  border: 'rgba(22,163,74,0.2)',
                  points: ['Browse startups filtered by stage and industry', 'Invest directly through an embedded Razorpay checkout', 'View full payment history and investment status'],
                  cta: 'Register as an investor',
                },
                {
                  role: 'Co-Founder',
                  tagline: 'Join early-stage startups that match your skills and ambition.',
                  color: 'var(--purple)',
                  bg: 'var(--purple-bg)',
                  border: 'rgba(124,58,237,0.2)',
                  points: ['Browse all approved startups on the platform', 'Receive and review team invitations from founders', 'Build your profile with skills and experience details'],
                  cta: 'Register as a co-founder',
                },
              ].map((item, i) => (
                <div
                  key={i}
                  style={{
                    background: 'var(--surface)',
                    border: '1px solid var(--border)',
                    borderRadius: 16,
                    padding: 'clamp(20px, 3vw, 28px)',
                    display: 'grid',
                    gridTemplateColumns: 'auto 1fr auto',
                    gap: 24,
                    alignItems: 'center',
                    boxShadow: 'var(--shadow-card)',
                  }}
                  className="flex-col md:grid"
                >
                  <div
                    style={{
                      width: 80,
                      padding: '6px 12px',
                      borderRadius: 8,
                      background: item.bg,
                      border: `1px solid ${item.border}`,
                      textAlign: 'center',
                      fontSize: 13, fontWeight: 700, color: item.color,
                      flexShrink: 0,
                      fontFamily: "'Syne', system-ui, sans-serif",
                    }}
                  >
                    {item.role}
                  </div>
                  <div>
                    <p style={{ color: 'var(--text-secondary)', fontSize: 14, marginBottom: 12, lineHeight: 1.5 }}>{item.tagline}</p>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px 16px' }}>
                      {item.points.map((pt, j) => (
                        <div key={j} style={{ display: 'flex', alignItems: 'flex-start', gap: 7, fontSize: 13, color: 'var(--text-muted)' }}>
                          <Check size={13} style={{ color: item.color, marginTop: 1, flexShrink: 0 }} />
                          {pt}
                        </div>
                      ))}
                    </div>
                  </div>
                  <Link
                    to="/register"
                    style={{
                      fontSize: 13, fontWeight: 600, color: item.color,
                      textDecoration: 'none', whiteSpace: 'nowrap',
                      flexShrink: 0,
                      display: 'flex', alignItems: 'center', gap: 4,
                    }}
                    onMouseEnter={e => ((e.currentTarget as HTMLElement).style.textDecoration = 'underline')}
                    onMouseLeave={e => ((e.currentTarget as HTMLElement).style.textDecoration = 'none')}
                  >
                    {item.cta} <ArrowRight size={13} />
                  </Link>
                </div>
              ))}
            </div>
          </div>
        </FadeSection>
      </section>

      {/* ── PAYMENT FLOW (vertical timeline) ─────────────────────────────── */}
      <section id="how-it-works" style={{ padding: 'clamp(60px, 8vw, 100px) 0' }}>
        <FadeSection>
          <div style={{ maxWidth: 680, margin: '0 auto', padding: '0 24px' }}>
            <div style={{ textAlign: 'center', marginBottom: 48 }}>
              <span style={{ fontSize: 11, fontWeight: 600, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--brand)', marginBottom: 8, display: 'block' }}>
                How it works
              </span>
              <h2 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 'clamp(26px, 3.5vw, 36px)', fontWeight: 700, color: 'var(--text-primary)' }}>
                From signup to funded in four steps
              </h2>
            </div>

            <div style={{ position: 'relative' }}>
              {[
                { n: '1', title: 'Create your profile', desc: 'Register with your role. Founders list their startups; investors and co-founders complete a public profile.' },
                { n: '2', title: 'Get discovered or discover', desc: 'All approved startups are browsable by investors and co-founders. Filter by stage, industry, or location.' },
                { n: '3', title: 'Connect and decide', desc: 'Use real-time messaging to ask questions, share decks, and align on terms before committing.' },
                { n: '4', title: 'Close the deal', desc: 'Investments are placed through Razorpay. Founders approve, funds are confirmed, and both parties receive a receipt.' },
              ].map((item, i) => (
                <div key={i} style={{ display: 'flex', gap: 20, marginBottom: i < 3 ? 32 : 0 }}>
                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', flexShrink: 0 }}>
                    <div style={{
                      width: 40, height: 40, borderRadius: 10,
                      background: 'var(--brand-subtle)',
                      border: '1px solid var(--brand-border)',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontFamily: "'Syne', system-ui, sans-serif",
                      fontWeight: 700, fontSize: 16, color: 'var(--brand)',
                    }}>
                      {item.n}
                    </div>
                    {i < 3 && <div style={{ width: 1, flex: 1, minHeight: 20, background: 'var(--border)', marginTop: 8 }} />}
                  </div>
                  <div style={{ paddingBottom: i < 3 ? 32 : 0 }}>
                    <h3 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 17, fontWeight: 600, color: 'var(--text-primary)', marginBottom: 6 }}>
                      {item.title}
                    </h3>
                    <p style={{ color: 'var(--text-muted)', fontSize: 14, lineHeight: 1.6 }}>{item.desc}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </FadeSection>
      </section>

      {/* ── SINGLE TESTIMONIAL ────────────────────────────────────────────── */}
      <section style={{ padding: 'clamp(60px, 8vw, 100px) 0', background: 'var(--surface-2)' }}>
        <FadeSection>
          <div style={{ maxWidth: 680, margin: '0 auto', padding: '0 24px', textAlign: 'center' }}>
            <div style={{ fontSize: 64, lineHeight: 0.8, color: 'var(--brand)', fontFamily: "'Syne', serif", marginBottom: 24, textAlign: 'left' }}>
              "
            </div>
            <p
              style={{
                fontFamily: "'Syne', system-ui, sans-serif",
                fontSize: 'clamp(18px, 2.5vw, 24px)',
                fontWeight: 500,
                color: 'var(--text-primary)',
                lineHeight: 1.5,
                marginBottom: 24,
              }}
            >
              We closed our first investor within a week of listing. The Razorpay flow made everything feel legitimate.
            </p>
            <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>
              Mihir Desai — co-founder, Konnect Labs, Ahmedabad
            </p>
          </div>
        </FadeSection>
      </section>

      {/* ── CTA ───────────────────────────────────────────────────────────── */}
      <section style={{ padding: 'clamp(70px, 9vw, 110px) 0', background: '#0F1210' }}>
        <FadeSection>
          <div style={{ textAlign: 'center', padding: '0 24px' }}>
            <h2
              style={{
                fontFamily: "'Syne', system-ui, sans-serif",
                fontSize: 'clamp(28px, 4vw, 46px)',
                fontWeight: 700, color: '#F5F5F0',
                marginBottom: 28, letterSpacing: '-0.02em',
              }}
            >
              Your startup's next chapter starts here.
            </h2>
            <Link
              to="/register"
              className="btn-primary"
              style={{ textDecoration: 'none', padding: '14px 32px', fontSize: 16 }}
            >
              Open a free account <ArrowRight size={16} />
            </Link>
          </div>
        </FadeSection>
      </section>

      {/* ── FOOTER ────────────────────────────────────────────────────────── */}
      <footer
        style={{
          background: '#0F1210',
          borderTop: '1px solid rgba(255,255,255,0.07)',
          padding: '20px 24px',
        }}
      >
        <div
          style={{
            maxWidth: 1100, margin: '0 auto',
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            flexWrap: 'wrap', gap: 16,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <Logo size={28} />
            <span style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 15, fontWeight: 700, color: '#F5F5F0' }}>
              FounderLink
            </span>
          </div>
          <span style={{ fontSize: 12, color: 'rgba(255,255,255,0.35)' }}>
            © {new Date().getFullYear()} FounderLink. All rights reserved.
          </span>
          <div style={{ display: 'flex', gap: 24 }}>
            {['Privacy', 'Terms', 'Contact'].map(label => (
              <a
                key={label}
                href="#"
                style={{ fontSize: 13, color: 'rgba(255,255,255,0.4)', textDecoration: 'none', transition: 'color 150ms' }}
                onMouseEnter={e => ((e.target as HTMLElement).style.color = 'rgba(255,255,255,0.8)')}
                onMouseLeave={e => ((e.target as HTMLElement).style.color = 'rgba(255,255,255,0.4)')}
              >
                {label}
              </a>
            ))}
          </div>
        </div>
      </footer>
    </div>
  );
};

export default LandingPage;
