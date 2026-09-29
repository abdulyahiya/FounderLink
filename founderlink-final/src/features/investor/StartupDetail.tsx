import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { MapPin, TrendingUp, DollarSign, Heart, ArrowLeft, Zap, CheckCircle, CreditCard, Shield } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Layout from '../../shared/components/Layout';
import useAuth from '../../shared/hooks/useAuth';
import { getStartupById, followStartup } from '../../core/api/startupApi';
import { getInvestmentsByStartup } from '../../core/api/investmentApi';
import { createOrder, verifyPayment, getPaymentsByStartup } from '../../core/api/paymentApi';
import { getAuthUserById } from '../../core/api/userApi';
import { investmentSchema } from '../../shared/utils/validationSchemas';
import { Startup, Investment, InvestmentFormData, RazorpayResponse, Payment } from '../../types';

const shimmer = {
  background: 'linear-gradient(90deg, var(--surface-2) 25%, var(--surface-3) 50%, var(--surface-2) 75%)',
  backgroundSize: '200% 100%', animation: 'shimmer 1.4s infinite',
};

const StartupDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { isInvestor, isCoFounder, user } = useAuth();
  const [startup, setStartup] = useState<Startup | null>(null);
  const [investments, setInvestments] = useState<Investment[]>([]);
  const [startupPayments, setStartupPayments] = useState<Payment[]>([]);
  const [paymentLoading, setPaymentLoading] = useState<boolean>(false);
  const [isFollowed, setIsFollowed] = useState<boolean>(false);
  const { register, handleSubmit, reset, formState: { errors } } = useForm<InvestmentFormData>({ resolver: yupResolver(investmentSchema) });

  useEffect(() => {
    if (!id) return;
    getStartupById(Number(id)).then(res => setStartup(res.data)).catch(() => toast.error('Failed to load'));
    getInvestmentsByStartup(Number(id)).then(res => setInvestments(res.data || [])).catch(() => {});
    getPaymentsByStartup(Number(id)).then(res => setStartupPayments(res.data || [])).catch(() => {});
    /* restore follow state persisted across sessions */
    const key = `fl_follow_${user?.userId}_${id}`;
    setIsFollowed(localStorage.getItem(key) === '1');
  }, [id, user]);

  const loadRazorpayScript = (): Promise<boolean> => {
    return new Promise((resolve) => {
      if (window.Razorpay) { resolve(true); return; }
      const script = document.createElement('script');
      script.src = 'https://checkout.razorpay.com/v1/checkout.js';
      script.onload = () => resolve(true);
      script.onerror = () => resolve(false);
      document.body.appendChild(script);
    });
  };

  const onInvest = async (data: InvestmentFormData): Promise<void> => {
    setPaymentLoading(true);
    try {
      const loaded = await loadRazorpayScript();
      if (!loaded) { toast.error('Failed to load payment gateway'); setPaymentLoading(false); return; }

      let founderEmail = '';
      try {
        const founderRes = await getAuthUserById(startup?.founderId as number);
        founderEmail = founderRes.data?.email || '';
      } catch { /* non-critical */ }

      const orderRes = await createOrder({
        investorId: user?.userId as number,
        founderId: startup?.founderId as number,
        startupId: parseInt(id!),
        startupName: startup?.name || '',
        investorName: user?.name || user?.email || '',
        investorEmail: user?.email || '',
        founderEmail,
        amount: parseFloat(String(data.amount)),
      });

      const { orderId, amount, currency, keyId } = orderRes.data;
      const options = {
        key: keyId, amount, currency,
        name: 'FounderLink',
        description: `Investment in ${startup!.name}`,
        order_id: orderId,
        handler: async (response: RazorpayResponse) => {
          try {
            const verifyRes = await verifyPayment({
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            });
            if (verifyRes.data.success) {
              toast.success('Payment successful! Investment confirmed.');
              reset();
              getInvestmentsByStartup(Number(id)).then(res => setInvestments(res.data || []));
              getPaymentsByStartup(Number(id)).then(res => setStartupPayments(res.data || []));
            } else { toast.error('Payment verification failed'); }
          } catch { toast.error('Error verifying payment'); }
        },
        prefill: { name: user?.name || '', email: user?.email || '' },
        theme: { color: '#059669' },
        modal: { ondismiss: () => toast('Payment cancelled', { icon: '⚠️' }) },
      };
      const rzp = new window.Razorpay(options);
      rzp.open();
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Failed to initiate payment');
    } finally { setPaymentLoading(false); }
  };

  const handleFollow = async (): Promise<void> => {
    const key = `fl_follow_${user?.userId}_${id}`;
    if (isFollowed) {
      toast('Already following this startup!', { icon: '💚' });
      return;
    }
    // Co-founders: the backend follow endpoint is restricted to investors only.
    // Track follow state client-side via localStorage to give the same UX.
    if (isCoFounder) {
      setIsFollowed(true);
      localStorage.setItem(key, '1');
      toast.success('Following this startup!');
      return;
    }
    try {
      await followStartup(Number(id));
      setIsFollowed(true);
      localStorage.setItem(key, '1');
      toast.success('Following this startup!');
    } catch (err: any) {
      const msg: string = err.response?.data?.message || '';
      if (msg.toLowerCase().includes('already')) {
        setIsFollowed(true);
        localStorage.setItem(key, '1');
        toast('Already following this startup!', { icon: '💚' });
      } else {
        toast.error(msg || 'Failed to follow');
      }
    }
  };

  if (!startup) {
    return (
      <Layout>
        <style>{`@keyframes shimmer { 0% { background-position: 200% 0 } 100% { background-position: -200% 0 } }`}</style>
        <div style={{ maxWidth: 1080, margin: '0 auto' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 24, alignItems: 'start' }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
              {[220, 160, 200].map((h, i) => <div key={i} style={{ height: h, borderRadius: 20, ...shimmer }} />)}
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              {[140, 200, 160].map((h, i) => <div key={i} style={{ height: h, borderRadius: 20, ...shimmer }} />)}
            </div>
          </div>
        </div>
      </Layout>
    );
  }

  const getInvestmentBadge = (status: string): string => {
    if (status === 'APPROVED') return 'badge-green';
    if (status === 'REJECTED') return 'badge-red';
    if (status === 'COMPLETED') return 'badge-blue';
    return 'badge-yellow';
  };

  const myPayments = startupPayments.filter(
    (p: Payment) => p.investorId === user?.userId && p.status === 'SUCCESS',
  );
  const myTotal = myPayments.reduce((sum: number, p: Payment) => sum + Number(p.amount), 0);
  const myProgress = startup.fundingGoal && myTotal > 0
    ? Math.min((myTotal / Number(startup.fundingGoal)) * 100, 100)
    : 0;

  /* derive a stable accent hue from the startup name */
  const hue = startup.name.split('').reduce((acc, c) => acc + c.charCodeAt(0), 0) % 360;
  const accent = `hsl(${hue},65%,52%)`;
  const accentSoft = `hsla(${hue},65%,52%,0.12)`;
  const accentMid = `hsla(${hue},65%,52%,0.35)`;
  const accentShift = `hsl(${(hue + 50) % 360},68%,56%)`;

  return (
    <Layout>
      <style>{`
        @keyframes shimmer { 0%{background-position:200% 0} 100%{background-position:-200% 0} }
        @keyframes sd-fade-up { from{opacity:0;transform:translateY(16px)} to{opacity:1;transform:translateY(0)} }
        @keyframes sd-pulse-ring {
          0%{transform:scale(1);opacity:0.55} 70%{transform:scale(1.6);opacity:0} 100%{transform:scale(1.6);opacity:0}
        }

        .sd-page { animation: sd-fade-up 0.4s cubic-bezier(0.22,1,0.36,1) both; }

        .sd-card {
          background: var(--surface);
          border: 1px solid var(--border);
          border-radius: 24px;
          overflow: hidden;
          box-shadow: 0 1px 3px rgba(0,0,0,0.07), 0 8px 32px rgba(0,0,0,0.06);
          transition: box-shadow 0.2s;
        }
        .sd-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.09), 0 16px 48px rgba(0,0,0,0.1); }

        .sd-back-btn {
          display: inline-flex; align-items: center; gap: 6px;
          background: var(--surface); border: 1px solid var(--border);
          cursor: pointer; color: var(--text-muted); font-size: 12px; font-weight: 500;
          padding: 7px 14px; border-radius: 99px; margin-bottom: 28px;
          transition: all 0.15s; letter-spacing: 0.01em;
        }
        .sd-back-btn:hover { color: var(--text-primary); border-color: var(--text-muted); background: var(--surface-2); }

        .sd-hero-banner {
          height: 124px; position: relative; overflow: hidden;
        }
        .sd-hero-banner::after {
          content: '';
          position: absolute; inset: 0;
          background: repeating-linear-gradient(
            -45deg, transparent, transparent 18px,
            rgba(255,255,255,0.045) 18px, rgba(255,255,255,0.045) 36px
          );
        }

        .sd-avatar {
          width: 66px; height: 66px; border-radius: 18px;
          display: flex; align-items: center; justify-content: center;
          font-size: 26px; font-weight: 800;
          font-family: 'Syne', system-ui, sans-serif;
          box-shadow: 0 4px 20px rgba(0,0,0,0.22);
          border: 3px solid var(--surface);
          position: relative; flex-shrink: 0;
        }
        .sd-avatar-ring {
          position: absolute; inset: -7px; border-radius: 24px;
          animation: sd-pulse-ring 3s ease-out infinite;
        }

        .sd-follow-btn {
          display: inline-flex; align-items: center; gap: 6px;
          background: var(--surface-2); border: 1px solid var(--border);
          border-radius: 99px; padding: 8px 16px;
          font-size: 12px; font-weight: 600; color: var(--text-secondary);
          cursor: pointer; transition: all 0.18s; letter-spacing: 0.01em;
        }
        .sd-follow-btn:hover { border-color: var(--brand); color: var(--brand); background: var(--brand-subtle); }

        .sd-meta-pill {
          display: inline-flex; align-items: center; gap: 4px;
          background: var(--surface-2); border: 1px solid var(--border);
          border-radius: 99px; padding: 4px 10px;
          font-size: 11px; font-weight: 500; color: var(--text-muted);
        }

        .sd-section-label {
          font-size: 10px; font-weight: 700; letter-spacing: 0.1em;
          text-transform: uppercase; color: var(--text-faint); margin: 0;
        }

        .sd-inv-row {
          display: flex; align-items: center; justify-content: space-between;
          padding: 14px 24px; transition: background 0.15s;
        }
        .sd-inv-row:hover { background: var(--surface-2); }

        .sd-sidebar-card {
          background: var(--surface);
          border: 1px solid var(--border);
          border-radius: 24px; overflow: hidden;
          box-shadow: 0 1px 3px rgba(0,0,0,0.07), 0 8px 32px rgba(0,0,0,0.06);
          transition: box-shadow 0.2s;
        }
        .sd-sidebar-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.09), 0 16px 48px rgba(0,0,0,0.1); }

        .sd-invest-btn {
          width: 100%; padding: 13px;
          border: none; border-radius: 14px;
          font-size: 13px; font-weight: 700; letter-spacing: 0.02em;
          cursor: pointer; display: flex; align-items: center; justify-content: center;
          gap: 8px; transition: opacity 0.18s, transform 0.12s; color: #fff;
        }
        .sd-invest-btn:hover:not(:disabled) { opacity: 0.88; transform: translateY(-1px); }
        .sd-invest-btn:disabled { opacity: 0.55; cursor: not-allowed; }

        .sd-detail-row {
          display: flex; justify-content: space-between; align-items: center;
          padding: 12px 20px;
        }
        .sd-detail-row + .sd-detail-row { border-top: 1px solid var(--border); }

        .sd-progress-track {
          height: 8px; border-radius: 99px; overflow: hidden;
          background: var(--surface-3); margin: 10px 0 7px;
        }
        .sd-progress-fill {
          height: 100%; border-radius: 99px;
          background: linear-gradient(90deg, #16a34a, #34d399);
          transition: width 0.9s cubic-bezier(0.4,0,0.2,1);
        }
      `}</style>

      <div className="sd-page" style={{ maxWidth: 1080, margin: '0 auto' }}>

        <button onClick={() => navigate(-1)} className="sd-back-btn">
          <ArrowLeft size={13} /> Back
        </button>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 24, alignItems: 'start' }}>

          {/* ══ LEFT COLUMN ═══════════════════════════════════════════════════ */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>

            {/* ── Hero card ──────────────────────────────────────────────────── */}
            <div className="sd-card">
              <div
                className="sd-hero-banner"
                style={{ background: `linear-gradient(135deg, ${accent} 0%, ${accentShift} 60%, hsl(${(hue + 90) % 360},62%,65%) 100%)` }}
              >
                <div style={{ position: 'absolute', width: 200, height: 200, borderRadius: '50%', border: '1px solid rgba(255,255,255,0.14)', top: -70, right: -40, zIndex: 1 }} />
                <div style={{ position: 'absolute', width: 110, height: 110, borderRadius: '50%', background: 'rgba(255,255,255,0.06)', top: 16, right: 90, zIndex: 1 }} />
                <div style={{ position: 'absolute', width: 60, height: 60, borderRadius: '50%', background: 'rgba(255,255,255,0.08)', bottom: -15, left: 130, zIndex: 1 }} />
              </div>

              <div style={{ padding: '0 28px 28px' }}>
                <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', marginTop: -33, marginBottom: 18 }}>
                  <div className="sd-avatar" style={{ background: accentSoft, color: accent }}>
                    <div className="sd-avatar-ring" style={{ border: `2px solid ${accentMid}` }} />
                    {startup.name[0].toUpperCase()}
                  </div>
                  <button onClick={handleFollow} className="sd-follow-btn" style={isFollowed ? { borderColor: 'var(--green)', color: 'var(--green)', background: 'var(--green-bg)' } : {}}>
                    <Heart size={13} style={isFollowed ? { fill: 'var(--green)' } : {}} />
                    {isFollowed ? 'Following' : 'Follow'}
                  </button>
                </div>

                <h1 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 27, fontWeight: 800, color: 'var(--text-primary)', marginBottom: 12, letterSpacing: '-0.4px' }}>
                  {startup.name}
                </h1>

                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginBottom: 18, alignItems: 'center' }}>
                  <span className="badge-blue" style={{ borderRadius: 99 }}>
                    {startup.stage === 'EARLY_TRACTION' ? 'Early Traction' : startup.stage}
                  </span>
                  {startup.isApproved && (
                    <span className="badge-green" style={{ display: 'flex', alignItems: 'center', gap: 4, borderRadius: 99 }}>
                      <CheckCircle size={10} /> Approved
                    </span>
                  )}
                  {startup.location && (
                    <span className="sd-meta-pill"><MapPin size={10} />{startup.location}</span>
                  )}
                  {startup.industry && (
                    <span className="sd-meta-pill"><TrendingUp size={10} />{startup.industry}</span>
                  )}
                </div>

                <p style={{ color: 'var(--text-secondary)', fontSize: 14, lineHeight: 1.85, margin: 0, borderTop: '1px solid var(--border)', paddingTop: 18 }}>
                  {startup.description}
                </p>
              </div>
            </div>

            {/* ── Problem + Solution ─────────────────────────────────────────── */}
            {(startup.problemStatement || startup.solution) && (
              <div className="sd-card">
                <div style={{ padding: '16px 24px', borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', gap: 8 }}>
                  <div style={{ width: 6, height: 6, borderRadius: '50%', background: accent }} />
                  <p className="sd-section-label">Overview</p>
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: startup.problemStatement && startup.solution ? 'repeat(auto-fit, minmax(240px, 1fr))' : '1fr' }}>
                  {startup.problemStatement && (
                    <div style={{ padding: 24, borderRight: startup.solution ? '1px solid var(--border)' : 'none' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 14 }}>
                        <div style={{ width: 34, height: 34, borderRadius: 10, background: 'rgba(239,68,68,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 15 }}>⚠️</div>
                        <p className="sd-section-label" style={{ color: 'rgba(239,68,68,0.7)' }}>Problem</p>
                      </div>
                      <p style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.8, margin: 0 }}>{startup.problemStatement}</p>
                    </div>
                  )}
                  {startup.solution && (
                    <div style={{ padding: 24 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 14 }}>
                        <div style={{ width: 34, height: 34, borderRadius: 10, background: 'rgba(16,185,129,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 15 }}>💡</div>
                        <p className="sd-section-label" style={{ color: 'rgba(16,185,129,0.8)' }}>Solution</p>
                      </div>
                      <p style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.8, margin: 0 }}>{startup.solution}</p>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* ── Investment history ─────────────────────────────────────────── */}
            {investments.length > 0 && (
              <div className="sd-card">
                <div style={{ padding: '16px 24px', borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <div style={{ width: 6, height: 6, borderRadius: '50%', background: accent }} />
                    <p className="sd-section-label">Investment history</p>
                  </div>
                  <span style={{ fontSize: 11, fontWeight: 600, padding: '3px 10px', borderRadius: 99, background: accentSoft, color: accent }}>
                    {investments.length} total
                  </span>
                </div>
                {investments.map((inv: Investment, idx) => (
                  <div key={inv.id} className="sd-inv-row" style={{ borderBottom: idx < investments.length - 1 ? '1px solid var(--border)' : 'none' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                      <div style={{ width: 36, height: 36, borderRadius: 10, background: 'var(--green-bg)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <DollarSign size={15} style={{ color: 'var(--green)' }} />
                      </div>
                      <div>
                        <p style={{ fontSize: 14, fontWeight: 700, color: 'var(--text-primary)', margin: 0, fontFamily: "'Syne', system-ui, sans-serif" }}>
                          ₹{Number(inv.amount).toLocaleString('en-IN')}
                        </p>
                        <p style={{ fontSize: 11, color: 'var(--text-faint)', margin: 0 }}>Investment</p>
                      </div>
                    </div>
                    <span className={getInvestmentBadge(inv.status)}>{inv.status}</span>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* ══ RIGHT SIDEBAR ══════════════════════════════════════════════════ */}
          <div style={{ position: 'sticky', top: 20, display: 'flex', flexDirection: 'column', gap: 16 }}>

            {/* ── Your investment ───────────────────────────────────────────── */}
            {isInvestor && myTotal > 0 && (
              <div style={{
                background: 'var(--surface)',
                border: '1px solid rgba(22,163,74,0.28)',
                borderRadius: 24, padding: 22,
                boxShadow: '0 0 0 4px rgba(22,163,74,0.06), 0 8px 32px rgba(0,0,0,0.06)',
                position: 'relative', overflow: 'hidden',
              }}>
                <div style={{ position: 'absolute', width: 130, height: 130, borderRadius: '50%', background: 'rgba(22,163,74,0.07)', top: -45, right: -35, pointerEvents: 'none' }} />
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 14 }}>
                  <CheckCircle size={13} style={{ color: 'var(--green)' }} />
                  <p style={{ fontSize: 10, fontWeight: 700, letterSpacing: '0.09em', textTransform: 'uppercase' as const, color: 'var(--green)', margin: 0 }}>Your Investment</p>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 4 }}>
                  <span style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 26, fontWeight: 800, color: 'var(--green)', letterSpacing: '-0.5px' }}>
                    ₹{myTotal.toLocaleString('en-IN')}
                  </span>
                  <span style={{ fontSize: 11, color: 'var(--text-muted)', fontWeight: 500 }}>
                    of ₹{Number(startup.fundingGoal).toLocaleString('en-IN')}
                  </span>
                </div>
                <div className="sd-progress-track">
                  <div className="sd-progress-fill" style={{ width: `${myProgress}%` }} />
                </div>
                <p style={{ fontSize: 11, color: 'var(--text-faint)', margin: 0 }}>
                  {myProgress.toFixed(1)}% of goal · {myPayments.length} confirmed
                </p>
              </div>
            )}

            {/* ── Invest form ───────────────────────────────────────────────── */}
            {isInvestor && (
              <div className="sd-sidebar-card">
                <div style={{ height: 5, background: `linear-gradient(90deg, ${accent}, ${accentShift})` }} />
                <div style={{ padding: 22 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 6 }}>
                    <div style={{ width: 36, height: 36, borderRadius: 11, background: accentSoft, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                      <Zap size={16} style={{ color: accent }} />
                    </div>
                    <h2 style={{ fontFamily: "'Syne', system-ui, sans-serif", fontSize: 15, fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
                      Invest in {startup.name}
                    </h2>
                  </div>
                  <p style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 18, marginTop: 8, lineHeight: 1.55 }}>
                    Your investment will be reviewed by the founder
                  </p>
                  <form onSubmit={handleSubmit(onInvest)} style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                    <div style={{ position: 'relative' }}>
                      <span style={{ position: 'absolute', left: 14, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)', fontSize: 14, pointerEvents: 'none' }}>₹</span>
                      <input
                        type="number"
                        className="input-field"
                        style={{ paddingLeft: 30 }}
                        placeholder="Enter amount"
                        {...register('amount', { required: true, min: 1 })}
                      />
                      {errors.amount && <p style={{ fontSize: 11, color: 'var(--red)', marginTop: 4 }}>Enter a valid amount</p>}
                    </div>
                    <button
                      type="submit"
                      disabled={paymentLoading}
                      className="sd-invest-btn"
                      style={{ background: `linear-gradient(135deg, ${accent}, ${accentShift})` }}
                    >
                      <CreditCard size={14} /> {paymentLoading ? 'Processing…' : 'Invest via Razorpay'}
                    </button>
                  </form>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 14, padding: '10px 14px', background: 'var(--surface-2)', borderRadius: 10 }}>
                    <Shield size={11} style={{ color: 'var(--text-faint)', flexShrink: 0 }} />
                    <p style={{ fontSize: 11, color: 'var(--text-faint)', margin: 0 }}>Secured by Razorpay · test mode active</p>
                  </div>
                </div>
              </div>
            )}

            {/* ── Startup details ───────────────────────────────────────────── */}
            <div className="sd-sidebar-card">
              <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', gap: 8 }}>
                <div style={{ width: 6, height: 6, borderRadius: '50%', background: accent }} />
                <p className="sd-section-label">Startup details</p>
              </div>
              <div>
                {[
                  { label: 'Industry', value: startup.industry },
                  { label: 'Stage', value: startup.stage === 'EARLY_TRACTION' ? 'Early Traction' : startup.stage },
                  { label: 'Location', value: startup.location },
                  startup.fundingGoal ? { label: 'Funding goal', value: `₹${Number(startup.fundingGoal).toLocaleString('en-IN')}` } : null,
                ].filter(Boolean).map((item, i) => (
                  <div key={i} className="sd-detail-row">
                    <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>{item!.label}</span>
                    <span style={{ fontSize: 12, fontWeight: 700, color: 'var(--text-primary)' }}>{item!.value || '—'}</span>
                  </div>
                ))}
              </div>
            </div>

          </div>
        </div>
      </div>
    </Layout>
  );
};

export default StartupDetail;
